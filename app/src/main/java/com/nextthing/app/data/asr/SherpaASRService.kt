package com.nextthing.app.data.asr

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Debug
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineSenseVoiceModelConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineParaformerModelConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineStream
import com.k2fsa.sherpa.onnx.QnnConfig
import com.nextthing.app.BuildConfig
import com.nextthing.app.domain.service.ASRService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
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
        private const val BENCHMARK_TAG = "ASR-Benchmark"
        private const val SAMPLE_RATE = 16000                    // 采样率 16kHz
        private const val SHERPA_MODEL_DIR = "models/sherpa-onnx"   // assets 下的模型目录
        private const val CPU_SENSE_VOICE_MODEL = "model.int8.onnx"
        private const val CPU_SENSE_VOICE_TOKENS = "tokens.txt"
        private const val QNN_SENSE_VOICE_DIR = "$SHERPA_MODEL_DIR/sense-voice-qnn-10s"
        private const val QNN_CONTEXT_ASSET = "$QNN_SENSE_VOICE_DIR/model-sm8550-v2.bin"
        private const val QNN_CONTEXT_SIZE_BYTES = 261_674_328L
        private const val QNN_CONTEXT_FILE = "model-sm8550-v2.bin"
        private const val QNN_TOKENS_SIZE_BYTES = 315_894L
        private const val QNN_BACKEND_LIBRARY = "libQnnHtp.so"
        private const val QNN_SYSTEM_LIBRARY = "libQnnSystem.so"
        private const val QNN_FIXED_AUDIO_SECONDS = 10
        private const val RESOURCE_MISSING_MESSAGE = "端侧语音资源未安装，请按 README 放置 ASR Runtime 资源包"

        private val COMMON_ASR_ASSETS = listOf(
            "$SHERPA_MODEL_DIR/paraformer/encoder.int8.onnx",
            "$SHERPA_MODEL_DIR/paraformer/decoder.int8.onnx",
            "$SHERPA_MODEL_DIR/paraformer/tokens.txt",
        )
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())    // 回调切主线程
    private val pass2Backend = BuildConfig.ASR_BACKEND.lowercase()
    private val useQnnPass2 = pass2Backend == "npu"
    private val pass2BackendLabel = if (useQnnPass2) "QNN_HTP" else "CPU_ONNX"

    private val _isReady = MutableStateFlow(false)               // 模型是否加载完成
    override val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    override val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var onlineRecognizer: OnlineRecognizer? = null       // Pass 1：流式识别器
    private var onlineStream: OnlineStream? = null               // 流式识别的 Stream
    private var offlineRecognizer: OfflineRecognizer? = null     // Pass 2：整段识别器
    private var pendingAudio = mutableListOf<Float>()            // 缓存音频数据，供 Pass 2 使用

    private var audioRecord: AudioRecord? = null                 // Android 录音器
    @Volatile
    private var isRunning = false                                // 是否正在识别
    @Volatile
    private var isRecognitionSessionActive = false
    @Volatile
    private var isWarmingUp = false
    @Volatile
    private var releaseRequested = false
    private val warmUpLock = Any()

    /**
     * @DESC: 预热模型（在后台线程加载模型，避免首次使用时卡顿）
     * 1. 初始化 OnlineRecognizer
     * 2. 初始化 OfflineRecognizer
     * 3. 首次长按时 start() 会跳过已初始化的模型，直接开始录音
     */
    override fun warmUp() {
        synchronized(warmUpLock) {
            releaseRequested = false
        }
        val missingAssets = findMissingRequiredAssets()
        if (missingAssets.isNotEmpty()) {
            _isReady.value = false
            _errorMessage.value = RESOURCE_MISSING_MESSAGE
            Timber.tag(TAG).w("ASR 资源缺失: ${missingAssets.joinToString()}")
            return
        }

        if (onlineRecognizer != null && offlineRecognizer != null) {
            Timber.tag(TAG).d("模型已初始化，跳过预热")
            _isReady.value = true
            _errorMessage.value = null
            return
        }
        synchronized(warmUpLock) {
            if (isWarmingUp) {
                Timber.tag(TAG).d("模型正在预热，合并本次重复请求")
                return
            }
            isWarmingUp = true
        }
        Timber.tag(TAG).d("warmUp() 被调用，开始预热，Pass 2 backend=$pass2BackendLabel")
        scope.launch {
            var initializationSucceeded = false
            var initializationError: Throwable? = null
            try {
                logMemorySnapshot("warmup_before")
                if (onlineRecognizer == null) {
                    val wallStartedNs = SystemClock.elapsedRealtimeNanos()
                    val processCpuStartedMs = Process.getElapsedCpuTime()
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
                    logInitializationMetrics(
                        stage = "online_ready",
                        wallStartedNs = wallStartedNs,
                        processCpuStartedMs = processCpuStartedMs,
                    )
                    logMemorySnapshot("online_ready")
                }
                if (offlineRecognizer == null) {
                    val wallStartedNs = SystemClock.elapsedRealtimeNanos()
                    val processCpuStartedMs = Process.getElapsedCpuTime()
                    Timber.tag(TAG).d("预热：开始初始化 $pass2BackendLabel OfflineRecognizer...")
                    offlineRecognizer = createOfflineRecognizer()
                    logInitializationMetrics(
                        stage = "offline_ready",
                        wallStartedNs = wallStartedNs,
                        processCpuStartedMs = processCpuStartedMs,
                    )
                    logMemorySnapshot("offline_ready")
                }
                initializationSucceeded = true
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                initializationError = t
            }

            synchronized(warmUpLock) {
                isWarmingUp = false
                when {
                    releaseRequested -> {
                        releaseLoadedResources()
                        Timber.tag(TAG).d("预热结束时页面已退出，立即释放模型")
                    }
                    initializationSucceeded -> {
                        _isReady.value = true
                        _errorMessage.value = null
                        Timber.tag(TAG).d("预热完成，模型就绪，Pass 2 backend=$pass2BackendLabel")
                    }
                    else -> {
                        _isReady.value = false
                        _errorMessage.value = initializationError?.let(::toUserFacingInitError)
                        Timber.tag(TAG).e(initializationError, "预热失败")
                    }
                }
            }
        }
    }

    /**
     * @DESC: 检查是否已配置（模型是否可加载）
     * 1. 模型打包在 assets 中，始终可用
     * @return true
     */
    override suspend fun isConfigured(): Boolean = findMissingRequiredAssets().isEmpty()

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
        onError: (String) -> Unit,
        onAudioCaptured: (samples: ShortArray, sampleRate: Int) -> Unit
    ) {
        try {
            val missingAssets = findMissingRequiredAssets()
            if (missingAssets.isNotEmpty()) {
                _isReady.value = false
                _errorMessage.value = RESOURCE_MISSING_MESSAGE
                Timber.tag(TAG).w("ASR 资源缺失，无法启动: ${missingAssets.joinToString()}")
                mainHandler.post { onError(RESOURCE_MISSING_MESSAGE) }
                return
            }
            if (!_isReady.value) {
                warmUp()
                mainHandler.post { onError("端侧语音模型正在初始化，请稍后再试") }
                return
            }

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
                offlineRecognizer = createOfflineRecognizer()
                Timber.tag(TAG).d("$pass2BackendLabel OfflineRecognizer 初始化完成")
            }

            // 3. 创建新的 Stream
            onlineStream = onlineRecognizer!!.createStream()
            pendingAudio.clear()
            isRunning = true
            isRecognitionSessionActive = true

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
                    completeRecognitionSession()
                    mainHandler.post { onFinal("") }
                    return@launch
                }
                recordingLoop(ar, frameSize, onPartial, onFinal, onError, onAudioCaptured)
            }

            Timber.tag(TAG).d("ASR 识别已启动")
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            val message = toUserFacingInitError(t)
            _isReady.value = false
            _errorMessage.value = message
            Timber.tag(TAG).e(t, "初始化失败")
            mainHandler.post { onError(message) }
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
        onError: (String) -> Unit,
        onAudioCaptured: (samples: ShortArray, sampleRate: Int) -> Unit
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

            if (releaseRequested) return

            // Pass 2：整段识别
            if (pendingAudio.isNotEmpty()) {
                val audioDurationMs = pendingAudio.size * 1000L / SAMPLE_RATE
                Timber.tag(TAG).d("开始 Pass 2 $pass2BackendLabel，音频长度: ${audioDurationMs}ms")
                if (useQnnPass2 && audioDurationMs > QNN_FIXED_AUDIO_SECONDS * 1000L) {
                    Timber.tag(TAG).w(
                        "Pass 2 QNN 模型最长支持 ${QNN_FIXED_AUDIO_SECONDS}s，超出部分会被截断"
                    )
                }
                val offlineStream = offlineRecognizer!!.createStream()
                offlineStream.acceptWaveform(pendingAudio.toFloatArray(), SAMPLE_RATE)
                logMemorySnapshot("pass2_before")
                val decodeStartedNs = SystemClock.elapsedRealtimeNanos()
                val processCpuStartedMs = Process.getElapsedCpuTime()
                val threadCpuStartedNs = Debug.threadCpuTimeNanos()
                offlineRecognizer!!.decode(offlineStream)
                val decodeElapsedMs =
                    (SystemClock.elapsedRealtimeNanos() - decodeStartedNs) / 1_000_000.0
                val processCpuElapsedMs = Process.getElapsedCpuTime() - processCpuStartedMs
                val threadCpuElapsedMs =
                    (Debug.threadCpuTimeNanos() - threadCpuStartedNs) / 1_000_000.0
                val finalResult = offlineRecognizer!!.getResult(offlineStream)
                offlineStream.release()

                Timber.tag(TAG).i(
                    "Pass 2 backend requested=%s actual=%s, decode=%.2fms, audio=%dms",
                    pass2BackendLabel,
                    pass2BackendLabel,
                    decodeElapsedMs,
                    audioDurationMs
                )
                Timber.tag(BENCHMARK_TAG).w(
                    "pass2 backend=%s wallMs=%.2f processCpuMs=%d threadCpuMs=%.2f audioMs=%d",
                    pass2BackendLabel,
                    decodeElapsedMs,
                    processCpuElapsedMs,
                    threadCpuElapsedMs,
                    audioDurationMs,
                )
                logMemorySnapshot("pass2_after")
                Timber.tag(TAG).d("Pass 2 结果: ${finalResult.text}")
                val capturedSamples = pendingAudio.toShortPcmArray()
                mainHandler.post {
                    onAudioCaptured(capturedSamples, SAMPLE_RATE)
                    onFinal(finalResult.text)
                }
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
        } finally {
            completeRecognitionSession()
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
    override fun release() {
        synchronized(warmUpLock) {
            releaseRequested = true
            _isReady.value = false
            isRunning = false
            if (isWarmingUp || isRecognitionSessionActive) {
                Timber.tag(TAG).d("ASR 正在初始化或识别，结束后再释放")
                return
            }
            releaseLoadedResources()
        }
    }

    private fun releaseLoadedResources() {
        logMemorySnapshot("release_before")
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
        pendingAudio.clear()
        _isReady.value = false
        logMemorySnapshot("release_after")
        Timber.tag(TAG).d("ASR 资源已释放")
    }

    private fun completeRecognitionSession() {
        synchronized(warmUpLock) {
            isRecognitionSessionActive = false
            if (releaseRequested && !isWarmingUp) {
                releaseLoadedResources()
            }
        }
    }

    private fun findMissingRequiredAssets(): List<String> {
        val backendAssets = if (useQnnPass2) {
            listOf("$QNN_SENSE_VOICE_DIR/tokens.txt", QNN_CONTEXT_ASSET)
        } else {
            listOf(CPU_SENSE_VOICE_MODEL, CPU_SENSE_VOICE_TOKENS)
        }
        val missingAssets = (COMMON_ASR_ASSETS + backendAssets).filter { path ->
            try {
                context.assets.open(path).use { }
                false
            } catch (_: Exception) {
                true
            }
        }
        val nativeLibraryDir = File(context.applicationInfo.nativeLibraryDir)
        val requiredNativeLibraries = if (useQnnPass2) {
            listOf(QNN_BACKEND_LIBRARY, QNN_SYSTEM_LIBRARY)
        } else {
            emptyList()
        }
        val missingNativeLibraries = requiredNativeLibraries.filter { library ->
            !File(nativeLibraryDir, library).isFile
        }
        return missingAssets + missingNativeLibraries
    }

    private fun createOfflineRecognizer(): OfflineRecognizer {
        return if (useQnnPass2) createQnnOfflineRecognizer() else createCpuOfflineRecognizer()
    }

    private fun createCpuOfflineRecognizer(): OfflineRecognizer {
        val config = OfflineRecognizerConfig(
            modelConfig = OfflineModelConfig(
                senseVoice = OfflineSenseVoiceModelConfig(
                    model = CPU_SENSE_VOICE_MODEL,
                    language = "zh",
                    useInverseTextNormalization = true,
                ),
                tokens = CPU_SENSE_VOICE_TOKENS,
                numThreads = 4,
                debug = true,
            )
        )
        Timber.tag(TAG).i("Initializing Pass 2 backend=CPU_ONNX")
        return OfflineRecognizer(assetManager = context.assets, config = config)
    }

    private fun createQnnOfflineRecognizer(): OfflineRecognizer {
        val nativeLibraryDir = File(context.applicationInfo.nativeLibraryDir)
        val backendFile = File(nativeLibraryDir, QNN_BACKEND_LIBRARY)
        val systemFile = File(nativeLibraryDir, QNN_SYSTEM_LIBRARY)
        val runtimeDir = File(context.filesDir, "asr/qnn-sensevoice-10s").apply { mkdirs() }
        val tokensFile = File(runtimeDir, "tokens.txt")
        val contextFile = File(runtimeDir, QNN_CONTEXT_FILE)
        copyAssetAtomically(
            assetPath = "$QNN_SENSE_VOICE_DIR/tokens.txt",
            target = tokensFile,
            expectedSize = QNN_TOKENS_SIZE_BYTES,
        )
        copyAssetAtomically(
            assetPath = QNN_CONTEXT_ASSET,
            target = contextFile,
            expectedSize = QNN_CONTEXT_SIZE_BYTES,
        )

        OfflineRecognizer.prependAdspLibraryPath(nativeLibraryDir.absolutePath)
        val config = OfflineRecognizerConfig(
            modelConfig = OfflineModelConfig(
                provider = "qnn",
                senseVoice = OfflineSenseVoiceModelConfig(
                    language = "zh",
                    useInverseTextNormalization = true,
                    qnnConfig = QnnConfig(
                        backendLib = backendFile.absolutePath,
                        systemLib = systemFile.absolutePath,
                        contextBinary = contextFile.absolutePath,
                    ),
                ),
                tokens = tokensFile.absolutePath,
                numThreads = 1,
                debug = true,
            )
        )
        Timber.tag(TAG).i(
            "Initializing Pass 2 backend=QNN_HTP mode=LOAD_CONTEXT context=%s",
            contextFile.absolutePath,
        )
        return OfflineRecognizer(config = config)
    }

    private fun copyAssetAtomically(assetPath: String, target: File, expectedSize: Long) {
        if (target.isFile && target.length() == expectedSize) return

        val temporary = File(target.parentFile, "${target.name}.tmp")
        temporary.delete()
        context.assets.open(assetPath).use { input ->
            temporary.outputStream().buffered().use(input::copyTo)
        }
        check(temporary.length() == expectedSize) {
            "ASR resource size mismatch: $assetPath, expected=$expectedSize, actual=${temporary.length()}"
        }
        target.delete()
        check(temporary.renameTo(target)) { "Unable to install ASR resource: ${target.absolutePath}" }
    }

    private fun logInitializationMetrics(
        stage: String,
        wallStartedNs: Long,
        processCpuStartedMs: Long,
    ) {
        val wallMs = (SystemClock.elapsedRealtimeNanos() - wallStartedNs) / 1_000_000.0
        val processCpuMs = Process.getElapsedCpuTime() - processCpuStartedMs
        Timber.tag(BENCHMARK_TAG).w(
            "init stage=%s backend=%s wallMs=%.2f processCpuMs=%d",
            stage,
            pass2BackendLabel,
            wallMs,
            processCpuMs,
        )
    }

    private fun logMemorySnapshot(stage: String) {
        val memoryInfo = Debug.MemoryInfo()
        Debug.getMemoryInfo(memoryInfo)
        val nativeHeapMb = Debug.getNativeHeapAllocatedSize().toDouble() / (1024.0 * 1024.0)
        val pssMb = memoryInfo.totalPss.toDouble() / 1024.0
        val rssMb = readVmRssKb().toDouble() / 1024.0
        Timber.tag(BENCHMARK_TAG).w(
            "memory stage=%s backend=%s nativeHeapMb=%.1f pssMb=%.1f rssMb=%.1f",
            stage,
            pass2BackendLabel,
            nativeHeapMb,
            pssMb,
            rssMb,
        )
    }

    private fun readVmRssKb(): Long {
        return runCatching {
            File("/proc/self/status").useLines { lines ->
                lines.firstOrNull { it.startsWith("VmRSS:") }
                    ?.substringAfter("VmRSS:")
                    ?.trim()
                    ?.substringBefore(' ')
                    ?.toLongOrNull()
                    ?: 0L
            }
        }.getOrDefault(0L)
    }

    private fun toUserFacingInitError(t: Throwable): String {
        return if (t is UnsatisfiedLinkError) {
            "端侧语音 Runtime 未安装，请按 README 放置 app/src/main/jniLibs 资源"
        } else {
            "端侧 ASR 初始化失败: ${t.message ?: "请检查模型资源包"}"
        }
    }

    private fun List<Float>.toShortPcmArray(): ShortArray {
        return ShortArray(size) { index ->
            (this[index].coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
        }
    }
}
