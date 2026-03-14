package com.example.nextthingb1.data.asr

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import com.example.nextthingb1.data.preferences.IFlyPreferences
import com.example.nextthingb1.domain.service.ASRService
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import timber.log.Timber
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class IFlyASRService @Inject constructor(
    @Named("ai") private val okHttpClient: OkHttpClient,
    private val iflyPreferences: IFlyPreferences,
    private val gson: Gson
) : ASRService {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var webSocket: WebSocket? = null
    private var audioRecord: AudioRecord? = null
    private var recordJob: Job? = null
    private var isRecording = false

    // 拼接识别结果（按句子序号）
    private val resultMap = mutableMapOf<Int, String>()

    override suspend fun isConfigured(): Boolean = iflyPreferences.isConfigured()

    override fun start(
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (isRecording) return
        resultMap.clear()

        scope.launch {
            val appId     = iflyPreferences.getAppIdOnce()
            val apiKey    = iflyPreferences.getApiKeyOnce()
            val apiSecret = iflyPreferences.getApiSecretOnce()
            val accent    = iflyPreferences.getAccentOnce()

            if (appId.isBlank() || apiKey.isBlank() || apiSecret.isBlank()) {
                mainHandler.post { onError("请先在设置 → 讯飞语音中填写 AppID / APIKey / APISecret") }
                return@launch
            }

            val url = buildAuthUrl(apiKey, apiSecret)
            Timber.tag("ASR").d("连接讯飞 WebSocket (AppID前4位: ${appId.take(4)}...)")

            val listener = object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, response: Response) {
                    Timber.tag("ASR").d("WebSocket 已连接，开始录音")
                    startAudioRecording(ws, appId, accent, onPartial, onFinal, onError)
                }

                override fun onMessage(ws: WebSocket, text: String) {
                    handleMessage(text, onPartial, onFinal, onError)
                }

                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                    val code = response?.code ?: -1
                    Timber.tag("ASR").e(t, "WebSocket 失败 HTTP=$code")
                    isRecording = false
                    val msg = when {
                        code == 401 -> "鉴权失败(401)：请检查 APIKey 和 APISecret 是否正确"
                        code == 403 -> "无权限(403)：AppID 未开通语音听写服务"
                        t.message?.contains("Unable to resolve host") == true -> "无法连接讯飞服务器，请检查网络"
                        else -> "语音识别连接失败 ($code): ${t.message}"
                    }
                    mainHandler.post { onError(msg) }
                }

                override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                    Timber.tag("ASR").d("WebSocket 关闭: code=$code reason=$reason")
                    isRecording = false
                }
            }

            val request = Request.Builder().url(url).build()
            webSocket = okHttpClient.newWebSocket(request, listener)
        }
    }

    override fun stop() {
        isRecording = false
        // recordJob 循环会检测 isRecording，退出后发送最终帧
    }

    // ── 音频录制 ──────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    private fun startAudioRecording(
        ws: WebSocket,
        appId: String,
        accent: String,
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        // 每 40ms 发一帧：40ms × 16000Hz × 2字节 = 1280 字节
        val frameSize = 1280
        val minBuf = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufSize = maxOf(minBuf, frameSize * 4)

        val ar = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, bufSize)
        audioRecord = ar

        if (ar.state != AudioRecord.STATE_INITIALIZED) {
            mainHandler.post { onError("麦克风初始化失败，请检查录音权限") }
            return
        }

        ar.startRecording()
        isRecording = true
        Timber.tag("ASR").d("AudioRecord 开始录音")

        recordJob = scope.launch {
            val buf = ByteArray(frameSize)
            var frameIndex = 0

            while (isActive && isRecording) {
                val read = ar.read(buf, 0, frameSize)
                if (read <= 0) continue

                val audioB64 = Base64.getEncoder().encodeToString(buf.copyOf(read))
                val status = if (frameIndex == 0) 0 else 1  // 0=首帧, 1=中间帧

                val msg = if (frameIndex == 0) {
                    // 首帧：携带 common + business
                    gson.toJson(mapOf(
                        "common"   to mapOf("app_id" to appId),
                        "business" to mapOf(
                            "language" to "zh_cn",
                            "domain"   to "iat",
                            "accent"   to accent,
                            "vad_eos"  to 3000,
                            "dwa"      to "wpgs"
                        ),
                        "data" to mapOf(
                            "status"   to status,
                            "format"   to "audio/L16;rate=16000",
                            "encoding" to "raw",
                            "audio"    to audioB64
                        )
                    ))
                } else {
                    gson.toJson(mapOf(
                        "data" to mapOf(
                            "status"   to status,
                            "format"   to "audio/L16;rate=16000",
                            "encoding" to "raw",
                            "audio"    to audioB64
                        )
                    ))
                }
                ws.send(msg)
                frameIndex++
            }

            // 发送最终帧
            ws.send(gson.toJson(mapOf(
                "data" to mapOf(
                    "status"   to 2,
                    "format"   to "audio/L16;rate=16000",
                    "encoding" to "raw",
                    "audio"    to ""
                )
            )))
            Timber.tag("ASR").d("已发送最终帧，等待结果")

            ar.stop()
            ar.release()
            audioRecord = null
        }
    }

    // ── 结果解析 ──────────────────────────────────────────────

    private fun handleMessage(
        text: String,
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val obj = gson.fromJson(text, JsonObject::class.java)
            val code = obj.get("code")?.asInt ?: -1

            if (code != 0) {
                val msg = obj.get("message")?.asString ?: "识别失败"
                Timber.tag("ASR").e("讯飞返回错误 code=$code msg=$msg")
                isRecording = false
                val errStr = when (code) {
                    10105 -> "AppID 无效，请检查讯飞配置"
                    10106 -> "AppID 未授权该功能"
                    10107 -> "APIKey 无效"
                    10110 -> "引擎繁忙，请稍后重试"
                    else  -> "识别错误 ($code): $msg"
                }
                mainHandler.post { onError(errStr) }
                webSocket?.close(1000, "error")
                return
            }

            val data = obj.getAsJsonObject("data") ?: return
            val status = data.get("status")?.asInt ?: 0
            val result = data.getAsJsonObject("result") ?: return

            // 提取本句文字
            val sb = StringBuilder()
            result.getAsJsonArray("ws")?.forEach { wsElem ->
                wsElem.asJsonObject.getAsJsonArray("cw")?.forEach { cwElem ->
                    sb.append(cwElem.asJsonObject.get("w")?.asString ?: "")
                }
            }
            val sn = result.get("sn")?.asInt ?: 0
            val isFinalSentence = result.get("ls")?.asBoolean ?: (status == 2)

            // 用 rg (替换规则) 处理词语粒度追加
            val rg = result.getAsJsonArray("rg")
            if (rg != null && rg.size() >= 2) {
                // rg=[from, to] 表示替换 sn from~to 的结果
                val rgFrom = rg[0].asInt
                val rgTo   = rg[1].asInt
                for (i in rgFrom..rgTo) resultMap.remove(i)
            }
            resultMap[sn] = sb.toString()

            val fullText = resultMap.entries.sortedBy { it.key }
                .joinToString("") { it.value }.trim()

            Timber.tag("ASR").d("识别中 sn=$sn status=$status text=$fullText")

            if (status == 2 || isFinalSentence) {
                // 最终结果
                mainHandler.post { onFinal(fullText) }
                webSocket?.close(1000, "done")
            } else {
                mainHandler.post { onPartial(fullText) }
            }
        } catch (e: Exception) {
            Timber.tag("ASR").e(e, "解析讯飞响应异常")
        }
    }

    // ── 鉴权 URL 构建 ──────────────────────────────────────────

    private fun buildAuthUrl(apiKey: String, apiSecret: String): String {
        val host = "iat-api.xfyun.cn"
        val path = "/v2/iat"

        val sdf = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("GMT")
        val date = sdf.format(Date())

        val signOrigin = "host: $host\ndate: $date\nGET $path HTTP/1.1"
        Timber.tag("ASR").d("签名原始串:\n$signOrigin")

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(apiSecret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val signature = Base64.getEncoder().encodeToString(
            mac.doFinal(signOrigin.toByteArray(Charsets.UTF_8))
        )
        Timber.tag("ASR").d("signature: $signature")

        val authOrigin = "api_key=\"$apiKey\", algorithm=\"hmac-sha256\", headers=\"host date request-line\", signature=\"$signature\""
        val authorization = Base64.getEncoder().encodeToString(
            authOrigin.toByteArray(Charsets.UTF_8)
        )

        // ⚠️ 必须 URL 编码：Base64 的 +、/、= 是 URL 特殊字符
        val authEnc = URLEncoder.encode(authorization, "UTF-8")
        val dateEnc = URLEncoder.encode(date, "UTF-8")

        val url = "wss://$host$path?authorization=$authEnc&date=$dateEnc&host=$host"
        Timber.tag("ASR").d("鉴权 URL 构建完成 (apiKey 前8位: ${apiKey.take(8)}...)")
        return url
    }
}
