package com.nextthing.app.presentation.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextthing.app.data.preferences.ThemePreferences
import com.nextthing.app.data.preferences.AIPreferences
import com.nextthing.app.data.preferences.AIProvider
import com.nextthing.app.data.preferences.IFlyPreferences
import com.nextthing.app.domain.model.ThemeMode
import com.nextthing.app.domain.usecase.AchievementUseCases
import com.nextthing.app.domain.model.AchievementProgress
import com.nextthing.app.domain.model.AchievementType
import com.nextthing.app.domain.usecase.TaskUseCases
import com.nextthing.app.domain.usecase.UserUseCases
import com.nextthing.app.data.local.dao.TaskDao
import com.nextthing.app.data.local.dao.GeofenceLocationDao
import com.nextthing.app.data.export.ExportFormat
import com.nextthing.app.data.export.TaskExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime
import javax.inject.Inject

data class SettingsUiState(
    val username: String = "加载中...",
    val avatarUri: Uri? = null,
    val usageDays: Int = 0,
    // 任务统计（真实数据）
    val completedCount: Int = 0,
    val pendingCount: Int = 0,
    val overdueCount: Int = 0,
    val streakDays: Int = 0,
    // 地理围栏数量
    val geofenceCount: Int = 0,
    // 成就
    val recentAchievements: List<AchievementProgress> = emptyList(),
    val unlockedAchievementsCount: Int = 0,
    val totalAchievementsCount: Int = AchievementType.entries.size,
    // 主题
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val showThemeDialog: Boolean = false,
    // 清除已完成确认弹窗
    val showClearConfirmDialog: Boolean = false,
    // 操作结果消息（用于 Snackbar）
    val actionMessage: String? = null,
    val isLoading: Boolean = false,
    // AI 配置
    val aiProvider: AIProvider = AIProvider.DEEPSEEK,
    val aiApiKey: String = "",
    val aiModel: String = "",
    val showAIConfigDialog: Boolean = false,
    // 讯飞 ASR 配置
    val iflyAppId: String = "",
    val iflyApiKey: String = "",
    val iflyApiSecret: String = "",
    val iflyAccent: String = "lmz",
    val showIFlyConfigDialog: Boolean = false,
    // 导出
    val showExportSheet: Boolean = false,
    val isExporting: Boolean = false,
    val exportResultUri: Uri? = null,
    val exportResultMimeType: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val taskUseCases: TaskUseCases,
    private val userUseCases: UserUseCases,
    private val achievementUseCases: AchievementUseCases,
    private val themePreferences: ThemePreferences,
    private val taskDao: TaskDao,
    private val geofenceLocationDao: GeofenceLocationDao,
    private val aiPreferences: AIPreferences,
    private val iflyPreferences: IFlyPreferences,
    private val taskExporter: TaskExporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var currentUserId: String? = null

    init {
        loadUserInfo()
        loadStatistics()
        loadAchievements()
        observeThemeMode()
        observeAISettings()
        observeIFlySettings()
    }

    // ── 主题 ──────────────────────────────────────────────

    private fun observeThemeMode() {
        viewModelScope.launch {
            themePreferences.themeMode.collect { mode ->
                _uiState.value = _uiState.value.copy(themeMode = mode)
            }
        }
    }

    fun showThemeDialog() {
        _uiState.value = _uiState.value.copy(showThemeDialog = true)
    }

    fun hideThemeDialog() {
        _uiState.value = _uiState.value.copy(showThemeDialog = false)
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themePreferences.setThemeMode(mode)
            _uiState.value = _uiState.value.copy(showThemeDialog = false)
        }
    }

    // ── 头像更新 ──────────────────────────────────────────

    fun updateAvatar(uri: Uri) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            try {
                userUseCases.updateAvatar(userId, uri.toString())
                _uiState.value = _uiState.value.copy(avatarUri = uri)
            } catch (e: Exception) {
                Timber.e(e, "更新头像失败")
            }
        }
    }

    // ── 数据加载 ──────────────────────────────────────────

    private fun loadUserInfo() {
        viewModelScope.launch {
            try {
                userUseCases.getCurrentUser().collect { user ->
                    if (user != null) {
                        currentUserId = user.id
                        val currentTime = System.currentTimeMillis()
                        val usageDays = ((currentTime - user.createdAt) / (24 * 60 * 60 * 1000)).toInt()
                        _uiState.value = _uiState.value.copy(
                            username = user.nickname,
                            avatarUri = user.avatarUri?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) },
                            usageDays = usageDays.coerceAtLeast(1)
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "加载用户信息失败")
            }
        }
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            try {
                val completedCount = taskDao.getCompletedTasksCount()
                val pendingCount = taskDao.getPendingTasksCount()
                val overdueCount = taskDao.getOverdueTasksCount()
                val geofenceCount = geofenceLocationDao.getCount()
                _uiState.value = _uiState.value.copy(
                    completedCount = completedCount,
                    pendingCount = pendingCount,
                    overdueCount = overdueCount,
                    geofenceCount = geofenceCount
                )
            } catch (e: Exception) {
                Timber.e(e, "加载任务统计失败")
            }
        }
    }

    private fun loadAchievements() {
        viewModelScope.launch {
            try {
                val (achievements, _) = achievementUseCases.checkAndUnlock()
                val unlockedCount = achievements.count { it.isUnlocked }
                // 从成就数据中取连续打卡天数（STREAK_100 的 currentValue 就是实际天数）
                val streakDays = achievements
                    .find { it.type == AchievementType.STREAK_100 }?.currentValue ?: 0
                val recent = (achievements.filter { it.isUnlocked }
                    .sortedByDescending { it.unlockedAt } +
                    achievements.filter { !it.isUnlocked }
                    .sortedByDescending { it.progress })
                    .take(6)
                _uiState.value = _uiState.value.copy(
                    recentAchievements = recent,
                    unlockedAchievementsCount = unlockedCount,
                    streakDays = streakDays
                )
            } catch (e: Exception) {
                Timber.e(e, "加载成就失败")
            }
        }
    }

    // ── 清除已完成任务 ────────────────────────────────────

    fun showClearConfirmDialog() {
        _uiState.value = _uiState.value.copy(showClearConfirmDialog = true)
    }

    fun hideClearConfirmDialog() {
        _uiState.value = _uiState.value.copy(showClearConfirmDialog = false)
    }

    fun clearCompletedTasks() {
        viewModelScope.launch {
            val countBefore = _uiState.value.completedCount
            try {
                taskUseCases.deleteCompletedTasks()
                _uiState.value = _uiState.value.copy(
                    completedCount = 0,
                    showClearConfirmDialog = false,
                    actionMessage = "已清除 $countBefore 条已完成任务"
                )
            } catch (e: Exception) {
                Timber.e(e, "清除已完成任务失败")
                _uiState.value = _uiState.value.copy(
                    showClearConfirmDialog = false,
                    actionMessage = "清除失败，请重试"
                )
            }
        }
    }

    fun clearActionMessage() {
        _uiState.value = _uiState.value.copy(actionMessage = null)
    }

    // ── AI 配置 ───────────────────────────────────────────

    private fun observeAISettings() {
        viewModelScope.launch {
            aiPreferences.provider.collect { provider ->
                _uiState.value = _uiState.value.copy(aiProvider = provider)
            }
        }
        viewModelScope.launch {
            aiPreferences.apiKey.collect { key ->
                _uiState.value = _uiState.value.copy(aiApiKey = key)
            }
        }
        viewModelScope.launch {
            aiPreferences.model.collect { model ->
                _uiState.value = _uiState.value.copy(aiModel = model)
            }
        }
    }

    fun showAIConfigDialog() {
        _uiState.value = _uiState.value.copy(showAIConfigDialog = true)
    }

    fun hideAIConfigDialog() {
        _uiState.value = _uiState.value.copy(showAIConfigDialog = false)
    }

    fun saveAIConfig(provider: AIProvider, apiKey: String, model: String) {
        viewModelScope.launch {
            aiPreferences.setProvider(provider)
            aiPreferences.setApiKey(apiKey)
            aiPreferences.setModel(model)
            _uiState.value = _uiState.value.copy(
                showAIConfigDialog = false,
                actionMessage = if (apiKey.isNotBlank()) "AI 配置已保存" else "AI 配置已清除"
            )
        }
    }

    // ── 讯飞 ASR 配置 ──────────────────────────────────────────

    private fun observeIFlySettings() {
        viewModelScope.launch {
            iflyPreferences.appId.collect { _uiState.value = _uiState.value.copy(iflyAppId = it) }
        }
        viewModelScope.launch {
            iflyPreferences.apiKey.collect { _uiState.value = _uiState.value.copy(iflyApiKey = it) }
        }
        viewModelScope.launch {
            iflyPreferences.apiSecret.collect { _uiState.value = _uiState.value.copy(iflyApiSecret = it) }
        }
        viewModelScope.launch {
            iflyPreferences.accent.collect { _uiState.value = _uiState.value.copy(iflyAccent = it) }
        }
    }

    fun showIFlyConfigDialog() {
        _uiState.value = _uiState.value.copy(showIFlyConfigDialog = true)
    }

    fun hideIFlyConfigDialog() {
        _uiState.value = _uiState.value.copy(showIFlyConfigDialog = false)
    }

    fun saveIFlyConfig(appId: String, apiKey: String, apiSecret: String, accent: String) {
        viewModelScope.launch {
            iflyPreferences.setAppId(appId)
            iflyPreferences.setApiKey(apiKey)
            iflyPreferences.setApiSecret(apiSecret)
            iflyPreferences.setAccent(accent)
            _uiState.value = _uiState.value.copy(
                showIFlyConfigDialog = false,
                actionMessage = if (appId.isNotBlank()) "讯飞配置已保存" else "讯飞配置已清除"
            )
        }
    }

    // ── 导出数据 ───────────────────────────────────────────

    fun showExportSheet() {
        _uiState.value = _uiState.value.copy(showExportSheet = true)
    }

    fun hideExportSheet() {
        _uiState.value = _uiState.value.copy(showExportSheet = false)
    }

    fun exportData(startDate: LocalDateTime, endDate: LocalDateTime, format: ExportFormat) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)
            try {
                val result = taskExporter.export(startDate, endDate, format)
                result.fold(
                    onSuccess = { uri ->
                        _uiState.value = _uiState.value.copy(
                            isExporting = false,
                            showExportSheet = false,
                            exportResultUri = uri,
                            exportResultMimeType = format.mimeType,
                            actionMessage = "导出成功，请选择打开方式"
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isExporting = false,
                            actionMessage = error.message ?: "导出失败"
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "导出数据失败")
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    actionMessage = "导出失败: ${e.message}"
                )
            }
        }
    }

    fun clearExportResult() {
        _uiState.value = _uiState.value.copy(exportResultUri = null, exportResultMimeType = null)
    }
}