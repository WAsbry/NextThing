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
import com.wasbry.nextthing.ui.bottombar.BottomBar
import com.wasbry.nextthing.ui.screen.AddTask.AddTaskDialog
import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.setValue
import com.wasbry.nextthing.database.repository.PersonalTimeRepository
import com.wasbry.nextthing.screen.TaskListScreen
import com.wasbry.nextthing.ui.screen.homepage.HomePage
import com.wasbry.nextthing.ui.screen.mine.MinePage
import com.wasbry.nextthing.viewmodel.personalTime.PersonalTimeViewModel
import com.wasbry.nextthing.viewmodel.personalTime.PersonalTimeViewModelFactory
import com.wasbry.nextthing.viewmodel.todoTask.TodoTaskViewModel
import com.wasbry.nextthing.viewmodel.todoTask.TodoTaskViewModelFactory

@Composable
fun NavigationGraph(context: Context) {
    val TAG = "NavigationGraph"
    val navController = rememberNavController()
    var showAddTaskDialog by remember { mutableStateOf(false) }

    val database = TodoDatabase.getDatabase(context)
    val repositoryTodoTask = TodoTaskRepository(database.todoTaskDao())
    val personalTimeRepository = PersonalTimeRepository(database.personalTimeDao())
    val viewModelTodoTask: TodoTaskViewModel = viewModel(
        factory = TodoTaskViewModelFactory(repositoryTodoTask)
    )

    val repositoryPersonalTime = PersonalTimeRepository(database.personalTimeDao())
    val personalTimeViewModel: PersonalTimeViewModel = viewModel(
        factory = PersonalTimeViewModelFactory(repositoryPersonalTime)
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
//                    androidx.compose.material3.Text(text = "首页")
                    HomePage()
                }
                composable(Screen.TaskDetail.route) {
                    TaskListScreen(todoTaskViewModel = viewModelTodoTask,personalTimeViewModel = personalTimeViewModel)
                }
                composable(Screen.AddTask.route) {
                    // 这里可以添加添加任务页面的逻辑
                }
                composable(Screen.Statistic.route) {
                    androidx.compose.material3.Text(text = "任务统计")
                }
                composable(Screen.Mine.route) {
                    MinePage(viewModel = personalTimeViewModel)
                }
            }
        }

        /**
         * 添加任务的对话框噻
         * */
        AddTaskDialog(
            isOpen = showAddTaskDialog,
            onDismiss = { showAddTaskDialog = false },
            onTaskAdded = { task ->
                Log.d(TAG, "task = $task")
                viewModelTodoTask.insertTodoTask(task)
                showAddTaskDialog = false
            }
            ,personalTimeRepository = personalTimeRepository
        )
    }
}