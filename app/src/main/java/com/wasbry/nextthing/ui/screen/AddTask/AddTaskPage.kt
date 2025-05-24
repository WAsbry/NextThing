package com.wasbry.nextthing.ui.screen.AddTask

import android.R.string
import android.util.Log
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.wasbry.nextthing.R
import com.wasbry.nextthing.ui.screen.AddTask.component.content.TimeTypeDisplayGrid
import com.wasbry.nextthing.ui.screen.AddTask.component.model.TimeIconModel
import com.wasbry.nextthing.ui.screen.AddTask.component.tab.TopTabBar

/**
 * 新建任务页面，把这个项目弄得更加专业点，要有商业化的感觉噻
 * */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AddTaskPage(navController: NavController) {
    // 定义分类标签和对应的时间类型
    val tabItems = listOf("健身", "工作", "生活", "娱乐")
    val tabTimeTypes = listOf("fitness", "work", "life", "entertainment")

    // 当前选中的标签索引
    var selectedTabIndex by remember { mutableStateOf(0) }
    // 当前选中的时间类型，与标签索引同步
    var selectedTimeType by remember { mutableStateOf(tabTimeTypes[0]) }


    // 后面放到主题里面的颜色区域噻
    val selectedTextColor: Color = Color.Gray // 选中后文字的颜色
    val unselectedTextColor: Color = Color.Black // 未选中文字的颜色
    val selectedIndicatorColor: Color = Color(0xFF89E5ED) // 选中后下划线的颜色
    val unselectedIndicatorColor: Color = Color.Transparent // 未选中后下划线的颜色

    // 根据当前时间类型获取图标列表
    val iconList = getIconList(selectedTimeType)
    // 当前选中的图标
    var selectedIcon by remember { mutableStateOf<TimeIconModel?>(null) }

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
                onTabSelected = {
                    // 更新选中的标签索引
                    selectedTabIndex = it
                    // 同步更新时间类型
                    selectedTimeType = tabTimeTypes[it]
                },
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

            TimeTypeDisplayGrid(
                iconList,
                onItemClick = { clickIcon ->
                    selectedIcon = clickIcon // 更新选中的图标
                    Log.d("clickIcon","选中 ${clickIcon.description}")

                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// 反射获取图标资源
@Composable
fun getIconList(iconType: String): List<TimeIconModel> {
    val context = LocalContext.current
    val resources = context.resources
    val packageName = context.packageName
    val prefix = "icon_${iconType}_"

    // 获取R.mipmap类中的所有字段（资源ID）
    val fields = R.mipmap::class.java.fields

    return fields
        .asSequence() // 使用序列优化性能
        .mapNotNull { field ->
            // 检查字段名是否以指定前缀开头
            if (field.name.startsWith(prefix)) {
                try {
                    // 获取资源ID
                    val resId = field.getInt(null)

                    // 从资源名提取描述（例如："swimming" 或 "weight_lifting"）
                    val description = field.name
                        .removePrefix(prefix)
                        .replace("_", " ")
                        .capitalizeWords() // 自定义扩展函数，将每个单词首字母大写

                    TimeIconModel(
                        iconResId = resId,
                        description = description,
                        type = iconType
                    )
                } catch (e: Exception) {
                    null // 忽略无法访问的字段
                }
            } else {
                null
            }
        }
        .toList()
}

// 扩展函数：将字符串中每个单词的首字母大写
private fun String.capitalizeWords(): String =
    split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }