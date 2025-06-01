package com.wasbry.nextthing.ui.screen.AddTask.component.addTaskDialog

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wasbry.nextthing.database.model.TaskImportance
import com.wasbry.nextthing.database.model.TaskStatus
import com.wasbry.nextthing.database.model.TimeType
import com.wasbry.nextthing.database.model.TodoTask
import com.wasbry.nextthing.viewmodel.todoTask.TodoTaskViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    timeType: TimeType,
    todoTaskViewModel: TodoTaskViewModel,
    modifier: Modifier = Modifier
) {
    // -------------------
    // 状态管理
    // -------------------
    val today = remember { LocalDate.now() }
    var selectedDate by remember { mutableStateOf(today) }
    var selectedTimeSlot by remember { mutableStateOf("默认") }
    var taskTitle by remember { mutableStateOf("") }
    var showLocationDialog by remember { mutableStateOf(false) }

    // -------------------
    // 时间段死数据
    // -------------------
    val timeSlots = listOf("默认")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
            )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // -------------------
            // 顶部行：两个下拉列表 + 位置图标 + 关闭按钮
            // -------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 日期下拉列表
                WeekDayDropdown(
                    selectedDay = selectedDate,
                    onDaySelected = { selectedDate = it },
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )

                // 时间段下拉列表
                TimeSlotDropdown(
                    selectedSlot = selectedTimeSlot,
                    slots = timeSlots,
                    onSlotSelected = { selectedTimeSlot = it },
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )

                // 位置图标（点击监听示例）
                IconButton(
                    onClick = { showLocationDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "选择位置",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // 关闭按钮
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭"
                    )
                }
            }

            // -------------------
            // 中间输入框
            // -------------------
            OutlinedTextField(
                value = taskTitle,
                onValueChange = { taskTitle = it },
                label = { Text(text = timeType.description) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                singleLine = true
            )

            // -------------------
            // 底部保存按钮
            // -------------------
            Button(
                onClick = {
                    // 保存逻辑
                    val newTask = TodoTask(
                        personalTimeId = 1L,
                        timeTypeIconPath = timeType.resPath,
                        title = timeType.description,
                        description = taskTitle,
                        duration = 15,
                        importance = TaskImportance.UNIMPORTANT_NOT_URGENT,
                        madeDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                        madeTime = Calendar.getInstance().time,
                        status = TaskStatus.INCOMPLETE
                    )
                    todoTaskViewModel.insertTodoTask(newTask)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存任务", fontWeight = FontWeight.Medium)
            }
        }
    }
}