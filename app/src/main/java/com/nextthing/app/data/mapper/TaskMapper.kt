package com.nextthing.app.data.mapper

import com.nextthing.app.data.local.entity.CategoryEntity
import com.nextthing.app.data.local.entity.TaskEntity
import com.nextthing.app.data.local.entity.TaskWithCategory
import com.nextthing.app.data.mapper.CategoryMapper.toDomain
import com.nextthing.app.domain.model.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import timber.log.Timber
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import com.nextthing.app.data.local.entity.SyncStatus
import com.nextthing.app.data.remote.dto.*

private val gson: Gson = GsonBuilder()
    .registerTypeAdapter(LocalDateTime::class.java, JsonSerializer<LocalDateTime> { src, _, _ ->
        JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
    })
    .registerTypeAdapter(LocalDateTime::class.java, JsonDeserializer { json, _, _ ->
        LocalDateTime.parse(json.asString, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    })
    .create()

/**
 * 将 TaskWithCategory（联表查询结果）转换为 Task 领域模型
 * 这是推荐的转换方式，包含完整的分类信息
 */
fun TaskWithCategory.toDomain(): Task {
    Timber.tag("DataFlow").v("  转换 TaskWithCategory->Task: ${task.title}")

    val entity = this.task
    val category = this.category.toDomain()

    return Task(
        id = entity.id,
        title = entity.title,
        description = entity.description,
        category = category, // 使用联表查询得到的分类
        status = entity.status,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
        dueDate = entity.dueDate,
        completedAt = entity.completedAt,
        tags = parseJsonList(entity.tags),
        isUrgent = entity.isUrgent,
        estimatedDuration = entity.estimatedDuration,
        actualDuration = entity.actualDuration,
        subtasks = parseSubtasks(entity.subtasksJson),
        imageUri = entity.imageUri,
        repeatFrequency = parseRepeatFrequency(entity.repeatFrequencyJson),
        locationInfo = parseLocationInfo(entity.locationInfoJson),
        importanceUrgency = parseImportanceUrgency(entity.importanceUrgencyJson),
        notificationStrategyId = entity.notificationStrategyId,
        isTemplate = entity.isTemplate,
        templateTaskId = entity.templateTaskId,
        instanceDate = entity.instanceDate
    )
}

/**
 * 将 Task 领域模型转换为 TaskEntity
 */
fun Task.toEntity(): TaskEntity {
    return TaskEntity(
        id = id,
        title = title,
        description = description,
        categoryId = category.id, // 使用分类ID作为外键
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
        dueDate = dueDate,
        completedAt = completedAt,
        tags = gson.toJson(tags),
        isUrgent = isUrgent,
        estimatedDuration = estimatedDuration,
        actualDuration = actualDuration,
        subtasksJson = gson.toJson(subtasks),
        imageUri = imageUri,
        repeatFrequencyJson = gson.toJson(repeatFrequency),
        locationInfoJson = locationInfo?.let { gson.toJson(it) },
        importanceUrgencyJson = importanceUrgency?.let { gson.toJson(it) },
        notificationStrategyId = notificationStrategyId,
        isTemplate = isTemplate,
        templateTaskId = templateTaskId,
        instanceDate = instanceDate
    )
}

/**
 * 将 Task 转换为同步DTO
 */
fun Task.toSyncDto(): TaskSyncDto {
    return TaskSyncDto(
        id = id,
        title = title,
        description = description,
        categoryId = category.id,
        status = status,
        createdAt = createdAt.toTimestamp(),
        updatedAt = updatedAt.toTimestamp(),
        dueDate = dueDate?.toTimestamp(),
        completedAt = completedAt?.toTimestamp(),
        tags = tags,
        isUrgent = isUrgent,
        estimatedDuration = estimatedDuration,
        actualDuration = actualDuration,
        subtasks = subtasks.map { SubtaskSyncDto(it.id, it.title, it.isCompleted) },
        imageUri = imageUri,
        repeatFrequency = repeatFrequency.toSyncDto(),
        locationInfo = locationInfo?.toSyncDto(),
        importanceUrgency = importanceUrgency?.toSyncDto(),
        notificationStrategyId = notificationStrategyId,
        isTemplate = isTemplate,
        templateTaskId = templateTaskId,
        instanceDate = instanceDate?.toTimestamp(),
        deleted = false
    )
}

/**
 * 将 TaskEntity 转换为同步DTO
 */
fun TaskEntity.toSyncDto(): TaskSyncDto {
    return TaskSyncDto(
        id = id,
        title = title,
        description = description,
        categoryId = categoryId,
        status = status,
        createdAt = createdAt.toTimestamp(),
        updatedAt = updatedAt.toTimestamp(),
        dueDate = dueDate?.toTimestamp(),
        completedAt = completedAt?.toTimestamp(),
        tags = parseJsonList(tags),
        isUrgent = isUrgent,
        estimatedDuration = estimatedDuration,
        actualDuration = actualDuration,
        subtasks = parseSubtasks(subtasksJson).map { SubtaskSyncDto(it.id, it.title, it.isCompleted) },
        imageUri = imageUri,
        repeatFrequency = parseRepeatFrequency(repeatFrequencyJson).toSyncDto(),
        locationInfo = parseLocationInfo(locationInfoJson)?.toSyncDto(),
        importanceUrgency = parseImportanceUrgency(importanceUrgencyJson)?.toSyncDto(),
        notificationStrategyId = notificationStrategyId,
        isTemplate = isTemplate,
        templateTaskId = templateTaskId,
        instanceDate = instanceDate?.toTimestamp(),
        deleted = false
    )
}

/**
 * 将 TaskSyncDto 转换为 TaskEntity
 */
fun TaskSyncDto.toEntity(syncStatus: SyncStatus = SyncStatus.SYNCED): TaskEntity {
    return TaskEntity(
        id = id,
        title = title,
        description = description,
        categoryId = categoryId,
        status = status,
        createdAt = createdAt.toLocalDateTime(),
        updatedAt = updatedAt.toLocalDateTime(),
        dueDate = dueDate?.toLocalDateTime(),
        completedAt = completedAt?.toLocalDateTime(),
        tags = gson.toJson(tags),
        isUrgent = isUrgent,
        estimatedDuration = estimatedDuration,
        actualDuration = actualDuration,
        subtasksJson = gson.toJson(subtasks.map { Subtask(it.id, it.title, it.isCompleted) }),
        imageUri = imageUri,
        repeatFrequencyJson = gson.toJson(repeatFrequency?.toDomain() ?: RepeatFrequency()),
        locationInfoJson = locationInfo?.let { gson.toJson(it.toDomain()) },
        importanceUrgencyJson = importanceUrgency?.let { gson.toJson(it.toDomain()) },
        notificationStrategyId = notificationStrategyId,
        isTemplate = isTemplate,
        templateTaskId = templateTaskId,
        instanceDate = instanceDate?.toLocalDateTime(),
        syncStatus = syncStatus,
        serverUpdatedAt = updatedAt
    )
}

// ========== 辅助转换方法 ==========

private fun LocalDateTime.toTimestamp(): Long {
    return atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

private fun Long.toLocalDateTime(): LocalDateTime {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())
}

private fun RepeatFrequency.toSyncDto(): RepeatFrequencySyncDto? {
    if (type == com.nextthing.app.domain.model.RepeatFrequencyType.NONE) return null
    return RepeatFrequencySyncDto(
        type = type.name,
        interval = 0,
        daysOfWeek = weekdays.toList().ifEmpty { null },
        dayOfMonth = monthDays.firstOrNull(),
        monthOfYear = null,
        endDate = null,
        occurrences = null
    )
}

private fun RepeatFrequencySyncDto.toDomain(): RepeatFrequency {
    return RepeatFrequency(
        type = com.nextthing.app.domain.model.RepeatFrequencyType.valueOf(type),
        weekdays = daysOfWeek?.toSet() ?: emptySet(),
        monthDays = dayOfMonth?.let { setOf(it) } ?: emptySet()
    )
}

private fun LocationInfo.toSyncDto(): LocationInfoSyncDto {
    return LocationInfoSyncDto(
        locationId = id,
        locationName = locationName,
        address = address,
        latitude = latitude,
        longitude = longitude
    )
}

private fun LocationInfoSyncDto.toDomain(): LocationInfo {
    return LocationInfo(
        id = locationId,
        locationName = locationName,
        latitude = latitude,
        longitude = longitude,
        address = address ?: ""
    )
}

private fun TaskImportanceUrgency.toSyncDto(): ImportanceUrgencySyncDto {
    val importanceValue = when (importance) {
        com.nextthing.app.domain.model.TaskImportance.IMPORTANT -> 1
        com.nextthing.app.domain.model.TaskImportance.NOT_IMPORTANT -> 0
    }
    val urgencyValue = when (urgency) {
        com.nextthing.app.domain.model.TaskUrgency.URGENT -> 1
        com.nextthing.app.domain.model.TaskUrgency.NOT_URGENT -> 0
    }
    return ImportanceUrgencySyncDto(
        importance = importanceValue,
        urgency = urgencyValue,
        quadrant = name
    )
}

private fun ImportanceUrgencySyncDto.toDomain(): TaskImportanceUrgency {
    return com.nextthing.app.domain.model.TaskImportanceUrgency.valueOf(quadrant)
}

/**
 * 批量转换 TaskWithCategory 列表为 Task 列表
 */
fun List<TaskWithCategory>.toDomainList(): List<Task> {
    Timber.tag("DataFlow").d("━━━━━━ Mapper.toDomainList ━━━━━━")
    Timber.tag("DataFlow").d("开始转换 ${this.size} 个 TaskWithCategory")
    val result = map { it.toDomain() }
    Timber.tag("DataFlow").d("✅ 转换完成，得到 ${result.size} 个 Task")
    return result
}

/**
 * 批量转换 Task 列表为 TaskEntity 列表
 */
fun List<Task>.toEntityList(): List<TaskEntity> {
    return map { it.toEntity() }
}

// ========== 私有辅助方法 ==========

private fun parseJsonList(json: String): List<String> {
    return try {
        val type = object : TypeToken<List<String>>() {}.type
        gson.fromJson(json, type) ?: emptyList()
    } catch (e: Exception) {
        Timber.tag("DataFlow").w("  解析 tags JSON 失败: ${e.message}")
        emptyList()
    }
}

private fun parseSubtasks(json: String): List<Subtask> {
    return try {
        val type = object : TypeToken<List<Subtask>>() {}.type
        gson.fromJson(json, type) ?: emptyList()
    } catch (e: Exception) {
        Timber.tag("DataFlow").w("  解析 subtasks JSON 失败: ${e.message}")
        emptyList()
    }
}

private fun parseRepeatFrequency(json: String): RepeatFrequency {
    return try {
        gson.fromJson(json, RepeatFrequency::class.java) ?: RepeatFrequency()
    } catch (e: Exception) {
        Timber.tag("DataFlow").w("  解析 repeatFrequency JSON 失败: ${e.message}")
        RepeatFrequency()
    }
}

private fun parseLocationInfo(json: String?): LocationInfo? {
    if (json.isNullOrBlank()) return null
    return try {
        gson.fromJson(json, LocationInfo::class.java)
    } catch (e: Exception) {
        Timber.tag("DataFlow").w("  解析 locationInfo JSON 失败: ${e.message}")
        null
    }
}

private fun parseImportanceUrgency(json: String?): TaskImportanceUrgency? {
    if (json.isNullOrBlank()) return null
    return try {
        gson.fromJson(json, TaskImportanceUrgency::class.java)
    } catch (e: Exception) {
        Timber.tag("DataFlow").w("  解析 importanceUrgency JSON 失败: ${e.message}")
        null
    }
}

/**
 * 将 SyncConflictDto 转换为 SyncConflict 领域模型
 */
fun SyncConflictDto.toDomain(): SyncConflict {
    return SyncConflict(
        taskId = taskId,
        taskTitle = localVersion.title,
        localModifiedAt = localVersion.updatedAt,
        serverModifiedAt = serverVersion.updatedAt,
        conflictType = com.nextthing.app.domain.model.ConflictType.valueOf(conflictType.name)
    )
}

/**
 * 将 CategoryConflictDto 转换为 SyncConflict 领域模型
 */
fun CategoryConflictDto.toDomain(): SyncConflict {
    return SyncConflict(
        taskId = categoryId,
        taskTitle = localVersion.name,
        localModifiedAt = localVersion.createdAt,
        serverModifiedAt = serverVersion.createdAt,
        conflictType = com.nextthing.app.domain.model.ConflictType.valueOf(conflictType.name)
    )
}

/**
 * 将 CategoryEntity 转换为 CategorySyncDto
 */
fun CategoryEntity.toSyncDto(): CategorySyncDto {
    return CategorySyncDto(
        id = id,
        name = name,
        type = type,
        icon = icon,
        colorHex = colorHex,
        sortOrder = sortOrder,
        createdAt = createdAt.toTimestamp(),
        isEnabled = isEnabled,
        deleted = false
    )
} 