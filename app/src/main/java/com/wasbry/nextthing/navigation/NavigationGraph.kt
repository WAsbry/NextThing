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
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.setValue
import com.wasbry.nextthing.database.repository.PersonalTimeRepository
import com.wasbry.nextthing.database.repository.TimeTypeRepository
import com.wasbry.nextthing.screen.TaskListScreen
import com.wasbry.nextthing.ui.screen.AddTask.AddTaskPage
import com.wasbry.nextthing.ui.screen.homepage.HomePage
import com.wasbry.nextthing.ui.screen.mine.MinePage
import com.wasbry.nextthing.viewmodel.personalTime.PersonalTimeViewModel
import com.wasbry.nextthing.viewmodel.personalTime.PersonalTimeViewModelFactory
import com.wasbry.nextthing.viewmodel.timetype.TimeTypeViewModel
import com.wasbry.nextthing.viewmodel.timetype.TimeTypeViewModelFactory
import com.wasbry.nextthing.viewmodel.todoTask.TodoTaskViewModel
import com.wasbry.nextthing.viewmodel.todoTask.TodoTaskViewModelFactory

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NavigationGraph(context: Context) {
    val TAG = "NavigationGraph"
    val navController = rememberNavController()

    val database = TodoDatabase.getInstance(context)
    val repositoryTodoTask = TodoTaskRepository(database.todoTaskDao())
    val viewModelTodoTask: TodoTaskViewModel = viewModel(
        factory = TodoTaskViewModelFactory(repositoryTodoTask)
    )

    val repositoryPersonalTime = PersonalTimeRepository(database.personalTimeDao())
    val personalTimeViewModel: PersonalTimeViewModel = viewModel(
        factory = PersonalTimeViewModelFactory(repositoryPersonalTime)
    )

    // 构造相关viewmodel 层实例
    val timeTypeRepository = TimeTypeRepository(database.timeTypeDao())
    val timeTypeViewModel: TimeTypeViewModel = viewModel(
        factory = TimeTypeViewModelFactory(timeTypeRepository)
    )

    Scaffold(
        bottomBar = {
            BottomBar(
                navController = navController,
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
                    HomePage()
                }
                composable(Screen.TaskDetail.route) {
                    TaskListScreen(todoTaskViewModel = viewModelTodoTask,personalTimeViewModel = personalTimeViewModel)
                }
                composable(Screen.AddTask.route) {
                    // 这里可以添加添加任务页面的逻辑
                    AddTaskPage(navController = navController,timeTypeViewModel)
                }
                composable(Screen.Statistic.route) {
                    androidx.compose.material3.Text(text = "任务统计")
                }
                composable(Screen.Mine.route) {
                    MinePage(viewModel = personalTimeViewModel)
                }
            }
        }
    }
}