package com.nextthing.app.presentation.screens.today

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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.hilt.navigation.compose.hiltViewModel
import com.nextthing.app.domain.model.Task
import com.nextthing.app.domain.model.TaskCategory
import com.nextthing.app.domain.model.TaskStatus
import com.nextthing.app.domain.model.TaskTab
import com.nextthing.app.LocalPermissionLauncher
import com.nextthing.app.presentation.components.LocationDetailDialog
import com.nextthing.app.presentation.components.LocationHelpDialog
import com.nextthing.app.presentation.components.LocationPermissionDialog
import com.nextthing.app.presentation.components.WeatherSummaryCard
import com.nextthing.app.presentation.components.CancelReasonDialog
import com.nextthing.app.presentation.components.PostponeReasonDialog
import com.nextthing.app.domain.model.WeatherCondition
import com.nextthing.app.domain.model.WeatherInfo
import com.nextthing.app.R
import com.nextthing.app.presentation.theme.*
import com.nextthing.app.presentation.components.TaskItemCard
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.nextthing.app.util.PermissionHelper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.nextthing.app.presentation.components.ParticleExplosionEffect

@Composable
fun TodayScreen(
    viewModel: TodayViewModel = hiltViewModel(),
    onNavigateToFocus: () -> Unit,
    onNavigateToTaskDetail: (String) -> Unit,
    onNavigateToCalendar: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    // 计算 NextThing：待办列表中 dueDate 距离现在最近的下一件未来任务
    // 使用 uiState 整体作为 key，确保任务数据和时间变化都能触发重算
    val now = remember(uiState.displayTasks) { java.time.LocalDateTime.now() }
    val nextThingId by remember(uiState.allTasks, now) {
        derivedStateOf {
            uiState.allTasks
                .filter { it.status == TaskStatus.PENDING && it.dueDate != null && it.dueDate.isAfter(now) }
                .minByOrNull { it.dueDate!! }
                ?.id
        }
    }

    // 监听UI状态变化并记录日志
    LaunchedEffect(uiState.totalTasks, uiState.displayTasks.size) {
        timber.log.Timber.tag("DataFlow").d("━━━━━━ TodayScreen UI状态更新 ━━━━━━")
        timber.log.Timber.tag("DataFlow").d("📊 UI渲染: totalTasks=${uiState.totalTasks}, displayTasks=${uiState.displayTasks.size}")
        timber.log.Timber.tag("DataFlow").d("  已完成=${uiState.completedTasks}, 待办=${uiState.remainingTasks}")
        timber.log.Timber.tag("DataFlow").d("  当前Tab=${uiState.selectedTab}")
        timber.log.Timber.tag("DataFlow").d("  isLoading=${uiState.isLoading}")
    }
    val showPermissionDialog by viewModel.showPermissionDialog.collectAsState()
    val showLocationDetailDialog by viewModel.showLocationDetailDialog.collectAsState()
    val showLocationHelpDialog by viewModel.showLocationHelpDialog.collectAsState()
    val permissionLauncher = LocalPermissionLauncher.current

    @Suppress("DEPRECATION") val lifecycleOwner = LocalLifecycleOwner.current

    // 每次 ON_RESUME（首次进入/从后台返回/从设置页返回）时统一触发权限检查与位置刷新
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onScreenResumed()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 权限授予后只需关闭对话框，位置获取由 ON_RESUME 统一处理
    LaunchedEffect(uiState.hasLocationPermission) {
        if (uiState.hasLocationPermission) {
            viewModel.hidePermissionDialog()
        }
    }

    // 粒子爆炸动画全局状态
    var explodingTaskId by remember { mutableStateOf<String?>(null) }
    var explodingColor by remember { mutableStateOf(Color.Transparent) }
    val successColor = Success
    val warningColor = Warning
    val dangerColor = Danger
    var pendingCompleteTaskId by remember { mutableStateOf<String?>(null) }
    var pendingPostponeTaskId by remember { mutableStateOf<String?>(null) }
    var pendingPostponeReason by remember { mutableStateOf<String?>(null) }
    var pendingCancelTaskId by remember { mutableStateOf<String?>(null) }
    var pendingCancelReason by remember { mutableStateOf<String?>(null) }

    // 当 explodingTaskId 对应的任务从列表中消失后，清理爆炸状态
    LaunchedEffect(explodingTaskId, uiState.displayTasks) {
        if (explodingTaskId != null && uiState.displayTasks.none { it.id == explodingTaskId }) {
            explodingTaskId = null
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
            viewModel = viewModel,
            onNavigateToCalendar = onNavigateToCalendar
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
                    remainingTasks = uiState.remainingTasks,
                    weatherInfo = uiState.weatherInfo,
                    onWeatherClick = {
                        // 天气详情页为可选增强功能,暂未实现
                        // 可扩展:显示未来7天天气预报、空气质量、生活指数等
                        timber.log.Timber.d("点击天气卡片")
                    }
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

            // 任务列表（支持折叠视图）
            val isPendingTab = uiState.selectedTab == TaskTab.PENDING

            // 分组逻辑：基于 dueDate 是否已过（而非仅靠 status 字段，避免 5 分钟空窗期）
            // 逾期组：status == OVERDUE，或 dueDate 已过但 status 尚未更新的任务
            val overdueTasks = if (isPendingTab) uiState.displayTasks.filter { task ->
                task.status == TaskStatus.OVERDUE ||
                (task.status == TaskStatus.PENDING && task.dueDate != null && task.dueDate.isBefore(now))
            } else emptyList()

            val overdueIds = overdueTasks.map { it.id }.toSet()

            // 未来组：排除逾期和 NextThing 的其他任务
            val futureTasks = if (isPendingTab) uiState.displayTasks.filter { task ->
                !overdueIds.contains(task.id) && task.id != nextThingId
            } else emptyList()

            val nextThingTask = if (isPendingTab) uiState.displayTasks.find { it.id == nextThingId } else null

            if (isPendingTab && (uiState.collapseOverdue || uiState.collapseFuture)) {
                // ── 折叠模式 ──

                // 逾期任务摘要/展开
                if (overdueTasks.isNotEmpty()) {
                    if (uiState.collapseOverdue) {
                        item(key = "overdue_summary") {
                            var expanded by remember { mutableStateOf(false) }
                            CollapsedSummaryBar(
                                icon = "🔴",
                                label = "已逾期",
                                count = overdueTasks.size,
                                color = Danger,
                                isExpanded = expanded,
                                onToggle = { expanded = !expanded }
                            )
                            if (expanded) {
                                overdueTasks.forEach { task ->
                                    TaskItem(
                                        task = task,
                                        isNextThing = false,
                                        isExploding = explodingTaskId == task.id,
                                        explosionColor = if (explodingTaskId == task.id) explodingColor else Success,
                                        onExplosionFinished = {
                                            handleExplosionFinished(
                                                pendingCompleteTaskId, pendingPostponeTaskId, pendingPostponeReason,
                                                pendingCancelTaskId, pendingCancelReason, viewModel,
                                                { pendingCompleteTaskId = null }, { pendingPostponeTaskId = null; pendingPostponeReason = null },
                                                { pendingCancelTaskId = null; pendingCancelReason = null }
                                            )
                                        },
                                        onToggleStatus = { pendingCompleteTaskId = task.id; explodingTaskId = task.id; explodingColor = successColor },
                                        onPostpone = { viewModel.showPostponeReasonDialog(task.id) },
                                        onCancel = { viewModel.showCancelReasonDialog(task.id) },
                                        onStartFocus = { onNavigateToFocus() },
                                        onClick = { onNavigateToTaskDetail(task.id) }
                                    )
                                }
                            }
                        }
                    } else {
                        items(items = overdueTasks, key = { it.id }) { task ->
                            TaskItem(
                                task = task, isNextThing = false,
                                isExploding = explodingTaskId == task.id,
                                explosionColor = if (explodingTaskId == task.id) explodingColor else Success,
                                onExplosionFinished = { handleExplosionFinished(pendingCompleteTaskId, pendingPostponeTaskId, pendingPostponeReason, pendingCancelTaskId, pendingCancelReason, viewModel, { pendingCompleteTaskId = null }, { pendingPostponeTaskId = null; pendingPostponeReason = null }, { pendingCancelTaskId = null; pendingCancelReason = null }) },
                                onToggleStatus = { pendingCompleteTaskId = task.id; explodingTaskId = task.id; explodingColor = successColor },
                                onPostpone = { viewModel.showPostponeReasonDialog(task.id) },
                                onCancel = { viewModel.showCancelReasonDialog(task.id) },
                                onStartFocus = { onNavigateToFocus() },
                                onClick = { onNavigateToTaskDetail(task.id) }
                            )
                        }
                    }
                }

                // NextThing 始终显示
                if (nextThingTask != null) {
                    item(key = "next_thing_${nextThingTask.id}") {
                        TaskItem(
                            task = nextThingTask, isNextThing = true,
                            isExploding = explodingTaskId == nextThingTask.id,
                            explosionColor = if (explodingTaskId == nextThingTask.id) explodingColor else Success,
                            onExplosionFinished = { handleExplosionFinished(pendingCompleteTaskId, pendingPostponeTaskId, pendingPostponeReason, pendingCancelTaskId, pendingCancelReason, viewModel, { pendingCompleteTaskId = null }, { pendingPostponeTaskId = null; pendingPostponeReason = null }, { pendingCancelTaskId = null; pendingCancelReason = null }) },
                            onToggleStatus = { pendingCompleteTaskId = nextThingTask.id; explodingTaskId = nextThingTask.id; explodingColor = successColor },
                            onPostpone = { viewModel.showPostponeReasonDialog(nextThingTask.id) },
                            onCancel = { viewModel.showCancelReasonDialog(nextThingTask.id) },
                            onStartFocus = { onNavigateToFocus() },
                            onClick = { onNavigateToTaskDetail(nextThingTask.id) }
                        )
                    }
                }

                // 未来任务摘要/展开
                if (futureTasks.isNotEmpty()) {
                    if (uiState.collapseFuture) {
                        item(key = "future_summary") {
                            var expanded by remember { mutableStateOf(false) }
                            CollapsedSummaryBar(
                                icon = "📋",
                                label = "未来任务",
                                count = futureTasks.size,
                                color = Primary,
                                isExpanded = expanded,
                                onToggle = { expanded = !expanded }
                            )
                            if (expanded) {
                                futureTasks.forEach { task ->
                                    TaskItem(
                                        task = task, isNextThing = false,
                                        isExploding = explodingTaskId == task.id,
                                        explosionColor = if (explodingTaskId == task.id) explodingColor else Success,
                                        onExplosionFinished = { handleExplosionFinished(pendingCompleteTaskId, pendingPostponeTaskId, pendingPostponeReason, pendingCancelTaskId, pendingCancelReason, viewModel, { pendingCompleteTaskId = null }, { pendingPostponeTaskId = null; pendingPostponeReason = null }, { pendingCancelTaskId = null; pendingCancelReason = null }) },
                                        onToggleStatus = { pendingCompleteTaskId = task.id; explodingTaskId = task.id; explodingColor = successColor },
                                        onPostpone = { viewModel.showPostponeReasonDialog(task.id) },
                                        onCancel = { viewModel.showCancelReasonDialog(task.id) },
                                        onStartFocus = { onNavigateToFocus() },
                                        onClick = { onNavigateToTaskDetail(task.id) }
                                    )
                                }
                            }
                        }
                    } else {
                        items(items = futureTasks, key = { it.id }) { task ->
                            TaskItem(
                                task = task, isNextThing = false,
                                isExploding = explodingTaskId == task.id,
                                explosionColor = if (explodingTaskId == task.id) explodingColor else Success,
                                onExplosionFinished = { handleExplosionFinished(pendingCompleteTaskId, pendingPostponeTaskId, pendingPostponeReason, pendingCancelTaskId, pendingCancelReason, viewModel, { pendingCompleteTaskId = null }, { pendingPostponeTaskId = null; pendingPostponeReason = null }, { pendingCancelTaskId = null; pendingCancelReason = null }) },
                                onToggleStatus = { pendingCompleteTaskId = task.id; explodingTaskId = task.id; explodingColor = successColor },
                                onPostpone = { viewModel.showPostponeReasonDialog(task.id) },
                                onCancel = { viewModel.showCancelReasonDialog(task.id) },
                                onStartFocus = { onNavigateToFocus() },
                                onClick = { onNavigateToTaskDetail(task.id) }
                            )
                        }
                    }
                }
            } else {
                // ── 正常模式（无折叠） ──
                items(
                    items = uiState.displayTasks,
                    key = { task -> task.id }
                ) { task ->
                    TaskItem(
                        task = task,
                        isNextThing = task.id == nextThingId,
                        isExploding = explodingTaskId == task.id,
                        explosionColor = if (explodingTaskId == task.id) explodingColor else Success,
                        onExplosionFinished = {
                            handleExplosionFinished(
                                pendingCompleteTaskId, pendingPostponeTaskId, pendingPostponeReason,
                                pendingCancelTaskId, pendingCancelReason, viewModel,
                                { pendingCompleteTaskId = null }, { pendingPostponeTaskId = null; pendingPostponeReason = null },
                                { pendingCancelTaskId = null; pendingCancelReason = null }
                            )
                        },
                        onToggleStatus = {
                            pendingCompleteTaskId = task.id
                            explodingTaskId = task.id
                            explodingColor = successColor
                        },
                        onPostpone = { viewModel.showPostponeReasonDialog(task.id) },
                        onCancel = { viewModel.showCancelReasonDialog(task.id) },
                        onStartFocus = { onNavigateToFocus() },
                        onClick = { onNavigateToTaskDetail(task.id) }
                    )
                }
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
        },
        onPermissionGranted = {
            // 权限授予后 Activity ON_RESUME 会触发 onScreenResumed() 统一处理
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
            // 位置设置已通过系统设置管理,可扩展:导航到应用详情页
        }
    )

    // 延期任务原因对话框
    PostponeReasonDialog(
        isVisible = uiState.showPostponeReasonDialog && explodingTaskId == null,
        onDismiss = { viewModel.hidePostponeReasonDialog() },
        onConfirm = { reason ->
            val taskId = uiState.postponeTaskId
            if (taskId != null) {
                pendingPostponeTaskId = taskId
                pendingPostponeReason = reason
                viewModel.hidePostponeReasonDialog()
                explodingTaskId = taskId
                explodingColor = warningColor
            }
        }
    )

    // 放弃任务原因对话框
    CancelReasonDialog(
        isVisible = uiState.showCancelReasonDialog && explodingTaskId == null,
        onDismiss = { viewModel.hideCancelReasonDialog() },
        onConfirm = { reason ->
            val taskId = uiState.cancelTaskId
            if (taskId != null) {
                pendingCancelTaskId = taskId
                pendingCancelReason = reason
                viewModel.hideCancelReasonDialog()
                explodingTaskId = taskId
                explodingColor = dangerColor
            }
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
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 140.dp)
            )
        }
    }
}

@Composable
private fun TopHeader(
    uiState: TodayUiState,
    viewModel: TodayViewModel,
    onNavigateToCalendar: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgCard)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "NextThing",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            // 日历图标
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onNavigateToCalendar() }
                    .padding(4.dp)
            ) {
                androidx.compose.foundation.text.BasicText(
                    text = "📅",
                    style = androidx.compose.ui.text.TextStyle(fontSize = 18.sp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 定位图标
            LocationIcon(
                currentLocation = uiState.currentLocationName,
                isLoading = uiState.isLocationLoading,
                hasPermission = uiState.hasLocationPermission,
                isLocationEnabled = uiState.isLocationEnabled,
                onClick = {
                    if (!uiState.hasLocationPermission) {
                        viewModel.requestLocationPermission()
                    } else if (uiState.currentLocation != null && !uiState.isLocationLoading) {
                        viewModel.showLocationDetail()
                    } else if (!uiState.isLocationLoading) {
                        viewModel.requestCurrentLocation()
                    }
                },
                onLongClick = {
                    if (uiState.hasLocationPermission && !uiState.isLocationLoading) {
                        viewModel.requestCurrentLocation()
                    }
                }
            )
        } // inner Row end
    } // outer Row end
}

private fun weatherBgRes(condition: WeatherCondition?): Int = when (condition) {
    WeatherCondition.SUNNY        -> R.drawable.weather_sunny
    WeatherCondition.CLOUDY       -> R.drawable.weather_cloudy
    WeatherCondition.PARTLY_CLOUDY -> R.drawable.weather_partly_cloudy
    WeatherCondition.RAINY        -> R.drawable.weather_rainy
    WeatherCondition.THUNDERSTORM -> R.drawable.weather_thunderstorm
    WeatherCondition.SNOWY        -> R.drawable.weather_snowy
    WeatherCondition.FOGGY        -> R.drawable.weather_foggy
    WeatherCondition.WINDY        -> R.drawable.weather_windy
    else                          -> R.drawable.weather_unknown
}

@Suppress("UNUSED_PARAMETER")
@Composable
private fun ProgressOverviewCard(
    completionRate: Float,
    totalTasks: Int,
    completedTasks: Int,
    remainingTasks: Int,
    weatherInfo: WeatherInfo? = null,
    onWeatherClick: () -> Unit = {}
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
                .clip(RoundedCornerShape(12.dp))
        ) {
            // 第一层：天气背景图
            Image(
                painter = painterResource(id = weatherBgRes(weatherInfo?.condition)),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )
            // 第二层：渐变蒙层（上浅下深，保证文字可读）
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.20f),
                                Color.Black.copy(alpha = 0.55f)
                            )
                        )
                    )
            )
            // 第三层：内容
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // 左侧：农历日期 + 节气/节日
                    val chineseDate = remember {
                        com.nextthing.app.util.ChineseDateHelper.getToday()
                    }
                    Column {
                        Text(
                            text = chineseDate.lunarText,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (chineseDate.secondaryText != null) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = chineseDate.secondaryText!!,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    // 右侧：天气信息区域
                    WeatherInfoSection(
                        weatherInfo = weatherInfo,
                        modifier = Modifier.padding(top = 4.dp)
                    )
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
private fun WeatherInfoSection(
    weatherInfo: WeatherInfo?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        if (weatherInfo != null) {
            // 天气状况和温度
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 天气图标
                Text(
                    text = weatherInfo.condition.iconRes,
                    fontSize = 16.sp,
                    color = Color(weatherInfo.condition.color)
                )

                // 天气状态
                Text(
                    text = weatherInfo.condition.displayName,
                    color = Color.White,
                    fontSize = 12.sp
                )

                // 温度
                Text(
                    text = "${weatherInfo.temperature}°C",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 湿度信息
            Text(
                text = "湿度 ${weatherInfo.humidity}%",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp
            )

            // 生活建议（如果有紧急建议）
            weatherInfo.getPrioritySuggestion()?.let { suggestion ->
                if (suggestion.isUrgent) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "💡 ${suggestion.message}",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

        } else {
            // 加载状态
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    color = Color.White.copy(alpha = 0.7f),
                    strokeWidth = 1.dp
                )
                Text(
                    text = "获取天气中...",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }
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

@Suppress("UNUSED_PARAMETER")
@Composable
private fun TaskItem(
    task: Task,
    isNextThing: Boolean = false,
    onToggleStatus: () -> Unit,
    onPostpone: () -> Unit,
    onCancel: () -> Unit,
    onStartFocus: () -> Unit,
    onClick: () -> Unit,
    isExploding: Boolean = false,
    explosionColor: Color = Success,
    onExplosionFinished: () -> Unit = {}
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val actionWidth = 72.dp
    val actionWidthPx = with(LocalDensity.current) { actionWidth.toPx() }
    val maxOffset = actionWidthPx * 3

    val hapticFeedback = LocalHapticFeedback.current

    // Item 尺寸（用于计算爆炸中心）
    var itemWidth by remember { mutableFloatStateOf(0f) }
    var itemHeight by remember { mutableFloatStateOf(0f) }

    // 爆炸开始时归位卡片
    LaunchedEffect(isExploding) {
        if (isExploding) offsetX = 0f
    }

    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .onGloballyPositioned { coords ->
                    itemWidth = coords.size.width.toFloat()
                    itemHeight = coords.size.height.toFloat()
                }
        ) {
        if (!isExploding) {
        // 背景操作按钮
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
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
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleStatus()
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.checkbox_on_background),
                        contentDescription = "完成",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(text = "完成", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
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
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPostpone()
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_recent_history),
                        contentDescription = "延期",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(text = "延期", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
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
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCancel()
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                        contentDescription = "放弃",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(text = "放弃", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        }

        // 主卡片
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isExploding) Modifier.graphicsLayer {
                        alpha = 0f
                    }
                    else Modifier.offset { IntOffset(offsetX.roundToInt(), 0) }
                )
                .pointerInput(isExploding) {
                    if (!isExploding) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
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
                    }
                }
        ) {
            TaskItemCard(
                task = task,
                isNextThing = isNextThing,
                onClick = { if (!isExploding) onClick() }
            )
        }

        // 粒子爆炸效果层
        if (isExploding) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .zIndex(10f)
            ) {
                ParticleExplosionEffect(
                    isActive = true,
                    originX = itemWidth / 2f,
                    originY = itemHeight / 2f,
                    baseColor = explosionColor,
                    particleCount = 200,
                    durationMs = 500L,
                    onFinished = onExplosionFinished
                )
            }
        }
        }

        if (!isExploding) {
            // 分割线
            HorizontalDivider(
                thickness = 1.dp,
                color = Color(0xFFE0E0E0)
            )
        }
    }
}

private fun handleExplosionFinished(
    pendingCompleteTaskId: String?,
    pendingPostponeTaskId: String?,
    pendingPostponeReason: String?,
    pendingCancelTaskId: String?,
    pendingCancelReason: String?,
    viewModel: TodayViewModel,
    clearComplete: () -> Unit,
    clearPostpone: () -> Unit,
    clearCancel: () -> Unit
) {
    when {
        pendingCompleteTaskId != null -> {
            viewModel.toggleTaskStatus(pendingCompleteTaskId)
            clearComplete()
        }
        pendingPostponeTaskId != null && pendingPostponeReason != null -> {
            viewModel.executePostponeTask(pendingPostponeTaskId, pendingPostponeReason)
            clearPostpone()
        }
        pendingCancelTaskId != null && pendingCancelReason != null -> {
            viewModel.executeCancelTask(pendingCancelTaskId, pendingCancelReason)
            clearCancel()
        }
    }
}

@Composable
private fun CollapsedSummaryBar(
    icon: String,
    label: String,
    count: Int,
    color: Color,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        onClick = onToggle,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = color.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "$count",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                painter = painterResource(
                    id = if (isExpanded) android.R.drawable.arrow_up_float
                    else android.R.drawable.arrow_down_float
                ),
                contentDescription = if (isExpanded) "收起" else "展开",
                tint = color,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
