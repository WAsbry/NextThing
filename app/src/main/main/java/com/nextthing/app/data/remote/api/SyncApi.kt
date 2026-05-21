package com.nextthing.app.data.remote.api

import com.nextthing.app.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * 数据同步API接口
 */
interface SyncApi {

    /**
     * 同步任务数据
     * 上传本地修改，下载服务器更新
     */
    @POST("sync/tasks")
    suspend fun syncTasks(@Body request: TaskSyncRequest): Response<TaskSyncResponse>

    /**
     * 获取自上次同步以来的服务器任务更新
     */
    @GET("sync/tasks")
    suspend fun fetchTaskUpdates(
        @Query("lastSyncTimestamp") lastSyncTimestamp: Long?
    ): Response<TaskSyncResponse>

    /**
     * 批量上传本地任务
     */
    @POST("sync/tasks/upload")
    suspend fun uploadTasks(@Body request: TaskSyncRequest): Response<TaskSyncResponse>

    /**
     * 同步分类数据
     */
    @POST("sync/categories")
    suspend fun syncCategories(@Body request: CategorySyncRequest): Response<CategorySyncResponse>

    /**
     * 获取分类更新
     */
    @GET("sync/categories")
    suspend fun fetchCategoryUpdates(
        @Query("lastSyncTimestamp") lastSyncTimestamp: Long?
    ): Response<CategorySyncResponse>

    /**
     * 获取同步状态
     */
    @GET("sync/status")
    suspend fun getSyncStatus(
        @Query("lastSyncTimestamp") lastSyncTimestamp: Long?
    ): Response<SyncStatusDto>

    /**
     * 解决同步冲突（选择使用服务器版本）
     */
    @POST("sync/conflicts/resolve/server")
    suspend fun resolveConflictUseServer(
        @Query("taskId") taskId: String
    ): Response<TaskSyncDto>

    /**
     * 解决同步冲突（选择使用本地版本）
     */
    @POST("sync/conflicts/resolve/local")
    suspend fun resolveConflictUseLocal(
        @Query("taskId") taskId: String,
        @Body task: TaskSyncDto
    ): Response<TaskSyncDto>

    /**
     * 全量同步（首次登录或数据恢复时使用）
     */
    @GET("sync/full")
    suspend fun fullSync(): Response<FullSyncResponse>
}

/**
 * 全量同步响应
 */
data class FullSyncResponse(
    val tasks: List<TaskSyncDto>,
    val categories: List<CategorySyncDto>,
    val serverTimestamp: Long,
    val userSettings: UserSettingsSyncDto?
)

/**
 * 用户设置同步DTO
 */
data class UserSettingsSyncDto(
    val notificationEnabled: Boolean,
    val theme: String,
    val language: String,
    val defaultReminderTime: Int,
    val geofenceEnabled: Boolean
)
