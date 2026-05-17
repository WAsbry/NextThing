package com.nextthing.app.util

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
import android.os.VibratorManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nextthing.app.R
import com.nextthing.app.domain.model.NotificationStrategy
import com.nextthing.app.domain.model.PresetAudio
import com.nextthing.app.domain.model.SoundSetting
import com.nextthing.app.domain.model.SystemNotificationMode
import com.nextthing.app.domain.model.VibrationSetting
import com.nextthing.app.domain.model.Task
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
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    private var mediaPlayer: MediaPlayer? = null

    // 通知去重：使用 SharedPreferences 持久化，防止进程被杀后去重失效
    private val dedupPrefs = context.getSharedPreferences("notification_dedup", Context.MODE_PRIVATE)
    private val DEDUP_WINDOW_MS = 15 * 60 * 1000L // 15 分钟内不重复通知

    companion object {
        private const val CHANNEL_ID = "task_notifications"
        private const val CHANNEL_NAME = "任务通知"
        private const val CHANNEL_DESCRIPTION = "任务到期和提醒通知"
        private const val STATUS_BAR_CHANNEL_ID = "task_status_bar_notifications"
        private const val STATUS_BAR_CHANNEL_NAME = "状态栏提醒"
        private const val STATUS_BAR_CHANNEL_DESCRIPTION = "仅在状态栏显示的静默提醒"
        private const val SILENT_CHANNEL_ID = "task_geofence_notifications"
        private const val SILENT_CHANNEL_NAME = "地理围栏提醒"
        private const val SILENT_CHANNEL_DESCRIPTION = "地理围栏触发的静默提醒通知"
        private const val TAG = "NotificationTask"
    }

    init {
        createNotificationChannel()
        createStatusBarNotificationChannel()
        createSilentNotificationChannel()
    }

    /**
     * 创建通知渠道（Android 8.0+）
     * 只在首次安装时创建；已存在时跳过，保留用户对渠道的自定义设置
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 渠道已存在则跳过，避免每次启动重置用户的自定义设置
            if (notificationManager.getNotificationChannel(CHANNEL_ID) != null) return

            Timber.tag(TAG).d("━━━━━━ 创建通知渠道 ━━━━━━")
            Timber.tag(TAG).d("Channel ID: $CHANNEL_ID")
            Timber.tag(TAG).d("Importance: IMPORTANCE_HIGH")

            // 配置声音的AudioAttributes
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
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
     * 创建低优先级状态栏通知渠道（STATUS_BAR 模式使用）
     */
    private fun createStatusBarNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(STATUS_BAR_CHANNEL_ID) != null) return

            val channel = NotificationChannel(
                STATUS_BAR_CHANNEL_ID,
                STATUS_BAR_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = STATUS_BAR_CHANNEL_DESCRIPTION
                enableVibration(false)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
            Timber.tag(TAG).d("状态栏通知渠道已创建: $STATUS_BAR_CHANNEL_ID")
        }
    }

    /**
     * 创建低优先级静默通知渠道（用于地理围栏提醒）
     */
    private fun createSilentNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(SILENT_CHANNEL_ID) != null) return

            val channel = NotificationChannel(
                SILENT_CHANNEL_ID,
                SILENT_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = SILENT_CHANNEL_DESCRIPTION
                enableVibration(false)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
            Timber.tag(TAG).d("✅ 静默通知渠道已创建: $SILENT_CHANNEL_ID")
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
     * 显示提前提醒通知
     * @param advanceMinutes 提前多少分钟的提醒
     */
    fun showAdvanceReminderNotification(
        task: Task,
        strategy: NotificationStrategy,
        advanceMinutes: Int
    ) {
        val reminderText = when {
            advanceMinutes >= 1440 -> "${advanceMinutes / 1440}天后到期"
            advanceMinutes >= 60 -> "${advanceMinutes / 60}小时后到期"
            else -> "${advanceMinutes}分钟后到期"
        }
        showTaskNotificationInternal(task, strategy, reminderText, advanceMinutes * 60L)
    }

    /**
     * 检查任务是否在去重窗口内已经被通知过（持久化存储，进程重启后仍有效）
     * @return true 表示已通知过，应该跳过
     */
    fun isRecentlyNotified(taskId: String): Boolean {
        val now = System.currentTimeMillis()
        val lastNotified = dedupPrefs.getLong("dedup_$taskId", 0L)
        if (lastNotified > 0 && now - lastNotified < DEDUP_WINDOW_MS) {
            return true
        }
        // 惰性清理：如果已过期则移除
        if (lastNotified > 0) {
            dedupPrefs.edit().remove("dedup_$taskId").apply()
        }
        return false
    }

    /**
     * 标记任务已通知（持久化到 SharedPreferences）
     */
    fun markAsNotified(taskId: String) {
        dedupPrefs.edit().putLong("dedup_$taskId", System.currentTimeMillis()).apply()
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
        Timber.tag(TAG).d("通知模式: ${strategy.systemNotificationMode.displayName}")

        // 去重检查（倒计时更新通知不做去重，允许反复刷新）
        if (countdownText == null && isRecentlyNotified(task.id)) {
            Timber.tag(TAG).d("任务 ${task.id} 在15分钟内已通知过，跳过")
            return
        }

        // 检查通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

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

        // 创建 FullScreenIntent（用于 BANNER / DIALOG 模式）
        val fullScreenIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("taskId", task.id)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            task.id.hashCode() + 1000,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 构建详细的通知内容
        val notificationContent = buildNotificationContent(task, countdownText)

        // 构建通知标题
        val notificationTitle = if (countdownText != null) {
            "${task.title} - $countdownText"
        } else {
            task.title
        }

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(notificationContent)
            .setBigContentTitle(notificationTitle)

        // 根据 SystemNotificationMode 选择渠道和行为
        val channelId: String
        val priority: Int
        val useFullScreenIntent: Boolean
        val isOngoing: Boolean

        when (strategy.systemNotificationMode) {
            SystemNotificationMode.STATUS_BAR -> {
                channelId = STATUS_BAR_CHANNEL_ID
                priority = NotificationCompat.PRIORITY_LOW
                useFullScreenIntent = false
                isOngoing = false
                Timber.tag(TAG).d("模式: STATUS_BAR (低优先级，仅状态栏)")
            }
            SystemNotificationMode.BANNER -> {
                channelId = CHANNEL_ID
                priority = NotificationCompat.PRIORITY_MAX
                useFullScreenIntent = true
                isOngoing = false
                Timber.tag(TAG).d("模式: BANNER (高优先级，横幅弹出)")
            }
            SystemNotificationMode.DIALOG -> {
                channelId = CHANNEL_ID
                priority = NotificationCompat.PRIORITY_MAX
                useFullScreenIntent = true
                isOngoing = true  // 需要用户手动关闭
                Timber.tag(TAG).d("模式: DIALOG (最高优先级，弹窗通知)")
            }
        }

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(notificationTitle)
            .setContentText(countdownText ?: notificationContent)
            .setStyle(bigTextStyle)
            .setPriority(priority)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(!isOngoing)
            .setOngoing(isOngoing)
            .setContentIntent(pendingIntent)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (useFullScreenIntent) {
            notificationBuilder.setFullScreenIntent(fullScreenPendingIntent, true)
        }

        // STATUS_BAR 模式静默；BANNER/DIALOG 模式由下方 executeVibration() + playSound() 手动处理
        if (strategy.systemNotificationMode == SystemNotificationMode.STATUS_BAR) {
            notificationBuilder.setSilent(true)
        } else {
            // 仅保留 LED 灯光默认行为，声音和震动通过手动调用控制，避免与 builder/channel 重复触发
            notificationBuilder.setDefaults(NotificationCompat.DEFAULT_LIGHTS)
            notificationBuilder.setSound(null)
            notificationBuilder.setVibrate(null)
        }

        val notification = notificationBuilder.build()
        val notificationId = task.id.hashCode()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            // 记录已通知（去重）
            markAsNotified(task.id)
            Timber.tag(TAG).d("通知已发送: ID=$notificationId, 标题=${task.title}")
        } catch (e: Exception) {
            Timber.tag(TAG).e("显示通知失败: ${e.message}")
        }

        // STATUS_BAR 模式不执行额外的震动和声音
        if (strategy.systemNotificationMode != SystemNotificationMode.STATUS_BAR) {
            executeVibration(strategy.vibrationSetting)
            playSound(strategy)
        }
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
        if (task.repeatFrequency.type != com.nextthing.app.domain.model.RepeatFrequencyType.NONE) {
            contentBuilder.append("🔄 重复: ${getRepeatFrequencyText(task.repeatFrequency)}\n")
        }

        return contentBuilder.toString().trim()
    }

    /**
     * 获取重复频率的文本描述
     */
    private fun getRepeatFrequencyText(repeatFrequency: com.nextthing.app.domain.model.RepeatFrequency): String {
        return when (repeatFrequency.type) {
            com.nextthing.app.domain.model.RepeatFrequencyType.NONE -> "不重复"
            com.nextthing.app.domain.model.RepeatFrequencyType.DAILY -> "每天"
            com.nextthing.app.domain.model.RepeatFrequencyType.WEEKDAYS -> "工作日"
            com.nextthing.app.domain.model.RepeatFrequencyType.WEEKENDS -> "周末"
            com.nextthing.app.domain.model.RepeatFrequencyType.LEGAL_HOLIDAY -> "法定节假日"
            com.nextthing.app.domain.model.RepeatFrequencyType.WEEKLY -> {
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
            com.nextthing.app.domain.model.RepeatFrequencyType.MONTHLY -> {
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
        Timber.tag(TAG).d("━━━━━━ 执行震动 ━━━━━━")
        Timber.tag(TAG).d("震动设置: ${vibrationSetting.displayName}")

        if (vibrationSetting == VibrationSetting.NONE) {
            Timber.tag(TAG).d("震动设置为NONE，跳过震动")
            return
        }

        try {
            Timber.tag(TAG).d("震动模式: ${vibrationSetting.pattern.contentToString()}")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(
                    vibrationSetting.pattern,
                    -1
                )
                vibrator.vibrate(effect)
                Timber.tag(TAG).d("✅ 震动已执行 (Android O+)")
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(vibrationSetting.pattern, -1)
                Timber.tag(TAG).d("✅ 震动已执行 (Legacy API)")
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e("❌ 震动失败: ${e.message}")
            e.printStackTrace()
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
     * 显示低优先级通知
     *
     * 用于地理围栏功能：当用户不在目标地点范围内时发送的提醒通知
     * 特点：
     * - 低优先级（PRIORITY_DEFAULT）
     * - 静默通知（无声音和震动）
     * - 可被用户滑动清除
     *
     * @param taskId 任务ID（用于通知ID）
     * @param title 通知标题
     * @param content 通知内容（简短）
     * @param fullContent 通知完整内容（展开时显示）
     */
    fun showLowPriorityNotification(
        taskId: String,
        title: String,
        content: String,
        fullContent: String
    ) {
        Timber.tag(TAG).d("━━━━━━ 显示低优先级通知 ━━━━━━")
        Timber.tag(TAG).d("任务ID: $taskId")
        Timber.tag(TAG).d("标题: $title")

        // 检查通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                Timber.tag(TAG).e("缺少POST_NOTIFICATIONS权限")
                return
            }
        }

        // 创建点击通知的Intent
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("taskId", taskId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 构建 BigTextStyle
        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(fullContent)
            .setBigContentTitle(title)

        // 构建低优先级通知（使用静默渠道，让渠道级别决定声音/震动行为）
        val notificationBuilder = NotificationCompat.Builder(context, SILENT_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(bigTextStyle)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // 低优先级
            .setCategory(NotificationCompat.CATEGORY_REMINDER) // 提醒类别
            .setAutoCancel(true) // 点击后自动消失
            .setContentIntent(pendingIntent)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true) // 只在第一次提醒（静默）
            .setSilent(true) // 静默通知（无声音和震动）

        // 显示通知
        try {
            NotificationManagerCompat.from(context).notify(
                taskId.hashCode(),
                notificationBuilder.build()
            )
            Timber.tag(TAG).d("✅ 低优先级通知已显示")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "显示低优先级通知失败")
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
