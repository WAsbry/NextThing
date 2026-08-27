package com.nextthing.app.presentation.screens.achievement

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.nextthing.app.R
import com.nextthing.app.domain.model.AchievementCategory
import com.nextthing.app.domain.model.AchievementProgress
import com.nextthing.app.domain.model.AchievementTier
import com.nextthing.app.domain.model.AchievementType
import com.nextthing.app.presentation.theme.BgCard
import com.nextthing.app.presentation.theme.BgPrimary
import com.nextthing.app.presentation.theme.Border
import com.nextthing.app.presentation.theme.Primary
import com.nextthing.app.presentation.theme.Success
import com.nextthing.app.presentation.theme.TextMuted
import com.nextthing.app.presentation.theme.TextPrimary
import com.nextthing.app.presentation.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementScreen(
    onBackPressed: () -> Unit,
    viewModel: AchievementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedAchievement by remember { mutableStateOf<AchievementProgress?>(null) }
    var expandedCategoryName by rememberSaveable { mutableStateOf<String?>(null) }

    if (uiState.newlyUnlocked.isNotEmpty()) {
        UnlockCelebrationDialog(
            achievements = uiState.newlyUnlocked,
            onDismiss = viewModel::clearNewlyUnlocked
        )
    }
    selectedAchievement?.let {
        AchievementDetailSheet(progress = it, onDismiss = { selectedAchievement = null })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .statusBarsPadding()
    ) {
        AchievementTopBar(onBackPressed)

        when {
            uiState.isLoading -> AchievementLoadingState()
            uiState.errorMessage != null -> AchievementErrorState(
                message = uiState.errorMessage,
                onRetry = viewModel::retry
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 5.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                item(key = "summary") {
                    AchievementSummaryCard(uiState.unlockedCount, uiState.totalCount)
                }

                item(key = "near_title") { SectionTitle("接近解锁") }
                val closest = uiState.achievements
                    .asSequence()
                    .filter { !it.isUnlocked && it.currentValue > 0 }
                    .sortedByDescending { it.progress }
                    .take(3)
                    .toList()
                item(key = "near_content") {
                    if (closest.isEmpty()) {
                        EmptyProgressCard()
                    } else {
                        AchievementListCard(
                            achievements = closest,
                            onAchievementClick = { selectedAchievement = it }
                        )
                    }
                }

                item(key = "all_title") { SectionTitle("全部成就") }
                items(AchievementCategory.entries, key = { it.name }) { category ->
                    val categoryAchievements = uiState.achievements.filter { it.type.category == category }
                    val expanded = expandedCategoryName == category.name
                    CategoryCard(
                        category = category,
                        achievements = categoryAchievements,
                        expanded = expanded,
                        onToggle = {
                            expandedCategoryName = if (expanded) null else category.name
                        },
                        onAchievementClick = { selectedAchievement = it }
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementTopBar(onBackPressed: () -> Unit) {
    Surface(color = BgCard, border = BorderStroke(1.dp, Border)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(start = 10.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackPressed, modifier = Modifier.size(40.dp)) {
                Icon(
                    painter = painterResource(R.drawable.icon_achievement_back),
                    contentDescription = "返回",
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(5.dp))
            Text("我的成就", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
    }
}

@Composable
private fun AchievementSummaryCard(unlockedCount: Int, totalCount: Int) {
    val percent = if (totalCount == 0) 0 else unlockedCount * 100 / totalCount
    BorderedCard {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("成就进度", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text("已解锁 $unlockedCount / $totalCount", fontSize = 14.sp, color = TextSecondary)
            }
            Text("$percent%", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Primary)
        }
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { percent / 100f },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = Primary,
            trackColor = Border
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
    )
}

@Composable
private fun EmptyProgressCard() {
    BorderedCard {
        Text("完成任务后，这里会展示最接近解锁的成就", fontSize = 14.sp, color = TextSecondary)
    }
}

@Composable
private fun AchievementListCard(
    achievements: List<AchievementProgress>,
    onAchievementClick: (AchievementProgress) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(1.dp, Border)
    ) {
        achievements.forEachIndexed { index, progress ->
            AchievementRow(progress, onClick = { onAchievementClick(progress) })
            if (index != achievements.lastIndex) HorizontalDivider(color = Border, modifier = Modifier.padding(start = 58.dp))
        }
    }
}

@Composable
private fun CategoryCard(
    category: AchievementCategory,
    achievements: List<AchievementProgress>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onAchievementClick: (AchievementProgress) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(1.dp, Border)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onToggle).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIcon(category = category, unlocked = achievements.any { it.isUnlocked })
            Spacer(Modifier.width(10.dp))
            Text(category.displayName, modifier = Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text("${achievements.count { it.isUnlocked }} / ${achievements.size}", fontSize = 13.sp, color = TextSecondary)
            Spacer(Modifier.width(6.dp))
            Icon(
                painter = painterResource(R.drawable.icon_detail_chevron),
                contentDescription = if (expanded) "收起" else "展开",
                tint = TextMuted,
                modifier = Modifier.size(16.dp).rotate(if (expanded) 90f else 0f)
            )
        }
        if (expanded) {
            HorizontalDivider(color = Border)
            achievements.forEachIndexed { index, progress ->
                AchievementRow(progress, onClick = { onAchievementClick(progress) })
                if (index != achievements.lastIndex) HorizontalDivider(color = Border, modifier = Modifier.padding(start = 58.dp))
            }
        }
    }
}

@Composable
private fun AchievementRow(progress: AchievementProgress, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryIcon(progress.type.category, progress.isUnlocked)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(progress.type.displayName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.width(6.dp))
                TierLabel(progress.type.tier)
            }
            Text(progress.type.description, fontSize = 12.sp, color = TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (!progress.isUnlocked) {
                LinearProgressIndicator(
                    progress = { progress.progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = Primary,
                    trackColor = Border
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        if (progress.isUnlocked) {
            Icon(painterResource(R.drawable.icon_achievement_check), "已解锁", tint = Success, modifier = Modifier.size(22.dp))
        } else {
            Text("${progress.currentValue}/${progress.type.threshold}", fontSize = 12.sp, color = TextMuted)
        }
    }
}

@Composable
private fun CategoryIcon(category: AchievementCategory, unlocked: Boolean) {
    Box(
        modifier = Modifier.size(38.dp).clip(RoundedCornerShape(8.dp)).background(if (unlocked) Primary.copy(alpha = 0.12f) else Border.copy(alpha = 0.65f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(category.iconRes()),
            contentDescription = category.displayName,
            tint = if (unlocked) Primary else TextMuted,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun TierLabel(tier: AchievementTier) {
    Surface(shape = RoundedCornerShape(4.dp), color = tierColor(tier).copy(alpha = 0.12f)) {
        Text("${tier.displayName}级", modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp), fontSize = 10.sp, color = tierColor(tier))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AchievementDetailSheet(progress: AchievementProgress, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BgCard,
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryIcon(progress.type.category, progress.isUnlocked)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(progress.type.displayName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("${progress.type.category.displayName} · ${progress.type.tier.displayName}级", fontSize = 13.sp, color = TextSecondary)
                }
                if (progress.isUnlocked) Icon(painterResource(R.drawable.icon_achievement_check), "已解锁", tint = Success, modifier = Modifier.size(24.dp))
            }
            HorizontalDivider(color = Border)
            DetailBlock("解锁条件", progress.type.description)
            DetailBlock("当前进度", "${progress.currentValue} / ${progress.type.threshold}")
            LinearProgressIndicator(
                progress = { progress.progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = if (progress.isUnlocked) Success else Primary,
                trackColor = Border
            )
            DetailBlock(
                if (progress.isUnlocked) "解锁时间" else "距离解锁",
                if (progress.isUnlocked) progress.formattedUnlockTime ?: "已解锁" else progress.remainingText ?: "继续保持"
            )
            DetailBlock("建议", progress.type.tip)
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) { Text("关闭", color = Color.White, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun DetailBlock(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 12.sp, color = TextMuted)
        Text(value, fontSize = 14.sp, color = TextPrimary, lineHeight = 20.sp)
    }
}

@Composable
private fun UnlockCelebrationDialog(achievements: List<AchievementType>, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = BgCard),
            border = BorderStroke(1.dp, Border)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(painterResource(R.drawable.icon_mine_achievement), null, tint = Primary, modifier = Modifier.size(40.dp))
                Text("成就已解锁", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                achievements.take(3).forEach { type ->
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        CategoryIcon(type.category, true)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(type.displayName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text(type.description, fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) { Text("知道了", color = Color.White, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun AchievementLoadingState() {
    Column(modifier = Modifier.fillMaxSize().padding(top = 5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(4) {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp).height(if (it == 0) 104.dp else 64.dp).clip(RoundedCornerShape(8.dp)).background(Border.copy(alpha = 0.5f)))
        }
    }
}

@Composable
private fun AchievementErrorState(message: String?, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("成就数据加载失败", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            if (!message.isNullOrBlank()) Text(message, fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center)
            Button(onClick = onRetry, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text("重新加载") }
        }
    }
}

@Composable
private fun BorderedCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(1.dp, Border)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), content = content)
    }
}

@DrawableRes
private fun AchievementCategory.iconRes(): Int = when (this) {
    AchievementCategory.TASK_MASTER -> R.drawable.icon_achievement_task
    AchievementCategory.PERSISTENCE -> R.drawable.icon_achievement_streak
    AchievementCategory.EFFICIENCY -> R.drawable.icon_achievement_efficiency
    AchievementCategory.VERSATILE -> R.drawable.icon_achievement_versatile
    AchievementCategory.MILESTONE -> R.drawable.icon_achievement_milestone
}

private fun tierColor(tier: AchievementTier): Color = when (tier) {
    AchievementTier.BRONZE -> Color(0xFF9A6A3A)
    AchievementTier.SILVER -> Color(0xFF728196)
    AchievementTier.GOLD -> Color(0xFFB57A00)
    AchievementTier.DIAMOND -> Color(0xFF008FA8)
}
