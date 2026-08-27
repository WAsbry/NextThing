package com.nextthing.app.presentation.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.nextthing.app.data.export.ExportFormat
import com.nextthing.app.R
import com.nextthing.app.data.export.TaskExporter
import com.nextthing.app.data.preferences.BriefingPreferences
import com.nextthing.app.domain.service.AIBriefingGenerator.BriefingType
import com.nextthing.app.domain.model.NotificationStrategy
import com.nextthing.app.domain.model.SoundSetting
import com.nextthing.app.domain.repository.NotificationStrategyRepository
import com.nextthing.app.util.NotificationHelper
import com.nextthing.app.work.TaskWorkScheduler
import com.nextthing.app.presentation.theme.BgCard
import com.nextthing.app.presentation.theme.BgPrimary
import com.nextthing.app.presentation.theme.Border
import com.nextthing.app.presentation.theme.Primary
import com.nextthing.app.presentation.theme.TextMuted
import com.nextthing.app.presentation.theme.TextPrimary
import com.nextthing.app.presentation.theme.TextSecondary
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun MineSubPageScaffold(
    title: String,
    onBackPressed: () -> Unit,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    Scaffold(
        containerColor = BgPrimary,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgCard)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 28.dp)
        ) {
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
    Text(
        text = subtitle,
        color = TextSecondary,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        modifier = Modifier.padding(bottom = 4.dp)
    )
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
            .clip(RoundedCornerShape(8.dp))
            .background(BgCard)
            .border(1.dp, Border, RoundedCornerShape(8.dp))
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
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Primary.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Primary else TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black
        )
    }
}

enum class BriefingGenerationState { IDLE, RUNNING, SUCCESS, ERROR }

data class BriefingSettingsUiState(
    val enabled: Boolean = false,
    val morningHour: Int = 8,
    val morningMinute: Int = 0,
    val eveningHour: Int = 21,
    val eveningMinute: Int = 0,
    val isLoading: Boolean = true,
    val generationState: BriefingGenerationState = BriefingGenerationState.IDLE,
    val generationType: BriefingType? = null,
    val message: String? = null,
    val needsNotificationSettings: Boolean = false
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
                BriefingSettingsUiState(
                    enabled = enabled,
                    morningHour = morningHour,
                    morningMinute = morningMinute,
                    eveningHour = eveningHour,
                    eveningMinute = eveningMinute,
                    isLoading = false
                )
            }.collect { latest ->
                _uiState.update { current ->
                    latest.copy(
                        generationState = current.generationState,
                        generationType = current.generationType,
                        message = current.message,
                        needsNotificationSettings = current.needsNotificationSettings
                    )
                }
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled && !NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                _uiState.update {
                    it.copy(
                        message = "请先开启通知权限",
                        needsNotificationSettings = true
                    )
                }
                return@launch
            }
            runCatching {
                briefingPreferences.setEnabled(enabled)
                applySchedule(_uiState.value.copy(enabled = enabled))
            }.onSuccess {
                _uiState.update { it.copy(message = if (enabled) "早晚报已开启" else "早晚报已关闭") }
            }.onFailure { error ->
                Timber.e(error, "更新早晚报开关失败")
                _uiState.update { it.copy(message = "设置保存失败，请重试") }
            }
        }
    }

    fun setMorningTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            runCatching {
                briefingPreferences.setMorningTime(hour, minute)
                applySchedule(_uiState.value.copy(morningHour = hour, morningMinute = minute))
            }.onSuccess {
                _uiState.update { it.copy(message = "早报时间已更新") }
            }.onFailure { error ->
                Timber.e(error, "更新早报时间失败")
                _uiState.update { it.copy(message = "时间保存失败，请重试") }
            }
        }
    }

    fun setEveningTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            runCatching {
                briefingPreferences.setEveningTime(hour, minute)
                applySchedule(_uiState.value.copy(eveningHour = hour, eveningMinute = minute))
            }.onSuccess {
                _uiState.update { it.copy(message = "晚报时间已更新") }
            }.onFailure { error ->
                Timber.e(error, "更新晚报时间失败")
                _uiState.update { it.copy(message = "时间保存失败，请重试") }
            }
        }
    }

    fun triggerNow(type: BriefingType) {
        val state = _uiState.value
        if (!state.enabled) {
            _uiState.update { it.copy(message = "请先开启早晚报") }
            return
        }
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            _uiState.update {
                it.copy(message = "请先开启通知权限", needsNotificationSettings = true)
            }
            return
        }
        if (state.generationState == BriefingGenerationState.RUNNING) return

        val workId = TaskWorkScheduler.triggerImmediateBriefing(context, type)
        _uiState.update {
            it.copy(
                generationState = BriefingGenerationState.RUNNING,
                generationType = type,
                message = null
            )
        }
        viewModelScope.launch {
            runCatching {
                val workManager = WorkManager.getInstance(context)
                var workInfo: WorkInfo
                do {
                    workInfo = withContext(Dispatchers.IO) {
                        workManager.getWorkInfoById(workId).get()
                    }
                    if (!workInfo.state.isFinished) delay(250)
                } while (!workInfo.state.isFinished)
                workInfo
            }.onSuccess { workInfo ->
                val succeeded = workInfo.state == WorkInfo.State.SUCCEEDED
                _uiState.update {
                    it.copy(
                        generationState = if (succeeded) BriefingGenerationState.SUCCESS else BriefingGenerationState.ERROR,
                        generationType = null,
                        message = if (succeeded) {
                            "${type.displayName()}已生成，请在通知中查看"
                        } else {
                            "${type.displayName()}生成失败，请重试"
                        }
                    )
                }
            }.onFailure { error ->
                Timber.e(error, "观察简报生成状态失败")
                _uiState.update {
                    it.copy(
                        generationState = BriefingGenerationState.ERROR,
                        generationType = null,
                        message = "简报生成失败，请重试"
                    )
                }
            }
        }
    }

    fun consumeMessage() {
        _uiState.update {
            it.copy(
                message = null,
                needsNotificationSettings = false,
                generationState = if (it.generationState == BriefingGenerationState.RUNNING) {
                    BriefingGenerationState.RUNNING
                } else {
                    BriefingGenerationState.IDLE
                }
            )
        }
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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun BriefingSettingsScreen(
    onBackPressed: () -> Unit,
    viewModel: BriefingSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var editingType by remember { mutableStateOf<BriefingType?>(null) }

    LaunchedEffect(uiState.message) {
        val message = uiState.message ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = if (uiState.needsNotificationSettings) "前往设置" else null
        )
        if (result == SnackbarResult.ActionPerformed && uiState.needsNotificationSettings) {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            )
        }
        viewModel.consumeMessage()
    }

    editingType?.let { type ->
        BriefingTimeDialog(
            title = if (type == BriefingType.MORNING) "设置早报时间" else "设置晚报时间",
            initialHour = if (type == BriefingType.MORNING) uiState.morningHour else uiState.eveningHour,
            initialMinute = if (type == BriefingType.MORNING) uiState.morningMinute else uiState.eveningMinute,
            onDismiss = { editingType = null },
            onConfirm = { hour, minute ->
                if (type == BriefingType.MORNING) {
                    viewModel.setMorningTime(hour, minute)
                } else {
                    viewModel.setEveningTime(hour, minute)
                }
                editingType = null
            }
        )
    }

    Scaffold(
        containerColor = BgPrimary,
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
        ) {
            BriefingTopBar(onBackPressed)
            if (uiState.isLoading) {
                BriefingLoadingState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    item {
                        BriefingCard {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("早晚报提醒", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        "每天定时生成任务规划与回顾",
                                        color = TextSecondary,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                Switch(
                                    checked = uiState.enabled,
                                    onCheckedChange = viewModel::setEnabled,
                                    colors = SwitchDefaults.colors(checkedTrackColor = Primary)
                                )
                            }
                        }
                    }

                    item { BriefingSectionTitle("推送安排", if (uiState.enabled) "正在生效" else "开启后生效") }
                    item {
                        BriefingCard {
                            BriefingTimeRow(
                                title = "早报",
                                subtitle = "规划今日重点",
                                time = formatBriefingTime(uiState.morningHour, uiState.morningMinute),
                                iconRes = R.drawable.icon_briefing_morning,
                                onClick = { editingType = BriefingType.MORNING }
                            )
                            HorizontalDivider(color = Border, modifier = Modifier.padding(start = 58.dp))
                            BriefingTimeRow(
                                title = "晚报",
                                subtitle = "回顾完成情况",
                                time = formatBriefingTime(uiState.eveningHour, uiState.eveningMinute),
                                iconRes = R.drawable.icon_briefing_evening,
                                onClick = { editingType = BriefingType.EVENING }
                            )
                        }
                    }

                    item { BriefingSectionTitle("测试简报") }
                    item {
                        BriefingCard {
                            Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    BriefingGenerateButton(
                                        text = "生成早报",
                                        loading = uiState.generationState == BriefingGenerationState.RUNNING && uiState.generationType == BriefingType.MORNING,
                                        enabled = uiState.enabled && uiState.generationState != BriefingGenerationState.RUNNING,
                                        modifier = Modifier.weight(1f),
                                        onClick = { viewModel.triggerNow(BriefingType.MORNING) }
                                    )
                                    BriefingGenerateButton(
                                        text = "生成晚报",
                                        loading = uiState.generationState == BriefingGenerationState.RUNNING && uiState.generationType == BriefingType.EVENING,
                                        enabled = uiState.enabled && uiState.generationState != BriefingGenerationState.RUNNING,
                                        modifier = Modifier.weight(1f),
                                        onClick = { viewModel.triggerNow(BriefingType.EVENING) }
                                    )
                                }
                                if (!uiState.enabled) {
                                    Text(
                                        "开启早晚报后可测试生成",
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BriefingTopBar(onBackPressed: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackPressed, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                "智能早晚报",
                modifier = Modifier.padding(start = 5.dp),
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        HorizontalDivider(color = Border)
    }
}

@Composable
private fun BriefingSectionTitle(title: String, trailing: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        trailing?.let { Text(it, color = TextMuted, fontSize = 12.sp) }
    }
}

@Composable
private fun BriefingCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(BgCard)
            .border(1.dp, Border, RoundedCornerShape(8.dp))
    ) { content() }
}

@Composable
private fun BriefingTimeRow(
    title: String,
    subtitle: String,
    time: String,
    iconRes: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .height(52.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
        }
        Text(time, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.size(8.dp))
        Icon(
            painter = painterResource(R.drawable.icon_detail_chevron),
            contentDescription = "修改${title}时间",
            tint = TextMuted,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun BriefingGenerateButton(
    text: String,
    loading: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (enabled) Primary else Border),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Primary)
            Spacer(Modifier.size(8.dp))
            Text("正在生成", fontWeight = FontWeight.SemiBold)
        } else {
            Text(text, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun BriefingTimeDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = BgCard,
            border = BorderStroke(1.dp, Border)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    title,
                    modifier = Modifier.fillMaxWidth(),
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                TimePicker(state = timePickerState)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Border)
                    ) { Text("取消", color = TextSecondary) }
                    Spacer(Modifier.size(8.dp))
                    Button(
                        onClick = { onConfirm(timePickerState.hour, timePickerState.minute) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) { Text("确定", color = Color.White) }
                }
            }
        }
    }
}

@Composable
private fun BriefingLoadingState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (index == 1) 145.dp else 78.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Border.copy(alpha = 0.5f))
            )
        }
    }
}

private fun formatBriefingTime(hour: Int, minute: Int): String =
    "%02d:%02d".format(hour, minute)

private fun BriefingType.displayName(): String = when (this) {
    BriefingType.MORNING -> "早报"
    BriefingType.EVENING -> "晚报"
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
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                if (uiState.isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                }
                Text(
                    if (uiState.isExporting) "导出中..." else "导出并分享",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
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
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = Primary,
                unselectedColor = TextMuted
            )
        )
    }
}
