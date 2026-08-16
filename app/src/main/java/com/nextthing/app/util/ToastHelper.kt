package com.nextthing.app.util

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.*

/**
 * 防抖Toast工具类
 * 用于避免短时间内多次触发相同Toast导致的重叠显示问题
 */
object ToastHelper {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var currentToast: Toast? = null
    private var debounceJob: Job? = null
    private var cleanupJob: Job? = null

    /**
     * 显示防抖Toast
     * @param context 上下文
     * @param message 要显示的消息
     * @param duration Toast显示时长，默认为Toast.LENGTH_SHORT
     * @param debounceDelayMs 防抖延迟时间（毫秒），默认500ms
     */
    fun showDebouncedToast(
        context: Context,
        message: String,
        duration: Int = Toast.LENGTH_SHORT,
        debounceDelayMs: Long = 500L
    ) {
        // 取消之前的防抖任务
        debounceJob?.cancel()
        cleanupJob?.cancel()

        // 所有 Toast 操作都收敛到同一个 Main scope，避免后台线程调用和游离协程。
        debounceJob = scope.launch {
            currentToast?.cancel()
            currentToast = null
            delay(debounceDelayMs)

            // 使用 ApplicationContext 避免内存泄漏
            val appContext = context.applicationContext
            val toast = Toast.makeText(appContext, message, duration)
            currentToast = toast
            toast.show()

            // Toast 显示后延迟清理引用，避免内存泄漏
            delay(if (duration == Toast.LENGTH_LONG) 3500L else 2000L)
            if (currentToast === toast) {
                currentToast = null
            }
        }
    }

    /**
     * 立即显示Toast（不防抖）
     * @param context 上下文
     * @param message 要显示的消息
     * @param duration Toast显示时长，默认为Toast.LENGTH_SHORT
     */
    fun showToast(
        context: Context,
        message: String,
        duration: Int = Toast.LENGTH_SHORT
    ) {
        debounceJob?.cancel()
        cleanupJob?.cancel()
        cleanupJob = scope.launch {
            currentToast?.cancel()
            val appContext = context.applicationContext
            val toast = Toast.makeText(appContext, message, duration)
            currentToast = toast
            toast.show()
            delay(if (duration == Toast.LENGTH_LONG) 3500L else 2000L)
            if (currentToast === toast) {
                currentToast = null
            }
        }
    }

    /**
     * 取消当前显示的Toast和防抖任务
     */
    fun cancelAll() {
        debounceJob?.cancel()
        cleanupJob?.cancel()
        currentToast?.cancel()
        currentToast = null
    }
}
