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
import com.example.nextthingb1.domain.model.TaskCategory
import com.example.nextthingb1.presentation.theme.*
import java.time.format.DateTimeFormatter
import kotlin.math.*

// 扩展属性：将 TaskCategory 的 colorHex 转换为 Compose Color
private val TaskCategory.color: Color
    get() = Color(android.graphics.Color.parseColor(this.colorHex))

// 扩展属性：为 TaskCategory 提供 emoji 表示
private val TaskCategory.emoji: String
    get() = when (this) {
        TaskCategory.WORK -> "💼"
        TaskCategory.STUDY -> "📚"
        TaskCategory.LIFE -> "🏠"
        TaskCategory.HEALTH -> "❤️"
        TaskCategory.PERSONAL -> "👤"
        TaskCategory.OTHER -> "⭕"
    }

@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
                item { OverviewContent(uiState) }
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
private fun OverviewContent(uiState: StatsUiState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 新增：智能洞察卡片
        if (uiState.insights.isNotEmpty()) {
            SmartInsightsCard(insights = uiState.insights)
        }

        // 核心指标卡片
        CoreMetricsCards(uiState)

        // 新增：本周vs上周对比卡片
        uiState.weekComparison?.let { comparison ->
            WeekComparisonCard(comparison = comparison)
        }

        // 完成率进度条
        CompletionProgressCard(uiState)

        // 重要程度分布环形图
        ImportanceDistributionCard(uiState)
    }
}

@Composable
private fun CoreMetricsCards(uiState: StatsUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "总任务数",
                value = uiState.totalTasks.toString(),
                icon = "📋",
                color = Primary,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "已完成",
                value = uiState.completedTasks.toString(),
                icon = "✅",
                color = Success,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "完成率",
                value = "${String.format("%.1f", uiState.completionRate)}%",
                icon = "📈",
                color = Color(0xFFAB47BC),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "放弃任务",
                value = uiState.cancelledTasks.toString(),
                icon = "🚫",
                color = Color(0xFFFFA726),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    icon: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
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
                    fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CompletionProgressCard(uiState: StatsUiState) {
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
                text = "任务状态分布",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 未完成进度条
            ProgressBarItem(
                label = "未完成",
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

            // 延期进度条
            ProgressBarItem(
                label = "延期",
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
                label = "放弃",
                count = uiState.cancelledTasks,
                total = uiState.totalTasks,
                color = Color(0xFF9E9E9E)  // 灰色
            )
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = TextSecondary,
            modifier = Modifier.width(60.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFE0E0E0))
        ) {
            val progress = if (total > 0) count.toFloat() / total else 0f
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
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
        // 新增：分类双层饼图
        CategoryDoublePieChart(
            categoryStats = uiState.categoryStats.values.toList(),
            selectedCategory = uiState.selectedCategory,
            onCategorySelected = { category -> viewModel.selectCategory(category) }
        )

        // 新增：分类效率排行榜
        if (uiState.categoryEfficiencyRanking.isNotEmpty()) {
            CategoryEfficiencyRanking(ranking = uiState.categoryEfficiencyRanking)
        }

        // 新增：分类时间投入热力图
        if (uiState.categoryWeekdayHeatmap.isNotEmpty()) {
            CategoryWeekdayHeatmap(heatmapData = uiState.categoryWeekdayHeatmap)
        }

        // 保留：分类完成时长对比
        CategoryDurationChart(uiState)
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
    category: TaskCategory,
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
    category: TaskCategory,
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
        // 新增：时间范围选择器
        TimeRangeSelector(
            selectedRange = uiState.selectedTimeRange,
            onRangeSelected = { range -> viewModel.selectTimeRange(range) }
        )

        // 周趋势折线图（支持时间范围切换）
        WeeklyTrendChart(uiState)

        // 新增：月历热力图（GitHub风格）
        if (uiState.calendarHeatmap.isNotEmpty()) {
            CalendarHeatmapCard(
                heatmapData = uiState.calendarHeatmap,
                stats = uiState.calendarStats
            )
        }

        // 新增：任务积压趋势面积图
        if (uiState.backlogTrend.isNotEmpty()) {
            BacklogTrendAreaChart(
                backlogData = uiState.backlogTrend,
                threshold = uiState.backlogThreshold
            )
        }

        // 新增：完成速度加速度柱状图
        if (uiState.velocityAcceleration.isNotEmpty()) {
            VelocityAccelerationBarChart(
                velocityData = uiState.velocityAcceleration
            )
        }

        // 创建 vs 完成对比
        CreateVsCompleteTrendCard(uiState)
    }
}

@Composable
private fun WeeklyTrendChart(uiState: StatsUiState) {
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
            val timeRangeText = when (uiState.selectedTimeRange) {
                TimeRange.WEEK_7 -> "最近7天"
                TimeRange.DAYS_30 -> "最近30天"
                TimeRange.DAYS_90 -> "最近90天"
                TimeRange.ALL -> "全部"
                TimeRange.CUSTOM -> "自定义"
            }
            Text(
                text = "任务完成趋势（$timeRangeText）",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

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
                    color = Primary,
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
                    color = Success,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // 绘制数据点
                data.forEachIndexed { index, dayData ->
                    val x = index * pointSpacing

                    // 创建点
                    val createdY = size.height - (dayData.createdCount * heightScale)
                    drawCircle(
                        color = Primary,
                        radius = 4.dp.toPx(),
                        center = Offset(x, createdY)
                    )

                    // 完成点
                    val completedY = size.height - (dayData.completedCount * heightScale)
                    drawCircle(
                        color = Success,
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
        // 新增：时间热力图（7×6矩阵）
        if (uiState.timeHeatmap.isNotEmpty()) {
            TimeHeatmapCard(
                heatmapData = uiState.timeHeatmap,
                stats = uiState.timeHeatmapStats
            )
        }

        // 新增：拖延分析雷达图
        uiState.procrastinationRadar?.let {
            ProcrastinationRadarCard(radarData = it)
        }

        // 新增：延迟成本分析卡片
        uiState.delayAnalysis?.let {
            DelayAnalysisCard(delayData = it)
        }

        // 新增：任务完成漏斗
        uiState.taskFunnel?.let {
            TaskFunnelCard(funnelData = it)
        }

        // 准时完成率双环图
        OnTimeCompletionCard(uiState)

        // 重要程度完成时长对比
        ImportanceDurationCard(uiState)
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
private fun SmartInsightsCard(insights: List<com.example.nextthingb1.presentation.screens.stats.InsightData>) {
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
            Text(
                text = "智能洞察",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

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
                color = TextPrimary,
                start = Offset(centerX, centerY),
                end = Offset(endX, endY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )

            // 中心圆点
            drawCircle(
                color = TextPrimary,
                radius = 6.dp.toPx(),
                center = Offset(centerX, centerY)
            )
        }
    }
}

// ==================== 新增：本周vs上周对比卡片 ====================
@Composable
private fun WeekComparisonCard(
    comparison: com.example.nextthingb1.presentation.screens.stats.WeekComparisonData
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
                .padding(20.dp)
        ) {
            Text(
                text = "本周 vs 上周",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 完成数量对比
            ComparisonRow(
                label = "完成数量",
                thisWeekValue = "${comparison.thisWeekCompleted}个",
                lastWeekValue = "${comparison.lastWeekCompleted}个",
                change = comparison.completedChange,
                isIncreaseBetter = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 完成率对比
            ComparisonRow(
                label = "完成率",
                thisWeekValue = "${String.format("%.1f", comparison.thisWeekCompletionRate)}%",
                lastWeekValue = "${String.format("%.1f", comparison.lastWeekCompletionRate)}%",
                change = comparison.completionRateChange.toInt(),
                isIncreaseBetter = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 平均完成时长对比
            ComparisonRow(
                label = "平均时长",
                thisWeekValue = "${comparison.thisWeekAvgDuration.toInt()}分",
                lastWeekValue = "${comparison.lastWeekAvgDuration.toInt()}分",
                change = comparison.avgDurationChange.toInt(),
                isIncreaseBetter = false  // 时长越短越好
            )
        }
    }
}

@Composable
private fun ComparisonRow(
    label: String,
    thisWeekValue: String,
    lastWeekValue: String,
    change: Int,
    isIncreaseBetter: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = TextSecondary,
            modifier = Modifier.width(70.dp)
        )

        // 上周数据
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "上周",
                fontSize = 11.sp,
                color = TextMuted
            )
            Text(
                text = lastWeekValue,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
        }

        // 箭头和变化
        val isPositiveChange = if (isIncreaseBetter) change > 0 else change < 0
        val changeColor = when {
            change == 0 -> TextMuted
            isPositiveChange -> Success
            else -> Danger
        }

        val arrow = when {
            change > 0 -> "↑"
            change < 0 -> "↓"
            else -> "—"
        }

        Text(
            text = "$arrow ${kotlin.math.abs(change)}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = changeColor,
            modifier = Modifier.width(60.dp),
            textAlign = TextAlign.Center
        )

        // 本周数据
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "本周",
                fontSize = 11.sp,
                color = TextMuted
            )
            Text(
                text = thisWeekValue,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
    }
}

/**
 * 分类双层饼图
 */
@Composable
private fun CategoryDoublePieChart(
    categoryStats: List<CategoryStatsData>,
    selectedCategory: TaskCategory?,
    onCategorySelected: (TaskCategory?) -> Unit
) {
    if (categoryStats.isEmpty()) return

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
                text = "分类任务分布",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

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
                    var startAngle = -90f

                    if (selectedCategory == null) {
                        // 外圈：显示分类分布
                        categoryStats.forEach { stat ->
                            val sweepAngle = (stat.totalCount.toFloat() / total) * 360f

                            drawArc(
                                color = stat.category.color,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                topLeft = Offset(
                                    centerX - outerRadius,
                                    centerY - outerRadius
                                ),
                                size = Size(outerRadius * 2, outerRadius * 2),
                                style = Stroke(width = (outerRadius - innerRadius))
                            )

                            startAngle += sweepAngle
                        }
                    } else {
                        // 显示选中分类的状态分布
                        val selectedStat = categoryStats.find { it.category == selectedCategory }
                        selectedStat?.let { stat ->
                            val statusData = listOf(
                                Triple("已完成", stat.completedCount, Color(0xFF4CAF50)),
                                Triple("进行中", stat.pendingCount, Color(0xFF2196F3)),
                                Triple("已逾期", stat.overdueCount, Color(0xFFF4336)),
                                Triple("已取消", stat.cancelledCount, Color(0xFF9E9E9E))
                            ).filter { it.second > 0 }

                            val statusTotal = statusData.sumOf { it.second }
                            var statusStartAngle = -90f

                            // 外圈：显示选中分类
                            drawArc(
                                color = stat.category.color,
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = Offset(
                                    centerX - outerRadius,
                                    centerY - outerRadius
                                ),
                                size = Size(outerRadius * 2, outerRadius * 2),
                                style = Stroke(width = (outerRadius - innerRadius) * 0.4f)
                            )

                            // 内圈：显示状态分布
                            statusData.forEach { (_, count, color) ->
                                val sweepAngle = (count.toFloat() / statusTotal) * 360f

                                drawArc(
                                    color = color,
                                    startAngle = statusStartAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    topLeft = Offset(
                                        centerX - innerRadius * 1.8f,
                                        centerY - innerRadius * 1.8f
                                    ),
                                    size = Size(innerRadius * 3.6f, innerRadius * 3.6f),
                                    style = Stroke(width = innerRadius * 0.8f)
                                )

                                statusStartAngle += sweepAngle
                            }
                        }
                    }
                }

                // 中心文字
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
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
                            .clickable { onCategorySelected(stat.category) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(stat.category.color, CircleShape)
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
    ranking: List<CategoryEfficiencyData>
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
                text = "分类效率排行榜",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

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

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "完成率 ${String.format("%.0f", data.completionRate)}%",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )

                                Text(
                                    text = "平均${String.format("%.1f", data.avgDuration)}分钟",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        // 效率分数
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
                    }
                }
            }
        }
    }
}

/**
 * 分类时间投入热力图
 */
@Composable
private fun CategoryWeekdayHeatmap(
    heatmapData: Map<TaskCategory, Map<Int, Int>>
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
 * 月历热力图（GitHub风格）
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
                text = "完成任务热力图",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = "最近90天的每日完成情况",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 热力图网格
            val weeks = heatmapData.chunked(7)

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                weeks.forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        week.forEach { dayData ->
                            val color = when (dayData.level) {
                                0 -> Color(0xFFEBEDF0)  // 白色
                                1 -> Color(0xFFC6E48B)  // 浅绿
                                2 -> Color(0xFF7BC96F)  // 绿色
                                3 -> Color(0xFF239A3B)  // 深绿
                                else -> Color(0xFF196127)  // 最深绿
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .background(color, RoundedCornerShape(2.dp))
                                    .border(
                                        0.5.dp,
                                        Color(0xFFE0E0E0),
                                        RoundedCornerShape(2.dp)
                                    )
                            )
                        }

                        // 如果这周不足7天，补充空白
                        repeat(7 - week.size) {
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }

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
 * 任务积压趋势面积图
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
                text = "任务积压趋势",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = "最近30天的未完成任务堆积情况",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 面积图
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
                    color = Primary,
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
                text = "时间效率热力图",
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
                text = "效率分析雷达图",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = "六维效率评估模型",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 雷达图（简化版：六边形）
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
                        color = Primary,
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
                    text = "延迟成本分析",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100)
                )
            }

            Text(
                text = "拖延带来的时间损失统计",
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
                text = "任务完成漏斗分析",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = "从创建到完成的全流程流失情况",
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

