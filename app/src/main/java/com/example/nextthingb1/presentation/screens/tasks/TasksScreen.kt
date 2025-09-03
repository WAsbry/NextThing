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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nextthingb1.domain.model.Task
import com.example.nextthingb1.domain.model.TaskCategory
import com.example.nextthingb1.domain.model.TaskStatus
import com.example.nextthingb1.presentation.theme.*
import java.time.format.DateTimeFormatter

@Composable
fun TasksScreen(
    viewModel: TasksViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
        ) {
        // 视图切换标签
        ViewTabs(
            selectedView = uiState.selectedView,
            onViewSelected = { viewModel.selectView(it) }
        )
        
        // 月份导航
        MonthNavigation(
            currentMonth = uiState.currentMonth,
            onPreviousMonth = { viewModel.previousMonth() },
            onNextMonth = { viewModel.nextMonth() }
        )
        
        // 任务统计卡片
        TaskStatsCard(
            totalTasks = uiState.totalTasks,
            completedTasks = uiState.completedTasks,
            pendingTasks = uiState.pendingTasks,
            overdueTasks = uiState.overdueTasks,
            completionRate = uiState.completionRate
        )
        
        // 根据选择的视图显示不同内容
        when (uiState.selectedView) {
            TaskView.LIST -> {
                TasksListView(
                    taskGroups = uiState.taskGroups,
                    onTaskClick = { /* TODO */ }
                )
            }
            TaskView.CALENDAR -> {
                TasksCalendarView(
                    calendarDays = uiState.calendarDays,
                    selectedDate = uiState.selectedDate,
                    onDateSelected = { viewModel.selectDate(it) }
                )
            }
        }
    }
}

@Composable
private fun ViewTabs(
    selectedView: TaskView,
    onViewSelected: (TaskView) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgCard)
                .padding(8.dp)
        ) {
            TaskView.values().forEach { view ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (selectedView == view) Primary else Color.Transparent
                        )
                        .clickable { onViewSelected(view) }
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = view.title,
                        color = if (selectedView == view) Color.White else TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthNavigation(
    currentMonth: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPreviousMonth,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(BgPrimary)
            ) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_media_previous),
                    contentDescription = "上个月",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
            
            Text(
                text = currentMonth,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            
            IconButton(
                onClick = onNextMonth,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(BgPrimary)
            ) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_media_next),
                    contentDescription = "下个月",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun TaskStatsCard(
    totalTasks: Int,
    completedTasks: Int,
    pendingTasks: Int,
    overdueTasks: Int,
    completionRate: Float
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Primary, PrimaryDark)
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "任务概览",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 16.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "${(completionRate * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "完成率",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TaskStatItem("总任务", totalTasks.toString())
                    TaskStatItem("已完成", completedTasks.toString())
                    TaskStatItem("进行中", pendingTasks.toString())
                    TaskStatItem("已逾期", overdueTasks.toString())
                }
            }
        }
    }
}

@Composable
private fun TaskStatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun TasksListView(
    taskGroups: List<TaskGroup>,
    onTaskClick: (Task) -> Unit
) {
    LazyColumn {
        items(taskGroups) { group ->
            TaskGroupItem(
                group = group,
                onTaskClick = onTaskClick
            )
        }
    }
}

@Composable
private fun TaskGroupItem(
    group: TaskGroup,
    onTaskClick: (Task) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            // 日期头部
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgPrimary)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = group.date,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                
                Row {
                    Text(
                        text = "已完成 ${group.completedCount}",
                        color = Success,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "总计 ${group.totalCount}",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
            
            // 任务列表
            group.tasks.forEach { task ->
                TaskListItem(
                    task = task,
                    onClick = { onTaskClick(task) }
                )
            }
        }
    }
}

@Composable
private fun TaskListItem(
    task: Task,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 任务图标
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = when (task.category) {
                            TaskCategory.WORK -> listOf(Color(0xFF42A5F5), Color(0xFF1E88E5))
                            TaskCategory.STUDY -> listOf(Color(0xFFAB47BC), Color(0xFF8E24AA))
                            TaskCategory.LIFE -> listOf(Color(0xFF66BB6A), Color(0xFF4CAF50))
                            TaskCategory.HEALTH -> listOf(Color(0xFFE91E63), Color(0xFFC2185B))
                            TaskCategory.PERSONAL -> listOf(Color(0xFFFF9800), Color(0xFFF57C00))
                            TaskCategory.OTHER -> listOf(Color(0xFF9E9E9E), Color(0xFF757575))
                        }
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (task.category) {
                    TaskCategory.WORK -> "💼"
                    TaskCategory.STUDY -> "📚"
                    TaskCategory.LIFE -> "🏠"
                    TaskCategory.HEALTH -> "❤️"
                    TaskCategory.PERSONAL -> "👤"
                    TaskCategory.OTHER -> "📋"
                },
                fontSize = 16.sp
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // 任务内容
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            
            Text(
                text = task.description,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
        
        // 任务状态
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = when (task.status) {
                    TaskStatus.COMPLETED -> "已完成"
                    TaskStatus.IN_PROGRESS -> "进行中"
                    TaskStatus.CANCELLED -> "已取消"
                    TaskStatus.OVERDUE -> "已逾期"
                    TaskStatus.PENDING -> if (task.isUrgent) "紧急" else "待办"
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = when (task.status) {
                    TaskStatus.COMPLETED -> Success
                    TaskStatus.IN_PROGRESS -> Primary
                    TaskStatus.CANCELLED -> TextMuted
                    TaskStatus.OVERDUE -> Danger
                    TaskStatus.PENDING -> if (task.isUrgent) Danger else TextPrimary
                }
            )
            
            Text(
                text = task.category.displayName,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun TasksCalendarView(
    calendarDays: List<CalendarDay>,
    selectedDate: String?,
    onDateSelected: (String) -> Unit
) {
    Column {
        // 星期标题
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("日", "一", "二", "三", "四", "五", "六").forEach { day ->
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
        
        // 日历网格
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(1.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            items(calendarDays) { day ->
                CalendarDayItem(
                    day = day,
                    isSelected = selectedDate == day.date,
                    onClick = { onDateSelected(day.date) }
                )
            }
        }
        
        // 今日详情
        selectedDate?.let {
            TodayDetailCard(selectedDate = it)
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
            containerColor = when {
                isSelected -> Primary
                day.hasTask -> Success.copy(alpha = 0.1f)
                else -> BgCard
            }
        )
    ) {
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
            
            if (day.hasTask) {
                Text(
                    text = day.taskCount.toString(),
                    fontSize = 10.sp,
                    color = if (isSelected) Color.White else TextSecondary
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(
                            if (day.taskCount > 3) Success else Primary,
                            RoundedCornerShape(2.dp)
                        )
                )
            }
        }
    }
}

@Composable
private fun TodayDetailCard(selectedDate: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedDate,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                
                Row {
                    Text(
                        text = "已完成 0个",
                        color = Success,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "待办 0个",
                        color = Primary,
                        fontSize = 14.sp
                    )
                }
            }
            
            // 空状态
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "📋",
                    fontSize = 48.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "当天没有任何任务哦",
                    color = TextMuted,
                    fontSize = 14.sp
                )
            }
        }
    }
} 