package com.example.nextthingb1.util

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 权限管理器
 * 负责检查和请求应用所需的所有权限
 *
 * 【NotificationTest】权限管理 - 统一管理通知和闹钟权限
 */
@Singleton
class PermissionManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "NotificationTest"
    }

    /**
     * 检查是否有通知权限
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Android 13以下默认有通知权限
        }
    }

    /**
     * 检查是否有精确闹钟权限
     */
    fun hasExactAlarmPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true // Android 12以下默认有精确闹钟权限
        }
    }

    /**
     * 检查所有必需的权限
     * @return 权限状态对象
     */
    fun checkAllPermissions(): PermissionStatus {
        val hasNotification = hasNotificationPermission()
        val hasExactAlarm = hasExactAlarmPermission()

        Timber.tag(TAG).d("权限检查结果:")
        Timber.tag(TAG).d("  通知权限: ${if (hasNotification) "✅已授予" else "❌未授予"}")
        Timber.tag(TAG).d("  精确闹钟权限: ${if (hasExactAlarm) "✅已授予" else "❌未授予"}")

        return PermissionStatus(
            hasNotificationPermission = hasNotification,
            hasExactAlarmPermission = hasExactAlarm
        )
    }

    /**
     * 打开精确闹钟权限设置页面
     */
    fun openExactAlarmSettings(activity: ComponentActivity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Timber.tag(TAG).d("打开精确闹钟权限设置页面")
            try {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                activity.startActivity(intent)
            } catch (e: Exception) {
                Timber.tag(TAG).e("无法打开精确闹钟设置页面: ${e.message}")
                // 降级到应用详情页
                openAppSettings(activity)
            }
        }
    }

    /**
     * 打开应用设置页面
     */
    fun openAppSettings(activity: ComponentActivity) {
        Timber.tag(TAG).d("打开应用设置页面")
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            Timber.tag(TAG).e("无法打开应用设置页面: ${e.message}")
        }
    }

    /**
     * 创建通知权限请求启动器
     * 在Activity的onCreate中调用
     */
    fun createNotificationPermissionLauncher(
        activity: ComponentActivity,
        onResult: (Boolean) -> Unit
    ): ActivityResultLauncher<String>? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                Timber.tag(TAG).d("通知权限请求结果: ${if (isGranted) "已授予" else "被拒绝"}")
                onResult(isGranted)
            }
        } else {
            null
        }
    }

    /**
     * 请求通知权限
     */
    fun requestNotificationPermission(
        launcher: ActivityResultLauncher<String>?
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Timber.tag(TAG).d("请求通知权限")
            launcher?.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

/**
 * 权限状态数据类
 */
data class PermissionStatus(
    val hasNotificationPermission: Boolean,
    val hasExactAlarmPermission: Boolean
) {
    /**
     * 是否所有权限都已授予
     */
    val allGranted: Boolean
        get() = hasNotificationPermission && hasExactAlarmPermission

    /**
     * 获取缺失的权限列表（用于显示）
     */
    val missingPermissions: List<MissingPermission>
        get() = buildList {
            if (!hasNotificationPermission) {
                add(
                    MissingPermission(
                        name = "通知权限",
                        description = "允许应用在任务到期时显示通知",
                        isRequired = true,
                        canRequestDirectly = true
                    )
                )
            }
            if (!hasExactAlarmPermission) {
                add(
                    MissingPermission(
                        name = "精确闹钟权限",
                        description = "允许应用在精确时间触发任务提醒",
                        isRequired = true,
                        canRequestDirectly = false // 需要跳转到设置页面
                    )
                )
            }
        }
}

/**
 * 缺失的权限信息
 */
data class MissingPermission(
    val name: String,
    val description: String,
    val isRequired: Boolean,
    val canRequestDirectly: Boolean // true表示可以直接弹窗请求，false表示需要跳转设置
)
