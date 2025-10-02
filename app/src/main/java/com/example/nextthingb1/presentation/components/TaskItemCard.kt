package com.example.nextthingb1.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nextthingb1.domain.model.*
import com.example.nextthingb1.presentation.theme.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun TaskItemCard(
    task: Task,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showSwipeActions: Boolean = false,
    onToggleStatus: (() -> Unit)? = null,
    onPostpone: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    onStartFocus: (() -> Unit)? = null,
    useRoundedCorners: Boolean = true,
    elevation: androidx.compose.ui.unit.Dp = 2.dp
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = if (useRoundedCorners) RoundedCornerShape(12.dp) else RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = elevation
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
        ) {
            // 顶部区域：分类图标、标题和重要程度
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // 分类图标
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.CenterVertically)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = getCategoryColors(task.category)
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = getCategoryBorderColor(task.category),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getCategoryEmoji(task.category),
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 标题
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (task.status == TaskStatus.COMPLETED) TextMuted else TextPrimary,
                        textDecoration = if (task.status == TaskStatus.COMPLETED) TextDecoration.LineThrough else null,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // 分类标签
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = task.category.displayName,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 重要程度标签
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = getImportanceBackgroundColor(task.importanceUrgency),
                ) {
                    Text(
                        text = getImportanceDisplayText(task.importanceUrgency),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = getImportanceTextColor(task.importanceUrgency),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // 任务内容（描述）
            if (task.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = task.description,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            // 底部区域：地理位置、重复频率
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧：地理位置
                Text(
                    text = "📍 ${task.locationInfo?.locationName ?: "未设置"}",
                    fontSize = 11.sp,
                    color = if (task.locationInfo != null) TextPrimary else TextMuted,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.width(8.dp))

                // 右侧：重复频率（限制最大宽度）
                Box(
                    modifier = Modifier.widthIn(max = 120.dp) // 限制最大宽度
                ) {
                    if (task.repeatFrequency.type != RepeatFrequencyType.NONE) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "🔄 ${getRepeatDisplayText(task.repeatFrequency)}",
                                fontSize = 10.sp,
                                color = Primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Text(
                            text = "🔄 单次",
                            fontSize = 11.sp,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// 辅助函数
private fun getCategoryColors(category: TaskCategory): List<Color> {
    return when (category) {
        TaskCategory.WORK -> listOf(Color(0xFF42A5F5).copy(alpha = 0.15f), Color(0xFF42A5F5).copy(alpha = 0.25f))
        TaskCategory.STUDY -> listOf(Color(0xFFAB47BC).copy(alpha = 0.15f), Color(0xFFAB47BC).copy(alpha = 0.25f))
        TaskCategory.LIFE -> listOf(Color(0xFF66BB6A).copy(alpha = 0.15f), Color(0xFF66BB6A).copy(alpha = 0.25f))
        TaskCategory.HEALTH -> listOf(Color(0xFFE91E63).copy(alpha = 0.15f), Color(0xFFE91E63).copy(alpha = 0.25f))
        TaskCategory.PERSONAL -> listOf(Color(0xFFFF9800).copy(alpha = 0.15f), Color(0xFFFF9800).copy(alpha = 0.25f))
        TaskCategory.OTHER -> listOf(Color(0xFF9E9E9E).copy(alpha = 0.15f), Color(0xFF9E9E9E).copy(alpha = 0.25f))
    }
}

private fun getCategoryBorderColor(category: TaskCategory): Color {
    return when (category) {
        TaskCategory.WORK -> Color(0xFF42A5F5).copy(alpha = 0.3f)
        TaskCategory.STUDY -> Color(0xFFAB47BC).copy(alpha = 0.3f)
        TaskCategory.LIFE -> Color(0xFF66BB6A).copy(alpha = 0.3f)
        TaskCategory.HEALTH -> Color(0xFFE91E63).copy(alpha = 0.3f)
        TaskCategory.PERSONAL -> Color(0xFFFF9800).copy(alpha = 0.3f)
        TaskCategory.OTHER -> Color(0xFF9E9E9E).copy(alpha = 0.3f)
    }
}

private fun getCategoryEmoji(category: TaskCategory): String {
    return when (category) {
        TaskCategory.WORK -> "💼"
        TaskCategory.STUDY -> "📚"
        TaskCategory.LIFE -> "🏠"
        TaskCategory.HEALTH -> "❤️"
        TaskCategory.PERSONAL -> "👤"
        TaskCategory.OTHER -> "📋"
    }
}

private fun getStatusEmoji(status: TaskStatus): String {
    return when (status) {
        TaskStatus.PENDING -> "📝"
        TaskStatus.COMPLETED -> "✓"
        TaskStatus.DELAYED -> "⏱️"
        TaskStatus.OVERDUE -> "⚠️"
        TaskStatus.CANCELLED -> "❌"
    }
}

private fun getStatusBackgroundColor(status: TaskStatus): Color {
    return when (status) {
        TaskStatus.PENDING -> Color(0xFFF5F5F5)
        TaskStatus.COMPLETED -> Success.copy(alpha = 0.1f)
        TaskStatus.DELAYED -> Color(0xFFFFA726).copy(alpha = 0.1f)  // 橙黄色
        TaskStatus.OVERDUE -> Danger.copy(alpha = 0.1f)
        TaskStatus.CANCELLED -> TextMuted.copy(alpha = 0.1f)
    }
}

private fun getImportanceDisplayText(importanceUrgency: TaskImportanceUrgency?): String {
    return when (importanceUrgency) {
        TaskImportanceUrgency.IMPORTANT_URGENT -> "重要且紧急"
        TaskImportanceUrgency.IMPORTANT_NOT_URGENT -> "重要但不紧急"
        TaskImportanceUrgency.NOT_IMPORTANT_URGENT -> "不重要但紧急"
        TaskImportanceUrgency.NOT_IMPORTANT_NOT_URGENT -> "不重要且不紧急"
        null -> "不重要且不紧急"
    }
}

private fun getImportanceBackgroundColor(importanceUrgency: TaskImportanceUrgency?): Color {
    return when (importanceUrgency) {
        TaskImportanceUrgency.IMPORTANT_URGENT -> Danger.copy(alpha = 0.1f)
        TaskImportanceUrgency.IMPORTANT_NOT_URGENT -> Warning.copy(alpha = 0.1f)
        TaskImportanceUrgency.NOT_IMPORTANT_URGENT -> Primary.copy(alpha = 0.1f)
        TaskImportanceUrgency.NOT_IMPORTANT_NOT_URGENT -> Success.copy(alpha = 0.1f)
        null -> Color(0xFFF5F5F5)
    }
}

private fun getImportanceTextColor(importanceUrgency: TaskImportanceUrgency?): Color {
    return when (importanceUrgency) {
        TaskImportanceUrgency.IMPORTANT_URGENT -> Danger
        TaskImportanceUrgency.IMPORTANT_NOT_URGENT -> Warning
        TaskImportanceUrgency.NOT_IMPORTANT_URGENT -> Primary
        TaskImportanceUrgency.NOT_IMPORTANT_NOT_URGENT -> Success
        null -> TextMuted
    }
}

private fun getRepeatDisplayText(repeatFrequency: RepeatFrequency): String {
    return when (repeatFrequency.type) {
        RepeatFrequencyType.DAILY -> "每日"
        RepeatFrequencyType.WEEKLY -> {
            if (repeatFrequency.weekdays.isEmpty()) {
                "每周"
            } else {
                val dayNames = repeatFrequency.weekdays.sorted().take(2).map { day ->
                    when (day) {
                        1 -> "一"
                        2 -> "二"
                        3 -> "三"
                        4 -> "四"
                        5 -> "五"
                        6 -> "六"
                        7 -> "日"
                        else -> ""
                    }
                }
                if (repeatFrequency.weekdays.size > 2) {
                    "周${dayNames.joinToString("")}等"
                } else {
                    "周${dayNames.joinToString("")}"
                }
            }
        }
        RepeatFrequencyType.MONTHLY -> {
            if (repeatFrequency.monthDays.isEmpty()) {
                "每月"
            } else {
                val days = repeatFrequency.monthDays.sorted().take(2)
                if (repeatFrequency.monthDays.size > 2) {
                    "${days.joinToString("、") { "${it}日" }}等"
                } else {
                    "${days.joinToString("、") { "${it}日" }}"
                }
            }
        }
        else -> "单次"
    }
}

private fun formatRelativeTime(dateTime: LocalDateTime): String {
    val now = LocalDateTime.now()
    val days = ChronoUnit.DAYS.between(dateTime.toLocalDate(), now.toLocalDate())

    return when {
        days == 0L -> "今天"
        days == 1L -> "昨天"
        days < 7L -> "${days}天前"
        days < 30L -> "${days / 7}周前"
        else -> dateTime.format(DateTimeFormatter.ofPattern("MM月dd日"))
    }
}

data class TimeInfo(val text: String, val color: Color)

private fun formatTimeInfo(dueDate: LocalDateTime, status: TaskStatus): TimeInfo {
    val now = LocalDateTime.now()
    val days = ChronoUnit.DAYS.between(now.toLocalDate(), dueDate.toLocalDate())
    val hours = ChronoUnit.HOURS.between(now, dueDate)

    return when {
        status == TaskStatus.COMPLETED -> TimeInfo(
            "已于 ${formatRelativeTime(dueDate)} 完成",
            Success
        )
        status == TaskStatus.OVERDUE -> TimeInfo(
            "已逾期 ${kotlin.math.abs(days)}天",
            Danger
        )
        days < 0 -> TimeInfo(
            "逾期 ${kotlin.math.abs(days)}天",
            Danger
        )
        days == 0L -> {
            when {
                hours <= 1 -> TimeInfo("1小时内到期", Danger)
                hours <= 6 -> TimeInfo("${hours}小时内到期", Warning)
                else -> TimeInfo("今天到期", Warning)
            }
        }
        days == 1L -> TimeInfo("明天到期", Warning.copy(alpha = 0.7f))
        days < 7L -> TimeInfo("${days}天后到期", TextMuted)
        else -> TimeInfo(
            "到期：${dueDate.format(DateTimeFormatter.ofPattern("MM月dd日"))}",
            TextMuted
        )
    }
}