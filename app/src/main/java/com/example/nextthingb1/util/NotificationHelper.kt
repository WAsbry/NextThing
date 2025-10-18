package com.example.nextthingb1.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.nextthingb1.R
import com.example.nextthingb1.domain.model.NotificationStrategy
import com.example.nextthingb1.domain.model.PresetAudio
import com.example.nextthingb1.domain.model.SoundSetting
import com.example.nextthingb1.domain.model.VibrationSetting
import com.example.nextthingb1.domain.model.Task
import java.time.format.DateTimeFormatter
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通知辅助工具类
 * 负责创建和显示任务通知，包括震动和声音处理
 *
 * 【NotificationTest】通知流程 - 第5步：显示通知
 * 此类负责实际显示通知、执行震动和播放声音
 */
@Singleton
class NotificationHelper @Inject constructor(
    private val context: Context
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    private var mediaPlayer: MediaPlayer? = null

    companion object {
        private const val CHANNEL_ID = "task_notifications"
        private const val CHANNEL_NAME = "任务通知"
        private const val CHANNEL_DESCRIPTION = "任务到期和提醒通知"
        private const val TAG = "NotificationTask"
    }

    init {
        createNotificationChannel()
    }

    /**
     * 创建通知渠道（Android 8.0+）
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Timber.tag(TAG).d("━━━━━━ 创建通知渠道 ━━━━━━")
            Timber.tag(TAG).d("Channel ID: $CHANNEL_ID")
            Timber.tag(TAG).d("Importance: IMPORTANCE_MAX")

            // 先删除旧渠道(如果存在配置错误的旧渠道)
            notificationManager.getNotificationChannel(CHANNEL_ID)?.let {
                notificationManager.deleteNotificationChannel(CHANNEL_ID)
                Timber.tag(TAG).d("已删除旧通知渠道")
            }

            // 配置声音的AudioAttributes
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_MAX
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                // 设置默认震动模式 (延迟0ms, 震动500ms, 暂停200ms, 震动500ms)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                setSound(
                    android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                    audioAttributes
                )
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                setBypassDnd(true)  // 允许在勿扰模式下显示
            }
            notificationManager.createNotificationChannel(channel)

            // 验证渠道设置
            val createdChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
            Timber.tag(TAG).d("✅ 渠道已创建")
            Timber.tag(TAG).d("   实际Importance: ${createdChannel?.importance}")
            Timber.tag(TAG).d("   震动已启用: ${createdChannel?.shouldVibrate()}")
            Timber.tag(TAG).d("   震动模式: ${createdChannel?.vibrationPattern?.contentToString()}")
            Timber.tag(TAG).d("   声音: ${createdChannel?.sound}")
            Timber.tag(TAG).d("   AudioAttributes: ${createdChannel?.audioAttributes}")
            Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━")
        }
    }

    /**
     * 显示带倒计时的任务通知
     *
     * @param task 任务对象
     * @param strategy 通知策略
     * @param secondsUntilDue 距离截止时间的秒数
     */
    fun showTaskNotificationWithCountdown(
        task: Task,
        strategy: NotificationStrategy,
        secondsUntilDue: Long
    ) {
        Timber.tag(TAG).d("━━━━━━ 显示倒计时通知开始 ━━━━━━")
        Timber.tag(TAG).d("任务: ${task.title}")
        Timber.tag(TAG).d("倒计时: ${secondsUntilDue}秒")

        // 格式化倒计时
        val countdownText = formatCountdown(secondsUntilDue)

        showTaskNotificationInternal(task, strategy, countdownText, secondsUntilDue)
    }

    /**
     * 显示任务通知
     * 【NotificationTest】显示通知的主函数
     *
     * @param task 任务对象
     * @param strategy 通知策略
     */
    fun showTaskNotification(
        task: Task,
        strategy: NotificationStrategy
    ) {
        showTaskNotificationInternal(task, strategy, null, null)
    }

    /**
     * 内部方法：显示任务通知
     */
    private fun showTaskNotificationInternal(
        task: Task,
        strategy: NotificationStrategy,
        countdownText: String? = null,
        secondsUntilDue: Long? = null
    ) {
        Timber.tag(TAG).d("━━━━━━ 显示通知开始 ━━━━━━")
        Timber.tag(TAG).d("任务: ${task.title}")
        Timber.tag(TAG).d("通知策略: ${strategy.name}")

        // 检查通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            Timber.tag(TAG).d("POST_NOTIFICATIONS权限: ${if (hasPermission) "✅已授权" else "❌未授权"}")

            if (!hasPermission) {
                Timber.tag(TAG).e("缺少POST_NOTIFICATIONS权限")
                return
            }
        }

        // 创建点击通知的Intent
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("taskId", task.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 创建 FullScreenIntent（用于 Heads-up notification）
        val fullScreenIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("taskId", task.id)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            task.id.hashCode() + 1000, // 使用不同的 requestCode
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 构建详细的通知内容
        val notificationContent = buildNotificationContent(task, countdownText)

        // 构建通知标题（包含倒计时）
        val notificationTitle = if (countdownText != null) {
            "⏰ ${task.title} - $countdownText"
        } else {
            task.title
        }

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(notificationContent)
            .setBigContentTitle(notificationTitle)

        // 构建通知
        Timber.tag(TAG).d("━━━━━━ 构建通知 ━━━━━━")
        Timber.tag(TAG).d("Priority: PRIORITY_MAX")
        Timber.tag(TAG).d("Category: CATEGORY_ALARM")
        Timber.tag(TAG).d("FullScreenIntent: 已设置")

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(notificationTitle)
            .setContentText(countdownText ?: notificationContent)
            .setStyle(bigTextStyle)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // 如果有倒计时，设置为持续通知（不自动消失）
        if (countdownText != null) {
            notificationBuilder.setOngoing(false)  // 允许用户滑动清除
            notificationBuilder.setAutoCancel(true)
        }

        // 设置震动和默认效果
        if (strategy.vibrationSetting != VibrationSetting.NONE) {
            notificationBuilder.setVibrate(strategy.vibrationSetting.pattern)
            notificationBuilder.setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_LIGHTS)
            Timber.tag(TAG).d("震动: 已设置 ${strategy.vibrationSetting.displayName}")
        } else {
            notificationBuilder.setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_LIGHTS)
            Timber.tag(TAG).d("震动: 无")
        }

        val notification = notificationBuilder.build()
        val notificationId = task.id.hashCode()

        // 检查通知渠道状态
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = notificationManager.getNotificationChannel(CHANNEL_ID)
            Timber.tag(TAG).d("━━━━━━ 通知渠道状态检查 ━━━━━━")
            Timber.tag(TAG).d("Channel Importance: ${channel?.importance} (期望: ${NotificationManager.IMPORTANCE_MAX})")
            Timber.tag(TAG).d("Channel能否弹出: ${channel?.importance == NotificationManager.IMPORTANCE_HIGH || channel?.importance == NotificationManager.IMPORTANCE_MAX}")
            Timber.tag(TAG).d("通知是否已启用: ${notificationManager.areNotificationsEnabled()}")
            Timber.tag(TAG).d("渠道震动已启用: ${channel?.shouldVibrate()}")
            Timber.tag(TAG).d("渠道震动模式: ${channel?.vibrationPattern?.contentToString()}")
            Timber.tag(TAG).d("渠道AudioAttributes: ${channel?.audioAttributes}")
        }

        try {
            Timber.tag(TAG).d("━━━━━━ 发送通知 ━━━━━━")
            Timber.tag(TAG).d("通知ID: $notificationId")
            Timber.tag(TAG).d("标题: ${task.title}")

            NotificationManagerCompat.from(context).notify(notificationId, notification)

            Timber.tag(TAG).d("✅ 通知已发送到系统")
            Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━")
        } catch (e: Exception) {
            Timber.tag(TAG).e("❌ 显示通知失败: ${e.message}")
            e.printStackTrace()
        }

        playSound(strategy)
    }

    /**
     * 构建通知的详细内容
     */
    private fun buildNotificationContent(task: Task, countdownText: String? = null): String {
        val contentBuilder = StringBuilder()

        // 倒计时提示
        if (countdownText != null) {
            contentBuilder.append("⏰ 距离截止还有：$countdownText\n\n")
        }

        // 任务描述
        if (task.description.isNotEmpty()) {
            contentBuilder.append(task.description)
            contentBuilder.append("\n\n")
        }

        // 分类
        contentBuilder.append("📂 分类: ${task.category.displayName}\n")

        // 截止时间
        if (task.dueDate != null) {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            contentBuilder.append("⏰ 截止时间: ${task.dueDate.format(formatter)}\n")
        }

        // 重要性
        task.importanceUrgency?.let {
            contentBuilder.append("⭐ 优先级: ${it.displayName}\n")
        }

        // 地理位置
        task.locationInfo?.let { location ->
            contentBuilder.append("📍 位置: ${location.address.ifEmpty { location.locationName }}\n")
            if (location.latitude != 0.0 && location.longitude != 0.0) {
                contentBuilder.append("   坐标: ${location.latitude}, ${location.longitude}\n")
            }
        }

        // 预计时长
        if (task.estimatedDuration > 0) {
            val hours = task.estimatedDuration / 60
            val minutes = task.estimatedDuration % 60
            if (hours > 0) {
                contentBuilder.append("⏱️ 预计时长: ${hours}小时${minutes}分钟\n")
            } else {
                contentBuilder.append("⏱️ 预计时长: ${minutes}分钟\n")
            }
        }

        // 标签
        if (task.tags.isNotEmpty()) {
            contentBuilder.append("🏷️ 标签: ${task.tags.joinToString(", ")}\n")
        }

        // 重复频率
        if (task.repeatFrequency.type != com.example.nextthingb1.domain.model.RepeatFrequencyType.NONE) {
            contentBuilder.append("🔄 重复: ${getRepeatFrequencyText(task.repeatFrequency)}\n")
        }

        return contentBuilder.toString().trim()
    }

    /**
     * 获取重复频率的文本描述
     */
    private fun getRepeatFrequencyText(repeatFrequency: com.example.nextthingb1.domain.model.RepeatFrequency): String {
        return when (repeatFrequency.type) {
            com.example.nextthingb1.domain.model.RepeatFrequencyType.NONE -> "不重复"
            com.example.nextthingb1.domain.model.RepeatFrequencyType.DAILY -> "每天"
            com.example.nextthingb1.domain.model.RepeatFrequencyType.WEEKLY -> {
                if (repeatFrequency.weekdays.isNotEmpty()) {
                    val days = repeatFrequency.weekdays.sorted().joinToString(", ") { day ->
                        when (day) {
                            1 -> "周一"
                            2 -> "周二"
                            3 -> "周三"
                            4 -> "周四"
                            5 -> "周五"
                            6 -> "周六"
                            7 -> "周日"
                            else -> ""
                        }
                    }
                    "每周 $days"
                } else {
                    "每周"
                }
            }
            com.example.nextthingb1.domain.model.RepeatFrequencyType.MONTHLY -> {
                if (repeatFrequency.monthDays.isNotEmpty()) {
                    val days = repeatFrequency.monthDays.sorted().joinToString(", ") { "${it}日" }
                    "每月 $days"
                } else {
                    "每月"
                }
            }
        }
    }

    /**
     * 执行震动
     */
    private fun executeVibration(vibrationSetting: VibrationSetting) {
        if (vibrationSetting == VibrationSetting.NONE) {
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(
                    vibrationSetting.pattern,
                    -1
                )
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(vibrationSetting.pattern, -1)
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e("震动失败: ${e.message}")
        }
    }

    /**
     * 播放声音
     */
    private fun playSound(strategy: NotificationStrategy) {
        if (strategy.soundSetting == SoundSetting.NONE) {
            return
        }

        try {
            releaseMediaPlayer()

            mediaPlayer = when (strategy.soundSetting) {
                SoundSetting.STANDARD_TONE -> {
                    val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    createMediaPlayer(uri, strategy.volume)
                }
                SoundSetting.PRESET_AUDIO -> {
                    val presetAudio = strategy.presetAudioName?.let { name ->
                        PresetAudio.findByFileName(name)
                    }
                    if (presetAudio != null) {
                        val resourceName = presetAudio.fileName.substringBeforeLast(".")
                        val resourceId = context.resources.getIdentifier(
                            resourceName,
                            "raw",
                            context.packageName
                        )
                        if (resourceId != 0) {
                            val uri = Uri.parse("android.resource://${context.packageName}/$resourceId")
                            createMediaPlayer(uri, strategy.volume)
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }
                SoundSetting.CUSTOM_AUDIO, SoundSetting.RECORDING_AUDIO -> {
                    if (strategy.customAudioPath != null) {
                        createMediaPlayer(Uri.parse(strategy.customAudioPath), strategy.volume)
                    } else {
                        null
                    }
                }
                SoundSetting.NONE -> null
            }

            mediaPlayer?.start()
        } catch (e: Exception) {
            Timber.tag(TAG).e("播放声音失败: ${e.message}")
            releaseMediaPlayer()
        }
    }

    /**
     * 创建 MediaPlayer
     */
    private fun createMediaPlayer(uri: Uri, volume: Int): MediaPlayer? {
        return try {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                )
                setDataSource(context, uri)

                // 设置音量（0.0 - 1.0）
                val volumeFloat = volume / 100f
                setVolume(volumeFloat, volumeFloat)

                prepare()

                // 播放完成后释放资源
                setOnCompletionListener {
                    releaseMediaPlayer()
                }

                // 错误处理
                setOnErrorListener { _, what, extra ->
                    Timber.e("MediaPlayer error: what=$what, extra=$extra")
                    releaseMediaPlayer()
                    true
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to create MediaPlayer")
            null
        }
    }

    /**
     * 释放 MediaPlayer 资源
     */
    private fun releaseMediaPlayer() {
        mediaPlayer?.apply {
            try {
                if (isPlaying) {
                    stop()
                }
                release()
            } catch (e: Exception) {
                Timber.e(e, "Error releasing MediaPlayer")
            }
        }
        mediaPlayer = null
    }

    /**
     * 停止所有通知相关的音效
     */
    fun stopAllSounds() {
        releaseMediaPlayer()
    }

    /**
     * 取消指定任务的通知
     */
    fun cancelNotification(taskId: String) {
        try {
            NotificationManagerCompat.from(context).cancel(taskId.hashCode())
            Timber.d("Notification cancelled for task: $taskId")
        } catch (e: Exception) {
            Timber.e(e, "Failed to cancel notification")
        }
    }

    /**
     * 取消所有通知
     */
    fun cancelAllNotifications() {
        try {
            NotificationManagerCompat.from(context).cancelAll()
            Timber.d("All notifications cancelled")
        } catch (e: Exception) {
            Timber.e(e, "Failed to cancel all notifications")
        }
    }

    /**
     * 格式化倒计时文本
     * @param seconds 秒数
     * @return 格式化的倒计时文本，如 "2分30秒"
     */
    private fun formatCountdown(seconds: Long): String {
        if (seconds <= 0) {
            return "已到时"
        }

        val minutes = seconds / 60
        val remainingSeconds = seconds % 60

        return when {
            minutes > 0 && remainingSeconds > 0 -> "${minutes}分${remainingSeconds}秒"
            minutes > 0 -> "${minutes}分钟"
            else -> "${remainingSeconds}秒"
        }
    }
}
