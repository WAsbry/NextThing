package com.example.nextthingb1.presentation.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextthingb1.domain.usecase.TaskUseCases
import com.example.nextthingb1.domain.usecase.UserUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.ui.graphics.Color
import com.example.nextthingb1.presentation.theme.*

data class SettingsUiState(
    val username: String = "加载中...",
    val avatarUri: Uri? = null,
    val usageDays: Int = 0,
    val settingSections: List<SettingSection> = emptyList(),
    val locationEnhancementEnabled: Boolean = false,
    val geofenceEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val taskUseCases: TaskUseCases,
    private val userUseCases: UserUseCases
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        initializeSettings()
        loadUserInfo()
    }

    private fun loadUserInfo() {
        viewModelScope.launch {
            userUseCases.getCurrentUser().collect { user ->
                if (user != null) {
                    // 计算使用天数
                    val currentTime = System.currentTimeMillis()
                    val usageDays = ((currentTime - user.createdAt) / (24 * 60 * 60 * 1000)).toInt()

                    _uiState.value = _uiState.value.copy(
                        username = user.nickname,
                        avatarUri = user.avatarUri?.let { Uri.parse(it) },
                        usageDays = usageDays.coerceAtLeast(1) // 至少显示1天
                    )
                }
            }
        }
    }

    private fun initializeSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // 初始化设置选项
                val settingSections = listOf(
                    SettingSection(
                        items = listOf(
                            SettingItem(
                                id = "theme",
                                title = "主题皮肤",
                                subtitle = "个性化您的应用外观",
                                icon = "🎨",
                                color = Color(0xFF9C27B0),
                                type = SettingType.ARROW
                            ),
                            SettingItem(
                                id = "location_enhancement",
                                title = "位置信息增强",
                                subtitle = "自动获取当前位置信息",
                                icon = "📍",
                                color = Color(0xFF4FC3F7),
                                type = SettingType.SWITCH,
                                isEnabled = false
                            ),
                            SettingItem(
                                id = "geofence",
                                title = "地理围栏",
                                subtitle = "设置位置提醒范围",
                                icon = "🗺️",
                                color = Color(0xFF29B6F6),
                                type = SettingType.SWITCH,
                                isEnabled = false
                            )
                        )
                    )
                )

                _uiState.value = _uiState.value.copy(
                    settingSections = settingSections,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message,
                    isLoading = false
                )
            }
        }
    }
    
    fun onSettingClick(setting: SettingItem) {
        when (setting.id) {
            "theme" -> {
                // 主题设置
            }
            "location_enhancement" -> {
                toggleLocationEnhancement()
            }
            "geofence" -> {
                toggleGeofence()
            }
        }
    }
    
    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun toggleLocationEnhancement() {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            locationEnhancementEnabled = !currentState.locationEnhancementEnabled
        )

        // TODO: 保存设置到SharedPreferences
        // TODO: 如果开启，请求位置权限
    }

    private fun toggleGeofence() {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            geofenceEnabled = !currentState.geofenceEnabled
        )

        // TODO: 保存设置到SharedPreferences
        // TODO: 如果开启，启动地理围栏服务
    }
} 