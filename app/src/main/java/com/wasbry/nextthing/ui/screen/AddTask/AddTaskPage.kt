package com.wasbry.nextthing.ui.screen.AddTask

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable // 正确的导入
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.wasbry.nextthing.R
import com.wasbry.nextthing.ui.screen.AddTask.component.TopTabBar

/**
 * 新建任务页面，把这个项目弄得更加专业点，要有商业化的感觉噻
 * */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AddTaskPage(navController: NavController) {
    val tabItems = listOf("健身", "工作", "生活", "娱乐")
    var selectedTabIndex by remember { mutableStateOf(0) } // 索引，可变且记住状态

    val selectedTextColor: Color = Color.Gray // 选中后文字的颜色
    val unselectedTextColor: Color = Color.Black // 未选中文字的颜色
    val selectedIndicatorColor: Color = Color(0xFF89E5ED) // 选中后下划线的颜色
    val unselectedIndicatorColor: Color = Color.Transparent // 未选中后下划线的颜色

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部导航栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.mipmap.icon_back),
                contentDescription = "返回",
                modifier = Modifier
                    .size(48.dp)
                    .padding(start = 16.dp)
                    .clickable { navController.navigateUp() }
            )
            // Tab栏（未选中标签改为清晰可见的灰色）
            TopTabBar(
                tabItems = tabItems,
                selectedIndex = selectedTabIndex,
                onTabSelected = { selectedTabIndex = it },
                selectedTextColor = selectedTextColor,
                unselectedTextColor = unselectedTextColor,
                selectedIndicatorColor = selectedIndicatorColor,
                unselectedIndicatorColor = unselectedIndicatorColor
            )
        }

        // 内容区域
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "当前选中: ${tabItems[selectedTabIndex]}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}