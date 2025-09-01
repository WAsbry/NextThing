package com.example.nextthingb1.presentation.components

import androidx.compose.foundation.background
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
import com.example.nextthingb1.domain.model.LocationInfo
import com.example.nextthingb1.presentation.theme.*
import java.time.format.DateTimeFormatter

@Composable
fun LocationDetailDialog(
    isVisible: Boolean,
    location: LocationInfo?,
    isLoading: Boolean = false,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit
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
                    // 标题栏
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = android.R.drawable.ic_menu_mylocation),
                                contentDescription = "位置",
                                tint = Primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "当前位置",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                        
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Primary
                            )
                        } else {
                            IconButton(
                                onClick = onRefresh,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = android.R.drawable.ic_menu_rotate),
                                    contentDescription = "刷新位置",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (location != null) {
                        // 主要位置信息
                        LocationInfoSection(
                            title = "位置名称",
                            content = location.locationName,
                            isPrimary = true
                        )
                        
                        if (location.address.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            LocationInfoSection(
                                title = "详细地址",
                                content = location.address
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // 坐标信息（为天气SDK准备）
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            LocationCoordinateItem(
                                label = "经度",
                                value = String.format("%.6f", location.longitude),
                                modifier = Modifier.weight(1f)
                            )
                            LocationCoordinateItem(
                                label = "纬度", 
                                value = String.format("%.6f", location.latitude),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // 技术信息
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            LocationTechItem(
                                label = "精度",
                                value = "${String.format("%.1f", location.accuracy ?: 0f)}m",
                                modifier = Modifier.weight(1f)
                            )
                            LocationTechItem(
                                label = "更新时间",
                                value = location.updatedAt.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        if (location.altitude != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LocationTechItem(
                                label = "海拔",
                                value = "${String.format("%.1f", location.altitude)}m"
                            )
                        }
                        
                    } else {
                        // 无位置信息状态
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                painter = painterResource(id = android.R.drawable.ic_menu_mylocation),
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "暂无位置信息",
                                fontSize = 14.sp,
                                color = TextMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // 关闭按钮
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Primary
                        )
                    ) {
                        Text(
                            text = "关闭",
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationInfoSection(
    title: String,
    content: String,
    isPrimary: Boolean = false
) {
    Column {
        Text(
            text = title,
            fontSize = 12.sp,
            color = TextMuted,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = content,
            fontSize = if (isPrimary) 16.sp else 14.sp,
            color = if (isPrimary) TextPrimary else TextSecondary,
            fontWeight = if (isPrimary) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun LocationCoordinateItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Primary.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = Primary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun LocationTechItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
    }
} 