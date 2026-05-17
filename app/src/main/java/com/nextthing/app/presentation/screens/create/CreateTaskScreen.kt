package com.nextthing.app.presentation.screens.create

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.animateScrollBy
import kotlinx.coroutines.launch
import java.time.LocalTime
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.nextthing.app.util.ToastHelper
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.nextthing.app.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import com.nextthing.app.domain.model.TaskCategory
import com.nextthing.app.domain.model.CategoryItem
import com.nextthing.app.domain.model.LocationInfo
import com.nextthing.app.domain.model.TaskImportanceUrgency
import com.nextthing.app.domain.model.NotificationStrategy
import com.nextthing.app.presentation.theme.*
import com.nextthing.app.domain.model.AITaskParseResult
import com.nextthing.app.domain.model.RepeatFrequencyType
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction

// 日期格式化辅助函数
private fun formatDate(date: LocalDate?): String {
    val today = LocalDate.now()
    val targetDate = date ?: today
    val tomorrow = today.plusDays(1)

    // 获取格式化后的日期部分
    val dateStr = when {
        targetDate.year == today.year && targetDate.monthValue == today.monthValue -> "${targetDate.dayOfMonth}号"
        targetDate.year == today.year -> "${targetDate.monthValue}月${targetDate.dayOfMonth}日"
        else -> "${targetDate.year}年${targetDate.monthValue}月${targetDate.dayOfMonth}日"
    }

    return when {
        targetDate == today -> "今天($dateStr)"
        targetDate == tomorrow -> "明天($dateStr)"
        else -> dateStr
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskScreen(
    onBackPressed: () -> Unit,
    onNavigateToCreateNotificationStrategy: () -> Unit,
    onEditNotificationStrategy: (String) -> Unit = {},
    onNavigateToGeofenceAdd: () -> Unit = {},
    onNavigateToGeofenceSettings: () -> Unit = {},
    onNavigateToManageCategories: () -> Unit = {},
    onNavigateToRepeatCustom: () -> Unit = {},
    viewModel: CreateTaskViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val availableGeofenceLocations by viewModel.availableGeofenceLocations.collectAsState()
    val isASRRecording by viewModel.isASRRecording.collectAsState()
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    val context = LocalContext.current

    // 折叠状态管理 - 使用单一状态追踪当前展开的卡片
    var expandedCard by remember { mutableStateOf<String?>(null) }
    // 日期选择状态
    var showDatePicker by remember { mutableStateOf(false) }

    // 麦克风权限请求
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startASR()
        else viewModel.clearAIError().also {
            viewModel.updateAIInputText("")
            // 通过 aiError 显示提示
            // 直接设置错误文字
        }
    }

    fun onVoiceClick() {
        if (isASRRecording) {
            viewModel.stopASR()
        } else {
            val hasPerm = ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPerm) viewModel.startASR()
            else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
    ) {
        // 顶部导航区 (8%高度)
        TopNavigationSection(
            screenHeight = screenHeight,
            screenWidth = screenWidth,
            isListening = isASRRecording,
            onBackPressed = onBackPressed,
            onVoiceClick = { onVoiceClick() }
        )

        // AI 智能输入区域
        AIInputSection(
            aiInputText = uiState.aiInputText,
            isAIParsing = uiState.isAIParsing,
            aiParseResult = uiState.aiParseResult,
            showAIResult = uiState.showAIResult,
            aiError = uiState.aiError,
            onInputChange = { viewModel.updateAIInputText(it) },
            onParse = { viewModel.parseWithAI() },
            onApplyAndEdit = { viewModel.applyAIResult() },
            onApplyAndCreate = { viewModel.applyAIResultAndCreate(); onBackPressed() },
            onDismissResult = { viewModel.dismissAIResult() },
            onDismissError = { viewModel.clearAIError() }
        )

        // 核心输入区 (28%高度)
        CoreInputSection(
            screenHeight = screenHeight,
            screenWidth = screenWidth,
            title = uiState.title,
            onTitleChange = { viewModel.updateTitle(it) }
        )

        // 折叠配置区 (28%高度)
        CollapsibleConfigSection(
            screenHeight = screenHeight,
            screenWidth = screenWidth,
            isTimeExpanded = expandedCard == "time",
            isPreciseTimeExpanded = expandedCard == "precise_time",
            isCategoryExpanded = expandedCard == "category",
            isImageExpanded = expandedCard == "image",
            isImportanceExpanded = expandedCard == "importance",
            isReminderExpanded = expandedCard == "reminder",
            isRepeatExpanded = expandedCard == "repeat",
            onTimeExpandToggle = { expandedCard = if (expandedCard == "time") null else "time" },
            onPreciseTimeExpandToggle = { expandedCard = if (expandedCard == "precise_time") null else "precise_time" },
            onCategoryExpandToggle = { expandedCard = if (expandedCard == "category") null else "category" },
            onImageExpandToggle = { expandedCard = if (expandedCard == "image") null else "image" },
            onImportanceExpandToggle = { expandedCard = if (expandedCard == "importance") null else "importance" },
            onReminderExpandToggle = { expandedCard = if (expandedCard == "reminder") null else "reminder" },
            onRepeatExpandToggle = { expandedCard = if (expandedCard == "repeat") null else "repeat" },
            selectedCategoryItem = uiState.selectedCategoryItem,
            categories = categories,
            onCategorySelected = { viewModel.updateSelectedCategory(it) },
            onManageCategoriesClicked = onNavigateToManageCategories,
            selectedDate = uiState.selectedDate,
            onDateSelected = { viewModel.updateSelectedDate(it) },
            onShowDatePicker = { showDatePicker = true },
            preciseTime = uiState.preciseTime,
            onPreciseTimeSelected = { viewModel.updatePreciseTime(it) },
            selectedImageUri = uiState.selectedImageUri,
            onImageSelected = { viewModel.updateSelectedImage(it) },
            onImageCleared = { viewModel.clearSelectedImage() },
            selectedImportanceUrgency = uiState.importanceUrgency,
            onImportanceUrgencySelected = { viewModel.updateImportanceUrgency(it) },
            availableNotificationStrategies = uiState.availableNotificationStrategies,
            selectedNotificationStrategyId = uiState.notificationStrategyId,
            onNotificationStrategySelected = { viewModel.updateNotificationStrategy(it) },
            onNavigateToCreateNotificationStrategy = onNavigateToCreateNotificationStrategy,
            onEditNotificationStrategy = onEditNotificationStrategy,
            onDeleteNotificationStrategy = { viewModel.deleteNotificationStrategy(it) },
            repeatFrequency = uiState.repeatFrequency,
            onRepeatFrequencyTypeChange = { viewModel.updateRepeatFrequencyType(it) },
            onRepeatWeekdaysChange = { viewModel.updateRepeatWeekdays(it) },
            onRepeatMonthDaysChange = { viewModel.updateRepeatMonthDays(it) },
            onNavigateToRepeatCustom = onNavigateToRepeatCustom,
            geofenceEnabled = uiState.geofenceEnabled,
            onGeofenceEnabledChange = { viewModel.updateGeofenceEnabled(it) },
            availableGeofenceLocations = availableGeofenceLocations,
            selectedGeofenceLocationId = uiState.selectedGeofenceLocationId,
            onGeofenceLocationSelected = { viewModel.updateSelectedGeofenceLocation(it) },
            onNavigateToAddGeofenceLocation = onNavigateToGeofenceAdd,
            onNavigateToGeofenceSettings = onNavigateToGeofenceSettings,
            defaultGeofenceRadius = uiState.defaultRadius
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 底部操作区
        BottomActionSection(
            screenHeight = screenHeight,
            screenWidth = screenWidth,
            onSave = { viewModel.createTask(); onBackPressed() },
            onCancel = onBackPressed,
            isEnabled = uiState.title.isNotBlank()
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Material 3 日期选择器对话框
    if (showDatePicker) {
        MaterialDatePickerDialog(
            onDateSelected = { date ->
                viewModel.updateSelectedDate(date)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
private fun TopNavigationSection(
    screenHeight: androidx.compose.ui.unit.Dp,
    screenWidth: androidx.compose.ui.unit.Dp,
    isListening: Boolean,
    onBackPressed: () -> Unit,
    onVoiceClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Primary)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 返回箭头
            IconButton(
                onClick = onBackPressed,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // 页面标题
            Text(
                text = "创建任务",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            // 语音输入按钮
            IconButton(
                onClick = onVoiceClick,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                if (isListening) {
                    Text(
                        text = "⏸",
                        fontSize = 20.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.mic_on),
                        contentDescription = "语音输入",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
private fun CoreInputSection(
    screenHeight: androidx.compose.ui.unit.Dp,
    screenWidth: androidx.compose.ui.unit.Dp,
    title: String,
    onTitleChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = BgCard
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                BasicTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    textStyle = TextStyle(
                        color = TextPrimary,
                        fontSize = 16.sp,
                        lineHeight = 20.sp
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    decorationBox = { innerTextField ->
                        if (title.isEmpty()) {
                            Text(
                                text = "输入任务内容",
                                color = TextSecondary,
                                fontSize = 16.sp
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }
    }
}

@Composable
private fun CollapsibleConfigSection(
    screenHeight: androidx.compose.ui.unit.Dp,
    screenWidth: androidx.compose.ui.unit.Dp,
    isTimeExpanded: Boolean,
    isPreciseTimeExpanded: Boolean,
    isCategoryExpanded: Boolean,
    isImageExpanded: Boolean,
    isImportanceExpanded: Boolean,
    isReminderExpanded: Boolean,
    isRepeatExpanded: Boolean,
    onTimeExpandToggle: () -> Unit,
    onPreciseTimeExpandToggle: () -> Unit,
    onCategoryExpandToggle: () -> Unit,
    onImageExpandToggle: () -> Unit,
    onImportanceExpandToggle: () -> Unit,
    onReminderExpandToggle: () -> Unit,
    onRepeatExpandToggle: () -> Unit,
    selectedCategoryItem: CategoryItem?,
    categories: List<CategoryItem>,
    onCategorySelected: (CategoryItem) -> Unit,
    onManageCategoriesClicked: () -> Unit,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate?) -> Unit,
    onShowDatePicker: () -> Unit,
    preciseTime: Pair<Int, Int>?,
    onPreciseTimeSelected: (Pair<Int, Int>?) -> Unit,
    selectedImageUri: String?,
    onImageSelected: (String?) -> Unit,
    onImageCleared: () -> Unit,
    selectedImportanceUrgency: TaskImportanceUrgency?,
    onImportanceUrgencySelected: (TaskImportanceUrgency?) -> Unit,
    availableNotificationStrategies: List<NotificationStrategy>,
    selectedNotificationStrategyId: String?,
    onNotificationStrategySelected: (String?) -> Unit,
    onNavigateToCreateNotificationStrategy: () -> Unit,
    onEditNotificationStrategy: (String) -> Unit,
    onDeleteNotificationStrategy: (String) -> Unit,
    repeatFrequency: com.nextthing.app.domain.model.RepeatFrequency,
    onRepeatFrequencyTypeChange: (com.nextthing.app.domain.model.RepeatFrequencyType) -> Unit,
    onRepeatWeekdaysChange: (Set<Int>) -> Unit,
    onRepeatMonthDaysChange: (Set<Int>) -> Unit,
    onNavigateToRepeatCustom: () -> Unit = {},
    geofenceEnabled: Boolean = false,
    onGeofenceEnabledChange: (Boolean) -> Unit = {},
    availableGeofenceLocations: List<com.nextthing.app.domain.model.GeofenceLocation> = emptyList(),
    selectedGeofenceLocationId: String? = null,
    onGeofenceLocationSelected: (String?) -> Unit = {},
    onNavigateToAddGeofenceLocation: () -> Unit = {},
    onNavigateToGeofenceSettings: () -> Unit = {},
    defaultGeofenceRadius: Int = 200
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 第一行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 时间配置卡
            TimeConfigCard(
                screenHeight = screenHeight,
                screenWidth = screenWidth,
                isExpanded = isTimeExpanded,
                onExpandToggle = onTimeExpandToggle,
                selectedDate = selectedDate,
                onDateSelected = onDateSelected,
                onShowDatePicker = onShowDatePicker,
                modifier = Modifier.weight(1f)
            )

            // 精确时间配置卡
            PreciseTimeConfigCard(
                screenHeight = screenHeight,
                screenWidth = screenWidth,
                isExpanded = isPreciseTimeExpanded,
                onExpandToggle = onPreciseTimeExpandToggle,
                preciseTime = preciseTime,
                onPreciseTimeSelected = onPreciseTimeSelected,
                modifier = Modifier.weight(1f)
            )
        }

        // 第二行：分类选择、重要程度
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 分类配置卡
            CategoryPriorityConfigCard(
                screenHeight = screenHeight,
                screenWidth = screenWidth,
                isExpanded = isCategoryExpanded,
                onExpandToggle = onCategoryExpandToggle,
                selectedCategoryItem = selectedCategoryItem,
                categories = categories,
                onCategorySelected = onCategorySelected,
                onManageCategoriesClicked = onManageCategoriesClicked,
                modifier = Modifier.weight(1f)
            )

            // 重要性配置卡
            ImportanceConfigCard(
                screenHeight = screenHeight,
                screenWidth = screenWidth,
                isExpanded = isImportanceExpanded,
                onExpandToggle = onImportanceExpandToggle,
                selectedImportanceUrgency = selectedImportanceUrgency,
                onImportanceUrgencySelected = onImportanceUrgencySelected,
                modifier = Modifier.weight(1f)
            )
        }

        // 第三行：任务图片、重复频次
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 图片配置卡
            ImageConfigCard(
                screenHeight = screenHeight,
                screenWidth = screenWidth,
                isExpanded = isImageExpanded,
                onExpandToggle = onImageExpandToggle,
                selectedImageUri = selectedImageUri,
                onImageSelected = onImageSelected,
                onImageCleared = onImageCleared,
                modifier = Modifier.weight(1f)
            )

            // 重复频次配置卡
            RepeatFrequencyConfigCard(
                screenHeight = screenHeight,
                screenWidth = screenWidth,
                isExpanded = isRepeatExpanded,
                onExpandToggle = onRepeatExpandToggle,
                repeatFrequency = repeatFrequency,
                onRepeatFrequencyTypeChange = onRepeatFrequencyTypeChange,
                onWeekdaysChange = onRepeatWeekdaysChange,
                onMonthDaysChange = onRepeatMonthDaysChange,
                onNavigateToRepeatCustom = onNavigateToRepeatCustom,
                modifier = Modifier.weight(1f)
            )
        }

        // 第四行：通知策略（独占一行）
        // 第四行：通知策略 + 地理围栏并排
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 通知策略配置卡
            NotificationStrategyConfigCard(
                screenHeight = screenHeight,
                screenWidth = screenWidth,
                isExpanded = isReminderExpanded,
                onExpandToggle = onReminderExpandToggle,
                availableStrategies = availableNotificationStrategies,
                selectedStrategyId = selectedNotificationStrategyId,
                onStrategySelected = onNotificationStrategySelected,
                onNavigateToCreateNotificationStrategy = onNavigateToCreateNotificationStrategy,
                onEditStrategy = onEditNotificationStrategy,
                onDeleteStrategy = onDeleteNotificationStrategy,
                modifier = Modifier.weight(1f)
            )

            // 地理围栏配置卡
            TaskGeofenceCard(
                geofenceEnabled = geofenceEnabled,
                onGeofenceEnabledChange = onGeofenceEnabledChange,
                availableLocations = availableGeofenceLocations,
                selectedLocationId = selectedGeofenceLocationId,
                onLocationSelected = onGeofenceLocationSelected,
                onNavigateToAddLocation = onNavigateToAddGeofenceLocation,
                onNavigateToGeofenceSettings = onNavigateToGeofenceSettings,
                isEditMode = true,
                defaultRadius = defaultGeofenceRadius,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// 时间配置卡
@Suppress("UNUSED_PARAMETER")
@Composable
internal fun TimeConfigCard(
    screenHeight: androidx.compose.ui.unit.Dp,
    screenWidth: androidx.compose.ui.unit.Dp,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate?) -> Unit,
    onShowDatePicker: () -> Unit,
    modifier: Modifier = Modifier,
    isEditMode: Boolean = true
) {
    Column(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clickable(enabled = isEditMode) { onExpandToggle() },
            colors = CardDefaults.cardColors(containerColor = BgCard),
            border = BorderStroke(0.5.dp, Border),
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // 左上角标签
                Text(
                    text = "完成时间",
                    color = TextSecondary,
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
                        text = "📅",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    Text(
                        text = formatDate(selectedDate),
                        color = TextPrimary,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )

                    if (isEditMode) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 展开的选项菜单
        if (isExpanded && isEditMode) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                colors = CardDefaults.cardColors(containerColor = BgCard),
                border = BorderStroke(0.5.dp, Border),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    // 今天选项
                    Text(
                        text = "今天",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDateSelected(LocalDate.now())
                                onExpandToggle()
                            }
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                        color = TextPrimary,
                        fontSize = 14.sp
                    )

                    // 明天选项
                    Text(
                        text = "明天",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDateSelected(LocalDate.now().plusDays(1))
                                onExpandToggle()
                            }
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                        color = TextPrimary,
                        fontSize = 14.sp
                    )

                    // 从日历中选择
                    Text(
                        text = "从日历中选择",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onShowDatePicker()
                                onExpandToggle()
                            }
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                        color = Primary,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

// 精确时间配置卡
@Suppress("UNUSED_PARAMETER")
@Composable
internal fun PreciseTimeConfigCard(
    screenHeight: androidx.compose.ui.unit.Dp,
    screenWidth: androidx.compose.ui.unit.Dp,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    preciseTime: Pair<Int, Int>?,
    onPreciseTimeSelected: (Pair<Int, Int>?) -> Unit,
    modifier: Modifier = Modifier,
    isEditMode: Boolean = true
) {
    // 内部临时状态，用于时间选择器
    val currentTime = remember { LocalTime.now() }
    var tempHour by remember { mutableStateOf(preciseTime?.first ?: currentTime.hour) }
    var tempMinute by remember { mutableStateOf(preciseTime?.second ?: currentTime.minute) }
    var wasCleared by remember { mutableStateOf(false) }

    // 当展开时，重置临时状态为当前值或已选值
    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            wasCleared = false // 重置清除标志
            if (preciseTime != null) {
                tempHour = preciseTime.first
                tempMinute = preciseTime.second
            } else {
                val now = LocalTime.now()
                tempHour = now.hour
                tempMinute = now.minute
            }
        }
        // 移除自动保存逻辑，只通过"确定"按钮保存
    }

    Column(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clickable(enabled = isEditMode) { onExpandToggle() },
            colors = CardDefaults.cardColors(containerColor = BgCard),
            border = BorderStroke(0.5.dp, Border),
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // 左上角标签
                Text(
                    text = "精确时间",
                    color = TextSecondary,
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
                        text = "⏰",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    Text(
                        text = if (preciseTime != null) {
                            String.format("%02d:%02d", preciseTime.first, preciseTime.second)
                        } else {
                            "未设置"
                        },
                        color = if (preciseTime != null) TextPrimary else TextSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )

                    if (isEditMode) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 展开的时间选择器
        if (isExpanded && isEditMode) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                colors = CardDefaults.cardColors(containerColor = BgCard),
                border = BorderStroke(0.5.dp, Border),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    // 时间选择器 - 降低高度，只显示3个数字
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp), // 3个数字 * 40dp = 120dp
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 小时选择器
                        TimePickerColumn(
                            items = (0..23).toList(),
                            selectedItem = tempHour,
                            onItemSelected = { tempHour = it },
                            modifier = Modifier.weight(1f),
                            formatItem = { String.format("%02d", it) }
                        )

                        // 冒号分隔
                        Box(
                            modifier = Modifier
                                .width(16.dp)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ":",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        // 分钟选择器
                        TimePickerColumn(
                            items = (0..59).toList(),
                            selectedItem = tempMinute,
                            onItemSelected = { tempMinute = it },
                            modifier = Modifier.weight(1f),
                            formatItem = { String.format("%02d", it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 操作按钮行 - 降低按钮高度
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 清除按钮（如果已设置时间）
                        if (preciseTime != null) {
                            OutlinedButton(
                                onClick = {
                                    wasCleared = true
                                    onPreciseTimeSelected(null)
                                    onExpandToggle()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp), // 降低按钮高度
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Danger
                                ),
                                border = BorderStroke(1.dp, Danger),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Text("清除", fontSize = 13.sp)
                            }
                        }

                        // 确定按钮
                        Button(
                            onClick = {
                                Timber.tag("NotificationTask").d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                                Timber.tag("NotificationTask").d("【UI】用户点击精确时间'确定'按钮")
                                Timber.tag("NotificationTask").d("  选择的时间: $tempHour:$tempMinute")
                                onPreciseTimeSelected(Pair(tempHour, tempMinute))
                                Timber.tag("NotificationTask").d("  已调用 onPreciseTimeSelected()")
                                Timber.tag("NotificationTask").d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                                onExpandToggle()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp), // 降低按钮高度
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Primary
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Text("确定", color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

// 时间选择器列组件 - iOS风格的滚动选择器
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TimePickerColumn(
    items: List<Int>,
    selectedItem: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    formatItem: (Int) -> String = { it.toString() }
) {
    val coroutineScope = rememberCoroutineScope()
    val itemHeight = 40.dp

    // 初始化滚动位置
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = selectedItem
    )

    // 标记是否正在进行自动吸附滚动
    var isSnapping by remember { mutableStateOf(false) }
    // 记录上一次滚动状态，用于检测从滚动到停止的转变
    var wasScrolling by remember { mutableStateOf(false) }
    // 记录上一次选中的项，避免重复更新
    var lastSelectedItem by remember { mutableStateOf(selectedItem) }

    // 实时更新选中项（滚动过程中）- 只用于视觉反馈，不触发吸附
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        if (!isSnapping && listState.isScrollInProgress) {
            val visibleItemsInfo = listState.layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) {
                Timber.tag("TimePickerScroll").w("⚠️ visibleItemsInfo为空")
                return@LaunchedEffect
            }

            val viewportStart = listState.layoutInfo.viewportStartOffset
            val viewportEnd = listState.layoutInfo.viewportEndOffset
            val viewportCenterY = viewportStart + (viewportEnd - viewportStart) / 2

            var closestItem = selectedItem
            var minDistance = Int.MAX_VALUE

            visibleItemsInfo.forEach { itemInfo ->
                // 计算每个项目的中心Y坐标（相对于视口）
                val itemCenterY = itemInfo.offset + itemInfo.size / 2
                // 计算项目中心与视口中心的距离
                val distance = kotlin.math.abs(itemCenterY - viewportCenterY)

                if (distance < minDistance) {
                    minDistance = distance
                    closestItem = itemInfo.index
                }
            }

            if (closestItem in items.indices && closestItem != lastSelectedItem) {
                Timber.tag("TimePickerScroll").d("📍 滚动中: 选中项从 $lastSelectedItem 更新到 $closestItem (值=${items[closestItem]})")
                lastSelectedItem = closestItem
                onItemSelected(items[closestItem])
            }
        }
    }

    // iOS风格的自动吸附：只在用户手动滚动停止后触发一次
    LaunchedEffect(listState.isScrollInProgress) {
        val isCurrentlyScrolling = listState.isScrollInProgress

        Timber.tag("TimePickerScroll").v("🔄 滚动状态: wasScrolling=$wasScrolling, isCurrentlyScrolling=$isCurrentlyScrolling, isSnapping=$isSnapping")

        // 只在从滚动状态切换到停止状态时执行吸附，且不是正在吸附中
        if (wasScrolling && !isCurrentlyScrolling && !isSnapping) {
            Timber.tag("TimePickerScroll").d("🎯 滚动停止，开始吸附逻辑")

            // 延迟一小段时间，确保惯性滚动完全停止
            kotlinx.coroutines.delay(50)

            val visibleItemsInfo = listState.layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) {
                Timber.tag("TimePickerScroll").w("⚠️ 吸附时visibleItemsInfo为空")
                wasScrolling = false
                return@LaunchedEffect
            }

            val viewportStart = listState.layoutInfo.viewportStartOffset
            val viewportEnd = listState.layoutInfo.viewportEndOffset
            val viewportCenterY = viewportStart + (viewportEnd - viewportStart) / 2

            var closestItem = selectedItem
            var minDistance = Int.MAX_VALUE

            visibleItemsInfo.forEach { itemInfo ->
                // 计算每个项目的中心Y坐标（相对于视口）
                val itemCenterY = itemInfo.offset + itemInfo.size / 2
                // 计算项目中心与视口中心的距离
                val distance = kotlin.math.abs(itemCenterY - viewportCenterY)

                if (distance < minDistance) {
                    minDistance = distance
                    closestItem = itemInfo.index
                }
            }

            if (closestItem in items.indices) {
                Timber.tag("TimePickerScroll").d("🎯 吸附目标: index=$closestItem, value=${items[closestItem]}, 当前选中=$selectedItem")

                if (closestItem != lastSelectedItem) {
                    Timber.tag("TimePickerScroll").d("✅ 更新选中项: $lastSelectedItem -> $closestItem")
                    lastSelectedItem = closestItem
                    onItemSelected(items[closestItem])
                }

                // 执行吸附滚动
                Timber.tag("TimePickerScroll").d("🔧 开始吸附动画到 index=$closestItem")
                isSnapping = true
                wasScrolling = false // 立即重置，防止吸附动画完成后再次触发
                coroutineScope.launch {
                    try {
                        listState.animateScrollToItem(
                            index = closestItem,
                            scrollOffset = 0
                        )
                        Timber.tag("TimePickerScroll").d("✅ 吸附动画完成")
                    } catch (e: Exception) {
                        Timber.tag("TimePickerScroll").e(e, "❌ 吸附动画失败")
                    } finally {
                        isSnapping = false
                        Timber.tag("TimePickerScroll").d("🏁 重置isSnapping标志")
                    }
                }
            } else {
                Timber.tag("TimePickerScroll").w("⚠️ closestItem=$closestItem 超出范围 [0, ${items.size})")
                wasScrolling = false
            }
        } else {
            // 更新滚动状态记录（只在非吸附状态下更新）
            if (!isSnapping) {
                wasScrolling = isCurrentlyScrolling
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(BgSecondary, RoundedCornerShape(8.dp))
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            userScrollEnabled = true,
            // 上下各留1个itemHeight的空间，这样正好显示3个数字
            contentPadding = PaddingValues(vertical = 40.dp)
        ) {
            items(items.size) { index ->
                val item = items[index]
                val isSelected = item == selectedItem

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = formatItem(item),
                        fontSize = if (isSelected) 18.sp else 15.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Primary else TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 中间选中区域的背景指示器
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .align(Alignment.Center)
                .background(
                    Primary.copy(alpha = 0.1f),
                    RoundedCornerShape(4.dp)
                )
        )
    }
}

// 分类配置卡
@Suppress("UNUSED_PARAMETER")
@Composable
internal fun CategoryPriorityConfigCard(
    screenHeight: androidx.compose.ui.unit.Dp,
    screenWidth: androidx.compose.ui.unit.Dp,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    selectedCategoryItem: CategoryItem?,
    categories: List<CategoryItem>,
    onCategorySelected: (CategoryItem) -> Unit,
    onManageCategoriesClicked: () -> Unit,
    modifier: Modifier = Modifier,
    isEditMode: Boolean = true
) {
    Column(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clickable(enabled = isEditMode) { onExpandToggle() },
            colors = CardDefaults.cardColors(containerColor = BgCard),
            border = BorderStroke(0.5.dp, Border),
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // 左上角标签
                Text(
                    text = "分类选择",
                    color = TextSecondary,
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
                    com.nextthing.app.presentation.components.CategoryIconView(
                        icon = selectedCategoryItem?.icon ?: "🏷️",
                        size = 18.dp,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    Text(
                        text = selectedCategoryItem?.displayName ?: "生活",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )

                    if (isEditMode) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 展开的分类选项菜单
        if (isExpanded && isEditMode) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                colors = CardDefaults.cardColors(containerColor = BgCard),
                border = BorderStroke(0.5.dp, Border),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    // 显示所有分类（简化行，仅图标+名称）
                    categories.forEach { category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCategorySelected(category); onExpandToggle() }
                                .padding(vertical = 6.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            com.nextthing.app.presentation.components.CategoryIconView(
                                icon = category.icon,
                                size = 18.dp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = category.displayName,
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // 管理分类入口
                    Text(
                        text = "管理分类 →",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onManageCategoriesClicked() }
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                        color = Primary,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

// 图片配置卡
@Suppress("UNUSED_PARAMETER")
@Composable
internal fun ImageConfigCard(
    screenHeight: androidx.compose.ui.unit.Dp,
    screenWidth: androidx.compose.ui.unit.Dp,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    selectedImageUri: String?,
    onImageSelected: (String?) -> Unit,
    onImageCleared: () -> Unit,
    modifier: Modifier = Modifier,
    isEditMode: Boolean = true
) {
    // 图片选择器launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        onImageSelected(uri?.toString())
    }

    val context = LocalContext.current

    // 创建临时文件来保存拍照结果
    val tempImageFile = remember {
        File(context.cacheDir, "temp_camera_image_${System.currentTimeMillis()}.jpg")
    }

    val tempImageUri = remember {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempImageFile
        )
    }

    // 相机拍照launcher
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            onImageSelected(tempImageUri.toString())
        }
    }

    // 相机权限launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            takePictureLauncher.launch(tempImageUri)
        }
    }

    Column(modifier = modifier) {
        // 主卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clickable(enabled = isEditMode) { onExpandToggle() },
            colors = CardDefaults.cardColors(containerColor = BgCard),
            border = BorderStroke(0.5.dp, Border),
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // 左上角标签
                Text(
                    text = "任务图片",
                    color = TextSecondary,
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
                        text = "🖼️",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    Text(
                        text = if (selectedImageUri != null) "已选择" else "未选择",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )

                    if (isEditMode) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 展开的内容区域
        if (isExpanded && isEditMode) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                colors = CardDefaults.cardColors(containerColor = BgCard),
                border = BorderStroke(0.5.dp, Border),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    // 如果有选中的图片，显示图片和删除按钮
                    if (selectedImageUri != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .padding(bottom = 8.dp)
                        ) {
                            // 显示选中的图片
                            Image(
                                painter = rememberAsyncImagePainter(selectedImageUri),
                                contentDescription = "选中的图片",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )

                            // 右上角删除按钮
                            IconButton(
                                onClick = onImageCleared,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .background(
                                        Color.Black.copy(alpha = 0.6f),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "删除图片",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else {
                        // 如果没有选中图片，显示选择选项
                        // 从相册选择选项
                        TextButton(
                            onClick = {
                                imagePickerLauncher.launch("image/*")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "📷",
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = "从相册选择",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Start
                                )
                            }
                        }

                        // 拍照选项
                        TextButton(
                            onClick = {
                                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "📸",
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = "拍照",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Start
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 重要性配置卡
@Suppress("UNUSED_PARAMETER")
@Composable
internal fun ImportanceConfigCard(
    screenHeight: androidx.compose.ui.unit.Dp,
    screenWidth: androidx.compose.ui.unit.Dp,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    selectedImportanceUrgency: TaskImportanceUrgency?,
    onImportanceUrgencySelected: (TaskImportanceUrgency?) -> Unit,
    modifier: Modifier = Modifier,
    isEditMode: Boolean = true
) {
    // 内部状态管理选中的重要性和紧急性组合
    var internalSelectedImportanceUrgency by remember(selectedImportanceUrgency) {
        mutableStateOf<TaskImportanceUrgency?>(selectedImportanceUrgency)
    }

    Column(modifier = modifier) {
        // 主卡片：显示当前选中的重要性和紧急性
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clickable(enabled = isEditMode) { onExpandToggle() },
            colors = CardDefaults.cardColors(containerColor = BgCard),
            border = BorderStroke(0.5.dp, Border),
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // 左上角标签
                Text(
                    text = "重要程度",
                    color = TextSecondary,
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
                        text = getImportanceIcon(internalSelectedImportanceUrgency),
                        fontSize = 16.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    Text(
                        text = internalSelectedImportanceUrgency?.displayName ?: "未选择",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )

                    if (isEditMode) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 展开的重要性和紧急性选项菜单
        if (isExpanded && isEditMode) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                colors = CardDefaults.cardColors(containerColor = BgCard),
                border = BorderStroke(0.5.dp, Border),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    // 显示四象限选项
                    TaskImportanceUrgency.values().forEach { importanceUrgency ->
                        ImportanceUrgencyMenuItem(
                            importanceUrgency = importanceUrgency,
                            onClick = {
                                internalSelectedImportanceUrgency = importanceUrgency
                                onImportanceUrgencySelected(importanceUrgency)
                                onExpandToggle()
                            }
                        )
                    }
                }
            }
        }
    }
}

// 获取重要性和紧急性对应的图标
private fun getImportanceIcon(importanceUrgency: TaskImportanceUrgency?): String {
    return when (importanceUrgency) {
        TaskImportanceUrgency.IMPORTANT_URGENT -> "🔥"
        TaskImportanceUrgency.IMPORTANT_NOT_URGENT -> "⭐"
        TaskImportanceUrgency.NOT_IMPORTANT_URGENT -> "⚡"
        TaskImportanceUrgency.NOT_IMPORTANT_NOT_URGENT -> "🔵"
        null -> "⚠️"
    }
}

// 重要性和紧急性选项组件
@Composable
private fun ImportanceUrgencyMenuItem(
    importanceUrgency: TaskImportanceUrgency,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
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
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        if (importanceUrgency.description.isNotBlank()) {
            Text(
                text = importanceUrgency.description,
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp, start = 24.dp)
            )
        }
    }
}

// 通知策略配置卡
@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotificationStrategyConfigCard(
    screenHeight: androidx.compose.ui.unit.Dp,
    screenWidth: androidx.compose.ui.unit.Dp,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    availableStrategies: List<NotificationStrategy>,
    selectedStrategyId: String?,
    onStrategySelected: (String?) -> Unit,
    onNavigateToCreateNotificationStrategy: () -> Unit,
    onEditStrategy: (String) -> Unit = {},
    onDeleteStrategy: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    isEditMode: Boolean = true
) {
    var showDialog by remember { mutableStateOf(false) }
    val selectedStrategy = availableStrategies.find { it.id == selectedStrategyId }

    // 简洁的主卡片
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable(enabled = isEditMode) { showDialog = true },
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(0.5.dp, Border),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // 左上角标签
            Text(
                text = "通知策略",
                color = TextSecondary,
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
                    text = "🔔",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )

                Text(
                    text = selectedStrategy?.name ?: "未设置",
                    color = if (selectedStrategy != null) TextPrimary else TextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )

                if (isEditMode) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    // 底部弹出对话框
    if (showDialog && isEditMode) {
        val sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = false
        )

        ModalBottomSheet(
            onDismissRequest = { showDialog = false },
            sheetState = sheetState,
            containerColor = BgCard,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            dragHandle = {
                Surface(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 32.dp, height = 4.dp)
                            .background(
                                color = Border,
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        ) {
            Box(modifier = Modifier.wrapContentHeight(Alignment.Top).fillMaxWidth().heightIn(max = 500.dp)) {
                NotificationStrategyBottomSheet(
                    availableStrategies = availableStrategies,
                    selectedStrategyId = selectedStrategyId,
                    onStrategySelected = onStrategySelected,
                    onNavigateToCreateNotificationStrategy = onNavigateToCreateNotificationStrategy,
                    onEditStrategy = onEditStrategy,
                    onDeleteStrategy = onDeleteStrategy,
                    onDismiss = { showDialog = false }
                )
            }
        }
    }
}

/**
 * 通知策略配置底部对话框
 */
@Composable
private fun NotificationStrategyBottomSheet(
    availableStrategies: List<NotificationStrategy>,
    selectedStrategyId: String?,
    onStrategySelected: (String?) -> Unit,
    onNavigateToCreateNotificationStrategy: () -> Unit,
    onEditStrategy: (String) -> Unit = {},
    onDeleteStrategy: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var strategyToDelete by remember { mutableStateOf<NotificationStrategy?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // 固定的标题栏（不滚动）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🔔 通知策略",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            TextButton(onClick = onDismiss) {
                Text("完成", color = Primary)
            }
        }

        HorizontalDivider(color = Border)

        // 可滚动的策略列表区域（weight 占据中间空间，策略多时可滚动）
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // 不使用选项
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onStrategySelected(null)
                            onDismiss()
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedStrategyId == null,
                        onClick = {
                            onStrategySelected(null)
                            onDismiss()
                        },
                        colors = RadioButtonDefaults.colors(selectedColor = Primary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "不使用",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selectedStrategyId == null) Primary else TextPrimary,
                        fontWeight = if (selectedStrategyId == null) FontWeight.Medium else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                }
            }

            item { HorizontalDivider(color = Border) }

            // 策略列表
            if (availableStrategies.isNotEmpty()) {
                itemsIndexed(availableStrategies) { index, strategy ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedStrategyId == strategy.id,
                            onClick = {
                                onStrategySelected(strategy.id)
                                onDismiss()
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = Primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strategy.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selectedStrategyId == strategy.id) Primary else TextPrimary,
                            fontWeight = if (selectedStrategyId == strategy.id) FontWeight.Medium else FontWeight.Normal,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onStrategySelected(strategy.id)
                                    onDismiss()
                                }
                        )

                        // 编辑按钮
                        IconButton(
                            onClick = {
                                onEditStrategy(strategy.id)
                                onDismiss()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "编辑策略",
                                tint = Primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // 删除按钮
                        IconButton(
                            onClick = { strategyToDelete = strategy },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "删除策略",
                                tint = Danger,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    // 策略之间添加分割线
                    if (index < availableStrategies.size - 1) {
                        HorizontalDivider(color = Border)
                    }
                }
            } else {
                // 无策略提示
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Warning.copy(alpha = 0.1f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "📣", fontSize = 32.sp)
                            Text(
                                text = "还没有通知策略",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "创建通知策略可以设置任务提醒时间",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // 固定在底部的新建策略按钮（不随列表滚动）
        HorizontalDivider(color = Border)

        OutlinedButton(
            onClick = {
                onNavigateToCreateNotificationStrategy()
                onDismiss()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Primary
            ),
            border = BorderStroke(1.dp, Primary)
        ) {
            Text("➕ 新建策略", fontSize = 14.sp)
        }
    }

    // 删除确认对话框
    strategyToDelete?.let { strategy ->
        AlertDialog(
            onDismissRequest = { strategyToDelete = null },
            title = {
                Text(
                    text = "确认删除",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "确定要删除通知策略「${strategy.name}」吗？",
                    fontSize = 14.sp,
                    color = TextPrimary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteStrategy(strategy.id)
                        strategyToDelete = null
                    }
                ) {
                    Text("删除", color = Danger, fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { strategyToDelete = null }
                ) {
                    Text("取消", color = TextSecondary)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// 底部操作区
@Suppress("UNUSED_PARAMETER")
@Composable
private fun BottomActionSection(
    screenHeight: androidx.compose.ui.unit.Dp,
    screenWidth: androidx.compose.ui.unit.Dp,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    isEnabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 取消按钮
        Text(
            text = "取消",
            color = TextPrimary,
            fontSize = 16.sp,
            modifier = Modifier.clickable { onCancel() }
        )

        // 保存任务按钮
        Button(
            onClick = onSave,
            enabled = isEnabled,
            modifier = Modifier
                .width(120.dp)
                .height(40.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isEnabled) Primary else Primary.copy(alpha = 0.5f),
                contentColor = if (isEnabled) Color.White else Color.White.copy(alpha = 0.8f)
            )
        ) {
            Text(
                text = "保存",
                fontSize = 16.sp
            )
        }
    }
}

// Material 3 日期选择器对话框
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MaterialDatePickerDialog(
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val instant = java.time.Instant.ofEpochMilli(millis)
                        val localDate = instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                        onDateSelected(localDate)
                    }
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            title = {
                Text(
                    text = "选择完成时间",
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp)
                )
            }
        )
    }
}

// 重复频次配置卡
@Suppress("UNUSED_PARAMETER")
@Composable
internal fun RepeatFrequencyConfigCard(
    screenHeight: androidx.compose.ui.unit.Dp,
    screenWidth: androidx.compose.ui.unit.Dp,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    repeatFrequency: com.nextthing.app.domain.model.RepeatFrequency,
    onRepeatFrequencyTypeChange: (com.nextthing.app.domain.model.RepeatFrequencyType) -> Unit,
    onWeekdaysChange: (Set<Int>) -> Unit,
    onMonthDaysChange: (Set<Int>) -> Unit,
    onNavigateToRepeatCustom: () -> Unit = {},
    modifier: Modifier = Modifier,
    isEditMode: Boolean = true
) {
    Column(modifier = modifier) {
        // 主卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clickable(enabled = isEditMode) { onExpandToggle() },
            colors = CardDefaults.cardColors(containerColor = BgCard),
            border = BorderStroke(0.5.dp, Border),
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // 左上角标签
                Text(
                    text = "重复频次",
                    color = TextSecondary,
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
                        text = "🔁",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    Text(
                        text = repeatFrequency.getDisplayText(),
                        color = if (repeatFrequency.type == com.nextthing.app.domain.model.RepeatFrequencyType.NONE)
                               TextSecondary else TextPrimary,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )

                    if (isEditMode) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 展开的选项面板
        if (isExpanded && isEditMode) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                colors = CardDefaults.cardColors(containerColor = BgCard),
                border = BorderStroke(0.5.dp, Border),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    // 单次任务
                    RepeatOptionItem(
                        text = "单次任务",
                        isSelected = repeatFrequency.type == com.nextthing.app.domain.model.RepeatFrequencyType.NONE,
                        onClick = {
                            onRepeatFrequencyTypeChange(com.nextthing.app.domain.model.RepeatFrequencyType.NONE)
                            onExpandToggle()
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), color = Border)

                    // 每日
                    RepeatOptionItem(
                        text = "每日",
                        isSelected = repeatFrequency.type == com.nextthing.app.domain.model.RepeatFrequencyType.DAILY,
                        onClick = {
                            onRepeatFrequencyTypeChange(com.nextthing.app.domain.model.RepeatFrequencyType.DAILY)
                            onExpandToggle()
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), color = Border)

                    // 工作日
                    RepeatOptionItem(
                        text = "工作日",
                        isSelected = repeatFrequency.type == com.nextthing.app.domain.model.RepeatFrequencyType.WEEKDAYS,
                        onClick = {
                            onRepeatFrequencyTypeChange(com.nextthing.app.domain.model.RepeatFrequencyType.WEEKDAYS)
                            onExpandToggle()
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), color = Border)

                    // 周末
                    RepeatOptionItem(
                        text = "周末",
                        isSelected = repeatFrequency.type == com.nextthing.app.domain.model.RepeatFrequencyType.WEEKENDS,
                        onClick = {
                            onRepeatFrequencyTypeChange(com.nextthing.app.domain.model.RepeatFrequencyType.WEEKENDS)
                            onExpandToggle()
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), color = Border)

                    // 法定节假日
                    RepeatOptionItem(
                        text = "法定节假日",
                        isSelected = repeatFrequency.type == com.nextthing.app.domain.model.RepeatFrequencyType.LEGAL_HOLIDAY,
                        onClick = {
                            onRepeatFrequencyTypeChange(com.nextthing.app.domain.model.RepeatFrequencyType.LEGAL_HOLIDAY)
                            onExpandToggle()
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), color = Border)

                    // 自定义（跳转独立页面）
                    val isCustom = repeatFrequency.type == com.nextthing.app.domain.model.RepeatFrequencyType.WEEKLY ||
                            repeatFrequency.type == com.nextthing.app.domain.model.RepeatFrequencyType.MONTHLY
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onExpandToggle()
                                onNavigateToRepeatCustom()
                            }
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isCustom,
                            onClick = {
                                onExpandToggle()
                                onNavigateToRepeatCustom()
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Primary,
                                unselectedColor = TextSecondary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isCustom) repeatFrequency.getDisplayText() else "自定义",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier
                                .size(18.dp)
                                .padding(start = 2.dp)
                        )
                        Text(
                            text = "设置",
                            color = Primary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RepeatOptionItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = Primary,
                unselectedColor = TextSecondary
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = text,
            color = TextPrimary,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun WeekdaySelector(
    selectedWeekdays: Set<Int>,
    onWeekdayToggle: (Int) -> Unit
) {
    val weekdays = listOf("一", "二", "三", "四", "五", "六", "日")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        weekdays.forEachIndexed { index, dayName ->
            val dayNumber = index + 1
            val isSelected = selectedWeekdays.contains(dayNumber)

            Button(
                onClick = { onWeekdayToggle(dayNumber) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) Primary else Border,
                    contentColor = if (isSelected) Color.White else TextPrimary
                ),
                shape = RoundedCornerShape(3.dp),
                contentPadding = PaddingValues(2.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "周",
                        fontSize = 10.sp,
                        color = if (isSelected) Color.White else TextPrimary
                    )
                    Text(
                        text = dayName,
                        fontSize = 10.sp,
                        color = if (isSelected) Color.White else TextPrimary
                    )
                }
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
        items(28) { index ->
            val day = index + 1
            val isSelected = selectedDays.contains(day)

            Button(
                onClick = { onDayToggle(day) },
                modifier = Modifier
                    .aspectRatio(1f)
                    .size(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) Primary else Color.Transparent,
                    contentColor = if (isSelected) Color.White else TextPrimary
                ),
                border = if (!isSelected) BorderStroke(0.5.dp, Border) else null,
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

// ══════════════════════════════════════════════════════════════
// AI 智能输入组件
// ══════════════════════════════════════════════════════════════

@Composable
private fun AIInputSection(
    aiInputText: String,
    isAIParsing: Boolean,
    aiParseResult: AITaskParseResult?,
    showAIResult: Boolean,
    aiError: String?,
    onInputChange: (String) -> Unit,
    onParse: () -> Unit,
    onApplyAndEdit: () -> Unit,
    onApplyAndCreate: () -> Unit,
    onDismissResult: () -> Unit,
    onDismissError: () -> Unit
) {
    val primaryColor = Primary
    val borderColor = Border
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        // 标题
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("✨", fontSize = 16.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "AI 智能输入",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = primaryColor
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 输入框 + 按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = aiInputText,
                onValueChange = onInputChange,
                placeholder = {
                    Text(
                        "试试：明天下午3点开项目周会",
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                },
                modifier = Modifier.weight(1f),
                maxLines = 3,
                enabled = !isAIParsing,
                shape = RoundedCornerShape(10.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = borderColor
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onParse() })
            )

            // 解析按钮
            val btnEnabled = aiInputText.isNotBlank() && !isAIParsing
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (btnEnabled) primaryColor else primaryColor.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable(enabled = btnEnabled, onClick = onParse),
                contentAlignment = Alignment.Center
            ) {
                if (isAIParsing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("🔮", fontSize = 20.sp)
                }
            }
        }

        // 错误提示
        if (aiError != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Danger.copy(alpha = 0.06f)),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Danger.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = aiError,
                        fontSize = 12.sp,
                        color = Danger,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = onDismissError,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("关闭", fontSize = 12.sp, color = Danger)
                    }
                }
            }
        }

        // AI 解析结果卡片
        if (showAIResult && aiParseResult != null) {
            Spacer(modifier = Modifier.height(8.dp))
            AIResultCard(
                result = aiParseResult,
                onApplyAndEdit = onApplyAndEdit,
                onApplyAndCreate = onApplyAndCreate,
                onDismiss = onDismissResult
            )
        }
    }
}

@Composable
private fun AIResultCard(
    result: AITaskParseResult,
    onApplyAndEdit: () -> Unit,
    onApplyAndCreate: () -> Unit,
    onDismiss: () -> Unit
) {
    val primaryColor = Primary
    val successColor = Success
    val warningColor = Warning
    Card(
        colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.04f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI 解析结果",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryColor
                )
                val conf = (result.confidence * 100).toInt()
                Text(
                    text = "置信度 $conf%",
                    fontSize = 11.sp,
                    color = if (result.confidence >= 0.8f) successColor else warningColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            AIResultRow("📝", "标题", result.title)
            result.dueDate?.let {
                AIResultRow(
                    "📅", "时间",
                    it.format(java.time.format.DateTimeFormatter.ofPattern("MM月dd日 HH:mm"))
                )
            }
            result.categoryName?.let { AIResultRow("📁", "分类", it) }
            result.importance?.let { AIResultRow("🔥", "优先级", it.displayName) }
            result.locationName?.let { AIResultRow("📍", "地点", it) }
            if (result.repeatType != null && result.repeatType != RepeatFrequencyType.NONE) {
                AIResultRow("🔄", "重复", result.repeatType.name)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Text("取消", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onApplyAndEdit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, primaryColor),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Text("继续编辑", fontSize = 12.sp, color = primaryColor)
                }
                Button(
                    onClick = onApplyAndCreate,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Text("直接创建", fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun AIResultRow(emoji: String, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 13.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text("$label：", fontSize = 12.sp, color = TextSecondary)
        Text(value, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
    }
}