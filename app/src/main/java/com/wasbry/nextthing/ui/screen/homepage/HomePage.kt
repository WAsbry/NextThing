package com.wasbry.nextthing.ui.screen.homepage

import android.os.Build
import android.widget.Space
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wasbry.nextthing.database.model.WeeklySummary
import com.wasbry.nextthing.ui.componet.homepage.summary.WeeklySummaryPanel
import com.wasbry.nextthing.ui.componet.homepage.today.TodayTaskPanel
import com.wasbry.nextthing.viewmodel.personalTime.PersonalTimeViewModel
import com.wasbry.nextthing.viewmodel.timetype.TimeTypeViewModel
import com.wasbry.nextthing.viewmodel.todoTask.TodoTaskViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomePage(
    todoTaskViewModel: TodoTaskViewModel,
    personalTimeViewModel: PersonalTimeViewModel,
    timeTypeViewModel: TimeTypeViewModel
) {

    val TAG = "HomePage"

    val today = LocalDate.now()

    // 获取今天的日期（格式：YYYY-MM-DD）
    val todayDate: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    val todayTasks by todoTaskViewModel.getTasksByDate(todayDate).collectAsState(initial = emptyList())

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

    Column (
        modifier = Modifier
            .fillMaxWidth() // 占满整块屏幕
    ) {
        WeeklySummaryPanel(weeklySummary)

        Spacer(modifier = Modifier.height(4.dp))

        TodayTaskPanel(todayTasks = todayTasks, modifier = Modifier.fillMaxWidth())
    }
}