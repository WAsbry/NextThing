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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nextthingb1.domain.model.LocationInfo
import com.example.nextthingb1.presentation.theme.*
import java.time.format.DateTimeFormatter

@Composable
fun LocationInfoCard(
    location: LocationInfo?,
    isLoading: Boolean = false,
    error: String? = null,
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = BgCard
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
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
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "当前位置",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
                
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
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
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            when {
                error != null -> {
                    Text(
                        text = error,
                        fontSize = 14.sp,
                        color = Danger
                    )
                }
                location != null -> {
                    Column {
                        Text(
                            text = location.locationName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        
                        if (location.address.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = location.address,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            LocationDetailItem(
                                label = "精度",
                                value = "${String.format("%.1f", location.accuracy ?: 0f)}m"
                            )
                            LocationDetailItem(
                                label = "更新时间",
                                value = location.updatedAt.format(DateTimeFormatter.ofPattern("HH:mm"))
                            )
                        }
                        
                        // 为天气SDK预留的位置信息
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "经度: ${String.format("%.4f", location.longitude)}",
                                fontSize = 10.sp,
                                color = TextMuted,
                                modifier = Modifier
                                    .background(BgSecondary, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                            Text(
                                text = "纬度: ${String.format("%.4f", location.latitude)}",
                                fontSize = 10.sp,
                                color = TextMuted,
                                modifier = Modifier
                                    .background(BgSecondary, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                else -> {
                    Text(
                        text = "暂无位置信息",
                        fontSize = 14.sp,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationDetailItem(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            fontSize = 10.sp,
            color = TextMuted
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
    }
} 