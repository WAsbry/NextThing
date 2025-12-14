package com.example.nextthingb1.presentation.screens.geofence.relatedtasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nextthingb1.domain.model.Task
import com.example.nextthingb1.presentation.theme.*
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelatedTasksScreen(
    navController: NavController,
    viewModel: RelatedTasksViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "关联任务 (${uiState.allTasks.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Text("←", fontSize = 24.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BgCard
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BgPrimary)
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Primary)
            }
        } else if (uiState.errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BgPrimary)
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.errorMessage ?: "加载失败",
                    color = TextSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BgPrimary)
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 地点信息卡片
                item {
                    uiState.location?.let { location ->
                        LocationInfoCard(
                            locationName = location.locationInfo.locationName,
                            address = location.locationInfo.address
                        )
                    }
                }

                // 状态筛选Tab
                item {
                    TaskFilterTabs(
                        currentTab = uiState.currentTab,
                        incompleteCount = uiState.incompleteCount,
                        completedCount = uiState.completedCount,
                        onTabChange = { viewModel.switchTab(it) }
                    )
                }

                // 任务列表
                if (uiState.filteredTasks.isEmpty()) {
                    item {
                        EmptyTasksPlaceholder(currentTab = uiState.currentTab)
                    }
                } else {
                    items(uiState.filteredTasks) { task ->
                        TaskCard(
                            task = task,
                            onClick = {
                                navController.navigate("task_detail/${task.id}")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationInfoCard(
    locationName: String,
    address: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📍",
                fontSize = 32.sp,
                modifier = Modifier.padding(end = 12.dp)
            )
            Column {
                Text(
                    text = locationName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (address.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = address,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskFilterTabs(
    currentTab: TaskFilterTab,
    incompleteCount: Int,
    completedCount: Int,
    onTabChange: (TaskFilterTab) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FilterTab(
            text = "未完成",
            count = incompleteCount,
            isSelected = currentTab == TaskFilterTab.INCOMPLETE,
            onClick = { onTabChange(TaskFilterTab.INCOMPLETE) },
            modifier = Modifier.weight(1f)
        )
        FilterTab(
            text = "已完成",
            count = completedCount,
            isSelected = currentTab == TaskFilterTab.COMPLETED,
            onClick = { onTabChange(TaskFilterTab.COMPLETED) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FilterTab(
    text: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Primary else BgCard
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$text ($count)",
                color = if (isSelected) Color.White else TextPrimary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun TaskCard(
    task: Task,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 任务标题
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isCompleted = task.status == com.example.nextthingb1.domain.model.TaskStatus.COMPLETED
                Text(
                    text = if (isCompleted) "✅" else "📋",
                    fontSize = 20.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCompleted) TextSecondary else TextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 截止时间
            task.dueDate?.let { dueDate ->
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📅",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text = dueDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // 分类和优先级
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 分类
                task.category?.let { category ->
                    Text(
                        text = "🏷️ ${category.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                // 优先级
                task.importanceUrgency?.let { iu ->
                    if (task.category != null) {
                        Text(
                            text = " · ",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Text(
                        text = when (iu) {
                            com.example.nextthingb1.domain.model.TaskImportanceUrgency.IMPORTANT_URGENT -> "⭐ 重要紧急"
                            com.example.nextthingb1.domain.model.TaskImportanceUrgency.IMPORTANT_NOT_URGENT -> "📌 重要不紧急"
                            com.example.nextthingb1.domain.model.TaskImportanceUrgency.NOT_IMPORTANT_URGENT -> "⚡ 紧急不重要"
                            com.example.nextthingb1.domain.model.TaskImportanceUrgency.NOT_IMPORTANT_NOT_URGENT -> "📋 不重要不紧急"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTasksPlaceholder(currentTab: TaskFilterTab) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "📭",
                fontSize = 48.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = when (currentTab) {
                    TaskFilterTab.INCOMPLETE -> "暂无未完成的任务"
                    TaskFilterTab.COMPLETED -> "暂无已完成的任务"
                    TaskFilterTab.ALL -> "暂无关联任务"
                },
                color = TextSecondary,
                fontSize = 14.sp
            )
        }
    }
}
