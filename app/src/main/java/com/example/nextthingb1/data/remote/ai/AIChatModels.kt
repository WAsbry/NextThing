package com.example.nextthingb1.data.remote.ai

import com.google.gson.annotations.SerializedName

// ── 请求模型 ─────────────────────────────────────────────

data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Float = 0.2f,
    @SerializedName("response_format")
    val responseFormat: ResponseFormat? = ResponseFormat()
)

data class ChatMessage(
    val role: String,   // "system" | "user"
    val content: String
)

data class ResponseFormat(
    val type: String = "json_object"
)

// ── 响应模型 ─────────────────────────────────────────────

data class ChatCompletionResponse(
    val choices: List<ChatChoice>?,
    val error: ChatErrorInfo?
)

data class ChatChoice(
    val message: ChatMessage?
)

data class ChatErrorInfo(
    val message: String?,
    val type: String?
)
