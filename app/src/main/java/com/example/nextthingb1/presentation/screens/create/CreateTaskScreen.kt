package com.example.nextthingb1.presentation.screens.create

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.example.nextthingb1.domain.model.TaskPriority
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
            onTimeExpandToggle = { isTimeExpanded = !isTimeExpanded },
            onCategoryExpandToggle = { isCategoryExpanded = !isCategoryExpanded },
            onLocationExpandToggle = { isLocationExpanded = !isLocationExpanded },
            onImageExpandToggle = { isImageExpanded = !isImageExpanded },
            onImportanceExpandToggle = { isImportanceExpanded = !isImportanceExpanded },
            onReminderExpandToggle = { isReminderExpanded = !isReminderExpanded },
            selectedCategoryItem = uiState.selectedCategoryItem,
            selectedPriority = uiState.priority,
            categories = categories,
            onCategorySelected = { viewModel.updateSelectedCategory(it) },
            onPrioritySelected = { viewModel.updatePriority(it) },
            onCreateCategoryClicked = { viewModel.showCreateCategoryDialog() },
            onDeleteCategory = { viewModel.deleteCategory(it) },
            onPinCategory = { categoryId, isPinned -> viewModel.pinCategory(categoryId, isPinned) },
            selectedDate = selectedDate,
            onDateSelected = { selectedDate = it },
            onShowDatePicker = { showDatePicker = true },
            savedLocations = savedLocations,
            onNavigateToCreateLocation = onNavigateToCreateLocation,
            selectedImageUri = uiState.selectedImageUri,
            onImageSelected = { viewModel.updateSelectedImage(it) },
            onImageCleared = { viewModel.clearSelectedImage() },
            selectedImportanceUrgency = uiState.importanceUrgency,
            onImportanceUrgencySelected = { viewModel.updateImportanceUrgency(it) },
            onNavigateToCreateNotificationStrategy = onNavigateToCreateNotificationStrategy
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
    onTimeExpandToggle: () -> Unit,
    onCategoryExpandToggle: () -> Unit,
    onLocationExpandToggle: () -> Unit,
    onImageExpandToggle: () -> Unit,
    onImportanceExpandToggle: () -> Unit,
    onReminderExpandToggle: () -> Unit,
    selectedCategoryItem: CategoryItem?,
    selectedPriority: TaskPriority,
    categories: List<CategoryItem>,
    onCategorySelected: (CategoryItem) -> Unit,
    onPrioritySelected: (TaskPriority) -> Unit,
    onCreateCategoryClicked: () -> Unit,
    onDeleteCategory: (String) -> Unit,
    onPinCategory: (String, Boolean) -> Unit,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate?) -> Unit,
    onShowDatePicker: () -> Unit,
    savedLocations: List<LocationInfo>,
    onNavigateToCreateLocation: () -> Unit,
    selectedImageUri: String?,
    onImageSelected: (String?) -> Unit,
    onImageCleared: () -> Unit,
    selectedImportanceUrgency: TaskImportanceUrgency?,
    onImportanceUrgencySelected: (TaskImportanceUrgency?) -> Unit,
    onNavigateToCreateNotificationStrategy: () -> Unit
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

            // 分类・优先级配置卡
            CategoryPriorityConfigCard(
                screenHeight = screenHeight,
                screenWidth = screenWidth,
                isExpanded = isCategoryExpanded,
                onExpandToggle = onCategoryExpandToggle,
                selectedCategoryItem = selectedCategoryItem,
                selectedPriority = selectedPriority,
                categories = categories,
                onCategorySelected = onCategorySelected,
                onPrioritySelected = onPrioritySelected,
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

            // 通知策略配置卡
            NotificationStrategyConfigCard(
                screenHeight = screenHeight,
                screenWidth = screenWidth,
                isExpanded = isReminderExpanded,
                onExpandToggle = onReminderExpandToggle,
                onNavigateToCreateNotificationStrategy = onNavigateToCreateNotificationStrategy,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// 时间配置卡
@Composable
private fun TimeConfigCard(
    screenHeight: androidx.compose.ui.unit.Dp,
    screenWidth: androidx.compose.ui.unit.Dp,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate?) -> Unit,
    onShowDatePicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clickable { onExpandToggle() },
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

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color(0xFF9E9E9E),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 展开的选项菜单
        if (isExpanded) {
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

// 分类・优先级配置卡
@Composable
private fun CategoryPriorityConfigCard(
    screenHeight: androidx.compose.ui.unit.Dp,
    screenWidth: androidx.compose.ui.unit.Dp,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    selectedCategoryItem: CategoryItem?,
    selectedPriority: TaskPriority,
    categories: List<CategoryItem>,
    onCategorySelected: (CategoryItem) -> Unit,
    onPrioritySelected: (TaskPriority) -> Unit,
    onCreateCategoryClicked: () -> Unit,
    onDeleteCategory: (String) -> Unit,
    onPinCategory: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clickable { onExpandToggle() },
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

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color(0xFF9E9E9E),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 展开的分类选项菜单
        if (isExpanded) {
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
private fun LocationConfigCard(
    screenHeight: androidx.compose.ui.unit.Dp,
    screenWidth: androidx.compose.ui.unit.Dp,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    savedLocations: List<LocationInfo>,
    selectedLocation: LocationInfo?,
    onLocationSelected: (LocationInfo?) -> Unit,
    onNavigateToCreateLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 内部状态管理选中的位置
    var internalSelectedLocation by remember { mutableStateOf<LocationInfo?>(null) }
    Column(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clickable { onExpandToggle() },
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

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color(0xFF9E9E9E),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 展开的位置选项菜单
        if (isExpanded) {
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
                    LocationMenuItem(
                        title = "实时位置",
                        onClick = {
                            internalSelectedLocation = null
                            onLocationSelected(null)
                        }
                    )

                    // 显示已保存的地点
                    savedLocations.forEach { location ->
                        LocationMenuItem(
                            title = location.locationName,
                            onClick = {
                                internalSelectedLocation = location
                                onLocationSelected(location)
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
    title: String,
    onClick: () -> Unit
) {
    Text(
        text = title,
        color = Color(0xFF424242),
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 12.dp)
    )
}

// 图片配置卡
@Composable
private fun ImageConfigCard(
    screenHeight: androidx.compose.ui.unit.Dp,
    screenWidth: androidx.compose.ui.unit.Dp,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    selectedImageUri: String?,
    onImageSelected: (String?) -> Unit,
    onImageCleared: () -> Unit,
    modifier: Modifier = Modifier
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
                .clickable { onExpandToggle() },
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

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color(0xFF9E9E9E),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 展开的内容区域
        if (isExpanded) {
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
private fun ImportanceConfigCard(
    screenHeight: androidx.compose.ui.unit.Dp,
    screenWidth: androidx.compose.ui.unit.Dp,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    selectedImportanceUrgency: TaskImportanceUrgency?,
    onImportanceUrgencySelected: (TaskImportanceUrgency?) -> Unit,
    modifier: Modifier = Modifier
) {
    // 内部状态管理选中的重要性和紧急性组合
    var internalSelectedImportanceUrgency by remember { mutableStateOf<TaskImportanceUrgency?>(null) }

    Column(modifier = modifier) {
        // 主卡片：显示当前选中的重要性和紧急性
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clickable { onExpandToggle() },
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

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color(0xFF9E9E9E),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 展开的重要性和紧急性选项菜单
        if (isExpanded) {
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
private fun NotificationStrategyConfigCard(
    screenHeight: androidx.compose.ui.unit.Dp,
    screenWidth: androidx.compose.ui.unit.Dp,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onNavigateToCreateNotificationStrategy: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 主卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clickable { onExpandToggle() },
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

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color(0xFF9E9E9E),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 展开的内容区域
        if (isExpanded) {
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
private fun MaterialDatePickerDialog(
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