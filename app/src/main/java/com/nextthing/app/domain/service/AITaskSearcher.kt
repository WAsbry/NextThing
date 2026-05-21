package com.nextthing.app.domain.service

import com.nextthing.app.domain.model.Task

interface AITaskSearcher {
    suspend fun searchByNaturalLanguage(
        query: String,
        allTasks: List<Task>
    ): Result<List<Task>>
}
