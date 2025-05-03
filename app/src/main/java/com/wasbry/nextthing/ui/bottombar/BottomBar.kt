package com.wasbry.nextthing.ui.bottombar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import com.wasbry.nextthing.R
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.wasbry.nextthing.navigation.CustomBottomNavIcon
import com.wasbry.nextthing.navigation.Screen

@Composable
fun BottomBar(
    navController: NavHostController,
    onAddTaskClick: () -> Unit
) {
    // 定义底部导航栏的屏幕路由及对应的图标资源
    val items = listOf(
        Screen.HomePage to R.drawable.icon_home_page,
        Screen.TaskDetail to R.drawable.icon_task_detail,
        Screen.AddTask to R.drawable.icon_add_task,
        Screen.Statistic to R.drawable.icon_statistic,
        Screen.Mine to R.drawable.icon_mine
    )

    // 获取当前导航栈的栈顶条目，用于判断当前显示的页面
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    // 获取当前页面的路由
    val currentRoute = navBackStackEntry?.destination?.route

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            items.forEach { (screen, iconResId) ->
                if (screen == Screen.AddTask) {
                    CustomBottomNavIcon(
                        modifier = Modifier.weight(1f),
                        iconResId = iconResId,
                        text = screen.route,
                        isSelected = false,
                        selectedColor = MaterialTheme.colorScheme.primary,
                        unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = onAddTaskClick
                    )
                } else {
                    CustomBottomNavIcon(
                        modifier = Modifier.weight(1f),
                        iconResId = iconResId,
                        text = screen.route,
                        isSelected = currentRoute == screen.route,
                        selectedColor = MaterialTheme.colorScheme.primary,
                        unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = {
                            navController.navigate(screen.route) {
                                navController.graph.startDestinationRoute?.let { route ->
                                    popUpTo(route) {
                                        saveState = true
                                    }
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    }
}