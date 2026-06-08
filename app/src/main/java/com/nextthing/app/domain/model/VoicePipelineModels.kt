package com.nextthing.app.domain.model

/**
 * 语音管道相关数据类
 */

/**
 * 语音管道结果（ASR + SER 合并）
 *
 * @param text ASR 识别出的文字
 * @param serResult SER 情绪识别结果（可能为 null，SER 未就绪时）
 * @param latencyMs 管道总耗时（从 PCM 输入到结果输出）
 */
data class VoicePipelineResult(
    val text: String,
    val serResult: SERResult?,
    val latencyMs: Long
)
