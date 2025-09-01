package com.example.nextthingb1.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.nextthingb1.presentation.theme.*

@Composable
fun ManualLocationDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (latitude: Double, longitude: Double, locationName: String) -> Unit
) {
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var locationName by remember { mutableStateOf("") }
    
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
                            painter = painterResource(id = android.R.drawable.ic_menu_edit),
                            contentDescription = "手动输入位置",
                            tint = Primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "手动输入位置",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "如果GPS获取失败，您可以手动输入位置信息：",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 位置名称输入
                    OutlinedTextField(
                        value = locationName,
                        onValueChange = { locationName = it },
                        label = { Text("位置名称") },
                        placeholder = { Text("例如：北京朝阳区") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            focusedLabelColor = Primary
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 坐标输入
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = latitude,
                            onValueChange = { latitude = it },
                            label = { Text("纬度") },
                            placeholder = { Text("39.9042") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                focusedLabelColor = Primary
                            )
                        )
                        
                        OutlinedTextField(
                            value = longitude,
                            onValueChange = { longitude = it },
                            label = { Text("经度") },
                            placeholder = { Text("116.4074") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                focusedLabelColor = Primary
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "提示：您可以在地图应用中获取精确的经纬度坐标",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // 按钮组
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("取消")
                        }
                        
                        Button(
                            onClick = {
                                val lat = latitude.toDoubleOrNull()
                                val lng = longitude.toDoubleOrNull()
                                val name = locationName.takeIf { it.isNotBlank() } ?: "手动输入位置"
                                
                                if (lat != null && lng != null && 
                                    lat >= -90 && lat <= 90 && 
                                    lng >= -180 && lng <= 180) {
                                    onConfirm(lat, lng, name)
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = latitude.toDoubleOrNull() != null && longitude.toDoubleOrNull() != null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Primary
                            )
                        ) {
                            Text(
                                text = "确认",
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
} 