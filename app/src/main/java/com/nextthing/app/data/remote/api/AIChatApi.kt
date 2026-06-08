package com.nextthing.app.data.remote.api

import com.nextthing.app.data.remote.dto.AIChatRequest
import com.nextthing.app.data.remote.dto.AIChatResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AIChatApi {

    @POST("api/ai/chat")
    suspend fun chat(@Body request: AIChatRequest): AIChatResponse
}
