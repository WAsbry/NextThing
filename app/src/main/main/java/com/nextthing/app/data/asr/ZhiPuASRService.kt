package com.nextthing.app.data.asr

import android.annotation.SuppressLint
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import com.nextthing.app.data.preferences.ASRPreferences
import com.nextthing.app.domain.service.ASRService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.buffer
import okio.sink
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ZhiPuASRService @Inject constructor(
    @Named("ai") private val okHttpClient: OkHttpClient,
    private val asrPreferences: ASRPreferences,
    @ApplicationContext private val context: Context
) : ASRService {

    companion object {
        private const val API_URL = "https://open.bigmodel.cn/api/paas/v4/audio/transcriptions"
        private const val MODEL = "glm-asr-2512"
        private const val SAMPLE_RATE = 16000
        private const val CHANNELS = 1
        private const val BITS_PER_SAMPLE = 16
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var audioRecord: AudioRecord? = null
    private var recordJob: Job? = null
    @Volatile
    private var isRecording = false
    private var pcmBuffer = ByteArrayOutputStream()
    private var pendingCallbacks: Triple<(String) -> Unit, (String) -> Unit, (String) -> Unit>? = null

    override suspend fun isConfigured(): Boolean = asrPreferences.isZhiPuConfigured()

    @SuppressLint("MissingPermission")
    override fun start(
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (isRecording) return
        pendingCallbacks = Triple(onPartial, onFinal, onError)
        pcmBuffer = ByteArrayOutputStream()

        scope.launch {
            val apiKey = asrPreferences.getZhipuApiKeyOnce()
            if (apiKey.isBlank()) {
                mainHandler.post { onError("请先在设置 → 语音识别中填写智谱 API Key") }
                return@launch
            }

            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val frameSize = 1280 // 40ms × 16000Hz × 2字节
            val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, channelConfig, audioFormat)
            val bufSize = maxOf(minBuf, frameSize * 4)

            val ar = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                channelConfig,
                audioFormat,
                bufSize
            )
            audioRecord = ar

            if (ar.state != AudioRecord.STATE_INITIALIZED) {
                mainHandler.post { onError("麦克风初始化失败，请检查录音权限") }
                return@launch
            }

            ar.startRecording()
            isRecording = true
            Timber.tag("ASR-ZhiPu").d("开始录音")

            recordJob = scope.launch {
                val buf = ByteArray(frameSize)
                while (isActive && isRecording) {
                    val read = ar.read(buf, 0, frameSize)
                    if (read > 0) {
                        pcmBuffer.write(buf, 0, read)
                    }
                }

                ar.stop()
                ar.release()
                audioRecord = null

                Timber.tag("ASR-ZhiPu").d("录音结束，PCM 大小: ${pcmBuffer.size()} bytes")
                uploadAndTranscribe(apiKey, onPartial, onFinal, onError)
            }
        }
    }

    override fun stop() {
        isRecording = false
    }

    private suspend fun uploadAndTranscribe(
        apiKey: String,
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val pcmData = pcmBuffer.toByteArray()
        if (pcmData.isEmpty()) {
            mainHandler.post { onError("未录制到音频数据") }
            return
        }

        var wavFile: File? = null
        try {
            // PCM → WAV
            wavFile = File(context.cacheDir, "zhipu_asr_${System.currentTimeMillis()}.wav")
            writeWavFile(wavFile, pcmData)
            Timber.tag("ASR-ZhiPu").d("WAV 文件大小: ${wavFile.length()} bytes")

            // 构建 multipart 请求
            val fileBody = wavFile.asRequestBody("audio/wav".toMediaType())
            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("model", MODEL)
                .addFormDataPart("file", "audio.wav", fileBody)
                .addFormDataPart("response_format", "json")
                .build()

            val request = Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .post(multipart)
                .build()

            Timber.tag("ASR-ZhiPu").d("上传音频到智谱 ASR...")

            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                Timber.tag("ASR-ZhiPu").e("HTTP ${response.code}: $errorBody")
                val msg = when (response.code) {
                    401 -> "智谱 API Key 无效（401），请检查配置"
                    403 -> "无权限（403），请确认 API Key 已开通 ASR 服务"
                    429 -> "请求过于频繁（429），请稍后重试"
                    else -> "语音识别失败 (${response.code}): $errorBody"
                }
                mainHandler.post { onError(msg) }
                return
            }

            // 解析 JSON 响应：{"text": "识别结果"}
            val responseBody = response.body?.string() ?: ""
            Timber.tag("ASR-ZhiPu").d("响应: $responseBody")

            val json = com.google.gson.JsonParser.parseString(responseBody).asJsonObject
            val result = json.get("text")?.asString?.trim() ?: ""

            if (result.isNotEmpty()) {
                Timber.tag("ASR-ZhiPu").d("识别完成: $result")
                mainHandler.post { onFinal(result) }
            } else {
                Timber.tag("ASR-ZhiPu").w("识别结果为空，原始响应: $responseBody")
                mainHandler.post { onError("未能识别到有效内容，请重试") }
            }

        } catch (e: Exception) {
            Timber.tag("ASR-ZhiPu").e(e, "上传/识别异常")
            val msg = when {
                e.message?.contains("Unable to resolve host") == true -> "无法连接智谱服务器，请检查网络"
                e is java.net.SocketTimeoutException -> "请求超时，请重试"
                else -> "语音识别异常: ${e.message}"
            }
            mainHandler.post { onError(msg) }
        } finally {
            wavFile?.delete()
        }
    }

    private fun writeWavFile(file: File, pcmData: ByteArray) {
        val dataSize = pcmData.size
        val byteRate = SAMPLE_RATE * CHANNELS * BITS_PER_SAMPLE / 8
        val blockAlign = CHANNELS * BITS_PER_SAMPLE / 8

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        // RIFF header
        header.put(byteArrayOf('R'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 'F'.code.toByte()))
        header.putInt(36 + dataSize) // ChunkSize
        header.put(byteArrayOf('W'.code.toByte(), 'A'.code.toByte(), 'V'.code.toByte(), 'E'.code.toByte()))
        // fmt subchunk
        header.put(byteArrayOf('f'.code.toByte(), 'm'.code.toByte(), 't'.code.toByte(), ' '.code.toByte()))
        header.putInt(16) // Subchunk1Size
        header.putShort(1) // AudioFormat = PCM
        header.putShort(CHANNELS.toShort())
        header.putInt(SAMPLE_RATE)
        header.putInt(byteRate)
        header.putShort(blockAlign.toShort())
        header.putShort(BITS_PER_SAMPLE.toShort())
        // data subchunk
        header.put(byteArrayOf('d'.code.toByte(), 'a'.code.toByte(), 't'.code.toByte(), 'a'.code.toByte()))
        header.putInt(dataSize)

        file.sink().buffer().use { sink ->
            sink.write(header.array())
            sink.write(pcmData)
        }
    }
}
