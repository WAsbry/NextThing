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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ContentCopy
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
import com.nextthing.app.data.service.AIRouteMode
import com.nextthing.app.presentation.theme.*
import com.nextthing.app.presentation.components.TaskItemCard
import com.nextthing.app.presentation.components.CategoryIconView
import androidx.compose.foundation.lazy.items
import java.time.format.DateTimeFormatter
import kotlin.math.*

private val StatsSoftStart = Color(0xFFF4EFFF)
private val StatsSoftMid = Color(0xFFF7F3FF)
private val StatsSoftEnd = Color(0xFFFBFAFF)
private val StatsInk = Color(0xFF202331)
private val StatsDeep = Color(0xFF2F2850)
private val StatsSub = Color(0xFF656B78)
private val StatsMuted = Color(0xFFA6ACB8)
private val StatsLine = Color(0xFFEEF0F5)

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
    onNavigateToStatsSection: (String, OverviewTimeRange) -> Unit = { _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
    ) {
        StatsTopBar(
            selectedTimeRange = uiState.selectedOverviewTimeRange,
            onTimeRangeSelected = viewModel::selectOverviewTimeRange
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item { StatsHomeContent(uiState, onNavigateToStatsSection) }
        }
    }
}

@Composable
private fun StatsTopBar(
    selectedTimeRange: OverviewTimeRange,
    onTimeRangeSelected: (OverviewTimeRange) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = BgCard,
        shadowElevation = 0.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "统计",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.weight(1f))

                OverviewTimeRangeChips(
                    selectedTimeRange = selectedTimeRange,
                    onTimeRangeSelected = onTimeRangeSelected,
                    modifier = Modifier.width(156.dp)
                )
            }
            HorizontalDivider(thickness = 0.5.dp, color = Border)
        }
    }
}

@Composable
fun StatsStructureScreen(
    viewModel: StatsViewModel = hiltViewModel(),
    initialTimeRange: OverviewTimeRange = OverviewTimeRange.TODAY,
    onBackPressed: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(initialTimeRange) {
        viewModel.selectCategoryTimeRange(initialTimeRange)
    }
    val categoryRangeReady = uiState.selectedCategoryTimeRange == initialTimeRange
    val showEmptyState = categoryRangeReady && uiState.categoryStats.isEmpty()

    StatsDetailScaffold(
        title = "任务结构",
        subtitle = null,
        contentTopPadding = 5.dp,
        contentSpacing = 5.dp,
        centerContent = showEmptyState,
        onBackPressed = onBackPressed
    ) {
        if (categoryRangeReady) {
            if (showEmptyState) {
                CategoryStructureEmptyState(uiState.selectedCategoryTimeRange)
            } else {
                CategoryContent(uiState = uiState)
            }
        }
    }
}

@Composable
fun StatsTrendScreen(
    viewModel: StatsViewModel = hiltViewModel(),
    initialTimeRange: OverviewTimeRange = OverviewTimeRange.TODAY,
    onBackPressed: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(initialTimeRange) {
        viewModel.selectOverviewTimeRange(initialTimeRange)
        viewModel.selectTrendTimeRange(initialTimeRange)
    }
    val rangeReady = uiState.selectedOverviewTimeRange == initialTimeRange &&
        uiState.selectedTrendTimeRange == initialTimeRange
    val comparison = uiState.weekComparison
    val showEmptyState = rangeReady && comparison != null &&
        comparison.thisWeekTotalTasks == 0 && comparison.lastWeekTotalTasks == 0

    StatsDetailScaffold(
        title = "趋势分析",
        subtitle = null,
        contentTopPadding = 5.dp,
        contentSpacing = 5.dp,
        centerContent = showEmptyState,
        onBackPressed = onBackPressed
    ) {
        if (rangeReady) {
            if (showEmptyState) {
                TrendEmptyState(initialTimeRange)
            } else {
                TrendContent(uiState = uiState)
            }
        }
    }
}

@Composable
fun StatsEfficiencyScreen(
    viewModel: StatsViewModel = hiltViewModel(),
    initialTimeRange: OverviewTimeRange = OverviewTimeRange.TODAY,
    onBackPressed: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(initialTimeRange) {
        viewModel.selectOverviewTimeRange(initialTimeRange)
        viewModel.selectEfficiencyTimeRange(initialTimeRange)
    }
    val rangeReady = uiState.selectedOverviewTimeRange == initialTimeRange &&
        uiState.selectedEfficiencyTimeRange == initialTimeRange
    val hasBasicData = uiState.efficiencySummary.completedWithDeadlineCount >= 3

    StatsDetailScaffold(
        title = "效率诊断",
        subtitle = null,
        trailingText = initialTimeRange.displayName,
        contentTopPadding = 5.dp,
        contentSpacing = 5.dp,
        centerContent = rangeReady && !hasBasicData,
        onBackPressed = onBackPressed
    ) {
        if (rangeReady) {
            if (hasBasicData) {
                EfficiencyContent(uiState = uiState)
            } else {
                EfficiencyInsufficientState(uiState.efficiencySummary)
            }
        }
    }
}

@Composable
fun StatsAIReportScreen(
    viewModel: StatsViewModel = hiltViewModel(),
    onBackPressed: () -> Unit,
    onNavigateToAIConfig: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.prepareAIReportContext()
    }
    val centerContent = !uiState.aiReportContextLoaded ||
        uiState.isAIReportContextLoading ||
        uiState.isGeneratingReport ||
        (uiState.weeklyReport == null && uiState.aiReportWeekTaskCount == 0)

    StatsDetailScaffold(
        title = "AI 周报",
        subtitle = null,
        trailingText = "本周",
        contentTopPadding = 5.dp,
        contentSpacing = 5.dp,
        centerContent = centerContent,
        onBackPressed = onBackPressed
    ) {
        AIWeeklyReportContent(
            uiState = uiState,
            onGenerate = viewModel::generateWeeklyReport,
            onNavigateToAIConfig = onNavigateToAIConfig,
            onRetryContext = viewModel::prepareAIReportContext,
            exportWeeklyReport = viewModel::exportWeeklyReport
        )
    }
}

@Composable
private fun StatsDetailScaffold(
    title: String,
    subtitle: String?,
    onBackPressed: () -> Unit,
    trailingText: String? = null,
    contentTopPadding: androidx.compose.ui.unit.Dp = 12.dp,
    contentSpacing: androidx.compose.ui.unit.Dp = 16.dp,
    centerContent: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = BgCard,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackPressed) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = TextPrimary
                    )
                }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (!trailingText.isNullOrBlank()) {
                Text(
                    text = trailingText,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(end = 10.dp)
                )
            }
            }
        }

        if (centerContent) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(contentSpacing),
                    content = content
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(top = contentTopPadding, bottom = 24.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(contentSpacing),
                        content = content
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsImmersiveHeader(
    uiState: StatsUiState,
    selectedTab: StatsTab,
    onTabSelected: (StatsTab) -> Unit,
    showTabs: Boolean = true
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
                        StatsSoftStart,
                        StatsSoftMid,
                        StatsSoftEnd
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = Color(0xFF7057F5).copy(alpha = 0.10f),
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
                                    listOf(Primary, Color(0xFFB06DFF))
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

                    Text(
                        text = "统计",
                        color = StatsDeep,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = Color.White.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, StatsLine)
                ) {
                    Text(
                        text = header.badge,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        color = Primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (showTabs) {
                StatsTabRow(
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected,
                    immersive = true
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.76f))
                    .border(1.dp, StatsLine, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = header.title,
                    color = Primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = header.label,
                            color = StatsSub,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = header.value,
                            color = StatsDeep,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = 32.sp
                        )
                    }
                    Surface(
                        color = Primary.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, Primary.copy(alpha = 0.16f))
                    ) {
                        Text(
                            text = header.state,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            color = Primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = header.summary,
                    color = StatsSub,
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
            title = "执行概览",
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
            title = "任务结构",
            label = "结构判断",
            value = "分布与变化",
            badge = "${uiState.categoryStats.size} 类",
            state = "看结构",
            summary = "这里把分类分布和趋势变化放在一起，重点看任务投入是否偏科，以及执行结构是在变好还是变差。",
            signals = listOf(
                "分类数" to "${uiState.categoryStats.size}",
                "日趋势" to "${uiState.weeklyTrend.size}",
                "排行" to "${uiState.categoryEfficiencyRanking.size}"
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
            .background(StatsSoftMid.copy(alpha = 0.72f))
            .border(1.dp, StatsLine, RoundedCornerShape(10.dp))
            .padding(8.dp)
    ) {
        Text(
            text = label,
            color = StatsSub,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            color = StatsDeep,
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
    onTimeRangeSelected: (OverviewTimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(BgCard)
            .border(1.dp, Border, RoundedCornerShape(8.dp))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        listOf(
            OverviewTimeRange.TODAY,
            OverviewTimeRange.THIS_WEEK,
            OverviewTimeRange.THIS_MONTH
        ).forEach { range ->
            val selected = selectedTimeRange == range
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (selected) Primary.copy(alpha = 0.10f) else Color.Transparent)
                    .clickable { onTimeRangeSelected(range) }
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = range.displayName,
                    color = if (selected) Primary else StatsSub,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
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
            containerColor = if (immersive) Color.White.copy(alpha = 0.62f) else BgCard
        ),
        border = if (immersive) BorderStroke(1.dp, StatsLine) else null
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
                                immersive && isSelected -> Primary
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
                            immersive && isSelected -> Color.White
                            immersive -> StatsSub
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

// ==================== 统计首页 ====================
@Composable
private fun StatsHomeContent(
    uiState: StatsUiState,
    onNavigateToStatsSection: (String, OverviewTimeRange) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 5.dp),
        verticalArrangement = Arrangement.Top
    ) {
        ExecutionSummaryCard(uiState)

        Spacer(modifier = Modifier.height(10.dp))

        StatsAnalysisEntryGrid(
            selectedTimeRange = uiState.selectedOverviewTimeRange,
            onNavigateToStatsSection = onNavigateToStatsSection
        )
    }
}

@Composable
private fun StatsAnalysisEntryGrid(
    selectedTimeRange: OverviewTimeRange,
    onNavigateToStatsSection: (String, OverviewTimeRange) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = "专项分析",
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        StatsAnalysisEntryCard(
            title = "任务结构",
            description = "查看分类分布与任务构成",
            onClick = { onNavigateToStatsSection("structure", selectedTimeRange) }
        )
        StatsAnalysisEntryCard(
            title = "趋势分析",
            description = "查看完成趋势与任务积压变化",
            onClick = { onNavigateToStatsSection("trend", selectedTimeRange) }
        )
        StatsAnalysisEntryCard(
            title = "效率诊断",
            description = "分析拖延、活跃时段与执行效率",
            onClick = { onNavigateToStatsSection("efficiency", selectedTimeRange) }
        )
        StatsAnalysisEntryCard(
            title = "AI 周报",
            description = "汇总行为变化与改进建议",
            onClick = { onNavigateToStatsSection("ai", selectedTimeRange) }
        )
    }
}

@Composable
private fun StatsAnalysisEntryCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(1.dp, Border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = description,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "进入$title",
                tint = TextMuted,
                modifier = Modifier.size(22.dp)
            )
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

        OverviewTimeRangeChips(
            selectedTimeRange = uiState.selectedOverviewTimeRange,
            onTimeRangeSelected = { viewModel.selectOverviewTimeRange(it) }
        )

        OverviewMetricStrip(uiState = uiState)

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
private fun ExecutionSummaryCard(uiState: StatsUiState) {
    val rangeName = uiState.selectedOverviewTimeRange.displayName
    val previousRangeName = when (uiState.selectedOverviewTimeRange) {
        OverviewTimeRange.TODAY -> "昨日"
        OverviewTimeRange.THIS_WEEK -> "上周"
        OverviewTimeRange.THIS_MONTH -> "上月"
        OverviewTimeRange.ALL -> "上期"
    }
    val comparison = uiState.weekComparison
    val comparisonChange = comparison?.completionRateChange ?: 0f
    val hasComparison = comparison != null && comparison.lastWeekTotalTasks > 0
    val comparisonText = when {
        !hasComparison -> "暂无对比"
        comparisonChange > 0.5f -> "较$previousRangeName ↑${comparisonChange.roundToInt()}个百分点"
        comparisonChange < -0.5f -> "较$previousRangeName ↓${abs(comparisonChange).roundToInt()}个百分点"
        else -> "较$previousRangeName 持平"
    }
    val comparisonColor = when {
        !hasComparison -> TextMuted
        comparisonChange > 0.5f -> Success
        comparisonChange < -0.5f -> Danger
        else -> TextMuted
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(1.dp, Border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${rangeName}执行",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = comparisonText,
                    color = comparisonColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (uiState.overviewTotalTasks == 0) {
                Text(
                    text = "${rangeName}暂无任务",
                    color = TextSecondary,
                    fontSize = 15.sp
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "${uiState.overviewCompletionRate.toInt()}%",
                        color = Primary,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "已完成 ${uiState.overviewCompletedTasks} / ${uiState.overviewTotalTasks} 项",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "逾期 ${uiState.coreMetricOverdue} 项",
                        color = Danger,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "重要紧急 ${uiState.coreMetricImportantUrgent} 项",
                        color = Warning,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsTrendPreview(uiState: StatsUiState) {
    val recentTrend = uiState.overviewTrend.take(7).reversed()
    val maxCompleted = recentTrend.maxOfOrNull { it.completedCount }?.coerceAtLeast(1) ?: 1
    val hasCompletion = recentTrend.any { it.completedCount > 0 }
    val rangeLabel = when (uiState.selectedOverviewTimeRange) {
        OverviewTimeRange.TODAY -> "今日"
        OverviewTimeRange.THIS_WEEK -> "本周"
        OverviewTimeRange.THIS_MONTH -> "本月"
        OverviewTimeRange.ALL -> "全部"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(1.dp, Border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("完成趋势", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(rangeLabel, color = TextMuted, fontSize = 12.sp)
            }
            if (recentTrend.isEmpty() || !hasCompletion) {
                Text("该时间范围内暂无完成记录", color = TextMuted, fontSize = 13.sp)
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(88.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    recentTrend.forEach { day ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((day.completedCount.toFloat() / maxCompleted * 52f).coerceAtLeast(4f).dp)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(Primary.copy(alpha = if (day.completedCount > 0) 1f else 0.16f))
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(day.date.dayOfMonth.toString(), color = TextMuted, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewMetricStrip(uiState: StatsUiState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(1.dp, Border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(82.dp)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OverviewMetricItem("待办", uiState.coreMetricPending.toString(), Primary, Modifier.weight(1f))
            VerticalDivider(Modifier.height(42.dp), thickness = 0.5.dp, color = Border)
            OverviewMetricItem("逾期", uiState.coreMetricOverdue.toString(), Danger, Modifier.weight(1f))
            VerticalDivider(Modifier.height(42.dp), thickness = 0.5.dp, color = Border)
            OverviewMetricItem("重要紧急", uiState.coreMetricImportantUrgent.toString(), Warning, Modifier.weight(1f))
        }
    }
}

@Composable
private fun OverviewMetricItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 10.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = color,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
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

// ==================== 结构统计页面 ====================
@Composable
private fun StructureContent(
    uiState: StatsUiState,
    viewModel: StatsViewModel,
    onNavigateToTrendDetail: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CategoryContent(uiState = uiState)
        TrendContent(uiState = uiState)
    }
}

// ==================== 分类统计页面 ====================
@Composable
private fun CategoryContent(uiState: StatsUiState) {
    val categoryStats = uiState.categoryStats.values
        .sortedWith(compareByDescending<CategoryStatsData> { it.totalCount }.thenBy { it.category.displayName })

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (categoryStats.isNotEmpty()) {
            CategoryDistributionCard(
                categoryStats = categoryStats,
                selectedTimeRange = uiState.selectedCategoryTimeRange
            )
            CategoryPerformanceCard(categoryStats = categoryStats)
        }
    }
}

@Composable
private fun CategoryStructureEmptyState(selectedTimeRange: OverviewTimeRange) {
    Text(
        text = "${selectedTimeRange.displayName}暂无任务",
        color = TextSecondary,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun CategoryDistributionCard(
    categoryStats: List<CategoryStatsData>,
    selectedTimeRange: OverviewTimeRange
) {
    val totalTasks = categoryStats.sumOf { it.totalCount }.coerceAtLeast(1)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(1.dp, Border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "分类分布",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${selectedTimeRange.displayName} · 共 $totalTasks 项",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(BgSecondary)
            ) {
                categoryStats.forEach { stat ->
                    Box(
                        modifier = Modifier
                            .weight(stat.totalCount.toFloat())
                            .fillMaxHeight()
                            .background(stat.category.color.copy(alpha = 0.72f))
                    )
                }
            }

            categoryStats.forEachIndexed { index, stat ->
                CategoryDistributionRow(stat = stat, totalTasks = totalTasks)
                if (index != categoryStats.lastIndex) {
                    HorizontalDivider(thickness = 0.5.dp, color = Border)
                }
            }
        }
    }
}

@Composable
private fun CategoryDistributionRow(
    stat: CategoryStatsData,
    totalTasks: Int
) {
    val share = stat.totalCount.toFloat() / totalTasks

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(stat.category.color.copy(alpha = 0.72f), CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        CategoryInitialBadge(category = stat.category)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stat.category.displayName,
            modifier = Modifier.weight(1f),
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${stat.totalCount}项",
            color = TextSecondary,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "${(share * 100).roundToInt()}%",
            modifier = Modifier.width(36.dp),
            color = TextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun CategoryPerformanceCard(categoryStats: List<CategoryStatsData>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(1.dp, Border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "分类表现",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            categoryStats.forEachIndexed { index, stat ->
                CategoryPerformanceRow(stat = stat)
                if (index != categoryStats.lastIndex) {
                    HorizontalDivider(thickness = 0.5.dp, color = Border)
                }
            }
        }
    }
}

@Composable
private fun CategoryPerformanceRow(stat: CategoryStatsData) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryInitialBadge(category = stat.category)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stat.category.displayName,
                modifier = Modifier.weight(1f),
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "已完成 ${stat.completedCount} / ${stat.totalCount} 项",
                color = TextSecondary,
                fontSize = 13.sp
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "完成率 ${stat.completionRate.roundToInt()}%",
                color = TextSecondary,
                fontSize = 12.sp
            )
            Text(
                text = "逾期 ${stat.overdueCount} 项",
                color = if (stat.overdueCount > 0) Danger else TextMuted,
                fontSize = 12.sp,
                fontWeight = if (stat.overdueCount > 0) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun CategoryInitialBadge(category: Category) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(category.pastelColor.copy(alpha = 0.32f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = category.displayName.take(1),
            color = category.color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}



// ==================== 趋势统计页面 ====================
@Composable
private fun TrendContent(
    uiState: StatsUiState
) {
    val timeRange = uiState.selectedOverviewTimeRange
    val window = remember(timeRange) { buildTrendWindow(timeRange) }
    val currentExecution = remember(uiState.allWeeklyTrend, window) {
        uiState.allWeeklyTrend.filter { it.date in window.currentStart..window.currentEnd }
    }
    val previousExecution = remember(uiState.allWeeklyTrend, window) {
        uiState.allWeeklyTrend.filter { it.date in window.previousStart..window.previousEnd }
    }
    val currentOverdue = remember(uiState.overdueTrend, window) {
        uiState.overdueTrend.filter { it.date in window.currentStart..window.currentEnd }
    }
    val previousOverdue = remember(uiState.overdueTrend, window) {
        uiState.overdueTrend.filter { it.date in window.previousStart..window.previousEnd }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        ExecutionChangeCard(
            uiState = uiState,
            currentData = currentExecution,
            previousData = previousExecution,
            window = window
        )
        OverdueChangeCard(
            currentData = currentOverdue,
            previousData = previousOverdue,
            window = window
        )
    }
}

private data class TrendWindow(
    val currentStart: java.time.LocalDate,
    val currentEnd: java.time.LocalDate,
    val previousStart: java.time.LocalDate,
    val previousEnd: java.time.LocalDate,
    val previousLabel: String
)

private fun buildTrendWindow(timeRange: OverviewTimeRange): TrendWindow {
    val today = java.time.LocalDate.now()
    return when (timeRange) {
        OverviewTimeRange.TODAY -> TrendWindow(
            currentStart = today,
            currentEnd = today,
            previousStart = today.minusDays(1),
            previousEnd = today.minusDays(1),
            previousLabel = "昨日"
        )
        OverviewTimeRange.THIS_WEEK -> {
            val currentStart = today.with(java.time.DayOfWeek.MONDAY)
            val elapsedDays = java.time.temporal.ChronoUnit.DAYS.between(currentStart, today)
            val previousStart = currentStart.minusWeeks(1)
            TrendWindow(
                currentStart = currentStart,
                currentEnd = today,
                previousStart = previousStart,
                previousEnd = previousStart.plusDays(elapsedDays),
                previousLabel = "上周"
            )
        }
        OverviewTimeRange.THIS_MONTH -> {
            val currentStart = today.withDayOfMonth(1)
            val previousStart = currentStart.minusMonths(1)
            val previousLastDay = previousStart.withDayOfMonth(previousStart.lengthOfMonth())
            val candidatePreviousEnd = previousStart.plusDays((today.dayOfMonth - 1).toLong())
            val alignedPreviousEnd = if (candidatePreviousEnd.isAfter(previousLastDay)) {
                previousLastDay
            } else {
                candidatePreviousEnd
            }
            TrendWindow(
                currentStart = currentStart,
                currentEnd = today,
                previousStart = previousStart,
                previousEnd = alignedPreviousEnd,
                previousLabel = "上月"
            )
        }
        OverviewTimeRange.ALL -> TrendWindow(
            currentStart = today.minusDays(29),
            currentEnd = today,
            previousStart = today.minusDays(59),
            previousEnd = today.minusDays(30),
            previousLabel = "上一周期"
        )
    }
}

@Composable
private fun TrendEmptyState(timeRange: OverviewTimeRange) {
    Text(
        text = "${timeRange.displayName}暂无趋势数据",
        color = TextSecondary,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun ExecutionChangeCard(
    uiState: StatsUiState,
    currentData: List<DailyTrendData>,
    previousData: List<DailyTrendData>,
    window: TrendWindow
) {
    val comparison = uiState.weekComparison
    val change = comparison?.completionRateChange ?: 0f
    val hasPreviousData = comparison?.lastWeekTotalTasks?.let { it > 0 } == true
    val changeText = when {
        !hasPreviousData -> "暂无同期数据"
        change > 0.5f -> "较${window.previousLabel} ↑ ${change.roundToInt()} 个百分点"
        change < -0.5f -> "较${window.previousLabel} ↓ ${abs(change).roundToInt()} 个百分点"
        else -> "较${window.previousLabel}基本持平"
    }
    val changeColor = when {
        !hasPreviousData -> TextMuted
        change > 0.5f -> Success
        change < -0.5f -> Danger
        else -> TextMuted
    }

    TrendCardContainer {
        TrendCardHeader(title = "执行变化", rangeName = uiState.selectedOverviewTimeRange.displayName)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            TrendMetric(label = "已完成", value = "${uiState.overviewCompletedTasks} / ${uiState.overviewTotalTasks} 项")
            TrendMetric(
                label = "完成率",
                value = "${uiState.overviewCompletionRate.roundToInt()}%",
                valueColor = Primary,
                horizontalAlignment = Alignment.End
            )
        }
        Text(
            text = changeText,
            color = changeColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        ComparisonTrendSection(
            currentValues = currentData.map { it.completedCount },
            previousValues = previousData.map { it.completedCount },
            labels = currentData.map { trendDateLabel(it.date, uiState.selectedOverviewTimeRange) },
            currentLabel = uiState.selectedOverviewTimeRange.displayName,
            previousLabel = window.previousLabel,
            currentColor = Primary,
            emptyText = "暂无足够数据生成趋势"
        )
    }
}

@Composable
private fun OverdueChangeCard(
    currentData: List<DailyOverdueTrendData>,
    previousData: List<DailyOverdueTrendData>,
    window: TrendWindow
) {
    val currentOverdue = currentData.lastOrNull()?.overdueCount ?: 0
    val previousOverdue = previousData.lastOrNull()?.overdueCount ?: 0
    val change = currentOverdue - previousOverdue
    val changeText = when {
        previousData.isEmpty() -> "暂无同期数据"
        change > 0 -> "较${window.previousLabel} +$change 项"
        change < 0 -> "较${window.previousLabel} $change 项"
        else -> "较${window.previousLabel}持平"
    }
    val changeColor = when {
        previousData.isEmpty() -> TextMuted
        change > 0 -> Danger
        change < 0 -> Success
        else -> TextMuted
    }
    val rangeName = when {
        window.currentStart == window.currentEnd -> "今日"
        window.currentStart.dayOfMonth == 1 && window.currentStart.month == window.currentEnd.month -> "本月"
        else -> "本周"
    }

    TrendCardContainer {
        TrendCardHeader(title = "逾期变化", rangeName = rangeName)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrendMetric(label = "当前逾期", value = "$currentOverdue 项", valueColor = if (currentOverdue > 0) Danger else TextPrimary)
            Text(
                text = changeText,
                color = changeColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
        ComparisonTrendSection(
            currentValues = currentData.map { it.overdueCount },
            previousValues = previousData.map { it.overdueCount },
            labels = currentData.map { trendDateLabel(it.date, rangeNameToTimeRange(rangeName)) },
            currentLabel = rangeName,
            previousLabel = window.previousLabel,
            currentColor = Danger,
            emptyText = "当前周期暂无逾期变化"
        )
    }
}

private fun rangeNameToTimeRange(rangeName: String): OverviewTimeRange = when (rangeName) {
    "今日" -> OverviewTimeRange.TODAY
    "本月" -> OverviewTimeRange.THIS_MONTH
    else -> OverviewTimeRange.THIS_WEEK
}

@Composable
private fun TrendCardContainer(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(1.dp, Border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
private fun TrendCardHeader(title: String, rangeName: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(text = rangeName, color = TextMuted, fontSize = 12.sp)
    }
}

@Composable
private fun TrendMetric(
    label: String,
    value: String,
    valueColor: Color = TextPrimary,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start
) {
    Column(
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(text = label, color = TextMuted, fontSize = 12.sp)
        Text(text = value, color = valueColor, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ComparisonTrendSection(
    currentValues: List<Int>,
    previousValues: List<Int>,
    labels: List<String>,
    currentLabel: String,
    previousLabel: String,
    currentColor: Color,
    emptyText: String
) {
    val hasEnoughPoints = maxOf(currentValues.size, previousValues.size) >= 2
    val hasVisibleValue = currentValues.any { it > 0 } || previousValues.any { it > 0 }

    if (!hasEnoughPoints || !hasVisibleValue) {
        Text(text = emptyText, color = TextMuted, fontSize = 13.sp)
        return
    }

    ComparisonLineChart(
        currentValues = currentValues,
        previousValues = previousValues,
        labels = labels,
        currentColor = currentColor,
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ComparisonTrendLegendItem(label = currentLabel, color = currentColor)
        Spacer(modifier = Modifier.width(20.dp))
        ComparisonTrendLegendItem(label = previousLabel, color = TextMuted)
    }
}

@Composable
private fun ComparisonTrendLegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(18.dp)
                .height(3.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, color = TextSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun ComparisonLineChart(
    currentValues: List<Int>,
    previousValues: List<Int>,
    labels: List<String>,
    currentColor: Color,
    modifier: Modifier = Modifier
) {
    val maxValue = (currentValues + previousValues).maxOrNull()?.coerceAtLeast(1) ?: 1
    val pointCount = maxOf(currentValues.size, previousValues.size).coerceAtLeast(2)
    val gridColor = Border.copy(alpha = 0.55f)
    val previousColor = TextMuted

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            repeat(3) { index ->
                val y = size.height * index / 2f
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            }

            fun drawSeries(values: List<Int>, color: Color, dashed: Boolean) {
                if (values.size < 2) return
                val path = Path()
                values.forEachIndexed { index, value ->
                    val x = size.width * index / (pointCount - 1).toFloat()
                    val y = size.height - (value.toFloat() / maxValue) * size.height
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(
                        width = if (dashed) 2f else 3f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = if (dashed) {
                            androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                        } else null
                    )
                )
            }

            drawSeries(previousValues, previousColor, dashed = true)
            drawSeries(currentValues, currentColor, dashed = false)
        }

        val visibleLabels = when {
            labels.size <= 7 -> labels
            labels.isEmpty() -> emptyList()
            else -> listOf(labels.first(), labels[labels.size / 2], labels.last())
        }
        if (visibleLabels.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                visibleLabels.forEach { label ->
                    Text(text = label, color = TextMuted, fontSize = 10.sp)
                }
            }
        }
    }
}

private fun trendDateLabel(date: java.time.LocalDate, timeRange: OverviewTimeRange): String = when (timeRange) {
    OverviewTimeRange.TODAY -> date.format(DateTimeFormatter.ofPattern("M/d"))
    OverviewTimeRange.THIS_WEEK -> when (date.dayOfWeek) {
        java.time.DayOfWeek.MONDAY -> "一"
        java.time.DayOfWeek.TUESDAY -> "二"
        java.time.DayOfWeek.WEDNESDAY -> "三"
        java.time.DayOfWeek.THURSDAY -> "四"
        java.time.DayOfWeek.FRIDAY -> "五"
        java.time.DayOfWeek.SATURDAY -> "六"
        java.time.DayOfWeek.SUNDAY -> "日"
    }
    OverviewTimeRange.THIS_MONTH -> date.dayOfMonth.toString()
    OverviewTimeRange.ALL -> date.format(DateTimeFormatter.ofPattern("M/d"))
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
private fun EfficiencyContent(uiState: StatsUiState) {
    val summary = uiState.efficiencySummary
    val hasTimePattern = summary.completedCount >= 5 && summary.activeDayCount >= 3

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        EfficiencyOverviewCard(
            summary = summary,
            rangeName = uiState.selectedEfficiencyTimeRange.displayName
        )
        if (hasTimePattern) {
            CompletionTimeDistributionCard(
                heatmapData = uiState.timeHeatmap,
                completedCount = summary.completedCount
            )
        }
        EvidenceBasedEfficiencyAdviceCard(
            summary = summary,
            heatmapData = if (hasTimePattern) uiState.timeHeatmap else emptyList()
        )
    }
}

@Composable
private fun EfficiencyInsufficientState(summary: EfficiencySummaryData) {
    val current = summary.completedWithDeadlineCount.coerceAtMost(3)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 28.dp)
    ) {
        Text(
            text = "数据不足，暂不能生成效率诊断",
            color = TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "再完成 ${3 - current} 个带截止时间的任务后，可分析准时率和完成耗时",
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = "当前 $current / 3",
            color = Primary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EfficiencyOverviewCard(
    summary: EfficiencySummaryData,
    rangeName: String
) {
    EfficiencyDiagnosticCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("效率概览", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(rangeName, color = TextMuted, fontSize = 12.sp)
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            EfficiencyMetric(
                label = "准时完成率",
                value = "${summary.onTimeRate.roundToInt()}%",
                valueColor = if (summary.overdueCompletedCount > 0) Danger else Success,
                modifier = Modifier.weight(1f)
            )
            EfficiencyMetricDivider()
            EfficiencyMetric(
                label = "平均完成耗时",
                value = formatEfficiencyDuration(summary.averageCompletionMinutes),
                modifier = Modifier.weight(1f)
            )
            EfficiencyMetricDivider()
            EfficiencyMetric(
                label = "超期完成",
                value = "${summary.overdueCompletedCount} 项",
                valueColor = if (summary.overdueCompletedCount > 0) Danger else TextPrimary,
                modifier = Modifier.weight(1f)
            )
        }
        Text(
            text = "基于 ${summary.completedWithDeadlineCount} 个带截止时间的已完成任务",
            color = TextMuted,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun EfficiencyMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = TextPrimary
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, color = TextMuted, fontSize = 11.sp, maxLines = 1)
        Text(value, color = valueColor, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun EfficiencyMetricDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .width(1.dp)
            .height(42.dp)
            .background(Border)
    )
}

private data class CompactTimeSlot(
    val label: String,
    val slots: Set<TimeSlot>
)

private val compactTimeSlots = listOf(
    CompactTimeSlot("上午", setOf(TimeSlot.DAWN, TimeSlot.MORNING)),
    CompactTimeSlot("下午", setOf(TimeSlot.AFTERNOON)),
    CompactTimeSlot("晚间", setOf(TimeSlot.EVENING)),
    CompactTimeSlot("深夜", setOf(TimeSlot.NIGHT, TimeSlot.MIDNIGHT))
)

@Composable
private fun CompletionTimeDistributionCard(
    heatmapData: List<TimeHeatmapData>,
    completedCount: Int
) {
    val counts = remember(heatmapData) {
        compactTimeSlots.associateWith { compactSlot ->
            (1..7).associateWith { day ->
                heatmapData
                    .filter { it.dayOfWeek == day && it.timeSlot in compactSlot.slots }
                    .sumOf { it.completedCount }
            }
        }
    }
    val maxCount = counts.values.flatMap { it.values }.maxOrNull()?.coerceAtLeast(1) ?: 1
    val peak = counts.flatMap { (slot, dayCounts) ->
        dayCounts.map { (day, count) -> Triple(slot, day, count) }
    }.maxByOrNull { it.third }

    EfficiencyDiagnosticCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("完成时段分布", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("基于 $completedCount 个任务", color = TextMuted, fontSize = 11.sp)
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.width(44.dp))
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { day ->
                Text(
                    text = day,
                    color = TextSecondary,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        compactTimeSlots.forEach { slot ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(slot.label, color = TextSecondary, fontSize = 10.sp, modifier = Modifier.width(44.dp))
                (1..7).forEach { day ->
                    val count = counts[slot]?.get(day) ?: 0
                    val alpha = if (count == 0) 0f else 0.15f + 0.75f * count / maxCount.toFloat()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.2f)
                            .padding(2.dp)
                            .background(
                                if (count == 0) BgSecondary else Primary.copy(alpha = alpha),
                                RoundedCornerShape(4.dp)
                            )
                            .border(1.dp, Border, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (count > 0) {
                            Text(
                                text = count.toString(),
                                color = if (alpha > 0.55f) Color.White else TextPrimary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
        if (peak != null && peak.third > 0) {
            val dayName = listOf("", "周一", "周二", "周三", "周四", "周五", "周六", "周日")[peak.second]
            Text(
                text = "任务完成主要集中在${dayName}${peak.first.label}",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun EvidenceBasedEfficiencyAdviceCard(
    summary: EfficiencySummaryData,
    heatmapData: List<TimeHeatmapData>
) {
    val advice = when {
        summary.overdueCompletedCount > 0 ->
            "${summary.completedWithDeadlineCount} 个带截止时间的已完成任务中，有 ${summary.overdueCompletedCount} 个超期。建议为同类任务预留缓冲时间，并提前设置提醒。"
        summary.averageCompletionMinutes >= 24 * 60 ->
            "任务平均完成耗时超过 1 天。建议把耗时较长的任务拆成更小的步骤，降低启动和持续推进的难度。"
        heatmapData.isNotEmpty() ->
            "本周期未发现明显准时风险，可以继续记录完成时间，观察稳定的任务节奏。"
        else ->
            "本周期暂未发现明显效率风险。继续完成任务后，可进一步识别稳定的完成时段。"
    }

    EfficiencyDiagnosticCard {
        Text("诊断建议", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(advice, color = TextSecondary, fontSize = 13.sp, lineHeight = 20.sp)
    }
}

@Composable
private fun EfficiencyDiagnosticCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(1.dp, Border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

private fun formatEfficiencyDuration(minutes: Double): String = when {
    minutes <= 0.0 -> "--"
    minutes < 60.0 -> "${minutes.roundToInt()} 分钟"
    minutes < 24 * 60 -> "${String.format("%.1f", minutes / 60.0)} 小时"
    else -> "${String.format("%.1f", minutes / (24.0 * 60.0))} 天"
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
//  AI 周报页面
// ══════════════════════════════════════════

@Composable
private fun AIWeeklyReportContent(
    uiState: StatsUiState,
    onGenerate: () -> Unit,
    onNavigateToAIConfig: () -> Unit,
    onRetryContext: () -> Unit,
    exportWeeklyReport: () -> String?
) {
    when {
        !uiState.aiReportContextLoaded || uiState.isAIReportContextLoading -> {
            AIReportLoadingState("正在准备本周任务数据…")
        }
        uiState.isGeneratingReport -> {
            AIReportLoadingState("正在整理本周任务…")
        }
        uiState.weeklyReport != null -> {
            AIReportResult(
                uiState = uiState,
                onRegenerate = onGenerate,
                exportWeeklyReport = exportWeeklyReport
            )
        }
        uiState.aiReportWeekTaskCount == 0 && uiState.aiReportErrorMessage != null -> {
            AIReportErrorState(
                message = uiState.aiReportErrorMessage,
                onRetry = onRetryContext
            )
        }
        uiState.aiReportWeekTaskCount == 0 -> {
            AIReportEmptyState()
        }
        else -> {
            AIReportLaunchCard(
                uiState = uiState,
                onGenerate = onGenerate,
                onNavigateToAIConfig = onNavigateToAIConfig
            )
        }
    }
}

@Composable
private fun AIReportLaunchCard(
    uiState: StatsUiState,
    onGenerate: () -> Unit,
    onNavigateToAIConfig: () -> Unit
) {
    val routeStatus = uiState.aiRouteStatus
    val isUnavailable = routeStatus?.mode == AIRouteMode.Unavailable

    AIReportCard {
        Text(
            text = "本周回顾",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = formatAIReportWeekRange(uiState.aiReportWeekStart, uiState.aiReportWeekEnd),
            color = TextSecondary,
            fontSize = 13.sp
        )
        Text(
            text = "将基于本周 ${uiState.aiReportWeekTaskCount} 项任务生成周报，其中已完成 ${uiState.aiReportCompletedCount} 项。",
            color = TextPrimary,
            fontSize = 14.sp,
            lineHeight = 21.sp
        )
        HorizontalDivider(thickness = 1.dp, color = Border)
        Text(
            text = "AI 服务：${routeStatus?.userLabel ?: "状态未知"}",
            color = if (isUnavailable) Danger else TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        if (!isUnavailable) {
            Text(
                text = "任务标题、分类、状态和时间将发送给当前配置的 AI 服务，用于生成本周周报。",
                color = TextMuted,
                fontSize = 11.sp,
                lineHeight = 17.sp
            )
        }
        uiState.aiReportErrorMessage?.let { message ->
            AIReportInlineError(message)
        }
        Button(
            onClick = if (isUnavailable) onNavigateToAIConfig else onGenerate,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Text(
                text = if (isUnavailable) "去配置 AI" else "生成本周周报",
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun AIReportResult(
    uiState: StatsUiState,
    onRegenerate: () -> Unit,
    exportWeeklyReport: () -> String?
) {
    val context = LocalContext.current
    val report = uiState.weeklyReport ?: return

    AIReportCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = report.title.ifBlank { "本周周报" },
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = formatAIReportWeekRange(uiState.aiReportWeekStart, uiState.aiReportWeekEnd),
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
            IconButton(
                onClick = {
                    val exportText = exportWeeklyReport() ?: return@IconButton
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("周报", exportText))
                    android.widget.Toast.makeText(context, "周报已复制到剪贴板", android.widget.Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "复制周报",
                    tint = Primary
                )
            }
        }
        HorizontalDivider(thickness = 1.dp, color = Border)
        Text(
            text = report.summary,
            color = TextPrimary,
            fontSize = 14.sp,
            lineHeight = 21.sp
        )
        AIReportSection("本周亮点", report.highlights)
        AIReportSection("行为洞察", report.behaviorInsights)
        if (uiState.aiReportCompletedCount < 3) {
            Text(
                text = "已完成任务少于 3 项，本次不生成稳定行为模式判断。",
                color = TextMuted,
                fontSize = 11.sp,
                lineHeight = 17.sp
            )
        }
        AIReportSection("待改进", report.improvements)
        AIReportSection("下周建议", report.nextWeekSuggestions)
        HorizontalDivider(thickness = 1.dp, color = Border)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onRegenerate) {
                Text("重新生成", color = Primary, fontWeight = FontWeight.Medium)
            }
        }
        Text(
            text = "由 ${uiState.aiRouteStatus?.userLabel ?: "当前 AI 服务"} 生成，请结合实际情况判断。",
            color = TextMuted,
            fontSize = 11.sp,
            lineHeight = 17.sp
        )
    }
}

@Composable
private fun AIReportSection(title: String, items: List<String>) {
    if (items.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        items.forEach { item ->
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = "•",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = item,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AIReportCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(1.dp, Border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
private fun AIReportInlineError(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Danger.copy(alpha = 0.08f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Danger.copy(alpha = 0.28f))
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            color = Danger,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun AIReportLoadingState(message: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(28.dp),
            color = Primary,
            strokeWidth = 3.dp
        )
        Text(message, color = TextSecondary, fontSize = 14.sp)
    }
}

@Composable
private fun AIReportEmptyState() {
    Column(
        modifier = Modifier.padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "本周暂无任务",
            color = TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "创建并推进任务后，再来生成本周周报。",
            color = TextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AIReportErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "周报数据加载失败",
            color = TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = message,
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center
        )
        OutlinedButton(
            onClick = onRetry,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Border),
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Text("重新加载", color = Primary)
        }
    }
}

private fun formatAIReportWeekRange(start: java.time.LocalDate?, end: java.time.LocalDate?): String {
    if (start == null || end == null) return "本周"
    val formatter = DateTimeFormatter.ofPattern("M月d日")
    return "${start.format(formatter)}－${end.format(formatter)}"
}
