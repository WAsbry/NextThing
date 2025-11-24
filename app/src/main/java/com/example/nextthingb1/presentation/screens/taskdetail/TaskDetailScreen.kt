package com.example.nextthingb1.presentation.screens.taskdetail

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
import androidx.compose.material.icons.filled.ArrowBack
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
import com.example.nextthingb1.domain.model.Task
import com.example.nextthingb1.domain.model.TaskCategory
import com.example.nextthingb1.domain.model.TaskImportanceUrgency
import com.example.nextthingb1.domain.model.RepeatFrequency
import com.example.nextthingb1.domain.model.Subtask
import com.example.nextthingb1.domain.model.LocationInfo
import com.example.nextthingb1.presentation.theme.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: String,
    onBackPressed: () -> Unit,
    onEditTask: () -> Unit = {},
    viewModel: TaskDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val savedLocations by viewModel.savedLocations.collectAsState()
    val availableNotificationStrategies by viewModel.availableNotificationStrategies.collectAsState()
    val showCreateCategoryDialog by viewModel.showCreateCategoryDialog.collectAsState()
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    val context = androidx.compose.ui.platform.LocalContext.current

    // 卡片展开状态
    var isTimeExpanded by remember { mutableStateOf(false) }
    var isPreciseTimeExpanded by remember { mutableStateOf(false) }
    var isCategoryExpanded by remember { mutableStateOf(false) }
    var isLocationExpanded by remember { mutableStateOf(false) }
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
            com.example.nextthingb1.util.ToastHelper.showToast(context, message)
            viewModel.clearMessages()
        }
    }

    // 显示错误消息
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            com.example.nextthingb1.util.ToastHelper.showToast(context, message)
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

                        // 第一行：时间 + 精确时间
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            com.example.nextthingb1.presentation.screens.create.TimeConfigCard(
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
                            com.example.nextthingb1.presentation.screens.create.PreciseTimeConfigCard(
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

                        // 第二行：分类 + 位置
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            com.example.nextthingb1.presentation.screens.create.CategoryPriorityConfigCard(
                                screenHeight = screenHeight,
                                screenWidth = screenWidth,
                                isExpanded = isCategoryExpanded,
                                onExpandToggle = { isCategoryExpanded = !isCategoryExpanded },
                                selectedCategoryItem = if (uiState.isEditMode) uiState.editedCategoryItem else null,
                                categories = categories,
                                onCategorySelected = { categoryItem ->
                                    viewModel.updateSelectedCategory(categoryItem)
                                },
                                onCreateCategoryClicked = {
                                    viewModel.showCreateCategoryDialog()
                                },
                                onDeleteCategory = { categoryId ->
                                    viewModel.deleteCategory(categoryId)
                                },
                                onPinCategory = { categoryId, isPinned ->
                                    viewModel.pinCategory(categoryId, isPinned)
                                },
                                modifier = Modifier.weight(1f),
                                isEditMode = uiState.isEditMode
                            )

                            com.example.nextthingb1.presentation.screens.create.LocationConfigCard(
                                screenHeight = screenHeight,
                                screenWidth = screenWidth,
                                isExpanded = isLocationExpanded,
                                onExpandToggle = { isLocationExpanded = !isLocationExpanded },
                                savedLocations = savedLocations,
                                selectedLocation = if (uiState.isEditMode) uiState.editedLocation else uiState.task!!.locationInfo,
                                onLocationSelected = { location ->
                                    viewModel.updateEditedLocation(location)
                                },
                                onNavigateToCreateLocation = { /* TODO: 导航到创建位置页面 */ },
                                onDeleteLocation = { locationId ->
                                    viewModel.deleteLocation(locationId)
                                },
                                modifier = Modifier.weight(1f),
                                isEditMode = uiState.isEditMode
                            )
                        }

                        // 第三行：重要性 + 图片
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            com.example.nextthingb1.presentation.screens.create.ImportanceConfigCard(
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

                            com.example.nextthingb1.presentation.screens.create.ImageConfigCard(
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
                        }

                        // 第四行：重复频次 + 通知策略
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            com.example.nextthingb1.presentation.screens.create.RepeatFrequencyConfigCard(
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
                                modifier = Modifier.weight(1f),
                                isEditMode = uiState.isEditMode
                            )

                            com.example.nextthingb1.presentation.screens.create.NotificationStrategyConfigCard(
                                screenHeight = screenHeight,
                                screenWidth = screenWidth,
                                isExpanded = isNotificationExpanded,
                                onExpandToggle = { isNotificationExpanded = !isNotificationExpanded },
                                availableStrategies = availableNotificationStrategies,
                                selectedStrategyId = if (uiState.isEditMode) uiState.editedNotificationStrategyId else uiState.task?.notificationStrategyId,
                                onStrategySelected = { strategyId ->
                                    viewModel.updateNotificationStrategy(strategyId)
                                },
                                onNavigateToCreateNotificationStrategy = { /* TODO: 导航到创建通知策略页面 */ },
                                modifier = Modifier.weight(1f),
                                isEditMode = uiState.isEditMode
                            )
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

        // 删除确认弹窗
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

        // 日期选择器对话框
        if (showDatePicker) {
            com.example.nextthingb1.presentation.screens.create.MaterialDatePickerDialog(
                onDateSelected = { date ->
                    viewModel.updateEditedDueDate(date.atStartOfDay())
                    showDatePicker = false
                },
                onDismiss = { showDatePicker = false }
            )
        }

        // 创建分类对话框
        if (showCreateCategoryDialog) {
            CreateCategoryDialog(
                onConfirm = { categoryName ->
                    viewModel.createCategory(categoryName)
                },
                onDismiss = { viewModel.hideCreateCategoryDialog() }
            )
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
            .background(Color.White)
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
                imageVector = Icons.Default.ArrowBack,
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
            colors = CardDefaults.cardColors(containerColor = Color.White),
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
                colors = CardDefaults.cardColors(containerColor = Color.White),
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
private fun TaskTagsCard(
    task: Task,
    isEditMode: Boolean,
    editedTags: List<String>,
    onTagsChange: (List<String>) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
private fun TaskSubtasksCard(
    task: Task,
    isEditMode: Boolean,
    editedSubtasks: List<Subtask>,
    onSubtasksChange: (List<Subtask>) -> Unit,
    onSubtaskStatusToggle: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "📝", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "无子任务",
                        color = Color(0xFF9E9E9E),
                        fontSize = 14.sp
                    )
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
private fun EditModeBottomButtons(
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
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

@Composable
private fun CreateCategoryDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var categoryName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "新建分类",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                Text(
                    text = "请输入分类名称",
                    color = Color(0xFF9E9E9E),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = { categoryName = it },
                    placeholder = { Text("分类名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (categoryName.isNotBlank()) {
                        onConfirm(categoryName.trim())
                    }
                },
                enabled = categoryName.isNotBlank()
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}