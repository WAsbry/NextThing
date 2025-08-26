package com.example.nextthingb1.presentation.screens.focus

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nextthingb1.presentation.theme.*
import kotlin.math.cos
import kotlin.math.sin

// 深色主题颜色
private val BgDark = Color(0xFF1A1A1A)
private val BgDarker = Color(0xFF0F0F0F)
private val TextLight = Color(0xFFE0E0E0)

@Composable
fun FocusScreen(
    viewModel: FocusViewModel = hiltViewModel(),
    onBackPressed: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // 头部导航
        FocusTopHeader(onBackPressed = onBackPressed)
        
        // 任务信息
        TaskInfo(taskTitle = uiState.currentTaskTitle)
        
        // 计时器
        TimerSection(
            timeRemaining = uiState.timeRemaining,
            totalTime = uiState.totalTime,
            isRunning = uiState.isRunning,
            onAdjustTime = { adjustment -> viewModel.adjustTime(adjustment) }
        )
        
        // 控制按钮
        ControlButtons(
            isRunning = uiState.isRunning,
            onPlayPause = { viewModel.toggleTimer() },
            onReset = { viewModel.resetTimer() }
        )
        
        // 专注统计
        FocusStats(
            todayFocus = uiState.todayFocusTime,
            totalFocus = uiState.totalFocusTime,
            focusCount = uiState.focusCount,
            achievements = uiState.achievements
        )
    }
    
    // 成就弹窗
    if (uiState.showAchievementPopup && uiState.latestAchievement != null) {
        AchievementPopup(
            achievement = uiState.latestAchievement!!,
            onDismiss = { viewModel.dismissAchievementPopup() }
        )
    }
}

@Composable
private fun FocusStatusBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(BgDark)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧：时间
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "2:05",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextLight
            )
        }
        
        // 右侧：电池等状态
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "5G",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextLight
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_call),
                contentDescription = "信号",
                tint = TextLight,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "36%",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextLight
            )
        }
    }
}

@Composable
private fun FocusTopHeader(onBackPressed: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackPressed,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                contentDescription = "关闭",
                tint = TextLight,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Text(
            text = "专注模式",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextLight
        )
        
        // 占位空间保持对称
        Spacer(modifier = Modifier.size(40.dp))
    }
}

@Composable
private fun TaskInfo(taskTitle: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_agenda),
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = taskTitle,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextLight
            )
        }
    }
}

@Composable
private fun TimerSection(
    timeRemaining: Int,
    totalTime: Int,
    isRunning: Boolean,
    onAdjustTime: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 计时器圆圈
        Box(
            modifier = Modifier
                .size(280.dp),
            contentAlignment = Alignment.Center
        ) {
            // 动画进度
            val animatedProgress by animateFloatAsState(
                targetValue = if (totalTime > 0) (totalTime - timeRemaining).toFloat() / totalTime else 0f,
                animationSpec = tween(durationMillis = 300)
            )
            
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val strokeWidth = 8.dp.toPx()
                val radius = (size.width - strokeWidth) / 2
                val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
                
                // 背景圆圈
                drawCircle(
                    color = Color.White.copy(alpha = 0.1f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokeWidth)
                )
                
                // 进度圆圈
                drawArc(
                    color = Primary,
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2, strokeWidth / 2),
                    size = androidx.compose.ui.geometry.Size(size.width - strokeWidth, size.height - strokeWidth)
                )
            }
            
            // 时间显示
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = formatTime(timeRemaining),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Light,
                    color = TextLight
                )
                
                if (isRunning) {
                    Text(
                        text = "专注中...",
                        fontSize = 14.sp,
                        color = Primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // 时间调节按钮
        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            TimeAdjustButton(
                text = "-5分钟",
                onClick = { onAdjustTime(-5) }
            )
            
            TimeAdjustButton(
                text = "+5分钟",
                onClick = { onAdjustTime(5) }
            )
        }
    }
}

@Composable
private fun TimeAdjustButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(alpha = 0.1f),
            contentColor = TextLight
        ),
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ControlButtons(
    isRunning: Boolean,
    onPlayPause: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        // 播放/暂停按钮
        FloatingActionButton(
            onClick = onPlayPause,
            modifier = Modifier.size(64.dp),
            containerColor = Primary,
            contentColor = Color.White
        ) {
            Icon(
                painter = painterResource(
                    id = if (isRunning) android.R.drawable.ic_media_pause 
                         else android.R.drawable.ic_media_play
                ),
                contentDescription = if (isRunning) "暂停" else "开始",
                modifier = Modifier.size(28.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(20.dp))
        
        // 重置按钮
        FloatingActionButton(
            onClick = onReset,
            modifier = Modifier.size(48.dp),
            containerColor = Color.White.copy(alpha = 0.1f),
            contentColor = TextLight
        ) {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_revert),
                contentDescription = "重置",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun FocusStats(
    todayFocus: Int,
    totalFocus: Int,
    focusCount: Int,
    achievements: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "专注统计",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextLight,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.height(120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    listOf(
                        StatItem("今日专注", "${todayFocus}分钟", "⏰"),
                        StatItem("累计专注", "${totalFocus}小时", "📊"),
                        StatItem("专注次数", "${focusCount}次", "🎯"),
                        StatItem("解锁成就", "${achievements}个", "🏆")
                    )
                ) { stat ->
                    StatCard(stat = stat)
                }
            }
        }
    }
}

@Composable
private fun StatCard(stat: StatItem) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stat.icon,
                fontSize = 20.sp
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = stat.value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
            
            Text(
                text = stat.label,
                fontSize = 12.sp,
                color = TextLight.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun AchievementPopup(
    achievement: Achievement,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = BgDark
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎉",
                    fontSize = 48.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "恭喜获得成就",
                    fontSize = 16.sp,
                    color = TextLight.copy(alpha = 0.8f)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = achievement.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = achievement.description,
                    fontSize = 14.sp,
                    color = TextLight.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("继续专注")
                }
            }
        }
    }
}

// 辅助函数
private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%02d:%02d".format(minutes, remainingSeconds)
}

// 数据类
data class StatItem(
    val label: String,
    val value: String,
    val icon: String
)

data class Achievement(
    val title: String,
    val description: String,
    val icon: String
) 