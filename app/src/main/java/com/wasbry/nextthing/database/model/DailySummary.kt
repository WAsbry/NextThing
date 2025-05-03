package com.wasbry.nextthing.database.model

import kotlinx.coroutines.flow.Flow

/**
 * 今日总结：
 *      已做完任务数，
 *      未做完任务数
 *      */
data class DailySummary(
    val completedTaskCount: Int,
    val uncompletedTaskCount: Int,
    val dailyTaskList: Flow<List<TodoTask>>
)
