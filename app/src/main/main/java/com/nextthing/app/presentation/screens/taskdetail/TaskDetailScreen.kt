package com.nextthing.app.presentation.screens.taskdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.nextthing.app.presentation.theme.*
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
    viewModel: TaskDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val availableNotificationStrategies by viewModel.availableNotificationStrategies.collectAsState()
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
                Column(
                    modifier = Modifier.fillMaxSize()
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                                modifier = Modifier.weight(1f),
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
                                modifier = Modifier.weight(1f),
                                isEditMode = uiState.isEditMode
                            )
                        }

                        // 第二行：分类选择 + 重要程度
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                                modifier = Modifier.weight(1f),
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
                                modifier = Modifier.weight(1f),
                                isEditMode = uiState.isEditMode
                            )
                        }

                        // 第三行：任务图片 + 重复频次
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                                modifier = Modifier.weight(1f),
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
                                modifier = Modifier.weight(1f),
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
private fun TaskDetailTopNavigation(
    isEditMode: Boolean,
    onBackPressed: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(BgPrimary)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧返回按钮
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(0xFF71CBF4))
                .clickable { onBackPressed() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        // 中间标题
        Text(
            text = "任务详情",
            color = Color(0xFF424242),
            fontSize = 18.sp,
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
            Spacer(modifier = Modifier.width(80.dp))
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
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
                    modifier = Modifier.fillMaxWidth(),
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
                                    color = Color(0xFF71CBF4),
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
                    modifier = Modifier.fillMaxWidth(),
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
                                .height(80.dp)
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
            .background(BgPrimary)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 取消按钮
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF424242))
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
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF71CBF4))
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
