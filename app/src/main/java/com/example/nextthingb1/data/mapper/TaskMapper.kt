package com.example.nextthingb1.data.mapper

import com.example.nextthingb1.data.local.entity.TaskEntity
import com.example.nextthingb1.domain.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

private val gson = Gson()

fun TaskEntity.toDomain(): Task {
    val subtasks = try {
        val listType = object : TypeToken<List<Subtask>>() {}.type
        gson.fromJson<List<Subtask>>(subtasksJson, listType) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }

    val repeatFrequency = try {
        gson.fromJson(repeatFrequencyJson, RepeatFrequency::class.java) ?: RepeatFrequency()
    } catch (e: Exception) {
        RepeatFrequency()
    }

    val locationInfo = try {
        locationInfoJson?.let {
            gson.fromJson(it, LocationInfo::class.java)
        }
    } catch (e: Exception) {
        null
    }

    val importanceUrgency = try {
        importanceUrgencyJson?.let {
            gson.fromJson(it, TaskImportanceUrgency::class.java)
        }
    } catch (e: Exception) {
        null
    }

    return Task(
        id = id,
        title = title,
        description = description,
        priority = priority,
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
        importanceUrgency = importanceUrgency
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
        priority = priority,
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
        importanceUrgencyJson = importanceUrgencyJson
    )
}

fun List<TaskEntity>.toDomain(): List<Task> = map { it.toDomain() }
fun List<Task>.toEntity(): List<TaskEntity> = map { it.toEntity() } 