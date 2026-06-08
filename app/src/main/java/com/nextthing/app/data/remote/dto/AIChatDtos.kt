package com.nextthing.app.data.remote.dto

data class AIChatRequest(
    val message: String,
    val sessionId: String? = null,
    val textOnly: Boolean = false
)

data class AIChatResponse(
    val success: Boolean,
    val reply: String?
)
