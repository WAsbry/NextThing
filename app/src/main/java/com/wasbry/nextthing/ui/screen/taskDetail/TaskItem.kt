package com.wasbry.nextthing.ui.screen.taskDetail

import android.content.Context
import android.content.res.Resources
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.wasbry.nextthing.database.model.PersonalTime
import com.wasbry.nextthing.database.model.TodoTask
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun TaskItem(
    task: TodoTask,
    personalTime: PersonalTime,
    onCompleted: () -> Unit,
    onAbandoned: () -> Unit,
    onPostponed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val buttonWidth = 60.dp
    val buttonWidthPx = with(density) { buttonWidth.toPx() }
    val totalButtonWidthPx = buttonWidthPx * 3
    val offsetX = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current // 获取全局context 属性噻

    val dragState = rememberDraggableState { delta ->
        coroutineScope.launch {
            offsetX.snapTo((offsetX.value + delta).coerceIn(-totalButtonWidthPx, 0f))
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .draggable(
                state = dragState,
                orientation = Orientation.Horizontal,
                onDragStopped = { velocity ->
                    coroutineScope.launch {
                        val target = if (abs(velocity) > 660f) {
                            if (velocity < 0) -totalButtonWidthPx else 0f
                        } else {
                            if (offsetX.value < -totalButtonWidthPx / 2) -totalButtonWidthPx else 0f
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
            )
    ) {
        // 背景按钮（从右到左排列）
        Row(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset((offsetX.value + totalButtonWidthPx).roundToInt(), 0) } // 修正偏移量
        ) {
            Spacer(modifier = Modifier.width(buttonWidth * 3)) // 留出按钮空间

            // 完成按钮（最左侧，显示在最右边）
            Box(
                modifier = Modifier
                    .width(buttonWidth)
                    .fillMaxHeight()
                    .clickable {
                        coroutineScope.launch {
                            onCompleted()
                            offsetX.animateTo(0f)
                        }
                    }
                    .background(Color.Red)
            ) {
                Icon(
                    imageVector = Icons.Default.Done,
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
                    .background(Color.Red)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "放弃",
                    tint = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // 顺延按钮（最右侧，显示在最左边）
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
                    .background(MaterialTheme.colorScheme.secondary)
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "顺延",
                    tint = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        // 任务内容卡片
        Card(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) },
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row (modifier = Modifier.padding(16.dp)) {
                val iconResId = getDrawableResourceId(context, personalTime.iconPath)
                Icon(
                    painter = painterResource(id = iconResId),
                    contentDescription = null,
                    modifier = Modifier.width(24.dp)
                )

                Text(
                    text = task.description ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = task.description ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

// 根据图标名称获取资源 ID（非 Compose 函数）
private fun getDrawableResourceId(context: Context, iconName: String): Int {
    // 这里假设传入的 iconName 是不带前缀和后缀的，例如 "icon_personal_time_cooking"
    // 实际使用时可能需要根据你的资源命名规则进行调整
    val packageName = context.packageName
    try {
        return context.resources.getIdentifier(
            iconName,
            "drawable",
            packageName
        )
    } catch (e: Resources.NotFoundException) {
        // 如果找不到资源，返回一个默认的资源 ID
        return android.R.drawable.ic_menu_report_image
    }
}