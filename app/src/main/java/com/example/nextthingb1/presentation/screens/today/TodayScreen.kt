package com.example.nextthingb1.presentation.screens.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import com.example.nextthingb1.LocalPermissionLauncher
import com.example.nextthingb1.presentation.components.LocationDetailDialog
import com.example.nextthingb1.presentation.components.LocationHelpDialog
import com.example.nextthingb1.presentation.components.LocationPermissionDialog
import com.example.nextthingb1.presentation.theme.*
import com.example.nextthingb1.util.PermissionHelper
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.style.TextAlign

@Composable
fun TodayScreen(
    viewModel: TodayViewModel = hiltViewModel(),
    onNavigateToFocus: () -> Unit,
    onNavigateToTaskDetail: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val showPermissionDialog by viewModel.showPermissionDialog.collectAsState()
    val showLocationDetailDialog by viewModel.showLocationDetailDialog.collectAsState()
    val showLocationHelpDialog by viewModel.showLocationHelpDialog.collectAsState()
    val permissionLauncher = LocalPermissionLauncher.current
    
    // 当屏幕可见时刷新位置信息
    LaunchedEffect(Unit) {
        viewModel.onScreenResumed()
        
        // 定期检查权限状态变化（降低频率避免性能问题）
        while (true) {
            kotlinx.coroutines.delay(5000) // 5秒检查一次
            viewModel.forceCheckPermissionsAndRefresh()
        }
    }
    
    // 处理权限请求结果
    LaunchedEffect(uiState.hasLocationPermission) {
        if (uiState.hasLocationPermission) {
            viewModel.hidePermissionDialog()
            // 权限授予后只更新状态，不自动获取位置
            kotlinx.coroutines.delay(500)
            viewModel.forceCheckPermissionsAndRefresh()
        }
    }
    
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
            items(
                items = uiState.displayTasks,
                key = { task -> task.id }
            ) { task ->
                TaskItem(
                    task = task,
                    onToggleStatus = { viewModel.toggleTaskStatus(task.id) },
                    onPostpone = { viewModel.postponeTask(task.id) },
                    onCancel = { viewModel.cancelTask(task.id) },
                    onStartFocus = { onNavigateToFocus() },
                    onClick = { onNavigateToTaskDetail(task.id) }
                )
            }
            
            // 底部间距
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
    
    // 位置权限对话框
    LocationPermissionDialog(
        isVisible = showPermissionDialog,
        onDismiss = { viewModel.hidePermissionDialog() },
        onRequestPermission = {
            viewModel.hidePermissionDialog()
            permissionLauncher?.launch(PermissionHelper.LOCATION_PERMISSIONS)
        },
        onOpenSettings = {
            viewModel.hidePermissionDialog()
            // TODO: 打开设置页面
        },
        onPermissionGranted = {
            // 权限授予后的回调
            GlobalScope.launch {
                kotlinx.coroutines.delay(1000)
                viewModel.forceCheckPermissionsAndRefresh()
            }
        }
    )
    
    // 位置详情对话框
    LocationDetailDialog(
        isVisible = showLocationDetailDialog,
        location = uiState.currentLocation,
        isLoading = uiState.isLocationLoading,
        onDismiss = { viewModel.hideLocationDetailDialog() },
        onRefresh = { viewModel.requestCurrentLocation() }
    )
    
    // 位置帮助对话框
    LocationHelpDialog(
        isVisible = showLocationHelpDialog,
        errorMessage = uiState.locationError,
        onDismiss = { viewModel.hideLocationHelpDialog() },
        onRetry = { viewModel.requestCurrentLocation() },
        onOpenSettings = {
            viewModel.hideLocationHelpDialog()
            // TODO: 打开位置设置页面
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LocationIcon(
    currentLocation: String,
    isLoading: Boolean,
    hasPermission: Boolean,
    isLocationEnabled: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .background(
                when {
                    !hasPermission -> Danger.copy(alpha = 0.1f)
                    !isLocationEnabled -> Warning.copy(alpha = 0.1f)
                    isLoading -> Primary.copy(alpha = 0.2f)
                    else -> Primary.copy(alpha = 0.1f)
                },
                RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.dp,
                color = when {
                    !hasPermission -> Danger.copy(alpha = 0.4f)
                    !isLocationEnabled -> Warning.copy(alpha = 0.4f)
                    isLoading -> Primary.copy(alpha = 0.6f)
                    else -> Primary.copy(alpha = 0.4f)
                },
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = Primary
            )
        } else {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_mylocation),
                contentDescription = "当前位置",
                tint = when {
                    !hasPermission -> Danger
                    !isLocationEnabled -> Warning
                    else -> Primary
                },
                modifier = Modifier.size(16.dp)
            )
        }
        
        if (currentLocation.isNotBlank()) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = currentLocation,
                fontSize = 13.sp,
                color = when {
                    !hasPermission -> Danger
                    !isLocationEnabled -> Warning
                    isLoading -> Primary
                    else -> TextSecondary
                },
                maxLines = 1,
                modifier = Modifier.widthIn(max = 120.dp)
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
            Text(
                text = "NextThing",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            // 定位图标 - 可点击
            LocationIcon(
                currentLocation = uiState.currentLocationName,
                isLoading = uiState.isLocationLoading,
                hasPermission = uiState.hasLocationPermission,
                isLocationEnabled = uiState.isLocationEnabled,
                onClick = { 
                    if (!uiState.hasLocationPermission) {
                        viewModel.requestLocationPermission()
                    } else if (uiState.currentLocation != null && !uiState.isLocationLoading) {
                        // 如果有位置信息且不在加载中，显示详情对话框
                        viewModel.showLocationDetail()
                    } else if (!uiState.isLocationLoading) {
                        // 如果不在加载中，手动刷新位置
                        viewModel.requestCurrentLocation()
                    }
                },
                onLongClick = {
                    // 长按强制刷新位置
                    if (uiState.hasLocationPermission && !uiState.isLocationLoading) {
                        viewModel.requestCurrentLocation()
                    }
                }
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Primary.copy(alpha = 0.85f), Primary)
                    ),
                    shape = RoundedCornerShape(12.dp)
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
    val actionWidth = 72.dp
    val actionWidthPx = with(LocalDensity.current) { actionWidth.toPx() }
    val maxOffset = actionWidthPx * 3 // 三个操作按钮的总宽度
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        // 背景操作按钮 - 使用圆角和更柔和的颜色
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .height(72.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            // 完成按钮
            Box(
                modifier = Modifier
                    .width(actionWidth)
                    .fillMaxHeight()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Success.copy(alpha = 0.9f), Success)
                        )
                    )
                    .clickable { 
                        onToggleStatus()
                        offsetX = 0f
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.checkbox_on_background),
                        contentDescription = "完成",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "完成",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // 延期按钮
            Box(
                modifier = Modifier
                    .width(actionWidth)
                    .fillMaxHeight()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Warning.copy(alpha = 0.9f), Warning)
                        )
                    )
                    .clickable { 
                        onPostpone()
                        offsetX = 0f
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_recent_history),
                        contentDescription = "延期",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "延期",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // 放弃按钮
            Box(
                modifier = Modifier
                    .width(actionWidth)
                    .fillMaxHeight()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Danger.copy(alpha = 0.9f), Danger)
                        )
                    )
                    .clickable { 
                        onCancel()
                        offsetX = 0f
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                        contentDescription = "放弃",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "放弃",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        // 主要内容卡片 - 使用更柔和的背景色
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
                                offsetX < -maxOffset / 3 -> -maxOffset
                                offsetX > maxOffset / 6 -> 0f
                                else -> 0f
                            }
                        }
                    ) { _, dragAmount ->
                        val newOffset = offsetX + dragAmount
                        offsetX = newOffset.coerceIn(-maxOffset, 0f)
                    }
                },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = BgCard
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgCard)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 任务图标 - 更柔和的设计
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = when (task.category) {
                                    TaskCategory.WORK -> listOf(Color(0xFF42A5F5).copy(alpha = 0.15f), Color(0xFF42A5F5).copy(alpha = 0.25f))
                                    TaskCategory.STUDY -> listOf(Color(0xFFAB47BC).copy(alpha = 0.15f), Color(0xFFAB47BC).copy(alpha = 0.25f))
                                    TaskCategory.LIFE -> listOf(Color(0xFF66BB6A).copy(alpha = 0.15f), Color(0xFF66BB6A).copy(alpha = 0.25f))
                                    TaskCategory.HEALTH -> listOf(Color(0xFFE91E63).copy(alpha = 0.15f), Color(0xFFE91E63).copy(alpha = 0.25f))
                                    TaskCategory.PERSONAL -> listOf(Color(0xFFFF9800).copy(alpha = 0.15f), Color(0xFFFF9800).copy(alpha = 0.25f))
                                    TaskCategory.OTHER -> listOf(Color(0xFF9E9E9E).copy(alpha = 0.15f), Color(0xFF9E9E9E).copy(alpha = 0.25f))
                                }
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = when (task.category) {
                                TaskCategory.WORK -> Color(0xFF42A5F5).copy(alpha = 0.3f)
                                TaskCategory.STUDY -> Color(0xFFAB47BC).copy(alpha = 0.3f)
                                TaskCategory.LIFE -> Color(0xFF66BB6A).copy(alpha = 0.3f)
                                TaskCategory.HEALTH -> Color(0xFFE91E63).copy(alpha = 0.3f)
                                TaskCategory.PERSONAL -> Color(0xFFFF9800).copy(alpha = 0.3f)
                                TaskCategory.OTHER -> Color(0xFF9E9E9E).copy(alpha = 0.3f)
                            },
                            shape = CircleShape
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
                        fontSize = 18.sp
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // 任务内容
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        textDecoration = if (task.status == TaskStatus.COMPLETED) TextDecoration.LineThrough else null
                    )
                    
                    if (task.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = task.description,
                            fontSize = 13.sp,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 优先级标签 - 更柔和的设计
                        Box(
                            modifier = Modifier
                                .background(
                                    when (task.priority) {
                                        TaskPriority.HIGH -> Danger.copy(alpha = 0.08f)
                                        TaskPriority.MEDIUM -> Warning.copy(alpha = 0.08f)
                                        TaskPriority.LOW -> Success.copy(alpha = 0.08f)
                                    },
                                    RoundedCornerShape(6.dp)
                                )
                                .border(
                                    width = 0.5.dp,
                                    color = when (task.priority) {
                                        TaskPriority.HIGH -> Danger.copy(alpha = 0.2f)
                                        TaskPriority.MEDIUM -> Warning.copy(alpha = 0.2f)
                                        TaskPriority.LOW -> Success.copy(alpha = 0.2f)
                                    },
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = when (task.priority) {
                                    TaskPriority.HIGH -> "高"
                                    TaskPriority.MEDIUM -> "中"
                                    TaskPriority.LOW -> "低"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = when (task.priority) {
                                    TaskPriority.HIGH -> Danger.copy(alpha = 0.8f)
                                    TaskPriority.MEDIUM -> Warning.copy(alpha = 0.8f)
                                    TaskPriority.LOW -> Success.copy(alpha = 0.8f)
                                }
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // 分类标签
                        Text(
                            text = task.category.displayName,
                            fontSize = 11.sp,
                            color = TextMuted,
                            modifier = Modifier
                                .background(
                                    BgSecondary,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                        
                        task.dueDate?.let {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (task.isUrgent) "距截止 1:20" else "14:00",
                                fontSize = 11.sp,
                                color = if (task.isUrgent) Danger else TextSecondary,
                                modifier = Modifier
                                    .background(
                                        if (task.isUrgent) Danger.copy(alpha = 0.1f) else BgSecondary,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // 任务状态 - 更精致的设计
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = when (task.status) {
                                        TaskStatus.COMPLETED -> listOf(Success.copy(alpha = 0.1f), Success.copy(alpha = 0.15f))
                                        TaskStatus.IN_PROGRESS -> listOf(Primary.copy(alpha = 0.1f), Primary.copy(alpha = 0.15f))
                                        TaskStatus.CANCELLED -> listOf(TextMuted.copy(alpha = 0.1f), TextMuted.copy(alpha = 0.15f))
                                        TaskStatus.OVERDUE -> listOf(Danger.copy(alpha = 0.1f), Danger.copy(alpha = 0.15f))
                                        TaskStatus.PENDING -> if (task.isUrgent) 
                                            listOf(Danger.copy(alpha = 0.1f), Danger.copy(alpha = 0.15f)) 
                                            else listOf(Primary.copy(alpha = 0.08f), Primary.copy(alpha = 0.12f))
                                    }
                                ),
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 0.5.dp,
                                color = when (task.status) {
                                    TaskStatus.COMPLETED -> Success.copy(alpha = 0.3f)
                                    TaskStatus.IN_PROGRESS -> Primary.copy(alpha = 0.3f)
                                    TaskStatus.CANCELLED -> TextMuted.copy(alpha = 0.3f)
                                    TaskStatus.OVERDUE -> Danger.copy(alpha = 0.3f)
                                    TaskStatus.PENDING -> if (task.isUrgent) Danger.copy(alpha = 0.3f) else Primary.copy(alpha = 0.2f)
                                },
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
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
                                TaskStatus.PENDING -> if (task.isUrgent) Danger else Primary
                            }
                        )
                    }
                }
            }
        }
    }
} 