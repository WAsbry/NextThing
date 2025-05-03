package com.wasbry.nextthing.ui.componet.mine

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wasbry.nextthing.database.model.PersonalTime
import com.wasbry.nextthing.viewmodel.personalTime.PersonalTimeViewModel
import java.util.Locale

@Composable
fun AddPersonalTimeDialog(
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    timeDescription: String,
    onTimeDescriptionChange: (String) -> Unit,
    startTime: String,
    onStartTimeChange: (String) -> Unit,
    endTime: String,
    onEndTimeChange: (String) -> Unit,
    selfControlDegree: Int,
    onSelfControlDegreeChange: (Int) -> Unit,
    timeValue: Int,
    onTimeValueChange: (Int) -> Unit,
    iconPath: String,
    onIconPathChange: (String) -> Unit
) {
    var showSelfControlDegreeDropdown by remember { mutableStateOf(false) }
    var showTimeValueDropdown by remember { mutableStateOf(false) }
    var showIconPathDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "添加个人时间")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = timeDescription,
                    onValueChange = onTimeDescriptionChange,
                    label = { Text("时间段描述") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "开始时间")
                TimePicker(
                    selectedTime = startTime,
                    onTimeSelected = onStartTimeChange
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "结束时间")
                TimePicker(
                    selectedTime = endTime,
                    onTimeSelected = onEndTimeChange
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
                                    onSelfControlDegreeChange(degree)
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
                                    onTimeValueChange(value)
                                    showTimeValueDropdown = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Box {
                    OutlinedTextField(
                        value = iconPath,
                        onValueChange = onIconPathChange,
                        label = { Text("Icon 路径") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = {
                            androidx.compose.material3.IconButton(onClick = { showIconPathDropdown = true }) {
                                androidx.compose.material3.Icon(
                                    androidx.compose.material.icons.Icons.Default.KeyboardArrowDown,
                                    contentDescription = null
                                )
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = showIconPathDropdown,
                        onDismissRequest = { showIconPathDropdown = false }
                    ) {
                        // 这里需要替换为实际的 Icon 路径列表
                        val iconPaths = listOf("icon1", "icon2", "icon3")
                        iconPaths.forEach { path ->
                            DropdownMenuItem(
                                text = { Text(path) },
                                onClick = {
                                    onIconPathChange(path)
                                    showIconPathDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onAdd) {
                Text("添加")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}