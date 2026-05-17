package com.nextthing.app.presentation.screens.geofence.relatedtasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextthing.app.domain.model.GeofenceLocation
import com.nextthing.app.domain.model.Task
import com.nextthing.app.domain.usecase.GeofenceUseCases
import com.nextthing.app.domain.usecase.TaskUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

enum class TaskFilterTab {
    ALL,        // 全部
    INCOMPLETE, // 未完成
    COMPLETED   // 已完成
}

data class RelatedTasksUiState(
    val location: GeofenceLocation? = null,
    val allTasks: List<Task> = emptyList(),
    val filteredTasks: List<Task> = emptyList(),
    val currentTab: TaskFilterTab = TaskFilterTab.INCOMPLETE,
    val incompleteCount: Int = 0,
    val completedCount: Int = 0,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class RelatedTasksViewModel @Inject constructor(
    private val geofenceUseCases: GeofenceUseCases,
    private val taskUseCases: TaskUseCases,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val TAG = "RelatedTasks"
    }

    private val locationId: String = checkNotNull(savedStateHandle["locationId"])

    private val _uiState = MutableStateFlow(RelatedTasksUiState())
    val uiState: StateFlow<RelatedTasksUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Timber.tag(TAG).d("📋 加载关联任务数据")
                Timber.tag(TAG).d("  locationId: $locationId")

                _uiState.update { it.copy(isLoading = true) }

                // 加载地点信息
                val location = geofenceUseCases.getGeofenceLocations.getByIdOnce(locationId)
                if (location == null) {
                    Timber.tag(TAG).e("❌ 地点不存在")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "地点不存在"
                        )
                    }
                    return@launch
                }

                Timber.tag(TAG).d("✅ 地点信息: ${location.locationInfo.locationName}")

                // 加载关联任务
                combine(
                    geofenceUseCases.getTaskGeofence.getByLocationId(locationId),
                    taskUseCases.getAllTasks()
                ) { taskGeofences, allTasks ->
                    // 获取关联的任务ID列表
                    val taskIds = taskGeofences.map { it.taskId }.toSet()

                    // 筛选出关联的任务
                    val relatedTasks = allTasks.filter { it.id in taskIds }

                    Timber.tag(TAG).d("📊 关联任务数量: ${relatedTasks.size}")

                    // 统计数量
                    val incompleteCount = relatedTasks.count { it.status != com.nextthing.app.domain.model.TaskStatus.COMPLETED }
                    val completedCount = relatedTasks.count { it.status == com.nextthing.app.domain.model.TaskStatus.COMPLETED }

                    Timber.tag(TAG).d("  未完成: $incompleteCount")
                    Timber.tag(TAG).d("  已完成: $completedCount")

                    Triple(relatedTasks, incompleteCount, completedCount)
                }.collect { (tasks, incompleteCount, completedCount) ->
                    _uiState.update {
                        it.copy(
                            location = location,
                            allTasks = tasks,
                            filteredTasks = filterTasks(tasks, it.currentTab),
                            incompleteCount = incompleteCount,
                            completedCount = completedCount,
                            isLoading = false
                        )
                    }
                }

                Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "❌ 加载数据失败")
                Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "加载失败: ${e.message}"
                    )
                }
            }
        }
    }

    fun switchTab(tab: TaskFilterTab) {
        Timber.tag(TAG).d("🔄 切换标签: $tab")
        _uiState.update {
            it.copy(
                currentTab = tab,
                filteredTasks = filterTasks(it.allTasks, tab)
            )
        }
    }

    private fun filterTasks(tasks: List<Task>, tab: TaskFilterTab): List<Task> {
        return when (tab) {
            TaskFilterTab.ALL -> tasks
            TaskFilterTab.INCOMPLETE -> tasks.filter { it.status != com.nextthing.app.domain.model.TaskStatus.COMPLETED }
            TaskFilterTab.COMPLETED -> tasks.filter { it.status == com.nextthing.app.domain.model.TaskStatus.COMPLETED }
        }.sortedByDescending { it.createdAt } // 按创建时间倒序
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
