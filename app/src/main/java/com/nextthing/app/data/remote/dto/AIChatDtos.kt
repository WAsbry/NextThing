package com.nextthing.app.data.remote.dto

data class AIChatRequest(
    val message: String,
    val sessionId: String? = null,
    val textOnly: Boolean = false
)

data class AIChatResponse(
    val success: Boolean,
    val reply: String?,
    val sessionId: String? = null
)

data class AIRunCreateRequest(
    val sessionId: String? = null,
    val message: String
)

data class AIRunResponse(
    val runId: String,
    val sessionId: String,
    val status: String,
    val errorCode: String? = null
)
