package com.example.nextthingb1.presentation.screens.create

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.TextStyle
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

    // 折叠状态管理
    var isTimeExpanded by remember { mutableStateOf(false) }
    var isCategoryExpanded by remember { mutableStateOf(false) }
    var isLocationExpanded by remember { mutableStateOf(false) }
    var isImageExpanded by remember { mutableStateOf(false) }
    var isImportanceExpanded by remember { mutableStateOf(false) }
    var isReminderExpanded by remember { mutableStateOf(false) }
    var isRepeatExpanded by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }

    // 日期选择状态
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
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
            isTimeExpanded = isTimeExpanded,
            isCategoryExpanded = isCategoryExpanded,
            isLocationExpanded = isLocationExpanded,
            isImageExpanded = isImageExpanded,
            isImportanceExpanded = isImportanceExpanded,
            isReminderExpanded = isReminderExpanded,
            isRepeatExpanded = isRepeatExpanded,
            onTimeExpandToggle = { isTimeExpanded = !isTimeExpanded },
            onCategoryExpandToggle = { isCategoryExpanded = !isCategoryExpanded },
            onLocationExpandToggle = { isLocationExpanded = !isLocationExpanded },
            onImageExpandToggle = { isImageExpanded = !isImageExpanded },
            onImportanceExpandToggle = { isImportanceExpanded = !isImportanceExpanded },
            onReminderExpandToggle = { isReminderExpanded = !isReminderExpanded },
            onRepeatExpandToggle = { isRepeatExpanded = !isRepeatExpanded },
            selectedCategoryItem = uiState.selectedCategoryItem,
            categories = categories,
            onCategorySelected = { viewModel.updateSelectedCategory(it) },
            onCreateCategoryClicked = { viewModel.showCreateCategoryDialog() },
            onDeleteCategory = { viewModel.deleteCategory(it) },
            onPinCategory = { categoryId, isPinned -> viewModel.pinCategory(categoryId, isPinned) },
            selectedDate = selectedDate,
            onDateSelected = { selectedDate = it },
            onShowDatePicker = { showDatePicker = true },
            savedLocations = savedLocations,
            onNavigateToCreateLocation = onNavigateToCreateLocation,
            onDeleteLocation = { locationId -> viewModel.deleteLocation(locationId) },
            selectedImageUri = uiState.selectedImageUri,
            onImageSelected = { viewModel.updateSelectedImage(it) },
            onImageCleared = { viewModel.clearSelectedImage() },
            selectedImportanceUrgency = uiState.importanceUrgency,
            onImportanceUrgencySelected = { viewModel.updateImportanceUrgency(it) },
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
                selectedDate = date
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
    isCategoryExpanded: Boolean,
    isLocationExpanded: Boolean,
    isImageExpanded: Boolean,
    isImportanceExpanded: Boolean,
    isReminderExpanded: Boolean,
    isRepeatExpanded: Boolean,
    onTimeExpandToggle: () -> Unit,
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
    savedLocations: List<LocationInfo>,
    onNavigateToCreateLocation: () -> Unit,
    onDeleteLocation: (String) -> Unit,
    selectedImageUri: String?,
    onImageSelected: (String?) -> Unit,
    onImageCleared: () -> Unit,
    selectedImportanceUrgency: TaskImportanceUrgency?,
    onImportanceUrgencySelected: (TaskImportanceUrgency?) -> Unit,
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
        }

        // 第二行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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

        // 第三行
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
                modifier = Modifier.weight(1f)
            )
        }

        // 第四行
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
                onNavigateToCreateNotificationStrategy = onNavigateToCreateNotificationStrategy,
                modifier = Modifier.weight(1f)
            )

            // 占位卡片（保持布局平衡）
            Spacer(modifier = Modifier.weight(1f))
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
                        text = internalSelectedLocation?.locationName ?: "实时位置",
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
                    // 实时位置选项
                    RealTimeLocationMenuItem(
                        onClick = {
                            internalSelectedLocation = null
                            onLocationSelected(null)
                        }
                    )

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
    var internalSelectedImportanceUrgency by remember { mutableStateOf<TaskImportanceUrgency?>(null) }

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
                        text = internalSelectedImportanceUrgency?.displayName ?: "选择重要程度",
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
    onNavigateToCreateNotificationStrategy: () -> Unit,
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
                        text = "未设置",
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
                    // 这里将来会显示已保存的通知策略列表
                    Text(
                        text = "暂无通知策略",
                        color = Color(0xFF9E9E9E),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

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
                                if ((selectedType == com.example.nextthingb1.domain.model.RepeatFrequencyType.WEEKLY && selectedWeekdays.isEmpty()) ||
                                    (selectedType == com.example.nextthingb1.domain.model.RepeatFrequencyType.MONTHLY && selectedMonthDays.isEmpty())) {
                                    // TODO: 显示错误提示
                                } else {
                                    onExpandToggle()
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