package com.wasbry.nextthing.ui.screen.homepage

import MonthlySummaryPanel
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wasbry.nextthing.database.model.MonthlySummary
import java.time.LocalDate

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomePage() {

    // 定义开始日期和结束日期，这里使用月日，忽略年份
    val start = LocalDate.of(2024, 10, 1) // 示例年份，实际使用时可忽略
    val end = LocalDate.of(2024, 10, 31)  // 示例年份，实际使用时可忽略

    val planned = 80
    val expected = 20
    val actual = 15

    // 实例化 MonthlySummary 对象
    val summary = MonthlySummary(
        startDate = start,
        endDate = end,
        plannedHours = planned,
        expectedTaskCount = expected,
        actualTaskCount = actual
    )

    Column (
        modifier = Modifier
            .fillMaxWidth() // 占满整块屏幕
    ) {
        MonthlySummaryPanel(summary)
    }
}