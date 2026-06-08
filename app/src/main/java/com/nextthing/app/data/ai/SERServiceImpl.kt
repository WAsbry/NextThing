package com.nextthing.app.data.ai

import com.nextthing.app.domain.model.Accelerator
import com.nextthing.app.domain.model.Emotion
import com.nextthing.app.domain.model.InferenceResult
import com.nextthing.app.domain.model.ModelConfig
import com.nextthing.app.domain.model.SERResult
import com.nextthing.app.domain.service.AudioPreprocessor
import com.nextthing.app.domain.service.OnDeviceAIEngine
import com.nextthing.app.domain.service.SERService
import kotlin.math.exp
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SER 服务实现
 *
 * @DESC: 串联 AudioPreprocessor 和 OnDeviceAIEngine，提供端到端情绪识别
 * 1. loadModel() — 加载 SER 模型到推理引擎
 * 2. recognize() — PCM → MFCC → 推理 → Softmax → 情绪结果
 * 3. release() — 释放资源
 *
 * 补充：通过 Hilt 注入 AudioPreprocessor 和 OnDeviceAIEngine
 */
@Singleton
class SERServiceImpl @Inject constructor(
    private val audioPreprocessor: AudioPreprocessor,       // 音频预处理器
    private val aiEngine: OnDeviceAIEngine                   // AI 推理引擎
) : SERService {

    companion object {
        private const val MODEL_FILE = "ser_model.tflite"   // 模型文件名（assets/models/ 下）
        private val EMOTIONS = Emotion.entries.toTypedArray() // 情绪标签，与训练顺序一致
    }

    private var isModelLoaded = false                        // 模型是否已加载

    /**
     * @DESC: 加载 SER 模型
     * 1. 构造 ModelConfig（NPU 优先，4 线程）
     * 2. 调用 aiEngine.loadModel()
     * 3. 标记为已加载
     */
    override suspend fun loadModel() {
        val config = ModelConfig(
            modelFileName = MODEL_FILE,
            accelerator = Accelerator.NPU,
            numThreads = 4
        )
        aiEngine.loadModel(config)
        isModelLoaded = true
    }

    /**
     * @DESC: 对一段 PCM 音频进行情绪识别
     * 1. 记录开始时间
     * 2. audioPreprocessor.extractMFCC() 提取特征
     * 3. aiEngine.infer() 执行推理，拿到 logits
     * 4. softmax() 将 logits 转为概率
     * 5. 找到最大概率对应的情绪
     * 6. 计算总耗时
     *
     * @Param: samples — PCM 采样点数组（16bit 有符号整数）
     * @Param: sampleRate — 采样率（默认 16000）
     * @return SERResult（情绪 + 置信度 + 概率分布 + 耗时）
     */
    override suspend fun recognize(samples: ShortArray, sampleRate: Int): SERResult {
        val startTime = System.nanoTime()                   // 记录开始时间

        // 1. 提取 MFCC 特征：[39, 帧数]
        val mfccFeatures = audioPreprocessor.extractMFCC(samples, sampleRate)

        // 2. 模型推理，拿到 logits（原始输出）
        val inferenceResult: InferenceResult = aiEngine.infer(mfccFeatures)

        // 3. 将 logits 转为概率分布
        val logits = inferenceResult.data as FloatArray      // 模型原始输出
        val probabilities = softmax(logits)                  // Softmax 归一化

        // 4. 找到最大概率对应的情绪
        val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
        val emotion = EMOTIONS[maxIndex]
        val confidence = probabilities[maxIndex]

        // 5. 构建概率分布 Map
        val probMap = EMOTIONS.associateWith { probabilities[EMOTIONS.indexOf(it)] }

        // 6. 计算总耗时
        val latencyMs = (System.nanoTime() - startTime) / 1_000_000

        return SERResult(
            emotion = emotion,
            confidence = confidence,
            probabilities = probMap,
            latencyMs = latencyMs
        )
    }

    /**
     * @DESC: 释放模型资源
     * 1. 调用 aiEngine.release()
     * 2. 标记为未加载
     */
    override fun release() {
        aiEngine.release()
        isModelLoaded = false
    }

    /**
     * @DESC: Softmax 函数，将 logits 转为概率分布
     * 1. 计算每个 logit 的 exp 值
     * 2. 求和
     * 3. 每个 exp 值除以总和，得到概率
     *
     * @Param: logits — 模型原始输出（任意实数）
     * @return 概率数组（0.0~1.0，总和为 1.0）
     *
     * 补充：公式 P(i) = e^(logit_i) / Σ e^(logit_j)
     */
    private fun softmax(logits: FloatArray): FloatArray {
        val maxLogit = logits.maxOrNull() ?: 0f             // 减去最大值防止数值溢出
        val exps = FloatArray(logits.size) { i ->
            exp((logits[i] - maxLogit).toDouble()).toFloat()
        }
        val sum = exps.sum()                                // 所有 exp 值求和
        return FloatArray(exps.size) { i -> exps[i] / sum } // 归一化为概率
    }
}
