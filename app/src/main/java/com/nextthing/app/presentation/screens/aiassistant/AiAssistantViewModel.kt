package com.nextthing.app.presentation.screens.aiassistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextthing.app.data.service.AICompletionClient
import com.nextthing.app.data.service.AIRouteMode
import com.nextthing.app.domain.model.AITaskParseResult
import com.nextthing.app.domain.model.Category
import com.nextthing.app.domain.model.PresetCategories
import com.nextthing.app.domain.model.RepeatFrequency
import com.nextthing.app.domain.model.RepeatFrequencyType
import com.nextthing.app.domain.repository.CategoryRepository
import com.nextthing.app.domain.service.AITaskParser
import com.nextthing.app.domain.usecase.TaskUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AiAssistantViewModel @Inject constructor(
    private val taskUseCases: TaskUseCases,
    private val categoryRepository: CategoryRepository,
    private val aiTaskParser: AITaskParser,
    private val aiCompletionClient: AICompletionClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiAssistantUiState())
    val uiState: StateFlow<AiAssistantUiState> = _uiState.asStateFlow()

    private var categories: List<Category> = emptyList()

    init {
        loadCategories()
        refreshRouteStatus()
    }

    fun updateInput(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text, errorMessage = null, statusMessage = null)
    }

    fun parseInput() {
        val state = _uiState.value
        val text = state.inputText.trim()
        if (text.isBlank() || state.isParsing) return

        _uiState.value = state.copy(
            isParsing = true,
            errorMessage = null,
            statusMessage = null,
            parseResults = emptyList(),
            selectedIndexes = emptySet()
        )
        viewModelScope.launch {
            val availableCategories = categories.ifEmpty { PresetCategories.getDefaultCategories() }

            aiTaskParser.parseTaskFromText(
                input = text,
                availableCategories = availableCategories.map { it.displayName }
            ).onSuccess { results ->
                _uiState.value = _uiState.value.copy(
                    isParsing = false,
                    parseResults = results,
                    selectedIndexes = results.indices.toSet(),
                    statusMessage = if (results.isEmpty()) "没有解析出可创建的任务" else null
                )
            }.onFailure { error ->
                Timber.tag("AI-Assistant").w(error, "AI assistant parse failed")
                _uiState.value = _uiState.value.copy(
                    isParsing = false,
                    errorMessage = error.message ?: "AI 解析失败，请稍后再试"
                )
            }
        }
    }

    fun toggleSelection(index: Int) {
        val current = _uiState.value.selectedIndexes
        _uiState.value = _uiState.value.copy(
            selectedIndexes = if (index in current) current - index else current + index
        )
    }

    fun saveSelectedTasks() {
        val state = _uiState.value
        val selectedResults = state.parseResults.filterIndexed { index, _ -> index in state.selectedIndexes }
        if (selectedResults.isEmpty() || state.isSaving) {
            _uiState.value = state.copy(statusMessage = "请选择要保存的任务")
            return
        }

        _uiState.value = state.copy(isSaving = true, errorMessage = null, statusMessage = null)
        viewModelScope.launch {
            var successCount = 0
            var failureCount = 0

            withContext(NonCancellable) {
                selectedResults.forEach { result ->
                    val saveResult = taskUseCases.createTask(
                        title = result.title,
                        description = result.description.orEmpty(),
                        category = resolveCategory(result.categoryName),
                        dueDate = result.dueDate,
                        repeatFrequency = result.toRepeatFrequency(),
                        importanceUrgency = result.importance
                    )
                    if (saveResult.isSuccess) {
                        successCount += 1
                    } else {
                        failureCount += 1
                        Timber.tag("AI-Assistant").w(
                            saveResult.exceptionOrNull(),
                            "Failed to save parsed task: ${result.title}"
                        )
                    }
                }
            }

            _uiState.value = _uiState.value.copy(
                isSaving = false,
                parseResults = if (failureCount == 0) emptyList() else _uiState.value.parseResults,
                selectedIndexes = if (failureCount == 0) emptySet() else _uiState.value.selectedIndexes,
                inputText = if (failureCount == 0) "" else _uiState.value.inputText,
                statusMessage = if (failureCount == 0) {
                    "已创建 $successCount 个任务"
                } else {
                    "已创建 $successCount 个任务，$failureCount 个失败"
                }
            )
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null, statusMessage = null)
    }

    fun refreshRouteStatus() {
        viewModelScope.launch {
            runCatching { aiCompletionClient.routeStatus() }
                .onSuccess { status ->
                    val routeText = when (status.mode) {
                        AIRouteMode.ExternalProvider -> "${status.provider.displayName} · ${status.model}"
                        AIRouteMode.BackendFallback -> "服务端 AI"
                        AIRouteMode.Unavailable -> "AI 未配置"
                    }
                    _uiState.value = _uiState.value.copy(routeStatusText = routeText)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(routeStatusText = "AI 状态未知")
                }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            runCatching {
                categoryRepository.initializeSystemCategories()
                categoryRepository.getAllCategories().collect { loaded ->
                    categories = loaded
                    _uiState.value = _uiState.value.copy(categoryNames = loaded.map { it.displayName })
                }
            }.onFailure { error ->
                Timber.tag("AI-Assistant").w(error, "Failed to load categories")
                categories = PresetCategories.getDefaultCategories()
                _uiState.value = _uiState.value.copy(categoryNames = categories.map { it.displayName })
            }
        }
    }

    private fun resolveCategory(name: String?): Category {
        val available = categories.ifEmpty { PresetCategories.getDefaultCategories() }
        return name?.let { categoryName ->
            available.firstOrNull { it.displayName == categoryName || it.name == categoryName }
        } ?: available.firstOrNull { it.id == PresetCategories.LIFE_ID || it.name == "生活" }
        ?: available.first()
    }

    private fun AITaskParseResult.toRepeatFrequency(): RepeatFrequency {
        val type = repeatType ?: RepeatFrequencyType.NONE
        return when (type) {
            RepeatFrequencyType.WEEKLY -> RepeatFrequency(type = type, weekdays = repeatWeekdays ?: emptySet())
            else -> RepeatFrequency(type = type)
        }
    }
}

data class AiAssistantUiState(
    val inputText: String = "",
    val isParsing: Boolean = false,
    val isSaving: Boolean = false,
    val parseResults: List<AITaskParseResult> = emptyList(),
    val selectedIndexes: Set<Int> = emptySet(),
    val categoryNames: List<String> = emptyList(),
    val routeStatusText: String = "AI 状态读取中",
    val errorMessage: String? = null,
    val statusMessage: String? = null
)
