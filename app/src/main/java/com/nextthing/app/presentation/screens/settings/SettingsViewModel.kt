package com.nextthing.app.presentation.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextthing.app.data.preferences.ThemePreferences
import com.nextthing.app.data.preferences.AIPreferences
import com.nextthing.app.data.preferences.AIProvider
import com.nextthing.app.data.preferences.ASRPreferences
import com.nextthing.app.data.preferences.BriefingPreferences
import com.nextthing.app.domain.model.ThemeMode
import com.nextthing.app.domain.usecase.AchievementUseCases
import com.nextthing.app.domain.model.AchievementProgress
import com.nextthing.app.domain.model.AchievementType
import com.nextthing.app.domain.usecase.TaskUseCases
import com.nextthing.app.domain.usecase.UserUseCases
import com.nextthing.app.domain.service.ASRService
import com.nextthing.app.data.local.dao.TaskDao
import com.nextthing.app.data.local.dao.GeofenceLocationDao
import com.nextthing.app.data.export.ExportFormat
import com.nextthing.app.data.export.TaskExporter
import dagger.hilt.android.qualifiers.ApplicationContext
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
    // 语音识别配置（端侧，无需额外配置）
    val showASRConfigDialog: Boolean = false,
    // 导出
    val showExportSheet: Boolean = false,
    val isExporting: Boolean = false,
    val exportResultUri: Uri? = null,
    val exportResultMimeType: String? = null,
    // 智能早晚报
    val briefingEnabled: Boolean = false,
    val morningHour: Int = 8,
    val morningMinute: Int = 0,
    val eveningHour: Int = 21,
    val eveningMinute: Int = 0,
    val showBriefingDialog: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val taskUseCases: TaskUseCases,
    private val userUseCases: UserUseCases,
    private val achievementUseCases: AchievementUseCases,
    private val themePreferences: ThemePreferences,
    private val taskDao: TaskDao,
    private val geofenceLocationDao: GeofenceLocationDao,
    private val aiPreferences: AIPreferences,
    private val asrPreferences: ASRPreferences,
    private val asrService: ASRService,
    private val briefingPreferences: BriefingPreferences,
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
        observeASRSettings()
        observeBriefingSettings()
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

    // ── 语音识别配置（端侧，初始化时预热） ─────────────────────────

    private fun observeASRSettings() {
        // 端侧 ASR 无需额外配置，启动时预热
        asrService.warmUp()
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

    // ── 智能早晚报 ──────────────────────────────────────────

    private fun observeBriefingSettings() {
        viewModelScope.launch {
            briefingPreferences.enabled.collect { _uiState.value = _uiState.value.copy(briefingEnabled = it) }
        }
        viewModelScope.launch {
            briefingPreferences.morningHour.collect { _uiState.value = _uiState.value.copy(morningHour = it) }
        }
        viewModelScope.launch {
            briefingPreferences.morningMinute.collect { _uiState.value = _uiState.value.copy(morningMinute = it) }
        }
        viewModelScope.launch {
            briefingPreferences.eveningHour.collect { _uiState.value = _uiState.value.copy(eveningHour = it) }
        }
        viewModelScope.launch {
            briefingPreferences.eveningMinute.collect { _uiState.value = _uiState.value.copy(eveningMinute = it) }
        }
    }

    fun showBriefingDialog() {
        _uiState.value = _uiState.value.copy(showBriefingDialog = true)
    }

    fun hideBriefingDialog() {
        _uiState.value = _uiState.value.copy(showBriefingDialog = false)
    }

    fun saveBriefingSettings(enabled: Boolean, morningHour: Int, morningMinute: Int, eveningHour: Int, eveningMinute: Int) {
        viewModelScope.launch {
            briefingPreferences.setEnabled(enabled)
            briefingPreferences.setMorningTime(morningHour, morningMinute)
            briefingPreferences.setEveningTime(eveningHour, eveningMinute)
            _uiState.value = _uiState.value.copy(showBriefingDialog = false)
            // 重新调度 Worker 需要在 Application 级别处理，这里通过 DataStore 变化触发
            // 实际上用户下次打开 App 时 NextThingApplication.onCreate 会读取最新设置
            // 但为了即时生效，也可以在这里手动调度
            if (enabled) {
                com.nextthing.app.work.TaskWorkScheduler.scheduleMorningBriefing(
                    appContext, morningHour, morningMinute
                )
                com.nextthing.app.work.TaskWorkScheduler.scheduleEveningBriefing(
                    appContext, eveningHour, eveningMinute
                )
            } else {
                com.nextthing.app.work.TaskWorkScheduler.cancelBriefingWork(appContext)
            }
            _uiState.value = _uiState.value.copy(
                actionMessage = if (enabled) "早晚报已开启" else "早晚报已关闭"
            )
        }
    }
}