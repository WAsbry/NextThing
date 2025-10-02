package com.example.nextthingb1.presentation.screens.stats

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nextthingb1.domain.model.TaskCategory
import com.example.nextthingb1.presentation.theme.*
import java.time.format.DateTimeFormatter
import kotlin.math.*

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
                item { CategoryContent(uiState) }
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
        // 核心指标卡片
        CoreMetricsCards(uiState)

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
private fun CategoryContent(uiState: StatsUiState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 分类任务分布柱状图
        CategoryDistributionChart(uiState)

        // 分类完成时长对比
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
                        .background(Color(android.graphics.Color.parseColor(category.colorHex))),
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
                        Color(android.graphics.Color.parseColor(category.colorHex)),
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
                        Color(android.graphics.Color.parseColor(category.colorHex)),
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
        // 周趋势折线图
        WeeklyTrendChart(uiState)

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
            Text(
                text = "任务完成趋势（最近7天）",
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
            }
        }
    }
}

@Composable
private fun LineChart(
    data: List<DailyTrendData>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas

        val maxValue = data.maxOf { maxOf(it.createdCount, it.completedCount) }.toFloat()
        if (maxValue == 0f) return@Canvas

        val pointSpacing = size.width / (data.size - 1).coerceAtLeast(1)
        val heightScale = size.height / maxValue * 0.8f

        // 绘制网格线
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
