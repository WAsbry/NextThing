package com.example.nextthingb1.presentation.screens.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import android.util.Log
import com.example.nextthingb1.domain.model.Task
import com.example.nextthingb1.domain.model.TaskCategory
import com.example.nextthingb1.domain.model.TaskStatus
import com.example.nextthingb1.domain.model.TaskTab
import com.example.nextthingb1.presentation.theme.*
import com.example.nextthingb1.presentation.components.TaskItemCard
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

@Composable
fun TasksScreen(
    viewModel: TasksViewModel = hiltViewModel(),
    onNavigateToTaskDetail: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
    ) {
        // 本周任务概览卡片（包含视图切换）
        WeeklyOverviewCard(
            tasks = uiState.allTasks,
            earliestTaskDate = uiState.earliestTaskDate,
            selectedView = uiState.selectedView,
            onViewSelected = { viewModel.selectView(it) },
            currentWeekOffset = uiState.currentWeekOffset,
            onWeekChanged = { viewModel.changeWeek(it) }
        )

        // 内容区域
        when (uiState.selectedView) {
            TaskView.LIST -> {
                Column {
                    // 任务标签页
                    TaskTabs(
                        selectedTab = uiState.selectedTab,
                        onTabSelected = { viewModel.selectTab(it) }
                    )

                    // 任务列表
                    TasksListView(
                        taskGroups = uiState.taskGroups,
                        onTaskClick = { task ->
                            onNavigateToTaskDetail(task.id)
                        },
                        onToggleStatus = { taskId -> viewModel.toggleTaskStatus(taskId) },
                        onDefer = { taskId -> viewModel.deferTask(taskId) },
                        onCancel = { taskId -> viewModel.cancelTask(taskId) }
                    )
                }
            }
            TaskView.CALENDAR -> {
                TasksCalendarView(
                    calendarDays = uiState.calendarDays,
                    selectedDate = uiState.selectedDate,
                    selectedDateTasks = uiState.selectedDateTasks,
                    selectedDateCompletedCount = uiState.selectedDateCompletedCount,
                    selectedDatePendingCount = uiState.selectedDatePendingCount,
                    selectedDateOverdueCount = uiState.selectedDateOverdueCount,
                    selectedDateCancelledCount = uiState.selectedDateCancelledCount,
                    onDateSelected = { viewModel.selectDate(it) },
                    onNavigateToTaskDetail = onNavigateToTaskDetail
                )
            }
        }
    }
}

@Composable
fun WeeklyOverviewCard(
    tasks: List<Task>,
    earliestTaskDate: LocalDate?,
    selectedView: TaskView,
    onViewSelected: (TaskView) -> Unit,
    currentWeekOffset: Int = 0,
    onWeekChanged: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val cardHeight = screenHeight * 0.3f

    // 计算当前周的任务数据
    val currentWeekData = remember(tasks, currentWeekOffset, earliestTaskDate) {
        val data = calculateCurrentWeekData(tasks, earliestTaskDate, currentWeekOffset)
        data
    }

    // 计算按钮状态
    val maxWeek = getCurrentMaxWeek(earliestTaskDate)
    val isFirstWeek = currentWeekData.weekNumber <= 1
    val isLastWeek = currentWeekData.weekNumber >= maxWeek

    Log.d("switchWeek", "=== 按钮状态计算 ===")
    Log.d("switchWeek", "当前周偏移量: $currentWeekOffset")
    Log.d("switchWeek", "最大周数: $maxWeek")
    Log.d("switchWeek", "当前周数: ${currentWeekData.weekNumber}")
    Log.d("switchWeek", "是否为第一周: $isFirstWeek")
    Log.d("switchWeek", "是否为最后一周: $isLastWeek")
    Log.d("switchWeek", "上箭头enabled: ${!isFirstWeek} (不是第一周)")
    Log.d("switchWeek", "下箭头enabled: ${!isLastWeek} (不是最后一周)")

    Log.d("clickEvent", "=== 按钮状态计算（clickEvent标签） ===")
    Log.d("clickEvent", "计算输入参数:")
    Log.d("clickEvent", "  - currentWeekOffset: $currentWeekOffset")
    Log.d("clickEvent", "  - earliestTaskDate: $earliestTaskDate")
    Log.d("clickEvent", "  - currentWeekData.weekNumber: ${currentWeekData.weekNumber}")
    Log.d("clickEvent", "计算结果:")
    Log.d("clickEvent", "  - maxWeek: $maxWeek")
    Log.d("clickEvent", "  - isFirstWeek: $isFirstWeek (currentWeekOffset == 0)")
    Log.d("clickEvent", "  - isLastWeek: $isLastWeek (${currentWeekData.weekNumber} >= $maxWeek)")
    Log.d("clickEvent", "按钮状态:")
    Log.d("clickEvent", "  - 上箭头enabled: ${!isFirstWeek}")
    Log.d("clickEvent", "  - 下箭头enabled: ${!isLastWeek}")
    Log.d("clickEvent", "按钮可视状态:")
    Log.d("clickEvent", "  - 上箭头透明度: ${if (isFirstWeek) "0.3 (灰色禁用)" else "1.0 (正常)"}")
    Log.d("clickEvent", "  - 下箭头透明度: ${if (isLastWeek) "0.3 (灰色禁用)" else "1.0 (正常)"}")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(cardHeight)
            .padding(12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF71CBF4)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 20.dp, start = 20.dp, end = 20.dp, bottom = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) {
                // 当前周数区域 (45% 高度)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.45f)
                ) {
                    // 周数数字
                    Text(
                        text = "Week ${currentWeekData.weekNumber}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.TopStart)
                    )

                    // 右侧日期和箭头区域
                    Column(
                        modifier = Modifier.align(Alignment.BottomEnd),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // 向上箭头（前一周）
                        IconButton(
                            onClick = {
                                Log.d("clickEvent", "=== 向上箭头点击事件开始 ===")
                                Log.d("clickEvent", "事件时间: ${System.currentTimeMillis()}")
                                Log.d("clickEvent", "点击组件: 上箭头（向前一周）")
                                Log.d("clickEvent", "当前周偏移量: $currentWeekOffset")
                                Log.d("clickEvent", "计算的新周偏移量: ${currentWeekOffset - 1}")
                                Log.d("clickEvent", "按钮状态检查:")
                                Log.d("clickEvent", "  - 是否为第一周: $isFirstWeek")
                                Log.d("clickEvent", "  - 按钮enabled状态: ${!isFirstWeek}")
                                Log.d("clickEvent", "  - 当前周数: ${currentWeekData.weekNumber}")
                                Log.d("clickEvent", "  - 最大周数: $maxWeek")

                                if (!isFirstWeek) {
                                    Log.d("clickEvent", "按钮可点击，准备调用 onWeekChanged()")
                                    Log.d("clickEvent", "回调函数参数: ${currentWeekOffset - 1}")
                                    onWeekChanged(currentWeekOffset - 1)
                                    Log.d("clickEvent", "onWeekChanged() 调用完成")
                                } else {
                                    Log.w("clickEvent", "按钮不可点击状态下被触发！这可能是个问题")
                                }

                                Log.d("clickEvent", "=== 向上箭头点击事件结束 ===")
                            },
                            enabled = !isFirstWeek, // 修复：不是第一周才能点击向上
                            modifier = Modifier
                                .size(40.dp)
                                .offset(y = 4.dp) // 向下偏移，紧贴日期
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "上一周",
                                tint = if (isFirstWeek) Color.White.copy(alpha = 0.3f) else Color.White, // 修复：第一周时变灰
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // 日期范围说明
                        Text(
                            text = currentWeekData.dateRange,
                            fontSize = 14.sp,
                            color = Color.White,
                            modifier = Modifier.padding(vertical = 0.dp) // 移除默认垂直内边距
                        )

                        // 向下箭头（后一周）
                        IconButton(
                            onClick = {
                                Log.d("clickEvent", "=== 向下箭头点击事件开始 ===")
                                Log.d("clickEvent", "事件时间: ${System.currentTimeMillis()}")
                                Log.d("clickEvent", "点击组件: 下箭头（向后一周）")
                                Log.d("clickEvent", "当前周偏移量: $currentWeekOffset")
                                Log.d("clickEvent", "计算的新周偏移量: ${currentWeekOffset + 1}")
                                Log.d("clickEvent", "按钮状态检查:")
                                Log.d("clickEvent", "  - 是否为最后一周: $isLastWeek")
                                Log.d("clickEvent", "  - 按钮enabled状态: ${!isLastWeek}")
                                Log.d("clickEvent", "  - 当前周数: ${currentWeekData.weekNumber}")
                                Log.d("clickEvent", "  - 最大周数: $maxWeek")

                                if (!isLastWeek) {
                                    Log.d("clickEvent", "按钮可点击，准备调用 onWeekChanged()")
                                    Log.d("clickEvent", "回调函数参数: ${currentWeekOffset + 1}")
                                    onWeekChanged(currentWeekOffset + 1)
                                    Log.d("clickEvent", "onWeekChanged() 调用完成")
                                } else {
                                    Log.w("clickEvent", "按钮不可点击状态下被触发！这可能是个问题")
                                }

                                Log.d("clickEvent", "=== 向下箭头点击事件结束 ===")
                            },
                            enabled = !isLastWeek, // 修复：不是最后一周才能点击向下
                            modifier = Modifier
                                .size(40.dp)
                                .offset(y = (-4).dp) // 向上偏移，紧贴日期
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "下一周",
                                tint = if (isLastWeek) Color.White.copy(alpha = 0.3f) else Color.White, // 修复：最后一周时变灰
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                // 间隔 (5% 高度)
                Spacer(modifier = Modifier.fillMaxHeight(0.05f))

                // 任务完成状态可视区域 (15% 高度)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.15f)
                ) {
                    TaskProgressBar(
                        pendingCount = currentWeekData.pendingCount,
                        completedCount = currentWeekData.completedCount,
                        overdueCount = currentWeekData.overdueCount,
                        cancelledCount = currentWeekData.cancelledCount,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // 添加一个励志文字：
            Row (
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.35f)
                    .align(Alignment.BottomStart) // 关键：强制对齐到Box底部
            ){
                Text(
                    text = "今日长枪在手，何时缚住苍龙",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                )

                Spacer(modifier = Modifier.weight(1f))


                // 右下角视图切换组件
                Box {
                    CompactViewSwitchSection(
                        selectedView = selectedView,
                        onViewSelected = onViewSelected
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskProgressBar(
    pendingCount: Int,
    completedCount: Int,
    overdueCount: Int,
    cancelledCount: Int,
    modifier: Modifier = Modifier
) {
    val totalCount = pendingCount + completedCount + overdueCount + cancelledCount

    if (totalCount == 0) {
        // 空状态 - 显示浅蓝背景
        Box(
            modifier = modifier
                .background(
                    color = Color(0xFFA9E0FF),
                    shape = RoundedCornerShape(8.dp)
                )
        )
        return
    }

    Row(
        modifier = modifier
            .background(
                color = Color(0xFFA9E0FF),
                shape = RoundedCornerShape(8.dp)
            )
            .clip(RoundedCornerShape(8.dp))
    ) {
        // 未完成段 - 蓝色
        if (pendingCount > 0) {
            Box(
                modifier = Modifier
                    .weight(pendingCount.toFloat())
                    .fillMaxHeight()
                    .background(Color(0xFF42A5F5))
            )
        }

        // 已完成段 - 绿色
        if (completedCount > 0) {
            Box(
                modifier = Modifier
                    .weight(completedCount.toFloat())
                    .fillMaxHeight()
                    .background(Color(0xFF66BB6A))
            )
        }

        // 延期段 - 橙色
        if (overdueCount > 0) {
            Box(
                modifier = Modifier
                    .weight(overdueCount.toFloat())
                    .fillMaxHeight()
                    .background(Color(0xFFFFA726))
            )
        }

        // 放弃段 - 红色
        if (cancelledCount > 0) {
            Box(
                modifier = Modifier
                    .weight(cancelledCount.toFloat())
                    .fillMaxHeight()
                    .background(Color(0xFFEF5350))
            )
        }
    }
}

data class WeekData(
    val weekNumber: Int,
    val dateRange: String,
    val pendingCount: Int,
    val completedCount: Int,
    val overdueCount: Int,
    val cancelledCount: Int
)

private fun calculateCurrentWeekData(tasks: List<Task>, earliestTaskDate: LocalDate?, weekOffset: Int = 0): WeekData {
    Log.d("calculateCurrentWeekData", "=== calculateCurrentWeekData() 开始 ===")
    Log.d("calculateCurrentWeekData", "传入参数 - 任务数量: ${tasks.size}, 最早日期: $earliestTaskDate, 周偏移: $weekOffset")
    Log.d("clickEvent", "calculateCurrentWeekData() 计算开始:")
    Log.d("clickEvent", "  - 输入任务数量: ${tasks.size}")
    Log.d("clickEvent", "  - 最早任务日期: $earliestTaskDate")
    Log.d("clickEvent", "  - 使用的周偏移: $weekOffset")

    val today = LocalDate.now()
    val currentWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val targetWeekStart = currentWeekStart.plusWeeks(weekOffset.toLong())
    val endOfWeek = targetWeekStart.plusDays(6)

    Log.d("calculateCurrentWeekData", "今天日期: $today")
    Log.d("calculateCurrentWeekData", "当前周的周一: $currentWeekStart")
    Log.d("calculateCurrentWeekData", "目标周的周一: $targetWeekStart")
    Log.d("calculateCurrentWeekData", "目标周的周日: $endOfWeek")

    // 计算周数：从数据库第一条任务的周开始为第一周
    val weekNumber = if (earliestTaskDate != null) {
        val earliestWeekStart = earliestTaskDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weeksBetweenEarliestAndTarget = ChronoUnit.WEEKS.between(earliestWeekStart, targetWeekStart)
        Log.d("weekCount", "最早任务所在周的周一: $earliestWeekStart")
        Log.d("weekCount", "最早周到目标周的周数差: $weeksBetweenEarliestAndTarget")
        val calculatedWeek = (weeksBetweenEarliestAndTarget + 1).toInt()
        Log.d("weekCount", "计算得出的周数: $calculatedWeek")
        calculatedWeek
    } else {
        Log.d("weekCount", "没有最早任务日期，默认周数: 1")
        1
    }

    // 格式化日期范围
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy.M.d")
    val dateRange = "${targetWeekStart.format(dateFormatter)} - ${endOfWeek.format(dateFormatter)}"

    // 筛选当前周的任务
    val currentWeekTasks = tasks.filter { task ->
        val taskDate = task.createdAt.toLocalDate()
        val inWeek = !taskDate.isBefore(targetWeekStart) && !taskDate.isAfter(endOfWeek)
        Log.d("calculateCurrentWeekData", "任务: ${task.title}, 日期: $taskDate, 在目标周: $inWeek")
        inWeek
    }

    Log.d("calculateCurrentWeekData", "过滤后的任务数量: ${currentWeekTasks.size}")

    // 统计各状态任务数量（5种状态）
    val pendingCount = currentWeekTasks.count { it.status == TaskStatus.PENDING }
    val completedCount = currentWeekTasks.count { it.status == TaskStatus.COMPLETED }
    val deferredCount = currentWeekTasks.count { it.status == TaskStatus.DELAYED }
    val overdueCount = currentWeekTasks.count { it.status == TaskStatus.OVERDUE }
    val cancelledCount = currentWeekTasks.count { it.status == TaskStatus.CANCELLED }

    val weekData = WeekData(
        weekNumber = weekNumber,
        dateRange = dateRange,
        pendingCount = pendingCount,
        completedCount = completedCount,
        overdueCount = overdueCount,
        cancelledCount = cancelledCount
    )

    Log.d("calculateCurrentWeekData", "返回结果: $weekData")
    Log.d("calculateCurrentWeekData", "=== calculateCurrentWeekData() 结束 ===")
    Log.d("clickEvent", "calculateCurrentWeekData() 计算完成:")
    Log.d("clickEvent", "  - 返回周数: ${weekData.weekNumber}")
    Log.d("clickEvent", "  - 返回日期范围: ${weekData.dateRange}")
    Log.d("clickEvent", "  - 返回任务统计: 待办${weekData.pendingCount}, 完成${weekData.completedCount}, 逾期${weekData.overdueCount}, 取消${weekData.cancelledCount}")

    return weekData
}

private fun getCurrentMaxWeek(earliestTaskDate: LocalDate?): Int {
    Log.d("weekCount", "=== 最大周数计算调试信息 ===")
    Log.d("clickEvent", "getCurrentMaxWeek() 计算开始:")
    Log.d("clickEvent", "  - 输入最早任务日期: $earliestTaskDate")
    if (earliestTaskDate == null) {
        Log.d("weekCount", "没有最早任务日期，最大周数: 1")
        Log.d("clickEvent", "  - 无最早日期，返回默认最大周数: 1")
        return 1
    }

    val today = LocalDate.now()
    val currentWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val earliestWeekStart = earliestTaskDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weeksBetween = ChronoUnit.WEEKS.between(earliestWeekStart, currentWeekStart)

    Log.d("weekCount", "最早任务日期: $earliestTaskDate")
    Log.d("weekCount", "最早任务所在周的周一: $earliestWeekStart")
    Log.d("weekCount", "当前周的周一: $currentWeekStart")
    Log.d("weekCount", "最早周到当前周的周数差: $weeksBetween")

    // 返回从第一周到当前周的总周数
    val maxWeek = (weeksBetween + 1).toInt()
    Log.d("weekCount", "计算得出的最大周数: $maxWeek")
    Log.d("clickEvent", "getCurrentMaxWeek() 计算完成:")
    Log.d("clickEvent", "  - 最早任务所在周的周一: $earliestWeekStart")
    Log.d("clickEvent", "  - 当前周的周一: $currentWeekStart")
    Log.d("clickEvent", "  - 周数差: $weeksBetween")
    Log.d("clickEvent", "  - 计算得出的最大周数: $maxWeek")
    return maxWeek
}

@Composable
private fun CompactViewSwitchSection(
    selectedView: TaskView,
    onViewSelected: (TaskView) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                Color.White.copy(alpha = 0.9f),
                RoundedCornerShape(12.dp)
            )
            .padding(4.dp)
    ) {
        TaskView.values().forEach { view ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (selectedView == view)
                            Primary
                        else
                            Color.Transparent
                    )
                    .clickable { onViewSelected(view) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = view.title,
                    color = if (selectedView == view)
                        Color.White
                    else
                        Primary,
                    fontSize = 13.sp,
                    fontWeight = if (selectedView == view)
                        FontWeight.SemiBold
                    else
                        FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun ViewSwitchSection(
    selectedView: TaskView,
    onViewSelected: (TaskView) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Color.White.copy(alpha = 0.9f),
                RoundedCornerShape(16.dp)
            )
            .padding(6.dp)
    ) {
        TaskView.values().forEach { view ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (selectedView == view)
                            Primary
                        else
                            Color.Transparent
                    )
                    .clickable { onViewSelected(view) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = view.title,
                    color = if (selectedView == view)
                        Color.White
                    else
                        Primary,
                    fontSize = 15.sp,
                    fontWeight = if (selectedView == view)
                        FontWeight.SemiBold
                    else
                        FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun UnifiedTopSection(
    selectedView: TaskView,
    onViewSelected: (TaskView) -> Unit,
    currentMonth: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    totalTasks: Int,
    completedTasks: Int,
    completionRate: Float
) {
    // 统一的渐变背景 - 消除分离感
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Primary.copy(alpha = 0.9f),
                        Primary.copy(alpha = 0.7f),
                        Primary.copy(alpha = 0.5f)
                    ),
                    startY = 0f,
                    endY = 800f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // 主标题和操作区域
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧：标题信息
                Column {
                    Text(
                        text = if (selectedView == TaskView.LIST) "任务流水" else currentMonth,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = if (totalTasks > 0) {
                            "共 $totalTasks 个任务，已完成 $completedTasks 个"
                        } else {
                            "还没有添加任何任务"
                        },
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // 右侧：操作和进度
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 日历视图的月份导航
                    if (selectedView == TaskView.CALENDAR) {
                        IconButton(
                            onClick = onPreviousMonth,
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    Color.White.copy(alpha = 0.2f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                painter = painterResource(id = android.R.drawable.ic_media_previous),
                                contentDescription = "上个月",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = onNextMonth,
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    Color.White.copy(alpha = 0.2f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                painter = painterResource(id = android.R.drawable.ic_media_next),
                                contentDescription = "下个月",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    // 完成率圆环
                    Box(
                        modifier = Modifier.size(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = completionRate,
                            modifier = Modifier.size(56.dp),
                            strokeWidth = 5.dp,
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.25f)
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${(completionRate * 100).toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 视图切换 - 整合到同一背景
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color.White.copy(alpha = 0.15f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(6.dp)
            ) {
                TaskView.values().forEach { view ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selectedView == view)
                                    Color.White
                                else
                                    Color.Transparent
                            )
                            .clickable { onViewSelected(view) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = view.title,
                            color = if (selectedView == view)
                                Primary
                            else
                                Color.White.copy(alpha = 0.9f),
                            fontSize = 15.sp,
                            fontWeight = if (selectedView == view)
                                FontWeight.SemiBold
                            else
                                FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun TasksListView(
    taskGroups: List<TaskGroup>,
    onTaskClick: (Task) -> Unit,
    onToggleStatus: ((String) -> Unit)? = null,
    onDefer: ((String) -> Unit)? = null,
    onCancel: ((String) -> Unit)? = null
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
    ) {
        items(taskGroups) { group ->
            TaskGroupItem(
                group = group,
                onTaskClick = onTaskClick,
                onToggleStatus = onToggleStatus,
                onDefer = onDefer,
                onCancel = onCancel
            )
        }
    }
}

@Composable
private fun TaskGroupItem(
    group: TaskGroup,
    onTaskClick: (Task) -> Unit,
    onToggleStatus: ((String) -> Unit)? = null,
    onDefer: ((String) -> Unit)? = null,
    onCancel: ((String) -> Unit)? = null
) {
    Column {
        // 日期标题 - 更简洁的设计
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatDateDisplay(group.date),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Primary.copy(alpha = 0.1f)
            ) {
                Text(
                    text = "${group.completedCount}/${group.totalCount}",
                    color = Primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Column {
            group.tasks.forEach { task ->
                TaskListItem(
                    task = task,
                    onClick = { onTaskClick(task) },
                    onToggleStatus = onToggleStatus?.let { { it(task.id) } },
                    onDefer = onDefer?.let { { it(task.id) } },
                    onCancel = onCancel?.let { { it(task.id) } }
                )
            }
        }
    }
}

// 日期格式化函数
private fun formatDateDisplay(dateString: String): String {
    return try {
        val today = java.time.LocalDate.now()
        val taskDate = java.time.LocalDate.parse(dateString)

        when {
            taskDate == today -> "今天"
            taskDate == today.minusDays(1) -> "昨天"
            taskDate == today.plusDays(1) -> "明天"
            taskDate.year == today.year -> {
                val formatter = DateTimeFormatter.ofPattern("MM月dd日")
                taskDate.format(formatter)
            }
            else -> {
                val formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日")
                taskDate.format(formatter)
            }
        }
    } catch (e: Exception) {
        dateString
    }
}

// 日历选定日期格式化结果（包含文本和字体大小）
data class DateDisplayInfo(
    val text: String,
    val fontSize: Int  // 单位：sp
)

// 日历选定日期格式化函数（仅显示日期，不显示"今天"等相对时间）
private fun formatSelectedDateDisplay(dateString: String): DateDisplayInfo {
    return try {
        val today = java.time.LocalDate.now()
        val selectedDate = java.time.LocalDate.parse(dateString)

        when {
            // 本月或跨月但同年：显示"x月x日" - 使用 18sp
            selectedDate.year == today.year -> {
                DateDisplayInfo("${selectedDate.monthValue}月${selectedDate.dayOfMonth}日", 18)
            }
            // 跨年：显示"yy年x月x日"（缩写年份）- 使用 16sp
            else -> {
                val shortYear = selectedDate.year % 100  // 取后两位
                DateDisplayInfo("${shortYear}年${selectedDate.monthValue}月${selectedDate.dayOfMonth}日", 16)
            }
        }
    } catch (e: Exception) {
        DateDisplayInfo(dateString, 18)
    }
}

@Composable
private fun TaskListItem(
    task: Task,
    onClick: () -> Unit,
    onToggleStatus: (() -> Unit)? = null,
    onDefer: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        TaskItemCard(
            task = task,
            onClick = onClick,
            showSwipeActions = true,
            onToggleStatus = onToggleStatus,
            onPostpone = onDefer,
            onCancel = onCancel
        )

        // 分割线
        HorizontalDivider(
            thickness = 1.dp,
            color = Color(0xFFE0E0E0)
        )
    }
}

@Composable
private fun TasksCalendarView(
    calendarDays: List<CalendarDay>,
    selectedDate: String?,
    selectedDateTasks: List<Task>,
    selectedDateCompletedCount: Int,
    selectedDatePendingCount: Int,
    selectedDateOverdueCount: Int,
    selectedDateCancelledCount: Int,
    onDateSelected: (String) -> Unit,
    onNavigateToTaskDetail: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        // 星期标题
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEach { day ->
                    Text(
                        text = day,
                        fontSize = 12.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 日历网格 - 使用普通网格布局
        item {
            CalendarGrid(
                calendarDays = calendarDays,
                selectedDate = selectedDate,
                onDateSelected = onDateSelected
            )
        }

        // 选定日期详情
        selectedDate?.let { date ->
            item {
                SelectedDateDetailCard(
                    selectedDate = date,
                    tasks = selectedDateTasks,
                    completedCount = selectedDateCompletedCount,
                    pendingCount = selectedDatePendingCount,
                    overdueCount = selectedDateOverdueCount,
                    cancelledCount = selectedDateCancelledCount,
                    onTaskClick = { task -> onNavigateToTaskDetail(task.id) }
                )
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    calendarDays: List<CalendarDay>,
    selectedDate: String?,
    onDateSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // 按每行7个分组
        calendarDays.chunked(7).forEach { weekDays ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                weekDays.forEach { day ->
                    Box(modifier = Modifier.weight(1f)) {
                        CalendarDayItem(
                            day = day,
                            isSelected = selectedDate == day.date,
                            onClick = { onDateSelected(day.date) }
                        )
                    }
                }
                // 填充空白（如果该行不足7天）
                repeat(7 - weekDays.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(1.dp))
        }
    }
}

@Composable
private fun CalendarDayItem(
    day: CalendarDay,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 背景填充：按任务状态比例或默认颜色
            if (day.hasTask) {
                // 有任务：按比例填充整个格子
                TaskStatusBackgroundFill(
                    pendingCount = day.pendingCount,
                    completedCount = day.completedCount,
                    overdueCount = day.overdueCount,
                    cancelledCount = day.cancelledCount,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // 无任务：按是否为当前周设置背景色
                val backgroundColor = when {
                    !day.isCurrentWeek -> Color.White  // 非当前周为白色
                    day.isCurrentWeek -> Success.copy(alpha = 0.3f)  // 无任务当前周为淡绿色
                    else -> BgCard
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            backgroundColor,
                            RoundedCornerShape(8.dp)
                        )
                )
            }

            // 选中状态的覆盖层
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Primary.copy(alpha = 0.8f),
                            RoundedCornerShape(8.dp)
                        )
                )
            }

            // 文字内容
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = day.dayNumber,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) Color.White else TextPrimary
                )
            }
        }
    }
}

@Composable
private fun TaskStatusBackgroundFill(
    pendingCount: Int,
    completedCount: Int,
    overdueCount: Int,
    cancelledCount: Int,
    modifier: Modifier = Modifier
) {
    val totalCount = pendingCount + completedCount + overdueCount + cancelledCount

    if (totalCount == 0) return

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
    ) {
        // 放弃任务 - 红色（最上方）
        if (cancelledCount > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(cancelledCount.toFloat())
                    .background(Color(0xFFEF5350))
            )
        }

        // 延期任务 - 橙色
        if (overdueCount > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(overdueCount.toFloat())
                    .background(Color(0xFFFFA726))
            )
        }

        // 未完成任务 - 蓝色
        if (pendingCount > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(pendingCount.toFloat())
                    .background(Color(0xFF42A5F5))
            )
        }

        // 已完成任务 - 绿色（最下方）
        if (completedCount > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(completedCount.toFloat())
                    .background(Color(0xFF66BB6A))
            )
        }
    }
}

@Composable
private fun SelectedDateDetailCard(
    selectedDate: String,
    tasks: List<Task>,
    completedCount: Int,
    pendingCount: Int,
    overdueCount: Int,
    cancelledCount: Int,
    onTaskClick: (Task) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 顶部概览
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Primary.copy(alpha = 0.05f))
                    .padding(16.dp)
            ) {
                // 日期 - 紧贴左侧（动态字体大小）
                val dateInfo = remember(selectedDate) {
                    formatSelectedDateDisplay(selectedDate)
                }
                Text(
                    text = dateInfo.text,
                    fontSize = dateInfo.fontSize.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = (-16).dp) // 抵消 Box 的左侧 padding，使其紧贴左边缘
                )

                // 统计标签 - 紧贴右侧
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = 16.dp) // 抵消 Box 的右侧 padding，使其紧贴右边缘
                ) {
                    // 完成
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Success.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "完成 ${completedCount}件",
                            color = Success,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }

                    // 未完成
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "未完成 ${pendingCount}件",
                            color = Primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }

                    // 逾期
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Warning.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "逾期 ${overdueCount}件",
                            color = Warning,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }

                    // 放弃
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Danger.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "放弃 ${cancelledCount}件",
                            color = Danger,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // 任务列表或空状态
            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "无任务",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                }
            } else {
                // 任务列表 - 普通 Column，由外层 LazyColumn 提供滚动
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    tasks.forEach { task ->
                        TaskItemCard(
                            task = task,
                            onClick = { onTaskClick(task) },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskTabs(selectedTab: TaskTab, onTabSelected: (TaskTab) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgPrimary)
        ) {
            TaskTab.values().forEach { tab ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTabSelected(tab) }
                        .background(
                            if (selectedTab == tab) BgCard else Color.Transparent
                        )
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.title,
                        color = if (selectedTab == tab) Primary else TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}