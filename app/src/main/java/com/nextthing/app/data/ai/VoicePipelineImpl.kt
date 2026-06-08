package com.nextthing.app.data.ai

import com.nextthing.app.domain.model.VoicePipelineResult
import com.nextthing.app.domain.service.ASRService
import com.nextthing.app.domain.service.SERService
import com.nextthing.app.domain.service.VoicePipeline
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 语音管道实现（ASR + SER 并行）
 *
 * @DESC: 将 ASR 和 SER 并行执行，合并结果后返回
 * 1. initialize() — 加载 SER 模型
 * 2. processAudio() — 并行执行 SER 识别，返回合并结果
 * 3. startListening() — 启动 ASR 录音（Two-pass 模式）
 * 4. stopListening() — 停止 ASR 录音，触发 Pass 2
 * 5. release() — 释放资源
 *
 * 补充：ASR 通过回调模式工作（onPartial 实时出字，onFinal 最终结果）
 */
@Singleton
class VoicePipelineImpl @Inject constructor(
    private val serService: SERService,                      // SER 情绪识别服务
    private val asrService: ASRService                       // ASR 语音识别服务
) : VoicePipeline {

    companion object {
        private const val TAG = "VoicePipeline"
    }

    private var isInitialized = false                        // 管道是否已初始化

    /**
     * @DESC: 初始化管道
     * 1. 调用 SERService.loadModel() 加载 SER 模型
     * 2. 检查 ASR 是否已配置
     * 3. 标记为已初始化
     */
    override suspend fun initialize() {
        Timber.tag(TAG).d("初始化管道...")
        serService.loadModel()
        val asrReady = asrService.isConfigured()
        isInitialized = true
        Timber.tag(TAG).d("管道初始化完成，ASR: $asrReady")
    }

    /**
     * @DESC: 对一段 PCM 音频执行 ASR + SER 并行识别
     * 1. 记录开始时间
     * 2. async 启动 SER 识别
     * 3. await 等待 SER 结果
     * 4. 合并为 VoicePipelineResult
     *
     * @Param: samples — PCM 采样点数组（16bit 有符号整数）
     * @Param: sampleRate — 采样率（默认 16000）
     * @return VoicePipelineResult（文字 + 情绪 + 耗时）
     *
     * 补充：此方法用于离线模式（已有 PCM 数据时）
     *       在线模式用 startListening/stopListening
     */
    override suspend fun processAudio(samples: ShortArray, sampleRate: Int): VoicePipelineResult {
        val startTime = System.nanoTime()                   // 记录开始时间

        return coroutineScope {
            // 1. async 启动 SER 识别（并行）
            val deferredSER = async { serService.recognize(samples, sampleRate) }

            // 2. await 等待 SER 结果
            val serResult = deferredSER.await()

            // 3. 计算总耗时
            val latencyMs = (System.nanoTime() - startTime) / 1_000_000

            // 4. 合并结果（ASR 文字由上层回调获取，此处留空）
            Timber.tag(TAG).d("processAudio 完成，SER: ${serResult.emotion}(${serResult.confidence}), 耗时: ${latencyMs}ms")
            VoicePipelineResult(
                text = "",                                   // ASR 文字通过 startListening 回调获取
                serResult = serResult,
                latencyMs = latencyMs
            )
        }
    }

    /**
     * @DESC: 启动 ASR 录音（Two-pass 模式）
     * 1. 调用 ASRService.start() 开始录音
     * 2. Pass 1 实时回调 onPartial（中间文字）
     * 3. 用户说完后调用 stopListening() 触发 Pass 2
     *
     * @Param: onPartial — ASR 中间结果回调（Pass 1 流式出字）
     * @Param: onFinal — ASR 最终结果回调（Pass 2 整段识别）
     * @Param: onError — 错误回调
     */
    override fun startListening(
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        Timber.tag(TAG).d("启动 ASR 录音...")
        asrService.start(
            onPartial = { text ->
                Timber.tag(TAG).d("ASR onPartial: $text")
                onPartial(text)
            },
            onFinal = { text ->
                Timber.tag(TAG).d("ASR onFinal: $text")
                onFinal(text)
            },
            onError = { error ->
                Timber.tag(TAG).e("ASR onError: $error")
                onError(error)
            }
        )
    }

    /**
     * @DESC: 停止 ASR 录音，触发 Pass 2 整段识别
     * 1. 调用 ASRService.stop()
     * 2. ASR 会自动做 Pass 2（SenseVoice 整段推理）
     * 3. 结果通过 onFinal 回调返回
     */
    override fun stopListening() {
        Timber.tag(TAG).d("停止 ASR 录音")
        asrService.stop()
    }

    /**
     * @DESC: 释放资源
     * 1. 调用 SERService.release()
     * 2. 标记为未初始化
     */
    override fun release() {
        Timber.tag(TAG).d("释放管道资源")
        serService.release()
        isInitialized = false
    }
}
