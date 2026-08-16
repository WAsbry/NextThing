package com.nextthing.app.data.service

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.nextthing.app.BuildConfig
import com.nextthing.app.data.remote.api.AIChatApi
import com.nextthing.app.data.remote.dto.AIRunCreateRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.EOFException
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class BackendAiRunClient @Inject constructor(
    private val backendApi: AIChatApi,
    @Named("ai-backend") backendHttpClient: OkHttpClient,
    private val gson: Gson
) {

    private val streamClient = backendHttpClient.newBuilder()
        .readTimeout(STREAM_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    suspend fun complete(message: String, sessionId: String?): String = withContext(Dispatchers.IO) {
        val run = backendApi.createRun(AIRunCreateRequest(sessionId, message))
        val collector = AiRunEventCollector(gson, MAX_RESPONSE_CHARS)
        var reconnectCount = 0

        while (!collector.isTerminal) {
            currentCoroutineContext().ensureActive()
            try {
                streamOnce(run.runId, collector)
                if (!collector.isTerminal) {
                    throw EOFException("AI 流在完成前中断")
                }
            } catch (error: IOException) {
                currentCoroutineContext().ensureActive()
                if (reconnectCount >= MAX_RECONNECTS) throw error
                delay(RECONNECT_DELAYS_MS[reconnectCount])
                reconnectCount++
            }
        }

        collector.result()
    }

    private suspend fun streamOnce(runId: String, collector: AiRunEventCollector) {
        val request = Request.Builder()
            .url("${BuildConfig.BACKEND_BASE_URL}api/ai/runs/$runId/stream")
            .header("Accept", "text/event-stream")
            .apply {
                if (collector.lastEventId > 0) {
                    header("Last-Event-ID", collector.lastEventId.toString())
                }
            }
            .build()
        val call = streamClient.newCall(request)
        val cancellation = currentCoroutineContext().job.invokeOnCompletion { cause ->
            if (cause != null) call.cancel()
        }

        try {
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("服务端 AI 流连接失败：HTTP ${response.code}")
                }
                val body = response.body ?: throw IOException("服务端 AI 流为空")
                val parser = SseFrameParser()
                val source = body.source()
                while (!source.exhausted()) {
                    currentCoroutineContext().ensureActive()
                    parser.accept(source.readUtf8Line() ?: break)?.let(collector::accept)
                    if (collector.isTerminal) return
                }
                parser.finish()?.let(collector::accept)
            }
        } finally {
            cancellation.dispose()
        }
    }

    private companion object {
        const val STREAM_READ_TIMEOUT_SECONDS = 30L
        const val MAX_RECONNECTS = 3
        const val MAX_RESPONSE_CHARS = 512 * 1024
        val RECONNECT_DELAYS_MS = longArrayOf(250, 500, 1_000)
    }
}

internal data class SseFrame(
    val id: Long?,
    val event: String?,
    val data: String
)

internal class SseFrameParser {
    private var id: Long? = null
    private var event: String? = null
    private val data = StringBuilder()

    fun accept(line: String): SseFrame? {
        if (line.isEmpty()) return emit()
        if (line.startsWith(':')) return null

        val separator = line.indexOf(':')
        val field = if (separator >= 0) line.substring(0, separator) else line
        val value = if (separator >= 0) {
            line.substring(separator + 1).removePrefix(" ")
        } else {
            ""
        }
        when (field) {
            "id" -> id = value.toLongOrNull()
            "event" -> event = value
            "data" -> {
                if (data.isNotEmpty()) data.append('\n')
                data.append(value)
            }
        }
        return null
    }

    fun finish(): SseFrame? = emit()

    private fun emit(): SseFrame? {
        if (id == null && event == null && data.isEmpty()) return null
        return SseFrame(id, event, data.toString()).also {
            id = null
            event = null
            data.clear()
        }
    }
}

internal class AiRunEventCollector(
    private val gson: Gson,
    private val maxResponseChars: Int
) {
    private val text = StringBuilder()
    var lastEventId: Long = 0
        private set
    var isTerminal: Boolean = false
        private set
    private var failure: String? = null

    fun accept(frame: SseFrame) {
        val eventId = frame.id ?: return
        if (eventId <= lastEventId) return

        val root = gson.fromJson(frame.data, JsonObject::class.java)
        val sequence = root.get("sequence")?.asLong ?: eventId
        if (sequence != eventId) {
            throw IOException("AI 事件序号不一致")
        }
        val type = root.get("type")?.asString ?: frame.event.orEmpty()
        val payload = root.getAsJsonObject("payload") ?: JsonObject()

        when (type) {
            "TEXT_DELTA" -> {
                val delta = payload.get("text")?.asString.orEmpty()
                if (text.length + delta.length > maxResponseChars) {
                    throw IOException("AI 返回内容超过安全上限")
                }
                text.append(delta)
            }
            "RUN_STATUS_CHANGED" -> when (payload.get("status")?.asString) {
                "COMPLETED" -> isTerminal = true
                "FAILED", "CANCELLED" -> {
                    failure = payload.get("errorCode")?.asString ?: "RUN_FAILED"
                    isTerminal = true
                }
            }
        }
        lastEventId = eventId
    }

    fun result(): String {
        failure?.let { throw IllegalStateException("服务端 AI 执行失败：$it") }
        return text.toString().trim().ifBlank {
            throw IllegalStateException("AI 返回内容为空")
        }
    }
}
