package com.nextthing.app.domain.service

import com.nextthing.app.domain.model.Task

data class TimeEstimate(
    val estimatedMinutes: Int,
    val reasoning: String,
    val similarTaskCount: Int
)

interface AITimeEstimator {
    suspend fun estimateTime(
        taskTitle: String,
        taskDescription: String,
        categoryName: String?,
        recentCompletedTasks: List<Task>
    ): Result<TimeEstimate>
}
