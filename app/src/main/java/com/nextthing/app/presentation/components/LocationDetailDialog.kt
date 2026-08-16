package com.nextthing.app.presentation.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nextthing.app.domain.model.LocationInfo
import com.nextthing.app.presentation.theme.BgCard
import com.nextthing.app.presentation.theme.Border
import com.nextthing.app.presentation.theme.Danger
import com.nextthing.app.presentation.theme.Primary
import com.nextthing.app.presentation.theme.TextMuted
import com.nextthing.app.presentation.theme.TextPrimary
import com.nextthing.app.presentation.theme.TextSecondary
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun LocationDetailDialog(
    isVisible: Boolean,
    location: LocationInfo?,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit
) {
    if (!isVisible) return

    val context = LocalContext.current
    var copiedAddress by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = BgCard),
            border = BorderStroke(1.dp, Border),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                DialogHeader(onDismiss = onDismiss)

                Spacer(modifier = Modifier.height(22.dp))

                if (!errorMessage.isNullOrBlank()) {
                    LocationError(message = errorMessage)
                    Spacer(modifier = Modifier.height(18.dp))
                }

                if (location != null) {
                    AddressSection(
                        address = location.address.ifBlank { location.locationName },
                        onCopy = {
                            copyToClipboard(context, location.address.ifBlank { location.locationName })
                            copiedAddress = true
                        }
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    CoordinateSection(
                        longitude = location.longitude,
                        latitude = location.latitude
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "精度 ${formatAccuracy(location.accuracy)} · ${formatUpdatedAt(location.updatedAt)}",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                } else {
                    EmptyLocationState()
                }

                if (copiedAddress) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "地址已复制", color = Primary, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onRefresh,
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("正在定位…", color = Color.White, fontWeight = FontWeight.SemiBold)
                    } else {
                        Text("刷新定位", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogHeader(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(25.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "当前位置",
            modifier = Modifier.weight(1f),
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "关闭",
                tint = TextSecondary,
                modifier = Modifier.size(21.dp)
            )
        }
    }
}

@Composable
private fun AddressSection(address: String, onCopy: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "定位地址",
                modifier = Modifier.weight(1f),
                color = TextMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            IconButton(onClick = onCopy, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "复制完整地址",
                    tint = Primary,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = address,
            color = TextPrimary,
            fontSize = 17.sp,
            lineHeight = 25.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CoordinateSection(longitude: Double, latitude: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CoordinateItem(label = "经度", value = String.format("%.6f", longitude), modifier = Modifier.weight(1f))
        CoordinateItem(label = "纬度", value = String.format("%.6f", latitude), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CoordinateItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(text = label, color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LocationError(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Danger.copy(alpha = 0.07f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = message, color = Danger, fontSize = 13.sp, lineHeight = 19.sp)
    }
}

@Composable
private fun EmptyLocationState() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(42.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = "暂无位置信息", color = TextSecondary, fontSize = 14.sp)
    }
}

private fun copyToClipboard(context: Context, address: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("定位地址", address))
}

private fun formatAccuracy(accuracy: Float?): String = accuracy?.let { String.format("%.0fm", it) } ?: "未知"

private fun formatUpdatedAt(updatedAt: LocalDateTime): String {
    val elapsed = Duration.between(updatedAt, LocalDateTime.now())
    return when {
        elapsed.isNegative || elapsed.seconds < 60 -> "刚刚更新"
        elapsed.toMinutes() < 60 -> "${elapsed.toMinutes()} 分钟前更新"
        else -> updatedAt.format(DateTimeFormatter.ofPattern("HH:mm 更新"))
    }
}
