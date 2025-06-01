package com.wasbry.nextthing.database.model

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate

/**
 * 一周的数据类
 * */
data class WeeklySummary @RequiresApi(Build.VERSION_CODES.O) constructor(
    val startDate: LocalDate = LocalDate.now(),  // 改为可为空
    val endDate: LocalDate = LocalDate.now(),    // 改为可为空
    val taskTotalCount: Int = 0,
    val taskIncompleteTotalCount: Int = 0,
    val taskCompletedTotalCount: Int = 0,
    val taskAbandonedTotalCount: Int = 0,
    val taskPostponedTotalCount: Int = 0,
    val expectedTaskCount: Int = 0
)
