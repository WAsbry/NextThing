package com.wasbry.nextthing.ui.componet.common

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wasbry.nextthing.database.model.TaskStatus
import com.wasbry.nextthing.database.model.TodoTask
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun TaskItem(
    task: TodoTask,
    onCompleted: () -> Unit,
    onAbandoned: () -> Unit,
    onPostponed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val buttonWidth = 60.dp // 增加按钮宽度以提高可点击性
    val totalButtonWidth = buttonWidth * 3 // 总宽度：180dp
    val buttonWidthPx = with(density) { buttonWidth.toPx() }
    val totalButtonWidthPx = with(density) { totalButtonWidth.toPx() }
    val offsetX = remember { Animatable(0f) } // 控制Card的水平偏移量
    val coroutineScope = rememberCoroutineScope()

    // 用于检测滑动方向的变量
    var lastDragDelta by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // 固定在右侧的三个按钮（不随Card滑动）
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .align(Alignment.CenterEnd)
        ) {
            // 完成按钮（最左侧，靠近卡片）
            Box(
                modifier = Modifier
                    .width(buttonWidth)
                    .fillMaxHeight()
                    .clickable {
                        coroutineScope.launch {
                            onCompleted()
                            offsetX.animateTo(0f) // 点击后隐藏按钮
                        }
                    }
                    .background(Color(0xFF4CAF50)) // 绿色
            ) {
                Icon(
                    Icons.Default.Done,
                    contentDescription = "完成",
                    tint = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // 放弃按钮（中间）
            Box(
                modifier = Modifier
                    .width(buttonWidth)
                    .fillMaxHeight()
                    .clickable {
                        coroutineScope.launch {
                            onAbandoned()
                            offsetX.animateTo(0f)
                        }
                    }
                    .background(Color(0xFFF44336)) // 红色
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "放弃",
                    tint = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // 顺延按钮（最右侧）
            Box(
                modifier = Modifier
                    .width(buttonWidth)
                    .fillMaxHeight()
                    .clickable {
                        coroutineScope.launch {
                            onPostponed()
                            offsetX.animateTo(0f)
                        }
                    }
                    .background(Color(0xFFFFC107)) // 黄色
            ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = "顺延",
                    tint = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        // 任务卡片（可滑动）
        Card(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) } // 向左滑动（负数为左）
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(12.dp),
                    // 只在卡片右侧显示阴影，增强层次感
                    ambientColor = if (offsetX.value < 0) Color.Black.copy(alpha = 0.2f) else Color.Transparent,
                    spotColor = if (offsetX.value < 0) Color.Black.copy(alpha = 0.2f) else Color.Transparent
                )
                .draggable(
                    state = rememberDraggableState { delta ->
                        lastDragDelta = delta
                        coroutineScope.launch {
                            // 限制最大滑动距离
                            val newOffset = (offsetX.value + delta).coerceIn(-totalButtonWidthPx, 0f)
                            offsetX.snapTo(newOffset)
                        }
                    },
                    orientation = Orientation.Horizontal,
                    onDragStarted = {
                        // 拖动开始时的逻辑
                    },
                    onDragStopped = { velocity ->
                        coroutineScope.launch {
                            // 滑动停止时自动回弹到完全隐藏或完全显示
                            val target = if (offsetX.value <= -totalButtonWidthPx / 2 || velocity < -500) {
                                -totalButtonWidthPx // 滑动超过一半或速度足够快，完全显示按钮
                            } else {
                                0f // 否则隐藏按钮
                            }

                            offsetX.animateTo(
                                targetValue = target,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        }
                    }
                ),
            elevation = CardDefaults.cardElevation(0.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            // 卡片内容
            Row(modifier = Modifier.fillMaxSize()) {
                // 图标区域
                Box(
                    modifier = Modifier
                        .fillMaxHeight() // 高度与文字区域一致
                        .weight(1f) // 宽度占满剩余空间
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    GlideImage(
                        resourceName = task.timeTypeIconPath,
                        contentDescription = "任务图标",
                        modifier = Modifier
                            .fillMaxSize() // 填充整个图标区域
                            .padding(0.dp,3.dp,0.dp,3.dp)
                    )
                }

                // 文本内容区域
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(16.dp, 12.dp)
                        .weight(4f) // 文字区域占2份，图标区域占1份（可调整比例）
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 右侧指示器（可选）
                if (task.status == TaskStatus.INCOMPLETE) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}