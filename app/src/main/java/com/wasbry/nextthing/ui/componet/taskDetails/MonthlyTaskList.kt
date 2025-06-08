package com.wasbry.nextthing.ui.componet.taskDetails


import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wasbry.nextthing.database.model.TaskSummary
import com.wasbry.nextthing.database.model.TodoTask
import com.wasbry.nextthing.ui.componet.common.TaskItem
import com.wasbry.nextthing.ui.componet.homepage.summary.TaskSummaryPanel
import com.wasbry.nextthing.viewmodel.todoTask.TodoTaskViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*

/**
 * 月度任务展示列表
 * */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MonthlyTaskListScreen(
    modifier: Modifier = Modifier,
    todoTaskViewModel: TodoTaskViewModel = viewModel()
) {

    val TAG = "MonthlyTaskListScreen"

    val coroutineScope = rememberCoroutineScope()

    // 计算本月第一天和最后一天
    val calendar = Calendar.getInstance()
    val startOfMonth = calendar.apply { set(Calendar.DAY_OF_MONTH, 1) }.time
    val endOfMonth = calendar.apply {
        set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
    }.time

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val startDateStr = dateFormat.format(startOfMonth)
    val endDateStr = dateFormat.format(endOfMonth)

    // 使用 collectAsStateWithLifecycle 收集冷流
    val monthTasks by todoTaskViewModel.getTasksByDateRange(startDateStr, endDateStr)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    // 按日期分组任务（过滤掉没有任务的日期）
    val tasksByDate by remember(monthTasks) {
        derivedStateOf {
            monthTasks.groupBy { task ->
                try {
                    dateFormat.parse(task.madeDate) ?: Date(0)
                } catch (e: Exception) {
                    Date(0)
                }
            }.filterValues { it.isNotEmpty() } // 过滤掉空的日期分组
                .toSortedMap(compareByDescending { it })
        }
    }

    val today = LocalDate.now()
    // 收集 ViewModel 返回的 Flow（关键行：直接使用 getWeeklySummary 返回的 Flow）
    val monthlySummary by todoTaskViewModel.getMonthlySummary(today)
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
        todoTaskViewModel.getMonthlySummary(today)
            .collect { summary ->
                // 此处会触发 Flow 执行，日志应打印
                Log.d(TAG, "UI层收到数据：$summary")
            }
    }

    // 主布局
    Column(modifier = modifier.fillMaxSize()) {
        // 引入月度总结面板
        TaskSummaryPanel(monthlySummary,1,modifier = Modifier
            .width(400.dp)
            .wrapContentHeight()
            .padding(16.dp))

        // 任务列表区域
        LazyColumn(modifier = Modifier.fillMaxSize()
                                .padding(start = 20.dp, end = 20.dp)) {
            // 检查是否有任务
            if (tasksByDate.isEmpty()) {
                item { EmptyTaskPlaceholder() }
            } else {
                // 遍历每一天的任务
                tasksByDate.forEach { (date, tasks) ->
                    // 日期标题项
                    item {
                        DateHeader(dateText = formatDateForDisplay(date))
                    }

                    // 该日期的任务列表
                    items(tasks, key = { it.id }) { task ->
                        TaskItem(
                            task = task,
                            onCompleted = {
                                coroutineScope.launch {
                                    todoTaskViewModel.markTaskAsCompleted(task)
                                }
                            },
                            onAbandoned = {
                                coroutineScope.launch {
                                    todoTaskViewModel.markTaskAsAbandoned(task)
                                }
                            },
                            onPostponed = {
                                coroutineScope.launch {
                                    Log.d("debug","准备延期这个任务，延期前：task = ${task}")
                                    todoTaskViewModel.markTaskAsPostponed(task)
                                }
                            }
                        )
                    }

                    // 日期分隔线
                    item {
                        Divider(
                            thickness = 1.dp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

// 空状态占位符
@Composable
fun EmptyTaskPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Done,
                contentDescription = "空任务",
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "本月暂无任务",
                fontSize = 18.sp,
            )
        }
    }
}

// 日期标题组件
@Composable
fun DateHeader(dateText: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = dateText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

// 辅助函数：格式化日期显示
private fun formatDateForDisplay(date: Date): String {
    val calendar = Calendar.getInstance().apply { time = date }
    val dayOfWeek = when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.SUNDAY -> "星期日"
        Calendar.MONDAY -> "星期一"
        Calendar.TUESDAY -> "星期二"
        Calendar.WEDNESDAY -> "星期三"
        Calendar.THURSDAY -> "星期四"
        Calendar.FRIDAY -> "星期五"
        Calendar.SATURDAY -> "星期六"
        else -> ""
    }

    return "${calendar.get(Calendar.YEAR)}年${calendar.get(Calendar.MONTH) + 1}月${calendar.get(Calendar.DAY_OF_MONTH)}日 $dayOfWeek"
}