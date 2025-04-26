package com.wasbry.nextthing.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wasbry.nextthing.database.TodoDatabase
import com.wasbry.nextthing.database.repository.TodoTaskRepository
import com.wasbry.nextthing.viewmodel.TodoTaskViewModel
import com.wasbry.nextthing.viewmodel.TodoTaskViewModelFactory
import com.wasbry.nextthing.ui.componet.bottombar.BottomBar
import com.wasbry.nextthing.ui.AddTask.AddTaskDialog
import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.setValue

@Composable
fun NavigationGraph(context: Context) {

    val TAG = "NavigationGraph"

    // 创建一个 NavHostController 实例，用于管理导航操作
    val navController = rememberNavController()
    // 使用 remember 和 mutableStateOf 创建一个可变状态，用于控制添加任务对话框的显示与隐藏
    var showAddTaskDialog by remember { mutableStateOf(false) }

    // 获取数据库实例
    val database = TodoDatabase.getDatabase(context)
    // 创建 TodoTaskRepository 实例
    val repository = TodoTaskRepository(database.todoTaskDao())
    // 创建 TodoTaskViewModel 实例
    val viewModel: TodoTaskViewModel = viewModel(
        factory = TodoTaskViewModelFactory(repository)
    )

    // 使用 Scaffold 组件构建应用的基本布局，包含底部导航栏
    Scaffold(
        bottomBar = {
            // 创建底部导航栏
            BottomBar(
                navController = navController,
                onAddTaskClick = { showAddTaskDialog = true }
            )
        }
    ) { innerPadding ->
        // 创建一个 Column 布局，填充整个可用空间，并应用内边距
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 使用 NavHost 组件创建导航主机，管理导航图
            NavHost(
                navController = navController,
                startDestination = Screen.HomePage.route
            ) {
                // 定义 HomePage 路由的页面内容
                composable(Screen.HomePage.route) {
                    androidx.compose.material3.Text(text = "首页")
                }
                // 定义 TaskDetail 路由的页面内容
                composable(Screen.TaskDetail.route) {
                    androidx.compose.material3.Text(text = "任务详情")
                }
                // 定义 Statistic 路由的页面内容
                composable(Screen.Statistic.route) {
                    androidx.compose.material3.Text(text = "任务统计")
                }
                // 定义 Mine 路由的页面内容
                composable(Screen.Mine.route) {
                    androidx.compose.material3.Text(text = "个人信息")
                }
            }
        }

        // 根据 showAddTaskDialog 状态显示或隐藏添加任务对话框
        AddTaskDialog(
            isOpen = showAddTaskDialog,
            onDismiss = { showAddTaskDialog = false },
            onTaskAdded = { task ->
                Log.d(TAG,"task = " + task)
                // 调用 ViewModel 的插入方法
                viewModel.insertTodoTask(task)
                showAddTaskDialog = false
            }
        )
    }
}
