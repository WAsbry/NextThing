package com.example.nextthingb1.presentation.screens.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nextthingb1.domain.model.Task
import com.example.nextthingb1.domain.model.TaskCategory
import com.example.nextthingb1.domain.model.TaskPriority
import com.example.nextthingb1.domain.model.TaskStatus

import com.example.nextthingb1.presentation.theme.*

@Composable
fun TodayScreen(
    viewModel: TodayViewModel = hiltViewModel(),
    onNavigateToFocus: () -> Unit,
    onNavigateToTaskDetail: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
        ) {
        // 头部导航
        TopHeader(
            uiState = uiState,
            viewModel = viewModel
        )
        
        // 内容区域
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            item {
                // 进度概览卡片
                ProgressOverviewCard(
                    completionRate = uiState.completionRate,
                    totalTasks = uiState.totalTasks,
                    completedTasks = uiState.completedTasks,
                    remainingTasks = uiState.remainingTasks
                )
            }
            
            item {
                // 今日任务标题
                TodaySectionHeader(
                    completedCount = uiState.completedTasks,
                    pendingCount = uiState.remainingTasks
                )
            }
            
            item {
                // 任务标签页
                TaskTabs(
                    selectedTab = uiState.selectedTab,
                    onTabSelected = { viewModel.selectTab(it) }
                )
            }
            
            // 任务列表
            items(uiState.displayTasks) { task ->
                TaskItem(
                    task = task,
                    onToggleStatus = { viewModel.toggleTaskStatus(task.id) },
                    onPostpone = { viewModel.postponeTask(task.id) },
                    onCancel = { viewModel.cancelTask(task.id) },
                    onStartFocus = { onNavigateToFocus() },
                    onClick = { onNavigateToTaskDetail(task.id) }
                )
            }
        }
    }
}

@Composable
private fun LocationIcon(
    currentLocation: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable { onClick() }
            .background(
                Primary.copy(alpha = 0.1f),
                RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            painter = painterResource(id = android.R.drawable.ic_menu_mylocation),
            contentDescription = "当前位置",
            tint = Primary,
            modifier = Modifier.size(14.dp)
        )
        if (currentLocation.isNotBlank()) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = currentLocation,
                fontSize = 12.sp,
                color = Primary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TopHeader(
    uiState: TodayUiState,
    viewModel: TodayViewModel
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgCard)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 定位图标 - 可点击
            LocationIcon(
                currentLocation = uiState.currentLocationName,
                onClick = { viewModel.requestCurrentLocation() }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "NextThing",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
        
        IconButton(
            onClick = { /* TODO: 搜索功能 */ },
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(BgPrimary)
        ) {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_search),
                contentDescription = "搜索",
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun ProgressOverviewCard(
    completionRate: Float,
    totalTasks: Int,
    completedTasks: Int,
    remainingTasks: Int
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
                        colors = listOf(Color(0xFF81C784), Color(0xFF66BB6A))
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "今日完成",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    
                    Box(
                        modifier = Modifier
                            .background(
                                Color.White.copy(alpha = 0.2f),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "8月1日-8月31日",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "${(completionRate * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem("今日任务", "$totalTasks 项")
                    StatItem("已完成", "$completedTasks 项")
                    StatItem("剩余", "$remainingTasks 项")
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp
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
private fun TodaySectionHeader(completedCount: Int, pendingCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "今日任务",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        
        Row {
            Text(
                text = "完成 ${completedCount}项",
                color = Success,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(24.dp))
            Text(
                text = "待办 ${pendingCount}项",
                color = Danger,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun TaskTabs(selectedTab: TaskTab, onTabSelected: (TaskTab) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
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

@Composable
private fun TaskItem(
    task: Task,
    onToggleStatus: () -> Unit,
    onPostpone: () -> Unit,
    onCancel: () -> Unit,
    onStartFocus: () -> Unit,
    onClick: () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val actionWidth = 80.dp
    val actionWidthPx = with(LocalDensity.current) { actionWidth.toPx() }
    val maxOffset = actionWidthPx * 3 // 三个操作按钮的总宽度
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        // 背景操作按钮
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .height(80.dp)
        ) {
            // 完成按钮
            Box(
                modifier = Modifier
                    .width(actionWidth)
                    .fillMaxHeight()
                    .background(Success)
                    .clickable { 
                        onToggleStatus()
                        offsetX = 0f
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = android.R.drawable.checkbox_on_background),
                    contentDescription = "完成",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // 延期按钮
            Box(
                modifier = Modifier
                    .width(actionWidth)
                    .fillMaxHeight()
                    .background(Warning)
                    .clickable { 
                        onPostpone()
                        offsetX = 0f
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_recent_history),
                    contentDescription = "延期",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // 放弃按钮
            Box(
                modifier = Modifier
                    .width(actionWidth)
                    .fillMaxHeight()
                    .background(Danger)
                    .clickable { 
                        onCancel()
                        offsetX = 0f
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                    contentDescription = "放弃",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        // 主要内容卡片
        Card(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            // 决定滑动后的最终位置
                            offsetX = when {
                                offsetX < -maxOffset / 2 -> -maxOffset
                                offsetX > maxOffset / 2 -> 0f
                                else -> 0f
                            }
                        }
                    ) { _, dragAmount ->
                        val newOffset = offsetX + dragAmount
                        offsetX = newOffset.coerceIn(-maxOffset, 0f)
                    }
                },
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
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
                        color = TextPrimary,
                        textDecoration = if (task.status == TaskStatus.COMPLETED) TextDecoration.LineThrough else null
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 优先级标签
                        Box(
                            modifier = Modifier
                                .background(
                                    when (task.priority) {
                                        TaskPriority.HIGH -> Danger.copy(alpha = 0.1f)
                                        TaskPriority.MEDIUM -> Warning.copy(alpha = 0.1f)
                                        TaskPriority.LOW -> Success.copy(alpha = 0.1f)
                                    },
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = when (task.priority) {
                                    TaskPriority.HIGH -> "高"
                                    TaskPriority.MEDIUM -> "中"
                                    TaskPriority.LOW -> "低"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = when (task.priority) {
                                    TaskPriority.HIGH -> Danger
                                    TaskPriority.MEDIUM -> Warning
                                    TaskPriority.LOW -> Success
                                }
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        task.dueDate?.let {
                            Text(
                                text = if (task.isUrgent) "距截止 1:20" else "14:00",
                                fontSize = 12.sp,
                                color = if (task.isUrgent) Danger else TextSecondary
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        
                        Text(
                            text = task.category.displayName,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // 任务状态
                Text(
                    text = when (task.status) {
                        TaskStatus.COMPLETED -> "已完成"
                        TaskStatus.IN_PROGRESS -> "进行中"
                        TaskStatus.CANCELLED -> "已取消"
                        TaskStatus.OVERDUE -> "已过期"
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
            }
        }
    }
} 