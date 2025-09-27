package com.example.nextthingb1.data.mapper

import com.example.nextthingb1.data.local.entity.TaskEntity
import com.example.nextthingb1.domain.model.Task
import com.example.nextthingb1.domain.model.Subtask
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
        imageUri = imageUri
    )
}

fun Task.toEntity(): TaskEntity {
    val subtasksJson = gson.toJson(subtasks)
    
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
        imageUri = imageUri
    )
}

fun List<TaskEntity>.toDomain(): List<Task> = map { it.toDomain() }
fun List<Task>.toEntity(): List<TaskEntity> = map { it.toEntity() } 