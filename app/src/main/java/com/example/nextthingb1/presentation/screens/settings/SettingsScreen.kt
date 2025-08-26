package com.example.nextthingb1.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

import com.example.nextthingb1.presentation.theme.*

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState: SettingsUiState by viewModel.uiState.collectAsState()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
    ) {
        item {
            // 头部导航
            SettingsTopHeader()
        }
        
        item {
            // 用户信息卡片
            UserInfoCard(
                username = uiState.username,
                usageDays = uiState.usageDays,
                isPro = uiState.isPro
            )
        }
        
        item {
            // 功能网格
            FeaturesGrid(
                features = uiState.features,
                onFeatureClick = { feature -> viewModel.onFeatureClick(feature) }
            )
        }
        
        items(uiState.settingSections) { section ->
            SettingsSection(
                section = section,
                uiState = uiState,
                onSettingClick = { setting -> viewModel.onSettingClick(setting) }
            )
        }
        
        item {
            // 版本信息
            VersionInfo(version = uiState.version)
        }
    }
}

@Composable
private fun SettingsTopHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgCard)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "NextThing",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_mylocation),
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(16.dp)
            )
        }
        
        IconButton(
            onClick = { /* TODO: 搜索功能 */ },
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(BgPrimary)
        ) {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_search),
                contentDescription = "搜索",
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun UserInfoCard(
    username: String,
    usageDays: Int,
    isPro: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF667eea), Color(0xFF764ba2))
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 用户头像
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "👤",
                        fontSize = 24.sp
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // 用户信息
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = username,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        if (isPro) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        Color.White.copy(alpha = 0.2f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "PRO",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "使用天数：$usageDays 天",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }
                
                // 设置按钮
                IconButton(
                    onClick = { /* TODO: 设置功能 */ },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_preferences),
                        contentDescription = "设置",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturesGrid(
    features: List<FeatureItem>,
    onFeatureClick: (FeatureItem) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.height(240.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(features) { feature ->
                    FeatureItem(
                        feature = feature,
                        onClick = { onFeatureClick(feature) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureItem(
    feature: FeatureItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(feature.color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = feature.icon,
                fontSize = 20.sp
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = feature.title,
            fontSize = 12.sp,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
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
        shape = RoundedCornerShape(16.dp)
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
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(item.color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = item.icon,
                fontSize = 16.sp
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // 标题和描述
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            
            item.subtitle?.let { subtitle ->
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
        
        // 右侧内容
        when (item.type) {
            SettingType.SWITCH -> {
                Switch(
                    checked = when (item.id) {
                        "location_enhancement" -> uiState.locationEnhancementEnabled
                        else -> item.isEnabled
                    },
                    onCheckedChange = { 
                        onClick()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Primary,
                        checkedTrackColor = Primary.copy(alpha = 0.5f)
                    )
                )
            }
            SettingType.ARROW -> {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_media_next),
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
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
        Divider(
            modifier = Modifier.padding(start = 60.dp, end = 16.dp),
            color = Border,
            thickness = 0.5.dp
        )
    }
}

@Composable
private fun VersionInfo(version: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "版本 $version",
            fontSize = 12.sp,
            color = TextMuted
        )
    }
}

// 数据类
data class FeatureItem(
    val id: String,
    val title: String,
    val icon: String,
    val color: Color
)

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