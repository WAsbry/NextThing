package com.wasbry.nextthing.ui.screen.AddTask.component.content

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wasbry.nextthing.ui.screen.AddTask.component.model.TimeIconModel
/**
 * 展示时间类型的单个Icon
 * */
@Composable
fun TimeTypeDisplayGridItem(
    model: TimeIconModel,
    onItemClick: () -> Unit,
    modifier: Modifier // 整体布局的 Modifier
) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .clickable{onItemClick()},
        horizontalAlignment = Alignment.CenterHorizontally, // 水平排列：水平方向上居中
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 图片容器
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colors.primary)
        ){
            Image(
                painter = painterResource(id = model.iconResId),
                contentDescription = "时间分类子项",
                modifier = Modifier.fillMaxSize(), // 看一下这几个Max 是什么意思
                contentScale = ContentScale.Fit, // 图片缩放的
            )
        }

        // 描述文本
        Text(
            text = model.description,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.surface,
            maxLines = 1
        )
    }
}