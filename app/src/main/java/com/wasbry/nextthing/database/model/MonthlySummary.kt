package com.wasbry.nextthing.database.model

import java.time.LocalDate

/**
 * 月度总结数据类：用于首页展示
 * */
data class MonthlySummary(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val plannedHours: Int,
    val expectedTaskCount: Int,
    val actualTaskCount: Int
)