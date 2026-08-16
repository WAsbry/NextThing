package com.nextthing.app.presentation.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextthing.app.data.export.ExportFormat
import com.nextthing.app.R
import com.nextthing.app.data.export.TaskExporter
import com.nextthing.app.data.preferences.BriefingPreferences
import com.nextthing.app.domain.model.NotificationStrategy
import com.nextthing.app.domain.model.SoundSetting
import com.nextthing.app.domain.repository.NotificationStrategyRepository
import com.nextthing.app.util.NotificationHelper
import com.nextthing.app.work.TaskWorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime
import javax.inject.Inject

private val MinePageBgStart = Color(0xFFF4EFFF)
private val MinePageBgMid = Color(0xFFF7F3FF)
private val MinePageBgEnd = Color(0xFFFBFAFF)
private val MinePagePrimary = Color(0xFF7057F5)
private val MinePagePrimary2 = Color(0xFFB06DFF)
private val MinePageInk = Color(0xFF202331)
private val MinePageDeep = Color(0xFF2F2850)
private val MinePageSub = Color(0xFF656B78)
private val MinePageMuted = Color(0xFFA6ACB8)
private val MinePageLine = Color(0xFFE7E9F1)
private val MinePageDanger = Color(0xFFE64B55)
private val MinePageSuccess = Color(0xFF20A875)

private fun Modifier.mineSubPageBackground(): Modifier = drawBehind {
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(MinePageBgStart, MinePageBgMid, MinePageBgEnd),
            startX = 0f,
            endX = size.width
        )
    )
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(MinePagePrimary2.copy(alpha = 0.16f), Color.Transparent),
            center = Offset(size.width * 0.12f, size.height * 0.04f),
            radius = size.width * 0.46f
        )
    )
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(MinePagePrimary.copy(alpha = 0.10f), Color.Transparent),
            center = Offset(size.width * 0.92f, size.height * 0.16f),
            radius = size.width * 0.48f
        )
    )
}

@Composable
private fun MineSubPageScaffold(
    title: String,
    onBackPressed: () -> Unit,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    Scaffold(
        containerColor = MinePageBgMid,
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .mineSubPageBackground()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 28.dp)
        ) {
            item { MineSubTopBar(title = title, onBackPressed = onBackPressed) }
            content()
        }
    }
}

@Composable
private fun MineSubTopBar(title: String, onBackPressed: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(34.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.76f),
            border = BorderStroke(1.dp, MinePagePrimary.copy(alpha = 0.10f)),
            shadowElevation = 2.dp
        ) {
            IconButton(onClick = onBackPressed, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = MinePageDeep,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Text(
            text = title,
            color = MinePageDeep,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(start = 14.dp)
        )
    }
}

@Composable
private fun MineSubHero(title: String, subtitle: String, tag: String, color: Color = MinePagePrimary) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(color, MinePagePrimary2, Color(0xFF16A8B8))))
            .padding(16.dp)
    ) {
        Column {
            Text(tag, color = Color.White.copy(alpha = 0.78f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 8.dp))
            Text(subtitle, color = Color.White.copy(alpha = 0.82f), fontSize = 12.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun MineSectionTitle(title: String, trailing: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(title, color = MinePageInk, fontSize = 15.sp, fontWeight = FontWeight.Black)
        trailing?.let { Text(it, color = MinePageMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun MineCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.84f))
            .border(1.dp, MinePagePrimary.copy(alpha = 0.09f), RoundedCornerShape(18.dp))
    ) {
        content()
    }
}

@Composable
private fun MineInfoRow(label: String, value: String, accent: Color = MinePageDeep) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MinePageSub, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
        Text(value, color = accent, fontSize = 14.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun SegmentedOption(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color.White else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) MinePagePrimary else MinePageSub,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black
        )
    }
}

data class BriefingSettingsUiState(
    val enabled: Boolean = false,
    val morningHour: Int = 8,
    val morningMinute: Int = 0,
    val eveningHour: Int = 21,
    val eveningMinute: Int = 0
)

@HiltViewModel
class BriefingSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val briefingPreferences: BriefingPreferences
) : ViewModel() {
    private val _uiState = MutableStateFlow(BriefingSettingsUiState())
    val uiState: StateFlow<BriefingSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                briefingPreferences.enabled,
                briefingPreferences.morningHour,
                briefingPreferences.morningMinute,
                briefingPreferences.eveningHour,
                briefingPreferences.eveningMinute
            ) { enabled, morningHour, morningMinute, eveningHour, eveningMinute ->
                BriefingSettingsUiState(enabled, morningHour, morningMinute, eveningHour, eveningMinute)
            }.collect { _uiState.value = it }
        }
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            briefingPreferences.setEnabled(enabled)
            applySchedule(_uiState.value.copy(enabled = enabled))
        }
    }

    fun setMorningTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            briefingPreferences.setMorningTime(hour, minute)
            applySchedule(_uiState.value.copy(morningHour = hour, morningMinute = minute))
        }
    }

    fun setEveningTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            briefingPreferences.setEveningTime(hour, minute)
            applySchedule(_uiState.value.copy(eveningHour = hour, eveningMinute = minute))
        }
    }

    fun triggerNow() {
        TaskWorkScheduler.triggerImmediateBriefing(context)
    }

    private fun applySchedule(state: BriefingSettingsUiState) {
        if (state.enabled) {
            TaskWorkScheduler.scheduleMorningBriefing(context, state.morningHour, state.morningMinute)
            TaskWorkScheduler.scheduleEveningBriefing(context, state.eveningHour, state.eveningMinute)
        } else {
            TaskWorkScheduler.cancelBriefingWork(context)
        }
    }
}

@Composable
fun BriefingSettingsScreen(
    onBackPressed: () -> Unit,
    viewModel: BriefingSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    MineSubPageScaffold(title = "智能早晚报", onBackPressed = onBackPressed) {
        item {
            MineSubHero(
                title = "智能早晚报",
                subtitle = "基于任务状态生成早报和晚报，帮助你在一天开始和结束时快速校准计划。",
                tag = if (uiState.enabled) "已开启" else "未开启",
                color = Color(0xFFFF7A1A)
            )
        }

        item {
            MineSectionTitle("开关状态")
            MineCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("启用智能早晚报", color = MinePageInk, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        Text("开启后会按设定时间生成提醒内容", color = MinePageSub, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                    Switch(
                        checked = uiState.enabled,
                        onCheckedChange = viewModel::setEnabled,
                        colors = SwitchDefaults.colors(checkedTrackColor = MinePagePrimary)
                    )
                }
            }
        }

        item {
            MineSectionTitle("推送时间", if (uiState.enabled) "正在生效" else "关闭中")
            MineCard {
                TimeSettingRow(
                    title = "早报",
                    subtitle = "一天开始时提醒今日重点",
                    hour = uiState.morningHour,
                    minute = uiState.morningMinute,
                    onTimeChange = viewModel::setMorningTime
                )
                HorizontalDivider(color = MinePageLine)
                TimeSettingRow(
                    title = "晚报",
                    subtitle = "一天结束时回顾完成情况",
                    hour = uiState.eveningHour,
                    minute = uiState.eveningMinute,
                    onTimeChange = viewModel::setEveningTime
                )
            }
        }

        item {
            Button(
                onClick = viewModel::triggerNow,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MinePagePrimary)
            ) {
                Text("立即生成一次早晚报", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun TimeSettingRow(
    title: String,
    subtitle: String,
    hour: Int,
    minute: Int,
    onTimeChange: (Int, Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MinePageInk, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = MinePageSub, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
        }
        TimeStepper(value = hour, range = 0..23, onChange = { onTimeChange(it, minute) })
        Text(":", color = MinePageMuted, fontSize = 16.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 4.dp))
        TimeStepper(value = minute, range = 0..59, onChange = { onTimeChange(hour, it) })
    }
}

@Composable
private fun TimeStepper(value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MinePagePrimary.copy(alpha = 0.08f)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "-",
            modifier = Modifier
                .clickable { onChange(if (value == range.first) range.last else value - 1) }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            color = MinePagePrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black
        )
        Text(value.toString().padStart(2, '0'), color = MinePageDeep, fontSize = 12.sp, fontWeight = FontWeight.Black)
        Text(
            "+",
            modifier = Modifier
                .clickable { onChange(if (value == range.last) range.first else value + 1) }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            color = MinePagePrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black
        )
    }
}

data class ReminderStrategyUiState(
    val strategies: List<NotificationStrategy> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class ReminderStrategyViewModel @Inject constructor(
    private val repository: NotificationStrategyRepository,
    private val notificationHelper: NotificationHelper
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReminderStrategyUiState())
    val uiState: StateFlow<ReminderStrategyUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensurePresetStrategies()
            repository.getAllStrategies().collect { strategies ->
                _uiState.value = ReminderStrategyUiState(
                    strategies = strategies.sortedBy { it.createdAt },
                    isLoading = false
                )
            }
        }
    }

    fun deleteStrategy(strategyId: String) {
        viewModelScope.launch {
            repository.deleteStrategy(strategyId)
        }
    }

    fun previewStrategy(strategy: NotificationStrategy) {
        notificationHelper.previewStrategy(strategy)
    }

    fun stopPreview() {
        notificationHelper.stopStrategyPreview()
    }
}

@Composable
fun ReminderStrategyScreen(
    onBackPressed: () -> Unit,
    onCreateStrategy: () -> Unit,
    onEditStrategy: (String) -> Unit,
    viewModel: ReminderStrategyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var previewingStrategy by remember { mutableStateOf<NotificationStrategy?>(null) }

    DisposableEffect(Unit) {
        onDispose { viewModel.stopPreview() }
    }
    Column(Modifier.fillMaxSize().background(Color(0xFFF7F8FC)).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().height(60.dp).background(Color.White).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF5F7FC)).clickable(onClick = onBackPressed),
                contentAlignment = Alignment.Center
            ) {
                Image(painterResource(R.drawable.icon_detail_back), "返回", Modifier.size(36.dp))
            }
            Text("提醒策略", Modifier.weight(1f), Color(0xFF0E131D), 18.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = onCreateStrategy, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Add, "新建提醒策略", tint = Color(0xFF1A7DFA), modifier = Modifier.size(22.dp))
            }
        }

        previewingStrategy?.let { strategy ->
            DockedStrategyPreview(
                strategy = strategy,
                onClose = {
                    viewModel.stopPreview()
                    previewingStrategy = null
                }
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 14.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("策略列表", Modifier.weight(1f), Color(0xFF0F1726), 16.sp, fontWeight = FontWeight.Bold)
                    Text(if (uiState.isLoading) "加载中" else "${uiState.strategies.size} 个", color = Color(0xFF91A1B5), fontSize = 12.sp)
                }
                Spacer(Modifier.height(8.dp))
            }
            if (uiState.strategies.isEmpty() && !uiState.isLoading) {
                item {
                    Box(
                        Modifier.fillMaxWidth().border(1.dp, Color(0xFFD6E0ED), RoundedCornerShape(8.dp)).background(Color.White, RoundedCornerShape(8.dp)).padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("还没有提醒策略", color = Color(0xFF61738F), fontSize = 14.sp)
                    }
                }
            } else {
                item {
                    Column(
                        Modifier.fillMaxWidth().border(1.dp, Color(0xFFD6E0ED), RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp)).background(Color.White)
                    ) {
                        uiState.strategies.forEachIndexed { index, strategy ->
                            ReminderStrategyRow(
                                strategy = strategy,
                                onEdit = { onEditStrategy(strategy.id) },
                                isPreviewing = previewingStrategy?.id == strategy.id,
                                onPreview = {
                                    if (previewingStrategy?.id == strategy.id) {
                                        viewModel.stopPreview()
                                        previewingStrategy = null
                                    } else {
                                        viewModel.stopPreview()
                                        previewingStrategy = strategy
                                        viewModel.previewStrategy(strategy)
                                    }
                                }
                            )
                            if (index < uiState.strategies.lastIndex) {
                                HorizontalDivider(Modifier.padding(start = 58.dp), color = Color(0xFFE8EDF5))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DockedStrategyPreview(strategy: NotificationStrategy, onClose: () -> Unit) {
    val visual = reminderStrategyVisual(strategy.name)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)
            .border(1.dp, visual.color.copy(alpha = 0.32f), RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp)).background(Color.White).padding(start = 10.dp, top = 9.dp, bottom = 9.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(visual.color.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(painterResource(visual.iconRes), null, tint = visual.color, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("${strategy.name} · 正在预览", color = Color(0xFF0F1726), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "${strategy.vibrationSetting.displayName} · ${strategy.soundSetting.displayName} · ${strategy.systemNotificationMode.displayName}",
                color = Color(0xFF61738F), fontSize = 11.sp, maxLines = 1
            )
        }
        IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Close, "关闭预览", tint = Color(0xFF61738F), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ReminderStrategyRow(
    strategy: NotificationStrategy,
    onEdit: () -> Unit,
    isPreviewing: Boolean,
    onPreview: () -> Unit
) {
    val visual = reminderStrategyVisual(strategy.name)
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onEdit).padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(visual.color.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(painterResource(visual.iconRes), null, tint = visual.color, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(strategy.name, Modifier.weight(1f), Color(0xFF0F1726), 14.sp, fontWeight = FontWeight.SemiBold)
                if (strategy.soundSetting != SoundSetting.NONE) {
                    Text("音量 ${strategy.volume}%", color = visual.color, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
            Text(
                "${strategy.vibrationSetting.displayName} · ${strategy.soundSetting.displayName} · ${strategy.systemNotificationMode.displayName}",
                color = Color(0xFF61738F), fontSize = 11.sp
            )
            Text(
                if (strategy.advanceReminderMinutes.isEmpty()) "按时提醒" else "提前 ${strategy.advanceReminderMinutes.joinToString("、")} 分钟",
                color = Color(0xFF91A1B5), fontSize = 11.sp
            )
        }
        Box(
            Modifier.size(width = 56.dp, height = 32.dp).clip(RoundedCornerShape(8.dp))
                .background(visual.color.copy(alpha = if (isPreviewing) 0.18f else 0.10f)).clickable(onClick = onPreview),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (isPreviewing) "预览中" else "预览",
                color = visual.color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Icon(painterResource(R.drawable.icon_detail_chevron), null, tint = Color(0xFF91A1B5), modifier = Modifier.size(16.dp))
    }
}

private data class ReminderStrategyVisual(val iconRes: Int, val color: Color)

private fun reminderStrategyVisual(name: String): ReminderStrategyVisual = when {
    name.contains("重要") -> ReminderStrategyVisual(R.drawable.icon_notification_important, Color(0xFFF2383D))
    name.contains("无声") || name.contains("静音") -> ReminderStrategyVisual(R.drawable.icon_notification_silent, Color(0xFF0EA5A8))
    name.contains("标准") || name.contains("默认") -> ReminderStrategyVisual(R.drawable.icon_notification_standard, Color(0xFF1A7DFA))
    else -> ReminderStrategyVisual(R.drawable.icon_notification_custom, Color(0xFFF59E0B))
}

data class ExportDataUiState(
    val selectedRange: ExportRange = ExportRange.LAST_30_DAYS,
    val selectedFormat: ExportFormat = ExportFormat.EXCEL,
    val isExporting: Boolean = false,
    val actionMessage: String? = null,
    val exportResultUri: Uri? = null,
    val exportResultMimeType: String? = null
)

enum class ExportRange(val label: String) {
    LAST_7_DAYS("近 7 天"),
    LAST_30_DAYS("近 30 天"),
    THIS_YEAR("今年"),
    ALL("全部")
}

@HiltViewModel
class ExportDataViewModel @Inject constructor(
    private val taskExporter: TaskExporter
) : ViewModel() {
    private val _uiState = MutableStateFlow(ExportDataUiState())
    val uiState: StateFlow<ExportDataUiState> = _uiState.asStateFlow()

    fun selectRange(range: ExportRange) {
        _uiState.update { it.copy(selectedRange = range, actionMessage = null) }
    }

    fun selectFormat(format: ExportFormat) {
        _uiState.update { it.copy(selectedFormat = format, actionMessage = null) }
    }

    fun export() {
        viewModelScope.launch {
            val state = _uiState.value
            val now = LocalDateTime.now()
            val start = when (state.selectedRange) {
                ExportRange.LAST_7_DAYS -> now.minusDays(7)
                ExportRange.LAST_30_DAYS -> now.minusDays(30)
                ExportRange.THIS_YEAR -> now.withDayOfYear(1).withHour(0).withMinute(0).withSecond(0)
                ExportRange.ALL -> LocalDateTime.of(2000, 1, 1, 0, 0)
            }

            _uiState.update { it.copy(isExporting = true, actionMessage = null, exportResultUri = null) }
            try {
                taskExporter.export(start, now, state.selectedFormat).fold(
                    onSuccess = { uri ->
                        _uiState.update {
                            it.copy(
                                isExporting = false,
                                exportResultUri = uri,
                                exportResultMimeType = state.selectedFormat.mimeType,
                                actionMessage = "导出成功"
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(isExporting = false, actionMessage = error.message ?: "导出失败")
                        }
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "导出失败")
                _uiState.update { it.copy(isExporting = false, actionMessage = "导出失败，请重试") }
            }
        }
    }

    fun clearExportResult() {
        _uiState.update { it.copy(exportResultUri = null, exportResultMimeType = null) }
    }
}

@Composable
fun ExportDataScreen(
    onBackPressed: () -> Unit,
    viewModel: ExportDataViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.exportResultUri) {
        uiState.exportResultUri?.let { uri ->
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = uiState.exportResultMimeType ?: "*/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "导出任务数据"))
            viewModel.clearExportResult()
        }
    }

    MineSubPageScaffold(title = "导出数据", onBackPressed = onBackPressed) {
        item {
            MineSubHero(
                title = "任务数据导出",
                subtitle = "选择时间范围和文件格式，将任务数据导出为可分享文件。",
                tag = uiState.selectedFormat.displayName,
                color = Color(0xFF42A5F5)
            )
        }

        item {
            MineSectionTitle("时间范围")
            MineCard {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SegmentedOption("近 7 天", uiState.selectedRange == ExportRange.LAST_7_DAYS, Modifier.weight(1f)) {
                            viewModel.selectRange(ExportRange.LAST_7_DAYS)
                        }
                        SegmentedOption("近 30 天", uiState.selectedRange == ExportRange.LAST_30_DAYS, Modifier.weight(1f)) {
                            viewModel.selectRange(ExportRange.LAST_30_DAYS)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SegmentedOption("今年", uiState.selectedRange == ExportRange.THIS_YEAR, Modifier.weight(1f)) {
                            viewModel.selectRange(ExportRange.THIS_YEAR)
                        }
                        SegmentedOption("全部", uiState.selectedRange == ExportRange.ALL, Modifier.weight(1f)) {
                            viewModel.selectRange(ExportRange.ALL)
                        }
                    }
                }
            }
        }

        item {
            MineSectionTitle("导出格式")
            MineCard {
                ExportFormat.entries.forEachIndexed { index, format ->
                    ExportFormatRow(
                        format = format,
                        selected = uiState.selectedFormat == format,
                        onClick = { viewModel.selectFormat(format) }
                    )
                    if (index != ExportFormat.entries.lastIndex) HorizontalDivider(color = MinePageLine)
                }
            }
        }

        item {
            uiState.actionMessage?.let {
                Text(
                    text = it,
                    color = if (it.contains("成功")) MinePageSuccess else MinePageDanger,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            Button(
                onClick = viewModel::export,
                enabled = !uiState.isExporting,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MinePagePrimary)
            ) {
                Text(if (uiState.isExporting) "导出中..." else "导出并分享", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun ExportFormatRow(
    format: ExportFormat,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(format.displayName, color = MinePageInk, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Text(format.extension.uppercase(), color = MinePageSub, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
        }
        Text(
            text = if (selected) "已选择" else "选择",
            color = if (selected) MinePagePrimary else MinePageMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black
        )
    }
}
