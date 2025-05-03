package com.wasbry.nextthing.navigation

/**
 * 密封类，用于表示不同的屏幕路由
 * */
sealed class Screen(val route: String) {
    object HomePage : Screen("首页") // 首页
    object TaskDetail : Screen("任务列表") // 任务的详情页，类比账单嘛
    object AddTask : Screen("添加任务") // 添加任务
    object Statistic : Screen("数据统计") // 任务的统计情况
    object Mine : Screen("我的") // 我的一些信息
}