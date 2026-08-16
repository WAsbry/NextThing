package com.nextthing.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
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
import com.nextthing.app.domain.model.*
import com.nextthing.app.presentation.theme.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun TaskItemCard(
    task: Task,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isNextThing: Boolean = false,
    showSwipeActions: Boolean = false,
    onToggleStatus: (() -> Unit)? = null,
    onPostpone: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    onStartFocus: (() -> Unit)? = null,
    useRoundedCorners: Boolean = true,
    elevation: androidx.compose.ui.unit.Dp = 2.dp
) {
    // 添加日志打印任务信息
    timber.log.Timber.tag("TodayScreenRender").d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    timber.log.Timber.tag("TodayScreenRender").d("【首页】渲染任务卡片")
    timber.log.Timber.tag("TodayScreenRender").d("  任务ID: ${task.id}")
    timber.log.Timber.tag("TodayScreenRender").d("  标题: ${task.title}")
    timber.log.Timber.tag("TodayScreenRender").d("  描述: ${task.description}")
    timber.log.Timber.tag("TodayScreenRender").d("  分类: ${task.category.name}")
    timber.log.Timber.tag("TodayScreenRender").d("  重要程度: ${task.importanceUrgency?.displayName ?: "null"}")
    timber.log.Timber.tag("TodayScreenRender").d("  截止时间: ${task.dueDate}")
    timber.log.Timber.tag("TodayScreenRender").d("  位置: ${task.locationInfo?.locationName ?: "null"}")
    timber.log.Timber.tag("TodayScreenRender").d("  状态: ${task.status}")
    timber.log.Timber.tag("TodayScreenRender").d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

    val accentColor = if (isNextThing) Primary else parseCategoryColor(task.category)
    val statusText = when (task.status) {
        TaskStatus.COMPLETED -> "完成"
        TaskStatus.DELAYED -> "延期"
        TaskStatus.OVERDUE -> "逾期"
        TaskStatus.CANCELLED -> "放弃"
        TaskStatus.PENDING -> "待办"
    }
    val timeText = task.dueDate?.let { dueDate ->
        if (dueDate.toLocalDate() == LocalDateTime.now().toLocalDate()) {
            "今天 ${dueDate.format(DateTimeFormatter.ofPattern("HH:mm"))}"
        } else {
            dueDate.format(DateTimeFormatter.ofPattern("MM月dd日 HH:mm"))
        }
    } ?: "未设置时间"

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .heightIn(min = 112.dp, max = 176.dp),
        shape = if (useRoundedCorners) RoundedCornerShape(8.dp) else RoundedCornerShape(0.dp),
        border = BorderStroke(1.dp, Color(0x6618202C)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(accentColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFEAF4FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = task.category.displayName.take(1),
                            color = Primary,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = task.title,
                        modifier = Modifier.weight(1f),
                        color = if (task.status == TaskStatus.COMPLETED) TextMuted else Color(0xFF16181D),
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (task.status == TaskStatus.COMPLETED) TextDecoration.LineThrough else null,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = statusText,
                        modifier = Modifier
                            .wrapContentWidth()
                            .align(Alignment.Top),
                        color = Color(0xFF475467),
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                        maxLines = 1
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(19.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(task.category.displayName, color = Color(0xFF667085), fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                    Text("·", color = Color(0xFF98A2B3), fontSize = 11.sp, lineHeight = 16.sp)
                    Text(timeText, color = if (task.status == TaskStatus.COMPLETED) Color(0xFF34C759) else Color(0xFFDC2626), fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text("·", color = Color(0xFF98A2B3), fontSize = 11.sp, lineHeight = 16.sp)
                    Surface(shape = RoundedCornerShape(5.dp), color = getImportanceBackgroundColor(task.importanceUrgency)) {
                        Text(
                            text = getCompactImportanceText(task.importanceUrgency),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = getImportanceTextColor(task.importanceUrgency),
                            fontSize = 10.sp,
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                    if (isNextThing) {
                        Surface(shape = RoundedCornerShape(5.dp), color = Color(0xFFEAF4FF)) {
                            Text(
                                text = "NextThing",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = Primary,
                                fontSize = 10.sp,
                                lineHeight = 15.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }

                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF667085),
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = task.locationInfo?.locationName ?: "未设置地点",
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF98A2B3),
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (task.repeatFrequency.type == RepeatFrequencyType.NONE) "单次任务" else getRepeatDisplayText(task.repeatFrequency),
                        color = Color(0xFF667085),
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private fun parseCategoryColor(category: Category): Color = try {
    Color(android.graphics.Color.parseColor(category.colorHex))
} catch (_: IllegalArgumentException) {
    Color(0xFF0A84FF)
}

private fun getCompactImportanceText(importanceUrgency: TaskImportanceUrgency?): String = when (importanceUrgency) {
    TaskImportanceUrgency.IMPORTANT_URGENT -> "重要紧急"
    TaskImportanceUrgency.IMPORTANT_NOT_URGENT -> "重要不紧急"
    TaskImportanceUrgency.NOT_IMPORTANT_URGENT -> "紧急"
    TaskImportanceUrgency.NOT_IMPORTANT_NOT_URGENT -> "普通"
    null -> "未设置"
}

// 辅助函数
private fun getCategoryColors(category: Category): List<Color> {
    // 从category.colorHex解析颜色
    val baseColor = try {
        Color(android.graphics.Color.parseColor(category.colorHex))
    } catch (e: Exception) {
        Color(0xFF9E9E9E) // 默认灰色
    }
    return listOf(baseColor.copy(alpha = 0.15f), baseColor.copy(alpha = 0.25f))
}

private fun getCategoryBorderColor(category: Category): Color {
    // 从category.colorHex解析颜色
    val baseColor = try {
        Color(android.graphics.Color.parseColor(category.colorHex))
    } catch (e: Exception) {
        Color(0xFF9E9E9E) // 默认灰色
    }
    return baseColor.copy(alpha = 0.3f)
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

@Composable @ReadOnlyComposable
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
        null -> "未设置"
    }
}

@Composable @ReadOnlyComposable
private fun getImportanceBackgroundColor(importanceUrgency: TaskImportanceUrgency?): Color {
    return when (importanceUrgency) {
        TaskImportanceUrgency.IMPORTANT_URGENT -> Danger.copy(alpha = 0.1f)
        TaskImportanceUrgency.IMPORTANT_NOT_URGENT -> Warning.copy(alpha = 0.1f)
        TaskImportanceUrgency.NOT_IMPORTANT_URGENT -> Primary.copy(alpha = 0.1f)
        TaskImportanceUrgency.NOT_IMPORTANT_NOT_URGENT -> Success.copy(alpha = 0.1f)
        null -> Color(0xFFF5F5F5)
    }
}

@Composable @ReadOnlyComposable
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

@Composable @ReadOnlyComposable
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
