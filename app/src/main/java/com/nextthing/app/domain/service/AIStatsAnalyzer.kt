package com.nextthing.app.domain.service

interface AIStatsAnalyzer {
    suspend fun generateSummary(statsData: String): Result<String>
}
