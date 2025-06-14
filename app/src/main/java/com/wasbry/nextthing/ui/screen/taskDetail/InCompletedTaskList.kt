package com.wasbry.nextthing.ui.screen.taskDetail

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wasbry.nextthing.viewmodel.todoTask.TodoTaskViewModel
import kotlinx.coroutines.launch

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.wasbry.nextthing.viewmodel.personalTime.PersonalTimeViewModel

/**
 * 展示所有的任务噻
 * */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun InCompletedTaskList(todoTaskViewModel: TodoTaskViewModel,personalTimeViewModel: PersonalTimeViewModel) {
    val tasks by todoTaskViewModel.getIncompleteTodoTasks.collectAsStateWithLifecycle(initialValue = emptyList())


    var expanded by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp,
            pressedElevation = 12.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            // 标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "未完成任务",
                    style = MaterialTheme.typography.titleMedium
                )

                AnimatedContent(targetState = expanded) { isExpanded ->
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Divider(modifier = Modifier.padding(horizontal = 24.dp))

            // 展开/折叠的列表
            AnimatedVisibility(visible = expanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .padding(24.dp)
                ) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(tasks) { task ->

                            // 为每个任务的 personTimeId 获取对应的 PersonalTime
                            val personalTime by personalTimeViewModel.getPersonalTimeByPersonTimeId(task.personalTimeId).collectAsState()

                            // 仅在 personalTime 变化时执行一次
                            LaunchedEffect(personalTime) {
                                Log.d("taskDisplay", "数据更新，准备渲染: $personalTime")
                            }

                            TaskItem(
                                task = task,
                                personalTime = personalTime,
                                onCompleted = {
                                    coroutineScope.launch {
                                        todoTaskViewModel.markTaskAsCompleted(task)
                                    }
                                },
                                onAbandoned = {
                                    coroutineScope.launch {
                                        todoTaskViewModel.markTaskAsAbandoned(task)
                                    }
                                },
                                onPostponed = {
                                    coroutineScope.launch {
                                        todoTaskViewModel.markTaskAsPostponed(task)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}