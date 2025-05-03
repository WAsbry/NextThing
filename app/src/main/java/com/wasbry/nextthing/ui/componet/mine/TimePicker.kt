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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 时间选择器的控件
 * */
@Composable
fun TimePicker(
    selectedTime: String,
    onTimeSelected: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val allTimes = mutableListOf<String>()
    for (hour in 0..23) {
        for (minute in 0..59) {
            allTimes.add(String.format(Locale.getDefault(), "%02d:%02d", hour, minute))
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
                    // 添加调试日志
                    println("TimePicker: Clicked to expand, isExpanded = $isExpanded")
                },
            textAlign = TextAlign.Center
        )
        if (isExpanded) {
            // 添加调试日志
            println("TimePicker: isExpanded is true, showing time list")
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(allTimes) { time ->
                    Text(
                        text = time,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clickable(role = Role.Button) {
                                onTimeSelected(time)
                                isExpanded = false
                                // 添加调试日志
                                println("TimePicker: Time selected, isExpanded = $isExpanded")
                            },
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}