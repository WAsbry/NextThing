package com.nextthing.app.presentation.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextthing.app.data.preferences.ViewPreferences
import com.nextthing.app.presentation.theme.BgCard
import com.nextthing.app.presentation.theme.BgPrimary
import com.nextthing.app.presentation.theme.Border
import com.nextthing.app.presentation.theme.Danger
import com.nextthing.app.presentation.theme.Primary
import com.nextthing.app.presentation.theme.TextMuted
import com.nextthing.app.presentation.theme.TextPrimary
import com.nextthing.app.presentation.theme.TextSecondary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

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
            combine(viewPreferences.collapseOverdue, viewPreferences.collapseFuture) { overdue, future ->
                ViewPreferencesUiState(overdue, future)
            }.collect { _uiState.value = it }
        }
    }

    fun setCollapseOverdue(enabled: Boolean) {
        viewModelScope.launch { viewPreferences.setCollapseOverdue(enabled) }
    }

    fun setCollapseFuture(enabled: Boolean) {
        viewModelScope.launch { viewPreferences.setCollapseFuture(enabled) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewPreferencesScreen(
    viewModel: ViewPreferencesViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = BgPrimary,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "视图偏好",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgCard)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp)
        ) {
            item {
                Text(
                    text = "将低优先级任务收起，优先聚焦当前待办",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = BgCard),
                    border = BorderStroke(1.dp, Border)
                ) {
                    Column {
                        CollapseSwitchRow(
                            icon = Icons.Filled.Schedule,
                            iconTint = Danger,
                            title = "折叠逾期任务",
                            subtitle = "在任务列表中以数量摘要显示",
                            checked = uiState.collapseOverdue,
                            onCheckedChange = viewModel::setCollapseOverdue
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 68.dp, end = 16.dp),
                            color = Border,
                            thickness = 1.dp
                        )
                        CollapseSwitchRow(
                            icon = Icons.Filled.Event,
                            iconTint = Primary,
                            title = "折叠未来任务",
                            subtitle = "将 NextThing 之后的任务以摘要显示",
                            checked = uiState.collapseFuture,
                            onCheckedChange = viewModel::setCollapseFuture
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CollapseSwitchRow(
    icon: ImageVector,
    iconTint: Color,
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
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 13.sp, color = TextMuted)
        }
        Spacer(modifier = Modifier.width(8.dp))
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
