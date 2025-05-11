package com.wasbry.nextthing.screen

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.wasbry.nextthing.ui.screen.taskDetail.AbandonedTasksList
import com.wasbry.nextthing.ui.screen.taskDetail.CompletedTaskList
import com.wasbry.nextthing.ui.screen.taskDetail.InCompletedTaskList
import com.wasbry.nextthing.ui.screen.taskDetail.PostponedTasksList
import com.wasbry.nextthing.viewmodel.personalTime.PersonalTimeViewModel
import com.wasbry.nextthing.viewmodel.todoTask.TodoTaskViewModel

@Composable
fun TaskListScreen(todoTaskViewModel: TodoTaskViewModel,personalTimeViewModel: PersonalTimeViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        InCompletedTaskList(todoTaskViewModel,personalTimeViewModel)
        CompletedTaskList(todoTaskViewModel,personalTimeViewModel)
        PostponedTasksList(todoTaskViewModel,personalTimeViewModel)
        AbandonedTasksList(todoTaskViewModel,personalTimeViewModel)
    }
}