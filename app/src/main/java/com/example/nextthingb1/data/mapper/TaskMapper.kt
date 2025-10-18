package com.example.nextthingb1.data.mapper

import com.example.nextthingb1.data.local.entity.TaskEntity
import com.example.nextthingb1.domain.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

private val gson = Gson()

fun TaskEntity.toDomain(): Task {
    timber.log.Timber.tag("DataFlow").v("  转换Entity->Domain: $title")

    val subtasks = try {
        val listType = object : TypeToken<List<Subtask>>() {}.type
        gson.fromJson<List<Subtask>>(subtasksJson, listType) ?: emptyList()
    } catch (e: Exception) {
        timber.log.Timber.tag("DataFlow").w("  解析subtasks失败: ${e.message}")
        emptyList()
    }

    val repeatFrequency = try {
        gson.fromJson(repeatFrequencyJson, RepeatFrequency::class.java) ?: RepeatFrequency()
    } catch (e: Exception) {
        timber.log.Timber.tag("DataFlow").w("  解析repeatFrequency失败: ${e.message}")
        RepeatFrequency()
    }

    val locationInfo = try {
        locationInfoJson?.let {
            gson.fromJson(it, LocationInfo::class.java)
        }
    } catch (e: Exception) {
        timber.log.Timber.tag("DataFlow").w("  解析locationInfo失败: ${e.message}")
        null
    }

    val importanceUrgency = try {
        importanceUrgencyJson?.let {
            gson.fromJson(it, TaskImportanceUrgency::class.java)
        }
    } catch (e: Exception) {
        timber.log.Timber.tag("DataFlow").w("  解析importanceUrgency失败: ${e.message}")
        null
    }

    return Task(
        id = id,
        title = title,
        description = description,
        category = category,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
        dueDate = dueDate,
        completedAt = completedAt,
        tags = tags,
        isUrgent = isUrgent,
        estimatedDuration = estimatedDuration,
        actualDuration = actualDuration,
        subtasks = subtasks,
        imageUri = imageUri,
        repeatFrequency = repeatFrequency,
        locationInfo = locationInfo,
        importanceUrgency = importanceUrgency,
        notificationStrategyId = notificationStrategyId
    )
}

fun Task.toEntity(): TaskEntity {
    val subtasksJson = gson.toJson(subtasks)
    val repeatFrequencyJson = gson.toJson(repeatFrequency)
    val locationInfoJson = locationInfo?.let { gson.toJson(it) }
    val importanceUrgencyJson = importanceUrgency?.let { gson.toJson(it) }

    return TaskEntity(
        id = id,
        title = title,
        description = description,
        category = category,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
        dueDate = dueDate,
        completedAt = completedAt,
        tags = tags,
        isUrgent = isUrgent,
        estimatedDuration = estimatedDuration,
        actualDuration = actualDuration,
        subtasksJson = subtasksJson,
        imageUri = imageUri,
        repeatFrequencyJson = repeatFrequencyJson,
        locationInfoJson = locationInfoJson,
        importanceUrgencyJson = importanceUrgencyJson,
        notificationStrategyId = notificationStrategyId
    )
}

fun List<TaskEntity>.toDomain(): List<Task> {
    timber.log.Timber.tag("DataFlow").d("━━━━━━ Mapper.toDomain ━━━━━━")
    timber.log.Timber.tag("DataFlow").d("开始转换 ${this.size} 个TaskEntity")
    val result = map { it.toDomain() }
    timber.log.Timber.tag("DataFlow").d("✅ 转换完成，得到 ${result.size} 个Task")
    return result
}

fun List<Task>.toEntity(): List<TaskEntity> = map { it.toEntity() } 