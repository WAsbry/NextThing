package com.nextthing.app.presentation.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextthing.app.R
import com.nextthing.app.presentation.navigation.Screen

@Composable
fun BottomNavigationBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        BottomNavEntry(R.drawable.icon_home, "首页", Screen.Today.route, currentRoute == Screen.Today.route),
        BottomNavEntry(R.drawable.icon_task, "任务", Screen.Tasks.route, currentRoute == Screen.Tasks.route || currentRoute == Screen.TasksCalendar.route),
        BottomNavEntry(R.drawable.icon_create, "创建", Screen.CreateTask.route, currentRoute == Screen.CreateTask.route),
        BottomNavEntry(R.drawable.icon_stats, "统计", Screen.Stats.route, currentRoute == Screen.Stats.route),
        BottomNavEntry(R.drawable.icon_profile, "我的", Screen.Settings.route, currentRoute == Screen.Settings.route)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(87.dp)
            .background(Color(0xFFF7F8FC))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(67.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(
                    border = BorderStroke(1.dp, Color(0x6618202C)),
                    shape = RoundedCornerShape(8.dp)
                )
        ) {
            items.forEach { item ->
                BottomNavItem(
                    item = item,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(item.route) }
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    item: BottomNavEntry,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(top = 10.dp, bottom = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(item.iconRes),
                contentDescription = item.label,
                modifier = Modifier.size(28.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(27.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = item.label,
                color = Color(0xFF98A2B3),
                fontSize = 11.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

private data class BottomNavEntry(
    @DrawableRes val iconRes: Int,
    val label: String,
    val route: String,
    val isSelected: Boolean
)
