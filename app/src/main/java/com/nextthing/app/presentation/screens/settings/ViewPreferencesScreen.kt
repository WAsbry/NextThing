package com.nextthing.app.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextthing.app.data.preferences.ViewPreferences
import com.nextthing.app.presentation.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── ViewModel ──

data class ViewPreferencesUiState(
    val collapseOverdue: Boolean = false,
    val collapseFuture: Boolean = false
)

@HiltViewModel
class ViewPreferencesViewModel @Inject constructor(
    private val viewPreferences: ViewPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(ViewPreferencesUiState())
    val uiState: StateFlow<ViewPreferencesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                viewPreferences.collapseOverdue,
                viewPreferences.collapseFuture
            ) { overdue, future -> ViewPreferencesUiState(overdue, future) }
                .collect { _uiState.value = it }
        }
    }

    fun setCollapseOverdue(enabled: Boolean) {
        viewModelScope.launch { viewPreferences.setCollapseOverdue(enabled) }
    }

    fun setCollapseFuture(enabled: Boolean) {
        viewModelScope.launch { viewPreferences.setCollapseFuture(enabled) }
    }
}

// ── Screen ──

@Composable
fun ViewPreferencesScreen(
    viewModel: ViewPreferencesViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = BgPrimary,
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // 顶部导航栏
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(BgCard)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Primary)
                            .clickable { onNavigateBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = "视图偏好",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            // 功能说明卡片
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 20.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "关于折叠视图",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "折叠视图让您专注于最重要的下一件事（NextThing）。\n\n" +
                                    "开启后，逾期和未来的任务将被折叠为摘要条，仅显示数量。" +
                                    "点击摘要条可临时展开查看全部任务。",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // 折叠视图 Section
            item {
                Text(
                    text = "折叠视图",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 20.dp, bottom = 6.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BgCard),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column {
                        // 折叠逾期任务
                        SwitchRow(
                            icon = "🕐",
                            iconBgColor = Danger,
                            title = "折叠逾期任务",
                            subtitle = "将已逾期的任务折叠为数量摘要",
                            checked = uiState.collapseOverdue,
                            onCheckedChange = { viewModel.setCollapseOverdue(it) }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 68.dp, end = 16.dp),
                            color = Border.copy(alpha = 0.6f),
                            thickness = 0.5.dp
                        )

                        // 折叠未来任务
                        SwitchRow(
                            icon = "🔮",
                            iconBgColor = Primary,
                            title = "折叠未来任务",
                            subtitle = "将 NextThing 之后的任务折叠为摘要",
                            checked = uiState.collapseFuture,
                            onCheckedChange = { viewModel.setCollapseFuture(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(
    icon: String,
    iconBgColor: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBgColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.width(14.dp))

        // 文字
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextMuted
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 开关
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Border
            )
        )
    }
}
