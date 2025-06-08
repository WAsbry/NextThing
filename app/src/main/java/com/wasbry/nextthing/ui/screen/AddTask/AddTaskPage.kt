package com.wasbry.nextthing.ui.screen.AddTask

import android.R.string
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable // 正确的导入
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.wasbry.nextthing.R
import com.wasbry.nextthing.database.model.TimeType
import com.wasbry.nextthing.ui.screen.AddTask.component.addTaskDialog.AddTaskDialog
import com.wasbry.nextthing.ui.screen.AddTask.component.content.TimeTypeDisplayGrid
import com.wasbry.nextthing.ui.screen.AddTask.component.tab.TopTabBar
import com.wasbry.nextthing.viewmodel.timetype.TimeTypeViewModel
import com.wasbry.nextthing.viewmodel.todoTask.TodoTaskViewModel
import kotlinx.coroutines.flow.Flow

/**
 * 新建任务页面，把这个项目弄得更加专业点，要有商业化的感觉噻
 * */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AddTaskPage(
    navController: NavController,
    timeTypeViewModel: TimeTypeViewModel,
    todoTaskViewModel: TodoTaskViewModel
) {
    // 定义分类标签和对应的数据库分类名
    val tabItems = listOf("健身", "工作", "生活", "娱乐")
    val categoryKeys = listOf("fitness", "work", "life", "entertainment")

    // 当前选中的标签索引
    var selectedTabIndex by remember { mutableStateOf(0) }
    // 当前选中的分类
    val currentCategory by remember { derivedStateOf { categoryKeys[selectedTabIndex] } }

    // 从数据库获取当前分类的图标数据
//    val tasks by todoTaskViewModel.allTodoTasks.collectAsStateWithLifecycle(initialValue = emptyList())
    val timeTypes by timeTypeViewModel.getTimeTypesByCategory(currentCategory)
        .collectAsStateWithLifecycle(initialValue = emptyList())



    // 当前选中的图标
    var selectedIcon by remember { mutableStateOf<TimeType?>(null) }

    // ✅ 新增对话框显示状态
    var showAddTaskDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部导航栏
        Row(
            modifier = Modifier.fillMaxWidth().height(50.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 返回按钮
            Image(
                painter = painterResource(id = R.mipmap.icon_back),
                contentDescription = "返回",
                modifier = Modifier.size(48.dp).padding(start = 16.dp).clickable {
                    navController.navigateUp()
                }
            )

            // Tab栏
            TopTabBar(
                tabItems = tabItems,
                selectedIndex = selectedTabIndex,
                onTabSelected = { selectedTabIndex = it },
                // 使用主题中的文本颜色
                selectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                // 使用主题中的主色作为指示器颜色
                selectedIndicatorColor = MaterialTheme.colorScheme.primary,
                unselectedIndicatorColor = Color.Transparent
            )
        }

        // 内容区域
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            // 图标网格展示
            TimeTypeDisplayGrid(
                timeTypes = timeTypes,
                onItemClick = { clickedIcon ->
                    selectedIcon = clickedIcon // 更新选中图标
                    Log.d("SelectedIcon", "选中图标: ${clickedIcon.description},准备弹出对话框")
                    showAddTaskDialog = true   // 显示对话框
                },
                onItemLongClick = { clickIcon ->
                    Log.d("SelectedIcon","长按监听")
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    // ✅ 显示对话框
    if (showAddTaskDialog && selectedIcon != null) {
        AddTaskDialog(
            onDismiss = { showAddTaskDialog = false }, // 关闭对话框回调
            timeType = selectedIcon!!,                // 传递选中的TimeType
            todoTaskViewModel = todoTaskViewModel,
            timeTypeViewModel = timeTypeViewModel
        )
    }
}