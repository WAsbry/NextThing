package com.example.nextthingb1.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.nextthingb1.domain.model.TaskCategory
import com.example.nextthingb1.domain.model.TaskPriority
import com.example.nextthingb1.domain.model.TaskStatus
import java.time.LocalDateTime

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val priority: TaskPriority,
    val category: TaskCategory,
    val status: TaskStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val dueDate: LocalDateTime?,
    val completedAt: LocalDateTime?,
    val tags: List<String>,
    val isUrgent: Boolean,
    val estimatedDuration: Int,
    val actualDuration: Int,
    val subtasksJson: String // JSON string for subtasks
) 