package com.example.nextthingb1.presentation.screens.createnotificationstrategy

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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.nextthingb1.domain.model.VibrationSetting
import com.example.nextthingb1.domain.model.SoundSetting
import com.example.nextthingb1.domain.model.SoundType
import com.example.nextthingb1.domain.model.SystemNotificationMode
import com.example.nextthingb1.domain.model.PresetAudio
import com.example.nextthingb1.util.AudioFileHelper
import com.example.nextthingb1.util.AudioFileInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateNotificationStrategyScreen(
    onBackPressed: () -> Unit,
    strategyId: String? = null,
    viewModel: CreateNotificationStrategyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp

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
            .background(Color.White)
    ) {
        // 顶部导航区
        TopNavigationSection(
            screenHeight = screenHeight,
            screenWidth = screenWidth,
            onBackPressed = onBackPressed,
            isEditMode = uiState.isEditMode
        )

        // 主要内容区域
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 策略名称输入
            StrategyNameSection(
                name = uiState.name,
                onNameChange = { viewModel.updateName(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 震动设置
            VibrationSection(
                selectedVibration = uiState.vibrationSetting,
                onVibrationSelected = { viewModel.updateVibrationSetting(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))

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

            Spacer(modifier = Modifier.height(24.dp))

            // 通知模式设置
            NotificationModeSection(
                selectedMode = uiState.systemNotificationMode,
                onModeSelected = { viewModel.updateSystemNotificationMode(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 提前提醒设置
            AdvanceReminderSection(
                selectedMinutes = uiState.advanceReminderMinutes,
                onToggleMinutes = { viewModel.toggleAdvanceReminder(it) }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 底部操作按钮
            BottomActionSection(
                isValid = uiState.isValid,
                onCancel = onBackPressed,
                onSave = { viewModel.saveStrategy() }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TopNavigationSection(
    screenHeight: androidx.compose.ui.unit.Dp,
    screenWidth: androidx.compose.ui.unit.Dp,
    onBackPressed: () -> Unit,
    isEditMode: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color(0xFF71CBF4))
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 返回按钮
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

            // 页面标题
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.5.dp, Color(0xFFE0E0E0)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "策略名称",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF424242),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            BasicTextField(
                value = name,
                onValueChange = onNameChange,
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    color = Color(0xFF424242)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color(0xFFF5F5F5),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(12.dp),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (name.isEmpty()) {
                            Text(
                                text = "请输入策略名称",
                                color = Color(0xFF9E9E9E),
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.5.dp, Color(0xFFE0E0E0)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "震动设置",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF424242),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            VibrationSetting.values().forEach { vibration ->
                VibrationOptionItem(
                    vibration = vibration,
                    isSelected = vibration == selectedVibration,
                    onClick = { onVibrationSelected(vibration) }
                )
            }
        }
    }
}

@Composable
private fun VibrationOptionItem(
    vibration: VibrationSetting,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = vibration.icon,
            fontSize = 16.sp,
            modifier = Modifier.padding(end = 8.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = vibration.displayName,
                fontSize = 14.sp,
                color = Color(0xFF424242)
            )
            Text(
                text = vibration.description,
                fontSize = 12.sp,
                color = Color(0xFF9E9E9E)
            )
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.5.dp, Color(0xFFE0E0E0)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "声音设置",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF424242),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 声音选项（过滤掉预置音效和录音文件）
            SoundSetting.values()
                .filter { it != SoundSetting.PRESET_AUDIO && it != SoundSetting.RECORDING_AUDIO }
                .forEach { sound ->
                    SoundOptionItem(
                        sound = sound,
                        isSelected = sound == selectedSound,
                        onClick = { onSoundSelected(sound) }
                    )
                }

            // 自定义音频选择
            if (selectedSound == SoundSetting.CUSTOM_AUDIO) {
                Spacer(modifier = Modifier.height(12.dp))
                CustomAudioSelector(
                    customAudioFileInfo = customAudioFileInfo,
                    customAudioName = customAudioName,
                    onCustomAudioSelected = onCustomAudioSelected,
                    onClearCustomAudio = onClearCustomAudio
                )
            }

            if (selectedSound != SoundSetting.NONE) {
                Spacer(modifier = Modifier.height(16.dp))

                // 音量调节
                Text(
                    text = "音量: ${volume}%",
                    fontSize = 14.sp,
                    color = Color(0xFF424242),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Slider(
                    value = volume.toFloat(),
                    onValueChange = { onVolumeChanged(it.toInt()) },
                    valueRange = 0f..100f,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 试听按钮
                Button(
                    onClick = onPlayPreview,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF71CBF4)
                    )
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
private fun SoundOptionItem(
    sound: SoundSetting,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = sound.icon,
            fontSize = 16.sp,
            modifier = Modifier.padding(end = 8.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = sound.displayName,
                fontSize = 14.sp,
                color = Color(0xFF424242)
            )
            Text(
                text = sound.description,
                fontSize = 12.sp,
                color = Color(0xFF9E9E9E)
            )
        }
    }
}

@Composable
private fun BottomActionSection(
    isValid: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 取消按钮
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.weight(1f),
            border = BorderStroke(1.dp, Color(0xFF71CBF4)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "取消",
                color = Color(0xFF71CBF4),
                fontSize = 16.sp
            )
        }

        // 保存按钮
        Button(
            onClick = onSave,
            enabled = isValid,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF71CBF4),
                disabledContainerColor = Color(0xFFE0E0E0)
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
private fun NotificationModeSection(
    selectedMode: SystemNotificationMode,
    onModeSelected: (SystemNotificationMode) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "通知方式",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF424242)
            )

            Spacer(modifier = Modifier.height(12.dp))

            SystemNotificationMode.entries.forEach { mode ->
                val isSelected = selectedMode == mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0xFFE3F2FD) else Color.Transparent)
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
                            color = if (isSelected) Color(0xFF1976D2) else Color(0xFF424242)
                        )
                        Text(
                            text = mode.description,
                            fontSize = 12.sp,
                            color = Color(0xFF9E9E9E)
                        )
                    }
                    if (isSelected) {
                        Text(text = "\u2713", fontSize = 18.sp, color = Color(0xFF1976D2))
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "提前提醒",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF424242)
            )
            Text(
                text = "在截止时间之前发送预提醒（可多选）",
                fontSize = 12.sp,
                color = Color(0xFF9E9E9E)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 使用 FlowRow 布局显示选项标签
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
                                .background(if (isSelected) Color(0xFF71CBF4) else Color.White)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color(0xFF71CBF4) else Color(0xFFE0E0E0),
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
                                color = if (isSelected) Color.White else Color(0xFF616161)
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