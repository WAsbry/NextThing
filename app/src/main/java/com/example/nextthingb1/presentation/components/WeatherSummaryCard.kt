package com.example.nextthingb1.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nextthingb1.domain.model.WeatherInfo
import com.example.nextthingb1.presentation.theme.TextSecondary

/**
 * 天气概要卡片组件
 * 显示在首页进度卡片的右上角20%区域
 */
@Composable
fun WeatherSummaryCard(
    weatherInfo: WeatherInfo?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    
    // 计算尺寸：右上角区域为屏幕宽度的30%，高度为父卡片的80%
    val cardWidth = screenWidth * 0.30f
    val cardHeight = (screenHeight * 0.30f) * 0.80f // 父卡片高度的80%
    
    // 点击动画
    var isPressed by remember { mutableStateOf(false) }
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(200),
        label = "weather_card_scale"
    )
    
    // 重置动画状态
    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(200)
            isPressed = false
        }
    }
    
    Box(
        modifier = modifier
            .width(cardWidth)
            .height(cardHeight)
            .scale(animatedScale)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) {
                isPressed = true
                onClick()
            }
    ) {
        if (weatherInfo != null) {
            WeatherContent(weatherInfo = weatherInfo)
        } else {
            LoadingContent()
        }
        
        // 可点击提示箭头
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = "查看更多",
            tint = TextSecondary.copy(alpha = 0.6f),
            modifier = Modifier
                .size(8.dp)
                .align(Alignment.BottomEnd)
        )
    }
}

@Composable
private fun WeatherContent(weatherInfo: WeatherInfo) {
    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp < 360
    
    // 根据屏幕大小调整字体和图标尺寸
    val iconSize = if (isSmallScreen) 20.dp else 24.dp
    val tempTextSize = if (isSmallScreen) 15.sp else 16.sp
    val conditionTextSize = if (isSmallScreen) 11.sp else 12.sp
    val suggestionTextSize = if (isSmallScreen) 9.sp else 10.sp
    
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 天气图标和状态
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // 天气图标（使用emoji）
            Text(
                text = weatherInfo.condition.iconRes,
                fontSize = iconSize.value.sp,
                color = Color(weatherInfo.condition.color)
            )
            
            // 天气状态文字
            Text(
                text = weatherInfo.condition.displayName,
                fontSize = conditionTextSize,
                fontWeight = FontWeight.Normal,
                color = Color.White
            )
        }
        
        // 当前温度
        Text(
            text = "${weatherInfo.temperature}℃",
            fontSize = tempTextSize,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        // 生活建议（仅在有紧急建议时显示）
        weatherInfo.suggestion?.let { suggestion ->
            if (suggestion.isUrgent) {
                Text(
                    text = suggestion.message,
                    fontSize = suggestionTextSize,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp < 360
    
    val tempTextSize = if (isSmallScreen) 15.sp else 16.sp
    val conditionTextSize = if (isSmallScreen) 11.sp else 12.sp
    
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 加载状态
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = Color.White.copy(alpha = 0.7f),
                strokeWidth = 2.dp
            )
            Text(
                text = "获取中",
                fontSize = conditionTextSize,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
        
        Text(
            text = "--℃",
            fontSize = tempTextSize,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.5f)
        )
    }
} 