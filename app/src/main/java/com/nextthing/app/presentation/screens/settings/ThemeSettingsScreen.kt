package com.nextthing.app.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nextthing.app.domain.model.ThemeMode
import com.nextthing.app.presentation.theme.BgCard
import com.nextthing.app.presentation.theme.BgPrimary
import com.nextthing.app.presentation.theme.Border
import com.nextthing.app.presentation.theme.Primary
import com.nextthing.app.presentation.theme.TextPrimary
import com.nextthing.app.presentation.theme.TextSecondary

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    viewModel: ThemeSettingsViewModel = hiltViewModel(),
    onBackPressed: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = BgPrimary,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "主题设置",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgPrimary)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgPrimary)
                .padding(paddingValues)
        ) {

        Text(
            text = "主题模式",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 5.dp)
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .border(1.dp, Border, RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            color = BgCard,
            shadowElevation = 0.dp
        ) {
            Column {
                ThemeModeOption(
                    title = "跟随系统",
                    description = "自动匹配设备的明暗模式",
                    selected = uiState.themeMode == ThemeMode.SYSTEM,
                    onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) }
                )
                HorizontalDivider(color = Border, thickness = 1.dp, modifier = Modifier.padding(start = 16.dp))
                ThemeModeOption(
                    title = "浅色模式",
                    description = "始终使用浅色界面",
                    selected = uiState.themeMode == ThemeMode.LIGHT,
                    onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) }
                )
                HorizontalDivider(color = Border, thickness = 1.dp, modifier = Modifier.padding(start = 16.dp))
                ThemeModeOption(
                    title = "深色模式",
                    description = "始终使用深色界面",
                    selected = uiState.themeMode == ThemeMode.DARK,
                    onClick = { viewModel.setThemeMode(ThemeMode.DARK) }
                )
            }
        }

        Text(
            text = "主题设置会立即应用到全局界面",
            fontSize = 13.sp,
            color = TextSecondary,
            modifier = Modifier.padding(start = 20.dp, top = 10.dp)
        )
    }
    }
}

@Composable
private fun ThemeModeOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Primary.copy(alpha = 0.06f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) Primary else TextPrimary
            )
            Text(text = description, fontSize = 13.sp, color = TextSecondary)
        }
        ThemeRadio(selected)
    }
}

@Composable
private fun ThemeRadio(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .border(2.dp, if (selected) Primary else Border, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Primary)
            )
        }
    }
}
