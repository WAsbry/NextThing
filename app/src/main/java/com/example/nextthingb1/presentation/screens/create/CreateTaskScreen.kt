package com.example.nextthingb1.presentation.screens.create

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
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.example.nextthingb1.util.ToastHelper
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.example.nextthingb1.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import com.example.nextthingb1.domain.model.TaskCategory
import com.example.nextthingb1.domain.model.CategoryItem
import com.example.nextthingb1.domain.model.LocationInfo
import com.example.nextthingb1.domain.model.TaskImportanceUrgency
import com.example.nextthingb1.domain.model.NotificationStrategy
import com.example.nextthingb1.presentation.theme.*

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
    onNavigateToCreateLocation: () -> Unit,
    onNavigateToCreateNotificationStrategy: () -> Unit,
    viewModel: CreateTaskViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val savedLocations by viewModel.savedLocations.collectAsState()
    val showCreateCategoryDialog by viewModel.showCreateCategoryDialog.collectAsState()
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp

    // 折叠状态管理 - 使用单一状态追踪当前展开的卡片
    var expandedCard by remember { mutableStateOf<String?>(null) }
    var isListening by remember { mutableStateOf(false) }

    // 日期选择状态
    var showDatePicker by remember { mutableStateOf(false) }

    // 移除本地状态，在LocationConfigCard内部处理

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 顶部导航区 (8%高度)
        TopNavigationSection(
            screenHeight = screenHeight,
            screenWidth = screenWidth,
            isListening = isListening,
            onBackPressed = onBackPressed,
            onVoiceClick = { isListening = !isListening }
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
            isLocationExpanded = expandedCard == "location",
            isImageExpanded = expandedCard == "image",
            isImportanceExpanded = expandedCard == "importance",
            isReminderExpanded = expandedCard == "reminder",
            isRepeatExpanded = expandedCard == "repeat",
            onTimeExpandToggle = { expandedCard = if (expandedCard == "time") null else "time" },
            onPreciseTimeExpandToggle = { expandedCard = if (expandedCard == "precise_time") null else "precise_time" },
            onCategoryExpandToggle = { expandedCard = if (expandedCard == "category") null else "category" },
            onLocationExpandToggle = { expandedCard = if (expandedCard == "location") null else "location" },
            onImageExpandToggle = { expandedCard = if (expandedCard == "image") null else "image" },
            onImportanceExpandToggle = { expandedCard = if (expandedCard == "importance") null else "importance" },
            onReminderExpandToggle = { expandedCard = if (expandedCard == "reminder") null else "reminder" },
            onRepeatExpandToggle = { expandedCard = if (expandedCard == "repeat") null else "repeat" },
            selectedCategoryItem = uiState.selectedCategoryItem,
            categories = categories,
            onCategorySelected = { viewModel.updateSelectedCategory(it) },
            onCreateCategoryClicked = { viewModel.showCreateCategoryDialog() },
            onDeleteCategory = { viewModel.deleteCategory(it) },
            onPinCategory = { categoryId, isPinned -> viewModel.pinCategory(categoryId, isPinned) },
            selectedDate = uiState.selectedDate,
            onDateSelected = { viewModel.updateSelectedDate(it) },
            onShowDatePicker = { showDatePicker = true },
            preciseTime = uiState.preciseTime,
            onPreciseTimeSelected = { viewModel.updatePreciseTime(it) },
            savedLocations = savedLocations,
            onNavigateToCreateLocation = onNavigateToCreateLocation,
            onDeleteLocation = { locationId -> viewModel.deleteLocation(locationId) },
            selectedImageUri = uiState.selectedImageUri,
            onImageSelected = { viewModel.updateSelectedImage(it) },
            onImageCleared = { viewModel.clearSelectedImage() },
            selectedImportanceUrgency = uiState.importanceUrgency,
            onImportanceUrgencySelected = { viewModel.updateImportanceUrgency(it) },
            availableNotificationStrategies = uiState.availableNotificationStrategies,
            selectedNotificationStrategyId = uiState.notificationStrategyId,
            onNotificationStrategySelected = { viewModel.updateNotificationStrategy(it) },
            onNavigateToCreateNotificationStrategy = onNavigateToCreateNotificationStrategy,
            repeatFrequency = uiState.repeatFrequency,
            onRepeatFrequencyTypeChange = { viewModel.updateRepeatFrequencyType(it) },
            onRepeatWeekdaysChange = { viewModel.updateRepeatWeekdays(it) },
            onRepeatMonthDaysChange = { viewModel.updateRepeatMonthDays(it) }
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

    // 新建分类对话框
    if (showCreateCategoryDialog) {
        CreateCategoryDialog(
            onConfirm = { categoryName ->
                viewModel.createCategory(categoryName)
            },
            onDismiss = { viewModel.hideCreateCategoryDialog() }
        )
    }
}

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
            .background(Color(0xFF71CBF4))
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
                    imageVector = Icons.Default.ArrowBack,
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
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // 主要输入区域
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    BasicTextField(
                        value = title,
                        onValueChange = onTitleChange,
                        textStyle = TextStyle(
                            color = Color(0xFF424242),
                            fontSize = 16.sp,
                            lineHeight = 20.sp
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                        decorationBox = { innerTextField ->
                            if (title.isEmpty()) {
                                Text(
                                    text = "输入任务（如 \"明天 4 点做饭\"）",
                                    color = Color(0xFF9E9E9E),
                                    fontSize = 16.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                }

                // 底部工具栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "AI 分析创建",
                        color = Color(0xFF2196F3),
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clickable { /* TODO: AI 分析 */ }
                            .padding(4.dp)
                    )
                }
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
    isLocationExpanded: Boolean,
    isImageExpanded: Boolean,
    isImportanceExpanded: Boolean,
    isReminderExpanded: Boolean,
    isRepeatExpanded: Boolean,
    onTimeExpandToggle: () -> Unit,
    onPreciseTimeExpandToggle: () -> Unit,
    onCategoryExpandToggle: () -> Unit,
    onLocationExpandToggle: () -> Unit,
    onImageExpandToggle: () -> Unit,
    onImportanceExpandToggle: () -> Unit,
    onReminderExpandToggle: () -> Unit,
    onRepeatExpandToggle: () -> Unit,
    selectedCategoryItem: CategoryItem?,
    categories: List<CategoryItem>,
    onCategorySelected: (CategoryItem) -> Unit,
    onCreateCategoryClicked: () -> Unit,
    onDeleteCategory: (String) -> Unit,
    onPinCategory: (String, Boolean) -> Unit,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate?) -> Unit,
    onShowDatePicker: () -> Unit,
    preciseTime: Pair<Int, Int>?,
    onPreciseTimeSelected: (Pair<Int, Int>?) -> Unit,
    savedLocations: List<LocationInfo>,
    onNavigateToCreateLocation: () -> Unit,
    onDeleteLocation: (String) -> Unit,
    selectedImageUri: String?,
    onImageSelected: (String?) -> Unit,
    onImageCleared: () -> Unit,
    selectedImportanceUrgency: TaskImportanceUrgency?,
    onImportanceUrgencySelected: (TaskImportanceUrgency?) -> Unit,
    availableNotificationStrategies: List<NotificationStrategy>,
    selectedNotificationStrategyId: String?,
    onNotificationStrategySelected: (String?) -> Unit,
    onNavigateToCreateNotificationStrategy: () -> Unit,
    repeatFrequency: com.example.nextthingb1.domain.model.RepeatFrequency,
    onRepeatFrequencyTypeChange: (com.example.nextthingb1.domain.model.RepeatFrequencyType) -> Unit,
    onRepeatWeekdaysChange: (Set<Int>) -> Unit,
    onRepeatMonthDaysChange: (Set<Int>) -> Unit
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

        // 第二行
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
                onCreateCategoryClicked = onCreateCategoryClicked,
                onDeleteCategory = onDeleteCategory,
                onPinCategory = onPinCategory,
                modifier = Modifier.weight(1f)
            )

            // 地点配置卡
            LocationConfigCard(
                screenHeight = screenHeight,
                screenWidth = screenWidth,
                isExpanded = isLocationExpanded,
                onExpandToggle = onLocationExpandToggle,
                savedLocations = savedLocations,
                selectedLocation = null,
                onLocationSelected = { onLocationExpandToggle() },
                onNavigateToCreateLocation = onNavigateToCreateLocation,
                onDeleteLocation = onDeleteLocation,
                modifier = Modifier.weight(1f)
            )
        }

        // 第三行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
        }

        // 第四行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                modifier = Modifier.weight(1f)
            )

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
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// 时间配置卡
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
            colors = CardDefaults.cardColors(containerColor = Color.White),
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
                    text = "完成时间",
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
                        text = "📅",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    Text(
                        text = formatDate(selectedDate),
                        color = Color(0xFF424242),
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

        // 展开的选项菜单
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
                        color = Color(0xFF424242),
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
                        color = Color(0xFF424242),
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
                        color = Color(0xFF2196F3),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

// 精确时间配置卡
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
            colors = CardDefaults.cardColors(containerColor = Color.White),
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
                    text = "精确时间",
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
                        color = if (preciseTime != null) Color(0xFF424242) else Color(0xFF9E9E9E),
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

        // 展开的时间选择器
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
                                color = Color(0xFF424242)
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
                                    contentColor = Color(0xFFEF5350)
                                ),
                                border = BorderStroke(1.dp, Color(0xFFEF5350)),
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
                                containerColor = Color(0xFF2196F3)
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
    val density = LocalDensity.current
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
            .background(Color(0xFFF8F9FA), RoundedCornerShape(8.dp))
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
                        color = if (isSelected) Color(0xFF2196F3) else Color(0xFF9E9E9E),
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
                    Color(0xFF2196F3).copy(alpha = 0.1f),
                    RoundedCornerShape(4.dp)
                )
        )
    }
}

// 分类配置卡
@Composable
internal fun CategoryPriorityConfigCard(
    screenHeight: androidx.compose.ui.unit.Dp,
    screenWidth: androidx.compose.ui.unit.Dp,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    selectedCategoryItem: CategoryItem?,
    categories: List<CategoryItem>,
    onCategorySelected: (CategoryItem) -> Unit,
    onCreateCategoryClicked: () -> Unit,
    onDeleteCategory: (String) -> Unit,
    onPinCategory: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isEditMode: Boolean = true
) {
    Column(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clickable(enabled = isEditMode) { onExpandToggle() },
            colors = CardDefaults.cardColors(containerColor = Color.White),
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
                    text = "分类选择",
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
                        text = "🏷️",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    Text(
                        text = selectedCategoryItem?.displayName ?: "生活",
                        color = Color(0xFF424242),
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

        // 展开的分类选项菜单
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
                    // 显示所有分类
                    categories.forEach { category ->
                        CategoryMenuItem(
                            category = category,
                            onClick = { onCategorySelected(category); onExpandToggle() },
                            onDelete = onDeleteCategory,
                            onPin = onPinCategory
                        )
                    }

                    // 新建分类选项
                    Text(
                        text = "新建分类",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onCreateCategoryClicked()
                                // 不关闭卡片,让用户看到对话框
                            }
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                        color = Color(0xFF2196F3),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

// 地点配置卡
@Composable
internal fun LocationConfigCard(
    screenHeight: androidx.compose.ui.unit.Dp,
    screenWidth: androidx.compose.ui.unit.Dp,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    savedLocations: List<LocationInfo>,
    selectedLocation: LocationInfo?,
    onLocationSelected: (LocationInfo?) -> Unit,
    onNavigateToCreateLocation: () -> Unit,
    onDeleteLocation: (String) -> Unit,
    modifier: Modifier = Modifier,
    isEditMode: Boolean = true
) {
    // 内部状态管理选中的位置
    var internalSelectedLocation by remember { mutableStateOf<LocationInfo?>(null) }
    Column(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clickable(enabled = isEditMode) { onExpandToggle() },
            colors = CardDefaults.cardColors(containerColor = Color.White),
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
                    text = "地理位置",
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
                        text = "📍",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    Text(
                        text = internalSelectedLocation?.locationName ?: "未选择",
                        color = Color(0xFF424242),
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

        // 展开的位置选项菜单
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
                    // 显示已保存的地点
                    savedLocations.forEach { location ->
                        LocationMenuItem(
                            location = location,
                            onClick = {
                                internalSelectedLocation = location
                                onLocationSelected(location)
                            },
                            onDelete = { locationId ->
                                onDeleteLocation(locationId)
                            }
                        )
                    }

                    // 新建地点选项
                    Text(
                        text = "新建地点",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onNavigateToCreateLocation()
                                onExpandToggle()
                            }
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                        color = Color(0xFF2196F3),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationMenuItem(
    location: LocationInfo,
    onClick: () -> Unit,
    onDelete: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = location.locationName,
            color = Color(0xFF424242),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        // 删除按钮（只对手动添加的地点显示）
        if (location.locationType == com.example.nextthingb1.domain.model.LocationType.MANUAL) {
            IconButton(
                onClick = { onDelete(location.id) },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_delete),
                    contentDescription = "删除地点",
                    tint = Color(0xFFE57373),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun RealTimeLocationMenuItem(
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "实时位置",
            color = Color(0xFF424242),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

// 图片配置卡
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
            colors = CardDefaults.cardColors(containerColor = Color.White),
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
                    text = "任务图片",
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
                        text = "🖼️",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    Text(
                        text = if (selectedImageUri != null) "已选择" else "未选择",
                        color = Color(0xFF424242),
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

        // 展开的内容区域
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
                                    color = Color(0xFF424242),
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
                                    color = Color(0xFF424242),
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
            colors = CardDefaults.cardColors(containerColor = Color.White),
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
                        text = getImportanceIcon(internalSelectedImportanceUrgency),
                        fontSize = 16.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    Text(
                        text = internalSelectedImportanceUrgency?.displayName ?: "未选择",
                        color = Color(0xFF424242),
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

        // 展开的重要性和紧急性选项菜单
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

// 通知策略配置卡
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
    modifier: Modifier = Modifier,
    isEditMode: Boolean = true
) {
    val selectedStrategy = availableStrategies.find { it.id == selectedStrategyId }

    Column(modifier = modifier) {
        // 主卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clickable(enabled = isEditMode) { onExpandToggle() },
            colors = CardDefaults.cardColors(containerColor = Color.White),
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
                    text = "通知策略",
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
                        text = "🔔",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    Text(
                        text = selectedStrategy?.name ?: "未设置",
                        color = if (selectedStrategy != null) Color(0xFF424242) else Color(0xFF9E9E9E),
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

        // 展开的内容区域
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
                    // 不使用通知策略选项
                    Text(
                        text = "不使用",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onStrategySelected(null)
                                onExpandToggle()
                            }
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                        color = if (selectedStrategyId == null) Color(0xFF2196F3) else Color(0xFF424242),
                        fontSize = 14.sp
                    )

                    // 显示已保存的通知策略列表
                    if (availableStrategies.isNotEmpty()) {
                        availableStrategies.forEach { strategy ->
                            Text(
                                text = strategy.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onStrategySelected(strategy.id)
                                        onExpandToggle()
                                    }
                                    .padding(vertical = 8.dp, horizontal = 12.dp),
                                color = if (strategy.id == selectedStrategyId) Color(0xFF2196F3) else Color(0xFF424242),
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        Text(
                            text = "暂无通知策略",
                            color = Color(0xFF9E9E9E),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
                        )
                    }

                    // 新建策略按钮
                    TextButton(
                        onClick = {
                            onNavigateToCreateNotificationStrategy()
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
                                text = "➕",
                                fontSize = 16.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = "新建策略",
                                color = Color(0xFF424242),
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

// 底部操作区
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
            color = Color(0xFF424242),
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
                containerColor = if (isEnabled) Color(0xFF2196F3) else Color(0xFFB3D9F2),
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

// 新建分类对话框
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

// 分类菜单项
@Composable
private fun CategoryMenuItem(
    category: CategoryItem,
    onClick: () -> Unit,
    onDelete: (String) -> Unit,
    onPin: (String, Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = category.displayName,
            color = Color(0xFF424242),
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )

        // 置顶按钮
        IconButton(
            onClick = { onPin(category.id, !category.isPinned) },
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                painter = painterResource(
                    id = if (category.isPinned) android.R.drawable.btn_star_big_on
                        else android.R.drawable.btn_star_big_off
                ),
                contentDescription = if (category.isPinned) "取消置顶" else "置顶",
                tint = if (category.isPinned) Color(0xFFFFD700) else Color(0xFF9E9E9E),
                modifier = Modifier.size(16.dp)
            )
        }

        // 删除按钮
        IconButton(
            onClick = { onDelete(category.id) },
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_delete),
                contentDescription = "删除",
                tint = Color(0xFFE57373),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// 重复频次配置卡
@Composable
internal fun RepeatFrequencyConfigCard(
    screenHeight: androidx.compose.ui.unit.Dp,
    screenWidth: androidx.compose.ui.unit.Dp,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    repeatFrequency: com.example.nextthingb1.domain.model.RepeatFrequency,
    onRepeatFrequencyTypeChange: (com.example.nextthingb1.domain.model.RepeatFrequencyType) -> Unit,
    onWeekdaysChange: (Set<Int>) -> Unit,
    onMonthDaysChange: (Set<Int>) -> Unit,
    modifier: Modifier = Modifier,
    isEditMode: Boolean = true
) {
    val context = LocalContext.current
    var selectedWeekdays by remember { mutableStateOf(repeatFrequency.weekdays) }
    var selectedMonthDays by remember { mutableStateOf(repeatFrequency.monthDays) }
    var selectedType by remember { mutableStateOf(repeatFrequency.type) }

    // 同步外部传入的repeatFrequency到本地状态
    LaunchedEffect(repeatFrequency) {
        selectedWeekdays = repeatFrequency.weekdays
        selectedMonthDays = repeatFrequency.monthDays
        selectedType = repeatFrequency.type
    }

    Column(modifier = modifier) {
        // 主卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clickable(enabled = isEditMode) { onExpandToggle() },
            colors = CardDefaults.cardColors(containerColor = Color.White),
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
                    text = "重复频次",
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
                        text = "⏰",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    Text(
                        text = repeatFrequency.getDisplayText(),
                        color = if (repeatFrequency.type == com.example.nextthingb1.domain.model.RepeatFrequencyType.NONE)
                               Color(0xFF9E9E9E) else Color(0xFF424242),
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

        // 展开的选项面板
        if (isExpanded && isEditMode) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .heightIn(max = (screenHeight * 0.6f)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(0.5.dp, Color(0xFFE0E0E0)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // 基础选项
                    RepeatOptionItem(
                        text = "单次任务",
                        isSelected = selectedType == com.example.nextthingb1.domain.model.RepeatFrequencyType.NONE,
                        onClick = {
                            selectedType = com.example.nextthingb1.domain.model.RepeatFrequencyType.NONE
                            onRepeatFrequencyTypeChange(com.example.nextthingb1.domain.model.RepeatFrequencyType.NONE)
                            onExpandToggle()
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = Color(0xFFE0E0E0)
                    )

                    RepeatOptionItem(
                        text = "每日任务",
                        isSelected = selectedType == com.example.nextthingb1.domain.model.RepeatFrequencyType.DAILY,
                        onClick = {
                            selectedType = com.example.nextthingb1.domain.model.RepeatFrequencyType.DAILY
                            onRepeatFrequencyTypeChange(com.example.nextthingb1.domain.model.RepeatFrequencyType.DAILY)
                            onExpandToggle()
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = Color(0xFFE0E0E0)
                    )

                    // 自定义每周
                    RepeatOptionItem(
                        text = "自定义（每周）",
                        isSelected = selectedType == com.example.nextthingb1.domain.model.RepeatFrequencyType.WEEKLY,
                        onClick = {
                            selectedType = com.example.nextthingb1.domain.model.RepeatFrequencyType.WEEKLY
                            onRepeatFrequencyTypeChange(com.example.nextthingb1.domain.model.RepeatFrequencyType.WEEKLY)
                        }
                    )

                    // 星期选择器
                    if (selectedType == com.example.nextthingb1.domain.model.RepeatFrequencyType.WEEKLY) {
                        WeekdaySelector(
                            selectedWeekdays = selectedWeekdays,
                            onWeekdayToggle = { day ->
                                selectedWeekdays = if (selectedWeekdays.contains(day)) {
                                    selectedWeekdays - day
                                } else {
                                    // 限制不能全选（最多选择6天）
                                    if (selectedWeekdays.size < 6) {
                                        selectedWeekdays + day
                                    } else {
                                        ToastHelper.showDebouncedToast(context, "若全选，建议设置为每日任务")
                                        selectedWeekdays
                                    }
                                }
                                onWeekdaysChange(selectedWeekdays)
                            }
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = Color(0xFFE0E0E0)
                    )

                    // 自定义每月
                    RepeatOptionItem(
                        text = "自定义（每月）",
                        isSelected = selectedType == com.example.nextthingb1.domain.model.RepeatFrequencyType.MONTHLY,
                        onClick = {
                            selectedType = com.example.nextthingb1.domain.model.RepeatFrequencyType.MONTHLY
                            onRepeatFrequencyTypeChange(com.example.nextthingb1.domain.model.RepeatFrequencyType.MONTHLY)
                        }
                    )

                    // 月份日期选择器
                    if (selectedType == com.example.nextthingb1.domain.model.RepeatFrequencyType.MONTHLY) {
                        MonthDaySelector(
                            selectedDays = selectedMonthDays,
                            onDayToggle = { day ->
                                selectedMonthDays = if (selectedMonthDays.contains(day)) {
                                    selectedMonthDays - day
                                } else {
                                    // 限制不能全选（最多选择27天）
                                    if (selectedMonthDays.size < 27) {
                                        selectedMonthDays + day
                                    } else {
                                        ToastHelper.showDebouncedToast(context, "若全选，建议设置为每日任务")
                                        selectedMonthDays
                                    }
                                }
                                onMonthDaysChange(selectedMonthDays)
                            }
                        )
                    }

                    // 确认按钮
                    if (selectedType == com.example.nextthingb1.domain.model.RepeatFrequencyType.WEEKLY ||
                        selectedType == com.example.nextthingb1.domain.model.RepeatFrequencyType.MONTHLY) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                when {
                                    selectedType == com.example.nextthingb1.domain.model.RepeatFrequencyType.WEEKLY && selectedWeekdays.isEmpty() -> {
                                        ToastHelper.showDebouncedToast(context, "请至少选择一个星期")
                                    }
                                    selectedType == com.example.nextthingb1.domain.model.RepeatFrequencyType.MONTHLY && selectedMonthDays.isEmpty() -> {
                                        ToastHelper.showDebouncedToast(context, "请至少选择一个日期")
                                    }
                                    else -> {
                                        onExpandToggle()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .align(Alignment.CenterHorizontally),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                            shape = RoundedCornerShape(5.dp)
                        ) {
                            Text(
                                text = "确认",
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
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
                    containerColor = if (isSelected) Color(0xFF2196F3) else Color(0xFFE0E0E0),
                    contentColor = if (isSelected) Color.White else Color(0xFF424242)
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
                        color = if (isSelected) Color.White else Color(0xFF424242)
                    )
                    Text(
                        text = dayName,
                        fontSize = 10.sp,
                        color = if (isSelected) Color.White else Color(0xFF424242)
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