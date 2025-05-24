package com.wasbry.nextthing.ui.screen.AddTask.component.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wasbry.nextthing.ui.screen.AddTask.component.model.TimeIconModel

/**
 * 时间类别的网格布局
 * */
@Composable
fun TimeTypeDisplayGrid(
    iconList: List<TimeIconModel>,
    onItemClick: (TimeIconModel) -> Unit = {}, // 增加点击回调参数
    modifier: Modifier,
    columnCount: Int = 5, // 每行5 列
    verticalSpacing: Dp = 16.dp, // 行间距
    horizontalSpacing: Dp = 8.dp // 列间距
) {
    LazyVerticalGrid(
        modifier = modifier.padding(horizontal = 8.dp),
        columns = GridCells.Fixed(columnCount),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        items(iconList) { item ->
            TimeTypeDisplayGridItem(
                model = item,
                onItemClick = {onItemClick(item)},
                modifier = Modifier.fillMaxWidth() // 网格布局，占满列宽
            )
        }
    }
}