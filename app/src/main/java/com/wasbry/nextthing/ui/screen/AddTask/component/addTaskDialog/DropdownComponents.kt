package com.wasbry.nextthing.ui.screen.AddTask.component.addTaskDialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// -------------------
// 星期下拉列表组件
// -------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekDayDropdown(
    selectedDay: LocalDate,
    onDaySelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val daysOfWeek = generateWeekDays(startFrom = selectedDay)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedDay.format(DateTimeFormatter.ofPattern("EEE")),
            onValueChange = {},
            label = { Text("选择日期") },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "展开菜单"
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            // ✅ 将 LazyColumn 替换为普通 Column
            Column {
                daysOfWeek.forEach { day ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                day.format(DateTimeFormatter.ofPattern("EEE")),
                                fontWeight = if (day == selectedDay) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            onDaySelected(day)
                            expanded = false
                        },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

// -------------------
// 时间段下拉列表组件
// -------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeSlotDropdown(
    selectedSlot: String,
    slots: List<String>,
    onSlotSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedSlot,
            onValueChange = {},
            label = { Text("选择时间段") },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "展开菜单"
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            // ✅ 将 LazyColumn 替换为普通 Column
            Column {
                slots.forEach { slot ->
                    DropdownMenuItem(
                        text = { Text(slot) },
                        onClick = {
                            onSlotSelected(slot)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

// -------------------
// 工具函数：生成七天日期
// -------------------
private fun generateWeekDays(startFrom: LocalDate): List<LocalDate> {
    return (0..6).map { days ->
        startFrom.plusDays(days.toLong())
    }
}