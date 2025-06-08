package com.wasbry.nextthing

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import com.wasbry.nextthing.navigation.NavigationGraph
import com.wasbry.nextthing.ui.theme.NextThingTheme

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NextThingTheme(
                dynamicColor = false, // 启用动态颜色
                darkTheme = isSystemInDarkTheme() // 深色模式，跟随系统设置
            ) {
                // 注意：NavigationGraph 的 context 参数需要传入 ApplicationContext,避免内存泄漏
                NavigationGraph(context = applicationContext)
            }
        }
    }
}