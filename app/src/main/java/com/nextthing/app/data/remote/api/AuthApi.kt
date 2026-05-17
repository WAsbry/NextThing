package com.nextthing.app.data.remote.api

import com.nextthing.app.data.remote.dto.AuthResponse
import com.nextthing.app.data.remote.dto.LoginRequest
import com.nextthing.app.data.remote.dto.RefreshRequest
import com.nextthing.app.data.remote.dto.RegisterRequest
import com.nextthing.app.data.remote.dto.ApiResult
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResult<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResult<AuthResponse>

    @POST("api/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): ApiResult<AuthResponse>
}
