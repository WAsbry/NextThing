package com.example.nextthingb1.data.remote.api

import com.example.nextthingb1.domain.model.Task
import retrofit2.Response
import retrofit2.http.*

interface TaskApi {
    @GET("tasks")
    suspend fun fetchTasks(): Response<List<Task>>

    @GET("tasks/{id}")
    suspend fun fetchTaskById(@Path("id") id: String): Response<Task>

    @POST("tasks")
    suspend fun createTask(@Body task: Task): Response<Task>

    @PUT("tasks/{id}")
    suspend fun updateTask(@Path("id") id: String, @Body task: Task): Response<Task>

    @DELETE("tasks/{id}")
    suspend fun deleteTask(@Path("id") id: String): Response<Unit>
} 