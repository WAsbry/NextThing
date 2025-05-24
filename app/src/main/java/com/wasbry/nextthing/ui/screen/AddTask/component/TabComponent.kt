package com.wasbry.nextthing.ui.screen.AddTask.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 带有底部指示器的Tab组件
 * @param tabItems Tab标签列表
 * @param initialSelectedIndex 初始选中的Tab索引
 * @param tabHeight Tab栏高度
 * @param indicatorHeight 指示器高度
 * @param selectedColor 选中状态的文本和指示器颜色
 * @param unselectedColor 未选中状态的文本颜色
 * @param content 每个Tab对应的内容区域
 */
@Composable
fun TopTabBar(
    tabItems: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    tabWidth: Dp = 80.dp,
    selectedTextColor: Color, // 选中后文字的颜色
    unselectedTextColor: Color, // 未选中文字的颜色
    selectedIndicatorColor: Color, // 选中后下划线的颜色
    unselectedIndicatorColor: Color // 未选中后下划线的颜色
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color.White),
        horizontalArrangement = Arrangement.Center
    ) {
        tabItems.forEachIndexed { index, tabName ->
            TabItem(
                text = tabName,
                isSelected = index == selectedIndex,
                onTabClick = { onTabSelected(index) },
                modifier = Modifier.width(tabWidth),
                selectedTextColor = selectedTextColor, // 选中后文字的颜色
                unselectedTextColor = unselectedTextColor, // 未选中文字的颜色
                selectedIndicatorColor = selectedIndicatorColor, // 选中后下划线的颜色
                unselectedIndicatorColor = unselectedIndicatorColor // 未选中后下划线的颜色
            )
        }
    }
}
@Composable
private fun TabItem(
    text: String,
    isSelected: Boolean,
    onTabClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedTextColor: Color, // 选中后文字的颜色
    unselectedTextColor: Color, // 未选中文字的颜色
    selectedIndicatorColor: Color, // 选中后下划线的颜色
    unselectedIndicatorColor: Color // 未选中后下划线的颜色
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable { onTabClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(top = 12.dp),
            fontSize = 18.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) selectedTextColor else unselectedTextColor // 确保未选中时文字可见
        )

        Box(
            modifier = Modifier
                .width(32.dp)
                .height(2.dp)
                .background(if (isSelected) selectedIndicatorColor else unselectedIndicatorColor)
        )
    }
}