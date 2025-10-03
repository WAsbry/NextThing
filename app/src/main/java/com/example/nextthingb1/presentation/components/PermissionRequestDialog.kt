package com.example.nextthingb1.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.nextthingb1.util.MissingPermission

/**
 * 权限请求对话框
 * 用于引导用户授予应用必需的权限
 */
@Composable
fun PermissionRequestDialog(
    missingPermissions: List<MissingPermission>,
    onRequestNotification: () -> Unit,
    onRequestExactAlarm: () -> Unit,
    onDismiss: () -> Unit,
    showDismissButton: Boolean = true
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 标题图标
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color(0xFFFF9800)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 标题
                Text(
                    text = "需要授予权限",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 说明文字
                Text(
                    text = "为了正常使用任务提醒功能，需要您授予以下权限：",
                    fontSize = 14.sp,
                    color = Color(0xFF757575),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 权限列表
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    missingPermissions.forEach { permission ->
                        PermissionItem(
                            permission = permission,
                            onRequest = {
                                when (permission.name) {
                                    "通知权限" -> onRequestNotification()
                                    "精确闹钟权限" -> onRequestExactAlarm()
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showDismissButton) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("稍后再说")
                        }
                    }

                    // 根据第一个缺失的权限决定主按钮行为
                    val firstPermission = missingPermissions.firstOrNull()
                    if (firstPermission != null) {
                        Button(
                            onClick = {
                                when (firstPermission.name) {
                                    "通知权限" -> onRequestNotification()
                                    "精确闹钟权限" -> onRequestExactAlarm()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2196F3)
                            )
                        ) {
                            Text(
                                text = if (firstPermission.canRequestDirectly) "授予权限" else "前往设置",
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 单个权限项
 */
@Composable
private fun PermissionItem(
    permission: MissingPermission,
    onRequest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = Color(0xFF2196F3)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // 文字信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = permission.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF212121)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = permission.description,
                    fontSize = 13.sp,
                    color = Color(0xFF757575),
                    lineHeight = 18.sp
                )

                if (permission.isRequired) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "必需",
                        fontSize = 11.sp,
                        color = Color(0xFFFF5252),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * 简化版权限请求对话框
 * 只显示标题和主要按钮
 */
@Composable
fun SimplePermissionDialog(
    title: String,
    message: String,
    confirmText: String = "前往设置",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFFF9800)
            )
        },
        title = {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = message,
                fontSize = 14.sp,
                color = Color(0xFF757575)
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3)
                )
            ) {
                Text(confirmText, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("稍后再说")
            }
        },
        containerColor = Color.White
    )
}
