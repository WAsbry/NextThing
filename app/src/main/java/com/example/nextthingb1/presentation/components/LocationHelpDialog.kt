package com.example.nextthingb1.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.nextthingb1.presentation.theme.*

@Composable
fun LocationHelpDialog(
    isVisible: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit
) {
    if (isVisible) {
        Dialog(onDismissRequest = onDismiss) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = BgCard
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    // 标题
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_info_details),
                            contentDescription = "位置帮助",
                            tint = Warning,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "位置获取帮助",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 错误信息
                    if (!errorMessage.isNullOrBlank()) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Danger.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = errorMessage,
                                fontSize = 13.sp,
                                color = Danger,
                                modifier = Modifier.padding(12.dp),
                                lineHeight = 18.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // 建议列表
                    Text(
                        text = "获取精确位置的建议：",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val suggestions = listOf(
                        "📍 移动到室外或窗边获取更好的GPS信号",
                        "🔄 确保位置服务已在系统设置中开启",
                        "📶 检查网络连接是否正常",
                        "⏱️ 首次定位可能需要1-2分钟时间",
                        "🏢 在室内定位精度可能较低",
                        "🔋 关闭省电模式可提高定位精度"
                    )
                    
                    suggestions.forEach { suggestion ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "• ",
                                fontSize = 13.sp,
                                color = Primary
                            )
                            Text(
                                text = suggestion.substring(2), // 移除emoji
                                fontSize = 13.sp,
                                color = TextSecondary,
                                lineHeight = 18.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // 按钮组
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Primary
                            )
                        ) {
                            Text(
                                text = "打开设置",
                                fontSize = 14.sp
                            )
                        }
                        
                        Button(
                            onClick = {
                                onDismiss()
                                onRetry()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Primary
                            )
                        ) {
                            Text(
                                text = "重试获取",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "关闭",
                            color = TextMuted,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
} 