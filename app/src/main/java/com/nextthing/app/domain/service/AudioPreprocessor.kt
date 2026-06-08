package com.nextthing.app.domain.service

/**
 * 音频预处理器接口
 *
 * @DESC: 将原始 PCM 音频数据预处理为模型可用的 MFCC 特征
 * 1. VAD 检测人声片段
 * 2. 对人声片段提取 MFCC 特征（与训练时参数对齐）
 * 3. z-score 归一化
 * 4. 输出 [39, 帧数] 的二维数组
 *
 * 补充：训练时用 librosa 做预处理，端侧需要复现相同的结果
 */
interface AudioPreprocessor {

    /**
     * @DESC: 判断一帧音频是否包含人声
     * 1. 计算当前帧的能量
     * 2. 与阈值对比
     *
     * @Param: samples — PCM 采样点数组（16bit 有符号整数）
     * @Param: sampleRate — 采样率（如 16000）
     * @return true=有人声，false=静音/噪音
     */
    fun isVoiceActive(samples: ShortArray, sampleRate: Int): Boolean

    /**
     * @DESC: 对一段完整音频提取 MFCC 特征
     * 1. 分帧（25ms 窗口，10ms 步进）
     * 2. 提取 13 维 MFCC + 13 维 Delta + 13 维 Delta-Delta = 39 维
     * 3. z-score 归一化
     *
     * @Param: samples — PCM 采样点数组（16bit 有符号整数）
     * @Param: sampleRate — 采样率（如 16000）
     * @return 二维浮点数组 [39][帧数]，可直接送入 TFLite 模型
     */
    fun extractMFCC(samples: ShortArray, sampleRate: Int): Array<FloatArray>
}
