package com.nextthing.app.presentation.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nextthing.app.domain.model.ThemeMode
import com.nextthing.app.domain.model.WeatherCondition
import com.nextthing.app.presentation.theme.*
import kotlinx.coroutines.launch

// 预设颜色调色板（供天气主题自定义主色）
private val COLOR_PALETTE = listOf(
    0xFFFFCC80L, 0xFFFFB74DL, 0xFFFFA726L, // 暖黄橙
    0xFFFFAB91L, 0xFFFF8A65L, 0xFFFF7043L, // 珊瑚橙
    0xFFF48FB1L, 0xFFF06292L, 0xFFE91E63L, // 粉红
    0xFFCE93D8L, 0xFFBA68C8L, 0xFF9C27B0L, // 丁香紫
    0xFF90CAF9L, 0xFF64B5F6L, 0xFF42A5F5L, // 天蓝
    0xFF80DEAL, 0xFF4DD0E1L, 0xFF26C6DAL, // 青绿
    0xFFA5D6A7L, 0xFF81C784L, 0xFF66BB6AL, // 草绿
    0xFFB0BEC5L, 0xFF90A4AEL, 0xFF78909CL, // 蓝灰
)

private data class WeatherItem(
    val condition: WeatherCondition,
    val icon: String,
    val label: String
)

private val WEATHER_ITEMS = listOf(
    WeatherItem(WeatherCondition.SUNNY,         "☀️", "晴天"),
    WeatherItem(WeatherCondition.CLOUDY,        "☁️", "阴天"),
    WeatherItem(WeatherCondition.PARTLY_CLOUDY, "⛅", "多云"),
    WeatherItem(WeatherCondition.RAINY,         "🌧️", "雨天"),
    WeatherItem(WeatherCondition.THUNDERSTORM,  "⛈️", "雷雨"),
    WeatherItem(WeatherCondition.SNOWY,         "❄️", "雪天"),
    WeatherItem(WeatherCondition.FOGGY,         "🌫️", "雾天"),
    WeatherItem(WeatherCondition.WINDY,         "💨", "风天"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    viewModel: ThemeSettingsViewModel = hiltViewModel(),
    onBackPressed: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // 点击天气卡片激活预览时，自动滚动到预览面板（item index 2）
    LaunchedEffect(uiState.previewCondition) {
        if (uiState.previewCondition != null) {
            scope.launch { listState.animateScrollToItem(2) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
    ) {
        // 顶部导航栏（与 AchievementScreen 保持一致）
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
                    text = "主题设置",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.size(48.dp))
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // ── 主题模式选择 ──────────────────────────────────
            item {
                SectionLabel("主题模式")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BgCard),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        ThemeModeRow("⚙️", "跟随系统", "自动切换明暗模式",
                            selected = uiState.themeMode == ThemeMode.SYSTEM,
                            onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) })
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Border.copy(alpha = 0.5f), thickness = 0.5.dp)
                        ThemeModeRow("☀️", "浅色模式", "始终使用浅色主题",
                            selected = uiState.themeMode == ThemeMode.LIGHT,
                            onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) })
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Border.copy(alpha = 0.5f), thickness = 0.5.dp)
                        ThemeModeRow("🌙", "深色模式", "始终使用深色主题",
                            selected = uiState.themeMode == ThemeMode.DARK,
                            onClick = { viewModel.setThemeMode(ThemeMode.DARK) })
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Border.copy(alpha = 0.5f), thickness = 0.5.dp)
                        ThemeModeRow("🌈", "跟随天气", "天气变，配色也变",
                            selected = uiState.themeMode == ThemeMode.WEATHER,
                            onClick = { viewModel.setThemeMode(ThemeMode.WEATHER) })
                    }
                }
            }

            // ── 天气配色管理（仅 WEATHER 模式可见） ───────────────
            item {
                AnimatedVisibility(
                    visible = uiState.themeMode == ThemeMode.WEATHER,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column {
                        SectionLabel("天气配色")
                        // 2列网格
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            WEATHER_ITEMS.chunked(2).forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    row.forEach { item ->
                                        val base = weatherAppColors(item.condition)
                                        val customArgb = uiState.customPrimaries[item.condition]
                                        val colors = weatherAppColorsWithCustom(base, customArgb)
                                        val isPreview = uiState.previewCondition == item.condition

                                        WeatherThemeCard(
                                            modifier = Modifier.weight(1f),
                                            item = item,
                                            colors = colors,
                                            isPreviewActive = isPreview,
                                            hasCustomColor = customArgb != null,
                                            onPreview = {
                                                viewModel.setPreviewCondition(
                                                    if (isPreview) null else item.condition
                                                )
                                            },
                                            onEdit = { viewModel.openColorEditor(item.condition) }
                                        )
                                    }
                                    // 补空格
                                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // ── 效果预览面板（独立 item，方便滚动定位）──────────────
            item {
                val previewCond = uiState.previewCondition
                AnimatedVisibility(
                    visible = previewCond != null,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    if (previewCond != null) {
                        val base = weatherAppColors(previewCond)
                        val custom = uiState.customPrimaries[previewCond]
                        val pc = weatherAppColorsWithCustom(base, custom)
                        ThemePreviewPanel(colors = pc)
                    }
                }
            }
        }
    }

    // 颜色选择器 BottomSheet
    val editing = uiState.editingCondition
    if (editing != null) {
        val base = weatherAppColors(editing)
        val current = uiState.customPrimaries[editing]?.let { Color(it) } ?: base.primary
        ColorPickerSheet(
            currentColor = current,
            onColorPicked = { argb -> viewModel.setCustomPrimary(editing, argb) },
            onReset = { viewModel.resetCustomPrimary(editing); viewModel.closeColorEditor() },
            onDismiss = { viewModel.closeColorEditor() }
        )
    }
}

// ─────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        color = TextSecondary,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 20.dp, top = 10.dp, bottom = 8.dp)
    )
}

@Composable
private fun ThemeModeRow(
    icon: String, label: String, description: String,
    selected: Boolean, onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Primary.copy(alpha = 0.08f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = icon, fontSize = 20.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label, fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) Primary else TextPrimary
            )
            Text(text = description, fontSize = 12.sp, color = TextSecondary)
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Primary),
                contentAlignment = Alignment.Center
            ) {
                Text("✓", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun WeatherThemeCard(
    modifier: Modifier,
    item: WeatherItem,
    colors: AppColors,
    isPreviewActive: Boolean,
    hasCustomColor: Boolean,
    onPreview: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable(onClick = onPreview)
            .then(
                if (isPreviewActive)
                    Modifier.border(2.dp, colors.primary, RoundedCornerShape(14.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.bgSecondary),
        elevation = CardDefaults.cardElevation(if (isPreviewActive) 3.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = item.icon, fontSize = 22.sp)
                // 编辑按钮
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(colors.bgCard)
                        .clickable(onClick = onEdit),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎨", fontSize = 13.sp)
                }
            }
            Text(
                text = item.label, fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary
            )
            // 主色 + 辅助色展示条
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(colors.primary)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(colors.bgPrimary)
                )
            }
            if (hasCustomColor) {
                Text(
                    text = "已自定义", fontSize = 10.sp,
                    color = colors.primary, fontWeight = FontWeight.Medium
                )
            }
            if (isPreviewActive) {
                Text(
                    text = "预览中", fontSize = 10.sp,
                    color = colors.primary, fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ThemePreviewPanel(colors: AppColors) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "效果预览",
            fontSize = 13.sp, color = TextSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = colors.bgCard),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // 模拟 AppBar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.primary)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text("应用标题", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                // 模拟 Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.bgSecondary)
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("今日任务", fontSize = 12.sp, color = colors.textPrimary, fontWeight = FontWeight.Medium)
                        Text("还有 3 项待完成", fontSize = 11.sp, color = colors.textSecondary)
                    }
                }
                // 模拟按钮行
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.primary)
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text("主要按钮", fontSize = 11.sp, color = Color.White)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text("次要按钮", fontSize = 11.sp, color = colors.textSecondary)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorPickerSheet(
    currentColor: Color,
    onColorPicked: (Long) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BgCard,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "选择主色",
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary
                )
                TextButton(onClick = onReset) {
                    Text("恢复默认", color = TextSecondary, fontSize = 13.sp)
                }
            }
            // 当前色预览
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(currentColor)
                        .border(1.dp, TextMuted.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                )
                Text("当前颜色", fontSize = 13.sp, color = TextSecondary)
            }
            // 调色板网格
            val chunked = COLOR_PALETTE.chunked(6)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                chunked.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { argb ->
                            val c = Color(argb)
                            val isSelected = currentColor == c
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(c)
                                    .then(
                                        if (isSelected)
                                            Modifier.border(2.dp, TextPrimary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                        else Modifier
                                    )
                                    .clickable { onColorPicked(argb) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Text("✓", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        // 补空格
                        repeat(6 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}
