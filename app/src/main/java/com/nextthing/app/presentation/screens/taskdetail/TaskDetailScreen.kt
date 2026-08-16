package com.nextthing.app.presentation.screens.taskdetail

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.hilt.navigation.compose.hiltViewModel
import com.nextthing.app.domain.model.Task
import com.nextthing.app.domain.model.TaskCategory
import com.nextthing.app.domain.model.TaskStatus
import com.nextthing.app.domain.model.TaskImportanceUrgency
import com.nextthing.app.domain.model.RepeatFrequency
import com.nextthing.app.domain.model.Subtask
import com.nextthing.app.domain.model.LocationInfo
import com.nextthing.app.domain.model.TaskGeofence
import com.nextthing.app.R
import com.nextthing.app.presentation.theme.*
import coil.compose.AsyncImage
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("UNUSED_PARAMETER")
@Composable
fun TaskDetailScreen(
    taskId: String,
    onBackPressed: () -> Unit,
    onEditTask: () -> Unit = {},
    onNavigateToManageCategories: () -> Unit = {},
    onNavigateToRepeatCustom: () -> Unit = {},
    onNavigateToGeofenceConfig: () -> Unit = {},
    onNavigateToGeofenceAdd: () -> Unit = {},
    onNavigateToNotificationStrategies: () -> Unit = {},
    viewModel: TaskDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val availableNotificationStrategies by viewModel.availableNotificationStrategies.collectAsState()
    val geofenceLocations by viewModel.geofenceLocations.collectAsState()
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    val context = androidx.compose.ui.platform.LocalContext.current

    // 卡片展开状态
    var isTimeExpanded by remember { mutableStateOf(false) }
    var isPreciseTimeExpanded by remember { mutableStateOf(false) }
    var isCategoryExpanded by remember { mutableStateOf(false) }
    var isImportanceExpanded by remember { mutableStateOf(false) }
    var isImageExpanded by remember { mutableStateOf(false) }
    var isRepeatExpanded by remember { mutableStateOf(false) }
    var isNotificationExpanded by remember { mutableStateOf(false) }

    // 精确时间状态
    var preciseTime by remember { mutableStateOf<Pair<Int, Int>?>(null) } // null表示未设置

    // 日期选择器状态
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(taskId) {
        viewModel.loadTask(taskId)
    }

    // 从 task 的 dueDate 中提取精确时间
    LaunchedEffect(uiState.task?.dueDate) {
        uiState.task?.dueDate?.let { dueDate ->
            preciseTime = Pair(dueDate.hour, dueDate.minute)
        }
    }

    // 显示成功消息
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            com.nextthing.app.util.ToastHelper.showToast(context, message)
            viewModel.clearMessages()
        }
    }

    // 显示错误消息
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            com.nextthing.app.util.ToastHelper.showToast(context, message)
            viewModel.clearMessages()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF71CBF4))
                }
            }
            uiState.task != null -> {
                if (!uiState.isEditMode) {
                    RefinedTaskDetailContent(
                        task = uiState.task!!,
                        categoryName = uiState.categoryItem?.displayName ?: uiState.task!!.category.displayName,
                        notificationName = availableNotificationStrategies
                            .firstOrNull { it.id == uiState.task!!.notificationStrategyId }?.name ?: "未设置",
                        onBackPressed = onBackPressed,
                        onEditClick = viewModel::enterEditMode
                    )
                } else {
                RefinedTaskEditContent(
                    title = uiState.editedTitle,
                    description = uiState.editedDescription,
                    status = uiState.editedStatus,
                    categoryName = uiState.editedCategoryItem?.displayName ?: uiState.task!!.category.displayName,
                    selectedCategoryId = uiState.editedCategoryItem?.id,
                    categories = categories,
                    notificationStrategies = availableNotificationStrategies,
                    selectedNotificationStrategyId = uiState.editedNotificationStrategyId,
                    geofenceLocations = geofenceLocations,
                    geofenceEnabled = uiState.editedGeofenceEnabled,
                    selectedGeofenceLocationId = uiState.editedGeofenceLocationId,
                    importance = uiState.editedImportanceUrgency,
                    dueDate = uiState.editedDueDate,
                    repeatFrequency = uiState.editedRepeatFrequency,
                    notificationName = availableNotificationStrategies.firstOrNull { it.id == uiState.editedNotificationStrategyId }?.name ?: "未设置",
                    imageUri = uiState.editedImageUri,
                    locationName = geofenceLocations.firstOrNull { it.id == uiState.editedGeofenceLocationId }?.locationInfo?.locationName ?: "未启用",
                    subtasks = uiState.editedSubtasks,
                    onBack = viewModel::exitEditMode,
                    onTitleChange = viewModel::updateEditedTitle,
                    onDescriptionChange = viewModel::updateEditedDescription,
                    onDueDateChange = viewModel::updateEditedDueDate,
                    onCategorySelected = viewModel::updateSelectedCategory,
                    onManageCategories = onNavigateToManageCategories,
                    onImportanceSelected = viewModel::updateEditedImportanceUrgency,
                    onRepeatSelected = viewModel::updateEditedRepeatFrequency,
                    onNavigateToRepeatCustom = onNavigateToRepeatCustom,
                    onNotificationSelected = viewModel::updateNotificationStrategy,
                    onManageNotificationStrategies = onNavigateToNotificationStrategies,
                    onImageSelected = viewModel::updateEditedImagePath,
                    onNavigateToGeofenceConfig = onNavigateToGeofenceConfig,
                    onNavigateToGeofenceAdd = onNavigateToGeofenceAdd,
                    onGeofenceEnabledChange = viewModel::updateEditedGeofenceEnabled,
                    onGeofenceLocationSelected = viewModel::updateEditedGeofenceLocation,
                    onStatusChange = viewModel::updateEditedStatus,
                    onGenerateSubtasks = viewModel::generateAISubtasks,
                    onScheduleAdvice = viewModel::generateScheduleAdvice,
                    onEstimateTime = viewModel::estimateTaskTime,
                    isGeneratingSubtasks = uiState.isGeneratingSubtasks,
                    isGeneratingAdvice = uiState.isGeneratingAdvice,
                    isEstimatingTime = uiState.isEstimatingTime,
                    onSave = viewModel::saveChanges
                )
                if (false) {
                Column(
                    modifier = Modifier.fillMaxSize().background(Color.White)
                ) {
                    // 顶部导航区
                    TaskDetailTopNavigation(
                        isEditMode = uiState.isEditMode,
                        onBackPressed = onBackPressed,
                        onEditClick = { viewModel.enterEditMode() },
                        onDeleteClick = { viewModel.showDeleteConfirmDialog() }
                    )

                    // 内容区域
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 任务标题和描述卡片
                        TaskTitleDescriptionCard(
                            task = uiState.task!!,
                            isEditMode = uiState.isEditMode,
                            editedTitle = uiState.editedTitle,
                            editedDescription = uiState.editedDescription,
                            onTitleChange = viewModel::updateEditedTitle,
                            onDescriptionChange = viewModel::updateEditedDescription
                        )

                        // 任务状态卡片（始终显示，编辑模式下可操作）
                        TaskStatusCard(
                            currentStatus = if (uiState.isEditMode) uiState.editedStatus else uiState.task!!.status,
                            onStatusChange = viewModel::updateEditedStatus,
                            isEditMode = uiState.isEditMode
                        )

                        // 第一行：时间 + 精确时间
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            com.nextthing.app.presentation.screens.create.TimeConfigCard(
                                screenHeight = screenHeight,
                                screenWidth = screenWidth,
                                isExpanded = isTimeExpanded,
                                onExpandToggle = { isTimeExpanded = !isTimeExpanded },
                                selectedDate = if (uiState.isEditMode) uiState.editedDueDate?.toLocalDate() else uiState.task!!.dueDate?.toLocalDate(),
                                onDateSelected = { date ->
                                    viewModel.updateEditedDueDate(date?.atStartOfDay())
                                },
                                onShowDatePicker = { showDatePicker = true },
                                modifier = Modifier.fillMaxWidth(),
                                isEditMode = uiState.isEditMode
                            )

                            // 精确时间配置卡
                            com.nextthing.app.presentation.screens.create.PreciseTimeConfigCard(
                                screenHeight = screenHeight,
                                screenWidth = screenWidth,
                                isExpanded = isPreciseTimeExpanded,
                                onExpandToggle = { isPreciseTimeExpanded = !isPreciseTimeExpanded },
                                preciseTime = preciseTime,
                                onPreciseTimeSelected = { newPreciseTime ->
                                    preciseTime = newPreciseTime
                                    // 更新 ViewModel 中的 editedDueDate
                                    val currentDueDate = uiState.editedDueDate ?: uiState.task?.dueDate
                                    if (newPreciseTime != null) {
                                        val baseDate = currentDueDate?.toLocalDate() ?: LocalDate.now()
                                        val newDueDate = baseDate.atTime(newPreciseTime.first, newPreciseTime.second, 0, 0)
                                        viewModel.updateEditedDueDate(newDueDate)
                                    } else {
                                        // 如果清除精确时间，保留日期但设置为 23:59:59
                                        val baseDate = currentDueDate?.toLocalDate()
                                        if (baseDate != null) {
                                            viewModel.updateEditedDueDate(baseDate.atTime(23, 59, 59))
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                isEditMode = uiState.isEditMode
                            )
                        }

                        // 第二行：分类选择 + 重要程度
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            com.nextthing.app.presentation.screens.create.CategoryPriorityConfigCard(
                                screenHeight = screenHeight,
                                screenWidth = screenWidth,
                                isExpanded = isCategoryExpanded,
                                onExpandToggle = { isCategoryExpanded = !isCategoryExpanded },
                                selectedCategoryItem = if (uiState.isEditMode) uiState.editedCategoryItem else uiState.categoryItem,
                                categories = categories,
                                onCategorySelected = { categoryItem ->
                                    viewModel.updateSelectedCategory(categoryItem)
                                },
                                onManageCategoriesClicked = onNavigateToManageCategories,
                                modifier = Modifier.fillMaxWidth(),
                                isEditMode = uiState.isEditMode
                            )

                            com.nextthing.app.presentation.screens.create.ImportanceConfigCard(
                                screenHeight = screenHeight,
                                screenWidth = screenWidth,
                                isExpanded = isImportanceExpanded,
                                onExpandToggle = { isImportanceExpanded = !isImportanceExpanded },
                                selectedImportanceUrgency = if (uiState.isEditMode) uiState.editedImportanceUrgency else uiState.task!!.importanceUrgency,
                                onImportanceUrgencySelected = { importance ->
                                    viewModel.updateEditedImportanceUrgency(importance)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                isEditMode = uiState.isEditMode
                            )
                        }

                        // 第三行：任务图片 + 重复频次
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            com.nextthing.app.presentation.screens.create.ImageConfigCard(
                                screenHeight = screenHeight,
                                screenWidth = screenWidth,
                                isExpanded = isImageExpanded,
                                onExpandToggle = { isImageExpanded = !isImageExpanded },
                                selectedImageUri = if (uiState.isEditMode) uiState.editedImageUri else uiState.task!!.imageUri,
                                onImageSelected = { uri ->
                                    viewModel.updateEditedImagePath(uri)
                                },
                                onImageCleared = {
                                    viewModel.updateEditedImagePath(null)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                isEditMode = uiState.isEditMode
                            )

                            com.nextthing.app.presentation.screens.create.RepeatFrequencyConfigCard(
                                screenHeight = screenHeight,
                                screenWidth = screenWidth,
                                isExpanded = isRepeatExpanded,
                                onExpandToggle = { isRepeatExpanded = !isRepeatExpanded },
                                repeatFrequency = if (uiState.isEditMode) uiState.editedRepeatFrequency else uiState.task!!.repeatFrequency,
                                onRepeatFrequencyTypeChange = { type ->
                                    viewModel.updateEditedRepeatFrequency(
                                        uiState.editedRepeatFrequency.copy(type = type)
                                    )
                                },
                                onWeekdaysChange = { weekdays ->
                                    viewModel.updateEditedRepeatFrequency(
                                        uiState.editedRepeatFrequency.copy(weekdays = weekdays)
                                    )
                                },
                                onMonthDaysChange = { monthDays ->
                                    viewModel.updateEditedRepeatFrequency(
                                        uiState.editedRepeatFrequency.copy(monthDays = monthDays)
                                    )
                                },
                                onNavigateToRepeatCustom = onNavigateToRepeatCustom,
                                modifier = Modifier.fillMaxWidth(),
                                isEditMode = uiState.isEditMode
                            )
                        }

                        // 第四行：通知策略（独占一行）
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            com.nextthing.app.presentation.screens.create.NotificationStrategyConfigCard(
                                screenHeight = screenHeight,
                                screenWidth = screenWidth,
                                isExpanded = isNotificationExpanded,
                                onExpandToggle = { isNotificationExpanded = !isNotificationExpanded },
                                availableStrategies = availableNotificationStrategies,
                                selectedStrategyId = if (uiState.isEditMode) uiState.editedNotificationStrategyId else uiState.task?.notificationStrategyId,
                                onStrategySelected = { strategyId ->
                                    viewModel.updateNotificationStrategy(strategyId)
                                },
                                onNavigateToCreateNotificationStrategy = {
                                    // 详情页暂不支持创建新通知策略,请在创建任务页使用该功能
                                },
                                modifier = Modifier.fillMaxWidth(),
                                isEditMode = uiState.isEditMode
                            )
                        }

                        // 第五行：地理围栏状态（独占一行）
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TaskGeofenceStatusCard(
                                taskGeofence = uiState.taskGeofence,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // 第六行：子任务
                        TaskSubtasksCard(
                            task = uiState.task!!,
                            isEditMode = uiState.isEditMode,
                            editedSubtasks = uiState.editedSubtasks,
                            onSubtasksChange = { viewModel.updateEditedSubtasks(it) },
                            onSubtaskStatusToggle = { viewModel.toggleSubtaskStatus(it) },
                            isGeneratingSubtasks = uiState.isGeneratingSubtasks,
                            onGenerateAISubtasks = { viewModel.generateAISubtasks() }
                        )

                        // AI 按钮组（非编辑模式）
                        if (!uiState.isEditMode) {
                            // AI 日程建议
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !uiState.isGeneratingAdvice) {
                                        viewModel.generateScheduleAdvice()
                                    }
                                    .background(
                                        color = com.nextthing.app.presentation.theme.Primary.copy(alpha = 0.06f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (uiState.isGeneratingAdvice) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = com.nextthing.app.presentation.theme.Primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("分析中...", fontSize = 14.sp, color = com.nextthing.app.presentation.theme.Primary)
                                } else {
                                    Text("🧠", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("AI 日程建议", fontSize = 14.sp, color = com.nextthing.app.presentation.theme.Primary)
                                }
                            }

                            // AI 时间预估
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !uiState.isEstimatingTime) {
                                        viewModel.estimateTaskTime()
                                    }
                                    .background(
                                        color = Color(0xFF4CAF50).copy(alpha = 0.06f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (uiState.isEstimatingTime) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFF4CAF50)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("预估中...", fontSize = 14.sp, color = Color(0xFF4CAF50))
                                } else {
                                    Text("⏱", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("AI 时间预估", fontSize = 14.sp, color = Color(0xFF4CAF50))
                                }
                            }

                            // AI 拖延预警（仅 OVERDUE 状态）
                            if (uiState.task?.status == com.nextthing.app.domain.model.TaskStatus.OVERDUE) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = !uiState.isDetectingProcrastination) {
                                            viewModel.detectProcrastination()
                                        }
                                        .background(
                                            color = Color(0xFFFF9800).copy(alpha = 0.06f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (uiState.isDetectingProcrastination) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = Color(0xFFFF9800)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("分析中...", fontSize = 14.sp, color = Color(0xFFFF9800))
                                    } else {
                                        Text("⚠️", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("AI 拖延预警", fontSize = 14.sp, color = Color(0xFFFF9800))
                                    }
                                }
                            }
                        }
                    }

                    // 编辑模式底部按钮
                    if (uiState.isEditMode) {
                        EditModeBottomButtons(
                            onCancel = { viewModel.exitEditMode() },
                            onSave = { viewModel.saveChanges() }
                        )
                    }
                }
                }
                }
            }
            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.errorMessage ?: "任务不存在",
                        color = Color(0xFF9E9E9E)
                    )
                }
            }
        }

        // 删除确认弹窗（普通任务）
        if (uiState.showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.hideDeleteConfirmDialog() },
                title = { Text("确认删除") },
                text = { Text("确定要删除这个任务吗？此操作无法撤销。") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteTask()
                            viewModel.hideDeleteConfirmDialog()
                            onBackPressed()
                        }
                    ) {
                        Text("删除", color = Color(0xFFEF5350))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.hideDeleteConfirmDialog() }) {
                        Text("取消")
                    }
                }
            )
        }

        // 重复任务删除选项对话框
        if (uiState.showRecurringDeleteDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.hideRecurringDeleteDialog() },
                title = { Text("删除重复任务") },
                text = {
                    Column {
                        Text("这是一个重复任务，请选择删除方式：")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "• 仅删除此任务：只删除今天的任务，未来还会继续生成",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "• 删除所有重复任务：删除此任务的所有重复，未来不再生成",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                },
                confirmButton = {
                    Column {
                        // 删除所有重复任务按钮
                        TextButton(
                            onClick = {
                                viewModel.deleteTask(
                                    com.nextthing.app.domain.model.DeleteMode.DELETE_ALL_RECURRING
                                )
                                viewModel.hideRecurringDeleteDialog()
                                onBackPressed()
                            }
                        ) {
                            Text("删除所有重复任务", color = Color(0xFFEF5350))
                        }
                        // 仅删除此任务按钮
                        TextButton(
                            onClick = {
                                viewModel.deleteTask(
                                    com.nextthing.app.domain.model.DeleteMode.DELETE_THIS_ONLY
                                )
                                viewModel.hideRecurringDeleteDialog()
                                onBackPressed()
                            }
                        ) {
                            Text("仅删除此任务", color = Color(0xFFFF9800))
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.hideRecurringDeleteDialog() }) {
                        Text("取消")
                    }
                }
            )
        }

        // 日期选择器对话框
        if (showDatePicker) {
            com.nextthing.app.presentation.screens.create.MaterialDatePickerDialog(
                onDateSelected = { date ->
                    viewModel.updateEditedDueDate(date.atStartOfDay())
                    showDatePicker = false
                },
                onDismiss = { showDatePicker = false }
            )
        }

        // AI 子任务建议弹窗
        if (uiState.showSubtaskSuggestionDialog && uiState.aiSubtaskSuggestions.isNotEmpty()) {
            SubtaskSuggestionDialog(
                suggestions = uiState.aiSubtaskSuggestions,
                onAccept = { viewModel.acceptSubtasks(it) },
                onDismiss = { viewModel.dismissSubtaskSuggestions() }
            )
        }

        // AI 日程建议弹窗
        if (uiState.showScheduleAdviceDialog) {
            uiState.aiScheduleAdvice?.let { advice ->
                ScheduleAdviceDialog(
                    advice = advice,
                    onDismiss = { viewModel.dismissScheduleAdvice() }
                )
            }
        }

        // AI 时间预估弹窗
        if (uiState.showTimeEstimateDialog) {
            uiState.aiTimeEstimate?.let { estimate ->
                TimeEstimateDialog(
                    estimate = estimate,
                    onDismiss = { viewModel.dismissTimeEstimate() }
                )
            }
        }

        // AI 拖延预警弹窗
        if (uiState.showProcrastinationDialog) {
            uiState.aiProcrastinationAdvice?.let { advice ->
                ProcrastinationDialog(
                    advice = advice,
                    onDismiss = { viewModel.dismissProcrastination() }
                )
            }
        }


    }
}

@Composable
private fun RefinedTaskEditContent(
    title: String,
    description: String,
    status: TaskStatus,
    categoryName: String,
    selectedCategoryId: String?,
    categories: List<com.nextthing.app.domain.model.CategoryItem>,
    notificationStrategies: List<com.nextthing.app.domain.model.NotificationStrategy>,
    selectedNotificationStrategyId: String?,
    geofenceLocations: List<com.nextthing.app.domain.model.GeofenceLocation>,
    geofenceEnabled: Boolean,
    selectedGeofenceLocationId: String?,
    importance: TaskImportanceUrgency?,
    dueDate: LocalDateTime?,
    repeatFrequency: RepeatFrequency,
    notificationName: String,
    imageUri: String?,
    locationName: String,
    subtasks: List<Subtask>,
    onBack: () -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDueDateChange: (LocalDateTime?) -> Unit,
    onCategorySelected: (com.nextthing.app.domain.model.CategoryItem) -> Unit,
    onManageCategories: () -> Unit,
    onImportanceSelected: (TaskImportanceUrgency) -> Unit,
    onRepeatSelected: (RepeatFrequency) -> Unit,
    onNavigateToRepeatCustom: () -> Unit,
    onNotificationSelected: (String?) -> Unit,
    onManageNotificationStrategies: () -> Unit,
    onImageSelected: (String?) -> Unit,
    onNavigateToGeofenceConfig: () -> Unit,
    onNavigateToGeofenceAdd: () -> Unit,
    onGeofenceEnabledChange: (Boolean) -> Unit,
    onGeofenceLocationSelected: (String?) -> Unit,
    onStatusChange: (TaskStatus) -> Unit,
    onGenerateSubtasks: () -> Unit,
    onScheduleAdvice: () -> Unit,
    onEstimateTime: () -> Unit,
    isGeneratingSubtasks: Boolean,
    isGeneratingAdvice: Boolean,
    isEstimatingTime: Boolean,
    onSave: () -> Unit
) {
    var showDateQuickOptions by remember { mutableStateOf(false) }
    var showCalendarPicker by remember { mutableStateOf(false) }
    var showPreciseTimePicker by remember { mutableStateOf(false) }
    var isCategoryExpanded by remember { mutableStateOf(false) }
    var isImportanceExpanded by remember { mutableStateOf(false) }
    var expandedPropertyIndex by remember { mutableStateOf<Int?>(null) }
    var showGeofenceSelector by remember { mutableStateOf(false) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            onImageSelected(it.toString())
            expandedPropertyIndex = null
        }
    }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF5F7FC)).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.icon_detail_back), "返回", Modifier.size(36.dp))
            }
            Text("编辑任务", Modifier.weight(1f), Color(0xFF0E131D), 18.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "保存",
                modifier = Modifier.clickable(onClick = onSave),
                color = Color(0xFF1A7DFA),
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            DetailSectionTitle("基本信息")
            EditTextBox("任务标题", title, 60.dp, Color(0xFF1A7DFA), onTitleChange)
            EditTextBox("任务描述", description, 94.dp, Color(0xFFDEE5F0), onDescriptionChange)
            DetailSectionTitle("任务状态")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EditStatusChip("当前：${getStatusDisplayName(status)}", Color(0xFFEBF5FF), Color(0xFF1A7DFA), null)
                EditStatusChip("改为已完成", Color(0xFFE8FAF0), Color(0xFF12A85C)) { onStatusChange(TaskStatus.COMPLETED) }
                EditStatusChip("改为已放弃", Color(0xFFFFEDED), Color(0xFFF2383D)) { onStatusChange(TaskStatus.CANCELLED) }
            }
            DetailSectionTitle("时间安排")
            EditableScheduleOverview(
                dueDate = dueDate,
                onDateClick = { showDateQuickOptions = true },
                onTimeClick = { showPreciseTimePicker = true }
            )
            DetailSectionTitle("任务属性")
            EditRowsCard(
                rows = listOf(
                EditRowData(R.drawable.icon_detail_category, "分类", categoryName, true),
                EditRowData(R.drawable.icon_detail_importance, "重要程度", importance?.displayName ?: "未设置", true, Color(0xFFF2383D)),
                EditRowData(R.drawable.icon_detail_repeat, "重复频次", repeatSummary(repeatFrequency), true),
                EditRowData(R.drawable.icon_detail_notification, "通知策略", notificationName, true),
                EditRowData(R.drawable.icon_detail_image, "任务图片", if (imageUri == null) "未添加" else "已添加 1 张", true),
                EditRowData(R.drawable.icon_detail_location, "地理围栏", locationName, true)
                ),
                onRowClick = { index ->
                    when (index) {
                        0 -> {
                            isCategoryExpanded = !isCategoryExpanded
                            isImportanceExpanded = false
                            expandedPropertyIndex = null
                        }
                        1 -> {
                            isImportanceExpanded = !isImportanceExpanded
                            isCategoryExpanded = false
                            expandedPropertyIndex = null
                        }
                        2, 3, 4 -> {
                            if (index == 4 && imageUri == null) {
                                imagePicker.launch("image/*")
                                expandedPropertyIndex = null
                            } else {
                                expandedPropertyIndex = if (expandedPropertyIndex == index) null else index
                            }
                            isCategoryExpanded = false
                            isImportanceExpanded = false
                        }
                        5 -> {
                            expandedPropertyIndex = null
                            isCategoryExpanded = false
                            isImportanceExpanded = false
                            showGeofenceSelector = true
                        }
                    }
                },
                afterRow = { index ->
                    if (index == 0 && isCategoryExpanded) {
                        CategoryInlineSelector(
                            categories = categories,
                            selectedCategoryId = selectedCategoryId,
                            onSelect = {
                                onCategorySelected(it)
                                isCategoryExpanded = false
                            },
                            onManage = onManageCategories
                        )
                    }
                    if (index == 1 && isImportanceExpanded) {
                        ImportanceInlineSelector(
                            selectedImportance = importance,
                            onSelect = {
                                onImportanceSelected(it)
                                isImportanceExpanded = false
                            }
                        )
                    }
                    if (index == 2 && expandedPropertyIndex == 2) {
                        RepeatInlineSelector(
                            repeatFrequency = repeatFrequency,
                            onSelect = {
                                onRepeatSelected(it)
                                expandedPropertyIndex = null
                            },
                            onCustom = {
                                expandedPropertyIndex = null
                                onNavigateToRepeatCustom()
                            }
                        )
                    }
                    if (index == 3 && expandedPropertyIndex == 3) {
                        NotificationInlineSelector(
                            strategies = notificationStrategies,
                            selectedStrategyId = selectedNotificationStrategyId,
                            onSelect = {
                                onNotificationSelected(it)
                                expandedPropertyIndex = null
                            },
                            onManage = onManageNotificationStrategies
                        )
                    }
                    if (index == 4 && expandedPropertyIndex == 4) {
                        ImageInlineSelector(
                            imageUri = imageUri,
                            onChoose = { imagePicker.launch("image/*") },
                            onRemove = {
                                onImageSelected(null)
                                expandedPropertyIndex = null
                            }
                        )
                    }
                }
            )
            DetailSectionTitle("子任务")
            Column(Modifier.fillMaxWidth().border(1.dp, Color(0xFFDEE5F0), RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp))) {
                subtasks.forEachIndexed { index, subtask ->
                    Row(Modifier.fillMaxWidth().height(46.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(22.dp).clip(RoundedCornerShape(6.dp)).background(if (subtask.isCompleted) Color(0xFF12A85C) else Color.White).border(1.dp, if (subtask.isCompleted) Color(0xFF12A85C) else Color(0xFFDEE5F0), RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                            if (subtask.isCompleted) Image(painterResource(R.drawable.icon_detail_check), null, Modifier.size(14.dp))
                        }
                        Text(subtask.title, Modifier.weight(1f), Color(0xFF0E131D), 13.sp, fontWeight = FontWeight.Medium)
                        Text("移除", color = Color(0xFFF2383D), fontSize = 11.sp)
                    }
                    if (index < subtasks.lastIndex) HorizontalDivider(color = Color(0xFFE8EDF5))
                }
                if (subtasks.isNotEmpty()) HorizontalDivider(color = Color(0xFFE8EDF5))
                Row(Modifier.fillMaxWidth().height(46.dp).padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Add, null, tint = Color(0xFF1A7DFA), modifier = Modifier.size(18.dp))
                    Text("添加子任务", color = Color(0xFF1A7DFA), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
            DetailSectionTitle("AI 辅助")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EditAiActionRow(
                    iconRes = R.drawable.icon_detail_ai_subtask,
                    title = if (isGeneratingSubtasks) "生成中..." else "AI 生成子任务",
                    subtitle = "拆分为可执行步骤",
                    tint = Color(0xFF1A7DFA),
                    enabled = !isGeneratingSubtasks,
                    onClick = onGenerateSubtasks
                )
                EditAiActionRow(
                    iconRes = R.drawable.icon_detail_ai_schedule,
                    title = if (isGeneratingAdvice) "分析中..." else "AI 日程建议",
                    subtitle = "结合截止时间安排执行顺序",
                    tint = Color(0xFF12A85C),
                    enabled = !isGeneratingAdvice,
                    onClick = onScheduleAdvice
                )
                EditAiActionRow(
                    iconRes = R.drawable.icon_detail_ai_estimate,
                    title = if (isEstimatingTime) "预估中..." else "AI 时间预估",
                    subtitle = "预估完成所需时长",
                    tint = Color(0xFFF59E0A),
                    enabled = !isEstimatingTime,
                    onClick = onEstimateTime
                )
            }
        }
    }

    if (showGeofenceSelector) {
        TaskGeofenceSelectorDialog(
            locations = geofenceLocations,
            initialEnabled = geofenceEnabled,
            initialLocationId = selectedGeofenceLocationId,
            onDismiss = { showGeofenceSelector = false },
            onComplete = { enabled, locationId ->
                onGeofenceLocationSelected(locationId)
                onGeofenceEnabledChange(enabled)
                showGeofenceSelector = false
            },
            onAddLocation = {
                showGeofenceSelector = false
                onNavigateToGeofenceAdd()
            },
            onManageLocations = {
                showGeofenceSelector = false
                onNavigateToGeofenceConfig()
            }
        )
    }
    if (showDateQuickOptions) {
        DateQuickOptionsDialog(
            onDismiss = { showDateQuickOptions = false },
            onSelectDate = { date ->
                val time = dueDate?.toLocalTime() ?: java.time.LocalTime.of(23, 59)
                onDueDateChange(date.atTime(time))
                showDateQuickOptions = false
            },
            onOpenCalendar = {
                showDateQuickOptions = false
                showCalendarPicker = true
            }
        )
    }
    if (showCalendarPicker) {
        CompactCalendarDialog(
            initialDate = dueDate?.toLocalDate() ?: LocalDate.now(),
            onDateSelected = { date ->
                val time = dueDate?.toLocalTime() ?: java.time.LocalTime.of(23, 59)
                onDueDateChange(date.atTime(time))
                showCalendarPicker = false
            },
            onDismiss = { showCalendarPicker = false }
        )
    }
    if (showPreciseTimePicker) {
        val initial = dueDate ?: LocalDateTime.now()
        com.nextthing.app.presentation.screens.create.PreciseTimePickerDialog(
            initialHour = initial.hour,
            initialMinute = initial.minute,
            onDismiss = { showPreciseTimePicker = false },
            onClear = {
                onDueDateChange(dueDate?.toLocalDate()?.atTime(23, 59))
                showPreciseTimePicker = false
            },
            onConfirm = { hour, minute ->
                onDueDateChange((dueDate?.toLocalDate() ?: LocalDate.now()).atTime(hour, minute))
                showPreciseTimePicker = false
            }
        )
    }
}

@Composable
private fun TaskGeofenceSelectorDialog(
    locations: List<com.nextthing.app.domain.model.GeofenceLocation>,
    initialEnabled: Boolean,
    initialLocationId: String?,
    onDismiss: () -> Unit,
    onComplete: (Boolean, String?) -> Unit,
    onAddLocation: () -> Unit,
    onManageLocations: () -> Unit
) {
    var enabled by remember(initialEnabled) { mutableStateOf(initialEnabled) }
    var selectedId by remember(initialLocationId) { mutableStateOf(initialLocationId) }
    val canComplete = !enabled || selectedId != null
    val borderColor = Color(0xFFD6E0ED)
    val accentColor = Color(0xFF1A7DFA)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp)
                .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp)).background(Color.White).padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(Modifier.fillMaxWidth().height(42.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("任务地理围栏", Modifier.weight(1f), Color(0xFF0F1726), 17.sp, fontWeight = FontWeight.Bold)
                Text(
                    "完成",
                    Modifier.clickable(enabled = canComplete) { onComplete(enabled, selectedId) },
                    color = if (canComplete) Color(0xFF1A7DFA) else Color(0xFFB6C0CE),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            HorizontalDivider(color = Color(0xFFE8EDF5))
            Row(Modifier.fillMaxWidth().height(58.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("启用地理围栏", color = Color(0xFF0F1726), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("到达所选地点附近时提醒", color = Color(0xFF61738F), fontSize = 11.sp)
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { enabled = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF1A7DFA))
                )
            }
            if (enabled) {
                HorizontalDivider(color = Color(0xFFE8EDF5))
                Text("选择地点", Modifier.padding(vertical = 10.dp), Color(0xFF0F1726), 13.sp, fontWeight = FontWeight.SemiBold)
                if (locations.isEmpty()) {
                    Column(
                        Modifier.fillMaxWidth().background(Color(0xFFF7F8FC), RoundedCornerShape(8.dp)).padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("还没有可用地点", color = Color(0xFF61738F), fontSize = 13.sp)
                        Text("新建地点", Modifier.clickable(onClick = onAddLocation), color = Color(0xFF1A7DFA), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 280.dp)) {
                        items(locations, key = { it.id }) { location ->
                            val selected = selectedId == location.id
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) Color(0xFFE8F5FF) else Color.Transparent)
                                    .clickable { selectedId = location.id }.padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RadioButton(selected = selected, onClick = { selectedId = location.id }, colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF1A7DFA)))
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(location.locationInfo.locationName.ifBlank { "未命名地点" }, color = if (selected) Color(0xFF1A7DFA) else Color(0xFF0F1726), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${location.locationInfo.address.ifBlank { "未填写地址" }} · 半径 ${location.customRadius ?: 200}m",
                                        color = Color(0xFF61738F), fontSize = 11.sp, maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                            HorizontalDivider(Modifier.padding(start = 48.dp), color = Color(0xFFE8EDF5))
                        }
                    }
                }
                HorizontalDivider(Modifier.padding(top = 8.dp), color = Color(0xFFE8EDF5))
                Row(
                    Modifier.fillMaxWidth().height(42.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        "管理常用地点",
                        Modifier.clickable(onClick = onManageLocations),
                        color = accentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

private data class EditRowData(val icon: Int?, val label: String, val value: String, val editable: Boolean, val valueColor: Color = Color(0xFF0E131D))

@Composable
private fun DateQuickOptionsDialog(
    onDismiss: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onOpenCalendar: () -> Unit
) {
    val today = LocalDate.now()
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.border(1.dp, Color(0x6618202C), RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        containerColor = Color.White,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("选择提示日期", color = Color(0xFF0E131D), fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold)
                Text("快速选择，或打开完整日历", color = Color(0xFF5C6B82), fontSize = 13.sp, lineHeight = 18.sp)
            }
        },
        text = {
            Column(Modifier.fillMaxWidth().border(1.dp, Color(0xFFDEE5F0), RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp))) {
                DateQuickOption("今天", today.format(DateTimeFormatter.ofPattern("M 月 d 日"))) { onSelectDate(today) }
                HorizontalDivider(color = Color(0xFFE8EDF5))
                DateQuickOption("明天", today.plusDays(1).format(DateTimeFormatter.ofPattern("M 月 d 日"))) { onSelectDate(today.plusDays(1)) }
                HorizontalDivider(color = Color(0xFFE8EDF5))
                DateQuickOption("从日历中选择", "选择其他日期", Color(0xFF1A7DFA), onOpenCalendar)
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@Composable
private fun DateQuickOption(title: String, subtitle: String, titleColor: Color = Color(0xFF0E131D), onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(58.dp).clickable(onClick = onClick).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = titleColor, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Color(0xFF91A1B5), fontSize = 12.sp, lineHeight = 16.sp)
        }
        Icon(painterResource(R.drawable.icon_detail_chevron), null, tint = Color(0xFF91A1B5), modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun CompactCalendarDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var visibleMonth by remember(initialDate) { mutableStateOf(java.time.YearMonth.from(initialDate)) }
    var selectedDate by remember(initialDate) { mutableStateOf(initialDate) }
    val leadingDays = visibleMonth.atDay(1).dayOfWeek.value - 1
    val daysInMonth = visibleMonth.lengthOfMonth()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .border(1.dp, Color(0x6618202C), RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("选择提示日期", color = Color(0xFF0E131D), fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold)
                    Text("${visibleMonth.year} 年 ${visibleMonth.monthValue} 月", color = Color(0xFF5C6B82), fontSize = 13.sp, lineHeight = 18.sp)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Close, "关闭", tint = Color(0xFF5C6B82), modifier = Modifier.size(20.dp))
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { visibleMonth = visibleMonth.minusMonths(1) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.KeyboardArrowLeft, "上个月", tint = Color(0xFF5C6B82), modifier = Modifier.size(20.dp))
                }
                Text(
                    text = "${visibleMonth.year} 年 ${visibleMonth.monthValue} 月",
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF0E131D),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                IconButton(onClick = { visibleMonth = visibleMonth.plusMonths(1) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.KeyboardArrowRight, "下个月", tint = Color(0xFF5C6B82), modifier = Modifier.size(20.dp))
                }
            }
            Row(Modifier.fillMaxWidth().height(26.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEach { day ->
                    Text(day, Modifier.weight(1f), Color(0xFF91A1B5), 12.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                val weekCount = (leadingDays + daysInMonth + 6) / 7
                repeat(weekCount) { week ->
                    Row(Modifier.fillMaxWidth()) {
                        repeat(7) { weekday ->
                            val dayNumber = week * 7 + weekday - leadingDays + 1
                            Box(Modifier.weight(1f).height(36.dp), contentAlignment = Alignment.Center) {
                                if (dayNumber in 1..daysInMonth) {
                                    val date = visibleMonth.atDay(dayNumber)
                                    val selected = date == selectedDate
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (selected) Color(0xFF1A7DFA) else Color.Transparent)
                                            .clickable { selectedDate = date },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(dayNumber.toString(), color = if (selected) Color.White else Color(0xFF0E131D), fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) { Text("取消", color = Color(0xFF0E131D), fontSize = 14.sp) }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onDateSelected(selectedDate) },
                    modifier = Modifier.height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A7DFA)),
                    contentPadding = PaddingValues(horizontal = 18.dp)
                ) { Text("确定", color = Color.White, fontSize = 14.sp) }
            }
        }
    }
}

@Composable
private fun EditAiActionRow(
    iconRes: Int,
    title: String,
    subtitle: String,
    tint: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .border(1.dp, Color(0xFFDEE5F0), RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
            Image(painterResource(iconRes), null, Modifier.size(34.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = Color(0xFF0E131D), fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color(0xFF5C6B82), fontSize = 11.sp, lineHeight = 15.sp)
        }
        Icon(painterResource(R.drawable.icon_detail_chevron), null, tint = tint, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun EditableScheduleOverview(dueDate: LocalDateTime?, onDateClick: () -> Unit, onTimeClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .border(1.dp, Color(0xFFDEE5F0), RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ScheduleValue(
            modifier = Modifier.weight(1f).clickable(onClick = onDateClick),
            iconRes = R.drawable.icon_detail_due_date,
            label = "截止日期",
            value = formatDueDate(dueDate),
            valueColor = Color(0xFF0E131D)
        )
        Box(Modifier.width(1.dp).height(48.dp).background(Color(0xFFE8EDF5)))
        ScheduleValue(
            modifier = Modifier.weight(1f).clickable(onClick = onTimeClick),
            iconRes = R.drawable.icon_detail_precise_time,
            label = "精确时间",
            value = dueDate?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "未设置",
            valueColor = Color(0xFFF2383D)
        )
    }
}

@Composable
private fun EditTextBox(label: String, value: String, height: androidx.compose.ui.unit.Dp, borderColor: Color, onChange: (String) -> Unit) {
    BasicTextField(value, onChange, Modifier.fillMaxWidth().height(height), textStyle = TextStyle(Color(0xFF0E131D), if (label == "任务标题") 16.sp else 14.sp, lineHeight = if (label == "任务标题") 19.sp else 21.sp), decorationBox = { field ->
        Column(Modifier.fillMaxSize().border(1.dp, borderColor, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(label, color = Color(0xFF91A1B5), fontSize = 11.sp, lineHeight = 15.sp)
            Box { if (value.isEmpty()) Text("请输入$label", color = Color(0xFF91A1B5), fontSize = 14.sp); field() }
        }
    })
}

@Composable
private fun EditStatusChip(text: String, background: Color, foreground: Color, onClick: (() -> Unit)?) {
    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(background).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 10.dp, vertical = 7.dp)) {
        Text(text, color = foreground, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
private fun EditRowsCard(
    rows: List<EditRowData>,
    onRowClick: (Int) -> Unit = {},
    afterRow: @Composable (Int) -> Unit = {}
) {
    Column(Modifier.fillMaxWidth().border(1.dp, Color(0xFFDEE5F0), RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp))) {
        rows.forEachIndexed { index, row ->
            Row(
                Modifier.fillMaxWidth().height(58.dp).then(if (row.editable) Modifier.clickable { onRowClick(index) } else Modifier).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (row.icon != null) {
                    Image(painterResource(row.icon), null, Modifier.size(34.dp))
                } else {
                    Box(Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFEBF5FF)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, null, tint = Color(0xFF1A7DFA), modifier = Modifier.size(18.dp))
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(row.label, color = Color(0xFF91A1B5), fontSize = 11.sp, lineHeight = 15.sp)
                    Text(row.value, color = row.valueColor, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                }
                if (row.editable) Icon(painterResource(R.drawable.icon_detail_chevron), null, tint = Color(0xFF91A1B5), modifier = Modifier.size(16.dp)) else Text("只读", color = Color(0xFF91A1B5), fontSize = 11.sp)
            }
            afterRow(index)
            if (index < rows.lastIndex) HorizontalDivider(color = Color(0xFFE8EDF5))
        }
    }
}

@Composable
private fun CategoryInlineSelector(
    categories: List<com.nextthing.app.domain.model.CategoryItem>,
    selectedCategoryId: String?,
    onSelect: (com.nextthing.app.domain.model.CategoryItem) -> Unit,
    onManage: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFD6E0ED), RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Text(
            "选择分类",
            modifier = Modifier.padding(bottom = 8.dp),
            color = Color(0xFF0F1726),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        categories.forEachIndexed { index, category ->
            val selected = category.id == selectedCategoryId
            val categoryColor = try {
                Color(android.graphics.Color.parseColor(category.colorHex))
            } catch (_: IllegalArgumentException) {
                Color(0xFF1A7DFA)
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) Color(0xFFE8F5FF) else Color.Transparent)
                    .clickable { onSelect(category) }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(categoryColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(category.displayName.take(1), color = categoryColor, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(category.displayName, color = if (selected) Color(0xFF1A7DFA) else Color(0xFF0F1726), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(
                        categoryDescription(category.displayName),
                        color = Color(0xFF61738F),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                if (selected) Text("✓", color = Color(0xFF1A7DFA), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            if (index < categories.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 46.dp, end = 10.dp),
                    thickness = 1.dp,
                    color = Color(0xFFE8EDF5)
                )
            }
        }
        Row(Modifier.fillMaxWidth().clickable(onClick = onManage).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("管理分类", Modifier.weight(1f), Color(0xFF1A7DFA), 14.sp, fontWeight = FontWeight.Medium)
            Icon(painterResource(R.drawable.icon_detail_chevron), null, tint = Color(0xFF1A7DFA), modifier = Modifier.size(18.dp))
        }
    }
}

private fun categoryDescription(name: String): String = when (name) {
    "工作" -> "项目、会议与职业事项"
    "个人" -> "个人成长与日常安排"
    "家庭" -> "家庭与生活事务"
    else -> "自定义任务分类"
}

@Composable
private fun ImportanceInlineSelector(
    selectedImportance: TaskImportanceUrgency?,
    onSelect: (TaskImportanceUrgency) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFD6E0ED), RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Text(
            text = "选择重要程度",
            modifier = Modifier.padding(bottom = 8.dp),
            color = Color(0xFF0F1726),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        TaskImportanceUrgency.entries.forEachIndexed { index, option ->
            val selected = option == selectedImportance
            val optionColor = Color(android.graphics.Color.parseColor(option.colorHex))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) optionColor.copy(alpha = 0.10f) else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(optionColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = importanceShortLabel(option),
                        color = optionColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = option.displayName,
                        color = if (selected) optionColor else Color(0xFF0F1726),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = option.description,
                        color = Color(0xFF61738F),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                if (selected) {
                    Text("✓", color = optionColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            if (index < TaskImportanceUrgency.entries.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 46.dp, end = 10.dp),
                    thickness = 1.dp,
                    color = Color(0xFFE8EDF5)
                )
            }
        }
    }
}

private fun importanceShortLabel(option: TaskImportanceUrgency): String = when (option) {
    TaskImportanceUrgency.IMPORTANT_URGENT -> "急"
    TaskImportanceUrgency.IMPORTANT_NOT_URGENT -> "重"
    TaskImportanceUrgency.NOT_IMPORTANT_URGENT -> "速"
    TaskImportanceUrgency.NOT_IMPORTANT_NOT_URGENT -> "缓"
}

@Composable
private fun RepeatInlineSelector(
    repeatFrequency: RepeatFrequency,
    onSelect: (RepeatFrequency) -> Unit,
    onCustom: () -> Unit
) {
    val options = listOf(
        RepeatOptionVisual(com.nextthing.app.domain.model.RepeatFrequencyType.NONE, "单次任务", R.drawable.icon_repeat_once, Color(0xFF1A7DFA)),
        RepeatOptionVisual(com.nextthing.app.domain.model.RepeatFrequencyType.DAILY, "每日", R.drawable.icon_repeat_daily, Color(0xFF0EA5A8)),
        RepeatOptionVisual(com.nextthing.app.domain.model.RepeatFrequencyType.WEEKDAYS, "工作日", R.drawable.icon_repeat_workday, Color(0xFF4F6BED)),
        RepeatOptionVisual(com.nextthing.app.domain.model.RepeatFrequencyType.WEEKENDS, "周末", R.drawable.icon_repeat_weekend, Color(0xFF12A85C)),
        RepeatOptionVisual(com.nextthing.app.domain.model.RepeatFrequencyType.LEGAL_HOLIDAY, "法定节假日", R.drawable.icon_repeat_holiday, Color(0xFFF2383D))
    )
    InlinePropertyPanel("选择重复频次") {
        options.forEach { option ->
            InlineChoiceRow(
                title = option.label,
                subtitle = repeatDescription(option.type),
                selected = repeatFrequency.type == option.type,
                accent = option.color,
                iconRes = option.iconRes,
                onClick = { onSelect(RepeatFrequency(type = option.type)) }
            )
            HorizontalDivider(color = Color(0xFFE8EDF5), modifier = Modifier.padding(start = 46.dp))
        }
        InlineChoiceRow(
            title = "自定义重复",
            subtitle = "按星期或每月日期设置",
            selected = repeatFrequency.type == com.nextthing.app.domain.model.RepeatFrequencyType.WEEKLY || repeatFrequency.type == com.nextthing.app.domain.model.RepeatFrequencyType.MONTHLY,
            accent = Color(0xFFF59E0B),
            iconRes = R.drawable.icon_repeat_custom,
            onClick = onCustom,
            showChevron = true
        )
    }
}

private data class RepeatOptionVisual(
    val type: com.nextthing.app.domain.model.RepeatFrequencyType,
    val label: String,
    val iconRes: Int,
    val color: Color
)

private fun repeatDescription(type: com.nextthing.app.domain.model.RepeatFrequencyType): String = when (type) {
    com.nextthing.app.domain.model.RepeatFrequencyType.NONE -> "仅执行一次"
    com.nextthing.app.domain.model.RepeatFrequencyType.DAILY -> "每天重复"
    com.nextthing.app.domain.model.RepeatFrequencyType.WEEKDAYS -> "周一至周五"
    com.nextthing.app.domain.model.RepeatFrequencyType.WEEKENDS -> "周六和周日"
    com.nextthing.app.domain.model.RepeatFrequencyType.LEGAL_HOLIDAY -> "法定节假日提醒"
    else -> "自定义周期"
}

@Composable
private fun NotificationInlineSelector(
    strategies: List<com.nextthing.app.domain.model.NotificationStrategy>,
    selectedStrategyId: String?,
    onSelect: (String?) -> Unit,
    onManage: () -> Unit
) {
    InlinePropertyPanel("选择通知策略") {
        InlineChoiceRow(
            title = "不提醒",
            subtitle = "关闭该任务的通知",
            selected = selectedStrategyId == null,
            accent = Color(0xFF61738F),
            iconRes = R.drawable.icon_notification_off,
            onClick = { onSelect(null) }
        )
        HorizontalDivider(color = Color(0xFFE8EDF5), modifier = Modifier.padding(start = 46.dp))
        strategies.forEach { strategy ->
            val visual = notificationVisual(strategy.name)
            InlineChoiceRow(
                title = strategy.name,
                subtitle = if (strategy.advanceReminderMinutes.isEmpty()) "按时提醒" else "提前 ${strategy.advanceReminderMinutes.joinToString("、")} 分钟",
                selected = strategy.id == selectedStrategyId,
                accent = visual.color,
                iconRes = visual.iconRes,
                onClick = { onSelect(strategy.id) }
            )
            HorizontalDivider(color = Color(0xFFE8EDF5), modifier = Modifier.padding(start = 46.dp))
        }
        Row(
            Modifier.fillMaxWidth().height(52.dp).clickable(onClick = onManage).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(painterResource(R.drawable.icon_notification_custom), null, tint = Color(0xFF1A7DFA), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text("管理通知策略", Modifier.weight(1f), Color(0xFF1A7DFA), 14.sp, fontWeight = FontWeight.Medium)
            Icon(painterResource(R.drawable.icon_detail_chevron), null, tint = Color(0xFF1A7DFA), modifier = Modifier.size(18.dp))
        }
    }
}

private data class NotificationVisual(val iconRes: Int, val color: Color)

private fun notificationVisual(name: String): NotificationVisual = when {
    name.contains("重要") -> NotificationVisual(R.drawable.icon_notification_important, Color(0xFFF2383D))
    name.contains("无声") || name.contains("静音") -> NotificationVisual(R.drawable.icon_notification_silent, Color(0xFF0EA5A8))
    name.contains("标准") || name.contains("默认") -> NotificationVisual(R.drawable.icon_notification_standard, Color(0xFF1A7DFA))
    else -> NotificationVisual(R.drawable.icon_notification_custom, Color(0xFFF59E0B))
}

@Composable
private fun ImageInlineSelector(imageUri: String?, onChoose: () -> Unit, onRemove: () -> Unit) {
    if (imageUri == null) return
    Column(
        Modifier.fillMaxWidth().border(1.dp, Color(0xFFD6E0ED), RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp)).background(Color.White).padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AsyncImage(
                model = imageUri,
                contentDescription = "任务图片预览",
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF5F7FC)),
                contentScale = ContentScale.Crop
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("已添加任务图片", color = Color(0xFF0F1726), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("图片将在任务详情中展示", color = Color(0xFF61738F), fontSize = 12.sp)
            }
        }
        HorizontalDivider(Modifier.padding(top = 10.dp), color = Color(0xFFE8EDF5))
        Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "替换图片",
                Modifier.weight(1f).clickable(onClick = onChoose),
                color = Color(0xFF1A7DFA),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Box(Modifier.width(1.dp).height(20.dp).background(Color(0xFFE8EDF5)))
            Text(
                "移除图片",
                Modifier.weight(1f).clickable(onClick = onRemove),
                color = Color(0xFFF2383D),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun InlinePropertyPanel(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().border(1.dp, Color(0xFFD6E0ED), RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp)).padding(8.dp)
    ) {
        Text(title, Modifier.padding(bottom = 8.dp), Color(0xFF0F1726), 15.sp, fontWeight = FontWeight.SemiBold)
        content()
    }
}

@Composable
private fun InlineChoiceRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    iconRes: Int? = null,
    showChevron: Boolean = false
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 58.dp).clip(RoundedCornerShape(8.dp))
            .background(if (selected) accent.copy(alpha = 0.10f) else Color.Transparent)
            .clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(accent.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            if (iconRes != null) {
                Icon(painterResource(iconRes), null, tint = accent, modifier = Modifier.size(18.dp))
            } else {
                Box(Modifier.size(10.dp).clip(CircleShape).background(accent))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = if (selected) accent else Color(0xFF0F1726), fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Color(0xFF61738F), fontSize = 12.sp, lineHeight = 16.sp)
        }
        when {
            showChevron -> Icon(painterResource(R.drawable.icon_detail_chevron), null, tint = accent, modifier = Modifier.size(16.dp))
            selected -> Text("✓", color = accent, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun RefinedTaskDetailContent(
    task: Task,
    categoryName: String,
    notificationName: String,
    onBackPressed: () -> Unit,
    onEditClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF5F7FC))
                    .clickable(onClick = onBackPressed),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.icon_detail_back),
                    contentDescription = "返回",
                    modifier = Modifier.size(36.dp)
                )
            }
            Text(
                text = "任务详情",
                modifier = Modifier.weight(1f),
                color = Color(0xFF0E131D),
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "编辑",
                modifier = Modifier.clickable(onClick = onEditClick),
                color = Color(0xFF1A7DFA),
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TaskIdentitySection(task, categoryName)
            DetailSectionTitle("时间安排")
            ScheduleOverview(task)
            DetailSectionTitle("任务属性")
            DetailSettingsCard(task, notificationName)
            DetailSectionTitle("子任务", "${task.subtasks.count { it.isCompleted }} / ${task.subtasks.size}")
            SubtasksOverview(task.subtasks)
        }
    }
}

@Composable
private fun TaskIdentitySection(task: Task, categoryName: String) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFF2F5FA)) {
                Text(
                    getStatusDisplayName(task.status),
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    color = Color(0xFF5C6B82),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                "更新于${formatUpdatedTime(task.updatedAt)}",
                color = Color(0xFF91A1B5),
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
        Text(task.title, color = Color(0xFF0E131D), fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold)
        if (task.description.isNotBlank()) {
            Text(task.description, color = Color(0xFF5C6B82), fontSize = 14.sp, lineHeight = 22.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DetailChip(categoryName, Color(0xFFEBF5FF), Color(0xFF1A7DFA))
            task.importanceUrgency?.let { DetailChip(compactImportance(it), Color(0xFFFFEDED), Color(0xFFF2383D)) }
        }
    }
}

@Composable
private fun DetailChip(text: String, background: Color, foreground: Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = background) {
        Text(text, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = foreground, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DetailSectionTitle(title: String, action: String? = null) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color(0xFF0E131D), fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        action?.let { Text(it, color = Color(0xFF1A7DFA), fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium) }
    }
}

@Composable
private fun ScheduleOverview(task: Task) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .border(1.dp, Color(0xFFDEE5F0), RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ScheduleValue(Modifier.weight(1f), R.drawable.icon_detail_due_date, "截止日期", formatDueDate(task.dueDate), Color(0xFF0E131D))
        Box(Modifier.width(1.dp).height(48.dp).background(Color(0xFFE8EDF5)))
        ScheduleValue(Modifier.weight(1f), R.drawable.icon_detail_precise_time, "精确时间", task.dueDate?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "未设置", Color(0xFFF2383D))
    }
}

@Composable
private fun ScheduleValue(modifier: Modifier, iconRes: Int, label: String, value: String, valueColor: Color) {
    Row(modifier = modifier.fillMaxHeight().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        DetailIconBox(iconRes)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = Color(0xFF6E7D94), fontSize = 12.sp, lineHeight = 16.sp)
            Text(value, color = valueColor, fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun DetailSettingsCard(task: Task, notificationName: String) {
    Column(modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFDEE5F0), RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp))) {
        val rows = listOf(
            Triple(R.drawable.icon_detail_repeat, "重复频次", repeatSummary(task.repeatFrequency)),
            Triple(R.drawable.icon_detail_notification, "通知策略", notificationName),
            Triple(R.drawable.icon_detail_image, "任务图片", if (task.imageUri == null) "未添加" else "已添加 1 张"),
            Triple(R.drawable.icon_detail_location, "地理围栏", task.locationInfo?.locationName ?: "未设置")
        )
        rows.forEachIndexed { index, row ->
            Row(
                modifier = Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DetailIconBox(row.first)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(row.second, color = Color(0xFF91A1B5), fontSize = 11.sp, lineHeight = 15.sp)
                    Text(row.third, color = Color(0xFF0E131D), fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                }
            }
            if (index < rows.lastIndex) HorizontalDivider(thickness = 1.dp, color = Color(0xFFE8EDF5))
        }
    }
}

@Composable
private fun DetailIconBox(iconRes: Int) {
    Image(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(34.dp))
}

@Composable
private fun SubtasksOverview(subtasks: List<Subtask>) {
    Column(modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFDEE5F0), RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp))) {
        if (subtasks.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(46.dp), contentAlignment = Alignment.CenterStart) {
                Text("暂无子任务", modifier = Modifier.padding(horizontal = 12.dp), color = Color(0xFF91A1B5), fontSize = 13.sp)
            }
        } else subtasks.forEachIndexed { index, subtask ->
            Row(modifier = Modifier.fillMaxWidth().height(46.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier.size(22.dp).clip(RoundedCornerShape(6.dp)).background(if (subtask.isCompleted) Color(0xFF12A85C) else Color.White).border(1.dp, if (subtask.isCompleted) Color(0xFF12A85C) else Color(0xFFDEE5F0), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (subtask.isCompleted) Image(painterResource(R.drawable.icon_detail_check), null, Modifier.size(14.dp))
                }
                Text(subtask.title, modifier = Modifier.weight(1f), color = if (subtask.isCompleted) Color(0xFF91A1B5) else Color(0xFF0E131D), fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium, textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else null)
            }
            if (index < subtasks.lastIndex) HorizontalDivider(thickness = 1.dp, color = Color(0xFFE8EDF5))
        }
    }
}

private fun formatUpdatedTime(dateTime: LocalDateTime): String = if (dateTime.toLocalDate() == LocalDate.now()) {
    "今天 ${dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))}"
} else dateTime.format(DateTimeFormatter.ofPattern("MM月dd日 HH:mm"))

private fun compactImportance(value: TaskImportanceUrgency): String = when (value) {
    TaskImportanceUrgency.IMPORTANT_URGENT -> "重要紧急"
    TaskImportanceUrgency.IMPORTANT_NOT_URGENT -> "重要不紧急"
    TaskImportanceUrgency.NOT_IMPORTANT_URGENT -> "紧急不重要"
    TaskImportanceUrgency.NOT_IMPORTANT_NOT_URGENT -> "普通"
}

private fun repeatSummary(repeat: RepeatFrequency): String = when (repeat.type) {
    com.nextthing.app.domain.model.RepeatFrequencyType.NONE -> "单次任务"
    com.nextthing.app.domain.model.RepeatFrequencyType.DAILY -> "每天"
    com.nextthing.app.domain.model.RepeatFrequencyType.WEEKLY -> if (repeat.weekdays.isEmpty()) "每周" else "每周" + repeat.weekdays.sorted().joinToString("、") { listOf("", "一", "二", "三", "四", "五", "六", "日").getOrElse(it) { it.toString() } }
    com.nextthing.app.domain.model.RepeatFrequencyType.MONTHLY -> "每月"
    else -> "自定义"
}

@Composable
private fun TaskDetailTopNavigation(
    isEditMode: Boolean,
    onBackPressed: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Color.White)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF5F7FC))
                .clickable { onBackPressed() },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.icon_detail_back),
                contentDescription = "返回",
                modifier = Modifier.size(24.dp, 26.dp)
            )
        }

        Text(
            text = if (isEditMode) "编辑任务" else "任务详情",
            modifier = Modifier.weight(1f),
            color = Color(0xFF0E131D),
            fontSize = 18.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold
        )

        // 右侧操作按钮
        if (!isEditMode) {
            Row {
                TextButton(onClick = onEditClick) {
                    Text(
                        text = "编辑",
                        color = Color(0xFF2196F3),
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDeleteClick) {
                    Text(
                        text = "删除",
                        color = Color(0xFFEF5350),
                        fontSize = 16.sp
                    )
                }
            }
        } else {
            Text(
                text = "未保存",
                color = Color(0xFFF2940F),
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


// 新的卡片组件
@Composable
private fun TaskTitleDescriptionCard(
    task: Task,
    isEditMode: Boolean,
    editedTitle: String,
    editedDescription: String,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 标题
            if (isEditMode) {
                BasicTextField(
                    value = editedTitle,
                    onValueChange = onTitleChange,
                    textStyle = TextStyle(
                        color = Color(0xFF424242),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = Color(0xFFF8F9FA),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFF1A7DFA),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                        ) {
                            if (editedTitle.isEmpty()) {
                                Text(
                                    text = "请输入任务标题...",
                                    color = Color(0xFF9E9E9E),
                                    fontSize = 18.sp
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            } else {
                Text(
                    text = task.title,
                    color = Color(0xFF424242),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // 描述
            if (isEditMode) {
                BasicTextField(
                    value = editedDescription,
                    onValueChange = onDescriptionChange,
                    textStyle = TextStyle(
                        color = Color(0xFF666666),
                        fontSize = 14.sp
                    ),
                    modifier = Modifier.fillMaxWidth().height(94.dp),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = Color(0xFFF8F9FA),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFFE0E0E0),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                        ) {
                            if (editedDescription.isEmpty()) {
                                Text(
                                    text = "请输入任务描述...",
                                    color = Color(0xFF9E9E9E),
                                    fontSize = 14.sp
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            } else if (task.description.isNotEmpty()) {
                Text(
                    text = task.description,
                    color = Color(0xFF666666),
                    fontSize = 14.sp
                )
            }
        }
    }
}


@Composable
private fun TaskImportanceCard(
    task: Task,
    isEditMode: Boolean,
    editedImportanceUrgency: TaskImportanceUrgency?,
    onImportanceUrgencyChange: (TaskImportanceUrgency?) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        // 主卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clickable { if (isEditMode) isExpanded = !isExpanded },
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = BgCard),
            border = BorderStroke(0.5.dp, Color(0xFFE0E0E0))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // 左上角标签
                Text(
                    text = "重要程度",
                    color = Color(0xFF9E9E9E),
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.TopStart)
                )

                // 主要内容行
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterStart)
                        .padding(top = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = getImportanceIcon(if (isEditMode) editedImportanceUrgency else task.importanceUrgency),
                        fontSize = 16.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    Text(
                        text = (if (isEditMode) editedImportanceUrgency else task.importanceUrgency)?.displayName ?: "未设置",
                        color = if ((if (isEditMode) editedImportanceUrgency else task.importanceUrgency) != null)
                               Color(0xFF424242) else Color(0xFF9E9E9E),
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )

                    if (isEditMode) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color(0xFF9E9E9E),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 展开的重要程度选项菜单
        if (isExpanded && isEditMode) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                colors = CardDefaults.cardColors(containerColor = BgCard),
                border = BorderStroke(0.5.dp, Color(0xFFE0E0E0)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    TaskImportanceUrgency.values().forEach { importanceUrgency ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onImportanceUrgencyChange(importanceUrgency)
                                    isExpanded = false
                                }
                                .padding(vertical = 8.dp, horizontal = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = getImportanceIcon(importanceUrgency),
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(end = 8.dp)
                                )

                                Text(
                                    text = importanceUrgency.displayName,
                                    color = Color(0xFF424242),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if (importanceUrgency.description.isNotBlank()) {
                                Text(
                                    text = importanceUrgency.description,
                                    color = Color(0xFF666666),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 2.dp, start = 24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@Suppress("UNUSED_PARAMETER")
private fun TaskDurationCard(
    task: Task,
    isEditMode: Boolean,
    editedEstimatedDuration: Int,
    editedActualDuration: Int,
    onEstimatedDurationChange: (Int) -> Unit,
    onActualDurationChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(0.5.dp, Color(0xFFE0E0E0))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "时长统计",
                color = Color(0xFF9E9E9E),
                fontSize = 10.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 预估时长
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "⏱️", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "预估: ${if (isEditMode) editedEstimatedDuration else task.estimatedDuration}分钟",
                            color = Color(0xFF424242),
                            fontSize = 12.sp
                        )
                    }
                }

                // 实际时长
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "📊", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "实际: ${if (isEditMode) editedActualDuration else task.actualDuration}分钟",
                            color = Color(0xFF424242),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
@Suppress("UNUSED_PARAMETER")
private fun TaskTagsCard(
    task: Task,
    isEditMode: Boolean,
    editedTags: List<String>,
    onTagsChange: (List<String>) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(0.5.dp, Color(0xFFE0E0E0))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "标签",
                color = Color(0xFF9E9E9E),
                fontSize = 10.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🏷️", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))

                val tags = if (isEditMode) editedTags else task.tags
                if (tags.isNotEmpty()) {
                    Text(
                        text = tags.joinToString(", "),
                        color = Color(0xFF424242),
                        fontSize = 14.sp
                    )
                } else {
                    Text(
                        text = "无标签",
                        color = Color(0xFF9E9E9E),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
@Suppress("UNUSED_PARAMETER")
private fun TaskSubtasksCard(
    task: Task,
    isEditMode: Boolean,
    editedSubtasks: List<Subtask>,
    onSubtasksChange: (List<Subtask>) -> Unit,
    onSubtaskStatusToggle: (String) -> Unit,
    isGeneratingSubtasks: Boolean = false,
    onGenerateAISubtasks: () -> Unit = {}
) {
    val primaryColor = com.nextthing.app.presentation.theme.Primary
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(0.5.dp, Color(0xFFE0E0E0))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "子任务",
                    color = Color(0xFF9E9E9E),
                    fontSize = 10.sp
                )

                val subtasks = if (isEditMode) editedSubtasks else task.subtasks
                Text(
                    text = "${subtasks.count { it.isCompleted }}/${subtasks.size}",
                    color = Color(0xFF9E9E9E),
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val subtasks = if (isEditMode) editedSubtasks else task.subtasks
            if (subtasks.isNotEmpty()) {
                subtasks.forEach { subtask ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = if (subtask.isCompleted) "✅" else "⭕",
                            fontSize = 16.sp,
                            modifier = if (!isEditMode)
                                      Modifier.clickable { onSubtaskStatusToggle(subtask.id) }
                                      else Modifier
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = subtask.title,
                            color = if (subtask.isCompleted) Color(0xFF9E9E9E) else Color(0xFF424242),
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // AI 生成子任务按钮
            if (!isEditMode) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isGeneratingSubtasks) { onGenerateAISubtasks() }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isGeneratingSubtasks) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 1.5.dp,
                            color = primaryColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("生成中...", fontSize = 12.sp, color = primaryColor)
                    } else {
                        Text("✨", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "AI 生成子任务",
                            fontSize = 12.sp,
                            color = primaryColor
                        )
                    }
                }
            }
        }
    }
}

// 重复频次选择组件
@Composable
private fun RepeatOptionItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp) // 进一步降低高度
            .height(18.dp), // 固定高度
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            modifier = Modifier.size(20.dp), // 缩小RadioButton
            colors = RadioButtonDefaults.colors(
                selectedColor = Color(0xFF2196F3),
                unselectedColor = Color(0xFF9E9E9E)
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = text,
            color = Color(0xFF424242),
            fontSize = 14.sp
        )
    }
}

@Composable
private fun WeekdaySelector(
    selectedWeekdays: Set<Int>,
    onWeekdayToggle: (Int) -> Unit
) {
    val weekdays = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        weekdays.forEachIndexed { index, dayName ->
            val dayNumber = index + 1
            val isSelected = selectedWeekdays.contains(dayNumber)

            Button(
                onClick = { onWeekdayToggle(dayNumber) },
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) Color(0xFF2196F3) else Color(0xFFE0E0E0),
                    contentColor = if (isSelected) Color.White else Color(0xFF424242)
                ),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                Text(
                    text = dayName,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun MonthDaySelector(
    selectedDays: Set<Int>,
    onDayToggle: (Int) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(31) { index ->
            val day = index + 1
            val isSelected = selectedDays.contains(day)

            Button(
                onClick = { onDayToggle(day) },
                modifier = Modifier
                    .aspectRatio(1f)
                    .size(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) Color(0xFF2196F3) else Color.Transparent,
                    contentColor = if (isSelected) Color.White else Color(0xFF424242)
                ),
                border = if (!isSelected) BorderStroke(0.5.dp, Color(0xFFE0E0E0)) else null,
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = day.toString(),
                    fontSize = 9.sp
                )
            }
        }
    }
}

// 辅助函数
private fun getImportanceIcon(importanceUrgency: TaskImportanceUrgency?): String {
    return when (importanceUrgency) {
        TaskImportanceUrgency.IMPORTANT_URGENT -> "🔥"
        TaskImportanceUrgency.IMPORTANT_NOT_URGENT -> "⭐"
        TaskImportanceUrgency.NOT_IMPORTANT_URGENT -> "⚡"
        TaskImportanceUrgency.NOT_IMPORTANT_NOT_URGENT -> "🔵"
        null -> "⚠️"
    }
}

@Composable
private fun TaskStatusCard(
    currentStatus: TaskStatus,
    onStatusChange: (TaskStatus) -> Unit,
    isEditMode: Boolean = false
) {
    // 根据当前状态提供可切换的目标状态
    val availableStatuses = when (currentStatus) {
        TaskStatus.PENDING -> listOf(TaskStatus.COMPLETED, TaskStatus.CANCELLED)
        TaskStatus.COMPLETED -> listOf(TaskStatus.PENDING, TaskStatus.CANCELLED)
        TaskStatus.DELAYED -> listOf(TaskStatus.COMPLETED, TaskStatus.CANCELLED)
        TaskStatus.OVERDUE -> listOf(TaskStatus.COMPLETED, TaskStatus.PENDING)
        TaskStatus.CANCELLED -> listOf(TaskStatus.PENDING, TaskStatus.COMPLETED)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：当前状态
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = getStatusIcon(currentStatus),
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "当前状态",
                        color = Color(0xFF9E9E9E),
                        fontSize = 11.sp
                    )
                    Text(
                        text = getStatusDisplayName(currentStatus),
                        color = Color(android.graphics.Color.parseColor(getStatusColorHex(currentStatus))),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 右侧：可选状态按钮（仅编辑模式显示）
            if (isEditMode) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableStatuses.forEach { status ->
                        val statusColor = Color(android.graphics.Color.parseColor(getStatusColorHex(status)))
                        OutlinedButton(
                            onClick = { onStatusChange(status) },
                            border = BorderStroke(1.dp, statusColor),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = statusColor)
                        ) {
                            Text(
                                text = "${getStatusIcon(status)} ${getStatusDisplayName(status)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getStatusIcon(status: TaskStatus): String {
    return when (status) {
        TaskStatus.PENDING -> "⏳"
        TaskStatus.COMPLETED -> "✅"
        TaskStatus.DELAYED -> "⏰"
        TaskStatus.OVERDUE -> "🔴"
        TaskStatus.CANCELLED -> "❌"
    }
}

private fun getStatusDisplayName(status: TaskStatus): String {
    return when (status) {
        TaskStatus.PENDING -> "待办"
        TaskStatus.COMPLETED -> "已完成"
        TaskStatus.DELAYED -> "已延期"
        TaskStatus.OVERDUE -> "已逾期"
        TaskStatus.CANCELLED -> "已取消"
    }
}

private fun getStatusColorHex(status: TaskStatus): String {
    return when (status) {
        TaskStatus.PENDING -> "#2196F3"
        TaskStatus.COMPLETED -> "#4CAF50"
        TaskStatus.DELAYED -> "#FF9800"
        TaskStatus.OVERDUE -> "#F44336"
        TaskStatus.CANCELLED -> "#9E9E9E"
    }
}

@Composable
private fun EditModeBottomButtons(
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(width = 1.dp, color = Color(0xFFE8EDF5))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 取消按钮
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFFDEE5F0)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0E131D))
        ) {
            Text(
                text = "取消",
                fontSize = 16.sp
            )
        }

        // 保存按钮
        Button(
            onClick = onSave,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A7DFA))
        ) {
            Text(
                text = "保存修改",
                color = Color.White,
                fontSize = 16.sp
            )
        }
    }
}


// 辅助函数
private fun formatDueDate(dueDate: LocalDateTime?): String {
    return if (dueDate != null) {
        val today = LocalDate.now()
        val dueDateLocal = dueDate.toLocalDate()
        when {
            dueDateLocal == today -> "今天 (${dueDateLocal.dayOfMonth}号)"
            dueDateLocal == today.plusDays(1) -> "明天 (${dueDateLocal.dayOfMonth}号)"
            else -> dueDate.format(DateTimeFormatter.ofPattern("MM月dd日"))
        }
    } else {
        "无截止"
    }
}

private fun formatLocation(location: LocationInfo?): String {
    return location?.locationName?.takeIf { it.isNotEmpty() } ?: "实时位置"
}

/**
 * 任务地理围栏状态卡片（只读显示）
 */
@Composable
private fun TaskGeofenceStatusCard(
    taskGeofence: TaskGeofence?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(0.5.dp, Color(0xFFE0E0E0)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // 左上角标签
            Text(
                text = "地理围栏",
                color = Color(0xFF9E9E9E),
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.TopStart)
            )

            // 主要内容行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterStart)
                    .padding(top = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🛡️",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )

                if (taskGeofence != null && taskGeofence.isEnabled) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = taskGeofence.geofenceLocation.locationInfo.locationName.ifEmpty { "未命名地点" },
                            color = Color(0xFF424242),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "半径: ${taskGeofence.snapshotRadius}米",
                            color = Color(0xFF9E9E9E),
                            fontSize = 10.sp
                        )
                    }
                    if (taskGeofence.geofenceLocation.isFrequent) {
                        Text(text = "⭐", fontSize = 14.sp)
                    }
                } else {
                    Text(
                        text = "未启用",
                        color = Color(0xFF9E9E9E),
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// ── AI 子任务建议弹窗 ─────────────────────────────────────────────

@Composable
private fun SubtaskSuggestionDialog(
    suggestions: List<String>,
    onAccept: (Set<Int>) -> Unit,
    onDismiss: () -> Unit
) {
    val primaryColor = com.nextthing.app.presentation.theme.Primary
    var selectedIndexes by remember { mutableStateOf(suggestions.indices.toSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "✨ AI 子任务建议",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column {
                Text(
                    text = "AI 为你生成了 ${suggestions.size} 个子任务步骤，勾选要添加的：",
                    fontSize = 13.sp,
                    color = com.nextthing.app.presentation.theme.TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                suggestions.forEachIndexed { index, title ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedIndexes = if (index in selectedIndexes)
                                    selectedIndexes - index else selectedIndexes + index
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = index in selectedIndexes,
                            onCheckedChange = {
                                selectedIndexes = if (index in selectedIndexes)
                                    selectedIndexes - index else selectedIndexes + index
                            },
                            colors = CheckboxDefaults.colors(checkedColor = primaryColor),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = title,
                            fontSize = 14.sp,
                            color = com.nextthing.app.presentation.theme.TextPrimary
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAccept(selectedIndexes) },
                enabled = selectedIndexes.isNotEmpty()
            ) {
                Text(
                    if (selectedIndexes.isNotEmpty()) "添加 (${selectedIndexes.size})"
                    else "请选择",
                    color = if (selectedIndexes.isNotEmpty()) primaryColor
                           else primaryColor.copy(alpha = 0.3f)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = com.nextthing.app.presentation.theme.TextSecondary)
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

// ── AI 日程建议弹窗 ─────────────────────────────────────────────

@Composable
private fun ScheduleAdviceDialog(
    advice: com.nextthing.app.domain.service.ScheduleAdvice,
    onDismiss: () -> Unit
) {
    val primaryColor = com.nextthing.app.presentation.theme.Primary
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "🧠 AI 日程建议",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 总结
                Surface(
                    color = primaryColor.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = advice.summary,
                        fontSize = 14.sp,
                        color = com.nextthing.app.presentation.theme.TextPrimary,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                // 具体建议
                if (advice.suggestions.isNotEmpty()) {
                    Text("调整建议：", fontSize = 13.sp, color = com.nextthing.app.presentation.theme.TextSecondary)
                    advice.suggestions.forEachIndexed { index, suggestion ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = "${index + 1}.",
                                fontSize = 14.sp,
                                color = primaryColor,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = suggestion,
                                fontSize = 14.sp,
                                color = com.nextthing.app.presentation.theme.TextPrimary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("知道了", color = primaryColor, fontWeight = FontWeight.Medium)
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

// ══════════════════════════════════════════
//  AI 时间预估弹窗
// ══════════════════════════════════════════

@Composable
private fun TimeEstimateDialog(
    estimate: com.nextthing.app.domain.service.TimeEstimate,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⏱", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI 时间预估", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 预估时间
                Surface(
                    color = Color(0xFF4CAF50).copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("预估耗时 ", fontSize = 15.sp, color = com.nextthing.app.presentation.theme.TextSecondary)
                        Text(
                            "${estimate.estimatedMinutes} 分钟",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }

                // 依据
                if (estimate.reasoning.isNotBlank()) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text("💡", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            estimate.reasoning,
                            fontSize = 14.sp,
                            color = com.nextthing.app.presentation.theme.TextSecondary
                        )
                    }
                }

                // 参考数据量
                if (estimate.similarTaskCount > 0) {
                    Text(
                        "基于 ${estimate.similarTaskCount} 个同类历史任务",
                        fontSize = 12.sp,
                        color = com.nextthing.app.presentation.theme.TextMuted
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("知道了", color = Color(0xFF4CAF50), fontWeight = FontWeight.Medium)
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

// ══════════════════════════════════════════
//  AI 拖延预警弹窗
// ══════════════════════════════════════════

@Composable
private fun ProcrastinationDialog(
    advice: com.nextthing.app.domain.service.ProcrastinationAdvice,
    onDismiss: () -> Unit
) {
    val severityColor = when (advice.severity) {
        "high" -> Color(0xFFF44336)
        "medium" -> Color(0xFFFF9800)
        else -> Color(0xFF4CAF50)
    }
    val severityText = when (advice.severity) {
        "high" -> "高"
        "medium" -> "中"
        else -> "低"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⚠️", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI 拖延预警", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 严重程度
                Surface(
                    color = severityColor.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("拖延程度", fontSize = 15.sp, color = com.nextthing.app.presentation.theme.TextSecondary)
                        Surface(
                            color = severityColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                " $severityText ",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = severityColor,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // 分析结论
                Text(advice.summary, fontSize = 14.sp, color = com.nextthing.app.presentation.theme.TextPrimary)

                // 建议
                if (advice.suggestions.isNotEmpty()) {
                    Text("建议：", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = com.nextthing.app.presentation.theme.TextSecondary)
                    advice.suggestions.forEachIndexed { i, suggestion ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text("${i + 1}.", fontSize = 14.sp, color = severityColor, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(suggestion, fontSize = 14.sp, color = com.nextthing.app.presentation.theme.TextPrimary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("知道了", color = Color(0xFFFF9800), fontWeight = FontWeight.Medium)
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
