package com.example.nextthingb1.presentation.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nextthingb1.domain.model.LocationInfo
import com.example.nextthingb1.presentation.theme.*
import kotlinx.coroutines.delay

@Composable
fun LocationTooltip(
    location: LocationInfo?,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut(),
        modifier = modifier
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = TextPrimary.copy(alpha = 0.9f)
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                if (location != null) {
                    Text(
                        text = location.locationName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    
                    if (location.address.isNotBlank() && location.address != location.locationName) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = location.address,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 1
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "精度${String.format("%.0f", location.accuracy ?: 0f)}m",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier
                                .background(
                                    Color.White.copy(alpha = 0.2f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                        
                        Text(
                            text = "${String.format("%.4f", location.latitude)}, ${String.format("%.4f", location.longitude)}",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier
                                .background(
                                    Color.White.copy(alpha = 0.2f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Text(
                        text = "暂无位置信息",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
} 