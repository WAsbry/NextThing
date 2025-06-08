package com.wasbry.nextthing.ui.screen.homepage

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.wasbry.nextthing.R
import com.wasbry.nextthing.database.model.TaskSummary
import com.wasbry.nextthing.ui.componet.homepage.summary.TaskSummaryPanel
import com.wasbry.nextthing.ui.componet.homepage.today.TodayIncompleteTasksPanel
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

    val todayIncompleteTasks by todoTaskViewModel.getIncompleteTasksByDate(todayDate).collectAsState(initial = emptyList())

    // 收集 ViewModel 返回的 Flow（关键行：直接使用 getWeeklySummary 返回的 Flow）
    val weeklySummary by todoTaskViewModel.getWeeklySummary(today)
        .collectAsState(
            initial = TaskSummary(
                startDate = LocalDate.now(), // 初始值建议使用合理日期
                endDate = LocalDate.now(),
                taskTotalCount = 0,
                taskIncompleteTotalCount = 0,
                taskCompletedTotalCount = 0,
                taskAbandonedTotalCount = 0,
                taskPostponedTotalCount = 0,
                expectedTaskCount = 0
            )
        )


    // 手动订阅 Flow（测试用）
    LaunchedEffect(today) {
        todoTaskViewModel.getWeeklySummary(today)
            .collect { summary ->
                // 此处会触发 Flow 执行，日志应打印
                Log.d(TAG, "UI层收到数据：$summary")
            }
    }


    Column (
        modifier = Modifier
            .fillMaxWidth() // 占满整块屏幕
            .background(color = colorResource(R.color.background_color))
    ) {
        TaskSummaryPanel(weeklySummary,0, modifier = Modifier
                .width(400.dp)
                .wrapContentHeight()
                .padding(16.dp)) // 本周概要面板

        // 展示今日未完成的任务
        TodayIncompleteTasksPanel(todayIncompleteTasks = todayIncompleteTasks, modifier = Modifier.fillMaxWidth().height(500.dp), todoTaskViewModel = todoTaskViewModel)
    }
}