package com.nextthing.app.presentation.screens.createnotificationstrategy

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.nextthing.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateNotificationStrategyScreen(
    onBackPressed: () -> Unit,
    strategyId: String? = null,
    viewModel: CreateNotificationStrategyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.checkPermissionStatus()
    }

    LaunchedEffect(strategyId) {
        strategyId?.let {
            viewModel.loadStrategy(it)
        }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onBackPressed()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8FC))
            .statusBarsPadding()
    ) {
        TopNavigationSection(
            onBackPressed = onBackPressed,
            isEditMode = uiState.isEditMode,
            isValid = uiState.isValid,
            onSave = viewModel::saveStrategy
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 10.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

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

            // 策略名称
            StrategyNameSection(
                name = uiState.name,
                onNameChange = { viewModel.updateName(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 震动 - 紧凑横排
            CompactSelectionSection(
                title = "震动",
                options = VibrationSetting.values().map { it.displayName.replace("震动", "") },
                selectedIndex = VibrationSetting.values().indexOf(uiState.vibrationSetting),
                onSelect = { viewModel.updateVibrationSetting(VibrationSetting.values()[it]) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 声音 - 紧凑横排
            val soundOptions = SoundSetting.values()
                .filter { it != SoundSetting.PRESET_AUDIO && it != SoundSetting.RECORDING_AUDIO }
            CompactSelectionSection(
                title = "声音",
                options = soundOptions.map { it.displayName },
                selectedIndex = soundOptions.indexOf(uiState.soundSetting).takeIf { it >= 0 } ?: 0,
                onSelect = { viewModel.updateSoundSetting(soundOptions[it]) }
            )

            // 自定义音频（选了自定义才显示）
            if (uiState.soundSetting == SoundSetting.CUSTOM_AUDIO) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFD6E0ED)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        CustomAudioSelector(
                            customAudioFileInfo = uiState.customAudioFileInfo,
                            customAudioName = uiState.customAudioName,
                            onCustomAudioSelected = { audioFileInfo, customName ->
                                viewModel.updateCustomAudioFile(audioFileInfo, customName)
                            },
                            onClearCustomAudio = { viewModel.clearCustomAudio() }
                        )
                    }
                }
            }

            // 音量条（非静音时显示）
            if (uiState.soundSetting != SoundSetting.NONE) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFD6E0ED)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("音量", fontSize = 13.sp, color = TextSecondary, modifier = Modifier.width(32.dp))
                        Slider(
                            value = uiState.volume.toFloat(),
                            onValueChange = { viewModel.updateVolume(it.toInt()) },
                            valueRange = 0f..100f,
                            modifier = Modifier.weight(1f)
                        )
                        Text("${uiState.volume}%", fontSize = 12.sp, color = TextMuted, modifier = Modifier.width(36.dp))
                        IconButton(
                            onClick = { viewModel.previewCurrentSound() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, null, tint = Primary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 通知方式 - 紧凑横排
            CompactSelectionSection(
                title = "通知方式",
                options = SystemNotificationMode.entries.map { it.displayName },
                selectedIndex = SystemNotificationMode.entries.indexOf(uiState.systemNotificationMode),
                onSelect = { viewModel.updateSystemNotificationMode(SystemNotificationMode.entries[it]) }
            )

            Spacer(modifier = Modifier.height(8.dp))
            StrategyPreviewRow(
                vibration = uiState.vibrationSetting.displayName,
                sound = uiState.soundSetting.displayName,
                notificationMode = uiState.systemNotificationMode.displayName,
                onPreview = viewModel::previewCurrentStrategy
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 提前提醒
            AdvanceReminderSection(
                selectedMinutes = uiState.advanceReminderMinutes,
                onToggleMinutes = { viewModel.toggleAdvanceReminder(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

    }
}

@Composable
private fun TopNavigationSection(
    onBackPressed: () -> Unit,
    isEditMode: Boolean = false,
    isValid: Boolean,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(60.dp).background(Color.White).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF5F7FC)).clickable(onClick = onBackPressed),
            contentAlignment = Alignment.Center
        ) {
            Image(painterResource(R.drawable.icon_detail_back), "返回", Modifier.size(36.dp))
        }
        Text(if (isEditMode) "编辑通知策略" else "新建通知策略", Modifier.weight(1f), Color(0xFF0E131D), 18.sp, fontWeight = FontWeight.Bold)
        Text(
            "保存",
            Modifier.clickable(enabled = isValid, onClick = onSave),
            if (isValid) Color(0xFF1A7DFA) else Color(0xFFB6C0CE),
            18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StrategyNameSection(
    name: String,
    onNameChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFD6E0ED)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Text(
                text = "策略名称",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            BasicTextField(
                value = name,
                onValueChange = onNameChange,
                textStyle = TextStyle(fontSize = 14.sp, color = TextPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFD6E0ED), RoundedCornerShape(8.dp))
                    .background(Color(0xFFF7F8FC), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (name.isEmpty()) {
                            Text("请输入策略名称", color = TextMuted, fontSize = 14.sp)
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

/**
 * 紧凑横排选择组件 - 选项排成一行，chip 风格
 */
@Composable
private fun CompactSelectionSection(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFD6E0ED)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                options.forEachIndexed { index, label ->
                    val isSelected = index == selectedIndex
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFF1A7DFA) else Color.White)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color(0xFF1A7DFA) else Color(0xFFD6E0ED),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelect(index) }
                            .height(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                            color = if (isSelected) Color.White else TextSecondary,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StrategyPreviewRow(
    vibration: String,
    sound: String,
    notificationMode: String,
    onPreview: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().border(1.dp, Color(0xFFD6E0ED), RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp)).background(Color.White).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Color(0x1A1A7DFA)), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.icon_action_preview), null, tint = Color(0xFF1A7DFA), modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("试用当前策略", color = Color(0xFF0F1726), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("$vibration · $sound · $notificationMode", color = Color(0xFF61738F), fontSize = 11.sp, maxLines = 1)
        }
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF1A7DFA)).clickable(onClick = onPreview),
            contentAlignment = Alignment.Center
        ) {
            Icon(painterResource(R.drawable.icon_action_preview), "试用当前策略", tint = Color.White, modifier = Modifier.size(18.dp))
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFD6E0ED)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Text(
                text = "提前提醒",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Text(
                text = "截止时间前预提醒（可多选）",
                fontSize = 11.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(8.dp))

            val chunked = options.chunked(3)
            chunked.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    row.forEach { option ->
                        val isSelected = selectedMinutes.contains(option.minutes)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF1A7DFA) else Color.White)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color(0xFF1A7DFA) else Color(0xFFD6E0ED),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onToggleMinutes(option.minutes) }
                                .height(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = option.label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                color = if (isSelected) Color.White else TextSecondary
                            )
                        }
                    }
                    repeat(3 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
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
            Text("取消", color = Primary, fontSize = 16.sp)
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
            Text("保存", color = Color.White, fontSize = 16.sp)
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
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text("权限提示", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Warning)

            Spacer(modifier = Modifier.height(8.dp))

            if (!hasNotificationPermission) {
                Text("未开启通知权限，通知将无法正常推送", fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 8.dp))
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
                Text("未开启精确闹钟权限，定时提醒可能不准确", fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 8.dp))
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
