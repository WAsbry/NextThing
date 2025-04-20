package com.wasbry.nextthing

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.wasbry.nextthing.navigation.BottomNavigationBar
import com.wasbry.nextthing.navigation.NavigationGraph
import com.wasbry.nextthing.ui.theme.NextThingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NextThingTheme {
                AppNavigation()
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun AppNavigation() {
    // 创建一个可记忆的 NavController 实例
    val navController = rememberNavController()
    // 创建一个带有底部栏的布局
    Scaffold (
        // 设置底部栏为 BottomNavigationBar 组件
        bottomBar = { BottomNavigationBar(navController = navController)}
    ) {
        // 创建导航图
        NavigationGraph(navController = navController)
    }
}