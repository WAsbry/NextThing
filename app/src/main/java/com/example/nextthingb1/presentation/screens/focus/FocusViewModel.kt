package com.example.nextthingb1.presentation.screens.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextthingb1.domain.usecase.TaskUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FocusUiState(
    val currentTaskTitle: String = "学习 Kotlin 协程",
    val timeRemaining: Int = 25 * 60, // 25分钟，单位：秒
    val totalTime: Int = 25 * 60,
    val isRunning: Boolean = false,
    val todayFocusTime: Int = 120, // 今日专注时间（分钟）
    val totalFocusTime: Int = 48, // 累计专注时间（小时）
    val focusCount: Int = 15, // 专注次数
    val achievements: Int = 3, // 解锁成就数
    val showAchievementPopup: Boolean = false,
    val latestAchievement: Achievement? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class FocusViewModel @Inject constructor(
    private val taskUseCases: TaskUseCases
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(FocusUiState())
    val uiState: StateFlow<FocusUiState> = _uiState.asStateFlow()
    
    private var timerJob: Job? = null
    private var sessionStartTime: Long = 0
    
    init {
        loadFocusData()
    }
    
    private fun loadFocusData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // 加载专注统计数据
                val statistics = taskUseCases.getTaskStatistics()
                
                _uiState.value = _uiState.value.copy(
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
    
    fun toggleTimer() {
        if (_uiState.value.isRunning) {
            pauseTimer()
        } else {
            startTimer()
        }
    }
    
    private fun startTimer() {
        if (_uiState.value.timeRemaining <= 0) {
            resetTimer()
        }
        
        sessionStartTime = System.currentTimeMillis()
        _uiState.value = _uiState.value.copy(isRunning = true)
        
        timerJob = viewModelScope.launch {
            while (_uiState.value.isRunning && _uiState.value.timeRemaining > 0) {
                delay(1000)
                val currentState = _uiState.value
                if (currentState.isRunning) {
                    val newTimeRemaining = currentState.timeRemaining - 1
                    _uiState.value = currentState.copy(timeRemaining = newTimeRemaining)
                    
                    // 检查是否完成
                    if (newTimeRemaining <= 0) {
                        onTimerComplete()
                    }
                }
            }
        }
    }
    
    private fun pauseTimer() {
        timerJob?.cancel()
        _uiState.value = _uiState.value.copy(isRunning = false)
    }
    
    fun resetTimer() {
        timerJob?.cancel()
        _uiState.value = _uiState.value.copy(
            timeRemaining = _uiState.value.totalTime,
            isRunning = false
        )
    }
    
    private fun onTimerComplete() {
        timerJob?.cancel()
        
        // 更新统计数据
        val sessionDuration = (System.currentTimeMillis() - sessionStartTime) / 1000 / 60 // 分钟
        val currentState = _uiState.value
        
        _uiState.value = currentState.copy(
            isRunning = false,
            todayFocusTime = currentState.todayFocusTime + sessionDuration.toInt(),
            focusCount = currentState.focusCount + 1
        )
        
        // 检查成就
        checkAchievements()
    }
    
    fun adjustTime(minutes: Int) {
        if (!_uiState.value.isRunning) {
            val currentState = _uiState.value
            val newTime = (currentState.timeRemaining + minutes * 60).coerceAtLeast(60) // 最少1分钟
            val newTotalTime = (currentState.totalTime + minutes * 60).coerceAtLeast(60)
            
            _uiState.value = currentState.copy(
                timeRemaining = newTime,
                totalTime = newTotalTime
            )
        }
    }
    
    private fun checkAchievements() {
        val currentState = _uiState.value
        
        // 检查各种成就条件
        when {
            currentState.focusCount == 1 -> {
                showAchievement(
                    Achievement(
                        title = "初次专注",
                        description = "恭喜完成第一次专注！",
                        icon = "🎯"
                    )
                )
            }
            currentState.focusCount == 10 -> {
                showAchievement(
                    Achievement(
                        title = "专注达人",
                        description = "已完成10次专注，继续保持！",
                        icon = "🏆"
                    )
                )
            }
            currentState.todayFocusTime >= 120 -> {
                showAchievement(
                    Achievement(
                        title = "今日专注王",
                        description = "今天专注时间超过2小时！",
                        icon = "👑"
                    )
                )
            }
        }
    }
    
    private fun showAchievement(achievement: Achievement) {
        _uiState.value = _uiState.value.copy(
            showAchievementPopup = true,
            latestAchievement = achievement,
            achievements = _uiState.value.achievements + 1
        )
    }
    
    fun dismissAchievementPopup() {
        _uiState.value = _uiState.value.copy(
            showAchievementPopup = false,
            latestAchievement = null
        )
    }
    
    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
} 