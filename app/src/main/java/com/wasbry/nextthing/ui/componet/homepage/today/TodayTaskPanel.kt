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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.wasbry.nextthing.ui.componet.common.TaskItem
import com.wasbry.nextthing.viewmodel.personalTime.PersonalTimeViewModel
import com.wasbry.nextthing.viewmodel.todoTask.TodoTaskViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.runtime.getValue // 关键导入！
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wasbry.nextthing.database.model.TodoTask

/**
 * 今日任务的实时记录面板
 * */
@SuppressLint("UnusedBoxWithConstraintsScope")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TodayTaskPanel(
    todayTasks: List<TodoTask>,
    modifier: Modifier
) {

    val TAG = "TodayTaskPanel"

    BoxWithConstraints(modifier) {
        val cardWidth = maxWidth * 0.95f  // 卡片宽度占父容器95%
        val cardHeight = maxHeight * 0.8f   // 卡片高度占父容器80%

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally // 水平居中
        ) {
            Card( // 项目里面用到了很多的这个Card 布局，应当仔细去总结一下才行啊
                modifier = modifier
                    .width(cardWidth)
                    .height(cardHeight)
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
                                text = "今日任务", // 后期需要做多语言适配的
                                style = MaterialTheme.typography.titleMedium, // 这个后期需要弄一个自定义的主题，NextThingTheme
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            // 任务统计：展示未完成的任务数
                            Text(
                                text = "未完成：6",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp)) // 添加分割线

                        // 任务列表
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(todayTasks) { task ->
                                Log.d(TAG,"Task = ${task}")
                                TaskItem(task)
                            }
                        }
                    }
                    // 任务详情页的跳转入口
                    Text(
                        text = "任务详情",
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally) // 这个是拿来干嘛的，规定字体内容在TextView 里面的布局位置哇
                            .padding(bottom = 16.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium

                    )
                }
            }
        }
    }

}