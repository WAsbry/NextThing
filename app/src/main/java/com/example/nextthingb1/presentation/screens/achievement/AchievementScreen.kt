package com.example.nextthingb1.presentation.screens.achievement

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nextthingb1.domain.model.AchievementCategory
import com.example.nextthingb1.domain.model.AchievementProgress
import com.example.nextthingb1.domain.model.AchievementTier
import com.example.nextthingb1.domain.model.AchievementType
import com.example.nextthingb1.presentation.theme.*
import kotlinx.coroutines.delay

@Composable
fun AchievementScreen(
    onBackPressed: () -> Unit,
    viewModel: AchievementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // 新解锁成就庆祝弹窗
    val newlyUnlocked = uiState.newlyUnlocked
    if (newlyUnlocked.isNotEmpty()) {
        UnlockCelebrationDialog(
            achievements = newlyUnlocked,
            onDismiss = { viewModel.clearNewlyUnlocked() }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
    ) {
        // 顶部导航栏
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackPressed,
                    modifier = Modifier.size(48.dp)
                ) {
                    Text(
                        text = "‹",
                        fontSize = 32.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Light
                    )
                }
                Text(
                    text = "我的成就",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.size(48.dp))
            }
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // 总览卡片
            item {
                AchievementSummaryCard(
                    unlockedCount = uiState.unlockedCount,
                    totalCount = uiState.totalCount
                )
            }

            // 按分类展示成就
            for (category in AchievementCategory.entries) {
                val categoryAchievements = uiState.achievements.filter {
                    it.type.category == category
                }

                item(key = category.name + "_header") {
                    CategorySectionHeader(category = category)
                }

                item(key = category.name + "_badges") {
                    AchievementBadgeRow(achievements = categoryAchievements)
                }
            }
        }
    }
}

// ── 总览统计卡片 ──

@Composable
private fun AchievementSummaryCard(
    unlockedCount: Int,
    totalCount: Int
) {
    val completionPct = if (totalCount > 0) (unlockedCount * 100 / totalCount) else 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Primary, Primary.copy(alpha = 0.75f))
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SummaryStatItem(value = "$unlockedCount", label = "已解锁", color = Color.White)

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(Color.White.copy(alpha = 0.4f))
                )

                SummaryStatItem(
                    value = "${totalCount - unlockedCount}",
                    label = "未解锁",
                    color = Color.White.copy(alpha = 0.8f)
                )

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(Color.White.copy(alpha = 0.4f))
                )

                SummaryStatItem(value = "$completionPct%", label = "完成度", color = Color.White)
            }
        }
    }
}

@Composable
private fun SummaryStatItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = color.copy(alpha = 0.85f)
        )
    }
}

// ── 分类标题行 ──

@Composable
private fun CategorySectionHeader(category: AchievementCategory) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = category.icon, fontSize = 18.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = category.displayName,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}

// ── 单个分类的4个成就横排 ──

@Composable
private fun AchievementBadgeRow(achievements: List<AchievementProgress>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            achievements.forEach { progress ->
                AchievementBadgeCard(progress = progress)
            }
        }
    }
}

// ── 单个成就卡片 ──

@Composable
private fun AchievementBadgeCard(progress: AchievementProgress) {
    Column(
        modifier = Modifier.width(76.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 图标圆形底板
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(getTierBackgroundColor(progress.type.tier, progress.isUnlocked))
                .alpha(if (progress.isUnlocked) 1f else 0.5f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (progress.isUnlocked) progress.type.icon else "🔒",
                fontSize = 26.sp
            )
        }

        // 成就名称
        Text(
            text = progress.type.displayName,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (progress.isUnlocked) TextPrimary else TextMuted,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 14.sp
        )

        // 进度文字 / 完成标记
        if (progress.isUnlocked) {
            Text(
                text = "已解锁",
                fontSize = 10.sp,
                color = Success,
                fontWeight = FontWeight.Medium
            )
        } else {
            // 进度数字
            Text(
                text = "${progress.currentValue}/${progress.type.threshold}",
                fontSize = 10.sp,
                color = TextMuted
            )
            // 进度条
            if (progress.type.threshold > 1) {
                LinearProgressIndicator(
                    progress = { progress.progress },
                    modifier = Modifier
                        .width(52.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = getTierAccentColor(progress.type.tier),
                    trackColor = Color(0xFFEEEEEE)
                )
            }
        }
    }
}

// ── 新解锁庆祝弹窗 ──

@Composable
private fun UnlockCelebrationDialog(
    achievements: List<AchievementType>,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
        delay(3000)
        visible = false
        delay(300)
        onDismiss()
    }

    Dialog(onDismissRequest = onDismiss) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "🎉", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "成就解锁！",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    achievements.take(3).forEach { type ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .background(
                                    color = getTierBackgroundColor(type.tier, true),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = type.icon, fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = type.displayName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = type.description,
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Primary,
                            contentColor = Color.White
                        )
                    ) {
                        Text(text = "太棒了", fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

// ── 辅助函数：等级颜色 ──

private fun getTierBackgroundColor(tier: AchievementTier, isUnlocked: Boolean): Color {
    if (!isUnlocked) return Color(0xFFF0F0F0)
    return when (tier) {
        AchievementTier.BRONZE  -> Color(0xFFCD7F32).copy(alpha = 0.18f)
        AchievementTier.SILVER  -> Color(0xFFC0C0C0).copy(alpha = 0.25f)
        AchievementTier.GOLD    -> Color(0xFFFFD700).copy(alpha = 0.20f)
        AchievementTier.DIAMOND -> Color(0xFF00BCD4).copy(alpha = 0.18f)
    }
}

private fun getTierAccentColor(tier: AchievementTier): Color {
    return when (tier) {
        AchievementTier.BRONZE  -> Color(0xFFCD7F32)
        AchievementTier.SILVER  -> Color(0xFF9E9E9E)
        AchievementTier.GOLD    -> Color(0xFFFFC107)
        AchievementTier.DIAMOND -> Color(0xFF00BCD4)
    }
}
