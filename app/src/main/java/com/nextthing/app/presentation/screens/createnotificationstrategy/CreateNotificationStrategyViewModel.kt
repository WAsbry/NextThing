package com.nextthing.app.presentation.screens.createnotificationstrategy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextthing.app.domain.model.VibrationSetting
import com.nextthing.app.domain.model.SoundSetting
import com.nextthing.app.domain.model.SoundType
import com.nextthing.app.domain.model.SystemNotificationMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import com.nextthing.app.domain.model.PresetAudio
import com.nextthing.app.util.AudioFileInfo
import com.nextthing.app.util.NotificationHelper
import javax.inject.Inject

@HiltViewModel
class CreateNotificationStrategyViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationStrategyRepository: com.nextthing.app.domain.repository.NotificationStrategyRepository,
    private val notificationHelper: NotificationHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateNotificationStrategyUiState())
    val uiState: StateFlow<CreateNotificationStrategyUiState> = _uiState.asStateFlow()

    init {
        checkPermissionStatus()
    }

    /**
     * 检查通知相关权限状态
     */
    fun checkPermissionStatus() {
        val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val hasExactAlarmPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        val areNotificationsEnabled = (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .areNotificationsEnabled()

        _uiState.value = _uiState.value.copy(
            hasNotificationPermission = hasNotificationPermission && areNotificationsEnabled,
            hasExactAlarmPermission = hasExactAlarmPermission
        )
    }

    /**
     * 加载现有策略（编辑模式）
     */
    fun loadStrategy(strategyId: String) {
        viewModelScope.launch {
            try {
                val strategy = notificationStrategyRepository.getStrategyById(strategyId)
                if (strategy != null) {
                    // 构建 AudioFileInfo（如果有自定义音频路径）
                    val audioFileInfo = strategy.customAudioPath?.let { path ->
                        AudioFileInfo(
                            uri = Uri.parse(path),
                            fileName = strategy.customAudioName ?: "自定义音频",
                            displayName = strategy.customAudioName ?: "自定义音频",
                            duration = null,
                            fileSize = null
                        )
                    }

                    // 构建 PresetAudio（如果有预置音频）
                    val presetAudio = strategy.presetAudioName?.let { fileName ->
                        PresetAudio.findByFileName(fileName)
                    }

                    _uiState.value = _uiState.value.copy(
                        strategyId = strategy.id,
                        isEditMode = true,
                        name = strategy.name,
                        vibrationSetting = strategy.vibrationSetting,
                        soundSetting = strategy.soundSetting,
                        volume = strategy.volume,
                        customAudioFileInfo = audioFileInfo,
                        customAudioName = strategy.customAudioName ?: "",
                        selectedPresetAudio = presetAudio,
                        systemNotificationMode = strategy.systemNotificationMode,
                        advanceReminderMinutes = strategy.advanceReminderMinutes
                    )
                    Timber.d("策略加载成功: ${strategy.name}")
                } else {
                    Timber.w("未找到策略: $strategyId")
                }
            } catch (e: Exception) {
                Timber.e(e, "加载策略失败: $strategyId")
            }
        }
    }

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    fun updateVibrationSetting(vibrationSetting: VibrationSetting) {
        _uiState.value = _uiState.value.copy(vibrationSetting = vibrationSetting)
    }

    fun updateSoundSetting(soundSetting: SoundSetting) {
        _uiState.value = _uiState.value.copy(soundSetting = soundSetting)
    }

    fun updateVolume(volume: Int) {
        _uiState.value = _uiState.value.copy(volume = volume)
    }

    fun updateSystemNotificationMode(mode: SystemNotificationMode) {
        _uiState.value = _uiState.value.copy(systemNotificationMode = mode)
    }

    fun toggleAdvanceReminder(minutes: Int) {
        val current = _uiState.value.advanceReminderMinutes.toMutableList()
        if (current.contains(minutes)) {
            current.remove(minutes)
        } else {
            current.add(minutes)
            current.sort()
        }
        _uiState.value = _uiState.value.copy(advanceReminderMinutes = current)
    }

    fun updateCustomAudioFile(audioFileInfo: AudioFileInfo, customName: String) {
        _uiState.value = _uiState.value.copy(
            soundSetting = SoundSetting.CUSTOM_AUDIO, // 重要：同时更新sound setting
            customAudioFileInfo = audioFileInfo,
            customAudioName = customName
        )
        Timber.d("Updated custom audio file: $customName, soundSetting: ${SoundSetting.CUSTOM_AUDIO}")
    }

    fun updatePresetAudio(presetAudio: PresetAudio) {
        _uiState.value = _uiState.value.copy(
            soundSetting = SoundSetting.PRESET_AUDIO, // 重要：同时更新sound setting
            selectedPresetAudio = presetAudio
        )
        Timber.d("Updated preset audio: ${presetAudio.displayName}, soundSetting: ${SoundSetting.PRESET_AUDIO}")
    }

    fun clearCustomAudio() {
        _uiState.value = _uiState.value.copy(
            soundSetting = SoundSetting.NONE, // 重置为无声音
            customAudioFileInfo = null,
            customAudioName = ""
        )
        Timber.d("Cleared custom audio, soundSetting reset to NONE")
    }

    fun playSoundPreview() {
        val currentState = _uiState.value
        val currentSound = currentState.soundSetting
        val currentVolume = currentState.volume

        Timber.d("🎵 [Preview] 点击试听按钮: sound=${currentSound.displayName}, type=${currentSound.soundType}, volume=$currentVolume")

        viewModelScope.launch {
            try {
                if (currentSound.soundType == SoundType.NONE) {
                    Timber.d("🎵 [Preview] 声音为NONE，跳过")
                    return@launch
                }

                when (currentSound.soundType) {
                    SoundType.NONE -> return@launch

                    SoundType.NOTIFICATION,
                    SoundType.DEFAULT_NOTIFICATION,
                    SoundType.RINGTONE -> {
                        Timber.d("🎵 [Preview] 系统铃声分支，soundType=${currentSound.soundType}")
                        val soundUri = getSoundUri(currentSound.soundType)
                        Timber.d("🎵 [Preview] getSoundUri返回: $soundUri")
                        if (soundUri != null) {
                            playSoundWithUri(soundUri, currentVolume)
                        } else {
                            Timber.w("🎵 [Preview] soundUri为null，无法播放")
                        }
                    }

                    SoundType.PRESET_AUDIO -> {
                        Timber.d("🎵 [Preview] 预置音频分支")
                        currentState.selectedPresetAudio?.let { presetAudio ->
                            playPresetAudio(presetAudio, currentVolume)
                        } ?: Timber.w("🎵 [Preview] selectedPresetAudio为null")
                    }

                    SoundType.CUSTOM_AUDIO,
                    SoundType.RECORDING_AUDIO -> {
                        Timber.d("🎵 [Preview] 自定义音频分支, fileInfo=${currentState.customAudioFileInfo}")
                        currentState.customAudioFileInfo?.let { audioInfo ->
                            playCustomAudio(audioInfo.uri, currentVolume)
                        } ?: Timber.w("🎵 [Preview] customAudioFileInfo为null")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "🎵 [Preview] 播放失败")
            }
        }
    }

    fun previewCurrentStrategy() {
        notificationHelper.previewStrategy(currentPreviewStrategy())
    }

    fun previewCurrentSound() {
        notificationHelper.previewStrategySound(currentPreviewStrategy())
    }

    private fun currentPreviewStrategy(): com.nextthing.app.domain.model.NotificationStrategy {
        val state = _uiState.value
        return com.nextthing.app.domain.model.NotificationStrategy(
            name = state.name.ifBlank { "提醒试用" },
            vibrationSetting = state.vibrationSetting,
            soundSetting = state.soundSetting,
            volume = state.volume,
            customAudioPath = state.customAudioFileInfo?.uri?.toString(),
            customAudioName = state.customAudioName.takeIf { it.isNotBlank() },
            presetAudioName = state.selectedPresetAudio?.fileName,
            systemNotificationMode = state.systemNotificationMode,
            advanceReminderMinutes = state.advanceReminderMinutes
        )
    }

    private fun getSoundUri(soundType: SoundType): Uri? {
        val uri = when (soundType) {
            SoundType.NONE -> null
            SoundType.NOTIFICATION -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            SoundType.DEFAULT_NOTIFICATION -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            SoundType.RINGTONE -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            SoundType.PRESET_AUDIO -> null
            SoundType.CUSTOM_AUDIO -> null
            SoundType.RECORDING_AUDIO -> null
        }
        Timber.d("🎵 [Preview] getSoundUri($soundType) = $uri")
        return uri
    }

    private fun playSoundWithUri(uri: Uri, volume: Int) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val ringerMode = audioManager.ringerMode
            Timber.d("🎵 [Preview] 系统铃声模式: $ringerMode (0=静音, 1=震动, 2=正常)")
            if (ringerMode == AudioManager.RINGER_MODE_SILENT || ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
                _uiState.value = _uiState.value.copy() // 触发状态更新
                android.widget.Toast.makeText(context, "手机处于静音/震动模式，试听无声音", android.widget.Toast.LENGTH_SHORT).show()
                return
            }

            val ringtone = RingtoneManager.getRingtone(context, uri)
            Timber.d("🎵 [Preview] ringtone对象: ${ringtone != null}, title=${ringtone?.getTitle(context)}")
            if (ringtone != null) {
                // 不改系统音量，避免勿扰模式下 SecurityException
                ringtone.play()
                Timber.d("🎵 [Preview] ringtone.play() 已调用, isPlaying=${ringtone.isPlaying}")

                viewModelScope.launch {
                    kotlinx.coroutines.delay(2000)
                    if (ringtone.isPlaying) {
                        ringtone.stop()
                        Timber.d("🎵 [Preview] 2秒后自动停止")
                    }
                }
            } else {
                Timber.w("🎵 [Preview] RingtoneManager.getRingtone返回null")
            }
        } catch (e: Exception) {
            Timber.e(e, "🎵 [Preview] playSoundWithUri异常")
        }
    }

    private fun playPresetAudio(presetAudio: PresetAudio, volume: Int) {
        try {
            // 从assets中播放预置音频
            val assetPath = "audio/${presetAudio.fileName}"
            context.assets.openFd(assetPath).use { assetFileDescriptor ->
                val mediaPlayer = MediaPlayer()
                try {
                    mediaPlayer.setDataSource(
                        assetFileDescriptor.fileDescriptor,
                        assetFileDescriptor.startOffset,
                        assetFileDescriptor.length
                    )

                    // 设置音量
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)
                    val targetVolume = (maxVolume * volume / 100f).toInt()
                    audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, targetVolume, 0)

                    // 使用AudioAttributes替代已弃用的setAudioStreamType
                    val audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    mediaPlayer.setAudioAttributes(audioAttributes)

                    mediaPlayer.prepare()
                    mediaPlayer.start()

                    // 播放完成后释放资源
                    mediaPlayer.setOnCompletionListener { player ->
                        player.release()
                    }
                } catch (e: Exception) {
                    mediaPlayer.release()
                    throw e
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error playing preset audio: ${presetAudio.fileName}")
        }
    }

    private fun playCustomAudio(uri: Uri, volume: Int) {
        try {
            Timber.d("Starting playCustomAudio with URI: $uri, volume: $volume")

            // 检查URI是否可以访问
            try {
                context.contentResolver.openInputStream(uri)?.use {
                    Timber.d("URI is accessible, file size: ${it.available()} bytes")
                }
            } catch (e: Exception) {
                Timber.e(e, "URI is not accessible: $uri")
                return
            }

            val mediaPlayer = MediaPlayer()

            // 设置音量
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)
            val targetVolume = (maxVolume * volume / 100f).toInt()
            Timber.d("Setting volume: $targetVolume / $maxVolume")
            audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, targetVolume, 0)

            // 使用AudioAttributes替代已弃用的setAudioStreamType
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            mediaPlayer.setAudioAttributes(audioAttributes)

            mediaPlayer.setDataSource(context, uri)
            Timber.d("MediaPlayer dataSource set, preparing...")

            mediaPlayer.prepareAsync()

            mediaPlayer.setOnPreparedListener { player ->
                Timber.d("MediaPlayer prepared, starting playback...")
                player.start()
            }

            mediaPlayer.setOnErrorListener { player, what, extra ->
                Timber.e("MediaPlayer error: what=$what, extra=$extra")
                player.release()
                true
            }

            // 播放完成后释放资源
            mediaPlayer.setOnCompletionListener { player ->
                Timber.d("MediaPlayer playback completed")
                player.release()
            }

            // 播放完成后自动释放，不限制时长
        } catch (e: Exception) {
            Timber.e(e, "Error playing custom audio from URI: $uri")
        }
    }

    fun saveStrategy() {
        val currentState = _uiState.value
        if (currentState.name.isBlank()) {
            Timber.w("Cannot save strategy with empty name")
            return
        }

        viewModelScope.launch {
            try {
                if (currentState.isEditMode && currentState.strategyId != null) {
                    // 编辑模式：更新现有策略
                    val existingStrategy = notificationStrategyRepository.getStrategyById(currentState.strategyId)
                    if (existingStrategy != null) {
                        val updatedStrategy = existingStrategy.copy(
                            name = currentState.name,
                            vibrationSetting = currentState.vibrationSetting,
                            soundSetting = currentState.soundSetting,
                            volume = currentState.volume,
                            customAudioPath = currentState.customAudioFileInfo?.uri?.toString(),
                            customAudioName = currentState.customAudioName.takeIf { it.isNotBlank() },
                            presetAudioName = currentState.selectedPresetAudio?.fileName,
                            systemNotificationMode = currentState.systemNotificationMode,
                            advanceReminderMinutes = currentState.advanceReminderMinutes,
                            updatedAt = java.time.LocalDateTime.now()
                        )
                        notificationStrategyRepository.updateStrategy(updatedStrategy)
                        Timber.d("通知策略更新成功: ${currentState.name}")
                    }
                } else {
                    // 新建模式：插入新策略
                    val strategy = com.nextthing.app.domain.model.NotificationStrategy(
                        id = java.util.UUID.randomUUID().toString(),
                        name = currentState.name,
                        isGeofenceEnabled = false,
                        vibrationSetting = currentState.vibrationSetting,
                        soundSetting = currentState.soundSetting,
                        volume = currentState.volume,
                        customAudioPath = currentState.customAudioFileInfo?.uri?.toString(),
                        customAudioName = currentState.customAudioName.takeIf { it.isNotBlank() },
                        presetAudioName = currentState.selectedPresetAudio?.fileName,
                        systemNotificationMode = currentState.systemNotificationMode,
                        advanceReminderMinutes = currentState.advanceReminderMinutes,
                        createdAt = java.time.LocalDateTime.now(),
                        updatedAt = java.time.LocalDateTime.now(),
                        usageCount = 0,
                        lastUsedAt = null
                    )
                    notificationStrategyRepository.insertStrategy(strategy)
                    Timber.d("通知策略保存成功: ${currentState.name}")
                }
                _uiState.value = _uiState.value.copy(isSaved = true)
            } catch (e: Exception) {
                Timber.e(e, "Failed to save notification strategy")
            }
        }
    }
}

data class CreateNotificationStrategyUiState(
    val strategyId: String? = null,
    val isEditMode: Boolean = false,
    val name: String = "",
    val vibrationSetting: VibrationSetting = VibrationSetting.NONE,
    val soundSetting: SoundSetting = SoundSetting.NONE,
    val volume: Int = 50,
    val customAudioFileInfo: AudioFileInfo? = null,
    val customAudioName: String = "",
    val selectedPresetAudio: PresetAudio? = null,
    val systemNotificationMode: SystemNotificationMode = SystemNotificationMode.STATUS_BAR,
    val advanceReminderMinutes: List<Int> = emptyList(),
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val hasNotificationPermission: Boolean = true,
    val hasExactAlarmPermission: Boolean = true
) {
    val isValid: Boolean
        get() = name.isNotBlank()
}
