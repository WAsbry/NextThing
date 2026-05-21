package com.nextthing.app.domain.service

import com.nextthing.app.domain.model.Task

data class BehaviorInsight(
    val patterns: List<String>,
    val suggestions: List<String>
)

interface AIBehaviorAnalyzer {
    suspend fun analyzeBehavior(
        completedTasks: List<Task>,
        allTasks: List<Task>
    ): Result<BehaviorInsight>
}
