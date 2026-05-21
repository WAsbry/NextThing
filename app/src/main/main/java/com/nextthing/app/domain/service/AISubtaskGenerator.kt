package com.nextthing.app.domain.service

interface AISubtaskGenerator {
    suspend fun generateSubtasks(taskTitle: String, taskDescription: String): Result<List<String>>
}
