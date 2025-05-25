package com.wasbry.nextthing.ui.screen.AddTask.component.content

import android.R.attr.onClick
import android.R.id.selectedIcon
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wasbry.nextthing.database.model.TimeType
import com.wasbry.nextthing.ui.screen.AddTask.component.model.TimeIconModel

/**
 * 时间类别的网格布局
 * */
// 图标展示网格组件（修改为接收TimeType数据）
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimeTypeDisplayGrid(
    timeTypes: List<TimeType>,
    onItemClick: (TimeType) -> Unit,
    onItemLongClick: (TimeType) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LazyVerticalGrid(
        modifier = modifier,
        columns = GridCells.Fixed(4),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(timeTypes) { timeType ->
            Box(
                modifier = Modifier
                    .combinedClickable(
                        onClick = { onItemClick(timeType) },
                        onLongClick = { onItemLongClick(timeType) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 加载图标资源
                    val iconResId = context.resources.getIdentifier(
                        timeType.resPath,
                        "mipmap",
                        context.packageName
                    )

                    Image(
                        painter = painterResource(id = iconResId),
                        contentDescription = timeType.description,
                        modifier = Modifier.size(48.dp),
                    )

                    Text(
                        text = timeType.description,
                        fontSize = 12.sp,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}