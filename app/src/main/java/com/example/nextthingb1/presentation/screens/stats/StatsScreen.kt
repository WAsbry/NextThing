package com.example.nextthingb1.presentation.screens.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

import com.example.nextthingb1.presentation.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
    ) {

        
        item {
            // 头部导航
            StatsTopHeader()
        }
        
        item {
            // 统计类型切换
            StatsTabs(
                selectedTab = uiState.selectedTab,
                onTabSelected = { viewModel.selectTab(it) }
            )
        }
        
        item {
            // 月份导航
            StatsMonthNavigation(
                currentMonth = uiState.currentMonth,
                onPreviousMonth = { viewModel.previousMonth() },
                onNextMonth = { viewModel.nextMonth() }
            )
        }
        
        item {
            // 任务概览
            TaskStatsOverviewGrid(
                totalTasks = uiState.totalTasks,
                completedTasks = uiState.completedTasks,
                completionRate = uiState.completionRate,
                averageCompletionTime = uiState.averageCompletionTime
            )
        }
        
        item {
            // 任务完成趋势图
            TaskCompletionTrendChart()
        }
        
        item {
            // 分类排行
            CategoryRankingCard(
                categories = uiState.categories
            )
        }
        
        item {
            // 明细排行
            DetailRankingCard(
                details = uiState.details
            )
        }
        
        item {
            // 精选图片
            FeaturedImageCard()
        }
    }
}

@Composable
private fun StatsTopHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgCard)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "NextThing",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_mylocation),
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(16.dp)
            )
        }
        
        IconButton(
            onClick = { /* TODO: 搜索功能 */ },
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(BgPrimary)
        ) {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_search),
                contentDescription = "搜索",
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun StatsTabs(
    selectedTab: StatsTab,
    onTabSelected: (StatsTab) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgCard)
                .padding(8.dp)
        ) {
            StatsTab.values().forEach { tab ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (selectedTab == tab) Success else Color.Transparent
                        )
                        .clickable { onTabSelected(tab) }
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.title,
                        color = if (selectedTab == tab) Color.White else TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsMonthNavigation(
    currentMonth: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPreviousMonth,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(BgPrimary)
            ) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_media_previous),
                    contentDescription = "上个月",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
            
            Text(
                text = currentMonth,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            
            IconButton(
                onClick = onNextMonth,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(BgPrimary)
            ) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_media_next),
                    contentDescription = "下个月",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun TaskStatsOverviewGrid(
    totalTasks: String,
    completedTasks: String,
    completionRate: String,
    averageCompletionTime: String
) {
    val overviewItems = listOf(
        OverviewItem("总任务数", totalTasks, Primary, android.R.drawable.ic_menu_agenda),
        OverviewItem("已完成", completedTasks, Success, android.R.drawable.checkbox_on_background),
        OverviewItem("完成率", completionRate, Color(0xFFAB47BC), android.R.drawable.stat_notify_sync),
        OverviewItem("平均完成", averageCompletionTime, Color(0xFF4CAF50), android.R.drawable.ic_menu_recent_history)
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        overviewItems.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                rowItems.forEach { item ->
                    OverviewCard(
                        item = item,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewCard(
    item: OverviewItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(item.color, item.color.copy(alpha = 0.8f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = item.iconRes),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = item.title,
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = item.value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TaskCompletionTrendChart() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            // 图表头部
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "完成趋势",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "2025-08-02",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                
                Row(
                    modifier = Modifier
                        .background(BgPrimary, RoundedCornerShape(16.dp))
                        .padding(2.dp)
                ) {
                    Button(
                        onClick = { },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Success,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "大类",
                            fontSize = 12.sp
                        )
                    }
                    
                    Button(
                        onClick = { },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = TextSecondary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "小类",
                            fontSize = 12.sp
                        )
                    }
                }
            }
            
            // 图表内容
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(16.dp)
                    .background(BgPrimary, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                // 简单的线性图表背景
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                ) {
                    drawTrendLine(this)
                }
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_info_details),
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "累计完成：18个任务",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                }
            }
            
            // 日期标签
            Text(
                text = "01     05     10     15     20     25     30",
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }
}

private fun drawTrendLine(drawScope: DrawScope) {
    val path = Path()
    val width = drawScope.size.width
    val height = drawScope.size.height
    
    // 绘制简单的趋势线
    path.moveTo(0f, height * 0.8f)
    path.lineTo(width * 0.2f, height * 0.6f)
    path.lineTo(width * 0.4f, height * 0.4f)
    path.lineTo(width * 0.6f, height * 0.5f)
    path.lineTo(width * 0.8f, height * 0.3f)
    path.lineTo(width, height * 0.2f)
    
    drawScope.drawPath(
        path = path,
        color = Color(0xFF4CAF50),
        style = Stroke(width = with(drawScope) { 3.dp.toPx() })
    )
}

@Composable
private fun CategoryRankingCard(
    categories: List<CategoryItem>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            // 卡片头部
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "分类排行",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                
                Row(
                    modifier = Modifier
                        .background(BgPrimary, RoundedCornerShape(16.dp))
                        .padding(2.dp)
                ) {
                    Button(
                        onClick = { },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TextPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "大类",
                            fontSize = 12.sp
                        )
                    }
                    
                    Button(
                        onClick = { },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = TextSecondary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "小类",
                            fontSize = 12.sp
                        )
                    }
                }
            }
            
            // 饼图
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                PieChart(
                    modifier = Modifier.size(200.dp)
                )
            }
            
            // 分类列表
            categories.forEach { category ->
                CategoryRankingItem(category = category)
            }
        }
    }
}

@Composable
private fun PieChart(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawPieChart(this)
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "本期",
                fontSize = 12.sp,
                color = TextSecondary
            )
            Text(
                text = "¥ 235.00",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "日用 14%",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }
}

private fun drawPieChart(drawScope: DrawScope) {
    val colors = listOf(
        Color(0xFF4FC3F7),
        Color(0xFF66BB6A),
        Color(0xFFFFA726),
        Color(0xFFEF5350),
        Color(0xFFAB47BC)
    )
    
    val percentages = listOf(27.83f, 23.58f, 17.23f, 15.64f, 15.72f)
    val center = drawScope.center
    val radius = minOf(drawScope.size.width, drawScope.size.height) / 2f * 0.8f
    
    var startAngle = -90f
    
    percentages.forEachIndexed { index, percentage ->
        val sweepAngle = percentage / 100f * 360f
        
        drawScope.drawArc(
            color = colors[index],
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = true,
            topLeft = androidx.compose.ui.geometry.Offset(
                center.x - radius,
                center.y - radius
            ),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
        )
        
        startAngle += sweepAngle
    }
}

@Composable
private fun CategoryRankingItem(category: CategoryItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 分类图标
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(category.color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = category.icon,
                fontSize = 14.sp
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // 分类内容
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            
            Text(
                text = "${category.percentage}%",
                fontSize = 12.sp,
                color = TextSecondary
            )
            
            // 进度条
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(BgPrimary, RoundedCornerShape(2.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(category.percentage / 100f)
                        .background(category.color, RoundedCornerShape(2.dp))
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // 数量
        Text(
            text = category.amount,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}

@Composable
private fun DetailRankingCard(
    details: List<DetailItem>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            // 卡片头部
            Text(
                text = "明细排行",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.padding(16.dp)
            )
            
            // 明细列表
            details.forEach { detail ->
                DetailRankingItem(detail = detail)
            }
        }
    }
}

@Composable
private fun DetailRankingItem(detail: DetailItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 明细图标
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(detail.color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = detail.icon,
                fontSize = 14.sp
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // 明细内容
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = detail.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            
            Text(
                text = detail.description,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
        
        // 状态
        Text(
            text = detail.amount,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Danger
        )
    }
}

@Composable
private fun FeaturedImageCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            // 卡片头部
            Text(
                text = "精选图片",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.padding(16.dp)
            )
            
            // 内容
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "📸",
                    fontSize = 48.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "为原创添加图片，记录更多精彩~",
                    color = TextMuted,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "¥ 0.00",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("分享报告", "导出数据", "任务分析", "效率统计").forEach { text ->
                        OutlinedButton(
                            onClick = { },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = TextSecondary
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Border)
                        ) {
                            Text(
                                text = text,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "数据备份中 ↑",
                    fontSize = 12.sp,
                    color = Primary
                )
            }
        }
    }
}

// 数据类
data class OverviewItem(
    val title: String,
    val value: String,
    val color: Color,
    val iconRes: Int
)

data class CategoryItem(
    val name: String,
    val percentage: Float,
    val amount: String,
    val color: Color,
    val icon: String
)

data class DetailItem(
    val name: String,
    val description: String,
    val amount: String,
    val color: Color,
    val icon: String
) 