package com.example.nextthingb1.presentation.screens.taskdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nextthingb1.domain.model.Task
import com.example.nextthingb1.domain.model.TaskCategory
import com.example.nextthingb1.domain.model.TaskPriority
import com.example.nextthingb1.domain.model.TaskStatus
import com.example.nextthingb1.domain.model.Subtask
import com.example.nextthingb1.presentation.theme.*
import java.time.format.DateTimeFormatter

@Composable
fun TaskDetailScreen(
    taskId: String,
    onBackPressed: () -> Unit,
    onEditTask: () -> Unit = {},
    viewModel: TaskDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(taskId) {
        viewModel.loadTask(taskId)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
    ) {
        // 头部导航
        TaskDetailTopHeader(
            onBackPressed = onBackPressed,
            onEditTask = onEditTask,
            onDeleteTask = { viewModel.deleteTask() }
        )
        
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.task != null -> {
                TaskDetailContent(
                    task = uiState.task!!,
                    onToggleStatus = { viewModel.toggleTaskStatus() },
                    onToggleSubtask = { subtaskId -> viewModel.toggleSubtaskStatus(subtaskId) }
                )
            }
            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.errorMessage ?: "任务不存在",
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskDetailTopHeader(
    onBackPressed: () -> Unit,
    onEditTask: () -> Unit,
    onDeleteTask: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgCard)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackPressed) {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_revert),
                contentDescription = "返回",
                tint = TextPrimary
            )
        }
        
        Text(
            text = "任务详情",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        
        Row {
            IconButton(onClick = onEditTask) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_edit),
                    contentDescription = "编辑",
                    tint = TextPrimary
                )
            }
            IconButton(onClick = onDeleteTask) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_delete),
                    contentDescription = "删除",
                    tint = Danger
                )
            }
        }
    }
}

@Composable
private fun TaskDetailContent(
    task: Task,
    onToggleStatus: () -> Unit,
    onToggleSubtask: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 任务基本信息卡片
        item {
            TaskBasicInfoCard(
                task = task,
                onToggleStatus = onToggleStatus
            )
        }
        
        // 任务详细信息卡片
        item {
            TaskDetailsCard(task = task)
        }
        
        // 时间信息卡片
        item {
            TaskTimeInfoCard(task = task)
        }
        
        // 子任务卡片
        if (task.subtasks.isNotEmpty()) {
            item {
                SubtasksCard(
                    subtasks = task.subtasks,
                    onToggleSubtask = onToggleSubtask
                )
            }
        }
        
        // 标签卡片
        if (task.tags.isNotEmpty()) {
            item {
                TaskTagsCard(tags = task.tags)
            }
        }
        
        // 时长统计卡片
        item {
            TaskDurationCard(
                estimatedDuration = task.estimatedDuration,
                actualDuration = task.actualDuration
            )
        }
    }
}

@Composable
private fun TaskBasicInfoCard(
    task: Task,
    onToggleStatus: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 分类图标
                Box(
                    modifier = Modifier
                        .size(48.dp)
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
                        fontSize = 20.sp
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textDecoration = if (task.status == TaskStatus.COMPLETED) TextDecoration.LineThrough else null
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 优先级标签
                        Box(
                            modifier = Modifier
                                .background(
                                    when (task.priority) {
                                        TaskPriority.HIGH -> Danger.copy(alpha = 0.1f)
                                        TaskPriority.MEDIUM -> Warning.copy(alpha = 0.1f)
                                        TaskPriority.LOW -> Success.copy(alpha = 0.1f)
                                    },
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = when (task.priority) {
                                    TaskPriority.HIGH -> "高优先级"
                                    TaskPriority.MEDIUM -> "中优先级"
                                    TaskPriority.LOW -> "低优先级"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = when (task.priority) {
                                    TaskPriority.HIGH -> Danger
                                    TaskPriority.MEDIUM -> Warning
                                    TaskPriority.LOW -> Success
                                }
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // 状态标签
                        Box(
                            modifier = Modifier
                                .background(
                                    when (task.status) {
                                        TaskStatus.COMPLETED -> Success.copy(alpha = 0.1f)
                                        TaskStatus.IN_PROGRESS -> Primary.copy(alpha = 0.1f)
                                        TaskStatus.CANCELLED -> TextMuted.copy(alpha = 0.1f)
                                        TaskStatus.OVERDUE -> Danger.copy(alpha = 0.1f)
                                        TaskStatus.PENDING -> if (task.isUrgent) Danger.copy(alpha = 0.1f) else TextMuted.copy(alpha = 0.1f)
                                    },
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = when (task.status) {
                                    TaskStatus.COMPLETED -> "已完成"
                                    TaskStatus.IN_PROGRESS -> "进行中"
                                    TaskStatus.CANCELLED -> "已取消"
                                    TaskStatus.OVERDUE -> "已过期"
                                    TaskStatus.PENDING -> if (task.isUrgent) "紧急" else "待办"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = when (task.status) {
                                    TaskStatus.COMPLETED -> Success
                                    TaskStatus.IN_PROGRESS -> Primary
                                    TaskStatus.CANCELLED -> TextMuted
                                    TaskStatus.OVERDUE -> Danger
                                    TaskStatus.PENDING -> if (task.isUrgent) Danger else TextSecondary
                                }
                            )
                        }
                    }
                }
                
                // 完成按钮
                Button(
                    onClick = onToggleStatus,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (task.status == TaskStatus.COMPLETED) Success else Primary
                    )
                ) {
                    Text(
                        text = if (task.status == TaskStatus.COMPLETED) "标记未完成" else "标记完成"
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskDetailsCard(task: Task) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "任务详情",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (task.description.isNotBlank()) {
                Text(
                    text = task.description,
                    fontSize = 16.sp,
                    color = TextSecondary,
                    lineHeight = 24.sp
                )
            } else {
                Text(
                    text = "暂无描述",
                    fontSize = 16.sp,
                    color = TextMuted,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "分类",
                        fontSize = 14.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = task.category.displayName,
                        fontSize = 16.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Column {
                    Text(
                        text = "紧急程度",
                        fontSize = 14.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (task.isUrgent) "紧急" else "普通",
                        fontSize = 16.sp,
                        color = if (task.isUrgent) Danger else TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskTimeInfoCard(task: Task) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "时间信息",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm")
            
            TimeInfoItem(
                label = "创建时间",
                value = task.createdAt.format(dateFormatter)
            )
            
            TimeInfoItem(
                label = "更新时间",
                value = task.updatedAt.format(dateFormatter)
            )
            
            task.dueDate?.let { dueDate ->
                TimeInfoItem(
                    label = "截止时间",
                    value = dueDate.format(dateFormatter),
                    isHighlight = task.isUrgent
                )
            }
            
            task.completedAt?.let { completedAt ->
                TimeInfoItem(
                    label = "完成时间",
                    value = completedAt.format(dateFormatter),
                    isSuccess = true
                )
            }
        }
    }
}

@Composable
private fun TimeInfoItem(
    label: String,
    value: String,
    isHighlight: Boolean = false,
    isSuccess: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = TextMuted,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = when {
                isSuccess -> Success
                isHighlight -> Danger
                else -> TextSecondary
            },
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SubtasksCard(
    subtasks: List<Subtask>,
    onToggleSubtask: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "子任务 (${subtasks.count { it.isCompleted }}/${subtasks.size})",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            subtasks.forEach { subtask ->
                SubtaskItem(
                    subtask = subtask,
                    onToggle = { onToggleSubtask(subtask.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SubtaskItem(
    subtask: Subtask,
    onToggle: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Checkbox(
            checked = subtask.isCompleted,
            onCheckedChange = { onToggle() }
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = subtask.title,
            fontSize = 16.sp,
            color = if (subtask.isCompleted) TextMuted else TextPrimary,
            textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TaskTagsCard(tags: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "标签",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tags) { tag ->
                    Box(
                        modifier = Modifier
                            .background(
                                Primary.copy(alpha = 0.1f),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = tag,
                            fontSize = 14.sp,
                            color = Primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskDurationCard(
    estimatedDuration: Int,
    actualDuration: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "时长统计",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "预估时长",
                        fontSize = 14.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (estimatedDuration > 0) "${estimatedDuration}分钟" else "未设置",
                        fontSize = 16.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Column {
                    Text(
                        text = "实际时长",
                        fontSize = 14.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (actualDuration > 0) "${actualDuration}分钟" else "未记录",
                        fontSize = 16.sp,
                        color = when {
                            actualDuration == 0 -> TextMuted
                            estimatedDuration > 0 && actualDuration > estimatedDuration -> Danger
                            else -> Success
                        },
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            if (estimatedDuration > 0 && actualDuration > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                val efficiency = (estimatedDuration.toFloat() / actualDuration * 100).toInt()
                Text(
                    text = "效率: ${efficiency}%",
                    fontSize = 14.sp,
                    color = when {
                        efficiency >= 100 -> Success
                        efficiency >= 80 -> Primary
                        else -> Warning
                    },
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
} 