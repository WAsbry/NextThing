package com.wasbry.nextthing.ui.componet.mine

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import java.util.Calendar
import java.util.Locale

// 生成从 00:00 到 23:59 每隔 15 分钟的完整时间列表
private val allTimes: List<String> by lazy {
    val times = mutableListOf<String>()
    for (hour in 0..23) {
        for (minute in 0 until 60 step 15) {
            times.add(String.format(Locale.getDefault(), "%02d:%02d", hour, minute))
        }
    }
    times
}


@Composable
fun TimePicker(
    selectedTime: String,
    onTimeSelected: (String) -> Unit,
    isStartTimePicker: Boolean,
    startSelectedTime: String
) {
    var isExpanded by remember { mutableStateOf(false) }
    val currentTime = Calendar.getInstance()
    val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
    val currentMinute = currentTime.get(Calendar.MINUTE)

    // 根据是否是开始时间选择器和当前时间或已选开始时间裁剪时间列表
    val displayTimes by remember(isStartTimePicker, startSelectedTime, currentHour, currentMinute) {
        derivedStateOf {
            val startIndex = if (isStartTimePicker) {
                val next15MinuteTime = calculateNext15MinuteTime(currentHour, currentMinute)
                allTimes.indexOfFirst { it >= next15MinuteTime }
            } else {
                allTimes.indexOfFirst { it > startSelectedTime }
            }
            if (startIndex != -1) {
                allTimes.subList(startIndex, allTimes.size)
            } else {
                emptyList()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = selectedTime,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(48.dp)
                .clickable {
                    isExpanded = true
                },
            textAlign = TextAlign.Center
        )
        if (isExpanded) {
            if (displayTimes.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(displayTimes) { time ->
                        Text(
                            text = time,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .clickable(role = Role.Button) {
                                    onTimeSelected(time)
                                    isExpanded = false
                                },
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Text(
                    text = "没有可用的时间选项",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun calculateNext15MinuteTime(hour: Int, minute: Int): String {
    val baseMinute = (minute / 15) * 15
    val newMinute = if (baseMinute == minute) baseMinute + 15 else baseMinute + 15
    val newHour = (hour + (newMinute / 60)) % 24
    val adjustedMinute = newMinute % 60
    return String.format(Locale.getDefault(), "%02d:%02d", newHour, adjustedMinute)
}