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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
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
import java.time.LocalDateTime

internal fun selectNextThingTask(
    tasks: List<Task>,
    now: LocalDateTime
): Task? = tasks
    .asSequence()
    .filter { it.status == TaskStatus.PENDING }
    .filter { task ->
        task.dueDate?.let { dueDate ->
            dueDate.toLocalDate() == now.toLocalDate() && dueDate.isAfter(now)
        } == true
    }
    .minWithOrNull(
        compareBy<Task> { it.dueDate!! }
            .thenBy { it.createdAt }
            .thenBy { it.id }
    )

internal fun isTodayTaskOverdue(
    task: Task,
    now: LocalDateTime
): Boolean {
    if (task.status == TaskStatus.OVERDUE) return true
    val dueDate = task.dueDate ?: return false
    return task.status == TaskStatus.PENDING &&
        dueDate.toLocalDate() == now.toLocalDate() &&
        now.isAfter(dueDate.plusMinutes(5))
}

@Composable
fun TodayScreen(
    viewModel: TodayViewModel = hiltViewModel(),
    onNavigateToFocus: () -> Unit,
    onNavigateToTaskDetail: (String) -> Unit,
    onNavigateToCalendar: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    // 面试版 NextThing：今天尚未到期的待办任务中，截止时间最近且排序稳定的一件。
    val now = remember(uiState.displayTasks) { java.time.LocalDateTime.now() }
    val nextThingId by remember(uiState.allTasks, now) {
        derivedStateOf {
            selectNextThingTask(uiState.allTasks, now)?.id
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
    val successColor = Color(0xFF8B7FF7)
    val warningColor = Color(0xFF6C5CE7)
    val dangerColor = Color(0xFF4A3BC1)
    var pendingCompleteTaskId by remember { mutableStateOf<String?>(null) }
    var pendingPostponeTaskId by remember { mutableStateOf<String?>(null) }
    var pendingPostponeReason by remember { mutableStateOf<String?>(null) }
    var pendingCancelTaskId by remember { mutableStateOf<String?>(null) }
    var pendingCancelReason by remember { mutableStateOf<String?>(null) }
    val confirmPostponeFromDialog: (String, String) -> Unit = { taskId, reason ->
        pendingPostponeTaskId = taskId
        pendingPostponeReason = reason
        viewModel.hidePostponeReasonDialog()
        explodingTaskId = taskId
        explodingColor = warningColor
    }

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

            // 与后台状态机统一使用 5 分钟宽限期，避免页面和数据库状态互相矛盾。
            val overdueTasks = if (isPendingTab) uiState.displayTasks.filter { task ->
                isTodayTaskOverdue(task, now)
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
                                        isPostponeDialogVisible = uiState.showPostponeReasonDialog && uiState.postponeTaskId == task.id,
                                        onDismissPostponeDialog = viewModel::hidePostponeReasonDialog,
                                        onConfirmPostponeDialog = { reason -> confirmPostponeFromDialog(task.id, reason) },
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
                                isPostponeDialogVisible = uiState.showPostponeReasonDialog && uiState.postponeTaskId == task.id,
                                onDismissPostponeDialog = viewModel::hidePostponeReasonDialog,
                                onConfirmPostponeDialog = { reason -> confirmPostponeFromDialog(task.id, reason) },
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
                            isPostponeDialogVisible = uiState.showPostponeReasonDialog && uiState.postponeTaskId == nextThingTask.id,
                            onDismissPostponeDialog = viewModel::hidePostponeReasonDialog,
                            onConfirmPostponeDialog = { reason -> confirmPostponeFromDialog(nextThingTask.id, reason) },
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
                                        isPostponeDialogVisible = uiState.showPostponeReasonDialog && uiState.postponeTaskId == task.id,
                                        onDismissPostponeDialog = viewModel::hidePostponeReasonDialog,
                                        onConfirmPostponeDialog = { reason -> confirmPostponeFromDialog(task.id, reason) },
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
                                isPostponeDialogVisible = uiState.showPostponeReasonDialog && uiState.postponeTaskId == task.id,
                                onDismissPostponeDialog = viewModel::hidePostponeReasonDialog,
                                onConfirmPostponeDialog = { reason -> confirmPostponeFromDialog(task.id, reason) },
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
                        isPostponeDialogVisible = uiState.showPostponeReasonDialog && uiState.postponeTaskId == task.id,
                        onDismissPostponeDialog = viewModel::hidePostponeReasonDialog,
                        onConfirmPostponeDialog = { reason -> confirmPostponeFromDialog(task.id, reason) },
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
        errorMessage = uiState.locationError,
        onDismiss = { viewModel.hideLocationDetailDialog() },
        onRefresh = { viewModel.requestCurrentLocation() }
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
    location: com.nextthing.app.domain.model.LocationInfo?,
    isLoading: Boolean,
    hasPermission: Boolean,
    isLocationEnabled: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val compactLocation = remember(currentLocation, location) {
        location?.toHomeLocationLabel() ?: currentLocation
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .background(Color(0xFFF7F8FC), RoundedCornerShape(8.dp))
            .border(
                width = 1.dp,
                color = Color(0xFF29293A).copy(alpha = 0.23f),
                shape = RoundedCornerShape(8.dp)
            )
            .widthIn(min = 132.dp, max = 132.dp)
            .height(40.dp)
            .padding(horizontal = 4.dp, vertical = 8.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = Primary
            )
        } else {
            Icon(
                painter = painterResource(id = R.drawable.icon_location),
                contentDescription = "当前位置",
                tint = when {
                    !hasPermission -> Danger
                    !isLocationEnabled -> Warning
                    else -> Primary
                },
                modifier = Modifier.size(24.dp)
            )
        }

        if (compactLocation.isNotBlank()) {
            Text(
                text = compactLocation,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = when {
                    !hasPermission -> Danger
                    !isLocationEnabled -> Warning
                    isLoading -> Primary
                    else -> TextSecondary
                },
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.width(96.dp)
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
            .background(Color.White)
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // NT Logo
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "NT",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "NextThing",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // 日历图标
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onNavigateToCalendar() }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.icon_calendar),
                    contentDescription = "日历",
                    modifier = Modifier.size(24.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 定位图标
            LocationIcon(
                currentLocation = uiState.currentLocationName,
                location = uiState.currentLocation,
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

private fun com.nextthing.app.domain.model.LocationInfo.toHomeLocationLabel(): String {
    val area = district.trim()
    val name = locationName.trim()
    if (area.isBlank()) return name.ifBlank { "当前位置" }.take(8)
    val detail = name.removePrefix(area).trim()
    return if (detail.isBlank()) area else "$area · ${detail.take(4)}"
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
            .height(210.dp)
            .padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
        shape = RoundedCornerShape(8.dp),
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
                .clip(RoundedCornerShape(8.dp))
        ) {
            // 第一层：天气背景图
            Image(
                painter = painterResource(id = weatherBgRes(weatherInfo?.condition)),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )
            // 第二层：渐变蒙层（上浅下深，保证文字可读）
            Box(modifier = Modifier.matchParentSize())
            // 第三层：内容
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(47.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val chineseDate = remember {
                        com.nextthing.app.util.ChineseDateHelper.getToday()
                    }
                    Box(
                        modifier = Modifier
                            .padding(
                                start = 9.dp + with(LocalDensity.current) { 10f.toDp() }
                            )
                            .wrapContentWidth()
                            .height(47.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = chineseDate.lunarText,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .padding(end = 18.dp)
                            .width(52.dp)
                            .height(43.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = weatherInfo?.let { "${it.temperature}°  ${it.condition.displayName}" }
                                ?: "--°  --",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                }

                Box(
                    modifier = Modifier.fillMaxWidth().height(76.dp).padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "${(completionRate * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(77.dp)
                ) {
                    StatItem(
                        label = "今日任务",
                        value = totalTasks.toString(),
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = with(LocalDensity.current) { 10f.toDp() })
                            .width(70.dp)
                    )
                    StatItem(
                        label = "已完成",
                        value = completedTasks.toString(),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .width(60.dp)
                    )
                    StatItem(
                        label = "剩余",
                        value = remainingTasks.toString(),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = with(LocalDensity.current) { 10f.toDp() })
                            .width(40.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(39.dp)
    ) {
        Text(
            text = "今日任务",
            modifier = Modifier.offset(x = 10.dp, y = 10.dp),
            fontSize = 16.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF16181D),
            maxLines = 1
        )
        Text(
            text = "$completedCount 已完成",
            modifier = Modifier.offset(x = 232.dp, y = 10.dp),
            color = Color(0xFF34C759),
            fontSize = 16.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
        Text(
            text = "$pendingCount 待办",
            modifier = Modifier.offset(x = 307.dp, y = 10.dp),
            color = Color(0xFFFF453A),
            fontSize = 16.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun TaskTabs(selectedTab: TaskTab, onTabSelected: (TaskTab) -> Unit) {
    val cornerRadius = with(LocalDensity.current) { 8f.toDp() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        shape = RoundedCornerShape(cornerRadius),
        border = BorderStroke(
            width = 1.dp,
            color = Color(0xFF29293A).copy(alpha = 0.23f)
        )
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
    isPostponeDialogVisible: Boolean = false,
    onDismissPostponeDialog: () -> Unit = {},
    onConfirmPostponeDialog: (String) -> Unit = {},
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
        modifier = Modifier.padding(horizontal = 10.dp)
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
                .clip(RoundedCornerShape(8.dp))
        ) {
            // 完成按钮
            Box(
                modifier = Modifier
                    .width(actionWidth)
                    .fillMaxHeight()
                    .background(Color(0xFF21C45E))
                    .clickable {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleStatus()
                    },
            ) {
                Image(
                        painter = painterResource(id = R.drawable.icon_item_finish),
                        contentDescription = "完成",
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = 53.dp)
                            .size(16.dp, 12.dp)
                    )
                Text(
                    text = "完成",
                    modifier = Modifier.align(Alignment.TopCenter).offset(y = 81.dp),
                    color = Color.White,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // 延期按钮
            Box(
                modifier = Modifier
                    .width(actionWidth)
                    .fillMaxHeight()
                    .background(Color(0xFFF59E0A))
                    .clickable {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPostpone()
                    },
            ) {
                Image(
                        painter = painterResource(id = R.drawable.icon_item_delay),
                        contentDescription = "延期",
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = 48.dp)
                            .size(34.dp, 22.dp)
                    )
                Text(
                    text = "延期",
                    modifier = Modifier.align(Alignment.TopCenter).offset(y = 84.dp),
                    color = Color.White,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // 放弃按钮
            Box(
                modifier = Modifier
                    .width(actionWidth)
                    .fillMaxHeight()
                    .background(Color(0xFFF54040))
                    .clickable {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCancel()
                    },
            ) {
                Image(
                        painter = painterResource(id = R.drawable.icon_item_giveup),
                        contentDescription = "放弃",
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = 53.5.dp)
                            .size(11.dp)
                    )
                Text(
                    text = "放弃",
                    modifier = Modifier.align(Alignment.TopCenter).offset(y = 81.dp),
                    color = Color.White,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        }

        // 主卡片
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = if (isPostponeDialogVisible) 0.18f else 1f }
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

        PostponeReasonDialog(
            isVisible = isPostponeDialogVisible && !isExploding,
            onDismiss = onDismissPostponeDialog,
            onConfirm = onConfirmPostponeDialog
        )

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
