package com.nextthing.app.presentation.screens.repeatcustom

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextthing.app.domain.model.RepeatFrequencyType
import com.nextthing.app.R
import com.nextthing.app.presentation.theme.*

/**
 * 自定义重复频次页面
 *
 * 仿 CategoryEditScreen 风格，支持按周/按月自定义重复日期。
 *
 * @param initialType     初始模式："WEEKLY" 或 "MONTHLY"，默认 WEEKLY
 * @param initialWeekdays 初始选中的星期，逗号分隔，如 "1,3,5"
 * @param initialMonthDays 初始选中的月份日期，逗号分隔，如 "1,15"
 * @param onSave          保存回调，返回 type、weekdays、monthDays
 * @param onBackPressed   返回回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepeatCustomScreen(
    initialType: String = "WEEKLY",
    initialWeekdays: String = "",
    initialMonthDays: String = "",
    onSave: (type: RepeatFrequencyType, weekdays: Set<Int>, monthDays: Set<Int>) -> Unit,
    onBackPressed: () -> Unit
) {
    // 解析初始值
    val parsedInitialType = if (initialType == "MONTHLY") RepeatFrequencyType.MONTHLY else RepeatFrequencyType.WEEKLY
    val parsedWeekdays = if (initialWeekdays.isBlank()) emptySet()
        else initialWeekdays.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
    val parsedMonthDays = if (initialMonthDays.isBlank()) emptySet()
        else initialMonthDays.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()

    var selectedMode by remember { mutableStateOf(parsedInitialType) }
    var selectedWeekdays by remember { mutableStateOf(parsedWeekdays) }
    var selectedMonthDays by remember { mutableStateOf(parsedMonthDays) }

    // 保存时的验证错误
    var validationError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8FC))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(60.dp).background(Color.White).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF5F7FC)).clickable(onClick = onBackPressed),
                contentAlignment = Alignment.Center
            ) {
                Image(painterResource(R.drawable.icon_detail_back), "返回", Modifier.size(36.dp))
            }
            Text("自定义重复", Modifier.weight(1f), Color(0xFF0E131D), 18.sp, fontWeight = FontWeight.Bold)
            val canSave = !isSelectionEmpty(selectedMode, selectedWeekdays, selectedMonthDays)
            Text(
                "保存",
                Modifier.clickable(enabled = canSave) { onSave(selectedMode, selectedWeekdays, selectedMonthDays) },
                if (canSave) Color(0xFF1A7DFA) else Color(0xFFB6C0CE),
                18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // ── 可滚动内容区 ─────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            // ── Card 1：重复方式切换 ──────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFD6E0ED))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "重复方式",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ModeChip(
                            label = "按周重复",
                            isSelected = selectedMode == RepeatFrequencyType.WEEKLY,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedMode = RepeatFrequencyType.WEEKLY }
                        )
                        ModeChip(
                            label = "按月重复",
                            isSelected = selectedMode == RepeatFrequencyType.MONTHLY,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedMode = RepeatFrequencyType.MONTHLY }
                        )
                    }
                }
            }

            // ── Card 2：日期选择 ──────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFD6E0ED))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (selectedMode == RepeatFrequencyType.WEEKLY) "选择星期" else "选择日期",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (selectedMode == RepeatFrequencyType.WEEKLY) {
                        WeekdayGrid(
                            selectedWeekdays = selectedWeekdays,
                            onToggle = { day ->
                                selectedWeekdays = if (selectedWeekdays.contains(day))
                                    selectedWeekdays - day
                                else
                                    selectedWeekdays + day
                                validationError = null
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "可多选，每周的选中日期都会生成任务",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    } else {
                        MonthDayGrid(
                            selectedDays = selectedMonthDays,
                            onToggle = { day ->
                                selectedMonthDays = if (selectedMonthDays.contains(day))
                                    selectedMonthDays - day
                                else
                                    selectedMonthDays + day
                                validationError = null
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "29~31日在月份天数不足时，顺延至当月最后一天",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // ── Card 3：预览 ──────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFD6E0ED))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color(0x1A1A7DFA)), contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.icon_repeat_daily), null, tint = Color(0xFF1A7DFA), modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (isSelectionEmpty(selectedMode, selectedWeekdays, selectedMonthDays)) "请选择重复日期" else buildPreviewText(selectedMode, selectedWeekdays, selectedMonthDays),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isSelectionEmpty(selectedMode, selectedWeekdays, selectedMonthDays))
                            TextSecondary else Primary
                    )
                }
            }

            // ── 错误提示 ──────────────────────────────────
            if (validationError != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Danger.copy(alpha = 0.1f))
                ) {
                    Text(
                        text = validationError!!,
                        color = Danger,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

// ── 私有组件 ─────────────────────────────────────────────────

@Composable
private fun ModeChip(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF1A7DFA) else Color(0xFFF5F7FC))
            .then(
                if (!isSelected) Modifier.border(1.dp, Color(0xFFD6E0ED), RoundedCornerShape(8.dp))
                else Modifier
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) Color.White else TextPrimary
        )
    }
}

@Composable
private fun WeekdayGrid(
    selectedWeekdays: Set<Int>,
    onToggle: (Int) -> Unit
) {
    val labels = listOf("一", "二", "三", "四", "五", "六", "日")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        labels.forEachIndexed { index, label ->
            val day = index + 1
            val isSelected = selectedWeekdays.contains(day)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) Color(0xFF1A7DFA) else Color.White)
                    .then(
                        if (!isSelected) Modifier.border(1.dp, Color(0xFFD6E0ED), RoundedCornerShape(8.dp))
                        else Modifier
                    )
                    .clickable { onToggle(day) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) Color.White else if (day >= 6) Color(0xFFF2383D) else Color(0xFF0F1726)
                )
            }
        }
    }
}

@Composable
private fun MonthDayGrid(
    selectedDays: Set<Int>,
    onToggle: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        (0..4).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                (0..6).forEach { col ->
                    val day = row * 7 + col + 1
                    if (day <= 31) {
                        val isSelected = selectedDays.contains(day)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF1A7DFA) else Color.White)
                                .then(
                                    if (!isSelected) Modifier.border(1.dp, Color(0xFFD6E0ED), RoundedCornerShape(8.dp))
                                    else Modifier
                                )
                                .clickable { onToggle(day) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.toString(),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) Color.White else TextPrimary,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // 占位，使最后一行对齐
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
        }
    }
}

// ── 工具函数 ─────────────────────────────────────────────────

private fun isSelectionEmpty(
    mode: RepeatFrequencyType,
    weekdays: Set<Int>,
    monthDays: Set<Int>
): Boolean = when (mode) {
    RepeatFrequencyType.WEEKLY -> weekdays.isEmpty()
    RepeatFrequencyType.MONTHLY -> monthDays.isEmpty()
    else -> false
}

private fun buildPreviewText(
    mode: RepeatFrequencyType,
    weekdays: Set<Int>,
    monthDays: Set<Int>
): String {
    return when (mode) {
        RepeatFrequencyType.WEEKLY -> {
            if (weekdays.isEmpty()) {
                "尚未选择星期"
            } else {
                val names = weekdays.sorted().map { day ->
                    when (day) {
                        1 -> "一"; 2 -> "二"; 3 -> "三"; 4 -> "四"
                        5 -> "五"; 6 -> "六"; 7 -> "日"; else -> ""
                    }
                }
                "每周 ${names.joinToString("、")}"
            }
        }
        RepeatFrequencyType.MONTHLY -> {
            if (monthDays.isEmpty()) {
                "尚未选择日期"
            } else {
                "每月 ${monthDays.sorted().joinToString("、") { "${it}日" }}"
            }
        }
        else -> ""
    }
}
