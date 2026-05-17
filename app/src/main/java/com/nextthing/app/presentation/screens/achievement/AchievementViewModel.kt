package com.nextthing.app.presentation.screens.achievement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextthing.app.domain.model.AchievementProgress
import com.nextthing.app.domain.model.AchievementType
import com.nextthing.app.domain.usecase.AchievementUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class AchievementUiState(
    val achievements: List<AchievementProgress> = emptyList(),
    val unlockedCount: Int = 0,
    val totalCount: Int = AchievementType.entries.size,
    val isLoading: Boolean = false,
    val newlyUnlocked: List<AchievementType> = emptyList()
)

@HiltViewModel
class AchievementViewModel @Inject constructor(
    private val achievementUseCases: AchievementUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(AchievementUiState())
    val uiState: StateFlow<AchievementUiState> = _uiState.asStateFlow()

    init {
        loadAchievements()
    }

    private fun loadAchievements() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                // 检查并解锁达标成就，同时获取全量进度（一次计算）
                val (achievements, newlyUnlocked) = achievementUseCases.checkAndUnlock()
                val unlockedCount = achievements.count { it.isUnlocked }

                _uiState.value = _uiState.value.copy(
                    achievements = achievements,
                    unlockedCount = unlockedCount,
                    newlyUnlocked = newlyUnlocked,
                    isLoading = false
                )
            } catch (e: Exception) {
                Timber.e(e, "加载成就失败")
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun clearNewlyUnlocked() {
        _uiState.value = _uiState.value.copy(newlyUnlocked = emptyList())
    }
}
