package com.wasbry.nextthing.ui.screen.AddTask

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.wasbry.nextthing.database.model.TaskImportance
import com.wasbry.nextthing.database.model.TaskStatus
import com.wasbry.nextthing.database.model.TodoTask
import com.wasbry.nextthing.database.repository.PersonalTimeRepository
import java.util.Calendar


private const val DEFAULT_CATEGORY_ID = 1L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onTaskAdded: (TodoTask) -> Unit,
    personalTimeRepository: PersonalTimeRepository
) {
    if (isOpen) {
        // 新增：个人时间段下拉菜单独立状态
        var personalTimeExpanded by remember { mutableStateOf(false) }
        // 任务重要程度选择框（独立状态 + 动态图标）
        var importanceExpanded by remember { mutableStateOf(false) } // 新增独立状态


        var selectedPersonalTime by remember { mutableStateOf<PersonalTime?>(null) }
        var personalTimeText by remember { mutableStateOf("请选择个人时间段") } // 初始提示
        var selectedPersonalTimeId by remember { mutableStateOf<Long>(1L) }

        var description by remember { mutableStateOf("") }
        var duration by remember { mutableStateOf(1) }
        var inputTextDirection by remember { mutableStateOf(duration.toString()) }
        var importance by remember { mutableStateOf(TaskImportance.UNIMPORTANT_NOT_URGENT) }

        val personalTimeList = remember { mutableStateOf<List<PersonalTime>>(emptyList()) }

        // 数据获取和日志（保持不变）
        androidx.compose.runtime.LaunchedEffect(Unit) {
            personalTimeRepository.allPersonalTime.collect { times ->
                personalTimeList.value = times
                Log.d("allPersonalTime", "获取到 ${times.size} 条个人时间段数据")
            }
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("添加任务") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // ------------------- 个人时间段选择框 -------------------
                    // 个人时间段输入框及下拉菜单
                    OutlinedTextField(
                        value = personalTimeText,
                        onValueChange = { },
                        label = { Text("个人时间段") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = {
                            androidx.compose.material3.IconButton(onClick = { personalTimeExpanded = !personalTimeExpanded }) {
                                val icon = if (personalTimeExpanded) {
                                    androidx.compose.material.icons.Icons.Default.KeyboardArrowUp
                                } else {
                                    androidx.compose.material.icons.Icons.Default.ArrowDropDown
                                }
                                androidx.compose.material3.Icon(
                                    imageVector = icon,
                                    contentDescription = if (personalTimeExpanded) "收起" else "展开"
                                )
                            }
                        }
                    )

                    DropdownMenu(
                        expanded = personalTimeExpanded,
                        onDismissRequest = { personalTimeExpanded = false },
                        modifier = Modifier.offset(y = 8.dp) // 关键：调整下拉菜单位置
                    ) {
                        personalTimeList.value.forEach { personalTimeItem ->
                            DropdownMenuItem(
                                text = { Text(personalTimeItem.timeDescription) },
                                onClick = {
                                    selectedPersonalTime = personalTimeItem
                                    selectedPersonalTimeId = personalTimeItem.id
                                    personalTimeText = personalTimeItem.timeDescription
                                    personalTimeExpanded = false
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ------------------- 任务描述输入框 -------------------
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("任务描述") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // ------------------- 持续时间输入框 -------------------
                    OutlinedTextField(
                        value = inputTextDirection,
                        onValueChange = { newInputText ->
                            inputTextDirection = newInputText
                            duration = try { newInputText.toInt() } catch (_: Exception) { 1 }
                        },
                        label = { Text("任务持续时间(min)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // ------------------- 重要程度选择框（独立状态） -------------------
                    OutlinedTextField(
                        value = importance.name,
                        onValueChange = { },
                        label = { Text("任务重要程度") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = {
                            androidx.compose.material3.IconButton(onClick = { importanceExpanded = !importanceExpanded }) {
                                // 根据状态动态切换箭头方向
                                val icon = if (importanceExpanded) {
                                    Icons.Default.KeyboardArrowUp
                                } else {
                                    Icons.Default.ArrowDropDown
                                }
                                androidx.compose.material3.Icon(
                                    imageVector = icon,
                                    contentDescription = "切换重要程度列表"
                                )
                            }
                        }
                    )

                    DropdownMenu(
                        expanded = importanceExpanded,
                        onDismissRequest = { importanceExpanded = false }
                    ) {
                        TaskImportance.values().forEach { importanceOption ->
                            DropdownMenuItem(
                                text = { Text(importanceOption.name) },
                                onClick = {
                                    importance = importanceOption
                                    importanceExpanded = false // 选择后关闭菜单
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDismiss, modifier = Modifier.padding(end = 8.dp)) {
                        Text("取消")
                    }
                    Button(onClick = {
                        // 确保 selectedPersonalTimeId 不为空（可添加非空校验）
                        val newTask = TodoTask(
                            personalTimeId = selectedPersonalTimeId,
                            description = description,
                            duration = duration,
                            importance = importance,
                            madeDate = Calendar.getInstance().time,
                            status = TaskStatus.INCOMPLETE
                        )
                        onTaskAdded(newTask)
                        onDismiss()
                    }) {
                        Text("确认")
                    }
                }
            }
        )
    }
}