package com.nextthing.app.presentation.screens.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.ContextWrapper
import android.net.Uri
import com.nextthing.app.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.nextthing.app.domain.model.ThemeMode
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nextthing.app.data.preferences.AIPreferences
import com.nextthing.app.presentation.theme.*
import kotlinx.coroutines.launch

private val MineStatusBar = Color(0xFFF4EFFF)
private val MineBgStart = Color(0xFFF4EFFF)
private val MineBgMid = Color(0xFFF7F3FF)
private val MineBgEnd = Color(0xFFFBFAFF)
private val MineInk = Color(0xFF202331)
private val MineDeep = Color(0xFF2F2850)
private val MineSub = Color(0xFF656B78)
private val MineProfileSub = Color(0xFF7B7391)
private val MineMuted = Color(0xFFA6ACB8)
private val MineLine = Color(0xFFEEF0F5)

private fun Modifier.mineControlPanelBackground(): Modifier = drawBehind {
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(MineBgStart, MineBgMid, MineBgEnd),
            startX = 0f,
            endX = size.width
        )
    )
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFB06DFF).copy(alpha = 0.18f), Color.Transparent),
            center = Offset(size.width * 0.14f, size.height * 0.04f),
            radius = size.width * 0.45f
        )
    )
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF7057F5).copy(alpha = 0.12f), Color.Transparent),
            center = Offset(size.width * 0.92f, size.height * 0.18f),
            radius = size.width * 0.48f
        )
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateToUserInfo: () -> Unit = {},
    onNavigateToAIConfig: () -> Unit = {},
    onNavigateToGeofence: () -> Unit = {},
    onNavigateToAchievement: () -> Unit = {},
    onNavigateToViewPreferences: () -> Unit = {},
    onNavigateToThemeSettings: () -> Unit = {},
    onNavigateToSync: () -> Unit = {},
    onNavigateToBriefing: () -> Unit = {},
    onNavigateToReminderStrategy: () -> Unit = {},
    onNavigateToExportData: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val view = LocalView.current

    DisposableEffect(view) {
        val window = context.findActivity()?.window
        val previousStatusBarColor = window?.statusBarColor
        val previousLightStatusBar = window?.let {
            WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars
        }
        window?.statusBarColor = MineStatusBar.toArgb()
        window?.let {
            WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = true
        }
        onDispose {
            window?.let {
                previousStatusBarColor?.let { color -> it.statusBarColor = color }
                previousLightStatusBar?.let { light ->
                    WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = light
                }
            }
        }
    }

    // 帮助弹窗状态
    var showAIHelpDialog by remember { mutableStateOf(false) }
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
        containerColor = MineBgMid,
        // 外层 NavHost 已处理系统 insets，内层不重复添加
        contentWindowInsets = WindowInsets(0.dp)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .mineControlPanelBackground()
                .padding(innerPadding)
        ) {

            UserProfileCard(
                username = uiState.username,
                avatarUri = uiState.avatarUri,
                usageDays = uiState.usageDays,
                completedCount = uiState.completedCount,
                pendingCount = uiState.pendingCount,
                streakDays = uiState.streakDays,
                achievements = uiState.recentAchievements,
                unlockedCount = uiState.unlockedAchievementsCount,
                totalCount = uiState.totalAchievementsCount,
                onUserClick = onNavigateToUserInfo,
                onAchievementClick = onNavigateToAchievement,
                onAvatarClick = onNavigateToUserInfo
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 56.dp, top = 0.dp)
            ) {

            item {
                SectionHeader(title = "AI 增强", subtitle = "配置与自动化")
            }

            item {
                SettingsGroupCard {
                    SettingsRow(
                        icon = "AI",
                        iconBgColor = Color(0xFF4F63E7),
                        title = "AI 智能助手",
                        subtitle = if (uiState.aiApiKey.isNotBlank())
                            "DeepSeek · 已配置"
                        else "填写 DeepSeek API Key",
                        onClick = onNavigateToAIConfig
                    )
                    RowDivider()
                    SettingsRow(
                        icon = "BR",
                        iconBgColor = Color(0xFFFF7A1A),
                        title = "智能早晚报",
                        subtitle = if (uiState.briefingEnabled)
                            "已开启 · 早报 ${String.format("%02d:%02d", uiState.morningHour, uiState.morningMinute)} / 晚报 ${String.format("%02d:%02d", uiState.eveningHour, uiState.eveningMinute)}"
                        else "基于任务状态生成早报和晚报",
                        onClick = onNavigateToBriefing
                    )
                }
            }

            item {
                SectionHeader(title = "任务增强", subtitle = "触发与提醒")
            }

            item {
                SettingsGroupCard {
                    SettingsRow(
                        icon = "LOC",
                        iconBgColor = Color(0xFF2196F3),
                        title = "地理围栏",
                        subtitle = if (uiState.geofenceCount > 0) "${uiState.geofenceCount} 个地点 · 到达/离开提醒" else "地点触发、到达离开提醒",
                        onClick = onNavigateToGeofence
                    )
                    RowDivider()
                    SettingsRow(
                        icon = "REM",
                        iconBgColor = Color(0xFFF08A35),
                        title = "提醒策略",
                        subtitle = "声音、震动、提前提醒与通知方式",
                        onClick = onNavigateToReminderStrategy
                    )
                }
            }

            item {
                SectionHeader(title = "偏好设置", subtitle = "显示与主题")
            }

            item {
                SettingsGroupCard {
                    SettingsRow(
                        icon = "THE",
                        iconBgColor = Color(0xFF9C27B0),
                        title = "主题设置",
                        subtitle = "当前：$themeModeLabel",
                        onClick = onNavigateToThemeSettings
                    )
                    RowDivider()
                    SettingsRow(
                        icon = "VIEW",
                        iconBgColor = Color(0xFF00897B),
                        title = "视图偏好",
                        subtitle = "折叠视图、显示模式",
                        onClick = onNavigateToViewPreferences
                    )
                }
            }

            item {
                SectionHeader(title = "数据管理", subtitle = "同步与导出")
            }

            item {
                SettingsGroupCard {
                    SettingsRow(
                        icon = "SYNC",
                        iconBgColor = Color(0xFF1565C0),
                        title = "数据同步",
                        subtitle = "同步状态、立即同步与冲突处理",
                        onClick = onNavigateToSync
                    )
                    RowDivider()
                    SettingsRow(
                        icon = "OUT",
                        iconBgColor = Color(0xFF42A5F5),
                        title = "导出数据",
                        subtitle = "选择时间范围，导出 Excel / CSV / Markdown",
                        onClick = onNavigateToExportData
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
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
    pendingCount: Int,
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val avatarShape = CircleShape
                val presetPainter = painterResource(R.drawable.preset_avatar)
                AsyncImage(
                    model = avatarUri,
                    contentDescription = "用户头像",
                    placeholder = presetPainter,
                    error = presetPainter,
                    fallback = presetPainter,
                    modifier = Modifier
                        .size(58.dp)
                        .clip(avatarShape)
                        .clickable(onClick = onAvatarClick),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onUserClick)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = username,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = MineDeep
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        HonorTitleChip(text = "见习掌控师")
                    }

                    Spacer(modifier = Modifier.height(7.dp))

                    AchievementPreview(modifier = Modifier.clickable(onClick = onAchievementClick))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AssetStat(value = "$completedCount", label = "已完成", hint = "累计推进")
                AssetStat(
                    value = "$streakDays",
                    label = "连续天数",
                    hint = if (streakDays > 0) "习惯积累" else "习惯还未形成"
                )
                AssetStat(value = "$pendingCount", label = "待办任务", hint = "当前队列")
                AssetStat(value = "$usageDays", label = "使用天数", hint = "开始积累")
            }
        }
    }
}

@Composable
private fun AchievementPreview(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(28.dp)
            .width(78.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((-5).dp)
    ) {
        AchievementMedal(bgColor = Color(0xFF7657FF), isMain = false)
        AchievementMedal(bgColor = Color(0xFFF1A832), isMain = true)
        AchievementMedal(bgColor = Color(0xFF20B4C6), isMain = false)
    }
}

@Composable
private fun AchievementMedal(bgColor: Color, isMain: Boolean) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(
                if (isMain) {
                    Brush.sweepGradient(
                        listOf(
                            Color(0xFFFFE68A),
                            Color(0xFFF1A832),
                            Color(0xFF7657FF),
                            Color(0xFF20B4C6),
                            Color(0xFFFFE68A)
                        )
                    )
                } else {
                    Brush.linearGradient(listOf(Color.White.copy(alpha = 0.72f), bgColor))
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "★", fontSize = 10.sp, color = Color(0xFFFFF4BC), fontWeight = FontWeight.Black)
    }
}

@Composable
private fun HonorTitleChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF3A276B),
                        Color(0xFF6B4EE8),
                        Color(0xFF2B2052)
                    )
                )
            )
            .padding(start = 12.dp, end = 10.dp, top = 4.dp, bottom = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFFF9E7A9)
        )
    }
}

@Composable
private fun RowScope.AssetStat(value: String, label: String, hint: String) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(15.dp))
            .background(Color.White.copy(alpha = 0.54f))
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MineProfileSub,
            maxLines = 1
        )
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = MineDeep
        )
        Text(
            text = hint,
            fontSize = 9.sp,
            color = Color(0xFF8C85A0),
            maxLines = 1
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
private fun SectionHeader(title: String, subtitle: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            color = MineInk
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MineMuted
            )
        }
    }
}

@Composable
private fun SettingsGroupCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.78f)),
        border = BorderStroke(1.dp, Color(0xFF7057F5).copy(alpha = 0.09f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
    titleColor: Color = MineInk,
    showArrow: Boolean = true,
    showSwitch: Boolean = false,
    switchChecked: Boolean = false,
    onHelpClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                fontSize = if (icon.length > 2) 8.sp else 11.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = titleColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MineSub
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

        if (showSwitch) {
            Switch(
                checked = switchChecked,
                onCheckedChange = null,
                enabled = false,
                colors = SwitchDefaults.colors(
                    disabledCheckedThumbColor = Color.White,
                    disabledCheckedTrackColor = Primary,
                    disabledUncheckedThumbColor = Color.White,
                    disabledUncheckedTrackColor = Color(0xFFE5E8F0),
                    disabledUncheckedBorderColor = Color.Transparent
                )
            )
        }

        if (showArrow) {
            Text(
                text = "›",
                fontSize = 20.sp,
                color = MineMuted,
                fontWeight = FontWeight.Light
            )
        }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 68.dp, end = 16.dp),
        color = MineLine,
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
                                "当前配置入口只需要填写 DeepSeek API Key，模型默认使用 deepseek-v4-flash。"
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
                                "6. 回到本 App → 我的 → AI 智能助手\n" +
                                "   粘贴 API Key → 保存并启用 AI\n\n" +
                                "费用：注册后有免费额度，正常使用每次约 0.001 元"
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
                                "A: 不需要。应用默认使用 deepseek-v4-flash。\n\n" +
                                "Q: API Key 安全吗？\n" +
                                "A: Key 仅保存在你的手机本地，用于后续 AI 能力调用。\n\n" +
                                "Q: 提示「API Key 无效」？\n" +
                                "A: 请检查是否复制完整（包括 sk- 前缀），以及 DeepSeek 账号额度是否可用。"
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
                        title = "端侧离线语音识别",
                        content = "语音识别完全在本地设备运行，无需网络：\n\n" +
                                "· 基于 Sherpa-ONNX + SenseVoice 模型\n" +
                                "· 支持中英日韩粤 5 种语言\n" +
                                "· 首次使用需加载模型（约 2~3 秒）\n" +
                                "· 后续使用即时响应\n" +
                                "· 所有数据不上传，完全隐私安全"
                    )

                    HorizontalDivider(color = Color(0xFFF0F0F0))

                    HelpSection(
                        title = "如何使用？",
                        content = "进入「创建任务」页面：\n\n" +
                                "1. 等待麦克风按钮显示「长按说话」\n" +
                                "   （首次加载模型时显示「端侧模型加载中...」）\n\n" +
                                "2. 长按麦克风按钮，开始录音\n\n" +
                                "3. 说出任务描述，例如：「明天下午三点开会」\n\n" +
                                "4. 松开按钮停止录音\n\n" +
                                "5. 识别出的文字自动填入输入框\n\n" +
                                "6. AI 自动解析并创建任务"
                    )

                    HorizontalDivider(color = Color(0xFFF0F0F0))

                    HelpSection(
                        title = "常见问题",
                        content = "Q: 按钮显示「端侧模型加载中...」？\n" +
                                "A: 首次使用需加载约 230MB 模型到内存，\n" +
                                "   请等待 2~3 秒。\n\n" +
                                "Q: 识别不准确？\n" +
                                "A: 请在安静环境说话，语速适中，靠近麦克风。\n\n" +
                                "Q: 识别无反应？\n" +
                                "A: 请检查是否授予麦克风权限。"
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
