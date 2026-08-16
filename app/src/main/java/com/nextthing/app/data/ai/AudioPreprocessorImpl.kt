package com.nextthing.app.data.ai

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.mfcc.MFCC
import com.nextthing.app.domain.service.AudioPreprocessor
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 音频预处理器实现
 *
 * @DESC: 将原始 PCM 音频预处理为 TFLite 模型可用的 MFCC 特征
 * 1. VAD：基于短时能量判断是否有人声
 * 2. MFCC：使用 TarsosDSP 提取 39 维特征（13 MFCC + 13 Delta + 13 Delta-Delta）
 * 3. 归一化：z-score 归一化，与训练时一致
 *
 * @Param: 无构造参数，通过 Hilt 注入
 *
 * 补充：MFCC 参数必须和训练脚本 train_ser.py 中的 librosa 参数对齐
 *       训练时：n_mfcc=13, n_fft=400(25ms), hop_length=160(10ms), sr=16000
 */
@Singleton
class AudioPreprocessorImpl @Inject constructor() : AudioPreprocessor {

    companion object {
        private const val SAMPLE_RATE = 16000                      // 采样率：16kHz，与训练时一致
        private const val FRAME_SIZE_MS = 25                       // 帧大小：25ms
        private const val HOP_SIZE_MS = 10                         // 帧移：10ms
        private const val FRAME_SIZE_SAMPLES = SAMPLE_RATE * FRAME_SIZE_MS / 1000   // 25ms = 400 采样点
        private const val HOP_SIZE_SAMPLES = SAMPLE_RATE * HOP_SIZE_MS / 1000       // 10ms = 160 采样点
        private const val MFCC_DIM = 13                            // MFCC 维度：13 维，与训练时一致
        private const val FEATURE_DIM = 39                         // 总特征维度：13 + 13 Delta + 13 Delta-Delta
        private const val VAD_ENERGY_THRESHOLD = 500.0             // VAD 能量阈值，低于此值认为是静音
    }

    /**
     * @DESC: 判断一帧音频是否包含人声（基于短时能量）
     * 1. 计算 PCM 采样点的均方根能量（RMS）
     * 2. 与阈值对比，超过阈值认为有人声
     *
     * @Param: samples — PCM 采样点数组（16bit 有符号整数）
     * @Param: sampleRate — 采样率（当前未使用，预留扩展）
     * @return true=有人声，false=静音/噪音
     */
    override fun isVoiceActive(samples: ShortArray, sampleRate: Int): Boolean {
        // 1. 计算短时能量（RMS）
        var sumSquares = 0.0
        for (sample in samples) {
            val normalized = sample.toDouble() / Short.MAX_VALUE    // 归一化到 [-1.0, 1.0]
            sumSquares += normalized * normalized                    // 累加平方
        }
        val rms = Math.sqrt(sumSquares / samples.size)              // 均方根 = sqrt(平方和/采样点数)

        // 2. 与阈值对比
        return rms * Short.MAX_VALUE > VAD_ENERGY_THRESHOLD         // 还原到原始量级比较
    }

    /**
     * @DESC: 对一段完整音频提取 39 维 MFCC 特征
     * 1. 将 ShortArray 转为 FloatArray（TarsosDSP 要求）
     * 2. 分帧提取 13 维 MFCC
     * 3. 计算 13 维 Delta（一阶差分）
     * 4. 计算 13 维 Delta-Delta（二阶差分）
     * 5. 拼接为 [39][帧数]，z-score 归一化后返回
     *
     * @Param: samples — PCM 采样点数组（16bit 有符号整数）
     * @Param: sampleRate — 采样率（如 16000）
     * @return 二维浮点数组 [39][帧数]，可直接送入 TFLite 模型
     */
    override fun extractMFCC(samples: ShortArray, sampleRate: Int): Array<FloatArray> {
        // 1. ShortArray → FloatArray（归一化到 [-1.0, 1.0]）
        val floatSamples = FloatArray(samples.size) { i ->
            samples[i].toFloat() / Short.MAX_VALUE.toFloat()
        }

        // 2. 计算帧数
        val numFrames = (floatSamples.size - FRAME_SIZE_SAMPLES) / HOP_SIZE_SAMPLES + 1
        require(numFrames > 0) { "音频太短，无法分帧" }

        // 3. 逐帧提取 13 维 MFCC
        val mfccFrames = Array(numFrames) { FloatArray(MFCC_DIM) }
        for (frameIdx in 0 until numFrames) {
            val start = frameIdx * HOP_SIZE_SAMPLES
            val end = minOf(start + FRAME_SIZE_SAMPLES, floatSamples.size)
            val frame = floatSamples.copyOfRange(start, end)

            // 如果最后一帧不够长，补零
            val paddedFrame = if (frame.size < FRAME_SIZE_SAMPLES) {
                FloatArray(FRAME_SIZE_SAMPLES) { i -> if (i < frame.size) frame[i] else 0f }
            } else {
                frame
            }

            mfccFrames[frameIdx] = computeMFCCForFrame(paddedFrame, sampleRate)
        }

        // 4. 计算 Delta（一阶差分）
        val deltaFrames = computeDelta(mfccFrames)

        // 5. 计算 Delta-Delta（二阶差分）
        val deltaDeltaFrames = computeDelta(deltaFrames)

        // 6. 拼接 [MFCC, Delta, Delta-Delta] → [39][帧数]
        val features = Array(FEATURE_DIM) { FloatArray(numFrames) }
        for (frameIdx in 0 until numFrames) {
            for (dim in 0 until MFCC_DIM) {
                features[dim][frameIdx] = mfccFrames[frameIdx][dim]                    // 前 13 维：MFCC
                features[MFCC_DIM + dim][frameIdx] = deltaFrames[frameIdx][dim]         // 中 13 维：Delta
                features[MFCC_DIM * 2 + dim][frameIdx] = deltaDeltaFrames[frameIdx][dim] // 后 13 维：Delta-Delta
            }
        }

        // 7. z-score 归一化（每个维度独立归一化）
        normalizeFeatures(features)

        return features
    }

    /**
     * @DESC: 对单帧音频计算 13 维 MFCC
     * 1. 创建 TarsosDSP MFCC 处理器
     * 2. 将帧数据送入处理
     * 3. 返回 13 维 MFCC 值
     *
     * @Param: frame — 单帧音频数据（FloatArray，归一化到 [-1.0, 1.0]）
     * @Param: sampleRate — 采样率
     * @return 13 维 MFCC 值
     *
     * 补充：TarsosDSP 的 MFCC 参数需和 librosa 对齐
     *       n_fft = FRAME_SIZE_SAMPLES (400 = 25ms * 16kHz)
     *       mel_filters = 26（librosa 默认 n_mels=128，但 TarsosDSP 用 26）
     */
    private fun computeMFCCForFrame(frame: FloatArray, sampleRate: Int): FloatArray {
        val format = TarsosDSPAudioFormat(sampleRate.toFloat(), 16, 1, true, false)   // 16bit 单声道
        val mfccProcessor = MFCC(FRAME_SIZE_SAMPLES, sampleRate.toFloat(), MFCC_DIM, 26, 133.33f, 6855.5f)   // 13 维 MFCC，26 个梅尔滤波器
        val audioEvent = AudioEvent(format).apply {
            setFloatBuffer(frame.copyOf())
        }
        mfccProcessor.process(audioEvent)
        return mfccProcessor.mfcc.clone()
    }

    /**
     * @DESC: 计算一阶差分（Delta）
     * 1. 对每个维度，用前后 N 帧的差分加权平均
     * 2. 边界帧用最近的有效值
     *
     * @Param: features — [帧数][13] 的 MFCC 数组
     * @return [帧数][13] 的 Delta 数组
     *
     * 补充：librosa.feature.delta 默认 width=9，这里简化为 width=2（前后各 1 帧）
     *       端侧计算量更小，效果接近
     */
    private fun computeDelta(features: Array<FloatArray>): Array<FloatArray> {
        val numFrames = features.size
        val numDims = features[0].size
        val delta = Array(numFrames) { FloatArray(numDims) }

        for (frameIdx in 0 until numFrames) {
            for (dim in 0 until numDims) {
                val prev = if (frameIdx > 0) features[frameIdx - 1][dim] else features[0][dim]      // 前一帧，首帧用自身
                val next = if (frameIdx < numFrames - 1) features[frameIdx + 1][dim] else features[numFrames - 1][dim]  // 后一帧，末帧用自身
                delta[frameIdx][dim] = (next - prev) / 2.0f                                          // 差分 = (后-前)/2
            }
        }
        return delta
    }

    /**
     * @DESC: z-score 归一化（对每个维度独立归一化）
     * 1. 计算每个维度的均值和标准差
     * 2. (值 - 均值) / 标准差
     *
     * @Param: features — [39][帧数] 的特征数组，原地修改
     *
     * 补充：与训练时 sklearn.preprocessing.scale 行为一致
     */
    private fun normalizeFeatures(features: Array<FloatArray>) {
        for (dim in features.indices) {
            val dimValues = features[dim]

            // 1. 计算均值
            var sum = 0.0
            for (v in dimValues) sum += v
            val mean = sum / dimValues.size

            // 2. 计算标准差
            var sumSq = 0.0
            for (v in dimValues) sumSq += (v - mean) * (v - mean)
            val std = Math.sqrt(sumSq / dimValues.size)

            // 3. 归一化，标准差为 0 时（常数维度）全部置 0
            if (std > 1e-6) {
                for (i in dimValues.indices) {
                    dimValues[i] = ((dimValues[i] - mean) / std).toFloat()
                }
            } else {
                for (i in dimValues.indices) {
                    dimValues[i] = 0f
                }
            }
        }
    }
}
