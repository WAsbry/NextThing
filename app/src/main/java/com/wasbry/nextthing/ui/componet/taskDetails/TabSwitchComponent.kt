package com.wasbry.nextthing.ui.componet.taskDetails

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TabSwitchComponent(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    tabWidth: Dp = 100.dp,
    tabHeight: Dp = 48.dp,
    // 使用主题中的主色和表面色
    selectedColor: Color = MaterialTheme.colorScheme.primary,
    unselectedColor: Color = MaterialTheme.colorScheme.surface
) {
    Surface(
        modifier = modifier
            .width(tabWidth)
            .height(tabHeight),
        shape = RoundedCornerShape(15.dp), // 保留原有圆角设计
        shadowElevation = 4.dp,
        color = unselectedColor // 外层默认未选中颜色（主题表面色）
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 流水按钮
            TabButton(
                text = "流水",
                isSelected = selectedIndex == 0,
                onClick = { onTabSelected(0) },
                // 使用主题颜色
                selectedColor = selectedColor,
                unselectedColor = unselectedColor,
                modifier = Modifier.weight(1f) // ✅ 正确：在 Row 中分配权重
            )

            // 日历按钮
            TabButton(
                text = "日历",
                isSelected = selectedIndex == 1,
                onClick = { onTabSelected(1) },
                // 使用主题颜色
                selectedColor = selectedColor,
                unselectedColor = unselectedColor,
                modifier = Modifier.weight(1f) // ✅ 正确：在 Row 中分配权重
            )
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    selectedColor: Color,
    unselectedColor: Color,
    modifier: Modifier = Modifier // 新增 modifier 参数接收权重
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(if (isSelected) selectedColor else unselectedColor)
            .clickable { onClick() }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            // 使用主题中的内容色（选中时用主色，未选中时用表面变体色）
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}