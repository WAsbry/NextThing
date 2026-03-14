package com.example.nextthingb1.domain.service

interface ASRService {
    suspend fun isConfigured(): Boolean

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
        onError: (String) -> Unit
    )

    /** 手动停止录音（发送最终帧，等待最终结果回调） */
    fun stop()
}
