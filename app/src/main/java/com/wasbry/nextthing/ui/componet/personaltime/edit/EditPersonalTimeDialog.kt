package com.wasbry.nextthing.ui.componet.personaltime.edit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wasbry.nextthing.database.model.PersonalTime
import com.wasbry.nextthing.ui.componet.mine.TimePicker
import com.wasbry.nextthing.ui.componet.personaltime.IconSelector

// 编辑个人时间的弹窗
@Composable
fun EditPersonalTimeDialog(
    personalTime: PersonalTime,
    onDismiss: () -> Unit,
    onSave: (PersonalTime) -> Unit
) {
    var timeDescription by remember { mutableStateOf(personalTime.timeDescription) }
    var startTime by remember { mutableStateOf(personalTime.startTime) }
    var endTime by remember { mutableStateOf(personalTime.endTime) }
    var selfControlDegree by remember { mutableStateOf(personalTime.selfControlDegree) }
    var timeValue by remember { mutableStateOf(personalTime.timeValue) }
    var iconPath by remember { mutableStateOf(personalTime.iconPath) }

    var showSelfControlDegreeDropdown by remember { mutableStateOf(false) }
    var showTimeValueDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "编辑个人时间")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = timeDescription,
                    onValueChange = { timeDescription = it },
                    label = { Text("时间段描述") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "开始时间")
                TimePicker(
                    selectedTime = startTime,
                    onTimeSelected = { startTime = it },
                    isStartTimePicker = true,
                    startSelectedTime = startTime
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "结束时间")
                TimePicker(
                    selectedTime = endTime,
                    onTimeSelected = { endTime = it },
                    isStartTimePicker = false,
                    startSelectedTime = startTime
                )
                Spacer(modifier = Modifier.height(16.dp))
                Box {
                    OutlinedTextField(
                        value = selfControlDegree.toString(),
                        onValueChange = { },
                        label = { Text("时间自主程度") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = {
                            androidx.compose.material3.IconButton(onClick = { showSelfControlDegreeDropdown = true }) {
                                androidx.compose.material3.Icon(
                                    androidx.compose.material.icons.Icons.Default.KeyboardArrowDown,
                                    contentDescription = null
                                )
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = showSelfControlDegreeDropdown,
                        onDismissRequest = { showSelfControlDegreeDropdown = false }
                    ) {
                        for (degree in 1..3) {
                            DropdownMenuItem(
                                text = { Text(degree.toString()) },
                                onClick = {
                                    selfControlDegree = degree
                                    showSelfControlDegreeDropdown = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Box {
                    OutlinedTextField(
                        value = timeValue.toString(),
                        onValueChange = { },
                        label = { Text("时间价值") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = {
                            androidx.compose.material3.IconButton(onClick = { showTimeValueDropdown = true }) {
                                androidx.compose.material3.Icon(
                                    androidx.compose.material.icons.Icons.Default.KeyboardArrowDown,
                                    contentDescription = null
                                )
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = showTimeValueDropdown,
                        onDismissRequest = { showTimeValueDropdown = false }
                    ) {
                        for (value in 1..3) {
                            DropdownMenuItem(
                                text = { Text(value.toString()) },
                                onClick = {
                                    timeValue = value
                                    showTimeValueDropdown = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                IconSelector(
                    iconPath = iconPath,
                    onIconPathChange = { iconPath = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val updatedPersonalTime = PersonalTime(
                    id = personalTime.id,
                    timeDescription = timeDescription,
                    startTime = startTime,
                    endTime = endTime,
                    selfControlDegree = selfControlDegree,
                    timeValue = timeValue,
                    iconPath = iconPath
                )
                onSave(updatedPersonalTime)
                onDismiss()
            }) {
                Text("保存")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
