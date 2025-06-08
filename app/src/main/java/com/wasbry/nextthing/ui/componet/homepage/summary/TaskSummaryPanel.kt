package com.wasbry.nextthing.ui.componet.homepage.summary

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wasbry.nextthing.database.model.TaskSummary
import java.time.format.DateTimeFormatter

/**
 * 一周总结面板
 * */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TaskSummaryPanel(
    taskSummary: TaskSummary,
    showType: Int, // 0: 展示本周概要；1: 展示本月概要
    modifier: Modifier
) {
    val dateFormatter = DateTimeFormatter.ofPattern("MM-dd")


    // 使用 Card 组件创建一个卡片式的容器
    Card(
        modifier = modifier,
        // 设置卡片的形状为圆角，圆角半径为 12dp
        shape = RoundedCornerShape(12.dp),
        // 设置卡片的阴影效果，默认阴影高度为 8dp
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            // 设置子组件之间的垂直间距为 8dp
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (showType == 0){
                    "本周概要"
                }else{
                    "本月概要"
                },
                style = MaterialTheme.typography.titleLarge.copy( // 使用主题中的标题大样式
                    fontWeight = FontWeight.Bold // 如需额外调整，可通过 copy 修改特定属性
                ),
                color = MaterialTheme.colorScheme.onSurface // 使用主题中的表面内容色
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                // 设置子组件在水平方向上均匀分布
                horizontalArrangement = Arrangement.SpaceBetween,
                // 设置子组件在垂直方向上居中对齐
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 使用 Box 组件包裹日期显示区域
                Box(
                    // 设置 Box 的修饰符，包括宽度、背景颜色、形状和内边距
                    modifier = Modifier
                        .width(120.dp)
                        .background(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(4.dp))
                        .padding(4.dp)
                ) {
                    // 在 Box 内部使用 Row 组件水平排列日期文本
                    Row(
                        // 设置子组件在水平方向上均匀分布
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        // 设置子组件在垂直方向上居中对齐
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 显示开始日期
                        Text(
                            text = taskSummary.startDate.format(dateFormatter),
                            // 设置文本的样式，包括字体大小和颜色
// 替换为主题颜色
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        // 显示分隔符 " - "
                        Text(
                            text = " - ",
                            // 设置文本的样式，包括字体大小和颜色
                            // 替换为主题颜色
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        // 显示结束日期
                        Text(
                            text = taskSummary.endDate.format(dateFormatter),
                            // 设置文本的样式，包括字体大小和颜色
// 替换为主题颜色
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                // 显示本周的任务总数
                Text(
                    text = "本周任务总数 = " + taskSummary.taskTotalCount.toString(),
                    // 设置文本的样式，包括字体大小、粗细和颜色
                    // 替换为主题颜色
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            // 创建一个高度为 16dp 的空白间距
            Spacer(modifier = Modifier.height(16.dp))

            // 使用 Row 组件进行水平布局
            Row(
                // 设置 Row 的修饰符，填充最大宽度并设置高度为 32dp
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                // 设置子组件在水平方向上均匀分布
                horizontalArrangement = Arrangement.SpaceBetween,
                // 设置子组件在垂直方向上居中对齐
                verticalAlignment = Alignment.CenterVertically
            ) {

                // 显示本周的已完成的任务总数
                Text(
                    text = "已经完成 = " + taskSummary.taskCompletedTotalCount.toString(),
                    // 设置文本的样式，包括字体大小和颜色
                    style = TextStyle(
                        fontSize = 16.sp,
                        color = Color(0xFF666666)
                    )
                )

                // 显示本周的未完成的任务总数
                Text(
                    text = "未完成 = " + taskSummary.taskIncompleteTotalCount.toString(),
                    // 设置文本的样式，包括字体大小和颜色
                    style = TextStyle(
                        fontSize = 16.sp,
                        color = Color(0xFF666666)
                    )
                )
                // 显示本周的延期的任务总数
                Text(
                    text = "延期 = " + taskSummary.taskPostponedTotalCount.toString(),
                    // 设置文本的样式，包括字体大小和颜色
                    style = TextStyle(
                        fontSize = 16.sp,
                        color = Color(0xFF666666)
                    )
                )

                // 显示本周的放弃任务数
                Text(
                    text = "放弃 = " + taskSummary.taskAbandonedTotalCount.toString(),
                    // 设置文本的样式，包括字体大小和颜色
                    style = TextStyle(
                        fontSize = 16.sp,
                        color = Color(0xFF666666)
                    )
                )
            }
        }
    }
}