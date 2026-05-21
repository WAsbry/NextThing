package com.nextthing.app.domain.service

interface AIBriefingGenerator {
    enum class BriefingType { MORNING, EVENING }
    suspend fun generateBriefing(type: BriefingType, taskData: String): Result<String>
}
