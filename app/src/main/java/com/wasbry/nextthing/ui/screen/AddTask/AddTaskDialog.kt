package com.wasbry.nextthing.ui.screen.AddTask

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import com.wasbry.nextthing.database.model.TodoTask
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// 定义默认分类 ID
private const val DEFAULT_CATEGORY_ID = 1L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onTaskAdded: (TodoTask) -> Unit
) {
    if (isOpen) {
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var dueDateStr by remember { mutableStateOf("") }
        var categoryIdStr by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(text = "添加任务")
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("任务标题") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("任务描述") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = dueDateStr,
                        onValueChange = { dueDateStr = it },
                        label = { Text("截止时间 (yyyy-MM-dd)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = categoryIdStr,
                        onValueChange = { categoryIdStr = it },
                        label = { Text("任务分类 ID") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("取消")
                    }
                    Button(
                        onClick = {
                            Log.d("addTask","点击监听的")
                            try {
                                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val dueDate = dateFormat.parse(dueDateStr)?.time ?: Calendar.getInstance().timeInMillis
                                // 如果用户未输入分类 ID，使用默认分类 ID
                                val categoryId = categoryIdStr.toLongOrNull() ?: DEFAULT_CATEGORY_ID
                                val newTask = TodoTask(
                                    title = title,
                                    description = description,
                                    dueDate = dueDate,
                                    isCompleted = false,
                                    categoryId = categoryId
                                )
                                onTaskAdded(newTask)
                                onDismiss()
                            } catch (e: Exception) {
                                // 处理日期格式错误或分类 ID 转换错误，可添加提示信息
                            }
                        }
                    ) {
                        Text("确认")
                    }
                }
            }
        )
    }
}