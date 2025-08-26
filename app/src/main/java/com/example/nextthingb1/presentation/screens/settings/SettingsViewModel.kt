package com.example.nextthingb1.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextthingb1.domain.usecase.TaskUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.ui.graphics.Color
import com.example.nextthingb1.presentation.theme.*

data class SettingsUiState(
    val username: String = "用户12345",
    val usageDays: Int = 365,
    val isPro: Boolean = true,
    val version: String = "1.0.0",
    val features: List<FeatureItem> = emptyList(),
    val settingSections: List<SettingSection> = emptyList(),
    val locationEnhancementEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val taskUseCases: TaskUseCases
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        initializeSettings()
    }
    
    private fun initializeSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // 初始化功能网格
                val features = listOf(
                    FeatureItem("smart_remind", "智能提醒", "🤖", Primary),
                    FeatureItem("voice_input", "语音输入", "🎤", Success),
                    FeatureItem("category_mgmt", "分类管理", "📁", Warning),
                    FeatureItem("priority_mgmt", "优先级管理", "⭐", Color(0xFFE91E63)),
                    FeatureItem("deadline_remind", "截止提醒", "⏰", Color(0xFF9C27B0)),
                    FeatureItem("progress_analysis", "进度分析", "📊", Color(0xFF3F51B5)),
                    FeatureItem("export_tasks", "导出任务", "📤", Color(0xFF009688)),
                    FeatureItem("cloud_sync", "云端同步", "☁️", Color(0xFF4CAF50)),
                    FeatureItem("team_share", "团队协作", "👥", Color(0xFFFF9800)),
                    FeatureItem("habit_track", "习惯追踪", "📈", Color(0xFFF44336)),
                    FeatureItem("time_track", "时间记录", "⏱️", Color(0xFF795548)),
                    FeatureItem("focus_mode", "专注模式", "🎯", Color(0xFF607D8B))
                )
                
                // 初始化设置选项
                val settingSections = listOf(
                    SettingSection(
                        items = listOf(
                            SettingItem(
                                id = "promotion",
                                title = "优惠中心",
                                subtitle = "查看最新优惠活动",
                                icon = "🎁",
                                color = Color(0xFFE91E63),
                                type = SettingType.ARROW
                            )
                        )
                    ),
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
                                id = "sound",
                                title = "音效",
                                icon = "🔊",
                                color = Color(0xFF4CAF50),
                                type = SettingType.SWITCH,
                                isEnabled = true
                            ),
                            SettingItem(
                                id = "vibration",
                                title = "震动",
                                icon = "📳",
                                color = Color(0xFFFF9800),
                                type = SettingType.SWITCH,
                                isEnabled = false
                            ),
                            SettingItem(
                                id = "location_enhancement",
                                title = "位置信息增强",
                                subtitle = "自动获取当前位置信息",
                                icon = "📍",
                                color = Color(0xFF2196F3),
                                type = SettingType.SWITCH,
                                isEnabled = false
                            )
                        )
                    ),
                    SettingSection(
                        items = listOf(
                            SettingItem(
                                id = "feedback",
                                title = "建议反馈",
                                subtitle = "帮助我们改进产品",
                                icon = "💬",
                                color = Color(0xFF2196F3),
                                type = SettingType.ARROW
                            ),
                            SettingItem(
                                id = "clear_all_tasks",
                                title = "清空所有任务",
                                subtitle = "删除所有任务数据（不可恢复）",
                                icon = "🗑️",
                                color = Color(0xFFF44336),
                                type = SettingType.ARROW
                            ),
                            SettingItem(
                                id = "about",
                                title = "关于我们",
                                subtitle = "了解更多应用信息",
                                icon = "ℹ️",
                                color = Color(0xFF607D8B),
                                type = SettingType.ARROW
                            )
                        )
                    )
                )
                
                _uiState.value = _uiState.value.copy(
                    features = features,
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
    
    fun onFeatureClick(feature: FeatureItem) {
        // TODO: 处理功能点击
        when (feature.id) {
            "smart_remind" -> {
                // 智能提醒功能
            }
            "voice_input" -> {
                // 语音输入功能
            }
            "category_mgmt" -> {
                // 分类管理功能
            }
            "priority_mgmt" -> {
                // 优先级管理功能
            }
            "deadline_remind" -> {
                // 截止提醒功能
            }
            "progress_analysis" -> {
                // 进度分析功能
            }
            "export_tasks" -> {
                // 导出任务功能
            }
            "cloud_sync" -> {
                // 云端同步功能
            }
            "team_share" -> {
                // 团队协作功能
            }
            "habit_track" -> {
                // 习惯追踪功能
            }
            "time_track" -> {
                // 时间记录功能
            }
            "focus_mode" -> {
                // 专注模式功能
            }
        }
    }
    
    fun onSettingClick(setting: SettingItem) {
        // TODO: 处理设置点击
        when (setting.id) {
            "promotion" -> {
                // 优惠中心
            }
            "theme" -> {
                // 主题设置
            }
            "clear_all_tasks" -> {
                clearAllTasks()
            }
            "feedback" -> {
                // 建议反馈
            }
            "location_enhancement" -> {
                toggleLocationEnhancement()
            }
            "about" -> {
                // 关于我们
            }
        }
    }
    
    private fun clearAllTasks() {
        viewModelScope.launch {
            try {
                taskUseCases.deleteAllTasks().fold(
                    onSuccess = {
                        // 任务清空成功
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            errorMessage = error.message
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message
                )
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
} 