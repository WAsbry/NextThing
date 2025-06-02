package com.wasbry.nextthing.ui.componet.homepage.today

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.wasbry.nextthing.ui.componet.common.TaskItem
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.material.color.ColorRoles
import com.wasbry.nextthing.R
import com.wasbry.nextthing.database.model.TodoTask
import com.wasbry.nextthing.viewmodel.todoTask.TodoTaskViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * 展示今日未完成的任务
 * */
@SuppressLint("UnusedBoxWithConstraintsScope")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TodayIncompleteTasksPanel(
    todayIncompleteTasks: List<TodoTask>,
    todoTaskViewModel: TodoTaskViewModel,
    modifier: Modifier
) {

    val TAG = "TodayTaskPanel"

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = colorResource(R.color.background_color))
        ,
        horizontalAlignment = Alignment.CenterHorizontally // 水平居中
    ) {
        Card( // 项目里面用到了很多的这个Card 布局，应当仔细去总结一下才行啊
            modifier = modifier
                .fillMaxSize()
                .background(color = colorResource(R.color.background_color))
                .padding(16.dp)
                .clip(MaterialTheme.shapes.medium), // 这个是什么意思
            elevation = CardDefaults.cardElevation(4.dp) // 添加阴影效果噻啊
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween // 顶部内容与底部按钮分离
            ) {
                Column {
                    // 顶部的标题栏
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Absolute.SpaceBetween, // 这几个关键属性，自己需要去总结一下
                        verticalAlignment = Alignment.CenterVertically // 这几个关键属性，自己需要去总结一下
                    ) {
                        Text(
                            text = "今日待办", // 后期需要做多语言适配的
                            style = MaterialTheme.typography.titleMedium, // 这个后期需要弄一个自定义的主题，NextThingTheme
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        // 任务统计：展示未完成的任务数
                        Text(
                            text = "未完成：${todayIncompleteTasks.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp)) // 添加分割线

                    // 任务列表
                    LazyColumn(
                        modifier = Modifier.height(360.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(todayIncompleteTasks) { task ->
                            Log.d(TAG,"Task = ${task}")
                            TaskItem(task,
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

                // 任务详情页的跳转入口
                Text(
                    text = "任务详情",
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally) // 这个是拿来干嘛的，规定字体内容在TextView 里面的布局位置哇
                        .padding(bottom = 3.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }

}