package com.wasbry.nextthing.database.model

import java.time.LocalDate

/**
 * 一周的数据类
 * */
data class WeeklySummary(
    val startDate: LocalDate, // 本周开始日期
    val endDate: LocalDate, // 本周结束日期
    val taskTotalCount: Int, // 本周任务总数
    val taskIncompleteTotalCount: Int, // 本周未完成任务总数
    val taskCompletedTotalCount: Int, // 本周已完成任务总数
    val taskAbandonedTotalCount: Int, // 本周放弃任务总数
    val taskPostponedTotalCount: Int, // 本周延期任务总数
    val expectedTaskCount: Int, // // 本周延期任务总数
)
