package com.nextthing.app.presentation.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nextthing.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendDetailScreen(
    trendType: String,
    onBackPressed: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val config = getTrendDetailConfig(trendType, uiState)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
    ) {
        // 顶部导航栏
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
                Text(
                    text = "趋势分析详情",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Card 1: 图表含义
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BgCard)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = config.icon, fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = config.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = config.description,
                        fontSize = 14.sp,
                        color = TextSecondary,
                        lineHeight = 22.sp
                    )
                }
            }

            // Card 2: 判断规则
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BgCard)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "判断规则",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    config.rules.forEach { rule ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .size(8.dp)
                                    .background(rule.color, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = rule.label,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = rule.color
                                )
                                Text(
                                    text = rule.condition,
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Card 3: 当前趋势结论
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = config.currentTrend.color.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "当前趋势",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${config.currentTrend.arrow} ${config.currentTrend.label}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = config.currentTrend.color
                    )
                }
            }

            // Card 4: 数据支撑
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BgCard)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "数据支撑",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    config.dataPoints.forEach { dp ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BgSecondary, RoundedCornerShape(10.dp))
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = dp.label, fontSize = 14.sp, color = TextSecondary)
                            Text(
                                text = dp.value,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ── 配置数据结构 ──

private data class TrendRule(val label: String, val condition: String, val color: androidx.compose.ui.graphics.Color)
private data class TrendConclusion(val arrow: String, val label: String, val color: androidx.compose.ui.graphics.Color)
private data class DataPoint(val label: String, val value: String)

private data class TrendDetailConfig(
    val icon: String,
    val title: String,
    val description: String,
    val rules: List<TrendRule>,
    val currentTrend: TrendConclusion,
    val dataPoints: List<DataPoint>
)

@Composable
private fun getTrendDetailConfig(trendType: String, uiState: StatsUiState): TrendDetailConfig {
    val successColor = Success
    val dangerColor = Danger
    val mutedColor = TextMuted

    return when (trendType) {
        "completion" -> {
            val data = uiState.weeklyTrend
            val recentAvg = if (data.size >= 3) data.takeLast(3).map { it.completedCount }.average() else 0.0
            val previousAvg = if (data.size >= 6) data.dropLast(3).takeLast(3).map { it.completedCount }.average() else recentAvg
            val trend = when {
                previousAvg == 0.0 -> TrendConclusion("→", "平稳", mutedColor)
                recentAvg > previousAvg * 1.1 -> TrendConclusion("↗", "上升", successColor)
                recentAvg < previousAvg * 0.9 -> TrendConclusion("↘", "下降", dangerColor)
                else -> TrendConclusion("→", "平稳", mutedColor)
            }
            TrendDetailConfig(
                icon = "📈",
                title = "任务完成趋势",
                description = "展示每日的任务创建数量和完成数量，叠加7日移动平均线来平滑短期波动。帮助你观察长期的任务完成趋势方向。",
                rules = listOf(
                    TrendRule("上升 ↗", "近3天完成均值 > 前3天完成均值 × 110%", successColor),
                    TrendRule("下降 ↘", "近3天完成均值 < 前3天完成均值 × 90%", dangerColor),
                    TrendRule("平稳 →", "变化幅度在 ±10% 以内", mutedColor)
                ),
                currentTrend = trend,
                dataPoints = listOf(
                    DataPoint("近3天日均完成", "${"%.1f".format(recentAvg)} 个"),
                    DataPoint("前3天日均完成", "${"%.1f".format(previousAvg)} 个"),
                    DataPoint("变化幅度", if (previousAvg > 0) "${"%.1f".format((recentAvg - previousAvg) / previousAvg * 100)}%" else "—")
                )
            )
        }
        "rate" -> {
            val data = uiState.completionRateTrend
            val thisWeek = data.lastOrNull()?.completionRate ?: 0f
            val lastWeek = if (data.size >= 2) data[data.size - 2].completionRate else thisWeek
            val change = thisWeek - lastWeek
            val trend = when {
                change > 0.02f -> TrendConclusion("↗", "上升", successColor)
                change < -0.02f -> TrendConclusion("↘", "下降", dangerColor)
                else -> TrendConclusion("→", "平稳", mutedColor)
            }
            TrendDetailConfig(
                icon = "📊",
                title = "完成率走势",
                description = "每周的任务完成率（已完成 ÷ 可结算任务数），消除了「忙碌周任务多、轻松周任务少」对绝对数量的干扰，更能反映真实的执行效率。",
                rules = listOf(
                    TrendRule("上升 ↗", "本周完成率 > 上周完成率 + 2%", successColor),
                    TrendRule("下降 ↘", "本周完成率 < 上周完成率 - 2%", dangerColor),
                    TrendRule("平稳 →", "完成率变化在 ±2% 以内", mutedColor)
                ),
                currentTrend = trend,
                dataPoints = listOf(
                    DataPoint("本周完成率", "${(thisWeek * 100).toInt()}%"),
                    DataPoint("上周完成率", "${(lastWeek * 100).toInt()}%"),
                    DataPoint("周环比变化", "${if (change >= 0) "+" else ""}${(change * 100).toInt()}%")
                )
            )
        }
        "cycletime" -> {
            val data = uiState.cycleTimeTrend
            val latest = data.lastOrNull()?.avgDays ?: 0f
            val previous = if (data.size >= 2) data[data.size - 2].avgDays else latest
            val change = latest - previous
            // 周期下降是好事
            val trend = when {
                change < -0.1f -> TrendConclusion("↘", "加速中", successColor)
                change > 0.1f -> TrendConclusion("↗", "变慢了", dangerColor)
                else -> TrendConclusion("→", "平稳", mutedColor)
            }
            TrendDetailConfig(
                icon = "⏱️",
                title = "平均完成周期",
                description = "Cycle Time（周期时间）是精益管理和敏捷方法的核心指标，衡量任务从创建到完成的平均天数。周期越短，说明执行力越强，响应越快。",
                rules = listOf(
                    TrendRule("加速 ↘", "本周周期 < 上周周期（好现象）", successColor),
                    TrendRule("变慢 ↗", "本周周期 > 上周周期（需关注）", dangerColor),
                    TrendRule("平稳 →", "变化幅度很小", mutedColor)
                ),
                currentTrend = trend,
                dataPoints = listOf(
                    DataPoint("本周平均周期", "${"%.1f".format(latest)} 天"),
                    DataPoint("上周平均周期", "${"%.1f".format(previous)} 天"),
                    DataPoint("变化", "${if (change >= 0) "+" else ""}${"%.1f".format(change)} 天")
                )
            )
        }
        "cfd" -> {
            val data = uiState.cumulativeFlow
            val latest = data.lastOrNull()
            val first = data.firstOrNull()
            val overdueChange = if (latest != null && first != null) latest.overdue - first.overdue else 0
            val completedGrowth = if (latest != null && first != null) latest.completed - first.completed else 0
            val trend = when {
                overdueChange > 2 -> TrendConclusion("↗", "堆积", dangerColor)
                completedGrowth > 0 && (latest?.overdue ?: 0) <= 2 -> TrendConclusion("↗", "健康", successColor)
                else -> TrendConclusion("→", "平稳", mutedColor)
            }
            TrendDetailConfig(
                icon = "🌊",
                title = "累积流图",
                description = "Cumulative Flow Diagram（累积流图）是看板方法的核心可视化工具。通过堆叠面积图展示不同状态的任务数量随时间变化，直观反映任务流转是否健康。绿色层（已完成）稳步增长 = 健康；红色层（逾期）变厚 = 任务堆积需关注。",
                rules = listOf(
                    TrendRule("健康 ↗", "已完成层稳定增长，逾期层 ≤ 2 个", successColor),
                    TrendRule("堆积 ↗", "逾期层比月初增加 > 2 个", dangerColor),
                    TrendRule("平稳 →", "各层变化不大", mutedColor)
                ),
                currentTrend = trend,
                dataPoints = listOf(
                    DataPoint("当前已完成", "${latest?.completed ?: 0} 个"),
                    DataPoint("当前逾期中", "${latest?.overdue ?: 0} 个"),
                    DataPoint("当前待办中", "${latest?.pending ?: 0} 个"),
                    DataPoint("30天完成增长", "+$completedGrowth 个"),
                    DataPoint("30天逾期变化", "${if (overdueChange >= 0) "+" else ""}$overdueChange 个")
                )
            )
        }
        else -> TrendDetailConfig(
            icon = "📊", title = "趋势详情", description = "暂无数据",
            rules = emptyList(),
            currentTrend = TrendConclusion("→", "平稳", mutedColor),
            dataPoints = emptyList()
        )
    }
}
