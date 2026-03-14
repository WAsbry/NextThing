package com.example.nextthingb1.presentation.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextthingb1.data.preferences.ThemePreferences
import com.example.nextthingb1.domain.model.ThemeMode
import com.example.nextthingb1.domain.usecase.AchievementUseCases
import com.example.nextthingb1.domain.model.AchievementProgress
import com.example.nextthingb1.domain.model.AchievementType
import com.example.nextthingb1.domain.usecase.TaskUseCases
import com.example.nextthingb1.domain.usecase.UserUseCases
import com.example.nextthingb1.data.local.dao.TaskDao
import com.example.nextthingb1.data.local.dao.GeofenceLocationDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
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
    val isLoading: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val taskUseCases: TaskUseCases,
    private val userUseCases: UserUseCases,
    private val achievementUseCases: AchievementUseCases,
    private val themePreferences: ThemePreferences,
    private val taskDao: TaskDao,
    private val geofenceLocationDao: GeofenceLocationDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadUserInfo()
        loadStatistics()
        loadAchievements()
        observeThemeMode()
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

    // ── 数据加载 ──────────────────────────────────────────

    private fun loadUserInfo() {
        viewModelScope.launch {
            try {
                userUseCases.getCurrentUser().collect { user ->
                    if (user != null) {
                        val currentTime = System.currentTimeMillis()
                        val usageDays = ((currentTime - user.createdAt) / (24 * 60 * 60 * 1000)).toInt()
                        _uiState.value = _uiState.value.copy(
                            username = user.nickname,
                            avatarUri = user.avatarUri?.let { Uri.parse(it) },
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
}