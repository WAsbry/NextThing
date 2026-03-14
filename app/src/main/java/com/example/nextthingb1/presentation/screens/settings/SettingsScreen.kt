package com.example.nextthingb1.presentation.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.nextthingb1.domain.model.ThemeMode
import com.example.nextthingb1.data.preferences.AIPreferences
import com.example.nextthingb1.data.preferences.AIProvider
import com.example.nextthingb1.presentation.theme.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateToUserInfo: () -> Unit = {},
    onNavigateToGeofence: () -> Unit = {},
    onNavigateToAchievement: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 监听操作消息，弹 Snackbar
    LaunchedEffect(uiState.actionMessage) {
        uiState.actionMessage?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            viewModel.clearActionMessage()
        }
    }

    // 主题选择弹窗
    if (uiState.showThemeDialog) {
        ThemePickerDialog(
            currentMode = uiState.themeMode,
            onSelect = { viewModel.setThemeMode(it) },
            onDismiss = { viewModel.hideThemeDialog() }
        )
    }

    // AI 配置弹窗
    if (uiState.showAIConfigDialog) {
        AIConfigDialog(
            currentProvider = uiState.aiProvider,
            currentApiKey = uiState.aiApiKey,
            currentModel = uiState.aiModel,
            onSave = { provider, apiKey, model -> viewModel.saveAIConfig(provider, apiKey, model) },
            onDismiss = { viewModel.hideAIConfigDialog() }
        )
    }

    // 讯飞 ASR 配置弹窗
    if (uiState.showIFlyConfigDialog) {
        IFlyConfigDialog(
            currentAppId = uiState.iflyAppId,
            currentApiKey = uiState.iflyApiKey,
            currentApiSecret = uiState.iflyApiSecret,
            currentAccent = uiState.iflyAccent,
            onSave = { appId, key, secret, accent ->
                viewModel.saveIFlyConfig(appId, key, secret, accent)
            },
            onDismiss = { viewModel.hideIFlyConfigDialog() }
        )
    }

    // 清除已完成任务确认弹窗
    if (uiState.showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideClearConfirmDialog() },
            title = {
                Text("清除已完成任务", fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Text(
                    "确定要删除所有已完成的任务（共 ${uiState.completedCount} 条）？\n此操作不可恢复。",
                    fontSize = 15.sp,
                    color = TextSecondary,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearCompletedTasks() }) {
                    Text("确认删除", color = Danger, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideClearConfirmDialog() }) {
                    Text("取消", color = TextSecondary)
                }
            },
            containerColor = BgCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    val themeModeLabel = when (uiState.themeMode) {
        ThemeMode.SYSTEM -> "跟随系统"
        ThemeMode.LIGHT -> "浅色模式"
        ThemeMode.DARK -> "深色模式"
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BgPrimary
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BgPrimary)
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 32.dp, top = 16.dp)
        ) {

            // ── 1. 用户信息卡片 ──────────────────────────────
            item {
                UserInfoCard(
                    username = uiState.username,
                    avatarUri = uiState.avatarUri,
                    usageDays = uiState.usageDays,
                    onClick = onNavigateToUserInfo
                )
            }

            // ── 2. 数据概览（2×2 宫格）────────────────────────
            item {
                Spacer(modifier = Modifier.height(12.dp))
                StatsOverviewCard(
                    completedCount = uiState.completedCount,
                    streakDays = uiState.streakDays,
                    pendingCount = uiState.pendingCount,
                    overdueCount = uiState.overdueCount
                )
            }

            // ── 3. 成就卡片 ──────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(12.dp))
                AchievementCard(
                    achievements = uiState.recentAchievements.map { progress ->
                        AchievementBadge(
                            icon = if (progress.isUnlocked) progress.type.icon else "🔒",
                            count = progress.currentValue,
                            unlocked = progress.isUnlocked
                        )
                    }.let { list ->
                        if (list.size < 6) list + List(6 - list.size) { AchievementBadge("🔒", 0, false) }
                        else list
                    },
                    unlockedCount = uiState.unlockedAchievementsCount,
                    totalCount = uiState.totalAchievementsCount,
                    onViewAllClick = onNavigateToAchievement
                )
            }

            // ── 4. 功能区 ────────────────────────────────────
            item {
                SectionHeader(title = "功能")
            }

            item {
                SettingsGroupCard {
                    SettingsRow(
                        icon = "📍",
                        iconBgColor = Color(0xFF2196F3),
                        title = "地理围栏",
                        subtitle = if (uiState.geofenceCount > 0) "${uiState.geofenceCount} 个地点 · 已启用" else "暂未设置地点",
                        onClick = onNavigateToGeofence
                    )
                    RowDivider()
                    SettingsRow(
                        icon = "🎨",
                        iconBgColor = Color(0xFF9C27B0),
                        title = "主题设置",
                        subtitle = "当前：$themeModeLabel",
                        onClick = { viewModel.showThemeDialog() }
                    )
                    RowDivider()
                    SettingsRow(
                        icon = "✨",
                        iconBgColor = Color(0xFF7C4DFF),
                        title = "AI 智能助手",
                        subtitle = if (uiState.aiApiKey.isNotBlank())
                            "${uiState.aiProvider.displayName} · 已配置"
                        else "未配置，点击设置 API Key",
                        onClick = { viewModel.showAIConfigDialog() }
                    )
                    RowDivider()
                    SettingsRow(
                        icon = "🎙",
                        iconBgColor = Color(0xFF0277BD),
                        title = "讯飞语音识别",
                        subtitle = if (uiState.iflyAppId.isNotBlank())
                            "已配置 · ${accentDisplayName(uiState.iflyAccent)}"
                        else "未配置，点击设置讯飞 AppID",
                        onClick = { viewModel.showIFlyConfigDialog() }
                    )
                }
            }

            // ── 5. 数据区 ────────────────────────────────────
            item {
                SectionHeader(title = "数据")
            }

            item {
                SettingsGroupCard {
                    SettingsRow(
                        icon = "📤",
                        iconBgColor = Color(0xFF42A5F5),
                        title = "导出数据",
                        subtitle = "通过邮件分享任务列表",
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:")
                                putExtra(Intent.EXTRA_SUBJECT, "NextThing 任务导出")
                                putExtra(Intent.EXTRA_TEXT, "（功能开发中，即将支持导出完整任务数据）")
                            }
                            context.startActivity(Intent.createChooser(intent, "分享"))
                        }
                    )
                    RowDivider()
                    SettingsRow(
                        icon = "🗑️",
                        iconBgColor = Danger.copy(alpha = 0.8f),
                        title = "清除已完成",
                        subtitle = if (uiState.completedCount > 0) "删除全部 ${uiState.completedCount} 条已完成任务" else "暂无已完成任务",
                        titleColor = if (uiState.completedCount > 0) Danger else TextMuted,
                        onClick = {
                            if (uiState.completedCount > 0) viewModel.showClearConfirmDialog()
                        }
                    )
                }
            }

            // ── 6. 关于区 ────────────────────────────────────
            item {
                SectionHeader(title = "关于")
            }

            item {
                SettingsGroupCard {
                    SettingsRow(
                        icon = "💬",
                        iconBgColor = Color(0xFF66BB6A),
                        title = "意见反馈",
                        subtitle = "帮助我们改进 NextThing",
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:")
                                putExtra(Intent.EXTRA_SUBJECT, "NextThing 意见反馈")
                            }
                            context.startActivity(Intent.createChooser(intent, "发送反馈"))
                        }
                    )
                    RowDivider()
                    SettingsRow(
                        icon = "📱",
                        iconBgColor = Color(0xFF78909C),
                        title = "关于应用",
                        subtitle = "版本 v1.0.0",
                        showArrow = false,
                        onClick = {}
                    )
                }
            }

            // ── 7. 底部版权 ──────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "NextThing v1.0.0  ·  © 2024",
                    fontSize = 12.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────
// 1. 用户信息卡片
// ──────────────────────────────────────────────────────────

@Composable
private fun UserInfoCard(
    username: String,
    avatarUri: Uri?,
    usageDays: Int,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Primary.copy(alpha = 0.08f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 头像
            if (avatarUri != null) {
                AsyncImage(
                    model = avatarUri,
                    contentDescription = "用户头像",
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Primary, Primary.copy(alpha = 0.6f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "👤", fontSize = 28.sp)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = username,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "已使用 $usageDays 天",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            Text(
                text = "›",
                fontSize = 28.sp,
                color = TextMuted,
                fontWeight = FontWeight.Light
            )
        }
    }
}

// ──────────────────────────────────────────────────────────
// 2. 数据概览 2×2 宫格
// ──────────────────────────────────────────────────────────

@Composable
private fun StatsOverviewCard(
    completedCount: Int,
    streakDays: Int,
    pendingCount: Int,
    overdueCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "我的数据",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                StatCell(
                    emoji = "✅",
                    value = completedCount.toString(),
                    label = "累计完成",
                    valueColor = Success,
                    modifier = Modifier.weight(1f)
                )
                StatDivider()
                StatCell(
                    emoji = "🔥",
                    value = streakDays.toString(),
                    label = "连续天数",
                    valueColor = Color(0xFFFF7043),
                    modifier = Modifier.weight(1f)
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Border.copy(alpha = 0.6f),
                thickness = 0.5.dp
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                StatCell(
                    emoji = "📋",
                    value = pendingCount.toString(),
                    label = "进行中",
                    valueColor = Primary,
                    modifier = Modifier.weight(1f)
                )
                StatDivider()
                StatCell(
                    emoji = "⏰",
                    value = overdueCount.toString(),
                    label = "已逾期",
                    valueColor = if (overdueCount > 0) Danger else TextMuted,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatCell(
    emoji: String,
    value: String,
    label: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = emoji, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(0.5.dp)
            .height(64.dp)
            .background(Border.copy(alpha = 0.6f))
    )
}

// ──────────────────────────────────────────────────────────
// 3. 成就卡片
// ──────────────────────────────────────────────────────────

data class AchievementBadge(
    val icon: String,
    val count: Int,
    val unlocked: Boolean
)

@Composable
private fun AchievementCard(
    achievements: List<AchievementBadge>,
    unlockedCount: Int,
    totalCount: Int,
    onViewAllClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF8E1)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "🏆", fontSize = 18.sp)
                    Text(
                        text = "我的成就",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                TextButton(
                    onClick = onViewAllClick,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "查看全部 →",
                        fontSize = 12.sp,
                        color = Primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(achievements) { AchievementBadgeItem(it) }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 进度条
            val progress = if (totalCount > 0) unlockedCount.toFloat() / totalCount else 0f
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFFFFB300),
                    trackColor = Color(0xFFEEEEEE)
                )
                Text(
                    text = "$unlockedCount/$totalCount",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun AchievementBadgeItem(badge: AchievementBadge) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color(0xFFEEEEEE))
                .alpha(if (badge.unlocked) 1f else 0.4f),
            contentAlignment = Alignment.Center
        ) {
            Text(text = badge.icon, fontSize = 24.sp)
        }
        Text(
            text = if (badge.unlocked) "${badge.count}" else "?",
            fontSize = 11.sp,
            fontWeight = if (badge.unlocked) FontWeight.Bold else FontWeight.Normal,
            color = if (badge.unlocked) TextPrimary else TextMuted
        )
    }
}

// ──────────────────────────────────────────────────────────
// 4. 通用设置组件
// ──────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = TextSecondary,
        modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 20.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingsGroupCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsRow(
    icon: String,
    iconBgColor: Color,
    title: String,
    subtitle: String,
    titleColor: Color = TextPrimary,
    showArrow: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBgColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = titleColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        if (showArrow) {
            Text(
                text = "›",
                fontSize = 22.sp,
                color = TextMuted,
                fontWeight = FontWeight.Light
            )
        }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 68.dp, end = 16.dp),
        color = Border.copy(alpha = 0.6f),
        thickness = 0.5.dp
    )
}

// ──────────────────────────────────────────────────────────
// 5. 主题选择弹窗
// ──────────────────────────────────────────────────────────

@Composable
private fun ThemePickerDialog(
    currentMode: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "主题设置",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                ThemeModeOption(
                    icon = "⚙️", label = "跟随系统", description = "自动切换明暗模式",
                    selected = currentMode == ThemeMode.SYSTEM,
                    onClick = { onSelect(ThemeMode.SYSTEM) }
                )
                ThemeModeOption(
                    icon = "☀️", label = "浅色模式", description = "始终使用浅色主题",
                    selected = currentMode == ThemeMode.LIGHT,
                    onClick = { onSelect(ThemeMode.LIGHT) }
                )
                ThemeModeOption(
                    icon = "🌙", label = "深色模式", description = "始终使用深色主题",
                    selected = currentMode == ThemeMode.DARK,
                    onClick = { onSelect(ThemeMode.DARK) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭", color = Primary)
            }
        },
        containerColor = BgCard,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun ThemeModeOption(
    icon: String,
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Primary.copy(alpha = 0.1f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = icon, fontSize = 20.sp, modifier = Modifier.size(26.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) Primary else TextPrimary
            )
            Text(text = description, fontSize = 12.sp, color = TextSecondary)
        }
        if (selected) {
            Text(text = "✓", fontSize = 15.sp, color = Primary, fontWeight = FontWeight.Bold)
        }
    }
}

// ──────────────────────────────────────────────────────────
// 数据类（Screen 内部使用）
// ──────────────────────────────────────────────────────────

data class SettingSection(val title: String? = null, val items: List<SettingItem>)

data class SettingItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val icon: String,
    val color: Color,
    val type: SettingType,
    val isEnabled: Boolean = false,
    val value: String? = null
)

enum class SettingType { SWITCH, ARROW, TEXT }

// ══════════════════════════════════════════════════════════════
// AI 配置弹窗
// ══════════════════════════════════════════════════════════════

@Composable
private fun AIConfigDialog(
    currentProvider: AIProvider,
    currentApiKey: String,
    currentModel: String,
    onSave: (AIProvider, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var provider by remember { mutableStateOf(currentProvider) }
    var apiKey by remember { mutableStateOf(currentApiKey) }
    var model by remember { mutableStateOf(currentModel) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "AI 智能助手配置",
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // 提供商选择
                Text("服务提供商", fontSize = 13.sp, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AIProvider.entries.forEach { p ->
                        FilterChip(
                            selected = provider == p,
                            onClick = {
                                provider = p
                                if (model.isBlank()) model = ""
                            },
                            label = { Text(p.displayName, fontSize = 13.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Primary.copy(alpha = 0.15f),
                                selectedLabelColor = Primary
                            )
                        )
                    }
                }

                // API Key 输入
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    placeholder = { Text("sk-...", color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Border
                    )
                )

                // 模型名称（可选）
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("模型名称（留空使用默认）") },
                    placeholder = { Text(provider.defaultModel, color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Border
                    )
                )

                // 默认模型提示
                Text(
                    "默认模型：${provider.defaultModel}",
                    fontSize = 11.sp,
                    color = TextMuted
                )

                // 安全提示
                Text(
                    "API Key 仅保存在本地设备，不会上传",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(provider, apiKey.trim(), model.trim()) }
            ) {
                Text("保存", color = Primary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = TextSecondary)
            }
        }
    )
}

// ── 方言显示名 ────────────────────────────────────────────────────

private fun accentDisplayName(accent: String) = when (accent) {
    "mandarin"  -> "普通话"
    "lmz"       -> "四川话"
    "cantonese" -> "粤语"
    "jnu"       -> "江淮话"
    "wuu"       -> "吴语"
    else        -> accent
}

// ── 讯飞 ASR 配置弹窗 ─────────────────────────────────────────────

@Composable
private fun IFlyConfigDialog(
    currentAppId: String,
    currentApiKey: String,
    currentApiSecret: String,
    currentAccent: String,
    onSave: (String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var appId     by remember { mutableStateOf(currentAppId) }
    var apiKey    by remember { mutableStateOf(currentApiKey) }
    var apiSecret by remember { mutableStateOf(currentApiSecret) }
    var accent    by remember { mutableStateOf(currentAccent) }

    val accentOptions = listOf(
        "lmz"       to "四川话",
        "mandarin"  to "普通话",
        "cantonese" to "粤语",
        "jnu"       to "江淮话",
        "wuu"       to "吴语"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("讯飞语音识别配置", fontWeight = FontWeight.Bold, color = TextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 方言选择
                Text("识别语言 / 方言", fontSize = 13.sp, color = TextSecondary)
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(accentOptions.size) { i ->
                        val (code, label) = accentOptions[i]
                        FilterChip(
                            selected = accent == code,
                            onClick = { accent = code },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF0277BD).copy(alpha = 0.15f),
                                selectedLabelColor = Color(0xFF0277BD)
                            )
                        )
                    }
                }

                // AppID
                OutlinedTextField(
                    value = appId,
                    onValueChange = { appId = it },
                    label = { Text("AppID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF0277BD),
                        unfocusedBorderColor = Border
                    )
                )

                // APIKey
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("APIKey") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF0277BD),
                        unfocusedBorderColor = Border
                    )
                )

                // APISecret
                OutlinedTextField(
                    value = apiSecret,
                    onValueChange = { apiSecret = it },
                    label = { Text("APISecret") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF0277BD),
                        unfocusedBorderColor = Border
                    )
                )

                Text(
                    "凭证在讯飞开放平台 → 我的应用 → 语音听写 中获取",
                    fontSize = 11.sp,
                    color = TextMuted
                )
                Text(
                    "所有凭证仅保存在本地设备，不会上传",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(appId.trim(), apiKey.trim(), apiSecret.trim(), accent) }
            ) {
                Text("保存", color = Color(0xFF0277BD), fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = TextSecondary)
            }
        }
    )
}
