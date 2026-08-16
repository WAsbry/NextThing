package com.nextthing.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun PostponeReasonDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    if (!isVisible) return

    var reason by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Card(
            modifier = Modifier.size(340.dp, 220.dp),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFED7AA)),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 14.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "延期任务",
                    modifier = Modifier.offset(x = 20.dp, y = 16.dp),
                    color = Color(0xFF0F172A),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = "任务将延期至明天，请输入延期原因",
                    modifier = Modifier.offset(x = 20.dp, y = 50.dp),
                    color = Color(0xFF64748B),
                    fontSize = 14.sp,
                    maxLines = 1
                )
                BasicTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    modifier = Modifier
                        .offset(x = 20.dp, y = 78.dp)
                        .size(300.dp, 84.dp)
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color(0xFF0F172A),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    ),
                    singleLine = false,
                    maxLines = 4,
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.TopStart) {
                            if (reason.isEmpty()) {
                                Text(
                                    text = "例如：时间冲突、准备不足…",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    maxLines = 2
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                Text(
                    text = "取消",
                    modifier = Modifier
                        .offset(x = 184.dp, y = 170.dp)
                        .size(44.dp, 38.dp)
                        .clickable {
                            reason = ""
                            onDismiss()
                        }
                        .wrapContentSize(Alignment.Center),
                    color = Color(0xFF64748B),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Button(
                    onClick = {
                        onConfirm(reason.trim())
                        reason = ""
                    },
                    modifier = Modifier
                        .offset(x = 238.dp, y = 170.dp)
                        .size(82.dp, 38.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF59E0B),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "确认延期",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
