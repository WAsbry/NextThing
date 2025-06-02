package com.wasbry.nextthing.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wasbry.nextthing.ui.componet.taskDetails.AutoSpacingThreeElementControl
import com.wasbry.nextthing.ui.componet.taskDetails.EmptyContent
import com.wasbry.nextthing.ui.componet.taskDetails.TabSwitchComponent
import com.wasbry.nextthing.viewmodel.personalTime.PersonalTimeViewModel
import com.wasbry.nextthing.viewmodel.todoTask.TodoTaskViewModel
import androidx.compose.ui.res.colorResource

// 添加项目的 R 类导入 ✅
import com.wasbry.nextthing.R



@Composable
fun TaskListScreen(todoTaskViewModel: TodoTaskViewModel,personalTimeViewModel: PersonalTimeViewModel) {

    var selectedIndex by remember { mutableStateOf(0) } // Switch 选中的标签页的索引噻

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = colorResource(R.color.background_color) // 修正此行
            ) // 推荐方式
    ) {
        // 顶部栏标签栏（使用封装后的组件）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.08f),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 引入封装的标签切换组件
            TabSwitchComponent(
                selectedIndex = selectedIndex,
                onTabSelected = { selectedIndex = it },
                modifier = Modifier,
                tabWidth = 100.dp,
                tabHeight = 40.dp
            )

            // 月度选择器（保持不变）
            AutoSpacingThreeElementControl(
                text = "2025 年 6 月",
                totalWidth = 200.dp
            )
        }


        // 内容区
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // 占据剩余空间
        ) {
            when (selectedIndex) {
                0 -> EmptyContent(text = "流水")
                1 -> EmptyContent(text = "日历")
            }
        }
    }
}