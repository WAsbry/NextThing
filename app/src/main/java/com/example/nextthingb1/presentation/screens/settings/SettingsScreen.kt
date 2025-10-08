package com.example.nextthingb1.presentation.screens.settings

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage

import com.example.nextthingb1.presentation.theme.*

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateToUserInfo: () -> Unit = {}
) {
    val uiState: SettingsUiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            // 用户信息卡片
            UserInfoCard(
                username = uiState.username,
                avatarUri = uiState.avatarUri,
                usageDays = uiState.usageDays,
                onClick = onNavigateToUserInfo
            )
        }

        items(uiState.settingSections) { section ->
            SettingsSection(
                section = section,
                uiState = uiState,
                onSettingClick = { setting -> viewModel.onSettingClick(setting) }
            )
        }
    }
}

@Composable
private fun UserInfoCard(
    username: String,
    avatarUri: Uri?,
    usageDays: Int,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 头像 - 支持显示实际图片
            if (avatarUri != null) {
                AsyncImage(
                    model = avatarUri,
                    contentDescription = "用户头像",
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "👤",
                        fontSize = 30.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 用户信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = username,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "使用天数：$usageDays 天",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }

            // 右箭头
            Text(
                text = "›",
                fontSize = 24.sp,
                color = TextMuted,
                fontWeight = FontWeight.Light
            )
        }
    }
}

@Composable
private fun SettingsSection(
    section: SettingSection,
    uiState: SettingsUiState,
    onSettingClick: (SettingItem) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column {
            section.items.forEachIndexed { index, item ->
                SettingItemRow(
                    item = item,
                    uiState = uiState,
                    onClick = { onSettingClick(item) },
                    showDivider = index < section.items.size - 1
                )
            }
        }
    }
}

@Composable
private fun SettingItemRow(
    item: SettingItem,
    uiState: SettingsUiState,
    onClick: () -> Unit,
    showDivider: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(item.color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = item.icon,
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // 标题和描述
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            item.subtitle?.let { subtitle ->
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 右侧内容
        when (item.type) {
            SettingType.SWITCH -> {
                Switch(
                    checked = when (item.id) {
                        "location_enhancement" -> uiState.locationEnhancementEnabled
                        "geofence" -> uiState.geofenceEnabled
                        else -> item.isEnabled
                    },
                    onCheckedChange = {
                        onClick()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Primary,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFE0E0E0),
                        uncheckedBorderColor = Color(0xFFE0E0E0)
                    )
                )
            }
            SettingType.ARROW -> {
                Text(
                    text = "›",
                    fontSize = 24.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Light
                )
            }
            SettingType.TEXT -> {
                item.value?.let { value ->
                    Text(
                        text = value,
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }

    if (showDivider) {
        HorizontalDivider(
            modifier = Modifier.padding(start = 70.dp, end = 16.dp),
            color = Color(0xFFF0F0F0),
            thickness = 1.dp
        )
    }
}

// 数据类
data class SettingSection(
    val title: String? = null,
    val items: List<SettingItem>
)

data class SettingItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val icon: String,
    val color: Color,
    val type: SettingType,
    val isEnabled: Boolean = false,
    val value: String? = null
)

enum class SettingType {
    SWITCH,
    ARROW,
    TEXT
} 