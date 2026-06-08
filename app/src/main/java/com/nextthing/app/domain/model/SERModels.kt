package com.nextthing.app.domain.model

/**
 * SER（语音情绪识别）相关数据类
 */

/**
 * 情绪类型枚举
 * 与 CASIA 数据集训练时的标签顺序一致：angry/fear/happy/neutral/sad/surprise
 */
enum class Emotion(val label: String) {
    ANGRY("angry"),       // 愤怒
    FEAR("fear"),         // 恐惧
    HAPPY("happy"),       // 开心
    NEUTRAL("neutral"),   // 中性
    SAD("sad"),           // 悲伤
    SURPRISE("surprise")  // 惊讶
}

/**
 * SER 识别结果
 *
 * @param emotion 识别到的情绪类型
 * @param confidence 置信度（0.0~1.0），Softmax 后的最大概率值
 * @param probabilities 所有情绪的概率分布（Softmax 后的完整输出）
 * @param latencyMs 整个 SER 管道耗时（预处理 + 推理）
 */
data class SERResult(
    val emotion: Emotion,
    val confidence: Float,
    val probabilities: Map<Emotion, Float>,
    val latencyMs: Long
)
