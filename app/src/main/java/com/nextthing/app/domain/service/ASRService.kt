package com.nextthing.app.domain.service

import kotlinx.coroutines.flow.StateFlow

interface ASRService {
    suspend fun isConfigured(): Boolean

    /**
     * 模型是否已加载就绪
     * 云端 ASR 始终返回 true，端侧 ASR 模型加载完成后才返回 true
     */
    val isReady: StateFlow<Boolean>

    /**
     * 模型或 native runtime 缺失时的状态说明。
     */
    val errorMessage: StateFlow<String?>

    /**
     * 预热模型（后台加载，不阻塞 UI）
     * 云端 ASR 无需操作，端侧 ASR 在后台初始化模型
     */
    fun warmUp() {}

    /**
     * 开始语音识别。
     * 所有回调均在主线程调用。
     *
     * @param onPartial 识别中（中间结果），实时更新显示用
     * @param onFinal   识别完成（最终结果）
     * @param onError   发生错误时的提示文字
     */
    fun start(
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onError: (String) -> Unit,
        onAudioCaptured: (samples: ShortArray, sampleRate: Int) -> Unit = { _, _ -> }
    )

    /** 手动停止录音（发送最终帧，等待最终结果回调） */
    fun stop()

    /** 页面不再使用 ASR 时释放模型、Stream 与录音资源。 */
    fun release() {}
}
