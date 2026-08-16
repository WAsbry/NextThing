package com.nextthing.app.data.service

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.nextthing.app.data.preferences.AIPreferences
import com.nextthing.app.data.preferences.AIProvider
import com.nextthing.app.data.preferences.TokenManager
import com.nextthing.app.data.remote.api.AIChatApi
import com.nextthing.app.data.remote.dto.AIChatRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

enum class AIRouteMode {
    ExternalProvider,
    BackendFallback,
    Unavailable
}

data class AIRouteStatus(
    val mode: AIRouteMode,
    val provider: AIProvider = AIProvider.DEEPSEEK,
    val model: String = AIProvider.DEEPSEEK.defaultModel
) {
    val userLabel: String
        get() = when (mode) {
            AIRouteMode.ExternalProvider -> "${provider.displayName} · $model"
            AIRouteMode.BackendFallback -> "服务端 AI"
            AIRouteMode.Unavailable -> "AI 未配置"
        }
}

@Singleton
class AICompletionClient @Inject constructor(
    private val aiPreferences: AIPreferences,
    private val tokenManager: TokenManager,
    private val backendApi: AIChatApi,
    private val backendAiRunClient: BackendAiRunClient,
    @Named("ai") private val aiHttpClient: OkHttpClient,
    private val gson: Gson
) {

    private companion object {
        const val MAX_MESSAGE_CHARS = 16_000
        const val MAX_EXTERNAL_RESPONSE_BYTES = 512 * 1024
        const val MAX_SESSION_ID_CHARS = 64
        val SESSION_ID_PATTERN = Regex("[A-Za-z0-9][A-Za-z0-9._:-]{0,63}")
    }

    suspend fun routeStatus(): AIRouteStatus {
        val provider = aiPreferences.getProviderOnce()
        val model = provider.resolveModel(aiPreferences.getModelOnce())
        val apiKey = aiPreferences.getApiKeyOnce().trim()
        if (apiKey.isNotBlank()) {
            return AIRouteStatus(AIRouteMode.ExternalProvider, provider, model)
        }
        val userId = tokenManager.serverUserId.first()
        return if (userId != null) {
            AIRouteStatus(AIRouteMode.BackendFallback, provider, model)
        } else {
            AIRouteStatus(AIRouteMode.Unavailable, provider, model)
        }
    }

    suspend fun complete(
        message: String,
        sessionId: String? = null,
        textOnly: Boolean = false
    ): Result<String> {
        if (message.isBlank()) {
            return Result.failure(IllegalArgumentException("AI 请求内容不能为空"))
        }
        if (message.length > MAX_MESSAGE_CHARS) {
            return Result.failure(
                IllegalArgumentException("AI 请求不能超过 $MAX_MESSAGE_CHARS 个字符")
            )
        }
        if (sessionId != null &&
            (sessionId.length > MAX_SESSION_ID_CHARS || !SESSION_ID_PATTERN.matches(sessionId))
        ) {
            return Result.failure(
                IllegalArgumentException("AI sessionId 格式不合法")
            )
        }
        if (textOnly && sessionId != null) {
            return Result.failure(
                IllegalArgumentException("textOnly 请求不能指定 sessionId")
            )
        }
        val apiKey = aiPreferences.getApiKeyOnce().trim()
        return if (apiKey.isNotBlank()) {
            completeWithUserProvider(message, apiKey)
        } else {
            completeWithBackend(message, sessionId, textOnly)
        }
    }

    private suspend fun completeWithBackend(
        message: String,
        sessionId: String?,
        textOnly: Boolean
    ): Result<String> {
        val userId = tokenManager.serverUserId.first()
        if (userId == null) {
            return Result.failure(
                IllegalStateException("请先在 AI 智能助手中填写 DeepSeek API Key，或登录后使用服务端 AI")
            )
        }

        return try {
            withContext(Dispatchers.IO) {
                if (textOnly) {
                    backendApi.chat(AIChatRequest(message, sessionId, true)).let { response ->
                        if (!response.success || response.reply.isNullOrBlank()) {
                            throw IllegalStateException(response.reply ?: "AI 返回内容为空")
                        }
                        response.reply.trim()
                    }
                } else {
                    backendAiRunClient.complete(message, sessionId)
                }
            }
                .let { Result.success(it) }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(IllegalStateException(toBackendUserMessage(error), error))
        }
    }

    private suspend fun completeWithUserProvider(
        message: String,
        apiKey: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val provider = aiPreferences.getProviderOnce()
            val model = provider.resolveModel(aiPreferences.getModelOnce())

            val body = JsonObject().apply {
                addProperty("model", model)
                addProperty("stream", false)
                addProperty("temperature", 0.2)
                add(
                    "messages",
                    gson.toJsonTree(
                        listOf(
                            mapOf(
                                "role" to "system",
                                "content" to "你是 NextThing 的任务管理 AI 助手，只输出用户要求的内容，不添加无关解释。"
                            ),
                            mapOf("role" to "user", "content" to message)
                        )
                    )
                )
            }.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url("${provider.baseUrl}chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            val call = aiHttpClient.newCall(request)
            val cancellationHandle = currentCoroutineContext().job.invokeOnCompletion { cause ->
                if (cause is CancellationException) call.cancel()
            }
            val content = try {
                call.execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IllegalStateException(
                            toExternalProviderUserMessage(provider, response.code)
                        )
                    }
                    val responseBody = response.body
                        ?.readUtf8Limited(MAX_EXTERNAL_RESPONSE_BYTES)
                        .orEmpty()
                    val root = gson.fromJson(responseBody, JsonObject::class.java)
                    root.getAsJsonArray("choices")
                        ?.firstOrNull()
                        ?.asJsonObject
                        ?.getAsJsonObject("message")
                        ?.get("content")
                        ?.asString
                        ?.trim()
                }
            } finally {
                cancellationHandle.dispose()
            }
            if (content.isNullOrBlank()) {
                throw IllegalStateException("AI 返回内容为空")
            }
            Result.success(content)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            currentCoroutineContext().ensureActive()
            Result.failure(error)
        }
    }

    private fun toBackendUserMessage(error: Throwable): String {
        return when {
            error is HttpException && error.code() == 401 ->
                "服务端 AI 登录状态已过期，请重新登录或配置 DeepSeek API Key。"
            error is HttpException ->
                "服务端 AI 暂时不可用，请稍后重试。"
            error.message?.contains("401") == true ->
                "服务端 AI 登录状态已过期，请重新登录或配置 DeepSeek API Key。"
            else ->
                error.message ?: "AI 服务暂时不可用，请稍后重试。"
        }
    }

    private fun toExternalProviderUserMessage(provider: AIProvider, code: Int): String {
        return when (code) {
            401 -> "${provider.displayName} API Key 无效或已过期，请在 AI 设置中检查后重试。"
            402 -> "${provider.displayName} 账户余额不足或额度已用完，请检查账户额度。"
            429 -> "${provider.displayName} 请求过于频繁，请稍后再试。"
            in 500..599 -> "${provider.displayName} 服务暂时不可用，请稍后重试。"
            else -> "${provider.displayName} AI 请求失败，请检查 AI 设置后重试。"
        }
    }

    private fun ResponseBody.readUtf8Limited(maxBytes: Int): String {
        val declaredLength = contentLength()
        if (declaredLength > maxBytes) {
            throw IllegalStateException("AI 返回内容超过安全上限")
        }
        return byteStream().use { input ->
            ByteArrayOutputStream().use { output ->
                val buffer = ByteArray(8 * 1024)
                var total = 0
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > maxBytes) {
                        throw IllegalStateException("AI 返回内容超过安全上限")
                    }
                    output.write(buffer, 0, read)
                }
                output.toString(Charsets.UTF_8.name())
            }
        }
    }
}
