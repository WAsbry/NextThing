package com.example.nextthingb1.domain.model

import java.time.LocalDateTime
import java.util.UUID

/**
 * 通知策略数据模型
 */
data class NotificationStrategy(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val isGeofenceEnabled: Boolean = false,
    val vibrationSetting: VibrationSetting = VibrationSetting.NONE,
    val soundSetting: SoundSetting = SoundSetting.NONE,
    val volume: Int = 50, // 0-100
    val customAudioPath: String? = null, // 自定义音频文件路径
    val customAudioName: String? = null, // 自定义音频文件的备注名称
    val presetAudioName: String? = null, // 预置音频资源名称
    val systemNotificationMode: SystemNotificationMode = SystemNotificationMode.STATUS_BAR,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val usageCount: Int = 0,
    val lastUsedAt: LocalDateTime? = null
)

/**
 * 震动设置枚举
 */
enum class VibrationSetting(
    val displayName: String,
    val description: String,
    val pattern: LongArray,
    val icon: String
) {
    NONE(
        "无震动",
        "不产生震动",
        longArrayOf(),
        "🔇"
    ),
    LIGHT(
        "轻微震动",
        "1 次短震",
        longArrayOf(0, 200),
        "📳"
    ),
    MEDIUM(
        "中等震动",
        "2 次短震，节奏：短 - 短",
        longArrayOf(0, 200, 100, 200),
        "📲"
    ),
    STRONG(
        "强烈震动",
        "3 次长震，节奏：长 - 短 - 长",
        longArrayOf(0, 500, 200, 200, 200, 500),
        "⚡"
    )
}

/**
 * 声音设置枚举
 */
enum class SoundSetting(
    val displayName: String,
    val description: String,
    val soundType: SoundType,
    val icon: String
) {
    NONE(
        "无声音",
        "静音模式",
        SoundType.NONE,
        "🔇"
    ),
    STANDARD_TONE(
        "标准提示音",
        "标准系统通知音",
        SoundType.DEFAULT_NOTIFICATION,
        "🔊"
    ),
    PRESET_AUDIO(
        "预置音效",
        "选择内置音频资源",
        SoundType.PRESET_AUDIO,
        "🎵"
    ),
    CUSTOM_AUDIO(
        "自定义音效",
        "从文件管理器选择",
        SoundType.CUSTOM_AUDIO,
        "📁"
    ),
    RECORDING_AUDIO(
        "录音文件",
        "使用录音作为提示音",
        SoundType.RECORDING_AUDIO,
        "🎤"
    )
}

enum class SoundType {
    NONE,
    NOTIFICATION,
    DEFAULT_NOTIFICATION,
    RINGTONE,
    PRESET_AUDIO,      // 预置音频资源
    CUSTOM_AUDIO,      // 用户选择的文件
    RECORDING_AUDIO    // 录音文件
}

/**
 * 系统通知方式枚举
 */
enum class SystemNotificationMode(
    val displayName: String,
    val description: String,
    val icon: String
) {
    STATUS_BAR(
        "状态栏图标",
        "仅显示小图标",
        "📱"
    ),
    BANNER(
        "横幅通知",
        "顶部弹出，3 秒自动消失",
        "📢"
    ),
    DIALOG(
        "弹窗通知",
        "屏幕中央，需手动关闭",
        "💬"
    )
}

/**
 * 通知策略创建请求
 */
data class CreateNotificationStrategyRequest(
    val name: String,
    val isGeofenceEnabled: Boolean,
    val vibrationSetting: VibrationSetting,
    val soundSetting: SoundSetting,
    val volume: Int,
    val customAudioPath: String? = null,
    val customAudioName: String? = null,
    val presetAudioName: String? = null,
    val systemNotificationMode: SystemNotificationMode
)

/**
 * 通知策略更新请求
 */
data class UpdateNotificationStrategyRequest(
    val id: String,
    val name: String,
    val isGeofenceEnabled: Boolean,
    val vibrationSetting: VibrationSetting,
    val soundSetting: SoundSetting,
    val volume: Int,
    val customAudioPath: String? = null,
    val customAudioName: String? = null,
    val presetAudioName: String? = null,
    val systemNotificationMode: SystemNotificationMode
)