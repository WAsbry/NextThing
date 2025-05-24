package com.wasbry.nextthing.ui.screen.AddTask

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.Navigation

/**
 * 新建任务页面，把这个项目弄得更加专业点，要有商业化的感觉噻
 * */
@Composable
fun AddTaskPage(navController: NavController) {

    val tag = "AddTaskPage"

    Column(
        modifier = Modifier.fillMaxWidth() // 占满全屏
            .background(color = Color.Green) // 先设置一个背景色，慢慢来调噻
    ) {
        Text(text = "这个就是新建消息页")
        Button(onClick = {
            Log.d(tag,"点击跳转回上一个页面")
            navController.navigateUp()
        }) {
            Text(text = "返回上一级")
        }
    }
}