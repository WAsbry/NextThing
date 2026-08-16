package com.nextthing.app.data.remote.api

import com.nextthing.app.data.remote.dto.AIChatRequest
import com.nextthing.app.data.remote.dto.AIChatResponse
import com.nextthing.app.data.remote.dto.AIRunCreateRequest
import com.nextthing.app.data.remote.dto.AIRunResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AIChatApi {

    @POST("api/ai/chat")
    suspend fun chat(@Body request: AIChatRequest): AIChatResponse

    @POST("api/ai/runs")
    suspend fun createRun(@Body request: AIRunCreateRequest): AIRunResponse
}
