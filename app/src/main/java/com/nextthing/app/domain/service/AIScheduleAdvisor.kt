package com.nextthing.app.domain.service

import com.nextthing.app.domain.model.Task

data class ScheduleAdvice(
    val summary: String,
    val suggestions: List<String>
)

interface AIScheduleAdvisor {
    suspend fun analyzeSchedule(
        overdueTasks: List<Task>,
        todayTasks: List<Task>,
        urgentTasks: List<Task>
    ): Result<ScheduleAdvice>
}
