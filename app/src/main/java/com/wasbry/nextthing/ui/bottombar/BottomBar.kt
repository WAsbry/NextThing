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
                CustomBottomNavIcon(
                    modifier = Modifier.weight(1f),
                    iconResId = iconResId,
                    text = screen.route,
                    isSelected = currentRoute == screen.route,
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = {

                        // TODO 根据不同的页面，执行不同的导航逻辑
                        if (screen.route == Screen.HomePage.route) {
                            // 首页：清除所有历史页面，进行全面重置
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) { // 移除起始目的地之上的所有页面
                                    // 默认情况下，popUpTo 不会移除目标目的地本身
                                    //设置 inclusive = true 后，会连起始目的地也一起移除
                                    //这里的效果是清除整个导航栈，让应用回到初始状态
                                    inclusive = true
                                }
                                // 确保目标页面在栈顶只有一个实例
                                // 如果用户已经在首页，再次点击首页按钮不会创建新的首页实例
                                launchSingleTop = true
                            }
                        } else {
                            // 其他页面：只导航，不清楚历史
                            navController.navigate(screen.route) {
                                launchSingleTop = true
                                // 当导航到已经存在于栈中的页面时，恢复该页面之前的状态
                                // 例如，用户在 "任务详情" 页滚动到了某个位置，离开后再回来，会恢复到之前的滚动位置
                                // 这与 saveState = true 配合使用
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    }
}