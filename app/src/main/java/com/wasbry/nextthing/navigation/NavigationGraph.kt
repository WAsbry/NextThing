package com.wasbry.nextthing.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.input.pointer.PointerIcon.Companion.Text
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController


/**
 * 路由表
 * */
@Composable
fun NavigationGraph(navController: NavHostController) {
    // 创建导航图，设置起始路由为 HomePage
    NavHost(navController, startDestination = Screen.HomePage.route) {
        composable(Screen.HomePage.route) {
            Text(text = "首页")
        }
        composable(Screen.TaskDetail.route) {
            Text(text = "任务详情")
        }
        composable(Screen.AddTask.route) {
            Text(text = "添加任务")
        }
        composable(Screen.Statistic.route) {
            Text(text = "任务统计")
        }
        composable(Screen.Mine.route) {
            Text(text = "个人信息")
        }
    }
}