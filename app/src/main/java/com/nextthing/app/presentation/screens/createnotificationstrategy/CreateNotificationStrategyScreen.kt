package com.nextthing.app.presentation.screens.createnotificationstrategy

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nextthing.app.domain.model.VibrationSetting
import com.nextthing.app.domain.model.SoundSetting
import com.nextthing.app.domain.model.SystemNotificationMode
import com.nextthing.app.presentation.theme.*
import com.nextthing.app.util.AudioFileInfo
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateNotificationStrategyScreen(
    onBackPressed: () -> Unit,
    strategyId: String? = null,
    viewModel: CreateNotificationStrategyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 从设置返回时刷新权限状态
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.checkPermissionStatus()
    }

    // 编辑模式：加载现有策略
    LaunchedEffect(strategyId) {
        strategyId?.let {
            viewModel.loadStrategy(it)
        }
    }

    // 如果保存成功，自动返回
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onBackPressed()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
    ) {
        // 顶部导航区
        TopNavigationSection(
            onBackPressed = onBackPressed,
            isEditMode = uiState.isEditMode
        )

        // 主要内容区域（可滚动）
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // 权限警告卡片
            if (!uiState.hasNotificationPermission || !uiState.hasExactAlarmPermission) {
                PermissionWarningCard(
                    hasNotificationPermission = uiState.hasNotificationPermission,
                    hasExactAlarmPermission = uiState.hasExactAlarmPermission,
                    onOpenNotificationSettings = {
                        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                        } else {
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = android.net.Uri.fromParts("package", context.packageName, null)
                            }
                        }
                        context.startActivity(intent)
                    },
                    onOpenAlarmSettings = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = android.net.Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 策略名称输入
            StrategyNameSection(
                name = uiState.name,
                onNameChange = { viewModel.updateName(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 震动设置
            VibrationSection(
                selectedVibration = uiState.vibrationSetting,
                onVibrationSelected = { viewModel.updateVibrationSetting(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 声音设置
            SoundSection(
                selectedSound = uiState.soundSetting,
                volume = uiState.volume,
                customAudioFileInfo = uiState.customAudioFileInfo,
                customAudioName = uiState.customAudioName,
                onSoundSelected = { viewModel.updateSoundSetting(it) },
                onVolumeChanged = { viewModel.updateVolume(it) },
                onPlayPreview = { viewModel.playSoundPreview() },
                onCustomAudioSelected = { audioFileInfo, customName ->
                    viewModel.updateCustomAudioFile(audioFileInfo, customName)
                },
                onClearCustomAudio = { viewModel.clearCustomAudio() }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 通知模式设置
            NotificationModeSection(
                selectedMode = uiState.systemNotificationMode,
                onModeSelected = { viewModel.updateSystemNotificationMode(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 提前提醒设置
            AdvanceReminderSection(
                selectedMinutes = uiState.advanceReminderMinutes,
                onToggleMinutes = { viewModel.toggleAdvanceReminder(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // 底部操作按钮（固定在底部）
        BottomActionSection(
            isValid = uiState.isValid,
            onCancel = onBackPressed,
            onSave = { viewModel.saveStrategy() },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun TopNavigationSection(
    onBackPressed: () -> Unit,
    isEditMode: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Primary)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackPressed,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.White
                )
            }

            Text(
                text = if (isEditMode) "编辑通知策略" else "新建通知策略",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
private fun StrategyNameSection(
    name: String,
    onNameChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BgSecondary),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = "策略名称",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            BasicTextField(
                value = name,
                onValueChange = onNameChange,
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    color = TextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        BgCard,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (name.isEmpty()) {
                            Text(
                                text = "请输入策略名称",
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

@Composable
private fun VibrationSection(
    selectedVibration: VibrationSetting,
    onVibrationSelected: (VibrationSetting) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BgSecondary),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = "震动设置",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            VibrationSetting.values().forEach { vibration ->
                val isSelected = vibration == selectedVibration
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Primary.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable { onVibrationSelected(vibration) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = vibration.icon,
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = vibration.displayName,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) PrimaryDark else TextPrimary
                        )
                        Text(
                            text = vibration.description,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    if (isSelected) {
                        Text(text = "\u2713", fontSize = 18.sp, color = PrimaryDark)
                    }
                }
            }
        }
    }
}

@Composable
private fun SoundSection(
    selectedSound: SoundSetting,
    volume: Int,
    customAudioFileInfo: AudioFileInfo?,
    customAudioName: String,
    onSoundSelected: (SoundSetting) -> Unit,
    onVolumeChanged: (Int) -> Unit,
    onPlayPreview: () -> Unit,
    onCustomAudioSelected: (AudioFileInfo, String) -> Unit,
    onClearCustomAudio: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BgSecondary),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = "声音设置",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 声音选项（过滤掉预置音效和录音文件）
            SoundSetting.values()
                .filter { it != SoundSetting.PRESET_AUDIO && it != SoundSetting.RECORDING_AUDIO }
                .forEach { sound ->
                    val isSelected = sound == selectedSound
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Primary.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable { onSoundSelected(sound) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = sound.icon,
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = sound.displayName,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) PrimaryDark else TextPrimary
                            )
                            Text(
                                text = sound.description,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        if (isSelected) {
                            Text(text = "\u2713", fontSize = 18.sp, color = PrimaryDark)
                        }
                    }
                }
        }
    }

    // 自定义音频选择（独立子卡片）
    if (selectedSound == SoundSetting.CUSTOM_AUDIO) {
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BgSecondary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                CustomAudioSelector(
                    customAudioFileInfo = customAudioFileInfo,
                    customAudioName = customAudioName,
                    onCustomAudioSelected = onCustomAudioSelected,
                    onClearCustomAudio = onClearCustomAudio
                )
            }
        }
    }

    // 音量与试听（独立子卡片，非静音时显示）
    if (selectedSound != SoundSetting.NONE) {
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BgSecondary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "音量: ${volume}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )

                Slider(
                    value = volume.toFloat(),
                    onValueChange = { onVolumeChanged(it.toInt()) },
                    valueRange = 0f..100f,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onPlayPreview,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("试听")
                }
            }
        }
    }
}

@Composable
private fun NotificationModeSection(
    selectedMode: SystemNotificationMode,
    onModeSelected: (SystemNotificationMode) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BgSecondary),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = "通知方式",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            SystemNotificationMode.entries.forEach { mode ->
                val isSelected = selectedMode == mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Primary.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable { onModeSelected(mode) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = mode.icon,
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mode.displayName,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) PrimaryDark else TextPrimary
                        )
                        Text(
                            text = mode.description,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    if (isSelected) {
                        Text(text = "\u2713", fontSize = 18.sp, color = PrimaryDark)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdvanceReminderSection(
    selectedMinutes: List<Int>,
    onToggleMinutes: (Int) -> Unit
) {
    data class ReminderOption(val minutes: Int, val label: String)

    val options = listOf(
        ReminderOption(5, "5分钟"),
        ReminderOption(15, "15分钟"),
        ReminderOption(30, "30分钟"),
        ReminderOption(60, "1小时"),
        ReminderOption(120, "2小时"),
        ReminderOption(1440, "1天")
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BgSecondary),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = "提前提醒",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "在截止时间之前发送预提醒（可多选）",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            val chunked = options.chunked(3)
            chunked.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { option ->
                        val isSelected = selectedMinutes.contains(option.minutes)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Primary else BgCard)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Primary else Border,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onToggleMinutes(option.minutes) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = option.label,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else TextSecondary
                            )
                        }
                    }
                    // 填充剩余空间
                    repeat(3 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun BottomActionSection(
    isValid: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.weight(1f),
            border = BorderStroke(1.dp, Primary),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "取消",
                color = Primary,
                fontSize = 16.sp
            )
        }

        Button(
            onClick = onSave,
            enabled = isValid,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = Primary,
                disabledContainerColor = Border
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "保存",
                color = Color.White,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun PermissionWarningCard(
    hasNotificationPermission: Boolean,
    hasExactAlarmPermission: Boolean,
    onOpenNotificationSettings: () -> Unit,
    onOpenAlarmSettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Warning.copy(alpha = 0.1f)),
        border = BorderStroke(1.dp, Warning.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = "权限提示",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Warning
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (!hasNotificationPermission) {
                Text(
                    text = "未开启通知权限，通知将无法正常推送",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedButton(
                    onClick = onOpenNotificationSettings,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Warning),
                    border = BorderStroke(1.dp, Warning)
                ) {
                    Text("前往开启通知权限", fontSize = 13.sp)
                }
            }

            if (!hasExactAlarmPermission) {
                if (!hasNotificationPermission) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(
                    text = "未开启精确闹钟权限，定时提醒可能不准确",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedButton(
                    onClick = onOpenAlarmSettings,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Warning),
                    border = BorderStroke(1.dp, Warning)
                ) {
                    Text("前往开启精确闹钟权限", fontSize = 13.sp)
                }
            }
        }
    }
}
