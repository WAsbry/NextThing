package com.example.nextthingb1.presentation.screens.create

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nextthingb1.domain.model.TaskCategory
import com.example.nextthingb1.domain.model.TaskPriority
import com.example.nextthingb1.presentation.theme.*

@Composable
fun CreateTaskScreen(
    onBackPressed: () -> Unit,
    viewModel: CreateTaskViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
    ) {
        // 头部导航
        CreateTaskTopHeader(onBackPressed = onBackPressed)
        
        // 表单内容
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 任务标题输入
            TaskTitleInput(
                title = uiState.title,
                onTitleChange = { viewModel.updateTitle(it) }
            )
            
            // 任务描述输入
            TaskDescriptionInput(
                description = uiState.description,
                onDescriptionChange = { viewModel.updateDescription(it) }
            )
            
            // 优先级选择
            PrioritySelector(
                selectedPriority = uiState.priority,
                onPrioritySelected = { viewModel.updatePriority(it) }
            )
            
            // 分类选择
            CategorySelector(
                selectedCategory = uiState.category,
                onCategorySelected = { viewModel.updateCategory(it) }
            )
            
            // 截止日期选择
            DueDateSelector(
                dueDate = uiState.dueDate,
                onDateSelected = { viewModel.updateDueDate(it) }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // 底部操作按钮
        CreateTaskActions(
            onSave = { viewModel.createTask(); onBackPressed() },
            onCancel = onBackPressed,
            isEnabled = uiState.title.isNotBlank()
        )
    }
}

@Composable
private fun CreateTaskTopHeader(
    onBackPressed: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgCard)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackPressed) {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_revert),
                contentDescription = "返回",
                tint = TextPrimary
            )
        }
        
        Text(
            text = "创建任务",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        
        Spacer(modifier = Modifier.width(48.dp))
    }
}

@Composable
private fun TaskTitleInput(
    title: String,
    onTitleChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "任务标题",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                placeholder = { Text("请输入任务标题") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TaskDescriptionInput(
    description: String,
    onDescriptionChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "任务描述",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                placeholder = { Text("请输入任务描述（可选）") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        }
    }
}

@Composable
private fun PrioritySelector(
    selectedPriority: TaskPriority,
    onPrioritySelected: (TaskPriority) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "优先级",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskPriority.values().forEach { priority ->
                    PriorityChip(
                        priority = priority,
                        isSelected = selectedPriority == priority,
                        onSelected = { onPrioritySelected(priority) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PriorityChip(
    priority: TaskPriority,
    isSelected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isSelected -> when (priority) {
            TaskPriority.HIGH -> Danger
            TaskPriority.MEDIUM -> Warning
            TaskPriority.LOW -> Success
        }
        else -> BgSecondary
    }
    
    val textColor = if (isSelected) Color.White else TextSecondary
    
    Card(
        onClick = onSelected,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Text(
            text = when (priority) {
                TaskPriority.HIGH -> "高"
                TaskPriority.MEDIUM -> "中"
                TaskPriority.LOW -> "低"
            },
            modifier = Modifier.padding(12.dp),
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CategorySelector(
    selectedCategory: TaskCategory,
    onCategorySelected: (TaskCategory) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "分类",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            val categories = listOf(
                TaskCategory.WORK to "工作",
                TaskCategory.STUDY to "学习",
                TaskCategory.LIFE to "生活",
                TaskCategory.HEALTH to "健康",
                TaskCategory.PERSONAL to "个人",
                TaskCategory.OTHER to "其他"
            )
            
            val categoryRows = categories.chunked(2)
            categoryRows.forEachIndexed { index, rowCategories ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for ((category, name) in rowCategories) {
                        CategoryChip(
                            category = category,
                            name = name,
                            isSelected = selectedCategory == category,
                            onSelected = { onCategorySelected(category) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // 填充剩余空间
                    repeat(2 - rowCategories.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                if (index < categoryRows.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(
    category: TaskCategory,
    name: String,
    isSelected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onSelected,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Primary else BgSecondary
        )
    ) {
        Text(
            text = name,
            modifier = Modifier.padding(12.dp),
            color = if (isSelected) Color.White else TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DueDateSelector(
    dueDate: String,
    onDateSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "截止日期",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = dueDate,
                onValueChange = onDateSelected,
                placeholder = { Text("选择截止日期（可选）") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_my_calendar),
                        contentDescription = "选择日期",
                        tint = TextSecondary
                    )
                }
            )
        }
    }
}

@Composable
private fun CreateTaskActions(
    onSave: () -> Unit,
    onCancel: () -> Unit,
    isEnabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgCard)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.weight(1f)
        ) {
            Text("取消")
        }
        
        Button(
            onClick = onSave,
            enabled = isEnabled,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = Primary
            )
        ) {
            Text("保存")
        }
    }
} 