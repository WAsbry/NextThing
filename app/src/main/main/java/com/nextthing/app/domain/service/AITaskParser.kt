package com.nextthing.app.domain.service

import com.nextthing.app.domain.model.AITaskParseResult

interface AITaskParser {
    suspend fun parseTaskFromText(
        input: String,
        availableCategories: List<String>,
        availableLocations: List<String> = emptyList()
    ): Result<List<AITaskParseResult>>
}
