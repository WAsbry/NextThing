package com.example.nextthingb1.domain.model

import java.time.LocalDateTime

data class AITaskParseResult(
    val title: String,
    val description: String? = null,
    val dueDate: LocalDateTime? = null,
    val categoryName: String? = null,
    val importance: TaskImportanceUrgency? = null,
    val repeatType: RepeatFrequencyType? = null,
    val repeatWeekdays: Set<Int>? = null,
    val locationName: String? = null,
    val confidence: Float = 0f
)
