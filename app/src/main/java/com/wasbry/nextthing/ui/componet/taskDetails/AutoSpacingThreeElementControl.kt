package com.wasbry.nextthing.ui.componet.taskDetails

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 月度总结选择器
 *  快捷选择：
 *      左侧箭头：跳转到上一个月
 *      右侧剪头：跳转到下一个月
 *  弹窗呼出：
 *      中间区域：点击后呼出一个自定义弹窗
 * */
@Composable
fun AutoSpacingThreeElementControl(
    text: String,
    totalWidth: Dp,
    onLeftArrowClick: () -> Unit = {},
    onRightArrowClick: () -> Unit = {}
) {
    SubcomposeLayout(modifier = Modifier.width(totalWidth)) { constraints ->
        // 1. 测量所有子元素（包括高度）
        val textPlaceable = subcompose("text") {
            Text(
                text = text,
                modifier = Modifier.padding(vertical = 4.dp), // 示例内边距
                style = MaterialTheme.typography.titleMedium, // 使用主题中的标题中号样式
                color = MaterialTheme.colorScheme.onSurface // 使用主题中的表面内容色
            )
        }.first().measure(constraints.copy(minWidth = 0, minHeight = 0)) // 允许自由测量高度

        val leftArrowPlaceable = subcompose("leftArrow") {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "左箭头",
                modifier = Modifier.clickable { onLeftArrowClick() },
                tint = MaterialTheme.colorScheme.primary // 使用主题中的主色调
            )
        }.first().measure(constraints.copy(minWidth = 0, minHeight = 0))

        val rightArrowPlaceable = subcompose("rightArrow") {
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "右箭头",
                modifier = Modifier.clickable { onRightArrowClick() },
                tint = MaterialTheme.colorScheme.primary // 使用主题中的主色调
            )
        }.first().measure(constraints.copy(minWidth = 0, minHeight = 0))

        // 2. 计算布局的高度：取所有子元素的最大高度
        val maxHeight = maxOf(
            textPlaceable.height,
            leftArrowPlaceable.height,
            rightArrowPlaceable.height
        )

        // 3. 计算剩余空间和间距（逻辑不变）
        val totalElementWidth = textPlaceable.width + leftArrowPlaceable.width + rightArrowPlaceable.width
        val remainingWidth = constraints.maxWidth - totalElementWidth
        val spacing = if (remainingWidth > 0) (remainingWidth / 4).toDp() else 0.dp

        // 4. 布局元素（注意垂直居中逻辑需基于各自的高度）
        layout(constraints.maxWidth, maxHeight) {
            // 垂直居中通用方法：(布局高度 - 子元素高度) / 2
            fun placeWithVerticalCenter(
                placeable: Placeable,
                x: Int,
                yOffset: Int = 0
            ) {
                val y = (maxHeight - placeable.height) / 2 + yOffset
                placeable.placeRelative(x, y)
            }

            // 放置左箭头
            val leftArrowX = spacing.roundToPx()
            placeWithVerticalCenter(leftArrowPlaceable, leftArrowX)

            // 放置文本
            val textX = leftArrowX + leftArrowPlaceable.width + spacing.roundToPx()
            placeWithVerticalCenter(textPlaceable, textX)

            // 放置右箭头
            val rightArrowX = textX + textPlaceable.width + spacing.roundToPx()
            placeWithVerticalCenter(rightArrowPlaceable, rightArrowX)
        }
    }
}