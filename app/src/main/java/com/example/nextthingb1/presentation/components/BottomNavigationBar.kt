package com.example.nextthingb1.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nextthingb1.presentation.navigation.Screen
import com.example.nextthingb1.presentation.theme.TextMuted
import com.example.nextthingb1.presentation.theme.TextPrimary

@Composable
fun BottomNavigationBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavItem(
            icon = "home",
            label = "首页",
            isSelected = currentRoute == Screen.Today.route,
            onClick = { onNavigate(Screen.Today.route) }
        )
        BottomNavItem(
            icon = "list",
            label = "任务",
            isSelected = currentRoute == Screen.Tasks.route,
            onClick = { onNavigate(Screen.Tasks.route) }
        )
        BottomNavItem(
            icon = "add",
            label = "创建",
            isSelected = currentRoute == Screen.CreateTask.route,
            onClick = { onNavigate(Screen.CreateTask.route) }
        )
        BottomNavItem(
            icon = "chart-pie",
            label = "统计",
            isSelected = currentRoute == Screen.Stats.route,
            onClick = { onNavigate(Screen.Stats.route) }
        )
        BottomNavItem(
            icon = "user",
            label = "我的",
            isSelected = currentRoute == Screen.Settings.route,
            onClick = { onNavigate(Screen.Settings.route) }
        )
    }
}

@Composable
private fun BottomNavItem(
    icon: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val iconResource = when (icon) {
            "home" -> android.R.drawable.ic_menu_myplaces
            "list" -> android.R.drawable.ic_menu_agenda
            "add" -> android.R.drawable.ic_input_add
            "chart-pie" -> android.R.drawable.ic_menu_info_details
            "user" -> android.R.drawable.ic_menu_preferences
            else -> android.R.drawable.ic_menu_help
        }
        
        Icon(
            painter = painterResource(id = iconResource),
            contentDescription = label,
            tint = if (isSelected) TextPrimary else TextMuted,
            modifier = Modifier.size(20.dp)
        )
        
        Text(
            text = label,
            color = if (isSelected) TextPrimary else TextMuted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
} 