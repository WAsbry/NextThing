package com.nextthing.app.presentation.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import com.nextthing.app.R
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.nextthing.app.domain.model.ThemeMode
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nextthing.app.data.preferences.AIPreferences
import com.nextthing.app.data.preferences.AIProvider
import com.nextthing.app.data.preferences.ASRProvider
import com.nextthing.app.presentation.theme.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateToUserInfo: () -> Unit = {},
    onNavigateToGeofence: () -> Unit = {},
    onNavigateToAchievement: () -> Unit = {},
    onNavigateToViewPreferences: () -> Unit = {},
    onNavigateToThemeSettings: () -> Unit = {},
    onNavigateToSync: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 相册选择头像
    val avatarPickerLauncher = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        uri?.let { viewModel.updateAvatar(it) }
    }

    // 帮助弹窗状态
    var showAIHelpDialog by remember { mutableStateOf(false) }
    var showIFlyHelpDialog by remember { mutableStateOf(false) }
    var showASRHelpDialog by remember { mutableStateOf(false) }

    // 监听操作消息，弹 Snackbar
    LaunchedEffect(uiState.actionMessage) {
        uiState.actionMessage?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            viewModel.clearActionMessage()
        }
    }

    // 导出成功后自动触发分享
    LaunchedEffect(uiState.exportResultUri) {
        uiState.exportResultUri?.let { uri ->
            val mimeType = uiState.exportResultMimeType ?: "*/*"
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "导出任务数据"))
            viewModel.clearExportResult()
        }
    }

    // 导出配置弹窗
    if (uiState.showExportSheet) {
        ExportBottomSheet(
            isExporting = uiState.isExporting,
            onDismiss = { viewModel.hideExportSheet() },
            onExport = { startDate, endDate, format ->
                viewModel.exportData(startDate, endDate, format)
            }
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

    // 语音识别配置弹窗
    if (uiState.showASRConfigDialog) {
        ASRConfigDialog(
            currentProvider = uiState.asrProvider,
            currentZhipuApiKey = uiState.zhipuApiKey,
            currentIFlyAppId = uiState.iflyAppId,
            currentIFlyApiKey = uiState.iflyApiKey,
            currentIFlyApiSecret = uiState.iflyApiSecret,
            currentIFlyAccent = uiState.iflyAccent,
            onProviderChange = { viewModel.saveASRProvider(it) },
            onSaveZhiPu = { viewModel.saveZhiPuApiKey(it) },
            onSaveIFly = { appId, key, secret, accent ->
                viewModel.saveIFlyConfig(appId, key, secret, accent)
            },
            onDismiss = { viewModel.hideASRConfigDialog() }
        )
    }

    // 早晚报配置弹窗
    if (uiState.showBriefingDialog) {
        BriefingConfigDialog(
            enabled = uiState.briefingEnabled,
            morningHour = uiState.morningHour,
            morningMinute = uiState.morningMinute,
            eveningHour = uiState.eveningHour,
            eveningMinute = uiState.eveningMinute,
            onSave = { enabled, mH, mM, eH, eM ->
                viewModel.saveBriefingSettings(enabled, mH, mM, eH, eM)
            },
            onDismiss = { viewModel.hideBriefingDialog() }
        )
    }

    // AI 智能助手帮助弹窗
    if (showAIHelpDialog) {
        AIHelpDialog(onDismiss = { showAIHelpDialog = false })
    }

    // 语音识别帮助弹窗
    if (showASRHelpDialog) {
        ASRHelpDialog(
            currentProvider = uiState.asrProvider,
            onDismiss = { showASRHelpDialog = false }
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
        ThemeMode.SYSTEM  -> "跟随系统"
        ThemeMode.LIGHT   -> "浅色模式"
        ThemeMode.DARK    -> "深色模式"
        ThemeMode.WEATHER -> "跟随天气"
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BgPrimary,
        // 外层 NavHost 已处理系统 insets，内层不重复添加
        contentWindowInsets = WindowInsets(0.dp)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BgPrimary)
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp)
        ) {

            // ── 1. 用户信息 + 成就卡片（合并） ──────────────────
            item {
                UserProfileCard(
                    username = uiState.username,
                    avatarUri = uiState.avatarUri,
                    usageDays = uiState.usageDays,
                    completedCount = uiState.completedCount,
                    streakDays = uiState.streakDays,
                    achievements = uiState.recentAchievements,
                    unlockedCount = uiState.unlockedAchievementsCount,
                    totalCount = uiState.totalAchievementsCount,
                    onUserClick = onNavigateToUserInfo,
                    onAchievementClick = onNavigateToAchievement,
                    onAvatarClick = {
                        avatarPickerLauncher.launch(
                            PickVisualMediaRequest(PickVisualMedia.ImageOnly)
                        )
                    }
                )
            }

            // ── 2. 功能区 ────────────────────────────────────
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
                        onClick = onNavigateToThemeSettings
                    )
                    RowDivider()
                    SettingsRow(
                        icon = "👁️",
                        iconBgColor = Color(0xFF00897B),
                        title = "视图偏好",
                        subtitle = "折叠视图、显示模式",
                        onClick = onNavigateToViewPreferences
                    )
                    RowDivider()
                    SettingsRow(
                        icon = "☁️",
                        iconBgColor = Color(0xFF1565C0),
                        title = "数据同步",
                        subtitle = "云端同步、冲突解决",
                        onClick = onNavigateToSync
                    )
                    RowDivider()
                    SettingsRow(
                        icon = "✨",
                        iconBgColor = Color(0xFF7C4DFF),
                        title = "AI 智能助手",
                        subtitle = if (uiState.aiApiKey.isNotBlank())
                            "${uiState.aiProvider.displayName} · 已配置"
                        else "未配置，点击设置 API Key",
                        onClick = { viewModel.showAIConfigDialog() },
                        onHelpClick = { showAIHelpDialog = true }
                    )
                    RowDivider()
                    SettingsRow(
                        icon = "🎙",
                        iconBgColor = Color(0xFF0277BD),
                        title = "语音识别",
                        subtitle = when (uiState.asrProvider) {
                            ASRProvider.IFLY -> if (uiState.iflyAppId.isNotBlank())
                                "讯飞 · 已配置 · ${accentDisplayName(uiState.iflyAccent)}"
                            else "讯飞 · 未配置"
                            ASRProvider.ZHIPU -> if (uiState.zhipuApiKey.isNotBlank())
                                "智谱 · 已配置"
                            else "智谱 · 未配置"
                        },
                        onClick = { viewModel.showASRConfigDialog() },
                        onHelpClick = { showASRHelpDialog = true }
                    )
                    RowDivider()
                    SettingsRow(
                        icon = "📰",
                        iconBgColor = Color(0xFFFF6D00),
                        title = "智能早晚报",
                        subtitle = if (uiState.briefingEnabled)
                            "已开启 · 早报 ${String.format("%02d:%02d", uiState.morningHour, uiState.morningMinute)} / 晚报 ${String.format("%02d:%02d", uiState.eveningHour, uiState.eveningMinute)}"
                        else "未开启，点击配置",
                        onClick = { viewModel.showBriefingDialog() }
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
                        subtitle = "导出任务为 Excel/CSV/Markdown",
                        onClick = { viewModel.showExportSheet() }
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
// 1. 用户资料 + 成就（合并卡片）
// ──────────────────────────────────────────────────────────

@Composable
private fun UserProfileCard(
    username: String,
    avatarUri: Uri?,
    usageDays: Int,
    completedCount: Int,
    streakDays: Int,
    achievements: List<com.nextthing.app.domain.model.AchievementProgress>,
    unlockedCount: Int,
    totalCount: Int,
    onUserClick: () -> Unit,
    onAchievementClick: () -> Unit,
    onAvatarClick: () -> Unit = {}
) {
    // 每个分类中已解锁的最高等级成就
    val bestBadges = remember(achievements) {
        achievements
            .filter { it.isUnlocked }
            .groupBy { it.type.category }
            .mapValues { (_, list) -> list.maxByOrNull { it.type.tier.ordinal }!! }
            .entries
            .sortedBy { it.key.ordinal }
            .map { it.value }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // ── 上半部分：头像 + 昵称 + 徽章 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onUserClick)
                    .padding(start = 20.dp, end = 16.dp, top = 18.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 头像（圆角矩形，点击换图）
                // fallback/error/placeholder 统一兜底为预置头像，避免 URI 无效时空白
                val avatarShape = RoundedCornerShape(14.dp)
                val presetPainter = painterResource(R.drawable.preset_avatar)
                AsyncImage(
                    model = avatarUri,
                    contentDescription = "用户头像",
                    placeholder = presetPainter,
                    error = presetPainter,
                    fallback = presetPainter,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(avatarShape)
                        .clickable(onClick = onAvatarClick),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(14.dp))

                // 昵称 + 成就徽章（同行）
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = username,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "已使用 $usageDays 天",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    // 最佳徽章（与昵称同行，靠右）
                    if (bestBadges.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                            bestBadges.take(5).forEach { progress ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(getBadgeTierBg(progress.type.tier)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = progress.type.icon, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }
            }

            // ── 分隔线 ──
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = Border.copy(alpha = 0.5f),
                thickness = 0.5.dp
            )

            // ── 下半部分：数据摘要 + 成就入口 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧：核心数据
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    MiniStat(value = "$completedCount", label = "已完成", color = Success)
                    MiniStat(value = "$streakDays", label = "连续天数", color = Color(0xFFFF7043))
                }

                // 右下角：成就入口
                Surface(
                    onClick = onAchievementClick,
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFFFF8E1)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "🏆", fontSize = 14.sp)
                        Text(
                            text = "$unlockedCount/$totalCount",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFF8F00)
                        )
                        Text(
                            text = "›",
                            fontSize = 16.sp,
                            color = Color(0xFFFFB300),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniStat(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextSecondary
        )
    }
}

private fun getBadgeTierBg(tier: com.nextthing.app.domain.model.AchievementTier): Color {
    return when (tier) {
        com.nextthing.app.domain.model.AchievementTier.BRONZE -> Color(0xFFCD7F32).copy(alpha = 0.2f)
        com.nextthing.app.domain.model.AchievementTier.SILVER -> Color(0xFFC0C0C0).copy(alpha = 0.3f)
        com.nextthing.app.domain.model.AchievementTier.GOLD -> Color(0xFFFFD700).copy(alpha = 0.25f)
        com.nextthing.app.domain.model.AchievementTier.DIAMOND -> Color(0xFF00BCD4).copy(alpha = 0.2f)
    }
}

// ──────────────────────────────────────────────────────────
// 2. 通用设置组件
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
    onHelpClick: (() -> Unit)? = null,
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

        if (onHelpClick != null) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF0F0F0))
                    .clickable(onClick = onHelpClick),
                contentAlignment = Alignment.Center
            ) {
                Text("?", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
            }
            Spacer(modifier = Modifier.width(8.dp))
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

// ── 语音识别配置弹窗 ─────────────────────────────────────────────

@Composable
private fun ASRConfigDialog(
    currentProvider: ASRProvider,
    currentZhipuApiKey: String,
    currentIFlyAppId: String,
    currentIFlyApiKey: String,
    currentIFlyApiSecret: String,
    currentIFlyAccent: String,
    onProviderChange: (ASRProvider) -> Unit,
    onSaveZhiPu: (String) -> Unit,
    onSaveIFly: (String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedProvider by remember { mutableStateOf(currentProvider) }
    var zhipuApiKey by remember { mutableStateOf(currentZhipuApiKey) }
    var appId     by remember { mutableStateOf(currentIFlyAppId) }
    var apiKey    by remember { mutableStateOf(currentIFlyApiKey) }
    var apiSecret by remember { mutableStateOf(currentIFlyApiSecret) }
    var accent    by remember { mutableStateOf(currentIFlyAccent) }

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
            Text("语音识别配置", fontWeight = FontWeight.Bold, color = TextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Provider 选择
                Text("识别引擎", fontSize = 13.sp, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ASRProvider.entries.forEach { provider ->
                        FilterChip(
                            selected = selectedProvider == provider,
                            onClick = {
                                selectedProvider = provider
                                onProviderChange(provider)
                            },
                            label = { Text(provider.displayName, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF0277BD).copy(alpha = 0.15f),
                                selectedLabelColor = Color(0xFF0277BD)
                            )
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(vertical = 4.dp))

                // 根据 provider 显示对应配置
                if (selectedProvider == ASRProvider.ZHIPU) {
                    OutlinedTextField(
                        value = zhipuApiKey,
                        onValueChange = { zhipuApiKey = it },
                        label = { Text("智谱 API Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF0277BD),
                            unfocusedBorderColor = Border
                        )
                    )
                    Text(
                        "API Key 在 open.bigmodel.cn → API Keys 中获取",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                } else {
                    // 讯飞配置
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
                }

                Text(
                    "所有凭证仅保存在本地设备，不会上传",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedProvider == ASRProvider.ZHIPU) {
                        onSaveZhiPu(zhipuApiKey.trim())
                    } else {
                        onSaveIFly(appId.trim(), apiKey.trim(), apiSecret.trim(), accent)
                    }
                }
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

// ══════════════════════════════════════════════════════════════
// AI 智能助手 帮助弹窗（全屏）
// ══════════════════════════════════════════════════════════════

@Composable
private fun AIHelpDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                HelpTopBar(title = "AI 智能助手 · 配置指南", onClose = onDismiss)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    HelpSection(
                        title = "这是什么功能？",
                        content = "AI 智能助手可以帮你用自然语言快速创建任务。\n\n" +
                                "比如你输入「明天下午3点开项目周会」，AI 会自动识别出：\n" +
                                "  · 任务标题：开项目周会\n" +
                                "  · 截止时间：明天 15:00\n\n" +
                                "目前支持 DeepSeek 和 通义千问 两个 AI 服务。"
                    )

                    HorizontalDivider(color = Color(0xFFF0F0F0))

                    HelpSection(
                        title = "DeepSeek API Key 获取步骤",
                        content = "1. 打开 DeepSeek 开放平台：\n" +
                                "   https://platform.deepseek.com\n\n" +
                                "2. 注册并登录账号\n\n" +
                                "3. 左侧菜单 → 「API Keys」\n\n" +
                                "4. 点击「创建 API Key」\n\n" +
                                "5. 复制生成的 Key（sk-开头）\n\n" +
                                "6. 回到本 App → 设置 → AI 智能助手\n" +
                                "   选择 DeepSeek → 粘贴 API Key → 保存\n\n" +
                                "费用：注册后有免费额度，正常使用每次约 0.001 元"
                    )

                    HorizontalDivider(color = Color(0xFFF0F0F0))

                    HelpSection(
                        title = "通义千问 API Key 获取步骤",
                        content = "1. 打开阿里云百炼平台：\n" +
                                "   https://bailian.console.aliyun.com\n\n" +
                                "2. 使用支付宝/阿里云账号登录\n\n" +
                                "3. 右上角头像 → 「API-KEY 管理」\n\n" +
                                "4. 点击「创建新的 API Key」\n\n" +
                                "5. 复制生成的 Key（sk-开头）\n\n" +
                                "6. 回到本 App → 设置 → AI 智能助手\n" +
                                "   选择通义千问 → 粘贴 API Key → 保存\n\n" +
                                "费用：新用户有免费调用额度"
                    )

                    HorizontalDivider(color = Color(0xFFF0F0F0))

                    HelpSection(
                        title = "如何使用？",
                        content = "配置完成后，进入「创建任务」页面：\n\n" +
                                "1. 在顶部「AI 智能输入」框中输入自然语言描述\n" +
                                "   例如：后天去超市买东西\n\n" +
                                "2. 点击右侧 🔮 按钮发送\n\n" +
                                "3. AI 解析后会显示识别结果卡片\n\n" +
                                "4. 你可以选择：\n" +
                                "   · 继续编辑 — 填入表单后手动调整\n" +
                                "   · 直接创建 — 立刻保存到任务列表"
                    )

                    HorizontalDivider(color = Color(0xFFF0F0F0))

                    HelpSection(
                        title = "常见问题",
                        content = "Q: 模型名称需要填吗？\n" +
                                "A: 不需要，留空即可，会使用默认模型。\n\n" +
                                "Q: API Key 安全吗？\n" +
                                "A: Key 仅保存在你的手机本地，不会上传到任何服务器。\n\n" +
                                "Q: 提示「API Key 无效」？\n" +
                                "A: 请检查是否复制完整（包括 sk- 前缀），以及是否选对了服务商。"
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// 语音识别 帮助弹窗（全屏）
// ══════════════════════════════════════════════════════════════

@Composable
private fun ASRHelpDialog(
    currentProvider: ASRProvider,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                HelpTopBar(title = "语音识别 · 配置指南", onClose = onDismiss)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    HelpSection(
                        title = "这是什么功能？",
                        content = "语音识别让你可以用说话代替打字。\n\n" +
                                "点击创建任务页面顶部的 🎙 麦克风按钮，\n" +
                                "说出任务描述，语音会自动转为文字，\n" +
                                "然后 AI 智能助手会自动解析并创建任务。"
                    )

                    HorizontalDivider(color = Color(0xFFF0F0F0))

                    if (currentProvider == ASRProvider.ZHIPU) {
                        HelpSection(
                            title = "智谱 GLM-ASR",
                            content = "智谱语音识别（GLM-ASR-2512）：\n" +
                                    "· 行业领先准确率，字符错误率仅 0.0717\n" +
                                    "· 支持普通话、粤语、英语等\n" +
                                    "· 按音频时长计费：¥0.06/分钟\n" +
                                    "· 新用户注册送 2000 万 tokens\n\n" +
                                    "API Key 获取步骤：\n\n" +
                                    "1. 打开智谱开放平台：\n" +
                                    "   https://open.bigmodel.cn\n\n" +
                                    "2. 注册并登录账号\n\n" +
                                    "3. 进入「API Keys」页面\n\n" +
                                    "4. 点击「添加 API Key」\n\n" +
                                    "5. 复制生成的 Key\n\n" +
                                    "6. 回到本 App → 设置 → 语音识别\n" +
                                    "   选择「智谱」→ 粘贴 API Key → 保存"
                        )

                        HorizontalDivider(color = Color(0xFFF0F0F0))

                        HelpSection(
                            title = "使用方式",
                            content = "智谱 ASR 采用录音后上传方式：\n\n" +
                                    "1. 按下麦克风按钮，开始录音\n" +
                                    "2. 说出任务描述\n" +
                                    "3. 再次点击按钮停止录音\n" +
                                    "4. 音频上传后自动识别为文字\n" +
                                    "5. AI 自动解析并创建任务"
                        )
                    } else {
                        HelpSection(
                            title = "讯飞语音识别",
                            content = "讯飞语音识别（WebSocket 实时流式）：\n" +
                                    "· 实时边说边识别\n" +
                                    "· 支持普通话、四川话、粤语等方言\n" +
                                    "· 每天免费 500 次\n\n" +
                                    "凭证获取步骤：\n\n" +
                                    "1. 打开讯飞开放平台：\n" +
                                    "   https://www.xfyun.cn\n\n" +
                                    "2. 注册并登录账号\n\n" +
                                    "3. 进入「控制台」→「创建新应用」\n\n" +
                                    "4. 进入应用 → 左侧「语音听写（流式版）」\n" +
                                    "   点击「立即开通」\n\n" +
                                    "5. 获取 AppID / APIKey / APISecret\n\n" +
                                    "6. 回到本 App → 设置 → 语音识别\n" +
                                    "   选择「讯飞」→ 填入三个值 → 保存"
                        )

                        HorizontalDivider(color = Color(0xFFF0F0F0))

                        HelpSection(
                            title = "方言支持",
                            content = "普通话和英语：注册即可免费使用\n\n" +
                                    "四川话 / 粤语 / 其他方言：\n" +
                                    "需要在讯飞平台额外开通，方言为付费功能。\n\n" +
                                    "如果只用普通话，选择「普通话」即可。"
                        )
                    }

                    HorizontalDivider(color = Color(0xFFF0F0F0))

                    HelpSection(
                        title = "如何使用？",
                        content = "配置完成后，进入「创建任务」页面：\n\n" +
                                "1. 点击右上角 🎙 麦克风按钮\n" +
                                "   （首次使用会请求麦克风权限，请允许）\n\n" +
                                "2. 对着手机说出任务描述\n" +
                                "   例如：「明天下午三点开会」\n\n" +
                                "3. 再次点击按钮停止录音\n\n" +
                                "4. 识别出的文字会自动填入输入框\n\n" +
                                "5. AI 会自动解析并弹出结果卡片\n\n" +
                                "6. 选择「继续编辑」或「直接创建」"
                    )

                    HorizontalDivider(color = Color(0xFFF0F0F0))

                    HelpSection(
                        title = "常见问题",
                        content = "Q: 语音识别无反应？\n" +
                                "A: 请检查是否授予麦克风权限，\n" +
                                "   以及语音识别配置是否正确。\n\n" +
                                "Q: 识别不准确？\n" +
                                "A: 请在安静环境说话，语速适中，靠近麦克风。\n\n" +
                                "Q: 智谱提示 401？\n" +
                                "A: API Key 无效，请到 open.bigmodel.cn 重新获取。\n\n" +
                                "Q: 讯飞提示鉴权失败？\n" +
                                "A: 请检查 AppID、APIKey、APISecret 是否正确。"
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

// ── 帮助弹窗通用组件 ──────────────────────────────────────────────

@Composable
private fun HelpTopBar(title: String, onClose: () -> Unit) {
    Surface(
        shadowElevation = 2.dp,
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF0F0F0))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Text("✕", fontSize = 15.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun HelpSection(title: String, content: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = content,
            fontSize = 14.sp,
            color = TextSecondary,
            lineHeight = 22.sp
        )
    }
}

// ── 智能早晚报配置弹窗 ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BriefingConfigDialog(
    enabled: Boolean,
    morningHour: Int,
    morningMinute: Int,
    eveningHour: Int,
    eveningMinute: Int,
    onSave: (enabled: Boolean, morningHour: Int, morningMinute: Int, eveningHour: Int, eveningMinute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var isEnabled by remember { mutableStateOf(enabled) }
    var mHour by remember { mutableStateOf(morningHour) }
    var mMinute by remember { mutableStateOf(morningMinute) }
    var eHour by remember { mutableStateOf(eveningHour) }
    var eMinute by remember { mutableStateOf(eveningMinute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "📰 智能早晚报",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "开启后，AI 将在你设定的时间推送任务简报通知。需要在「AI 智能助手」中配置 API Key。",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )

                // 开关
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("启用早晚报", fontSize = 15.sp, color = TextPrimary)
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it }
                    )
                }

                if (isEnabled) {
                    // 早报时间
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("☀️ 早报时间", fontSize = 14.sp, color = TextPrimary)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BriefingTimePicker(
                                value = mHour, range = 5..12,
                                onValueChange = { mHour = it },
                                label = { String.format("%02d", it) }
                            )
                            Text(":", fontSize = 16.sp, color = TextPrimary)
                            BriefingTimePicker(
                                value = mMinute, range = 0..59 step 15,
                                onValueChange = { mMinute = it },
                                label = { String.format("%02d", it) }
                            )
                        }
                    }

                    // 晚报时间
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🌙 晚报时间", fontSize = 14.sp, color = TextPrimary)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BriefingTimePicker(
                                value = eHour, range = 18..23,
                                onValueChange = { eHour = it },
                                label = { String.format("%02d", it) }
                            )
                            Text(":", fontSize = 16.sp, color = TextPrimary)
                            BriefingTimePicker(
                                value = eMinute, range = 0..59 step 15,
                                onValueChange = { eMinute = it },
                                label = { String.format("%02d", it) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(isEnabled, mHour, mMinute, eHour, eMinute) }) {
                Text("保存", color = Primary, fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = TextSecondary)
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun BriefingTimePicker(
    value: Int,
    range: IntProgression,
    onValueChange: (Int) -> Unit,
    label: (Int) -> String
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Text(
            text = label(value),
            fontSize = 16.sp,
            color = Primary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clickable { expanded = true }
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            range.forEach { v ->
                DropdownMenuItem(
                    text = { Text(label(v), fontSize = 14.sp) },
                    onClick = { onValueChange(v); expanded = false }
                )
            }
        }
    }
}
