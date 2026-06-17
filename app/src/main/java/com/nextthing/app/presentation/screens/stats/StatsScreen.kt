package com.nextthing.app.presentation.screens.stats

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nextthing.app.domain.model.Category
import com.nextthing.app.domain.model.Task
import com.nextthing.app.presentation.theme.*
import com.nextthing.app.presentation.components.TaskItemCard
import com.nextthing.app.presentation.components.CategoryIconView
import androidx.compose.foundation.lazy.items
import java.time.format.DateTimeFormatter
import kotlin.math.*

// 扩展属性：将 Category 的 colorHex 转换为 Compose Color
private val Category.color: Color
    get() = Color(android.graphics.Color.parseColor(this.colorHex))

// 扩展属性：基于 colorHex 自动生成柔和色（与白色混合 40%）
private val Category.pastelColor: Color
    get() {
        val base = try {
            Color(android.graphics.Color.parseColor(this.colorHex))
        } catch (_: Exception) {
            Color(0xFF42A5F5)
        }
        return Color(
            red   = base.red * 0.6f + 0.4f,
            green = base.green * 0.6f + 0.4f,
            blue  = base.blue * 0.6f + 0.4f,
            alpha = 1f
        )
    }


@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel(),
    onNavigateToTrendDetail: (trendType: String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BgPrimary),
            contentPadding = PaddingValues(bottom = 112.dp)
        ) {
            item {
                StatsImmersiveHeader(
                    uiState = uiState,
                    selectedTab = uiState.selectedTab,
                    onTabSelected = { viewModel.selectTab(it) },
                    onOverviewTimeRangeSelected = { viewModel.selectOverviewTimeRange(it) }
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
                    item { TrendContent(uiState, viewModel, onNavigateToTrendDetail) }
                }
                StatsTab.EFFICIENCY -> {
                    item { EfficiencyContent(uiState, viewModel) }
                }
                StatsTab.AI_INSIGHT -> {
                    item { AIInsightContent(uiState, viewModel) }
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
private fun StatsImmersiveHeader(
    uiState: StatsUiState,
    selectedTab: StatsTab,
    onTabSelected: (StatsTab) -> Unit,
    onOverviewTimeRangeSelected: (OverviewTimeRange) -> Unit
) {
    val header = remember(uiState, selectedTab) {
        buildStatsHeaderModel(uiState, selectedTab)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF171B2D),
                        Color(0xFF242E53),
                        Color(0xFF0F9DB2)
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.09f),
                radius = size.width * 0.32f,
                center = Offset(size.width * 0.88f, size.height * 0.14f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF4F63E7), Color(0xFF0F9DB2))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "NT",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Column {
                        Text(
                            text = "统计",
                            color = Color.White.copy(alpha = 0.68f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = header.title,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                ) {
                    Text(
                        text = header.badge,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            StatsTabRow(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                immersive = true
            )

            if (selectedTab == StatsTab.OVERVIEW) {
                OverviewTimeRangeChips(
                    selectedTimeRange = uiState.selectedOverviewTimeRange,
                    onTimeRangeSelected = onOverviewTimeRangeSelected
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.11f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = header.label,
                            color = Color.White.copy(alpha = 0.68f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = header.value,
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = 32.sp
                        )
                    }
                    Surface(
                        color = Color.White.copy(alpha = 0.13f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                    ) {
                        Text(
                            text = header.state,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = header.summary,
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    header.signals.forEach { signal ->
                        HeaderSignalCard(
                            label = signal.first,
                            value = signal.second,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

private data class StatsHeaderModel(
    val title: String,
    val label: String,
    val value: String,
    val badge: String,
    val state: String,
    val summary: String,
    val signals: List<Pair<String, String>>
)

private fun buildStatsHeaderModel(
    uiState: StatsUiState,
    selectedTab: StatsTab
): StatsHeaderModel {
    val completion = "${uiState.completionRate.roundToInt()}%"
    val healthState = when {
        uiState.healthScore >= 85 -> "优秀"
        uiState.healthScore >= 70 -> "良好"
        uiState.healthScore >= 50 -> "需要校准"
        else -> "风险上升"
    }
    val firstInsight = uiState.insights.firstOrNull()?.message

    return when (selectedTab) {
        StatsTab.OVERVIEW -> StatsHeaderModel(
            title = "洞察中枢",
            label = "AI 判断",
            value = when {
                uiState.totalTasks == 0 -> "等待数据"
                uiState.healthScore >= 75 -> "节奏回稳"
                uiState.healthScore >= 50 -> "节奏波动"
                else -> "需要收敛"
            },
            badge = if (uiState.totalTasks == 0) "待分析" else "健康度 ${uiState.healthScore}",
            state = if (uiState.totalTasks == 0) "暂无风险" else healthState,
            summary = if (uiState.totalTasks == 0) {
                "完成几个任务后，我会在这里提炼你的执行节奏、逾期风险和下一步优先级。"
            } else {
                firstInsight ?: "完成几个任务后，我会在这里提炼你的执行节奏、逾期风险和下一步优先级。"
            },
            signals = listOf(
                "完成率" to if (uiState.totalTasks == 0) "--" else completion,
                "逾期" to "${uiState.coreMetricOverdue}",
                "重要紧急" to "${uiState.coreMetricImportantUrgent}"
            )
        )
        StatsTab.CATEGORY -> StatsHeaderModel(
            title = "分类分析",
            label = "结构判断",
            value = "投入分布",
            badge = "${uiState.categoryStats.size} 类",
            state = "看结构",
            summary = "分类页重点看任务投入是否偏科，以及哪些分类完成率高、拖延成本低。",
            signals = listOf(
                "分类数" to "${uiState.categoryStats.size}",
                "排行" to "${uiState.categoryEfficiencyRanking.size}",
                "热力" to if (uiState.categoryWeekdayHeatmap.isEmpty()) "暂无" else "已生成"
            )
        )
        StatsTab.TREND -> StatsHeaderModel(
            title = "趋势追踪",
            label = "趋势判断",
            value = if (uiState.completionRateTrend.isEmpty()) "等待数据" else "持续观察",
            badge = uiState.selectedTrendTimeRange.displayName,
            state = "看变化",
            summary = "趋势页保留完成量、完成率、周期时间和累积流，用来判断效率是在变好还是变差。",
            signals = listOf(
                "日趋势" to "${uiState.weeklyTrend.size}",
                "完成率" to "${uiState.completionRateTrend.size}",
                "周期" to "${uiState.cycleTimeTrend.size}"
            )
        )
        StatsTab.EFFICIENCY -> StatsHeaderModel(
            title = "效率诊断",
            label = "效率判断",
            value = when {
                uiState.completedTasks == 0 -> "等待数据"
                uiState.procrastinationRadar != null -> "${uiState.procrastinationRadar.totalScore} 分"
                else -> "待评估"
            },
            badge = uiState.selectedEfficiencyTimeRange.displayName,
            state = when {
                uiState.completedTasks == 0 -> "待分析"
                uiState.procrastinationRadar != null -> uiState.procrastinationRadar.efficiencyGrade
                else -> "诊断"
            },
            summary = when {
                uiState.completedTasks == 0 -> "完成一些任务后，我会从准时率、响应速度、完成稳定性和任务漏斗里找出主要阻力。"
                uiState.procrastinationRadar != null -> "当前最强项是「${uiState.procrastinationRadar.strongestDimension}」，最需要提升的是「${uiState.procrastinationRadar.weakestDimension}」。"
                else -> "效率页会从准时率、响应速度、完成稳定性和任务漏斗里找出主要阻力。"
            },
            signals = listOf(
                "准时率" to if (uiState.completedTasks == 0) "--" else "${uiState.onTimeCompletionRate.roundToInt()}%",
                "漏斗" to if (uiState.taskFunnel == null) "暂无" else "已生成",
                "热力" to if (uiState.timeHeatmap.isEmpty()) "暂无" else "已生成"
            )
        )
        StatsTab.AI_INSIGHT -> StatsHeaderModel(
            title = "AI 周报",
            label = "本周判断",
            value = uiState.weeklyReport?.title ?: "等待生成",
            badge = if (uiState.isGeneratingReport || uiState.isAnalyzingBehavior) "分析中" else "AI",
            state = "行动建议",
            summary = uiState.weeklyReport?.summary
                ?: uiState.behaviorInsight?.patterns?.firstOrNull()
                ?: "AI 洞察会把行为模式、周报摘要和下周建议收敛成可以执行的行动。",
            signals = listOf(
                "行为模式" to (uiState.behaviorInsight?.patterns?.size?.toString() ?: "0"),
                "改进建议" to (uiState.behaviorInsight?.suggestions?.size?.toString() ?: "0"),
                "周报" to if (uiState.weeklyReport == null) "未生成" else "已生成"
            )
        )
    }
}

@Composable
private fun HeaderSignalCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(10.dp))
            .padding(8.dp)
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.62f),
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun OverviewTimeRangeChips(
    selectedTimeRange: OverviewTimeRange,
    onTimeRangeSelected: (OverviewTimeRange) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        OverviewTimeRange.values().forEach { range ->
            val selected = selectedTimeRange == range
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) Color.White else Color.Transparent)
                    .clickable { onTimeRangeSelected(range) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = range.displayName,
                    color = if (selected) Color(0xFF171B2D) else Color.White.copy(alpha = 0.70f),
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun StatsTabRow(
    selectedTab: StatsTab,
    onTabSelected: (StatsTab) -> Unit,
    immersive: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (immersive) Modifier else Modifier.padding(16.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (immersive) Color.White.copy(alpha = 0.10f) else BgCard
        ),
        border = if (immersive) BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            StatsTab.values().forEach { tab ->
                val isSelected = selectedTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when {
                                immersive && isSelected -> Color.White
                                isSelected -> Primary
                                else -> Color.Transparent
                            }
                        )
                        .clickable { onTabSelected(tab) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.title,
                        color = when {
                            immersive && isSelected -> Color(0xFF171B2D)
                            immersive -> Color.White.copy(alpha = 0.70f)
                            isSelected -> Color.White
                            else -> TextSecondary
                        },
                        fontSize = 12.sp,
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
        Spacer(modifier = Modifier.height(2.dp))

        OverviewMetricGrid(uiState = uiState, viewModel = viewModel)

        // 新增：本周vs上周对比卡片
        uiState.weekComparison?.let { comparison ->
            WeekComparisonCard(
                comparison = comparison,
                timeRange = uiState.selectedOverviewTimeRange
            )
        }

        // 完成率进度条
        CompletionProgressCard(uiState)

        if (uiState.totalTasks > 0) {
            ImportanceDistributionCard(uiState)
        }
    }
}

@Composable
private fun OverviewMetricGrid(
    uiState: StatsUiState,
    viewModel: StatsViewModel
) {
    val progressTitle = if (uiState.coreMetricProgressType == "count") "已完成" else "完成率"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OverviewMetricTile(
                label = "待办任务",
                value = uiState.coreMetricPending.toString(),
                trend = "需要推进",
                color = Primary,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.showTaskList(TaskListType.PENDING) }
            )
            OverviewMetricTile(
                label = "逾期",
                value = uiState.coreMetricOverdue.toString(),
                trend = if (uiState.coreMetricOverdue == 0) "当前清爽" else "优先处理",
                color = Danger,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.showTaskList(TaskListType.OVERDUE) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OverviewMetricTile(
                label = "重要紧急",
                value = uiState.coreMetricImportantUrgent.toString(),
                trend = if (uiState.coreMetricImportantUrgent == 0) "风险较低" else "风险仍在",
                color = Warning,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.showTaskList(TaskListType.IMPORTANT_URGENT) }
            )
            OverviewMetricTile(
                label = progressTitle,
                value = uiState.coreMetricProgress,
                trend = "当前进度",
                color = Success,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.showTaskList(TaskListType.COMPLETED) }
            )
        }
    }
}

@Composable
private fun OverviewMetricTile(
    label: String,
    value: String,
    trend: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(104.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(1.dp, Border.copy(alpha = 0.65f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }

            Column {
                Text(
                    text = value,
                    color = TextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
                Text(
                    text = trend,
                    color = color,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
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
            onClick = { viewModel.showTaskList(com.nextthing.app.presentation.screens.stats.TaskListType.PENDING) }
        )
        MetricCard(
            title = "重要紧急",
            value = uiState.coreMetricImportantUrgent.toString(),
            icon = "🔥",
            color = Danger,
            modifier = Modifier.weight(1f),
            onClick = { viewModel.showTaskList(com.nextthing.app.presentation.screens.stats.TaskListType.IMPORTANT_URGENT) }
        )
        MetricCard(
            title = "逾期任务",
            value = uiState.coreMetricOverdue.toString(),
            icon = "⏰",
            color = Warning,
            modifier = Modifier.weight(1f),
            onClick = { viewModel.showTaskList(com.nextthing.app.presentation.screens.stats.TaskListType.OVERDUE) }
        )
        MetricCard(
            title = progressTitle,
            value = uiState.coreMetricProgress,
            icon = progressIcon,
            color = Success,
            modifier = Modifier.weight(1f),
            onClick = { viewModel.showTaskList(com.nextthing.app.presentation.screens.stats.TaskListType.COMPLETED) }
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
                color = Primary  // 蓝色
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
                color = Warning  // 橙黄色
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
                color = TextMuted  // 灰色
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 汇总信息
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        BgSecondary,
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
                .background(Border)
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
                    LegendItem("重要紧急", uiState.importantUrgentCount, Danger)
                    LegendItem("重要不紧急", uiState.importantNotUrgentCount, Warning)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LegendItem("不重要紧急", uiState.notImportantUrgentCount, Primary)
                    LegendItem("不重要不紧急", uiState.notImportantNotUrgentCount, Success)
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
        val dangerColor = Danger
        val warningColor = Warning
        val primaryColor = Primary
        val successColor = Success
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
                    color = dangerColor,
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
                    color = warningColor,
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
                    color = primaryColor,
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
                    color = successColor,
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

        // 分类×星期热力图
        if (uiState.categoryWeekdayHeatmap.isNotEmpty()) {
            CategoryWeekdayHeatmap(
                heatmapData = uiState.categoryWeekdayHeatmap
            )
        }
    }
}



// ==================== 趋势统计页面 ====================
@Composable
private fun TrendContent(
    uiState: StatsUiState,
    viewModel: StatsViewModel,
    onNavigateToTrendDetail: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 公共时间维度选择器
        TrendTimeRangeSelector(
            selectedTimeRange = uiState.selectedTrendTimeRange,
            onTimeRangeSelected = { viewModel.selectTrendTimeRange(it) }
        )

        // 1. 任务完成趋势折线图（含7日移动平均线）
        WeeklyTrendChart(uiState, onNavigateToTrendDetail)

        // 2. 完成率走势
        if (uiState.completionRateTrend.isNotEmpty()) {
            CompletionRateTrendChart(data = uiState.completionRateTrend, onNavigateToDetail = onNavigateToTrendDetail)
        }

        // 3. 平均完成周期（Cycle Time）
        if (uiState.cycleTimeTrend.isNotEmpty()) {
            CycleTimeTrendChart(data = uiState.cycleTimeTrend, onNavigateToDetail = onNavigateToTrendDetail)
        }

        // 4. 累积流图（CFD）
        if (uiState.cumulativeFlow.isNotEmpty()) {
            CumulativeFlowChart(data = uiState.cumulativeFlow, onNavigateToDetail = onNavigateToTrendDetail)
        }
    }
}

@Composable
private fun TrendTimeRangeSelector(
    selectedTimeRange: OverviewTimeRange,
    onTimeRangeSelected: (OverviewTimeRange) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(BgCard)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(
            OverviewTimeRange.THIS_WEEK,
            OverviewTimeRange.THIS_MONTH,
            OverviewTimeRange.ALL
        ).forEach { timeRange ->
            val isSelected = selectedTimeRange == timeRange
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) Primary else Color.Transparent)
                    .clickable { onTimeRangeSelected(timeRange) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = timeRange.displayName,
                    color = if (isSelected) Color.White else TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

// 公共趋势标签 + 问号按钮组件
@Composable
private fun TrendLabelWithHelp(
    arrow: String,
    label: String,
    color: Color,
    trendType: String,
    onNavigateToDetail: (String) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = Modifier
                .background(color.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(text = arrow, fontSize = 13.sp, color = color, fontWeight = FontWeight.Bold)
            Text(text = label, fontSize = 12.sp, color = color, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(0xFFF0F0F0))
                .clickable { onNavigateToDetail(trendType) },
            contentAlignment = Alignment.Center
        ) {
            Text("?", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
        }
    }
}

@Composable
private fun WeeklyTrendChart(uiState: StatsUiState, onNavigateToDetail: (String) -> Unit) {
    // 计算趋势
    val trendInfo = if (uiState.weeklyTrend.size >= 6) {
        val recentAvg = uiState.weeklyTrend.takeLast(3).map { it.completedCount }.average()
        val previousAvg = uiState.weeklyTrend.dropLast(3).takeLast(3).map { it.completedCount }.average()
        when {
            previousAvg == 0.0 -> Triple("→", "平稳", TextMuted)
            recentAvg > previousAvg * 1.1 -> Triple("↗", "上升", Success)
            recentAvg < previousAvg * 0.9 -> Triple("↘", "下降", Danger)
            else -> Triple("→", "平稳", TextMuted)
        }
    } else {
        Triple("→", "平稳", TextMuted)
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "任务完成趋势", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                TrendLabelWithHelp(
                    arrow = trendInfo.first,
                    label = trendInfo.second,
                    color = trendInfo.third,
                    trendType = "completion",
                    onNavigateToDetail = onNavigateToDetail
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

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

                    // 7日均线图例
                    Spacer(modifier = Modifier.width(24.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp, 2.dp)
                                .background(Success.copy(alpha = 0.4f), RoundedCornerShape(1.dp))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "7日均线",
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

    val yAxisMax = ((maxValue / 5).toInt() + 1) * 5

    Column(modifier = modifier) {
        val primaryColor = Primary
        val successColor = Success
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val pointSpacing = size.width / (data.size - 1).coerceAtLeast(1)
            val heightScale = size.height / yAxisMax

            // 面积填充 - 创建任务（蓝色渐变）
            val createdAreaPath = Path()
            createdAreaPath.moveTo(0f, size.height)
            data.forEachIndexed { index, dayData ->
                val x = index * pointSpacing
                val y = size.height - (dayData.createdCount * heightScale)
                createdAreaPath.lineTo(x, y)
            }
            createdAreaPath.lineTo((data.size - 1) * pointSpacing, size.height)
            createdAreaPath.close()
            drawPath(
                createdAreaPath,
                Brush.verticalGradient(listOf(primaryColor.copy(alpha = 0.25f), primaryColor.copy(alpha = 0.02f)))
            )

            // 面积填充 - 完成任务（绿色渐变）
            val completedAreaPath = Path()
            completedAreaPath.moveTo(0f, size.height)
            data.forEachIndexed { index, dayData ->
                val x = index * pointSpacing
                val y = size.height - (dayData.completedCount * heightScale)
                completedAreaPath.lineTo(x, y)
            }
            completedAreaPath.lineTo((data.size - 1) * pointSpacing, size.height)
            completedAreaPath.close()
            drawPath(
                completedAreaPath,
                Brush.verticalGradient(listOf(successColor.copy(alpha = 0.3f), successColor.copy(alpha = 0.02f)))
            )

            // 绘制创建任务折线（蓝色）
            val createdPath = Path()
            data.forEachIndexed { index, dayData ->
                val x = index * pointSpacing
                val y = size.height - (dayData.createdCount * heightScale)
                if (index == 0) createdPath.moveTo(x, y) else createdPath.lineTo(x, y)
            }
            drawPath(createdPath, primaryColor, style = Stroke(width = 2.5f, cap = StrokeCap.Round))

            // 绘制完成任务折线（绿色）
            val completedPath = Path()
            data.forEachIndexed { index, dayData ->
                val x = index * pointSpacing
                val y = size.height - (dayData.completedCount * heightScale)
                if (index == 0) completedPath.moveTo(x, y) else completedPath.lineTo(x, y)
            }
            drawPath(completedPath, successColor, style = Stroke(width = 2.5f, cap = StrokeCap.Round))

            // 绘制7日移动平均线（虚线）
            if (data.size >= 7) {
                val maPath = Path()
                var maStarted = false
                for (i in data.indices) {
                    val windowStart = (i - 6).coerceAtLeast(0)
                    val avg = data.subList(windowStart, i + 1).map { it.completedCount }.average().toFloat()
                    val x = i * pointSpacing
                    val y = size.height - (avg * heightScale)
                    if (!maStarted) { maPath.moveTo(x, y); maStarted = true }
                    else maPath.lineTo(x, y)
                }
                drawPath(
                    path = maPath,
                    color = successColor.copy(alpha = 0.4f),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            floatArrayOf(10f, 8f), 0f
                        )
                    )
                )
            }

            // 绘制数据点
            data.forEachIndexed { index, dayData ->
                val x = index * pointSpacing
                val createdY = size.height - (dayData.createdCount * heightScale)
                drawCircle(primaryColor, radius = 3.5f, center = Offset(x, createdY))
                val completedY = size.height - (dayData.completedCount * heightScale)
                drawCircle(successColor, radius = 3.5f, center = Offset(x, completedY))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 横轴日期标签
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
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
                        color = AccentPurple
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
private fun EfficiencyContent(uiState: StatsUiState, viewModel: StatsViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (uiState.completedTasks == 0) {
            EfficiencyReadinessCard(uiState = uiState)
        } else {
            // 1. 效率雷达图（六维度评分 + 蛛网结构）
            uiState.procrastinationRadar?.let {
            ProcrastinationRadarCard(
                radarData = it,
                selectedTimeRange = uiState.selectedEfficiencyTimeRange,
                onTimeRangeSelected = { range -> viewModel.selectEfficiencyTimeRange(range) }
            )
            }
        }

        // 2. 黄金时段热力图
        if (uiState.timeHeatmap.isNotEmpty()) {
            TimeHeatmapCard(
                heatmapData = uiState.timeHeatmap,
                stats = uiState.timeHeatmapStats,
                selectedTimeRange = uiState.selectedEfficiencyTimeRange,
                onTimeRangeSelected = { range -> viewModel.selectEfficiencyTimeRange(range) }
            )
        }

        // 3. 效率周对比卡
        EfficiencyComparisonCard(uiState = uiState)

        // 4. 效率建议卡
        EfficiencyAdviceCard(uiState = uiState)
    }
}

@Composable
private fun EfficiencyReadinessCard(uiState: StatsUiState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(1.dp, Border.copy(alpha = 0.65f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "效率诊断准备中",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "完成一些任务后，这里会生成综合效率评分、准时率、响应速度和稳定性分析。",
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = TextSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactReadinessMetric(
                    label = "已完成",
                    value = uiState.completedTasks.toString(),
                    color = Success,
                    modifier = Modifier.weight(1f)
                )
                CompactReadinessMetric(
                    label = "待办",
                    value = uiState.pendingTasks.toString(),
                    color = Primary,
                    modifier = Modifier.weight(1f)
                )
                CompactReadinessMetric(
                    label = "逾期",
                    value = uiState.overdueTasks.toString(),
                    color = Danger,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CompactReadinessMetric(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.08f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(text = label, fontSize = 11.sp, color = TextMuted, maxLines = 1)
        Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
    }
}



// ==================== 任务洞察卡片（规则洞察 + AI 深度分析）====================
@Composable
private fun InsightsCard(
    insights: List<InsightData>,
    selectedTimeRange: OverviewTimeRange,
    onTimeRangeSelected: (OverviewTimeRange) -> Unit,
    aiSummary: String?,
    isAILoading: Boolean,
    aiError: String?,
    onGenerateAI: () -> Unit,
    onClearAIError: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── 上半部分：始终显示，与 AI 状态无关 ──

            // 标题 + 时间维度选择器（始终显示）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💡 任务洞察",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(BgSecondary)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    OverviewTimeRange.values().forEach { timeRange ->
                        val isSelected = selectedTimeRange == timeRange
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) Primary else BgCard)
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

            // 规则洞察条目（始终显示）
            if (insights.isNotEmpty()) {
                insights.forEach { InsightRow(insight = it) }
            }

            // ── 分割线 ──
            HorizontalDivider(color = BgSecondary, thickness = 1.dp)

            // ── 下半部分：AI 深度分析区域 ──

            // AI 区域标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "✨ AI 深度分析",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                // 已生成时显示刷新按钮
                if (aiSummary != null && !isAILoading) {
                    IconButton(
                        onClick = onGenerateAI,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text(text = "🔄", fontSize = 15.sp)
                    }
                }
            }

            // AI 内容区
            when {
                isAILoading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "AI 正在深度分析...",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                }
                aiError != null -> {
                    Text(
                        text = aiError,
                        fontSize = 13.sp,
                        color = Danger,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onGenerateAI,
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(text = "重试", fontSize = 13.sp)
                        }
                        TextButton(onClick = onClearAIError) {
                            Text(text = "关闭", fontSize = 13.sp, color = TextSecondary)
                        }
                    }
                }
                aiSummary != null -> {
                    Text(
                        text = aiSummary,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        lineHeight = 22.sp
                    )
                }
                else -> {
                    Button(
                        onClick = onGenerateAI,
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Text(text = "生成 AI 分析", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightRow(insight: com.nextthing.app.presentation.screens.stats.InsightData) {
    val backgroundColor = when (insight.type) {
        com.nextthing.app.presentation.screens.stats.InsightType.POSITIVE -> Success.copy(alpha = 0.08f)
        com.nextthing.app.presentation.screens.stats.InsightType.WARNING -> Warning.copy(alpha = 0.08f)
        com.nextthing.app.presentation.screens.stats.InsightType.ALERT -> Danger.copy(alpha = 0.08f)
    }

    val iconColor = when (insight.type) {
        com.nextthing.app.presentation.screens.stats.InsightType.POSITIVE -> Success
        com.nextthing.app.presentation.screens.stats.InsightType.WARNING -> Warning
        com.nextthing.app.presentation.screens.stats.InsightType.ALERT -> Danger
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
    healthLevel: com.nextthing.app.presentation.screens.stats.HealthLevel
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
                    com.nextthing.app.presentation.screens.stats.HealthLevel.EXCELLENT -> Success
                    com.nextthing.app.presentation.screens.stats.HealthLevel.GOOD -> Primary
                    com.nextthing.app.presentation.screens.stats.HealthLevel.AVERAGE -> Warning
                    com.nextthing.app.presentation.screens.stats.HealthLevel.POOR -> Danger
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
    level: com.nextthing.app.presentation.screens.stats.HealthLevel,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val textPrimaryColor = TextPrimary
        val borderColor = Border
        val successColor = Success
        val primaryColor = Primary
        val warningColor = Warning
        val dangerColor = Danger
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height * 0.85f
            val radius = minOf(centerX, centerY) * 0.9f
            val strokeWidth = radius * 0.2f

            // 背景弧线（灰色）
            drawArc(
                color = borderColor,
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
                score >= 85 -> successColor  // 绿色
                score >= 70 -> primaryColor  // 蓝色
                score >= 50 -> warningColor  // 橙色
                else -> dangerColor         // 红色
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
    comparison: com.nextthing.app.presentation.screens.stats.WeekComparisonData,
    timeRange: com.nextthing.app.presentation.screens.stats.OverviewTimeRange
) {
    // 根据时间维度确定标题和标签
    val (title, currentLabel, previousLabel) = when (timeRange) {
        com.nextthing.app.presentation.screens.stats.OverviewTimeRange.TODAY ->
            Triple("今日 vs 昨日", "今", "昨")
        com.nextthing.app.presentation.screens.stats.OverviewTimeRange.THIS_WEEK ->
            Triple("本周 vs 上周", "本周", "上周")
        com.nextthing.app.presentation.screens.stats.OverviewTimeRange.THIS_MONTH ->
            Triple("本月 vs 上月", "本月", "上月")
        com.nextthing.app.presentation.screens.stats.OverviewTimeRange.ALL ->
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
                        .background(TextMuted, RoundedCornerShape(2.dp))
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
                barColor = TextMuted,
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
        colors = CardDefaults.cardColors(containerColor = BgCard),
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
                        .background(BgSecondary)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    OverviewTimeRange.values().forEach { timeRange ->
                        val isSelected = selectedTimeRange == timeRange
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isSelected) Primary else BgCard
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
                val successColor = Success
                val primaryColor = Primary
                val dangerColor = Danger
                val textMutedColor = TextMuted
                Canvas(                    modifier = Modifier
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
                                Triple("已完成", stat.completedCount, successColor),
                                Triple("进行中", stat.pendingCount, primaryColor),
                                Triple("已逾期", stat.overdueCount, dangerColor),
                                Triple("已取消", stat.cancelledCount, textMutedColor)
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
                        CategoryIconView(
                            icon = selectedCategory.icon,
                            size = 36.dp
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
                // 分类图例：emoji + 名称 + 进度条 + 完成率 + 任务数
                categoryStats.forEach { stat ->
                    val completionRate = if (stat.totalCount > 0)
                        stat.completedCount.toFloat() / stat.totalCount else 0f

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(stat.category.pastelColor.copy(alpha = 0.08f))
                            .clickable { onCategorySelected(stat.category) }
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 颜色圆点
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(stat.category.pastelColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // 图标 + 名称
                        Row(
                            modifier = Modifier.width(80.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryIconView(icon = stat.category.icon, size = 18.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stat.category.displayName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        // 进度条
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(BgSecondary)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(completionRate)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(stat.category.pastelColor)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        // 完成率
                        Text(
                            text = "${(completionRate * 100).toInt()}%",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.width(32.dp),
                            textAlign = TextAlign.End
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        // 任务数
                        Text(
                            text = "${stat.totalCount}个",
                            fontSize = 12.sp,
                            color = TextMuted,
                            modifier = Modifier.width(28.dp),
                            textAlign = TextAlign.End
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            } else {
                // 状态详情
                val selectedStat = categoryStats.find { it.category == selectedCategory }
                selectedStat?.let { stat ->
                    listOf(
                        Triple("已完成", stat.completedCount, Success),
                        Triple("进行中", stat.pendingCount, Primary),
                        Triple("已逾期", stat.overdueCount, Danger),
                        Triple("已取消", stat.cancelledCount, TextMuted)
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
 * 分类效率排行榜 — 默认只展示 Top 1，点击展开全部
 */
@Composable
private fun CategoryEfficiencyRanking(
    ranking: List<CategoryEfficiencyData>,
    selectedTimeRange: OverviewTimeRange
) {
    var expanded by remember { mutableStateOf(false) }
    var showEfficiencyExplanation by remember { mutableStateOf(false) }

    val displayList = if (expanded) ranking else ranking.take(3)

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
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "分类效率排行",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = selectedTimeRange.displayName,
                    color = Primary,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .background(Primary.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            displayList.forEach { data ->
                EfficiencyRankingItem(
                    data = data,
                    onInfoClick = { showEfficiencyExplanation = true }
                )
                if (data != displayList.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // 展开/收起按钮（分类超过3个时才显示）
            if (ranking.size > 3) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { expanded = !expanded }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (expanded) "收起" else "查看全部 ${ranking.size} 个分类",
                        fontSize = 13.sp,
                        color = Primary
                    )
                    Text(
                        text = if (expanded) " ∧" else " ∨",
                        fontSize = 13.sp,
                        color = Primary
                    )
                }
            }
        }
    }

    if (showEfficiencyExplanation) {
        EfficiencyScoreExplanationSheet(onDismiss = { showEfficiencyExplanation = false })
    }
}

@Composable
private fun EfficiencyRankingItem(
    data: CategoryEfficiencyData,
    onInfoClick: () -> Unit
) {
    val scoreColor = when {
        data.efficiencyScore >= 80 -> Success
        data.efficiencyScore >= 60 -> Primary
        data.efficiencyScore >= 40 -> Warning
        else -> TextMuted
    }
    val medal = when (data.rank) {
        1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BgSecondary)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧颜色竖条（跟随分类颜色）
        val categoryColor = try {
            Color(android.graphics.Color.parseColor(data.category.colorHex))
        } catch (_: Exception) { Primary }
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(categoryColor)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // 排名徽章
        Box(modifier = Modifier.width(26.dp), contentAlignment = Alignment.Center) {
            if (medal != null) {
                Text(text = medal, fontSize = 18.sp)
            } else {
                Text(
                    text = "${data.rank}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // 中间：图标 + 名称 + 进度条 + 子标题
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryIconView(icon = data.category.icon, size = 20.dp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = data.category.displayName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            // 完成率进度条
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(BgCard)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(data.completionRate.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(3.dp))
                        .background(scoreColor)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "完成率 ${String.format("%.0f", data.completionRate * 100)}%",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 右侧效率分（可点击查看说明）
        Column(
            modifier = Modifier
                .clickable { onInfoClick() }
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${data.efficiencyScore}",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = scoreColor
            )
            Text(
                text = "效率分",
                fontSize = 10.sp,
                color = TextMuted
            )
        }
    }
}

/**
 * 效率分说明底部弹窗 — 5 维度评分体系
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EfficiencyScoreExplanationSheet(
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BgCard,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 标题
            Text(
                text = "效率分计算规则",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            // 核心理念
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.08f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "💯", fontSize = 28.sp, modifier = Modifier.padding(end = 12.dp))
                    Column {
                        Text(
                            text = "五维综合评估，满分100分",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "综合衡量一个分类的任务执行质量",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            // 评分维度
            Text(text = "评分维度", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)

            EfficiencyDimensionItem(icon = "✅", name = "完成率", weight = 35, desc = "已完成 / 总任务数", color = Success)
            EfficiencyDimensionItem(icon = "⏰", name = "准时率", weight = 25, desc = "按时完成 / 已完成数", color = Primary)
            EfficiencyDimensionItem(icon = "💪", name = "执行率", weight = 15, desc = "未放弃 / 总任务数", color = Warning)
            EfficiencyDimensionItem(icon = "⚡", name = "响应速度", weight = 15, desc = "基于平均完成时长", color = AccentPurple)
            EfficiencyDimensionItem(icon = "📋", name = "积压控制", weight = 10, desc = "已处理 / 总任务数", color = TextSecondary)

            // 公式
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = BgSecondary)
            ) {
                Text(
                    text = "完成率×35 + 准时率×25 + 执行率×15 + 速度×15 + 积压×10",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.padding(14.dp)
                )
            }

            // 评分等级
            Text(text = "评分等级", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ScoreGradeBadge("80+", "优秀", Success)
                ScoreGradeBadge("60+", "良好", Primary)
                ScoreGradeBadge("40+", "一般", Warning)
                ScoreGradeBadge("0+", "待改进", TextMuted)
            }
        }
    }
}

@Composable
private fun EfficiencyDimensionItem(
    icon: String,
    name: String,
    weight: Int,
    desc: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 20.sp)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(text = desc, fontSize = 12.sp, color = TextSecondary)
        }
        // 权重条
        Box(
            modifier = Modifier
                .width(50.dp)
                .height(6.dp)
                .background(BgSecondary, RoundedCornerShape(3.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(weight / 35f) // 相对最大权重的比例
                    .background(color, RoundedCornerShape(3.dp))
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${weight}%",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun ScoreGradeBadge(score: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = score, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(text = label, fontSize = 11.sp, color = TextSecondary)
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
        colors = CardDefaults.cardColors(containerColor = BgCard),
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

            // GitHub 风格绿色色阶（暗色主题适配）
            val githubGreenLevels = listOf(
                BgSecondary,              // 0 个任务：深色底
                Color(0xFFC8E6C9),        // 1~2 个：很浅绿
                Color(0xFF81C784),        // 3~4 个：浅绿
                Color(0xFF4CAF50),        // 5~7 个：中绿
                Color(0xFF2E7D32)         // 8+ 个：深绿
            )

            // 热力图行
            heatmapData.forEach { (category, weekdayMap) ->
                val maxCount = weekdayMap.values.maxOrNull()?.coerceAtLeast(1) ?: 1

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 分类标签：图标 + 名称
                    Row(
                        modifier = Modifier.width(64.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CategoryIconView(icon = category.icon, size = 18.dp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = category.displayName,
                            fontSize = 11.sp,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // 7天的热力方块（GitHub 绿色色阶）
                    (1..7).forEach { dayOfWeek ->
                        val count = weekdayMap[dayOfWeek] ?: 0
                        val level = when {
                            count == 0 -> 0
                            count <= 2 -> 1
                            count <= 4 -> 2
                            count <= 7 -> 3
                            else       -> 4
                        }
                        val cellColor = githubGreenLevels[level]
                        val textColor = if (level >= 3) Color.White else TextPrimary

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(cellColor),
                            contentAlignment = Alignment.Center
                        ) {
                            if (count > 0) {
                                Text(
                                    text = "$count",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 图例说明（仿 GitHub 色阶标注）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "少", fontSize = 10.sp, color = TextMuted)
                Spacer(modifier = Modifier.width(4.dp))
                listOf(BgSecondary, Color(0xFFC8E6C9), Color(0xFF81C784), Color(0xFF4CAF50), Color(0xFF2E7D32))
                    .forEach { color ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .size(12.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(color)
                        )
                    }
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "多", fontSize = 10.sp, color = TextMuted)
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
        colors = CardDefaults.cardColors(containerColor = BgCard),
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

// ==================== 完成率走势图 ====================
@Composable
private fun CompletionRateTrendChart(data: List<WeeklyCompletionRateData>, onNavigateToDetail: (String) -> Unit = {}) {
    val avgRate = data.map { it.completionRate }.average().toFloat()
    // 周环比
    val thisWeekRate = data.lastOrNull()?.completionRate ?: 0f
    val lastWeekRate = if (data.size >= 2) data[data.size - 2].completionRate else thisWeekRate
    val rateChange = thisWeekRate - lastWeekRate
    val changeSign = if (rateChange >= 0) "+" else ""
    val changeColor = if (rateChange >= 0) Success else Danger

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "完成率走势", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                TrendLabelWithHelp(
                    arrow = if (rateChange > 0.02f) "↗" else if (rateChange < -0.02f) "↘" else "→",
                    label = if (rateChange > 0.02f) "上升" else if (rateChange < -0.02f) "下降" else "平稳",
                    color = if (rateChange > 0.02f) Success else if (rateChange < -0.02f) Danger else TextMuted,
                    trendType = "rate",
                    onNavigateToDetail = onNavigateToDetail
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val successColor = Success
            val primaryColor = Primary
            val borderColor = Border
            val bgSecondaryColor = BgSecondary

            Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                val maxRate = 1f
                val pointSpacing = size.width / (data.size - 1).coerceAtLeast(1)
                val heightScale = size.height / maxRate

                // 网格线
                for (i in 0..4) {
                    val y = size.height - (size.height / 4 * i)
                    drawLine(borderColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                }

                // 均值水平虚线
                val avgY = size.height - (avgRate * heightScale)
                drawLine(
                    color = primaryColor.copy(alpha = 0.5f),
                    start = Offset(0f, avgY),
                    end = Offset(size.width, avgY),
                    strokeWidth = 1.5f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                )

                // 面积填充
                val areaPath = Path()
                areaPath.moveTo(0f, size.height)
                data.forEachIndexed { index, d ->
                    val x = index * pointSpacing
                    val y = size.height - (d.completionRate * heightScale)
                    areaPath.lineTo(x, y)
                }
                areaPath.lineTo((data.size - 1) * pointSpacing, size.height)
                areaPath.close()
                drawPath(areaPath, Brush.verticalGradient(listOf(successColor.copy(alpha = 0.3f), successColor.copy(alpha = 0.02f))))

                // 折线
                val linePath = Path()
                data.forEachIndexed { index, d ->
                    val x = index * pointSpacing
                    val y = size.height - (d.completionRate * heightScale)
                    if (index == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
                }
                drawPath(linePath, successColor, style = Stroke(width = 2.5f, cap = StrokeCap.Round))

                // 数据点
                data.forEachIndexed { index, d ->
                    val x = index * pointSpacing
                    val y = size.height - (d.completionRate * heightScale)
                    drawCircle(successColor, radius = 3.5f, center = Offset(x, y))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 横轴标签
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val step = (data.size - 1) / 5.coerceAtLeast(1)
                for (i in 0..5) {
                    val idx = (i * step).coerceAtMost(data.size - 1)
                    Text(text = data[idx].weekLabel, fontSize = 10.sp, color = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 底部周环比
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "本周 ${(thisWeekRate * 100).toInt()}%", fontSize = 13.sp, color = TextPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "vs 上周 ${(lastWeekRate * 100).toInt()}%", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${changeSign}${(rateChange * 100).toInt()}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = changeColor
                )
            }
        }
    }
}

// ==================== 平均完成周期（Cycle Time）====================
@Composable
private fun CycleTimeTrendChart(data: List<WeeklyCycleTimeData>, onNavigateToDetail: (String) -> Unit = {}) {
    val latestDays = data.lastOrNull()?.avgDays ?: 0f
    val previousDays = if (data.size >= 2) data[data.size - 2].avgDays else latestDays
    val dayChange = latestDays - previousDays
    // 周期下降是好事
    val trendColor = if (dayChange <= 0) Success else Warning
    val trendText = if (dayChange <= 0) "↘ 加速中" else "↗ 变慢了"

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "平均完成周期", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = "Cycle Time · 从创建到完成", fontSize = 12.sp, color = TextSecondary)
                }
                TrendLabelWithHelp(
                    arrow = if (dayChange <= 0) "↘" else "↗",
                    label = if (dayChange <= 0) "加速中" else "变慢了",
                    color = trendColor,
                    trendType = "cycletime",
                    onNavigateToDetail = onNavigateToDetail
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val maxDays = data.maxOfOrNull { it.avgDays }?.coerceAtLeast(1f) ?: 1f
            val primaryColor = Primary
            val borderColor = Border
            val warningColor = Warning
            val successBarColor = Success

            Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                val barWidth = (size.width / data.size) * 0.6f
                val spacing = size.width / data.size
                val heightScale = size.height * 0.85f / maxDays

                // 网格线
                for (i in 0..4) {
                    val y = size.height - (size.height / 4 * i)
                    drawLine(borderColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                }

                // 柱状图
                data.forEachIndexed { index, d ->
                    val x = index * spacing + (spacing - barWidth) / 2
                    val barHeight = d.avgDays * heightScale
                    val y = size.height - barHeight
                    val barColor = if (d.avgDays <= maxDays * 0.5f) successBarColor else
                        if (d.avgDays <= maxDays * 0.75f) primaryColor else warningColor
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                    )
                }

                // 趋势线
                if (data.size >= 2) {
                    val trendPath = Path()
                    data.forEachIndexed { index, d ->
                        val x = index * spacing + spacing / 2
                        val y = size.height - (d.avgDays * heightScale)
                        if (index == 0) trendPath.moveTo(x, y) else trendPath.lineTo(x, y)
                    }
                    drawPath(
                        trendPath, warningColor.copy(alpha = 0.6f),
                        style = Stroke(width = 2f, cap = StrokeCap.Round,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 横轴标签
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val step = (data.size - 1) / 5.coerceAtLeast(1)
                for (i in 0..5) {
                    val idx = (i * step).coerceAtMost(data.size - 1)
                    Text(text = data[idx].weekLabel, fontSize = 10.sp, color = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 底部统计
            Text(
                text = "本周平均 ${"%.1f".format(latestDays)} 天",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
    }
}

// ==================== 累积流图（Cumulative Flow Diagram）====================
@Composable
private fun CumulativeFlowChart(data: List<CumulativeFlowData>, onNavigateToDetail: (String) -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "累积流图", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = "Cumulative Flow · 看板任务流转", fontSize = 12.sp, color = TextSecondary)
                }
                val latest = data.lastOrNull()
                val first = data.firstOrNull()
                val cfdTrend = if (latest != null && first != null) {
                    if (latest.overdue > first.overdue + 2) Triple("↗", "堆积", Danger)
                    else if (latest.completed > first.completed) Triple("↗", "健康", Success)
                    else Triple("→", "平稳", TextMuted)
                } else Triple("→", "平稳", TextMuted)
                TrendLabelWithHelp(
                    arrow = cfdTrend.first,
                    label = cfdTrend.second,
                    color = cfdTrend.third,
                    trendType = "cfd",
                    onNavigateToDetail = onNavigateToDetail
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val successColor = Success
            val dangerColor = Danger
            val borderColor = Border
            val mutedColor = TextMuted

            Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                val maxTotal = data.maxOfOrNull { it.completed + it.overdue + it.pending }?.toFloat()?.coerceAtLeast(1f) ?: 1f
                val pointSpacing = size.width / (data.size - 1).coerceAtLeast(1)
                val heightScale = size.height / maxTotal

                // 网格线
                for (i in 0..4) {
                    val y = size.height - (size.height / 4 * i)
                    drawLine(borderColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                }

                // 待办区域（最上层，灰色）
                val pendingPath = Path()
                pendingPath.moveTo(0f, size.height)
                data.forEachIndexed { index, d ->
                    val x = index * pointSpacing
                    val totalH = (d.completed + d.overdue + d.pending) * heightScale
                    pendingPath.lineTo(x, size.height - totalH)
                }
                pendingPath.lineTo((data.size - 1) * pointSpacing, size.height)
                pendingPath.close()
                drawPath(pendingPath, mutedColor.copy(alpha = 0.15f))

                // 逾期区域（中间层，红色）
                val overduePath = Path()
                overduePath.moveTo(0f, size.height)
                data.forEachIndexed { index, d ->
                    val x = index * pointSpacing
                    val h = (d.completed + d.overdue) * heightScale
                    overduePath.lineTo(x, size.height - h)
                }
                overduePath.lineTo((data.size - 1) * pointSpacing, size.height)
                overduePath.close()
                drawPath(overduePath, dangerColor.copy(alpha = 0.25f))

                // 已完成区域（最下层，绿色）
                val completedPath = Path()
                completedPath.moveTo(0f, size.height)
                data.forEachIndexed { index, d ->
                    val x = index * pointSpacing
                    val h = d.completed * heightScale
                    completedPath.lineTo(x, size.height - h)
                }
                completedPath.lineTo((data.size - 1) * pointSpacing, size.height)
                completedPath.close()
                drawPath(completedPath, successColor.copy(alpha = 0.4f))

                // 各层边界线
                // 已完成上边界
                val completedLine = Path()
                data.forEachIndexed { index, d ->
                    val x = index * pointSpacing
                    val y = size.height - (d.completed * heightScale)
                    if (index == 0) completedLine.moveTo(x, y) else completedLine.lineTo(x, y)
                }
                drawPath(completedLine, successColor, style = Stroke(2f, cap = StrokeCap.Round))

                // 逾期上边界
                val overdueLine = Path()
                data.forEachIndexed { index, d ->
                    val x = index * pointSpacing
                    val y = size.height - ((d.completed + d.overdue) * heightScale)
                    if (index == 0) overdueLine.moveTo(x, y) else overdueLine.lineTo(x, y)
                }
                drawPath(overdueLine, dangerColor, style = Stroke(1.5f, cap = StrokeCap.Round))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 横轴标签
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val step = (data.size - 1) / 5.coerceAtLeast(1)
                for (i in 0..5) {
                    val idx = (i * step).coerceAtMost(data.size - 1)
                    val d = data[idx].date
                    Text(text = "${d.monthValue}/${d.dayOfMonth}", fontSize = 10.sp, color = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 图例
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp, 6.dp).background(Success.copy(alpha = 0.4f), RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("已完成", fontSize = 11.sp, color = TextSecondary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp, 6.dp).background(Danger.copy(alpha = 0.25f), RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("逾期中", fontSize = 11.sp, color = TextSecondary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp, 6.dp).background(TextMuted.copy(alpha = 0.15f), RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("待办", fontSize = 11.sp, color = TextSecondary)
                }
            }

            // 解读提示
            Spacer(modifier = Modifier.height(8.dp))
            val latest = data.lastOrNull()
            val first = data.firstOrNull()
            if (latest != null && first != null) {
                val completedGrowth = latest.completed - first.completed
                val hint = when {
                    latest.overdue > first.overdue + 2 -> "⚠️ 逾期层变厚，需关注任务按时完成"
                    completedGrowth > 0 && latest.overdue <= 2 -> "✅ 完成层稳步增长，流转健康"
                    else -> "📊 任务流转平稳，保持节奏"
                }
                Text(text = hint, fontSize = 12.sp, color = TextSecondary)
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
        colors = CardDefaults.cardColors(containerColor = BgCard),
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
                    Border,
                    Primary.copy(alpha = 0.2f),
                    Primary.copy(alpha = 0.4f),
                    Primary.copy(alpha = 0.7f),
                    Primary
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
                                0 -> Border
                                1 -> Primary.copy(alpha = 0.2f)
                                2 -> Primary.copy(alpha = 0.4f)
                                3 -> Primary.copy(alpha = 0.7f)
                                4 -> Primary
                                else -> Border
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
        colors = CardDefaults.cardColors(containerColor = BgCard),
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
                    currentBacklog >= threshold -> Triple("⚠️ 预警", Danger, Danger.copy(alpha = 0.08f))
                    currentBacklog >= threshold * 0.7 -> Triple("⚡ 注意", Warning, Warning.copy(alpha = 0.08f))
                    else -> Triple("✅ 健康", Success, Success.copy(alpha = 0.08f))
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
                                        backlogChange > 0 -> Danger
                                        backlogChange < 0 -> Success
                                        else -> TextMuted
                                    }
                                )
                                Text(
                                    text = if (backlogChange >= 0) "+$backlogChange" else "$backlogChange",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        backlogChange > 0 -> Danger
                                        backlogChange < 0 -> Success
                                        else -> TextMuted
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
            val dangerColor = Danger
            val warningColor = Warning
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
                    color = dangerColor,
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
                    dangerColor.copy(alpha = 0.3f)  // 深红
                } else if (colorIntensity > 0.5f) {
                    warningColor.copy(alpha = 0.3f)  // 橙色
                } else {
                    warningColor.copy(alpha = 0.3f)  // 浅橙
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
                    color = warningColor,
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
                LegendItem(color = Warning, label = "积压量")
                Spacer(modifier = Modifier.width(16.dp))
                LegendItem(color = Primary, label = "新增任务", isDashed = true)
                Spacer(modifier = Modifier.width(16.dp))
                LegendItem(color = Danger, label = "预警线(${threshold}个)", isDashed = true)
            }

            // 当前积压警告
            val currentBacklog = backlogData.lastOrNull()?.backlogCount ?: 0
            if (currentBacklog > threshold) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Danger.copy(alpha = 0.1f),
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
                        color = Danger,
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
        colors = CardDefaults.cardColors(containerColor = BgCard),
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
            val borderColor = Border
            val successColor = Success
            val dangerColor = Danger
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
                    color = borderColor,
                    start = Offset(0f, centerY),
                    end = Offset(size.width, centerY),
                    strokeWidth = 2f
                )

                // 绘制柱状图
                velocityData.forEachIndexed { index, data ->
                    val x = index * barSpacing + (barSpacing - barWidth) / 2
                    val barHeight = kotlin.math.abs(data.acceleration) * scale
                    val color = if (data.isAcceleration) successColor else dangerColor

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
                                color = Success
                            )
                            Text(
                                text = "+${it.acceleration}个",
                                fontSize = 13.sp,
                                color = Success
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
                                color = Danger
                            )
                            Text(
                                text = "${it.acceleration}个",
                                fontSize = 13.sp,
                                color = Danger
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
    stats: TimeHeatmapStats?,
    selectedTimeRange: OverviewTimeRange,
    onTimeRangeSelected: (OverviewTimeRange) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题行 + 时间维度选择器
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "黄金工作时段分析",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(BgSecondary)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    OverviewTimeRange.values().forEach { timeRange ->
                        val isSelected = selectedTimeRange == timeRange
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) Primary else BgCard)
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
                            0 -> BgSecondary  // 灰白
                            1 -> Primary.copy(alpha = 0.2f)  // 浅蓝
                            2 -> Primary.copy(alpha = 0.4f)  // 蓝色
                            3 -> Primary.copy(alpha = 0.7f)  // 深蓝
                            else -> Primary  // 最深蓝
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .background(color, RoundedCornerShape(4.dp))
                                .border(
                                    0.5.dp,
                                    Border,
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
                            BgSecondary,
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
private fun ProcrastinationRadarCard(
    radarData: ProcrastinationRadarData,
    selectedTimeRange: OverviewTimeRange,
    onTimeRangeSelected: (OverviewTimeRange) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题行 + 时间维度选择器
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "综合效率评分",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(BgSecondary)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    OverviewTimeRange.values().forEach { timeRange ->
                        val isSelected = selectedTimeRange == timeRange
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) Primary else BgCard)
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

            Text(
                text = "六维度效率评估，一图看懂综合表现",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 雷达图（六维度，含维度标签 + 刻度值 + 蓝色渐变填充）
            val primaryColor = Primary
            val primaryDarkColor = PrimaryDark
            val textMutedColor = TextMuted
            val textPrimaryColor = TextPrimary
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                val centerX = size.width / 2f
                val centerY = size.height / 2f
                val radius = minOf(size.width, size.height) * 0.28f
                val labelOffset = radius + 38.dp.toPx()

                val dimensions = listOf(
                    "准时完成" to radarData.onTimeRate,
                    "响应速度" to radarData.responseSpeed,
                    "完成时长" to radarData.completionSpeed,
                    "重要优先" to radarData.importantPriority,
                    "完成稳定" to radarData.completionStability,
                    "目标达成" to radarData.goalAchievementRate
                )

                // 对角线（从中心到顶点）
                dimensions.forEachIndexed { index, _ ->
                    val angle = (index * 60f - 90f) * (Math.PI / 180).toFloat()
                    drawLine(
                        color = textMutedColor.copy(alpha = 0.25f),
                        start = Offset(centerX, centerY),
                        end = Offset(centerX + radius * cos(angle), centerY + radius * sin(angle)),
                        strokeWidth = 1f
                    )
                }

                // 3层同心六边形（33%、66%、100%），由内到外颜色渐深
                val hexLayers = listOf(
                    Triple(0.33f, textMutedColor.copy(alpha = 0.15f), 1f),
                    Triple(0.66f, textMutedColor.copy(alpha = 0.25f), 1f),
                    Triple(1.0f,  textMutedColor.copy(alpha = 0.40f), 1.5f)
                )
                hexLayers.forEach { (scale, hexColor, strokeW) ->
                    val webPath = Path()
                    dimensions.forEachIndexed { index, _ ->
                        val angle = (index * 60f - 90f) * (Math.PI / 180).toFloat()
                        val x = centerX + radius * scale * cos(angle)
                        val y = centerY + radius * scale * sin(angle)
                        if (index == 0) webPath.moveTo(x, y) else webPath.lineTo(x, y)
                    }
                    webPath.close()
                    drawPath(
                        path = webPath,
                        color = hexColor,
                        style = Stroke(width = strokeW)
                    )
                }

                // 数据区域路径
                val dataPath = Path()
                dimensions.forEachIndexed { index, (_, value) ->
                    val angle = (index * 60f - 90f) * (Math.PI / 180).toFloat()
                    val distance = radius * (value / 100f).coerceIn(0f, 1f)
                    val x = centerX + distance * cos(angle)
                    val y = centerY + distance * sin(angle)
                    if (index == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
                }
                dataPath.close()

                // 数据区域填充（Primary 蓝色半透明）
                drawPath(
                    path = dataPath,
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.50f),
                            primaryColor.copy(alpha = 0.20f)
                        ),
                        center = Offset(centerX, centerY),
                        radius = radius
                    )
                )

                // 轮廓线（Primary 蓝色首尾相连）
                drawPath(
                    path = dataPath,
                    color = primaryColor,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // 端点圆点（蓝色外圆 + 白色内芯，value=0 时点在原点）
                dimensions.forEachIndexed { index, (_, value) ->
                    val angle = (index * 60f - 90f) * (Math.PI / 180).toFloat()
                    val distance = radius * (value / 100f).coerceIn(0f, 1f)
                    val dotCenter = Offset(
                        centerX + distance * cos(angle),
                        centerY + distance * sin(angle)
                    )
                    drawCircle(color = primaryColor, radius = 5.dp.toPx(), center = dotCenter)
                    drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = dotCenter)
                }

                // 刻度值 + 维度标签（通过 drawContext.canvas.nativeCanvas 绘制文字）
                val nativeCanvas = drawContext.canvas.nativeCanvas

                // 刻度值（沿顶轴右侧偏移，避免与轴线重叠）
                val scalePaint = android.graphics.Paint().apply {
                    textSize = 9.sp.toPx()
                    color = textMutedColor.copy(alpha = 0.85f).toArgb()
                    textAlign = android.graphics.Paint.Align.LEFT
                    isAntiAlias = true
                }
                val topAngle = (-90f * Math.PI / 180).toFloat()
                listOf(0.33f to "0", 0.66f to "50", 1f to "100").forEach { (scale, label) ->
                    val sx = centerX + radius * scale * cos(topAngle) + 6.dp.toPx()
                    val sy = centerY + radius * scale * sin(topAngle) + scalePaint.textSize * 0.35f
                    nativeCanvas.drawText(label, sx, sy, scalePaint)
                }

                // 维度标签（各顶点外侧，居中对齐）
                val labelPaint = android.graphics.Paint().apply {
                    textSize = 11.sp.toPx()
                    color = textPrimaryColor.toArgb()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                    isFakeBoldText = true
                }
                dimensions.forEachIndexed { index, (label, _) ->
                    val angle = (index * 60f - 90f) * (Math.PI / 180).toFloat()
                    val lx = centerX + labelOffset * cos(angle)
                    val ly = centerY + labelOffset * sin(angle) + labelPaint.textSize * 0.35f
                    nativeCanvas.drawText(label, lx, ly, labelPaint)
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
                    Text(radarData.strongestDimension, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Success)
                    Text("${String.format("%.0f", radarData.strongestScore)}%", fontSize = 11.sp, color = Success)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚠️ 待提升", fontSize = 11.sp, color = TextMuted)
                    Text(radarData.weakestDimension, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Warning)
                    Text("${String.format("%.0f", radarData.weakestScore)}%", fontSize = 11.sp, color = Warning)
                }
            }
        }
    }
}

// ==================== 效率周对比卡 ====================
@Composable
private fun EfficiencyComparisonCard(uiState: StatsUiState) {
    val comparison = uiState.weekComparison ?: return
    val radar = uiState.procrastinationRadar

    // 4个核心效率指标：日均完成、准时率、平均速度、高效时段利用
    val thisWeekDailyAvg = if (comparison.thisWeekTotalTasks > 0)
        comparison.thisWeekCompleted.toFloat() / 7f else 0f
    val lastWeekDailyAvg = if (comparison.lastWeekTotalTasks > 0)
        comparison.lastWeekCompleted.toFloat() / 7f else 0f

    val onTimeRate = radar?.onTimeRate ?: 0f
    val responseSpeed = radar?.responseSpeed ?: 0f

    // 高效时段利用率：基于黄金时段完成数占总完成数的比例
    val goldenSlotRate = radar?.goalAchievementRate ?: 0f

    data class MetricItem(
        val icon: String,
        val name: String,
        val thisWeek: String,
        val change: Float, // 正=好
        val isHigherBetter: Boolean = true
    )

    val metrics = listOf(
        MetricItem("📊", "日均完成", "${"%.1f".format(thisWeekDailyAvg)}个",
            thisWeekDailyAvg - lastWeekDailyAvg),
        MetricItem("⏰", "准时率", "${onTimeRate.toInt()}%",
            comparison.completionRateChange),
        MetricItem("⚡", "响应速度", "${responseSpeed.toInt()}分",
            responseSpeed - 50f),  // 以50为基准
        MetricItem("🎯", "目标达成", "${goldenSlotRate.toInt()}%",
            goldenSlotRate - 50f)
    )

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(text = "效率周对比", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(text = "本周 vs 上周核心效率指标变化", fontSize = 12.sp, color = TextSecondary)

            Spacer(modifier = Modifier.height(16.dp))

            // 2×2 网格
            for (row in metrics.chunked(2)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { item ->
                        val isPositive = if (item.isHigherBetter) item.change >= 0 else item.change <= 0
                        val changeColor = if (isPositive) Success else Danger
                        val arrow = if (item.change >= 0) "↗" else "↘"

                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = BgSecondary)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = item.icon, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = item.name, fontSize = 13.sp, color = TextSecondary)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = item.thisWeek,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "$arrow ${if (item.change >= 0) "+" else ""}${"%.1f".format(item.change)}",
                                    fontSize = 12.sp,
                                    color = changeColor,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

// ==================== 效率建议卡 ====================
@Composable
private fun EfficiencyAdviceCard(uiState: StatsUiState) {
    val radar = uiState.procrastinationRadar ?: return
    val stats = uiState.timeHeatmapStats

    // 基于数据自动生成建议
    val advices = mutableListOf<Pair<String, String>>() // icon to text

    // 1. 最弱维度建议
    when (radar.weakestDimension) {
        "准时完成" -> advices.add("⏰" to "准时率偏低(${radar.weakestScore.toInt()}%)，建议为任务预留缓冲时间，截止日期提前1天设置")
        "响应速度" -> advices.add("🚀" to "响应速度较慢(${radar.weakestScore.toInt()}%)，建议收到任务后24小时内开始行动，避免拖延")
        "完成时长" -> advices.add("⚡" to "完成时长偏长(${radar.weakestScore.toInt()}%)，建议拆分大任务为小步骤，每步控制在2小时内")
        "重要优先" -> advices.add("🔥" to "重要任务优先度不足(${radar.weakestScore.toInt()}%)，建议每天先处理1个重要紧急任务再做其他")
        "完成稳定" -> advices.add("📊" to "完成量波动较大(${radar.weakestScore.toInt()}%)，建议设定每日固定完成数量目标，保持节奏稳定")
        "目标达成" -> advices.add("🎯" to "目标达成率偏低(${radar.weakestScore.toInt()}%)，建议减少任务创建量，聚焦已有任务完成")
    }

    // 2. 黄金时段建议
    stats?.mostProductiveSlot?.let { (day, slot) ->
        val dayName = listOf("", "周一", "周二", "周三", "周四", "周五", "周六", "周日")[day]
        advices.add("🏆" to "你在${dayName}${slot.displayName}效率最高，建议将重要任务优先安排在这个时段")
    }

    // 3. 综合分建议
    if (radar.totalScore < 60) {
        advices.add("💡" to "综合效率分${radar.totalScore}分，建议先聚焦提升「${radar.weakestDimension}」这一项，逐步突破")
    } else if (radar.totalScore >= 80) {
        advices.add("🌟" to "综合效率${radar.totalScore}分，表现优秀！继续保持，同时关注「${radar.weakestDimension}」的进一步提升")
    }

    if (advices.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(text = "效率提升建议", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(text = "基于你的数据自动生成的可执行建议", fontSize = 12.sp, color = TextSecondary)

            Spacer(modifier = Modifier.height(14.dp))

            advices.forEachIndexed { index, (icon, text) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (index == 0) Primary.copy(alpha = 0.08f) else BgSecondary,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(text = icon, fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = text,
                        fontSize = 13.sp,
                        color = TextPrimary,
                        lineHeight = 20.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (index < advices.size - 1) Spacer(modifier = Modifier.height(8.dp))
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
        colors = CardDefaults.cardColors(containerColor = Warning.copy(alpha = 0.08f)),  // 浅橙色背景
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
                    color = Warning
                )
            }

            Text(
                text = "量化拖延造成的时间损失",
                fontSize = 12.sp,
                color = TextSecondary,
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
                        color = TextSecondary
                    )
                    Text(
                        text = "${delayData.totalDelayDays}天",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Danger
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
                        color = TextSecondary
                    )
                    Text(
                        text = "${String.format("%.1f", delayData.avgDelayPerTask)}天",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Danger
                    )
                }
            }

            if (delayData.mostDelayedDays > 0) {
                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Danger.copy(alpha = 0.15f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = "💸 拖延最严重的任务",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Danger
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = delayData.mostDelayedTask,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "延迟 ${delayData.mostDelayedDays} 天完成",
                        fontSize = 13.sp,
                        color = Danger,
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
        colors = CardDefaults.cardColors(containerColor = BgCard),
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
                            Danger.copy(alpha = 0.08f),
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
                        color = Danger
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
                            Warning.copy(alpha = 0.08f),
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
                        color = Warning,
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
                color = Border,
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

// ══════════════════════════════════════════
//  AI 洞察页面
// ══════════════════════════════════════════

@Composable
private fun AIInsightContent(
    uiState: StatsUiState,
    viewModel: StatsViewModel
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        AIActionCard(
            title = "行为模式分析",
            description = "从完成时间、延期、分类和节奏里找出稳定模式。",
            accent = Color(0xFF4F63E7),
            actionText = if (uiState.isAnalyzingBehavior) "分析中..." else "分析我的行为模式",
            loading = uiState.isAnalyzingBehavior,
            onAction = { viewModel.analyzeBehavior() }
        ) {
            val insight = uiState.behaviorInsight
            if (insight == null) {
                AIEmptyHint("分析后会显示高效时段、易积压节点和可复用习惯。")
            } else {
                if (insight.patterns.isNotEmpty()) {
                    AIResultBlock(
                        title = "发现的模式",
                        items = insight.patterns,
                        accent = Color(0xFF4F63E7)
                    )
                }
                if (insight.suggestions.isNotEmpty()) {
                    AIResultBlock(
                        title = "改进建议",
                        items = insight.suggestions,
                        accent = Success
                    )
                }
            }
        }

        AIActionCard(
            title = "本周周报",
            description = "把本周表现整理成摘要、亮点、待改进和下周建议。",
            accent = Color(0xFF0F9DB2),
            actionText = if (uiState.isGeneratingReport) "生成中..." else "生成本周周报",
            loading = uiState.isGeneratingReport,
            onAction = { viewModel.generateWeeklyReport() }
        ) {
            val report = uiState.weeklyReport
            if (report == null) {
                AIEmptyHint("生成后会在这里展示可复制的周报内容。")
            } else {
                Surface(
                    color = Color(0xFF0F9DB2).copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(report.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F9DB2))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(report.summary, fontSize = 13.sp, color = TextPrimary, lineHeight = 19.sp)
                    }
                }

                AIResultBlock("本周亮点", report.highlights, Warning)
                AIResultBlock("待改进", report.improvements, Color(0xFFFF9800))
                AIResultBlock("下周建议", report.nextWeekSuggestions, Success)

                OutlinedButton(
                    onClick = {
                        val exportText = viewModel.exportWeeklyReport() ?: return@OutlinedButton
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("周报", exportText))
                        android.widget.Toast.makeText(context, "周报已复制到剪贴板", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF0F9DB2))
                ) {
                    Text("复制周报到剪贴板", color = Color(0xFF0F9DB2))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun AIActionCard(
    title: String,
    description: String,
    accent: Color,
    actionText: String,
    loading: Boolean,
    onAction: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(1.dp, Border.copy(alpha = 0.65f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(description, fontSize = 12.sp, lineHeight = 18.sp, color = TextSecondary)
                }
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
            }

            Button(
                onClick = onAction,
                enabled = !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    disabledContainerColor = accent.copy(alpha = 0.52f)
                )
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(actionText, color = Color.White, fontWeight = FontWeight.Bold)
            }

            content()
        }
    }
}

@Composable
private fun AIEmptyHint(text: String) {
    Surface(
        color = BgSecondary,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            fontSize = 12.sp,
            color = TextSecondary,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun AIResultBlock(
    title: String,
    items: List<String>,
    accent: Color
) {
    if (items.isEmpty()) return

    Surface(
        color = accent.copy(alpha = 0.08f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = accent)
            items.forEach { item ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .padding(top = 7.dp)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(accent)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(item, fontSize = 13.sp, color = TextPrimary, lineHeight = 19.sp)
                }
            }
        }
    }
}
