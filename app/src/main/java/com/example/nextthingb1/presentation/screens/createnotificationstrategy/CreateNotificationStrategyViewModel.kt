package com.example.nextthingb1.presentation.screens.createnotificationstrategy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextthingb1.domain.model.VibrationSetting
import com.example.nextthingb1.domain.model.SoundSetting
import com.example.nextthingb1.domain.model.SoundType
import com.example.nextthingb1.domain.model.SystemNotificationMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import com.example.nextthingb1.domain.model.PresetAudio
import com.example.nextthingb1.util.AudioFileInfo
import javax.inject.Inject

@HiltViewModel
class CreateNotificationStrategyViewModel @Inject constructor(
    @ApplicationContext private val context: Context
    // TODO: 注入通知策略用例
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateNotificationStrategyUiState())
    val uiState: StateFlow<CreateNotificationStrategyUiState> = _uiState.asStateFlow()

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    fun updateGeofenceEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isGeofenceEnabled = enabled)
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

        viewModelScope.launch {
            try {
                if (currentSound.soundType == SoundType.NONE) {
                    Timber.d("Sound is set to NONE, no preview to play")
                    return@launch
                }

                when (currentSound.soundType) {
                    SoundType.NONE -> return@launch

                    SoundType.NOTIFICATION,
                    SoundType.DEFAULT_NOTIFICATION,
                    SoundType.RINGTONE -> {
                        val soundUri = getSoundUri(currentSound.soundType)
                        if (soundUri != null) {
                            playSoundWithUri(soundUri, currentVolume)
                        }
                    }

                    SoundType.PRESET_AUDIO -> {
                        currentState.selectedPresetAudio?.let { presetAudio ->
                            playPresetAudio(presetAudio, currentVolume)
                        }
                    }

                    SoundType.CUSTOM_AUDIO,
                    SoundType.RECORDING_AUDIO -> {
                        Timber.d("Attempting to play custom audio. customAudioFileInfo: ${currentState.customAudioFileInfo}")
                        currentState.customAudioFileInfo?.let { audioInfo ->
                            Timber.d("Playing custom audio with URI: ${audioInfo.uri}")
                            playCustomAudio(audioInfo.uri, currentVolume)
                        } ?: run {
                            Timber.w("Custom audio selected but customAudioFileInfo is null")
                        }
                    }
                }

                Timber.d("Playing sound preview: ${currentSound.displayName} at volume $currentVolume")
            } catch (e: Exception) {
                Timber.e(e, "Failed to play sound preview")
            }
        }
    }

    private fun getSoundUri(soundType: SoundType): Uri? {
        return when (soundType) {
            SoundType.NONE -> null
            SoundType.NOTIFICATION -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            SoundType.DEFAULT_NOTIFICATION -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            SoundType.RINGTONE -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            SoundType.PRESET_AUDIO -> null // 预置音频通过playPresetAudio处理
            SoundType.CUSTOM_AUDIO -> null // 自定义音频通过playCustomAudio处理
            SoundType.RECORDING_AUDIO -> null // 录音文件通过playCustomAudio处理
        }
    }

    private fun playSoundWithUri(uri: Uri, volume: Int) {
        try {
            val ringtone = RingtoneManager.getRingtone(context, uri)
            if (ringtone != null) {
                // 设置音量（通过AudioManager）
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)
                val targetVolume = (maxVolume * volume / 100f).toInt()
                audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, targetVolume, 0)

                // 播放铃声
                ringtone.play()

                // 设置延迟停止，避免播放过长
                viewModelScope.launch {
                    kotlinx.coroutines.delay(2000) // 2秒后停止
                    if (ringtone.isPlaying) {
                        ringtone.stop()
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error playing sound with URI: $uri")
        }
    }

    private fun playPresetAudio(presetAudio: PresetAudio, volume: Int) {
        try {
            // 从assets中播放预置音频
            val assetPath = "audio/${presetAudio.fileName}"
            context.assets.openFd(assetPath).use { assetFileDescriptor ->
                val mediaPlayer = MediaPlayer()
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

                // 播放完成后自动释放，不限制时长
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
                // TODO: 保存通知策略
                /*
                val result = notificationStrategyUseCases.createStrategy(
                    name = currentState.name,
                    isGeofenceEnabled = currentState.isGeofenceEnabled,
                    vibrationSetting = currentState.vibrationSetting,
                    soundSetting = currentState.soundSetting,
                    volume = currentState.volume,
                    systemNotificationMode = currentState.systemNotificationMode
                )

                if (result.isSuccess) {
                    Timber.d("Notification strategy saved successfully: ${currentState.name}")
                    _uiState.value = _uiState.value.copy(isSaved = true)
                } else {
                    Timber.e("Failed to save notification strategy: ${result.exceptionOrNull()?.message}")
                }
                */

                // 临时模拟保存成功
                Timber.d("Notification strategy saved successfully: ${currentState.name}")
                _uiState.value = _uiState.value.copy(isSaved = true)
            } catch (e: Exception) {
                Timber.e(e, "Failed to save notification strategy")
            }
        }
    }
}

data class CreateNotificationStrategyUiState(
    val name: String = "",
    val isGeofenceEnabled: Boolean = false,
    val vibrationSetting: VibrationSetting = VibrationSetting.NONE,
    val soundSetting: SoundSetting = SoundSetting.NONE,
    val volume: Int = 50,
    val customAudioFileInfo: AudioFileInfo? = null,
    val customAudioName: String = "",
    val selectedPresetAudio: PresetAudio? = null,
    val systemNotificationMode: SystemNotificationMode = SystemNotificationMode.STATUS_BAR,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false
) {
    val isValid: Boolean
        get() = name.isNotBlank()
}