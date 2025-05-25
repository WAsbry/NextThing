package com.wasbry.nextthing.ui.screen.homepage

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.wasbry.nextthing.database.model.WeeklySummary
import com.wasbry.nextthing.ui.componet.homepage.WeeklySummaryPanel
import com.wasbry.nextthing.viewmodel.personalTime.PersonalTimeViewModel
import com.wasbry.nextthing.viewmodel.todoTask.TodoTaskViewModel
import java.time.LocalDate

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomePage(
    todoTaskViewModel: TodoTaskViewModel,
    personalTimeViewModel: PersonalTimeViewModel
) {

    // 定义开始日期和结束日期，这里使用月日，忽略年份
    val tag = "HomePage"

    val today = LocalDate.now()

    val defaultSummary = WeeklySummary(
        startDate = LocalDate.MIN,
        endDate = LocalDate.MIN,
        taskTotalCount = 0,
        taskIncompleteTotalCount = 0,
        taskCompletedTotalCount = 0,
        taskAbandonedTotalCount = 0,
        taskPostponedTotalCount = 0,
        expectedTaskCount = 0
    )

    var weeklySummary by remember { mutableStateOf<WeeklySummary>(defaultSummary) }

    LaunchedEffect(today) {
        // 调用ViewModel的协程方法
        weeklySummary = todoTaskViewModel.getWeeklySummaryByDate(today)
    }


//    // 实例化 MonthlySummary 对象
//    val summary = MonthlySummary(
//        startDate = start,
//        endDate = end,
//        plannedHours = planned,
//        expectedTaskCount = expected,
//        actualTaskCount = actual
//    )
//
    Column (
        modifier = Modifier
            .fillMaxWidth() // 占满整块屏幕
    ) {
        WeeklySummaryPanel(weeklySummary)
    }
}