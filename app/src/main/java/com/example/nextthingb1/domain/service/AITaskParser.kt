package com.example.nextthingb1.domain.service

import com.example.nextthingb1.domain.model.AITaskParseResult

interface AITaskParser {
    suspend fun parseTaskFromText(
        input: String,
        availableCategories: List<String>,
        availableLocations: List<String> = emptyList()
    ): Result<AITaskParseResult>
}
