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
import com.wasbry.nextthing.ui.bottombar.BottomBar
import com.wasbry.nextthing.ui.screen.AddTask.AddTaskDialog
import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.setValue
import com.wasbry.nextthing.screen.TaskListScreen

@Composable
fun NavigationGraph(context: Context) {
    val TAG = "NavigationGraph"
    val navController = rememberNavController()
    var showAddTaskDialog by remember { mutableStateOf(false) }

    val database = TodoDatabase.getDatabase(context)
    val repository = TodoTaskRepository(database.todoTaskDao())
    val viewModel: TodoTaskViewModel = viewModel(
        factory = TodoTaskViewModelFactory(repository)
    )

    Scaffold(
        bottomBar = {
            BottomBar(
                navController = navController,
                onAddTaskClick = { showAddTaskDialog = true }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.HomePage.route
            ) {
                composable(Screen.HomePage.route) {
                    androidx.compose.material3.Text(text = "首页")
                }
                composable(Screen.TaskDetail.route) {
                    TaskListScreen(viewModel = viewModel)
                }
                composable(Screen.AddTask.route) {
                    // 这里可以添加添加任务页面的逻辑
                }
                composable(Screen.Statistic.route) {
                    androidx.compose.material3.Text(text = "任务统计")
                }
                composable(Screen.Mine.route) {
                    androidx.compose.material3.Text(text = "个人信息")
                }
            }
        }

        AddTaskDialog(
            isOpen = showAddTaskDialog,
            onDismiss = { showAddTaskDialog = false },
            onTaskAdded = { task ->
                Log.d(TAG, "task = $task")
                viewModel.insertTodoTask(task)
                showAddTaskDialog = false
            }
        )
    }
}