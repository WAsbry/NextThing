package com.wasbry.nextthing.navigation

/**
 * 密封类，用于表示不同的屏幕路由
 * */
sealed class Screen(val route: String) {
    object HomePage : Screen("HomePage") // 首页
    object TaskDetail : Screen("TaskDetail") // 任务的详情页，类比账单嘛
    object AddTask : Screen("AddTask") // 添加任务
    object Statistic : Screen("Statistic") // 任务的统计情况
    object Mine : Screen("Mine") // 我的一些信息
}