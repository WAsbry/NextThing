package com.nextthing.app.data.asr

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineSenseVoiceModelConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineParaformerModelConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineStream
import com.nextthing.app.domain.service.ASRService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sherpa-ONNX 端侧 ASR 服务（Two-pass 模拟流式）
 *
 * @DESC: 基于 Sherpa-ONNX 实现端侧离线语音识别
 * 1. Pass 1（流式）：Paraformer-streaming 边说边出字，提供实时反馈
 * 2. Pass 2（整段）：SenseVoice 整段推理，提供高精度最终结果
 * 3. 两个 Pass 互不依赖，用户先看到中间结果，说完后替换为最终结果
 *
 * 补充：完全离线，不依赖网络
 */
@Singleton
class SherpaASRService @Inject constructor(
    @ApplicationContext private val context: Context                                 // 用于获取 AssetManager
) : ASRService {

    companion object {
        private const val TAG = "SherpaASR"
        private const val SAMPLE_RATE = 16000                    // 采样率 16kHz
        private const val SHERPA_MODEL_DIR = "models/sherpa-onnx"   // assets 下的模型目录
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())    // 回调切主线程

    private val _isReady = MutableStateFlow(false)               // 模型是否加载完成
    override val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private var onlineRecognizer: OnlineRecognizer? = null       // Pass 1：流式识别器
    private var onlineStream: OnlineStream? = null               // 流式识别的 Stream
    private var offlineRecognizer: OfflineRecognizer? = null     // Pass 2：整段识别器
    private var pendingAudio = mutableListOf<Float>()            // 缓存音频数据，供 Pass 2 使用

    private var audioRecord: AudioRecord? = null                 // Android 录音器
    @Volatile
    private var isRunning = false                                // 是否正在识别

    /**
     * @DESC: 预热模型（在后台线程加载模型，避免首次使用时卡顿）
     * 1. 初始化 OnlineRecognizer
     * 2. 初始化 OfflineRecognizer
     * 3. 首次长按时 start() 会跳过已初始化的模型，直接开始录音
     */
    override fun warmUp() {
        if (onlineRecognizer != null && offlineRecognizer != null) {
            Timber.tag(TAG).d("模型已初始化，跳过预热")
            _isReady.value = true
            return
        }
        Timber.tag(TAG).d("warmUp() 被调用，开始预热...")
        scope.launch {
            try {
                if (onlineRecognizer == null) {
                    val t1 = System.currentTimeMillis()
                    Timber.tag(TAG).d("预热：开始初始化 OnlineRecognizer...")
                    val onlineConfig = OnlineRecognizerConfig(
                        modelConfig = OnlineModelConfig(
                            paraformer = OnlineParaformerModelConfig(
                                encoder = "$SHERPA_MODEL_DIR/paraformer/encoder.int8.onnx",
                                decoder = "$SHERPA_MODEL_DIR/paraformer/decoder.int8.onnx",
                            ),
                            tokens = "$SHERPA_MODEL_DIR/paraformer/tokens.txt",
                            modelType = "paraformer",
                            numThreads = 4,
                        )
                    )
                    onlineRecognizer = OnlineRecognizer(
                        assetManager = context.assets,
                        config = onlineConfig
                    )
                    val t2 = System.currentTimeMillis()
                    Timber.tag(TAG).d("预热：OnlineRecognizer 完成，耗时 ${t2 - t1}ms")
                }
                if (offlineRecognizer == null) {
                    val t3 = System.currentTimeMillis()
                    Timber.tag(TAG).d("预热：开始初始化 OfflineRecognizer...")
                    val offlineConfig = OfflineRecognizerConfig(
                        modelConfig = OfflineModelConfig(
                            senseVoice = OfflineSenseVoiceModelConfig(
                                model = "$SHERPA_MODEL_DIR/sense-voice/model.int8.onnx",
                                language = "zh",
                                useInverseTextNormalization = true,
                            ),
                            tokens = "$SHERPA_MODEL_DIR/sense-voice/tokens.txt",
                            numThreads = 4,
                        )
                    )
                    offlineRecognizer = OfflineRecognizer(
                        assetManager = context.assets,
                        config = offlineConfig
                    )
                    val t4 = System.currentTimeMillis()
                    Timber.tag(TAG).d("预热：OfflineRecognizer 完成，耗时 ${t4 - t3}ms")
                }
                _isReady.value = true
                Timber.tag(TAG).d("预热完成，模型就绪")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "预热失败")
            }
        }
    }

    /**
     * @DESC: 检查是否已配置（模型是否可加载）
     * 1. 模型打包在 assets 中，始终可用
     * @return true
     */
    override suspend fun isConfigured(): Boolean = true          // 端侧模型始终可用

    /**
     * @DESC: 开始语音识别（Two-pass）
     * 1. 初始化 OnlineRecognizer（Paraformer-streaming）
     * 2. 初始化 OfflineRecognizer（SenseVoice）
     * 3. 创建 OnlineStream
     * 4. 启动 AudioRecord 录音
     * 5. 持续读取 PCM 数据送入 OnlineStream
     * 6. 每次 decode 后回调 onPartial（中间结果）
     *
     * @Param: onPartial — 中间结果回调（Pass 1 流式出字）
     * @Param: onFinal — 最终结果回调（Pass 2 整段精准识别）
     * @Param: onError — 错误回调
     */
    @SuppressLint("MissingPermission")
    override fun start(
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            // 1. 初始化 Pass 1（流式 Paraformer）
            if (onlineRecognizer == null) {
                val onlineConfig = OnlineRecognizerConfig(
                    modelConfig = OnlineModelConfig(
                        paraformer = OnlineParaformerModelConfig(
                            encoder = "$SHERPA_MODEL_DIR/paraformer/encoder.int8.onnx",
                            decoder = "$SHERPA_MODEL_DIR/paraformer/decoder.int8.onnx",
                        ),
                        tokens = "$SHERPA_MODEL_DIR/paraformer/tokens.txt",
                        modelType = "paraformer",
                        numThreads = 4,
                    )
                )
                onlineRecognizer = OnlineRecognizer(
                    assetManager = context.assets,
                    config = onlineConfig
                )
                Timber.tag(TAG).d("OnlineRecognizer 初始化完成")
            }

            // 2. 初始化 Pass 2（整段 SenseVoice）
            if (offlineRecognizer == null) {
                val offlineConfig = OfflineRecognizerConfig(
                    modelConfig = OfflineModelConfig(
                        senseVoice = OfflineSenseVoiceModelConfig(
                            model = "$SHERPA_MODEL_DIR/sense-voice/model.int8.onnx",
                            language = "zh",
                            useInverseTextNormalization = true,
                        ),
                        tokens = "$SHERPA_MODEL_DIR/sense-voice/tokens.txt",
                        numThreads = 4,
                    )
                )
                offlineRecognizer = OfflineRecognizer(
                    assetManager = context.assets,
                    config = offlineConfig
                )
                Timber.tag(TAG).d("OfflineRecognizer 初始化完成")
            }

            // 3. 创建新的 Stream
            onlineStream = onlineRecognizer!!.createStream()
            pendingAudio.clear()
            isRunning = true

            // 4. 初始化 AudioRecord
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val frameSize = 3200                                // 100ms × 16000Hz × 2字节
            val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, channelConfig, audioFormat)
            val bufSize = maxOf(minBuf, frameSize * 4)

            val ar = AudioRecord(
                MediaRecorder.AudioSource.MIC,                   // 麦克风音源
                SAMPLE_RATE,
                channelConfig,
                audioFormat,
                bufSize
            )
            audioRecord = ar

            if (ar.state != AudioRecord.STATE_INITIALIZED) {
                mainHandler.post { onError("麦克风初始化失败，请检查录音权限") }
                return
            }

            ar.startRecording()
            Timber.tag(TAG).d("AudioRecord 开始录音")

            // 5. 启动录音循环
            scope.launch {
                // 检查是否在模型初始化期间已被 stop
                if (!isRunning) {
                    Timber.tag(TAG).d("录音循环启动前已被 stop，清理资源")
                    ar.stop()
                    ar.release()
                    audioRecord = null
                    mainHandler.post { onFinal("") }
                    return@launch
                }
                recordingLoop(ar, frameSize, onPartial, onFinal, onError)
            }

            Timber.tag(TAG).d("ASR 识别已启动")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "初始化失败")
            mainHandler.post { onError("端侧 ASR 初始化失败: ${e.message}") }
        }
    }

    /**
     * @DESC: 停止录音，触发 Pass 2 整段识别
     * 1. 标记停止（录音循环会退出）
     * 2. Pass 2 在录音循环退出后自动执行
     */
    override fun stop() {
        Timber.tag(TAG).d("stop() 被调用")
        isRunning = false
    }

    /**
     * @DESC: 录音循环
     * 1. 持续从 AudioRecord 读取 PCM 数据
     * 2. ShortArray → FloatArray 转换
     * 3. 送入 OnlineStream（Pass 1 流式识别）
     * 4. 同时缓存到 pendingAudio（供 Pass 2 使用）
     * 5. 每次 decode 后获取中间结果，回调 onPartial
     * 6. isRunning=false 后退出循环
     * 7. 执行 Pass 2：整段送入 OfflineRecognizer，回调 onFinal
     *
     * @Param: ar — AudioRecord 实例
     * @Param: frameSize — 每次读取的字节数（1280 = 40ms）
     * @Param: onPartial — Pass 1 中间结果回调
     * @Param: onFinal — Pass 2 最终结果回调
     * @Param: onError — 错误回调
     */
    private suspend fun recordingLoop(
        ar: AudioRecord,
        frameSize: Int,
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val buf = ShortArray(frameSize / 2)                      // 1280字节 / 2 = 640个Short

        try {
            // Pass 1：流式识别循环
            while (isRunning) {
                val read = ar.read(buf, 0, buf.size)
                if (read <= 0) continue

                // ShortArray → FloatArray（归一化到 -1.0 ~ 1.0）
                val samples = FloatArray(read) { i ->
                    buf[i].toFloat() / 32768.0f                  // 16bit PCM 归一化
                }

                // 送入 OnlineStream
                onlineStream!!.acceptWaveform(samples, SAMPLE_RATE)
                pendingAudio.addAll(samples.toList())            // 缓存供 Pass 2

                // 只有 isReady 时才 decode（累积足够帧后）
                if (onlineRecognizer!!.isReady(onlineStream!!)) {
                    onlineRecognizer!!.decode(onlineStream!!)
                    val result = onlineRecognizer!!.getResult(onlineStream!!)
                    if (result.text.isNotEmpty()) {
                        mainHandler.post { onPartial(result.text) }
                    }
                }
            }

            // 停止录音
            ar.stop()
            ar.release()
            audioRecord = null

            // Pass 2：整段识别
            if (pendingAudio.isNotEmpty()) {
                Timber.tag(TAG).d("开始 Pass 2，音频长度: ${pendingAudio.size} 采样点")
                val offlineStream = offlineRecognizer!!.createStream()
                offlineStream.acceptWaveform(pendingAudio.toFloatArray(), SAMPLE_RATE)
                offlineRecognizer!!.decode(offlineStream)
                val finalResult = offlineRecognizer!!.getResult(offlineStream)
                offlineStream.release()

                Timber.tag(TAG).d("Pass 2 结果: ${finalResult.text}")
                mainHandler.post { onFinal(finalResult.text) }
            } else {
                mainHandler.post { onFinal("") }
            }

            // 重置 OnlineStream
            onlineStream?.let { onlineRecognizer?.reset(it) }

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "录音循环异常")
            ar.release()
            audioRecord = null
            mainHandler.post { onError("端侧 ASR 录音异常: ${e.message}") }
        }
    }

    /**
     * @DESC: 释放所有资源
     * 1. 停止录音
     * 2. 释放 AudioRecord
     * 3. 释放 OnlineRecognizer
     * 4. 释放 OfflineRecognizer
     * 5. 释放 Stream
     */
    fun release() {
        isRunning = false
        audioRecord?.let {
            try { it.stop() } catch (_: Exception) {}
            it.release()
        }
        audioRecord = null
        onlineStream?.let {
            onlineRecognizer?.reset(it)
        }
        onlineRecognizer?.release()
        offlineRecognizer?.release()
        onlineRecognizer = null
        offlineRecognizer = null
        onlineStream = null
        Timber.tag(TAG).d("资源已释放")
    }
}
