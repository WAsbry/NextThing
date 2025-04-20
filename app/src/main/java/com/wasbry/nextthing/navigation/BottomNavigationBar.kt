// 声明该文件所在的包名
package com.wasbry.nextthing.navigation

// 导入用于创建底部导航栏的组件
import androidx.compose.material.BottomNavigation
// 导入底部导航栏中的每个菜单项组件
import androidx.compose.material.BottomNavigationItem
// 导入用于显示图标的组件
import androidx.compose.material.Icon
// 导入用于显示文本的组件
import androidx.compose.material.Text
// 导入 Material Design 图标库相关类
import androidx.compose.material.icons.Icons
// 导入填充样式的首页图标
import androidx.compose.material.icons.filled.Home
// 导入填充样式的信息图标
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.MaterialTheme
// 导入 Composable 注解，用于标记这是一个可组合的函数
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
// 导入颜色类，用于设置颜色
import androidx.compose.ui.graphics.Color
// 导入导航控制器类，用于控制导航操作
import androidx.navigation.NavController
// 导入获取当前导航栈状态的函数
import androidx.navigation.compose.currentBackStackEntryAsState
import com.wasbry.nextthing.R

/**
 * 底部导航栏
 * */
// 使用 Composable 注解标记，表明这是一个可组合函数，用于构建 UI
@Composable
// 定义底部导航栏函数，接收一个 NavController 作为参数，用于控制导航
fun BottomNavigationBar(navController: NavController) {
    // 定义一个列表，包含底部导航栏的所有屏幕路由
    val items = listOf(
        // 首页路由
        Screen.HomePage to R.drawable.icon_home_page,
        // 任务详情页路由
        Screen.TaskDetail to R.drawable.icon_task_detail,
        // 添加任务页路由
        Screen.AddTask to R.drawable.icon_add_task,
        // 统计页路由
        Screen.Statistic to R.drawable.icon_statistic,
        // 我的页面路由
        Screen.Mine to R.drawable.icon_mine
    )
    // 创建底部导航栏组件
    androidx.compose.material3.NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        // 获取当前导航栈的状态
        val navBackStackEntry = navController.currentBackStackEntryAsState()
        // 获取当前所在的路由名称
        val currentRoute = navBackStackEntry.value?.destination?.route
        // 遍历所有的屏幕路由
        items.forEach { (screen : Screen, iconResId: Int) ->
            // 创建底部导航栏中的每个菜单项
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
                            {
                                popUpTo(route) {
                                    saveState = true
                                }
                            }
                        }
                        launchSingleTop = true
                        // 恢复之前保存的状态
                        restoreState = true
                    }
                }
            )
        }
    }
}    