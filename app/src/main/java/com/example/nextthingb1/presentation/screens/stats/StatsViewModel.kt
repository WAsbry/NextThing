package com.example.nextthingb1.presentation.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextthingb1.domain.usecase.TaskUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import androidx.compose.ui.graphics.Color
import com.example.nextthingb1.presentation.theme.*

enum class StatsTab(val title: String) {
    OVERVIEW("概览"),
    CATEGORY("分类"),
    TREND("趋势"),
    PERFORMANCE("效率")
}

data class StatsUiState(
    val selectedTab: StatsTab = StatsTab.OVERVIEW,
    val currentMonth: String = "",
    val totalTasks: String = "0",
    val completedTasks: String = "0", 
    val completionRate: String = "0%",
    val averageCompletionTime: String = "0天",
    val categories: List<CategoryItem> = emptyList(),
    val details: List<DetailItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val taskUseCases: TaskUseCases
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()
    
    private val currentDate = LocalDate.now()
    private var currentMonthDate = currentDate
    
    init {
        updateCurrentMonth()
        loadStatistics()
    }
    
    private fun updateCurrentMonth() {
        val formatter = DateTimeFormatter.ofPattern("yyyy年MM月")
        _uiState.value = _uiState.value.copy(
            currentMonth = currentMonthDate.format(formatter)
        )
    }
    
    private fun loadStatistics() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val statistics = taskUseCases.getTaskStatistics()
                
                // 模拟分类数据
                val categories = listOf(
                    CategoryItem(
                        name = "香烟",
                        percentage = 27.83f,
                        amount = "¥ 255.00",
                        color = Color(0xFF4FC3F7),
                        icon = "🚬"
                    ),
                    CategoryItem(
                        name = "餐饮",
                        percentage = 23.58f,
                        amount = "¥ 199.00",
                        color = Color(0xFF66BB6A),
                        icon = "🍽️"
                    ),
                    CategoryItem(
                        name = "读书",
                        percentage = 17.23f,
                        amount = "¥ 145.50",
                        color = Color(0xFFFFA726),
                        icon = "📚"
                    )
                )
                
                // 模拟任务明细数据
                val details = listOf(
                    DetailItem(
                        name = "工作任务",
                        description = "完成项目报告",
                        amount = "已完成 3个",
                        color = Color(0xFF4FC3F7),
                        icon = "💼"
                    ),
                    DetailItem(
                        name = "学习任务",
                        description = "阅读技术书籍",
                        amount = "进行中 2个",
                        color = Color(0xFFFFA726),
                        icon = "📚"
                    ),
                    DetailItem(
                        name = "生活任务",
                        description = "家务整理",
                        amount = "待办 1个",
                        color = Color(0xFF66BB6A),
                        icon = "🏠"
                    )
                )
                
                _uiState.value = _uiState.value.copy(
                    categories = categories,
                    details = details,
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
    
    fun selectTab(tab: StatsTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
        // 根据选择的标签重新加载数据
        loadStatistics()
    }
    
    fun previousMonth() {
        currentMonthDate = currentMonthDate.minusMonths(1)
        updateCurrentMonth()
        loadStatistics()
    }
    
    fun nextMonth() {
        currentMonthDate = currentMonthDate.plusMonths(1)
        updateCurrentMonth()
        loadStatistics()
    }
    
    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
} 