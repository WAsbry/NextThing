package com.example.nextthingb1.data.mapper

import com.example.nextthingb1.data.local.entity.TaskEntity
import com.example.nextthingb1.data.local.entity.TaskWithCategory
import com.example.nextthingb1.data.mapper.CategoryMapper.toDomain
import com.example.nextthingb1.domain.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import timber.log.Timber

private val gson = Gson()

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
        notificationStrategyId = entity.notificationStrategyId
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
        notificationStrategyId = notificationStrategyId
    )
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