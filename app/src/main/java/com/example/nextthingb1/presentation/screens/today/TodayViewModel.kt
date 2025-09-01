package com.example.nextthingb1.presentation.screens.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextthingb1.domain.model.LocationInfo
import com.example.nextthingb1.domain.model.Task
import com.example.nextthingb1.domain.model.TaskCategory
import com.example.nextthingb1.domain.model.TaskPriority
import com.example.nextthingb1.domain.model.TaskStatus
import com.example.nextthingb1.domain.service.LocationService
import com.example.nextthingb1.domain.usecase.TaskUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject

enum class TaskTab(val title: String) {
    PENDING("待办"),
    COMPLETED("已完成")
}

data class TodayUiState(
    val allTasks: List<Task> = emptyList(),
    val displayTasks: List<Task> = emptyList(),
    val selectedTab: TaskTab = TaskTab.PENDING,
    val completionRate: Float = 0f,
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val remainingTasks: Int = 0,
    val currentLocationName: String = "点击获取位置",
    val currentLocation: LocationInfo? = null,
    val isLocationLoading: Boolean = false,
    val locationError: String? = null,
    val hasLocationPermission: Boolean = false,
    val isLocationEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val taskUseCases: TaskUseCases,
    private val locationService: LocationService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()
    
    init {
        loadTodayTasks()
        checkLocationPermissionAndStatus()
        // 移除自动位置刷新，改为用户主动触发
    }
    
    private fun loadTodayTasks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                taskUseCases.getTodayTasks().collect { tasks ->
                    val completed = tasks.filter { it.status == TaskStatus.COMPLETED }
                    val pending = tasks.filter { 
                    it.status == TaskStatus.PENDING || 
                    it.status == TaskStatus.IN_PROGRESS || 
                    it.status == TaskStatus.OVERDUE 
                }
                    
                    _uiState.value = _uiState.value.copy(
                        allTasks = tasks,
                        displayTasks = if (_uiState.value.selectedTab == TaskTab.PENDING) pending else completed,
                        totalTasks = tasks.size,
                        completedTasks = completed.size,
                        remainingTasks = pending.size,
                        completionRate = if (tasks.isNotEmpty()) completed.size.toFloat() / tasks.size else 0f,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message,
                    isLoading = false
                )
            }
        }
    }
    
    fun selectTab(tab: TaskTab) {
        val displayTasks = when (tab) {
            TaskTab.PENDING -> _uiState.value.allTasks.filter { 
                it.status == TaskStatus.PENDING || 
                it.status == TaskStatus.IN_PROGRESS || 
                it.status == TaskStatus.OVERDUE 
            }
            TaskTab.COMPLETED -> _uiState.value.allTasks.filter { 
                it.status == TaskStatus.COMPLETED 
            }
        }
        
        _uiState.value = _uiState.value.copy(
            selectedTab = tab,
            displayTasks = displayTasks
        )
    }
    
    fun toggleTaskStatus(taskId: String) {
        viewModelScope.launch {
            try {
                taskUseCases.toggleTaskStatus(taskId).fold(
                    onSuccess = {
                        // 重新加载任务列表
                        loadTodayTasks()
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
    

    fun postponeTask(taskId: String) {
        viewModelScope.launch {
            try {
                // 延期任务：将截止日期推迟一天
                val task = _uiState.value.allTasks.find { it.id == taskId }
                task?.let {
                    val newDueDate = it.dueDate?.plusDays(1) 
                        ?: java.time.LocalDateTime.now().plusDays(1)
                    
                    taskUseCases.updateTask(
                        it.copy(dueDate = newDueDate)
                    ).fold(
                        onSuccess = {
                            loadTodayTasks()
                        },
                        onFailure = { error ->
                            _uiState.value = _uiState.value.copy(
                                errorMessage = error.message
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message
                )
            }
        }
    }
    
    fun cancelTask(taskId: String) {
        viewModelScope.launch {
            try {
                val task = _uiState.value.allTasks.find { it.id == taskId }
                task?.let {
                    taskUseCases.updateTask(
                        it.copy(status = TaskStatus.CANCELLED)
                    ).fold(
                        onSuccess = {
                            loadTodayTasks()
                        },
                        onFailure = { error ->
                            _uiState.value = _uiState.value.copy(
                                errorMessage = error.message
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message
                )
            }
        }
    }
    
    fun showCreateTaskDialog() {
        // TODO: 实现创建任务对话框
    }
    
    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun requestCurrentLocation() {
        viewModelScope.launch {
            Timber.d("开始请求位置信息")
            
            // 立即设置加载状态
            _uiState.value = _uiState.value.copy(
                isLocationLoading = true,
                locationError = null,
                currentLocationName = "正在获取精确位置..."
            )
            
            // 检查权限状态
            if (!locationService.hasLocationPermission()) {
                Timber.w("位置权限检查失败")
                _uiState.value = _uiState.value.copy(
                    currentLocationName = "需要位置权限",
                    isLocationLoading = false,
                    locationError = "请授予位置权限以获取当前位置"
                )
                return@launch
            }
            
            if (!locationService.isLocationEnabled()) {
                Timber.w("位置服务检查失败")
                _uiState.value = _uiState.value.copy(
                    currentLocationName = "请开启位置服务",
                    isLocationLoading = false,
                    locationError = "请在设置中开启位置服务"
                )
                return@launch
            }
            
            Timber.d("权限和服务检查通过，开始获取位置")
            
            try {
                // 添加超时保护，避免一直加载
                withTimeoutOrNull(15000) { // 15秒超时
                    locationService.getCurrentLocation(forceRefresh = true)
                }?.fold(
                    onSuccess = { location ->
                        _uiState.value = _uiState.value.copy(
                            currentLocation = location,
                            currentLocationName = location.locationName,
                            isLocationLoading = false,
                            locationError = null
                        )
                        // 显示位置更新提示
                        showLocationTooltip()
                        Timber.d("位置更新成功: ${location.locationName}")
                    },
                    onFailure = { error ->
                        val errorMsg = when {
                            error is SecurityException -> "需要位置权限"
                            error is IllegalStateException -> "请开启位置服务"
                            error.message?.contains("超时") == true -> "位置获取超时"
                            error.message?.contains("GPS") == true -> "GPS信号弱"
                            error.message?.contains("首次使用") == true -> "首次GPS定位"
                            else -> "获取位置失败"
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            currentLocationName = errorMsg,
                            isLocationLoading = false,
                            locationError = error.message
                        )
                        Timber.w(error, "位置获取失败: $errorMsg")
                        
                        // 显示位置获取帮助对话框
                        _showLocationHelpDialog.value = true
                    }
                ) ?: run {
                    // 超时处理
                    _uiState.value = _uiState.value.copy(
                        currentLocationName = "位置获取超时",
                        isLocationLoading = false,
                        locationError = "位置获取超时，请检查GPS信号或稍后重试"
                    )
                    Timber.w("位置获取操作超时")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    currentLocationName = "位置获取异常",
                    isLocationLoading = false,
                    locationError = e.message
                )
                Timber.e(e, "位置获取异常")
            }
        }
    }
    
    /**
     * 显示权限对话框状态
     */
    private val _showPermissionDialog = MutableStateFlow(false)
    val showPermissionDialog: StateFlow<Boolean> = _showPermissionDialog.asStateFlow()
    
    /**
     * 显示位置详情对话框状态
     */
    private val _showLocationDetailDialog = MutableStateFlow(false)
    val showLocationDetailDialog: StateFlow<Boolean> = _showLocationDetailDialog.asStateFlow()
    
    /**
     * 显示位置提示状态
     */
    private val _showLocationTooltip = MutableStateFlow(false)
    val showLocationTooltip: StateFlow<Boolean> = _showLocationTooltip.asStateFlow()
    
    /**
     * 显示位置帮助对话框状态
     */
    private val _showLocationHelpDialog = MutableStateFlow(false)
    val showLocationHelpDialog: StateFlow<Boolean> = _showLocationHelpDialog.asStateFlow()
    
    /**
     * 请求位置权限
     */
    fun requestLocationPermission() {
        _showPermissionDialog.value = true
        Timber.d("显示位置权限请求对话框")
    }
    
    /**
     * 强制检查权限状态并刷新
     */
    fun forceCheckPermissionsAndRefresh() {
        viewModelScope.launch {
            checkLocationPermissionAndStatus()
            // 只检查权限状态，不自动获取位置
            if (_uiState.value.hasLocationPermission && _uiState.value.isLocationEnabled) {
                // 有权限时更新状态为可点击
                if (_uiState.value.currentLocationName == "需要位置权限" || 
                    _uiState.value.currentLocationName == "请开启位置服务") {
                    _uiState.value = _uiState.value.copy(
                        currentLocationName = "点击获取位置",
                        isLocationLoading = false
                    )
                }
            }
        }
    }
    
    /**
     * 隐藏权限对话框
     */
    fun hidePermissionDialog() {
        _showPermissionDialog.value = false
    }
    
    /**
     * 显示位置详情对话框
     */
    fun showLocationDetail() {
        _showLocationDetailDialog.value = true
    }
    
    /**
     * 隐藏位置详情对话框
     */
    fun hideLocationDetailDialog() {
        _showLocationDetailDialog.value = false
    }
    
    /**
     * 显示位置提示
     */
    fun showLocationTooltip() {
        viewModelScope.launch {
            _showLocationTooltip.value = true
            // 3秒后自动隐藏
            kotlinx.coroutines.delay(3000)
            _showLocationTooltip.value = false
        }
    }
    
    /**
     * 隐藏位置提示
     */
    fun hideLocationTooltip() {
        _showLocationTooltip.value = false
    }
    
    /**
     * 显示位置帮助对话框
     */
    fun showLocationHelpDialog() {
        _showLocationHelpDialog.value = true
    }
    
    /**
     * 隐藏位置帮助对话框
     */
    fun hideLocationHelpDialog() {
        _showLocationHelpDialog.value = false
    }
    

    
    private fun checkLocationPermissionAndStatus() {
        viewModelScope.launch {
            val hasPermission = locationService.hasLocationPermission()
            val isEnabled = locationService.isLocationEnabled()
            
            _uiState.value = _uiState.value.copy(
                hasLocationPermission = hasPermission,
                isLocationEnabled = isEnabled
            )
            
            if (!hasPermission) {
                _uiState.value = _uiState.value.copy(
                    currentLocationName = "需要位置权限",
                    isLocationLoading = false
                )
            } else if (!isEnabled) {
                _uiState.value = _uiState.value.copy(
                    currentLocationName = "请开启位置服务",
                    isLocationLoading = false
                )
            } else if (_uiState.value.currentLocation == null) {
                // 有权限但没有位置信息时，显示可点击状态
                _uiState.value = _uiState.value.copy(
                    currentLocationName = "点击获取位置",
                    isLocationLoading = false
                )
            }
        }
    }
    

    
    /**
     * 当用户切换到首页时调用
     */
    fun onScreenResumed() {
        val oldPermission = _uiState.value.hasLocationPermission
        val oldLocationEnabled = _uiState.value.isLocationEnabled
        
        checkLocationPermissionAndStatus()
        
        // 如果权限状态发生变化，立即处理
        val newPermission = _uiState.value.hasLocationPermission
        val newLocationEnabled = _uiState.value.isLocationEnabled
        
        if (!oldPermission && newPermission && newLocationEnabled) {
            // 权限刚被授予，更新状态但不自动获取位置
            Timber.d("检测到权限刚被授予，更新位置状态")
            _uiState.value = _uiState.value.copy(
                currentLocationName = "点击获取位置",
                isLocationLoading = false
            )
        } else if (newPermission && newLocationEnabled) {
            // 有权限时检查缓存
            refreshLocationIfNeeded()
        }
    }
    
    /**
     * 公开方法：刷新位置信息（如果需要）
     */
    fun refreshLocationIfNeeded() {
        viewModelScope.launch {
            try {
                // 先尝试获取缓存位置
                val cachedLocation = locationService.getCachedLocation()
                if (cachedLocation != null) {
                    _uiState.value = _uiState.value.copy(
                        currentLocation = cachedLocation,
                        currentLocationName = cachedLocation.locationName
                    )
                    return@launch
                }
                
                // 如果没有缓存位置且需要刷新，显示提示用户点击获取
                if (cachedLocation == null && locationService.shouldRefreshLocation()) {
                    _uiState.value = _uiState.value.copy(
                        currentLocationName = "点击获取位置"
                    )
                }
            } catch (e: Exception) {
                Timber.w(e, "位置刷新检查失败")
            }
        }
    }
} 