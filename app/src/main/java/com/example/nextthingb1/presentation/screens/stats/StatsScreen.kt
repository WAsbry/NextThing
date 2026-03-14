package com.example.nextthingb1.presentation.screens.stats

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nextthingb1.domain.model.Category
import com.example.nextthingb1.domain.model.Task
import com.example.nextthingb1.presentation.theme.*
import com.example.nextthingb1.presentation.components.TaskItemCard
import androidx.compose.foundation.lazy.items
import java.time.format.DateTimeFormatter
import kotlin.math.*

// 扩展属性：将 Category 的 colorHex 转换为 Compose Color
private val Category.color: Color
    get() = Color(android.graphics.Color.parseColor(this.colorHex))

// 扩展属性：柔和浅色系配色（用于饼图显示）
private val Category.pastelColor: Color
    get() = when (this.name) {
        "工作" -> Color(0xFF81D4FA)  // 浅蓝色
        "学习" -> Color(0xFFA5D6A7)  // 浅绿色
        "生活" -> Color(0xFFFFF59D)  // 浅黄色
        "健康" -> Color(0xFFFFAB91)  // 浅橙色
        "运动" -> Color(0xFFCE93D8)  // 浅紫色
        "娱乐" -> Color(0xFF80CBC4)  // 浅青色
        "购物" -> Color(0xFFF48FB1)  // 浅粉色
        "社交" -> Color(0xFFFFCC80)  // 浅橘色
        else -> Color(0xFFB0BEC5)    // 浅灰色
    }

// 扩展属性：为 Category 提供 emoji 表示
private val Category.emoji: String
    get() = when (this.name) {
        "工作" -> "💼"
        "学习" -> "📚"
        "生活" -> "🏠"
        "健康" -> "❤️"
        "个人" -> "👤"
        else -> "⭕"
    }

@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BgPrimary),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Tab 切换
            item {
                StatsTabRow(
                    selectedTab = uiState.selectedTab,
                    onTabSelected = { viewModel.selectTab(it) }
                )
            }

            // 根据选中的 Tab 显示不同内容
            when (uiState.selectedTab) {
                StatsTab.OVERVIEW -> {
                    item { OverviewContent(uiState, viewModel) }
                }
                StatsTab.CATEGORY -> {
                    item { CategoryContent(uiState, viewModel) }
                }
                StatsTab.TREND -> {
                    item { TrendContent(uiState, viewModel) }
                }
                StatsTab.EFFICIENCY -> {
                    item { EfficiencyContent(uiState) }
                }
            }

            // 最后更新时间
            item {
                Text(
                    text = "最后更新于: ${uiState.lastUpdateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"))}",
                    fontSize = 12.sp,
                    color = TextMuted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        // 任务列表弹窗
        if (uiState.showTaskListSheet) {
            TaskListBottomSheet(
                taskListType = uiState.taskListType!!,
                tasks = uiState.filteredTasks,
                timeRange = uiState.selectedOverviewTimeRange,
                onDismiss = { viewModel.hideTaskList() }
            )
        }
    }
}

@Composable
private fun StatsTabRow(
    selectedTab: StatsTab,
    onTabSelected: (StatsTab) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            StatsTab.values().forEach { tab ->
                val isSelected = selectedTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) Primary else Color.Transparent
                        )
                        .clickable { onTabSelected(tab) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.title,
                        color = if (isSelected) Color.White else TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

// ==================== 概览页面 ====================
@Composable
private fun OverviewContent(uiState: StatsUiState, viewModel: StatsViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 新增：智能洞察卡片
        if (uiState.insights.isNotEmpty()) {
            SmartInsightsCard(
                insights = uiState.insights,
                selectedTimeRange = uiState.selectedOverviewTimeRange,
                onTimeRangeSelected = { viewModel.selectOverviewTimeRange(it) }
            )
        }

        // 核心指标卡片
        CoreMetricsCards(uiState, viewModel)

        // 新增：本周vs上周对比卡片
        uiState.weekComparison?.let { comparison ->
            WeekComparisonCard(
                comparison = comparison,
                timeRange = uiState.selectedOverviewTimeRange
            )
        }

        // 完成率进度条
        CompletionProgressCard(uiState)
    }
}

@Composable
private fun CoreMetricsCards(uiState: StatsUiState, viewModel: StatsViewModel) {
    // 进度指标的标题和图标（根据时间维度动态调整）
    val (progressTitle, progressIcon) = if (uiState.coreMetricProgressType == "count") {
        "已完成" to "✅"
    } else {
        "完成率" to "📊"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MetricCard(
            title = "待办任务",
            value = uiState.coreMetricPending.toString(),
            icon = "📋",
            color = Primary,
            modifier = Modifier.weight(1f),
            onClick = { viewModel.showTaskList(com.example.nextthingb1.presentation.screens.stats.TaskListType.PENDING) }
        )
        MetricCard(
            title = "重要紧急",
            value = uiState.coreMetricImportantUrgent.toString(),
            icon = "🔥",
            color = Color(0xFFEF5350),
            modifier = Modifier.weight(1f),
            onClick = { viewModel.showTaskList(com.example.nextthingb1.presentation.screens.stats.TaskListType.IMPORTANT_URGENT) }
        )
        MetricCard(
            title = "逾期任务",
            value = uiState.coreMetricOverdue.toString(),
            icon = "⏰",
            color = Color(0xFFFF9800),
            modifier = Modifier.weight(1f),
            onClick = { viewModel.showTaskList(com.example.nextthingb1.presentation.screens.stats.TaskListType.OVERDUE) }
        )
        MetricCard(
            title = progressTitle,
            value = uiState.coreMetricProgress,
            icon = progressIcon,
            color = Success,
            modifier = Modifier.weight(1f),
            onClick = { viewModel.showTaskList(com.example.nextthingb1.presentation.screens.stats.TaskListType.COMPLETED) }
        )
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    icon: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .height(160.dp)
            .clickable(onClick = onClick), // 添加点击事件
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(color, color.copy(alpha = 0.7f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    fontSize = 22.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 固定标题区域高度
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    lineHeight = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 固定数值区域高度
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun CompletionProgressCard(uiState: StatsUiState) {
    // 根据时间维度显示标题
    val timeRangeText = when (uiState.selectedOverviewTimeRange) {
        OverviewTimeRange.TODAY -> "今日"
        OverviewTimeRange.THIS_WEEK -> "本周"
        OverviewTimeRange.THIS_MONTH -> "本月"
        OverviewTimeRange.ALL -> "全部"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // 标题带时间维度
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "任务状态分布",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                // 时间维度标签
                Text(
                    text = timeRangeText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Primary,
                    modifier = Modifier
                        .background(
                            Primary.copy(alpha = 0.1f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 进行中进度条（原"未完成"）
            ProgressBarItem(
                label = "进行中",
                count = uiState.pendingTasks,
                total = uiState.totalTasks,
                color = Color(0xFF2196F3)  // 蓝色
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 已完成进度条
            ProgressBarItem(
                label = "已完成",
                count = uiState.completedTasks,
                total = uiState.totalTasks,
                color = Success  // 绿色 #66BB6A
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 延期/暂停进度条
            ProgressBarItem(
                label = "延期/暂停",
                count = uiState.deferredTasks,
                total = uiState.totalTasks,
                color = Color(0xFFFFA726)  // 橙黄色
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 逾期进度条
            ProgressBarItem(
                label = "逾期",
                count = uiState.overdueTasks,
                total = uiState.totalTasks,
                color = Danger  // 红色 #F44336
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 放弃进度条
            ProgressBarItem(
                label = "已放弃",
                count = uiState.cancelledTasks,
                total = uiState.totalTasks,
                color = Color(0xFF9E9E9E)  // 灰色
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 汇总信息
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color(0xFFF5F5F5),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "总计：",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "${uiState.totalTasks}个任务",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "完成率：",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "${String.format("%.1f", uiState.completionRate)}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.completionRate >= 70) Success
                               else if (uiState.completionRate >= 40) Primary
                               else Danger
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressBarItem(
    label: String,
    count: Int,
    total: Int,
    color: Color
) {
    // 计算进度比例
    val progress = if (total > 0) count.toFloat() / total else 0f

    // 动画状态
    var targetProgress by remember { mutableStateOf(0f) }

    // 首次进入时触发动画
    LaunchedEffect(progress) {
        targetProgress = progress
    }

    // 进度条宽度动画
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(
            durationMillis = 1000,  // 1秒动画时长
            easing = FastOutSlowInEasing
        ),
        label = "progressAnimation"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = TextSecondary,
            modifier = Modifier.width(80.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFE0E0E0))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .background(color, RoundedCornerShape(4.dp))
            )
        }

        Text(
            text = "$count",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier
                .width(40.dp)
                .padding(start = 8.dp)
        )
    }
}

@Composable
private fun ImportanceDistributionCard(uiState: StatsUiState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "重要程度分布",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 环形图
            ImportanceDonutChart(
                importantUrgentCount = uiState.importantUrgentCount,
                importantNotUrgentCount = uiState.importantNotUrgentCount,
                notImportantUrgentCount = uiState.notImportantUrgentCount,
                notImportantNotUrgentCount = uiState.notImportantNotUrgentCount,
                modifier = Modifier.size(180.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 图例
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LegendItem("重要紧急", uiState.importantUrgentCount, Color(0xFFF44336))
                    LegendItem("重要不紧急", uiState.importantNotUrgentCount, Color(0xFFFF9800))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LegendItem("不重要紧急", uiState.notImportantUrgentCount, Color(0xFF42A5F5))
                    LegendItem("不重要不紧急", uiState.notImportantNotUrgentCount, Color(0xFF66BB6A))
                }
            }
        }
    }
}

@Composable
private fun ImportanceDonutChart(
    importantUrgentCount: Int,
    importantNotUrgentCount: Int,
    notImportantUrgentCount: Int,
    notImportantNotUrgentCount: Int,
    modifier: Modifier = Modifier
) {
    val total = importantUrgentCount + importantNotUrgentCount + notImportantUrgentCount + notImportantNotUrgentCount
    if (total == 0) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("暂无数据", color = TextMuted, fontSize = 14.sp)
        }
        return
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val radius = minOf(centerX, centerY) * 0.8f
            val strokeWidth = radius * 0.35f

            var startAngle = -90f

            // 重要且紧急 (红色)
            if (importantUrgentCount > 0) {
                val sweepAngle = (importantUrgentCount.toFloat() / total) * 360f
                drawArc(
                    color = Color(0xFFF44336),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    topLeft = Offset(centerX - radius, centerY - radius),
                    size = Size(radius * 2, radius * 2)
                )
                startAngle += sweepAngle
            }

            // 重要但不紧急 (橙色)
            if (importantNotUrgentCount > 0) {
                val sweepAngle = (importantNotUrgentCount.toFloat() / total) * 360f
                drawArc(
                    color = Color(0xFFFF9800),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    topLeft = Offset(centerX - radius, centerY - radius),
                    size = Size(radius * 2, radius * 2)
                )
                startAngle += sweepAngle
            }

            // 不重要但紧急 (蓝色)
            if (notImportantUrgentCount > 0) {
                val sweepAngle = (notImportantUrgentCount.toFloat() / total) * 360f
                drawArc(
                    color = Color(0xFF42A5F5),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    topLeft = Offset(centerX - radius, centerY - radius),
                    size = Size(radius * 2, radius * 2)
                )
                startAngle += sweepAngle
            }

            // 不重要且不紧急 (绿色)
            if (notImportantNotUrgentCount > 0) {
                val sweepAngle = (notImportantNotUrgentCount.toFloat() / total) * 360f
                drawArc(
                    color = Color(0xFF66BB6A),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    topLeft = Offset(centerX - radius, centerY - radius),
                    size = Size(radius * 2, radius * 2)
                )
            }
        }

        // 中心文字
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "总计",
                fontSize = 12.sp,
                color = TextSecondary
            )
            Text(
                text = "$total",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun LegendItem(
    label: String,
    count: Int,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = "$label: $count",
            fontSize = 13.sp,
            color = TextSecondary
        )
    }
}

// ==================== 分类统计页面 ====================
@Composable
private fun CategoryContent(uiState: StatsUiState, viewModel: StatsViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 分类双层饼图（带时间维度选择）
        CategoryDoublePieChart(
            categoryStats = uiState.categoryStats.values.toList(),
            selectedCategory = uiState.selectedCategory,
            onCategorySelected = { category -> viewModel.selectCategory(category) },
            selectedTimeRange = uiState.selectedCategoryTimeRange,
            onTimeRangeSelected = { timeRange -> viewModel.selectCategoryTimeRange(timeRange) }
        )

        // 分类效率排行榜
        if (uiState.categoryEfficiencyRanking.isNotEmpty()) {
            CategoryEfficiencyRanking(
                ranking = uiState.categoryEfficiencyRanking,
                selectedTimeRange = uiState.selectedCategoryTimeRange
            )
        }
    }
}

@Composable
private fun CategoryDistributionChart(uiState: StatsUiState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "分类任务分布",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(20.dp))

            uiState.categoryStats.forEach { (category, stats) ->
                CategoryStatItem(
                    category = category,
                    stats = stats
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun CategoryStatItem(
    category: Category,
    stats: CategoryStatsData
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(category.color),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category.icon,
                        fontSize = 16.sp
                    )
                }
                Text(
                    text = category.displayName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${stats.totalCount} 个",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "${String.format("%.1f", stats.completionRate)}% 完成",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 完成率进度条
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFE0E0E0))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(stats.completionRate / 100f)
                    .background(
                        category.color,
                        RoundedCornerShape(4.dp)
                    )
            )
        }
    }
}

@Composable
private fun CategoryDurationChart(uiState: StatsUiState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "分类平均完成时长",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(20.dp))

            val maxDuration = uiState.completionTimeByCategory.values.maxOrNull() ?: 1.0

            uiState.completionTimeByCategory.forEach { (category, duration) ->
                if (duration > 0) {
                    CategoryDurationItem(
                        category = category,
                        duration = duration,
                        maxDuration = maxDuration
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun CategoryDurationItem(
    category: Category,
    duration: Double,
    maxDuration: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = category.displayName,
            fontSize = 13.sp,
            color = TextSecondary,
            modifier = Modifier.width(50.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFE0E0E0))
        ) {
            val widthFraction = (duration / maxDuration).toFloat()
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(widthFraction)
                    .background(
                        category.color,
                        RoundedCornerShape(12.dp)
                    )
            )
        }

        Text(
            text = "${duration.toInt()}分",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            modifier = Modifier
                .width(50.dp)
                .padding(start = 8.dp)
        )
    }
}

// ==================== 趋势统计页面 ====================
@Composable
private fun TrendContent(
    uiState: StatsUiState,
    viewModel: StatsViewModel
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 周趋势折线图（支持时间范围切换）
        WeeklyTrendChart(uiState, viewModel)

        // 新增：任务积压趋势面积图
        if (uiState.backlogTrend.isNotEmpty()) {
            BacklogTrendAreaChart(
                backlogData = uiState.backlogTrend,
                threshold = uiState.backlogThreshold
            )
        }

        // 新增：月历热力图（GitHub风格） - 固定显示最近3个月
        if (uiState.calendarHeatmap.isNotEmpty()) {
            CalendarHeatmapCard(
                heatmapData = uiState.calendarHeatmap,
                stats = uiState.calendarStats
            )
        }
    }
}

@Composable
private fun WeeklyTrendChart(uiState: StatsUiState, viewModel: StatsViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // 计算趋势
            val trendInfo = if (uiState.weeklyTrend.size >= 2) {
                val recentAvg = uiState.weeklyTrend.takeLast(3).map { it.completedCount }.average()
                val previousAvg = uiState.weeklyTrend.dropLast(3).takeLast(3).map { it.completedCount }.average()

                when {
                    previousAvg == 0.0 -> Triple("→", "平稳", Color(0xFF9E9E9E))
                    recentAvg > previousAvg * 1.1 -> Triple("↗", "上升", Success)
                    recentAvg < previousAvg * 0.9 -> Triple("↘", "下降", Color(0xFFFF5252))
                    else -> Triple("→", "平稳", Color(0xFF9E9E9E))
                }
            } else {
                Triple("→", "平稳", Color(0xFF9E9E9E))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "任务完成趋势",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                // 趋势标签
                Row(
                    modifier = Modifier
                        .background(trendInfo.third.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = trendInfo.first,
                        fontSize = 14.sp,
                        color = trendInfo.third,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = trendInfo.second,
                        fontSize = 12.sp,
                        color = trendInfo.third,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 时间维度选择器（本周/本月/全部）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF5F5F5))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                listOf(
                    OverviewTimeRange.THIS_WEEK,
                    OverviewTimeRange.THIS_MONTH,
                    OverviewTimeRange.ALL
                ).forEach { timeRange ->
                    val isSelected = uiState.selectedTrendTimeRange == timeRange
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isSelected) Primary else Color.White
                            )
                            .clickable { viewModel.selectTrendTimeRange(timeRange) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = timeRange.displayName,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (uiState.weeklyTrend.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无趋势数据", color = TextMuted)
                }
            } else {
                LineChart(
                    data = uiState.weeklyTrend,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 图例
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 创建任务图例
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp, 3.dp)
                                .background(Primary, RoundedCornerShape(1.5.dp))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "创建任务",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    // 完成任务图例
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp, 3.dp)
                                .background(Success, RoundedCornerShape(1.5.dp))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "完成任务",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LineChart(
    data: List<DailyTrendData>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val maxValue = data.maxOf { maxOf(it.createdCount, it.completedCount) }.toFloat()
    if (maxValue == 0f) return

    // 计算纵轴刻度值（向上取整到5的倍数）
    val yAxisMax = ((maxValue / 5).toInt() + 1) * 5

    Row(modifier = modifier) {
        // 纵轴标签
        Column(
            modifier = Modifier
                .width(32.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            for (i in 4 downTo 0) {
                val value = (yAxisMax * i / 4)
                Text(
                    text = value.toString(),
                    fontSize = 10.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }

        // 图表区域
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            // 折线图Canvas
            val primaryColor = Primary
            val successColor = Success
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val pointSpacing = size.width / (data.size - 1).coerceAtLeast(1)
                val heightScale = size.height / yAxisMax

                // 绘制横向网格线
                for (i in 0..4) {
                    val y = size.height - (size.height / 4 * i)
                    drawLine(
                        color = Color(0xFFE0E0E0),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                }

                // 绘制创建任务折线（蓝色）
                val createdPath = Path()
                data.forEachIndexed { index, dayData ->
                    val x = index * pointSpacing
                    val y = size.height - (dayData.createdCount * heightScale)
                    if (index == 0) {
                        createdPath.moveTo(x, y)
                    } else {
                        createdPath.lineTo(x, y)
                    }
                }
                drawPath(
                    path = createdPath,
                    color = primaryColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // 绘制完成任务折线（绿色）
                val completedPath = Path()
                data.forEachIndexed { index, dayData ->
                    val x = index * pointSpacing
                    val y = size.height - (dayData.completedCount * heightScale)
                    if (index == 0) {
                        completedPath.moveTo(x, y)
                    } else {
                        completedPath.lineTo(x, y)
                    }
                }
                drawPath(
                    path = completedPath,
                    color = successColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // 绘制数据点
                data.forEachIndexed { index, dayData ->
                    val x = index * pointSpacing

                    // 创建点
                    val createdY = size.height - (dayData.createdCount * heightScale)
                    drawCircle(
                        color = primaryColor,
                        radius = 4.dp.toPx(),
                        center = Offset(x, createdY)
                    )

                    // 完成点
                    val completedY = size.height - (dayData.completedCount * heightScale)
                    drawCircle(
                        color = successColor,
                        radius = 4.dp.toPx(),
                        center = Offset(x, completedY)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 横轴日期标签
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 根据数据量显示合适数量的日期标签
                val labelsToShow = when {
                    data.size <= 7 -> data.size
                    data.size <= 30 -> 6
                    else -> 5
                }
                val step = (data.size - 1) / (labelsToShow - 1).coerceAtLeast(1)

                for (i in 0 until labelsToShow) {
                    val index = (i * step).coerceAtMost(data.size - 1)
                    val date = data[index].date
                    Text(
                        text = "${date.monthValue}/${date.dayOfMonth}",
                        fontSize = 10.sp,
                        color = TextMuted,
                        modifier = Modifier.weight(1f),
                        textAlign = if (i == 0) androidx.compose.ui.text.style.TextAlign.Start
                        else if (i == labelsToShow - 1) androidx.compose.ui.text.style.TextAlign.End
                        else androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateVsCompleteTrendCard(uiState: StatsUiState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "创建 vs 完成对比",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TrendLegendItem("创建", Primary)
                TrendLegendItem("完成", Success)
            }

            Spacer(modifier = Modifier.height(16.dp))

            val totalCreated = uiState.weeklyTrend.sumOf { it.createdCount }
            val totalCompleted = uiState.weeklyTrend.sumOf { it.completedCount }
            val avgCompletionRate = if (uiState.weeklyTrend.isNotEmpty())
                uiState.weeklyTrend.map { it.completionRate }.average()
            else 0.0

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$totalCreated",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                    Text(
                        text = "总创建",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$totalCompleted",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Success
                    )
                    Text(
                        text = "总完成",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${String.format("%.1f", avgCompletionRate)}%",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFAB47BC)
                    )
                    Text(
                        text = "平均完成率",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendLegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(color)
        )
        Text(
            text = label,
            fontSize = 13.sp,
            color = TextSecondary
        )
    }
}

// ==================== 效率统计页面 ====================
@Composable
private fun EfficiencyContent(uiState: StatsUiState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. 效率雷达图（核心卡片）- 六维度评分
        uiState.procrastinationRadar?.let {
            ProcrastinationRadarCard(radarData = it)
        }

        // 2. 黄金时段热力图 - 最佳工作时间
        if (uiState.timeHeatmap.isNotEmpty()) {
            TimeHeatmapCard(
                heatmapData = uiState.timeHeatmap,
                stats = uiState.timeHeatmapStats
            )
        }

        // 3. 任务完成漏斗 - 流程健康度
        uiState.taskFunnel?.let {
            TaskFunnelCard(funnelData = it)
        }
    }
}

@Composable
private fun OnTimeCompletionCard(uiState: StatsUiState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "完成及时率",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 双环图
            DualRingChart(
                onTimeRate = uiState.onTimeCompletionRate,
                overdueRate = uiState.overdueCompletionRate,
                modifier = Modifier.size(180.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Success)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "准时完成",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                    Text(
                        text = "${String.format("%.1f", uiState.onTimeCompletionRate)}%",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Success
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Danger)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "逾期完成",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                    Text(
                        text = "${String.format("%.1f", uiState.overdueCompletionRate)}%",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Danger
                    )
                }
            }
        }
    }
}

@Composable
private fun DualRingChart(
    onTimeRate: Float,
    overdueRate: Float,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val outerRadius = minOf(centerX, centerY) * 0.8f
            val innerRadius = outerRadius * 0.6f
            val strokeWidth = (outerRadius - innerRadius) / 2

            // 外环 (准时完成)
            drawArc(
                color = Color(0xFFE0E0E0),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth * 1.5f),
                topLeft = Offset(centerX - outerRadius, centerY - outerRadius),
                size = Size(outerRadius * 2, outerRadius * 2)
            )

            val onTimeSweep = (onTimeRate / 100f) * 360f
            drawArc(
                color = Color(0xFF66BB6A),
                startAngle = -90f,
                sweepAngle = onTimeSweep,
                useCenter = false,
                style = Stroke(width = strokeWidth * 1.5f, cap = StrokeCap.Round),
                topLeft = Offset(centerX - outerRadius, centerY - outerRadius),
                size = Size(outerRadius * 2, outerRadius * 2)
            )

            // 内环 (逾期完成)
            drawArc(
                color = Color(0xFFE0E0E0),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth),
                topLeft = Offset(centerX - innerRadius, centerY - innerRadius),
                size = Size(innerRadius * 2, innerRadius * 2)
            )

            val overdueSweep = (overdueRate / 100f) * 360f
            drawArc(
                color = Color(0xFFFFA726),
                startAngle = -90f,
                sweepAngle = overdueSweep,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(centerX - innerRadius, centerY - innerRadius),
                size = Size(innerRadius * 2, innerRadius * 2)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "效率",
                fontSize = 12.sp,
                color = TextSecondary
            )
            Text(
                text = if (onTimeRate > 70) "优秀" else if (onTimeRate > 50) "良好" else "待提升",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (onTimeRate > 70) Success else if (onTimeRate > 50) Primary else Danger
            )
        }
    }
}

@Composable
private fun ImportanceDurationCard(uiState: StatsUiState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "重要程度完成时长分析",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(20.dp))

            val maxDuration = uiState.completionTimeByImportance.values.maxOrNull() ?: 1.0

            // 显示四个象限的完成时长
            val importanceData = listOf(
                Triple("重要紧急", uiState.completionTimeByImportance[com.example.nextthingb1.domain.model.TaskImportanceUrgency.IMPORTANT_URGENT] ?: 0.0, Color(0xFFF44336)),
                Triple("重要不紧急", uiState.completionTimeByImportance[com.example.nextthingb1.domain.model.TaskImportanceUrgency.IMPORTANT_NOT_URGENT] ?: 0.0, Color(0xFFFF9800)),
                Triple("不重要紧急", uiState.completionTimeByImportance[com.example.nextthingb1.domain.model.TaskImportanceUrgency.NOT_IMPORTANT_URGENT] ?: 0.0, Color(0xFF42A5F5)),
                Triple("不重要不紧急", uiState.completionTimeByImportance[com.example.nextthingb1.domain.model.TaskImportanceUrgency.NOT_IMPORTANT_NOT_URGENT] ?: 0.0, Color(0xFF66BB6A))
            )

            importanceData.forEach { (label, duration, color) ->
                if (duration > 0) {
                    ImportanceDurationItem(
                        label = label,
                        duration = duration,
                        maxDuration = maxDuration,
                        color = color
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun ImportanceDurationItem(
    label: String,
    duration: Double,
    maxDuration: Double,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = TextSecondary,
            modifier = Modifier.width(80.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFE0E0E0))
        ) {
            val widthFraction = (duration / maxDuration).toFloat()
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(widthFraction)
                    .background(color, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (widthFraction > 0.3f) {
                    Text(
                        text = "${duration.toInt()}分",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
            }
        }

        if ((duration / maxDuration).toFloat() <= 0.3f) {
            Text(
                text = "${duration.toInt()}分",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                modifier = Modifier
                    .width(50.dp)
                    .padding(start = 8.dp)
            )
        } else {
            Spacer(modifier = Modifier.width(50.dp))
        }
    }
}

// ==================== 新增：智能洞察卡片 ====================
@Composable
private fun SmartInsightsCard(
    insights: List<com.example.nextthingb1.presentation.screens.stats.InsightData>,
    selectedTimeRange: com.example.nextthingb1.presentation.screens.stats.OverviewTimeRange,
    onTimeRangeSelected: (com.example.nextthingb1.presentation.screens.stats.OverviewTimeRange) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = BgCard
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 标题和时间维度选择器
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "智能洞察",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                // 时间维度选择器
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF5F5F5))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    com.example.nextthingb1.presentation.screens.stats.OverviewTimeRange.values().forEach { timeRange ->
                        val isSelected = selectedTimeRange == timeRange
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isSelected) Primary else Color.White
                                )
                                .clickable { onTimeRangeSelected(timeRange) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = timeRange.displayName,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            insights.forEach { insight ->
                InsightRow(insight = insight)
            }
        }
    }
}

@Composable
private fun InsightRow(insight: com.example.nextthingb1.presentation.screens.stats.InsightData) {
    val backgroundColor = when (insight.type) {
        com.example.nextthingb1.presentation.screens.stats.InsightType.POSITIVE -> Color(0xFFE8F5E9)
        com.example.nextthingb1.presentation.screens.stats.InsightType.WARNING -> Color(0xFFFFF3E0)
        com.example.nextthingb1.presentation.screens.stats.InsightType.ALERT -> Color(0xFFFFEBEE)
    }

    val iconColor = when (insight.type) {
        com.example.nextthingb1.presentation.screens.stats.InsightType.POSITIVE -> Success
        com.example.nextthingb1.presentation.screens.stats.InsightType.WARNING -> Color(0xFFFF9800)
        com.example.nextthingb1.presentation.screens.stats.InsightType.ALERT -> Danger
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = insight.icon,
            fontSize = 20.sp,
            modifier = Modifier.padding(end = 10.dp)
        )

        Text(
            text = insight.message,
            fontSize = 14.sp,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}

// ==================== 新增：任务健康度仪表盘 ====================
@Composable
private fun HealthScoreGaugeCard(
    healthScore: Int,
    healthLevel: com.example.nextthingb1.presentation.screens.stats.HealthLevel
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "任务健康度",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 半圆仪表盘
            SemiCircleGauge(
                score = healthScore,
                level = healthLevel,
                modifier = Modifier.size(200.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 健康度说明
            Text(
                text = healthLevel.displayName,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = when (healthLevel) {
                    com.example.nextthingb1.presentation.screens.stats.HealthLevel.EXCELLENT -> Success
                    com.example.nextthingb1.presentation.screens.stats.HealthLevel.GOOD -> Primary
                    com.example.nextthingb1.presentation.screens.stats.HealthLevel.AVERAGE -> Color(0xFFFF9800)
                    com.example.nextthingb1.presentation.screens.stats.HealthLevel.POOR -> Danger
                }
            )

            Text(
                text = "综合得分：$healthScore/100",
                fontSize = 14.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun SemiCircleGauge(
    score: Int,
    level: com.example.nextthingb1.presentation.screens.stats.HealthLevel,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val textPrimaryColor = TextPrimary
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height * 0.85f
            val radius = minOf(centerX, centerY) * 0.9f
            val strokeWidth = radius * 0.2f

            // 背景弧线（灰色）
            drawArc(
                color = Color(0xFFE0E0E0),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(centerX - radius, centerY - radius),
                size = Size(radius * 2, radius * 2)
            )

            // 分数弧线（彩色）
            val sweepAngle = (score / 100f) * 180f
            val gaugeColor = when {
                score >= 85 -> Color(0xFF66BB6A)  // 绿色
                score >= 70 -> Color(0xFF42A5F5)  // 蓝色
                score >= 50 -> Color(0xFFFF9800)  // 橙色
                else -> Color(0xFFF44336)         // 红色
            }

            drawArc(
                color = gaugeColor,
                startAngle = 180f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(centerX - radius, centerY - radius),
                size = Size(radius * 2, radius * 2)
            )

            // 指针
            val angle = 180f + sweepAngle
            val radian = Math.toRadians(angle.toDouble())
            val pointerLength = radius * 0.8f
            val endX = centerX + (pointerLength * cos(radian)).toFloat()
            val endY = centerY + (pointerLength * sin(radian)).toFloat()

            drawLine(
                color = textPrimaryColor,
                start = Offset(centerX, centerY),
                end = Offset(endX, endY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )

            // 中心圆点
            drawCircle(
                color = textPrimaryColor,
                radius = 6.dp.toPx(),
                center = Offset(centerX, centerY)
            )
        }
    }
}

// ==================== 新增：本周vs上周对比卡片 ====================
@Composable
private fun WeekComparisonCard(
    comparison: com.example.nextthingb1.presentation.screens.stats.WeekComparisonData,
    timeRange: com.example.nextthingb1.presentation.screens.stats.OverviewTimeRange
) {
    // 根据时间维度确定标题和标签
    val (title, currentLabel, previousLabel) = when (timeRange) {
        com.example.nextthingb1.presentation.screens.stats.OverviewTimeRange.TODAY ->
            Triple("今日 vs 昨日", "今", "昨")
        com.example.nextthingb1.presentation.screens.stats.OverviewTimeRange.THIS_WEEK ->
            Triple("本周 vs 上周", "本周", "上周")
        com.example.nextthingb1.presentation.screens.stats.OverviewTimeRange.THIS_MONTH ->
            Triple("本月 vs 上月", "本月", "上月")
        com.example.nextthingb1.presentation.screens.stats.OverviewTimeRange.ALL ->
            Triple("全部数据", "全部", "全部") // 不应该显示
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 5组柱状图
            GroupedBarChart(
                previousLabel = previousLabel,
                currentLabel = currentLabel,
                groups = listOf(
                    BarGroupData(
                        label = "任务总数",
                        previousValue = comparison.lastWeekTotalTasks,
                        currentValue = comparison.thisWeekTotalTasks,
                        unit = "",
                        isRateData = false
                    ),
                    BarGroupData(
                        label = "延期任务",
                        previousValue = comparison.lastWeekDelayedTasks,
                        currentValue = comparison.thisWeekDelayedTasks,
                        unit = "",
                        isRateData = false
                    ),
                    BarGroupData(
                        label = "逾期任务",
                        previousValue = comparison.lastWeekOverdueTasks,
                        currentValue = comparison.thisWeekOverdueTasks,
                        unit = "",
                        isRateData = false
                    ),
                    BarGroupData(
                        label = "放弃任务",
                        previousValue = comparison.lastWeekCancelledTasks,
                        currentValue = comparison.thisWeekCancelledTasks,
                        unit = "",
                        isRateData = false
                    ),
                    BarGroupData(
                        label = "完成率",
                        previousValue = comparison.lastWeekCompletionRate.toInt(),
                        currentValue = comparison.thisWeekCompletionRate.toInt(),
                        unit = "%",
                        isRateData = true  // 标记为百分比数据，独立计算比例
                    )
                )
            )
        }
    }
}

/**
 * 柱状图分组数据
 */
private data class BarGroupData(
    val label: String,
    val previousValue: Int,
    val currentValue: Int,
    val unit: String,
    val isRateData: Boolean
)

/**
 * 分组柱状图 - 5组，每组2个柱子紧挨着
 */
@Composable
private fun GroupedBarChart(
    previousLabel: String,
    currentLabel: String,
    groups: List<BarGroupData>
) {
    // 定义柱状图显示区域的总高度（保持不变）
    val containerHeight = 140.dp

    // 最大柱子高度 = 容器高度的80%，为112dp
    val maxBarHeight = containerHeight * 0.8f

    // 第一组：任务数量类（非百分比数据），找出所有任务数量的最大值
    val taskCountMaxValue = groups
        .filter { !it.isRateData }
        .flatMap { listOf(it.previousValue, it.currentValue) }
        .maxOrNull()?.coerceAtLeast(1) ?: 1

    // 第二组：完成率类（百分比数据），找出完成率的最大值
    val rateMaxValue = groups
        .filter { it.isRateData }
        .flatMap { listOf(it.previousValue, it.currentValue) }
        .maxOrNull()?.coerceAtLeast(1) ?: 1

    Column {
        // 柱状图区域
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(containerHeight),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            groups.forEach { group ->
                // 根据数据类型选择对应的最大值
                val maxValue = if (group.isRateData) rateMaxValue else taskCountMaxValue

                BarGroup(
                    label = group.label,
                    previousValue = group.previousValue,
                    currentValue = group.currentValue,
                    unit = group.unit,
                    maxValue = maxValue,  // 传入对应组的最大值
                    maxBarHeight = maxBarHeight,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 图例
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 昨日图例
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color(0xFFBDBDBD), RoundedCornerShape(2.dp))
                )
                Text(
                    text = previousLabel,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 今日图例
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Primary, RoundedCornerShape(2.dp))
                )
                Text(
                    text = currentLabel,
                    fontSize = 12.sp,
                    color = TextPrimary
                )
            }
        }
    }
}

/**
 * 单组柱状图 - 两个柱子紧挨着
 */
@Composable
private fun BarGroup(
    label: String,
    previousValue: Int,
    currentValue: Int,
    unit: String,
    maxValue: Int,  // 使用传入的组最大值
    maxBarHeight: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    // 按照传入的最大值计算柱子高度
    val previousBarHeight = if (maxValue > 0) maxBarHeight * (previousValue.toFloat() / maxValue) else 0.dp
    val currentBarHeight = if (maxValue > 0) maxBarHeight * (currentValue.toFloat() / maxValue) else 0.dp

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 两个柱子紧挨着
        Row(
            modifier = Modifier.height(maxBarHeight),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // 昨日柱子
            SingleBar(
                value = previousValue,
                unit = unit,
                barHeight = previousBarHeight,
                barColor = Color(0xFFBDBDBD),
                barWidth = 20.dp
            )

            // 今日柱子
            SingleBar(
                value = currentValue,
                unit = unit,
                barHeight = currentBarHeight,
                barColor = Primary,
                barWidth = 20.dp
            )
        }

        // 标签
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 6.dp),
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

/**
 * 单个柱子
 */
@Composable
private fun SingleBar(
    value: Int,
    unit: String,
    barHeight: androidx.compose.ui.unit.Dp,
    barColor: Color,
    barWidth: androidx.compose.ui.unit.Dp
) {
    // 记录目标高度，初始为0，用于触发首次动画
    var targetHeight by remember { mutableStateOf(0.dp) }

    // 首次进入时，延迟触发动画
    LaunchedEffect(barHeight) {
        targetHeight = barHeight
    }

    // 动画：柱子从0高度增长到实际高度
    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = tween(
            durationMillis = 800,  // 动画时长800ms
            easing = FastOutSlowInEasing  // 快进慢出的缓动曲线
        ),
        label = "barHeightAnimation"
    )

    // 数值透明度动画（延迟显示）
    val animatedAlpha by animateFloatAsState(
        targetValue = if (targetHeight > 0.dp) 1f else 0f,
        animationSpec = tween(
            durationMillis = 400,
            delayMillis = 600,  // 延迟600ms后开始
            easing = LinearEasing
        ),
        label = "valueAlphaAnimation"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (value == 0) {
            // 没有数据时显示"暂无"，位置靠近底部
            Box(
                modifier = Modifier.height(112.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    text = "暂无",
                    fontSize = 11.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(bottom = 0.dp)  // 贴近底部
                )
            }
        } else {
            // 有数据时，始终显示数值（带淡入动画）
            Text(
                text = "$value$unit",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary.copy(alpha = animatedAlpha),
                modifier = Modifier.padding(bottom = 2.dp)
            )

            // 柱子（带高度增长动画）
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(animatedHeight.coerceAtLeast(0.dp))
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                barColor.copy(alpha = 0.8f),
                                barColor
                            )
                        )
                    )
            )
        }
    }
}

/**
 * 分类双层饼图（带动画）
 */
@Composable
private fun CategoryDoublePieChart(
    categoryStats: List<CategoryStatsData>,
    selectedCategory: Category?,
    onCategorySelected: (Category?) -> Unit,
    selectedTimeRange: OverviewTimeRange,
    onTimeRangeSelected: (OverviewTimeRange) -> Unit
) {
    if (categoryStats.isEmpty()) return

    // 使用 Animatable 控制动画进度
    val animatedProgress = remember { Animatable(0f) }

    // 创建一个key来标识数据变化（包括数据内容和选中状态）
    val dataKey = remember(categoryStats, selectedCategory) {
        categoryStats.hashCode() + (selectedCategory?.id?.hashCode() ?: 0)
    }

    // 监听数据变化、时间维度变化、选中分类变化，触发动画
    LaunchedEffect(dataKey) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing
            )
        )
    }

    // 中心文字淡入动画
    val textAlpha by animateFloatAsState(
        targetValue = if (animatedProgress.value > 0.1f) 1f else 0f,
        animationSpec = tween(
            durationMillis = 400,
            delayMillis = 200,
            easing = LinearEasing
        ),
        label = "textAlphaAnimation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题和时间维度选择器
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "分类任务分布",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                // 时间维度选择器
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF5F5F5))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    OverviewTimeRange.values().forEach { timeRange ->
                        val isSelected = selectedTimeRange == timeRange
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isSelected) Primary else Color.White
                                )
                                .clickable { onTimeRangeSelected(timeRange) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = timeRange.displayName,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 饼图绘制区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .size(240.dp)
                        .pointerInput(selectedCategory, categoryStats) {
                            detectTapGestures { offset ->
                                val canvasSize = size.width
                                val centerX = canvasSize / 2f
                                val centerY = canvasSize / 2f

                                val dx = offset.x - centerX
                                val dy = offset.y - centerY
                                val distance = sqrt(dx * dx + dy * dy)

                                val outerRadius = canvasSize * 0.4f
                                val innerRadius = canvasSize * 0.2f

                                if (distance in innerRadius..outerRadius) {
                                    var angle = atan2(dy.toDouble(), dx.toDouble()).toFloat() * 180f / PI.toFloat()
                                    if (angle < 0) angle += 360f
                                    angle = (angle + 90f) % 360f

                                    var currentAngle = 0f
                                    val total = categoryStats.sumOf { it.totalCount }

                                    for (stat in categoryStats) {
                                        val sweepAngle = (stat.totalCount.toFloat() / total) * 360f
                                        if (angle >= currentAngle && angle < currentAngle + sweepAngle) {
                                            if (selectedCategory == stat.category) {
                                                onCategorySelected(null)
                                            } else {
                                                onCategorySelected(stat.category)
                                            }
                                            break
                                        }
                                        currentAngle += sweepAngle
                                    }
                                }
                            }
                        }
                ) {
                    val canvasSize = size.width
                    val centerX = canvasSize / 2f
                    val centerY = canvasSize / 2f

                    val outerRadius = canvasSize * 0.4f
                    val innerRadius = canvasSize * 0.2f

                    val total = categoryStats.sumOf { it.totalCount }
                    var cumulativeAngle = 0f
                    val currentProgress = animatedProgress.value

                    if (selectedCategory == null) {
                        // 外圈：显示分类分布（顺序绘制动画）
                        categoryStats.forEach { stat ->
                            val targetSweepAngle = (stat.totalCount.toFloat() / total) * 360f
                            val proportion = stat.totalCount.toFloat() / total

                            // 计算该扇形的绘制进度区间
                            val startProgress = cumulativeAngle / 360f
                            val endProgress = (cumulativeAngle + targetSweepAngle) / 360f

                            // 计算当前应该绘制的角度
                            val animatedSweepAngle = when {
                                currentProgress <= startProgress -> 0f
                                currentProgress >= endProgress -> targetSweepAngle
                                else -> {
                                    // 在该扇形的绘制区间内，按比例绘制
                                    val segmentProgress = (currentProgress - startProgress) / (endProgress - startProgress)
                                    targetSweepAngle * segmentProgress
                                }
                            }

                            if (animatedSweepAngle > 0f) {
                                drawArc(
                                    color = stat.category.pastelColor,
                                    startAngle = -90f + cumulativeAngle,
                                    sweepAngle = animatedSweepAngle,
                                    useCenter = false,
                                    topLeft = Offset(
                                        centerX - outerRadius,
                                        centerY - outerRadius
                                    ),
                                    size = Size(outerRadius * 2, outerRadius * 2),
                                    style = Stroke(width = (outerRadius - innerRadius))
                                )
                            }

                            cumulativeAngle += targetSweepAngle
                        }
                    } else {
                        // 显示选中分类的状态分布（顺序绘制动画）
                        val selectedStat = categoryStats.find { it.category == selectedCategory }
                        selectedStat?.let { stat ->
                            val statusData = listOf(
                                Triple("已完成", stat.completedCount, Color(0xFF4CAF50)),
                                Triple("进行中", stat.pendingCount, Color(0xFF2196F3)),
                                Triple("已逾期", stat.overdueCount, Color(0xFFF44336)),
                                Triple("已取消", stat.cancelledCount, Color(0xFF9E9E9E))
                            ).filter { it.second > 0 }

                            val statusTotal = statusData.sumOf { it.second }

                            // 外圈：显示选中分类（整圈动画，使用高亮色）
                            val outerCircleAngle = 360f * currentProgress
                            drawArc(
                                color = stat.category.pastelColor.copy(alpha = 0.6f),
                                startAngle = -90f,
                                sweepAngle = outerCircleAngle,
                                useCenter = false,
                                topLeft = Offset(
                                    centerX - outerRadius,
                                    centerY - outerRadius
                                ),
                                size = Size(outerRadius * 2, outerRadius * 2),
                                style = Stroke(width = (outerRadius - innerRadius) * 0.4f)
                            )

                            // 内圈：显示状态分布（顺序绘制动画）
                            var statusCumulativeAngle = 0f
                            statusData.forEach { (_, count, color) ->
                                val targetSweepAngle = (count.toFloat() / statusTotal) * 360f

                                // 计算该扇形的绘制进度区间
                                val startProgress = statusCumulativeAngle / 360f
                                val endProgress = (statusCumulativeAngle + targetSweepAngle) / 360f

                                // 计算当前应该绘制的角度
                                val animatedSweepAngle = when {
                                    currentProgress <= startProgress -> 0f
                                    currentProgress >= endProgress -> targetSweepAngle
                                    else -> {
                                        val segmentProgress = (currentProgress - startProgress) / (endProgress - startProgress)
                                        targetSweepAngle * segmentProgress
                                    }
                                }

                                if (animatedSweepAngle > 0f) {
                                    drawArc(
                                        color = color,
                                        startAngle = -90f + statusCumulativeAngle,
                                        sweepAngle = animatedSweepAngle,
                                        useCenter = false,
                                        topLeft = Offset(
                                            centerX - innerRadius * 1.8f,
                                            centerY - innerRadius * 1.8f
                                        ),
                                        size = Size(innerRadius * 3.6f, innerRadius * 3.6f),
                                        style = Stroke(width = innerRadius * 0.8f)
                                    )
                                }

                                statusCumulativeAngle += targetSweepAngle
                            }
                        }
                    }
                }

                // 中心文字（带淡入动画）
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.alpha(textAlpha)
                ) {
                    if (selectedCategory != null) {
                        Text(
                            text = selectedCategory.emoji,
                            fontSize = 32.sp
                        )
                        Text(
                            text = selectedCategory.displayName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        val selectedStat = categoryStats.find { it.category == selectedCategory }
                        selectedStat?.let {
                            Text(
                                text = "${it.totalCount}个任务",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                        }
                    } else {
                        Text(
                            text = "总计",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${categoryStats.sumOf { it.totalCount }}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 图例/详情列表
            if (selectedCategory == null) {
                // 分类列表
                categoryStats.forEach { stat ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(stat.category.pastelColor.copy(alpha = 0.08f))
                            .clickable { onCategorySelected(stat.category) }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(stat.category.pastelColor, CircleShape)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "${stat.category.emoji} ${stat.category.displayName}",
                            fontSize = 14.sp,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = "${stat.totalCount}个",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )

                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_info_details),
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                // 状态详情
                val selectedStat = categoryStats.find { it.category == selectedCategory }
                selectedStat?.let { stat ->
                    listOf(
                        Triple("已完成", stat.completedCount, Color(0xFF4CAF50)),
                        Triple("进行中", stat.pendingCount, Color(0xFF2196F3)),
                        Triple("已逾期", stat.overdueCount, Color(0xFFF44336)),
                        Triple("已取消", stat.cancelledCount, Color(0xFF9E9E9E))
                    ).forEach { (label, count, color) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(color, CircleShape)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = label,
                                fontSize = 14.sp,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                text = "$count 个 (${String.format("%.1f", count.toFloat() / stat.totalCount * 100)}%)",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { onCategorySelected(null) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Primary.copy(alpha = 0.1f),
                            contentColor = Primary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("返回总览", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

/**
 * 分类效率排行榜
 */
@Composable
private fun CategoryEfficiencyRanking(
    ranking: List<CategoryEfficiencyData>,
    selectedTimeRange: OverviewTimeRange
) {
    // 效率分说明弹窗状态
    var showEfficiencyExplanation by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题和时间维度
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "分类效率排行榜",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                // 时间维度标签
                Text(
                    text = selectedTimeRange.displayName,
                    color = Primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .background(Primary.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            ranking.forEach { data ->
                val bgColor = when {
                    data.efficiencyScore >= 80 -> Color(0xFFE8F5E9)
                    data.efficiencyScore >= 60 -> Color(0xFFE3F2FD)
                    else -> Color(0xFFF5F5F5)
                }

                val medal = when (data.rank) {
                    1 -> "🥇"
                    2 -> "🥈"
                    3 -> "🥉"
                    else -> ""
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = bgColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 排名/奖牌
                        Box(
                            modifier = Modifier.width(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (medal.isNotEmpty()) {
                                Text(
                                    text = medal,
                                    fontSize = 24.sp
                                )
                            } else {
                                Text(
                                    text = "${data.rank}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // 分类图标和名称
                        Text(
                            text = data.category.emoji,
                            fontSize = 20.sp
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = data.category.displayName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )

                            // 只显示完成率
                            Text(
                                text = "完成率 ${String.format("%.0f", data.completionRate)}%",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        // 效率分数和信息图标
                        Row(
                            modifier = Modifier
                                .clickable { showEfficiencyExplanation = true }
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "${data.efficiencyScore}",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Primary
                                )

                                Text(
                                    text = "效率分",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }

                            // 信息图标
                            Icon(
                                painter = painterResource(id = android.R.drawable.ic_menu_info_details),
                                contentDescription = "效率分说明",
                                tint = Primary.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // 效率分说明底部弹窗
    if (showEfficiencyExplanation) {
        EfficiencyScoreExplanationSheet(
            onDismiss = { showEfficiencyExplanation = false }
        )
    }
}

/**
 * 效率分说明底部弹窗
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EfficiencyScoreExplanationSheet(
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // 标题
            Text(
                text = "效率分计算规则",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 核心理念
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💯",
                        fontSize = 32.sp,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column {
                        Text(
                            text = "满分100分，有延期或放弃就扣分",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "分数越高，说明执行力越强",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 扣分项
            Text(
                text = "扣分项",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 延期扣分
            EfficiencyPenaltyItem(
                icon = "⏰",
                title = "延期/逾期",
                weight = "60%",
                description = "未按时完成的任务",
                color = Danger
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 放弃扣分
            EfficiencyPenaltyItem(
                icon = "🚫",
                title = "放弃任务",
                weight = "40%",
                description = "中途取消的任务",
                color = Color(0xFF9E9E9E)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 计算公式
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "计算公式",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "效率分 = 100 - (延期率×60 + 放弃率×40)",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 评分标准
            Text(
                text = "评分标准",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            ScoreRangeItem("90-100分", "优秀", Color(0xFF4CAF50))
            ScoreRangeItem("75-89分", "良好", Color(0xFF2196F3))
            ScoreRangeItem("60-74分", "一般", Color(0xFFFFA726))
            ScoreRangeItem("0-59分", "待改进", Color(0xFF9E9E9E))
        }
    }
}

/**
 * 效率扣分项
 */
@Composable
private fun EfficiencyPenaltyItem(
    icon: String,
    title: String,
    weight: String,
    description: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标
        Text(
            text = icon,
            fontSize = 24.sp,
            modifier = Modifier.padding(end = 12.dp)
        )

        // 内容
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Text(
                    text = weight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier
                        .background(color, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Text(
                text = description,
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/**
 * 评分范围项
 */
@Composable
private fun ScoreRangeItem(
    range: String,
    label: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, CircleShape)
            )

            Text(
                text = range,
                fontSize = 14.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
        }

        Text(
            text = label,
            fontSize = 14.sp,
            color = TextSecondary
        )
    }
}

/**
 * 分类时间投入热力图
 */
@Composable
private fun CategoryWeekdayHeatmap(
    heatmapData: Map<Category, Map<Int, Int>>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "分类时间投入热力图",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = "查看各分类在一周不同时间的任务完成情况",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 星期标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(modifier = Modifier.width(60.dp))

                listOf("一", "二", "三", "四", "五", "六", "日").forEach { day ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 热力图行
            heatmapData.forEach { (category, weekdayMap) ->
                val maxCount = weekdayMap.values.maxOrNull() ?: 1

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 分类标签
                    Row(
                        modifier = Modifier.width(60.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Text(
                            text = category.emoji,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = category.displayName,
                            fontSize = 11.sp,
                            color = TextPrimary,
                            maxLines = 1
                        )
                    }

                    // 7天的热力方块
                    (1..7).forEach { dayOfWeek ->
                        val count = weekdayMap[dayOfWeek] ?: 0
                        val intensity = if (maxCount > 0) count.toFloat() / maxCount else 0f
                        val alpha = if (count > 0) 0.2f + intensity * 0.8f else 0.1f

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .background(
                                    category.color.copy(alpha = alpha),
                                    RoundedCornerShape(4.dp)
                                )
                                .border(
                                    1.dp,
                                    if (count > 0) category.color.copy(alpha = 0.3f) else Border,
                                    RoundedCornerShape(4.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (count > 0) {
                                Text(
                                    text = "$count",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (intensity > 0.6f) Color.White else TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 图例说明
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "颜色深度 = 完成任务数量",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }
    }
}

// ==================== 趋势Tab新增组件 ====================

/**
 * 时间范围选择器
 */
@Composable
private fun TimeRangeSelector(
    selectedRange: TimeRange,
    onRangeSelected: (TimeRange) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(TimeRange.WEEK_7, TimeRange.DAYS_30, TimeRange.DAYS_90, TimeRange.ALL).forEach { range ->
                val isSelected = selectedRange == range

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .background(
                            if (isSelected) Primary else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            1.dp,
                            if (isSelected) Primary else Border,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onRangeSelected(range) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = range.displayName,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) Color.White else TextSecondary
                    )
                }
            }
        }
    }
}

/**
 * 完成连贯性热力图（GitHub风格）- 固定显示最近3个月
 */
@Composable
private fun CalendarHeatmapCard(
    heatmapData: List<CalendarHeatmapData>,
    stats: CalendarHeatmapStats?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "完成连贯性热力图",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = "可视化你的完成习惯，培养持续行动力（最近3个月）",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // GitHub风格热力图
            GitHubStyleHeatmap(heatmapData)

            // 颜色图例
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "少",
                    fontSize = 11.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.width(4.dp))

                listOf(
                    Color(0xFFEBEDF0),
                    Color(0xFFC6E48B),
                    Color(0xFF7BC96F),
                    Color(0xFF239A3B),
                    Color(0xFF196127)
                ).forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .padding(horizontal = 2.dp)
                            .background(color, RoundedCornerShape(2.dp))
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = "多",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            // 统计信息
            stats?.let {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        icon = "🔥",
                        label = "最长连续",
                        value = "${it.maxStreakDays}天"
                    )

                    StatItem(
                        icon = "❄️",
                        label = "最长中断",
                        value = "${it.maxGapDays}天"
                    )

                    StatItem(
                        icon = "📅",
                        label = "本月完成",
                        value = "${it.currentMonthCompleted}个"
                    )
                }
            }
        }
    }
}

/**
 * GitHub风格热力图组件
 */
@Composable
private fun GitHubStyleHeatmap(heatmapData: List<CalendarHeatmapData>) {
    if (heatmapData.isEmpty()) return

    // 按周分组（每周7天，从周一开始）
    val weeks = mutableListOf<List<CalendarHeatmapData>>()
    val sortedData = heatmapData.sortedBy { it.date }

    // 获取第一天是星期几（1=Monday, 7=Sunday）
    val firstDayOfWeek = sortedData.first().date.dayOfWeek.value

    // 添加前置空白（填充第一周的空缺）
    val leadingEmptyDays = firstDayOfWeek - 1
    var currentWeek = mutableListOf<CalendarHeatmapData?>()
    repeat(leadingEmptyDays) {
        currentWeek.add(null)
    }

    // 填充数据
    sortedData.forEach { data ->
        currentWeek.add(data)
        if (currentWeek.size == 7) {
            weeks.add(currentWeek.filterNotNull())
            currentWeek = mutableListOf()
        }
    }

    // 添加最后一周（如果有剩余）
    if (currentWeek.isNotEmpty()) {
        weeks.add(currentWeek.filterNotNull())
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // 月份标签行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            // 左侧空白（对齐星期标签）
            Spacer(modifier = Modifier.width(24.dp))

            // 月份标签
            val months = mutableListOf<Pair<String, Int>>()
            weeks.forEachIndexed { index, week ->
                if (week.isNotEmpty()) {
                    val month = week.first().date.monthValue
                    if (months.isEmpty() || months.last().first != month.toString()) {
                        months.add(Pair(month.toString(), index))
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                months.forEach { (month, weekIndex) ->
                    val monthName = when (month.toInt()) {
                        1 -> "1月"
                        2 -> "2月"
                        3 -> "3月"
                        4 -> "4月"
                        5 -> "5月"
                        6 -> "6月"
                        7 -> "7月"
                        8 -> "8月"
                        9 -> "9月"
                        10 -> "10月"
                        11 -> "11月"
                        12 -> "12月"
                        else -> ""
                    }

                    Text(
                        text = monthName,
                        fontSize = 10.sp,
                        color = TextMuted,
                        modifier = Modifier.offset(x = (weekIndex * 14).dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 热力图主体
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            // 左侧星期标签
            Column(
                modifier = Modifier.width(24.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                listOf("", "Mon", "", "Wed", "", "Fri", "").forEach { label ->
                    Box(
                        modifier = Modifier.height(12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = label,
                            fontSize = 9.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            // 热力图格子
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // 按列绘制（每列代表一周）
                var weekIndex = 0
                val maxWeeks = 13 // 最多显示13周（约3个月）

                while (weekIndex < weeks.size && weekIndex < maxWeeks) {
                    val week = weeks[weekIndex]
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // 绘制7天
                        for (dayOfWeek in 1..7) {
                            val dayData = week.find { it.date.dayOfWeek.value == dayOfWeek }
                            val color = when (dayData?.level) {
                                0 -> Color(0xFFEBEDF0)
                                1 -> Color(0xFFC6E48B)
                                2 -> Color(0xFF7BC96F)
                                3 -> Color(0xFF239A3B)
                                4 -> Color(0xFF196127)
                                else -> Color(0xFFEBEDF0)
                            }

                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(color, RoundedCornerShape(2.dp))
                            )
                        }
                    }
                    weekIndex++
                }
            }
        }
    }
}

@Composable
private fun StatItem(icon: String, label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = icon,
            fontSize = 24.sp
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextMuted,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

/**
 * 任务积压预警趋势面积图
 */
@Composable
private fun BacklogTrendAreaChart(
    backlogData: List<BacklogTrendData>,
    threshold: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "积压预警趋势",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = "及时发现任务堆积问题，避免失控",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 预警状态卡片
            if (backlogData.isNotEmpty()) {
                val currentBacklog = backlogData.lastOrNull()?.backlogCount ?: 0
                val previousBacklog = if (backlogData.size >= 7) {
                    backlogData[backlogData.size - 7].backlogCount
                } else {
                    currentBacklog
                }
                val backlogChange = currentBacklog - previousBacklog
                val changePercentage = if (previousBacklog > 0) {
                    ((backlogChange.toFloat() / previousBacklog) * 100).toInt()
                } else if (currentBacklog > 0) {
                    100
                } else {
                    0
                }

                val warningLevel = when {
                    currentBacklog >= threshold -> Triple("⚠️ 预警", Color(0xFFFF5252), Color(0xFFFFF3F3))
                    currentBacklog >= threshold * 0.7 -> Triple("⚡ 注意", Color(0xFFFF9800), Color(0xFFFFF8E1))
                    else -> Triple("✅ 健康", Success, Color(0xFFF1F8F4))
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = warningLevel.third)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 当前积压
                        Column {
                            Text(
                                text = "当前积压",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "$currentBacklog",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = warningLevel.second
                                )
                                Text(
                                    text = "个任务",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }

                        // 相比上周
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "相比上周",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = when {
                                        backlogChange > 0 -> "↗"
                                        backlogChange < 0 -> "↘"
                                        else -> "→"
                                    },
                                    fontSize = 16.sp,
                                    color = when {
                                        backlogChange > 0 -> Color(0xFFFF5252)
                                        backlogChange < 0 -> Success
                                        else -> Color(0xFF9E9E9E)
                                    }
                                )
                                Text(
                                    text = if (backlogChange >= 0) "+$backlogChange" else "$backlogChange",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        backlogChange > 0 -> Color(0xFFFF5252)
                                        backlogChange < 0 -> Success
                                        else -> Color(0xFF9E9E9E)
                                    }
                                )
                                if (changePercentage != 0) {
                                    Text(
                                        text = "(${if (changePercentage > 0) "+" else ""}$changePercentage%)",
                                        fontSize = 12.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }

                        // 预警状态
                        Row(
                            modifier = Modifier
                                .background(warningLevel.second.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = warningLevel.first,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = warningLevel.second
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // 面积图
            val primaryColor = Primary
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                if (backlogData.isEmpty()) return@Canvas

                val maxBacklog = backlogData.maxOf { it.backlogCount }.coerceAtLeast(threshold)
                val maxNew = backlogData.maxOf { it.newTasksCount }

                val pointSpacing = size.width / (backlogData.size - 1).coerceAtLeast(1)
                val heightScale = size.height / maxBacklog.toFloat() * 0.85f

                // 绘制预警线
                val thresholdY = size.height - (threshold * heightScale)
                drawLine(
                    color = Color(0xFFFF5252),
                    start = Offset(0f, thresholdY),
                    end = Offset(size.width, thresholdY),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        floatArrayOf(10f, 10f)
                    )
                )

                // 绘制积压面积
                val backlogPath = Path()
                backlogData.forEachIndexed { index, data ->
                    val x = index * pointSpacing
                    val y = size.height - (data.backlogCount * heightScale)

                    if (index == 0) {
                        backlogPath.moveTo(x, size.height)
                        backlogPath.lineTo(x, y)
                    } else {
                        backlogPath.lineTo(x, y)
                    }
                }
                backlogPath.lineTo(size.width, size.height)
                backlogPath.close()

                // 根据积压量调整颜色深浅
                val avgBacklog = backlogData.map { it.backlogCount }.average().toFloat()
                val colorIntensity = (avgBacklog / threshold).coerceIn(0f, 1f)
                val areaColor = if (colorIntensity > 0.8f) {
                    Color(0xFFFF5252).copy(alpha = 0.3f)  // 深红
                } else if (colorIntensity > 0.5f) {
                    Color(0xFFFF9800).copy(alpha = 0.3f)  // 橙色
                } else {
                    Color(0xFFFFC107).copy(alpha = 0.3f)  // 浅橙
                }

                drawPath(
                    path = backlogPath,
                    color = areaColor
                )

                // 绘制积压折线
                val backlogLine = Path()
                backlogData.forEachIndexed { index, data ->
                    val x = index * pointSpacing
                    val y = size.height - (data.backlogCount * heightScale)

                    if (index == 0) {
                        backlogLine.moveTo(x, y)
                    } else {
                        backlogLine.lineTo(x, y)
                    }
                }

                drawPath(
                    path = backlogLine,
                    color = Color(0xFFFF6F00),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // 绘制新增任务虚线
                val newScale = size.height / maxNew.toFloat() * 0.85f
                val newTaskLine = Path()
                backlogData.forEachIndexed { index, data ->
                    val x = index * pointSpacing
                    val y = size.height - (data.newTasksCount * newScale)

                    if (index == 0) {
                        newTaskLine.moveTo(x, y)
                    } else {
                        newTaskLine.lineTo(x, y)
                    }
                }

                drawPath(
                    path = newTaskLine,
                    color = primaryColor,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            floatArrayOf(8f, 8f)
                        )
                    )
                )
            }

            // 图例
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(color = Color(0xFFFF6F00), label = "积压量")
                Spacer(modifier = Modifier.width(16.dp))
                LegendItem(color = Primary, label = "新增任务", isDashed = true)
                Spacer(modifier = Modifier.width(16.dp))
                LegendItem(color = Color(0xFFFF5252), label = "预警线(${threshold}个)", isDashed = true)
            }

            // 当前积压警告
            val currentBacklog = backlogData.lastOrNull()?.backlogCount ?: 0
            if (currentBacklog > threshold) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color(0xFFFF5252).copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚠️",
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "当前积压${currentBacklog}个任务，已超过预警阈值，建议优先处理！",
                        fontSize = 13.sp,
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, isDashed: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(
            modifier = Modifier.size(width = 20.dp, height = 3.dp)
        ) {
            if (isDashed) {
                drawLine(
                    color = color,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = size.height,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        floatArrayOf(4f, 4f)
                    )
                )
            } else {
                drawLine(
                    color = color,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = size.height
                )
            }
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextMuted
        )
    }
}

/**
 * 完成速度加速度柱状图
 */
@Composable
private fun VelocityAccelerationBarChart(
    velocityData: List<VelocityAccelerationData>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "完成速度变化趋势",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = "每周完成数量的增减情况（正值=加速，负值=减速）",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 柱状图
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                if (velocityData.isEmpty()) return@Canvas

                val maxAbsAcceleration = velocityData.maxOf { kotlin.math.abs(it.acceleration) }
                    .coerceAtLeast(1)

                val barWidth = size.width / velocityData.size * 0.7f
                val barSpacing = size.width / velocityData.size
                val centerY = size.height / 2f
                val scale = (size.height / 2f) / maxAbsAcceleration * 0.85f

                // 绘制中心线
                drawLine(
                    color = Color(0xFFE0E0E0),
                    start = Offset(0f, centerY),
                    end = Offset(size.width, centerY),
                    strokeWidth = 2f
                )

                // 绘制柱状图
                velocityData.forEachIndexed { index, data ->
                    val x = index * barSpacing + (barSpacing - barWidth) / 2
                    val barHeight = kotlin.math.abs(data.acceleration) * scale
                    val color = if (data.isAcceleration) Color(0xFF4CAF50) else Color(0xFFF44336)

                    if (data.acceleration >= 0) {
                        // 正值：向上
                        drawRect(
                            color = color,
                            topLeft = Offset(x, centerY - barHeight),
                            size = Size(barWidth, barHeight)
                        )
                    } else {
                        // 负值：向下
                        drawRect(
                            color = color,
                            topLeft = Offset(x, centerY),
                            size = Size(barWidth, barHeight)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 标注最大加速和减速
            val maxAcceleration = velocityData.maxByOrNull { it.acceleration }
            val maxDeceleration = velocityData.minByOrNull { it.acceleration }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                maxAcceleration?.let {
                    if (it.acceleration > 0) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🚀 最大加速",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                            Text(
                                text = it.weekLabel,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF4CAF50)
                            )
                            Text(
                                text = "+${it.acceleration}个",
                                fontSize = 13.sp,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                }

                maxDeceleration?.let {
                    if (it.acceleration < 0) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "📉 最大减速",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                            Text(
                                text = it.weekLabel,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFF44336)
                            )
                            Text(
                                text = "${it.acceleration}个",
                                fontSize = 13.sp,
                                color = Color(0xFFF44336)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== 效率Tab新增组件 ====================

/**
 * 时间热力图（7×6矩阵）
 */
@Composable
private fun TimeHeatmapCard(
    heatmapData: List<TimeHeatmapData>,
    stats: TimeHeatmapStats?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "黄金工作时段分析",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = "发现你的高效时段，优化时间安排",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 星期标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Box(modifier = Modifier.width(50.dp))
                listOf("一", "二", "三", "四", "五", "六", "日").forEach { day ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(text = day, fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 热力图矩阵（6行×7列）
            TimeSlot.values().forEach { timeSlot ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // 时间段标签
                    Box(
                        modifier = Modifier.width(50.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "${timeSlot.emoji} ${timeSlot.displayName}",
                            fontSize = 10.sp,
                            color = TextPrimary
                        )
                    }

                    // 7天的热力方块
                    (1..7).forEach { dayOfWeek ->
                        val data = heatmapData.find {
                            it.dayOfWeek == dayOfWeek && it.timeSlot == timeSlot
                        }

                        val color = when (data?.level ?: 0) {
                            0 -> Color(0xFFECEFF1)  // 灰白
                            1 -> Color(0xFFBBDEFB)  // 浅蓝
                            2 -> Color(0xFF64B5F6)  // 蓝色
                            3 -> Color(0xFF2196F3)  // 深蓝
                            else -> Color(0xFF1565C0)  // 最深蓝
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .background(color, RoundedCornerShape(4.dp))
                                .border(
                                    0.5.dp,
                                    Color(0xFFE0E0E0),
                                    RoundedCornerShape(4.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if ((data?.completedCount ?: 0) > 0) {
                                Text(
                                    text = "${data?.completedCount}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if ((data?.level ?: 0) >= 3) Color.White else TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            // 统计信息
            stats?.let {
                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color(0xFFF5F5F5),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    if (it.mostProductiveSlot != null) {
                        val (day, slot) = it.mostProductiveSlot
                        val dayName = listOf("", "周一", "周二", "周三", "周四", "周五", "周六", "周日")[day]
                        Text(
                            text = "🏆 最高效时段：$dayName ${slot.displayName}（${it.mostProductiveCount}个任务）",
                            fontSize = 13.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "💡 建议：将重要任务安排在高效时段完成",
                        fontSize = 12.sp,
                        color = Primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * 拖延分析雷达图
 */
@Composable
private fun ProcrastinationRadarCard(radarData: ProcrastinationRadarData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "综合效率评分",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = "六维度效率评估，一图看懂综合表现",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 雷达图（简化版：六边形）
            val primaryColor = Primary
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {
                val centerX = size.width / 2f
                val centerY = size.height / 2f
                val radius = size.width.coerceAtMost(size.height) * 0.35f

                val dimensions = listOf(
                    "准时完成" to radarData.onTimeRate,
                    "响应速度" to radarData.responseSpeed,
                    "完成时长" to radarData.completionSpeed,
                    "重要优先" to radarData.importantPriority,
                    "完成稳定" to radarData.completionStability,
                    "目标达成" to radarData.goalAchievementRate
                )

                // 绘制灰色背景六边形（满分标准）
                val bgPath = Path()
                dimensions.forEachIndexed { index, _ ->
                    val angle = (index * 60f - 90f) * (Math.PI / 180).toFloat()
                    val x = centerX + radius * cos(angle)
                    val y = centerY + radius * sin(angle)
                    if (index == 0) bgPath.moveTo(x, y) else bgPath.lineTo(x, y)
                }
                bgPath.close()

                drawPath(
                    path = bgPath,
                    color = Color(0xFFEEEEEE),
                    style = androidx.compose.ui.graphics.drawscope.Fill
                )

                drawPath(
                    path = bgPath,
                    color = Color(0xFFBDBDBD),
                    style = Stroke(width = 1.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f, 5f)))
                )

                // 绘制当前表现六边形（绿色）
                val dataPath = Path()
                dimensions.forEachIndexed { index, (_, value) ->
                    val angle = (index * 60f - 90f) * (Math.PI / 180).toFloat()
                    val distance = radius * (value / 100f)
                    val x = centerX + distance * cos(angle)
                    val y = centerY + distance * sin(angle)
                    if (index == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
                }
                dataPath.close()

                drawPath(
                    path = dataPath,
                    color = Color(0xFF4CAF50).copy(alpha = 0.3f),
                    style = androidx.compose.ui.graphics.drawscope.Fill
                )

                drawPath(
                    path = dataPath,
                    color = Color(0xFF4CAF50),
                    style = Stroke(width = 2.dp.toPx())
                )

                // 绘制标签
                dimensions.forEachIndexed { index, (label, value) ->
                    val angle = (index * 60f - 90f) * (Math.PI / 180).toFloat()
                    val x = centerX + (radius + 30.dp.toPx()) * cos(angle)
                    val y = centerY + (radius + 30.dp.toPx()) * sin(angle)

                    // 绘制圆点
                    drawCircle(
                        color = primaryColor,
                        radius = 4.dp.toPx(),
                        center = Offset(centerX + radius * (value / 100f) * cos(angle), centerY + radius * (value / 100f) * sin(angle))
                    )
                }
            }

            // 维度标签（手动布局）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⏰ 准时", fontSize = 10.sp, color = TextSecondary)
                    Text("${String.format("%.0f", radarData.onTimeRate)}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🚀 响应", fontSize = 10.sp, color = TextSecondary)
                    Text("${String.format("%.0f", radarData.responseSpeed)}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚡ 时长", fontSize = 10.sp, color = TextSecondary)
                    Text("${String.format("%.0f", radarData.completionSpeed)}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔥 优先", fontSize = 10.sp, color = TextSecondary)
                    Text("${String.format("%.0f", radarData.importantPriority)}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📊 稳定", fontSize = 10.sp, color = TextSecondary)
                    Text("${String.format("%.0f", radarData.completionStability)}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎯 目标", fontSize = 10.sp, color = TextSecondary)
                    Text("${String.format("%.0f", radarData.goalAchievementRate)}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 统计信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📈 效率总分", fontSize = 12.sp, color = TextMuted)
                    Text("${radarData.totalScore}/100", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Primary)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏅 效率等级", fontSize = 12.sp, color = TextMuted)
                    Text(radarData.efficiencyGrade, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Primary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("💪 最强项", fontSize = 11.sp, color = TextMuted)
                    Text(radarData.strongestDimension, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF4CAF50))
                    Text("${String.format("%.0f", radarData.strongestScore)}%", fontSize = 11.sp, color = Color(0xFF4CAF50))
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚠️ 待提升", fontSize = 11.sp, color = TextMuted)
                    Text(radarData.weakestDimension, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFFFF9800))
                    Text("${String.format("%.0f", radarData.weakestScore)}%", fontSize = 11.sp, color = Color(0xFFFF9800))
                }
            }
        }
    }
}

/**
 * 延迟成本分析卡片
 */
@Composable
private fun DelayAnalysisCard(delayData: DelayAnalysisData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),  // 浅橙色背景
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "⚠️",
                    fontSize = 24.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "拖延时间成本",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100)
                )
            }

            Text(
                text = "量化拖延造成的时间损失",
                fontSize = 12.sp,
                color = Color(0xFFBF360C),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 三个指标
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "📅",
                        fontSize = 28.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "累计拖延",
                        fontSize = 12.sp,
                        color = Color(0xFF5D4037)
                    )
                    Text(
                        text = "${delayData.totalDelayDays}天",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD32F2F)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "⏰",
                        fontSize = 28.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "平均延迟",
                        fontSize = 12.sp,
                        color = Color(0xFF5D4037)
                    )
                    Text(
                        text = "${String.format("%.1f", delayData.avgDelayPerTask)}天",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD32F2F)
                    )
                }
            }

            if (delayData.mostDelayedDays > 0) {
                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color(0xFFFFCDD2).copy(alpha = 0.5f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = "💸 拖延最严重的任务",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD32F2F)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = delayData.mostDelayedTask,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF5D4037),
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "延迟 ${delayData.mostDelayedDays} 天完成",
                        fontSize = 13.sp,
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * 任务完成漏斗卡片（简化版）
 */
@Composable
private fun TaskFunnelCard(funnelData: TaskFunnelData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "任务流程健康度",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = "从创建到完成的全流程转化分析",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 漏斗阶段
            val stages = listOf(
                "创建任务" to funnelData.totalCreated,
                "设置截止" to funnelData.withDeadline,
                "开始执行" to funnelData.started,
                "最终完成" to funnelData.finalCompleted
            )

            stages.forEachIndexed { index, (label, count) ->
                val percentage = if (funnelData.totalCreated > 0) {
                    (count.toFloat() / funnelData.totalCreated * 100f)
                } else 0f

                val widthFraction = (count.toFloat() / funnelData.totalCreated.coerceAtLeast(1)).coerceIn(0.3f, 1f)

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )

                        Text(
                            text = "$count (${String.format("%.1f", percentage)}%)",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(widthFraction)
                            .height(32.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Primary.copy(alpha = 0.8f),
                                        Primary.copy(alpha = 0.5f)
                                    )
                                ),
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$count 个",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 放弃任务
            if (funnelData.abandoned > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color(0xFFFFEBEE),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "❌", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "放弃任务：${funnelData.abandoned}个",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFD32F2F)
                    )
                }
            }

            // 最大流失环节
            if (funnelData.maxLossRate > 0) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color(0xFFFFF3E0),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "⚠️", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "最大流失环节：${funnelData.maxLossStage}（${String.format("%.1f", funnelData.maxLossRate)}%）",
                        fontSize = 13.sp,
                        color = Color(0xFFE65100),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * 任务列表弹窗 - 显示筛选后的任务列表
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskListBottomSheet(
    taskListType: TaskListType,
    tasks: List<Task>,
    timeRange: OverviewTimeRange,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BgPrimary,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = taskListType.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${timeRange.displayName} · 共 ${tasks.size} 项任务",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // 关闭按钮
                IconButton(onClick = onDismiss) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                        contentDescription = "关闭",
                        tint = TextSecondary
                    )
                }
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = Color(0xFFE0E0E0),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // 任务列表
            if (tasks.isEmpty()) {
                // 空状态
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "📋",
                            fontSize = 48.sp
                        )
                        Text(
                            text = "暂无任务",
                            fontSize = 15.sp,
                            color = TextMuted
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = tasks,
                        key = { task -> task.id }
                    ) { task ->
                        TaskItemCard(
                            task = task,
                            onClick = { /* 任务详情页面未在此实现 */ }
                        )
                    }
                }
            }
        }
    }
}


