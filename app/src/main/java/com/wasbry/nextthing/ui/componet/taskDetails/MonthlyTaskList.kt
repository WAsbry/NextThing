package com.wasbry.nextthing.ui.componet.taskDetails

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wasbry.nextthing.database.model.TodoTask
import com.wasbry.nextthing.ui.componet.common.TaskItem
import com.wasbry.nextthing.viewmodel.todoTask.TodoTaskViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
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

    // 主布局
    Column(modifier = modifier.fillMaxSize()) {
        // 顶部标题栏
        Text(
            text = "本月任务列表",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.CenterHorizontally)
        )

        // 任务列表区域
        LazyColumn(modifier = Modifier.fillMaxSize()) {
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