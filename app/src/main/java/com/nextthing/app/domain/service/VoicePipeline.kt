package com.nextthing.app.domain.service

import com.nextthing.app.domain.model.VoicePipelineResult

/**
 * 语音管道接口（ASR + SER 并行）
 *
 * @DESC: 顶层语音处理入口，将 ASR 和 SER 并行执行后合并结果
 * 1. 接收 PCM 音频数据
 * 2. 同时启动 ASR（语音转文字）和 SER（情绪识别）
 * 3. 两个结果合并返回
 *
 * 补充：App 层（ViewModel/Activity）只调用此接口，不直接操作 ASR 或 SER
 */
interface VoicePipeline {

    /**
     * @DESC: 初始化管道（加载 SER 模型等）
     * 1. 调用 SERService.loadModel()
     */
    suspend fun initialize()

    /**
     * @DESC: 对一段 PCM 音频执行 ASR + SER 并行识别
     * 1. async 启动 SER 识别
     * 2. 两个结果合并为 VoicePipelineResult
     *
     * @Param: samples — PCM 采样点数组（16bit 有符号整数）
     * @Param: sampleRate — 采样率（默认 16000）
     * @return VoicePipelineResult（文字 + 情绪 + 耗时）
     */
    suspend fun processAudio(samples: ShortArray, sampleRate: Int = 16000): VoicePipelineResult

    /**
     * @DESC: 启动 ASR 录音（Two-pass 模式）
     * 1. Pass 1（流式）实时回调 onPartial
     * 2. 用户说完后调用 stopListening() 触发 Pass 2
     *
     * @Param: onPartial — ASR 中间结果回调（流式出字）
     * @Param: onFinal — ASR 最终结果回调（整段识别）
     * @Param: onError — 错误回调
     */
    fun startListening(
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onError: (String) -> Unit
    )

    /**
     * @DESC: 停止 ASR 录音，触发 Pass 2 整段识别
     */
    fun stopListening()

    /**
     * @DESC: 释放资源
     */
    fun release()
}
