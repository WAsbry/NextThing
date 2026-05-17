package com.nextthing.app.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextthing.app.data.export.ExportFormat
import com.nextthing.app.presentation.theme.*
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/**
 * 时间范围预设选项
 */
enum class DateRangePreset(val displayName: String) {
    THIS_WEEK("本周"),
    THIS_MONTH("本月"),
    CUSTOM("自定义")
}

/**
 * 导出配置底部弹窗
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportBottomSheet(
    isExporting: Boolean,
    onDismiss: () -> Unit,
    onExport: (startDate: LocalDateTime, endDate: LocalDateTime, format: ExportFormat) -> Unit
) {
    // 状态
    var selectedPreset by remember { mutableStateOf(DateRangePreset.THIS_WEEK) }
    var selectedFormat by remember { mutableStateOf(ExportFormat.EXCEL) }
    var customStartDate by remember { mutableStateOf(LocalDate.now().minusDays(7)) }
    var customEndDate by remember { mutableStateOf(LocalDate.now()) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // 计算实际的起止时间
    val (startDate, endDate) = remember(selectedPreset, customStartDate, customEndDate) {
        when (selectedPreset) {
            DateRangePreset.THIS_WEEK -> {
                val today = LocalDate.now()
                val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val sunday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                Pair(monday.atStartOfDay(), sunday.atTime(LocalTime.MAX))
            }
            DateRangePreset.THIS_MONTH -> {
                val today = LocalDate.now()
                val firstDay = today.withDayOfMonth(1)
                val lastDay = today.with(TemporalAdjusters.lastDayOfMonth())
                Pair(firstDay.atStartOfDay(), lastDay.atTime(LocalTime.MAX))
            }
            DateRangePreset.CUSTOM -> {
                Pair(customStartDate.atStartOfDay(), customEndDate.atTime(LocalTime.MAX))
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BgCard,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            Surface(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 32.dp, height = 4.dp)
                        .background(
                            color = Color(0xFFE0E0E0),
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 标题
            Text(
                text = "📤 导出任务数据",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            // ── 时间范围选择 ──
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "选择时间范围",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DateRangePreset.entries.forEach { preset ->
                        FilterChip(
                            selected = selectedPreset == preset,
                            onClick = { selectedPreset = preset },
                            label = {
                                Text(
                                    text = preset.displayName,
                                    fontSize = 13.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Primary.copy(alpha = 0.15f),
                                selectedLabelColor = Primary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 自定义日期选择器
                if (selectedPreset == DateRangePreset.CUSTOM) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 起始日期
                        DatePickerButton(
                            label = "起始日期",
                            date = customStartDate.format(dateFormatter),
                            onClick = { showStartDatePicker = true },
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = "~",
                            fontSize = 16.sp,
                            color = TextMuted
                        )

                        // 结束日期
                        DatePickerButton(
                            label = "结束日期",
                            date = customEndDate.format(dateFormatter),
                            onClick = { showEndDatePicker = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 显示选中范围
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = BgPrimary)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "📅", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${startDate.format(dateFormatter)} ~ ${endDate.toLocalDate().format(dateFormatter)}",
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                    }
                }
            }

            HorizontalDivider(color = Border)

            // ── 导出格式选择 ──
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "选择导出格式",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )

                ExportFormat.entries.forEach { format ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedFormat = format }
                            .background(
                                if (selectedFormat == format) Primary.copy(alpha = 0.08f)
                                else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedFormat == format,
                            onClick = { selectedFormat = format },
                            colors = RadioButtonDefaults.colors(selectedColor = Primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = format.displayName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                            Text(
                                text = getFormatDescription(format),
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // ── 导出按钮 ──
            Button(
                onClick = { onExport(startDate, endDate, selectedFormat) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = !isExporting,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("导出中...", fontSize = 15.sp)
                } else {
                    Text("📤 导出", fontSize = 15.sp)
                }
            }
        }
    }

    // ── DatePicker 弹窗 ──
    if (showStartDatePicker) {
        DatePickerDialog(
            initialDate = customStartDate,
            onDateSelected = { date ->
                customStartDate = date
                if (date.isAfter(customEndDate)) {
                    customEndDate = date
                }
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false }
        )
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            initialDate = customEndDate,
            onDateSelected = { date ->
                customEndDate = date
                if (date.isBefore(customStartDate)) {
                    customStartDate = date
                }
                showEndDatePicker = false
            },
            onDismiss = { showEndDatePicker = false }
        )
    }
}

/**
 * 日期选择按钮
 */
@Composable
private fun DatePickerButton(
    label: String,
    date: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = BgCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = TextMuted
            )
            Text(
                text = date,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
        }
    }
}

/**
 * 日期选择弹窗（使用 Material3 DatePicker）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val initialMillis = initialDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        onDateSelected(date)
                    }
                }
            ) {
                Text("确定", color = Primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = TextSecondary)
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            colors = DatePickerDefaults.colors(
                selectedDayContainerColor = Primary,
                todayDateBorderColor = Primary
            )
        )
    }
}

/**
 * 获取格式说明文字
 */
private fun getFormatDescription(format: ExportFormat): String {
    return when (format) {
        ExportFormat.CSV -> "通用表格格式，WPS/Excel 均可打开"
        ExportFormat.EXCEL -> "Excel 格式，支持表头样式和列宽"
        ExportFormat.MARKDOWN -> "文档格式，适合阅读和分享"
    }
}
