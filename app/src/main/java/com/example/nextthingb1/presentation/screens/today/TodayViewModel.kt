package com.example.nextthingb1.presentation.screens.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextthingb1.domain.model.LocationInfo
import com.example.nextthingb1.domain.model.Task
import com.example.nextthingb1.domain.model.TaskCategory
import com.example.nextthingb1.domain.model.TaskStatus
import com.example.nextthingb1.domain.model.WeatherInfo
import com.example.nextthingb1.domain.service.LocationService
import com.example.nextthingb1.domain.service.WeatherService
import com.example.nextthingb1.domain.usecase.TaskUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject
import com.example.nextthingb1.domain.model.TaskTab
import java.time.LocalDateTime

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
    val errorMessage: String? = null,
    val weatherInfo: WeatherInfo? = null,
    val isWeatherLoading: Boolean = false,
    val weatherError: String? = null,
    val showCancelReasonDialog: Boolean = false,
    val cancelTaskId: String? = null,
    val showPostponeReasonDialog: Boolean = false,
    val postponeTaskId: String? = null
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val taskUseCases: TaskUseCases,
    private val locationService: LocationService,
    private val weatherService: WeatherService
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    init {
        // 生成今日重复任务实例
        generateTodayRecurringTasks()
        // 启动持续的任务数据监听
        startTasksObserver()
        // 串行初始化：权限检查 → 加载DB缓存 → 开始位置获取，避免竞态条件
        initLocationFlow()
    }

    /**
     * 生成今日重复任务实例
     */
    private fun generateTodayRecurringTasks() {
        viewModelScope.launch {
            try {
                Timber.tag("RecurringTask").d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Timber.tag("RecurringTask").d("【TodayViewModel】开始生成今日重复任务")

                taskUseCases.generateRecurringTasks(java.time.LocalDate.now()).fold(
                    onSuccess = { count ->
                        Timber.tag("RecurringTask").d("✅ 成功生成 $count 个重复任务实例")
                        Timber.tag("RecurringTask").d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    },
                    onFailure = { error ->
                        Timber.tag("RecurringTask").e("❌ 生成重复任务失败: ${error.message}")
                        Timber.tag("RecurringTask").d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    }
                )
            } catch (e: Exception) {
                Timber.tag("RecurringTask").e(e, "💥 生成重复任务异常")
            }
        }
    }

    // 位置缓存
    private var cachedLocationInfo: LocationInfo? = null
    private var lastLocationUpdateTime: Long = 0
    private val LOCATION_CACHE_DURATION = 5 * 60 * 1000L // 5分钟缓存

    // 经纬度变化阈值（约110米）
    private val LOCATION_CHANGE_THRESHOLD = 0.001

    // 初始化标志，防止初始化期间触发重复更新
    private var isInitializing = true
    
    /**
     * 启动任务数据持续监听
     * 使用 viewModelScope 确保在 ViewModel 生命周期内持续订阅
     */
    private fun startTasksObserver() {
        viewModelScope.launch {
            Timber.tag("DataFlow").d("━━━━━━ TodayViewModel.startTasksObserver ━━━━━━")
            _uiState.value = _uiState.value.copy(isLoading = true)
            Timber.tag("DataFlow").d("开始加载今日任务，isLoading=true")

            try {
                Timber.tag("DataFlow").d("建立持久 Flow 订阅：taskUseCases.getTodayTasks()")
                taskUseCases.getTodayTasks().collect { tasks ->
                    Timber.tag("DataFlow").d("━━━━━━ Flow回调收到数据 ━━━━━━")
                    Timber.tag("DataFlow").d("📊 收到 ${tasks.size} 个今日任务")
                    Timber.tag("DataFlow").d("当前 Thread: ${Thread.currentThread().name}")

                    // 🔍 检查并更新逾期任务状态
                    val now = LocalDateTime.now()
                    val tasksToUpdate = mutableListOf<Task>()

                    tasks.forEachIndexed { index, task ->
                        Timber.tag("DataFlow").d("  [$index] ${task.title} | status=${task.status} | id=${task.id.take(8)}")

                        // 检查是否应该标记为逾期
                        if (task.status == TaskStatus.PENDING &&
                            task.dueDate != null &&
                            now.isAfter(task.dueDate.plusMinutes(5))) {

                            Timber.tag("DataFlow").w("  ⚠️ 任务已逾期: ${task.title}, 截止时间: ${task.dueDate}")
                            tasksToUpdate.add(task.copy(status = TaskStatus.OVERDUE))
                        }
                    }

                    // 批量更新逾期任务（异步更新，不阻塞当前 Flow）
                    if (tasksToUpdate.isNotEmpty()) {
                        Timber.tag("DataFlow").d("🔄 发现 ${tasksToUpdate.size} 个任务需要更新为逾期状态")
                        viewModelScope.launch {
                            tasksToUpdate.forEach { task ->
                                try {
                                    taskUseCases.updateTask(task)
                                    Timber.tag("DataFlow").d("  ✅ 已更新: ${task.title} → OVERDUE")
                                } catch (e: Exception) {
                                    Timber.tag("DataFlow").e(e, "  ❌ 更新失败: ${task.title}")
                                }
                            }
                        }
                        // 不 return，继续处理当前数据，Flow 会在更新后自动触发下一次
                    }

                    val completed = tasks.filter { it.status == TaskStatus.COMPLETED }
                    val pending = tasks.filter {
                        it.status == TaskStatus.PENDING || it.status == TaskStatus.OVERDUE || it.status == TaskStatus.DELAYED
                    }

                    Timber.tag("DataFlow").d("📊 筛选结果: 已完成=${completed.size}, 待办=${pending.size}")

                    _uiState.value = _uiState.value.copy(
                        allTasks = tasks,
                        displayTasks = if (_uiState.value.selectedTab == TaskTab.PENDING) pending else completed,
                        totalTasks = tasks.size,
                        completedTasks = completed.size,
                        remainingTasks = pending.size,
                        completionRate = if (tasks.isNotEmpty()) completed.size.toFloat() / tasks.size else 0f,
                        isLoading = false
                    )

                    Timber.tag("DataFlow").d("✅ UI状态已更新: totalTasks=${tasks.size}, displayTasks=${_uiState.value.displayTasks.size}")
                }
            } catch (e: Exception) {
                Timber.tag("DataFlow").e(e, "❌ 加载今日任务失败")
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
                it.status == TaskStatus.PENDING || it.status == TaskStatus.OVERDUE || it.status == TaskStatus.DELAYED
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
                Timber.tag("TaskToggle").d("━━━━━━ 开始切换任务状态 ━━━━━━")
                Timber.tag("TaskToggle").d("taskId: ${taskId.take(8)}")

                // 记录切换前的状态
                val taskBefore = _uiState.value.allTasks.find { it.id == taskId }
                Timber.tag("TaskToggle").d("切换前: ${taskBefore?.title} | status=${taskBefore?.status}")
                Timber.tag("TaskToggle").d("当前 Tab: ${_uiState.value.selectedTab}")
                Timber.tag("TaskToggle").d("当前 displayTasks 数量: ${_uiState.value.displayTasks.size}")

                taskUseCases.toggleTaskStatus(taskId).fold(
                    onSuccess = {
                        Timber.tag("TaskToggle").d("✅ UseCase 执行成功")
                        Timber.tag("TaskToggle").d("等待 Room Flow 自动触发更新...")

                        // 延迟检查更新是否生效
                        viewModelScope.launch {
                            delay(500)  // 等待 500ms
                            val taskAfter = _uiState.value.allTasks.find { it.id == taskId }
                            Timber.tag("TaskToggle").d("500ms后检查: ${taskAfter?.title} | status=${taskAfter?.status}")

                            if (taskAfter?.status == taskBefore?.status) {
                                Timber.tag("TaskToggle").w("⚠️ 警告：500ms后状态未变化！Flow可能未触发")
                            } else {
                                Timber.tag("TaskToggle").d("✅ 状态已更新")
                            }
                        }
                    },
                    onFailure = { error ->
                        Timber.tag("TaskToggle").e("❌ UseCase 执行失败: ${error.message}")
                        _uiState.value = _uiState.value.copy(
                            errorMessage = error.message
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.tag("TaskToggle").e(e, "❌ 异常: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message
                )
            }
        }
    }
    

    /**
     * 显示延期任务对话框
     */
    fun showPostponeReasonDialog(taskId: String) {
        _uiState.value = _uiState.value.copy(
            showPostponeReasonDialog = true,
            postponeTaskId = taskId
        )
    }

    /**
     * 隐藏延期任务对话框
     */
    fun hidePostponeReasonDialog() {
        _uiState.value = _uiState.value.copy(
            showPostponeReasonDialog = false,
            postponeTaskId = null
        )
    }

    /**
     * 确认延期任务，并将延期原因追加到任务描述中
     */
    fun confirmPostponeTask(reason: String) {
        viewModelScope.launch {
            try {
                val taskId = _uiState.value.postponeTaskId
                if (taskId == null) {
                    Timber.w("confirmPostponeTask: taskId 为空")
                    return@launch
                }

                val task = _uiState.value.allTasks.find { it.id == taskId }
                if (task == null) {
                    Timber.w("confirmPostponeTask: 找不到任务 $taskId")
                    return@launch
                }

                // 将延期原因追加到任务描述中
                val updatedDescription = if (task.description.isBlank()) {
                    "【延期原因】\n$reason"
                } else {
                    "${task.description}\n\n【延期原因】\n$reason"
                }

                Timber.d("延期任务: ${task.title}, 原因: $reason")

                // 先更新描述
                taskUseCases.updateTask(
                    task.copy(
                        description = updatedDescription,
                        updatedAt = LocalDateTime.now()
                    )
                ).fold(
                    onSuccess = {
                        // 更新描述成功后，再调用延期任务 UseCase
                        taskUseCases.deferTask(taskId).fold(
                            onSuccess = {
                                Timber.d("任务已延期: ${task.title}")
                                hidePostponeReasonDialog()
                                // Flow 会自动更新，不需要手动刷新
                            },
                            onFailure = { error ->
                                Timber.e("延期任务失败: ${error.message}")
                                _uiState.value = _uiState.value.copy(
                                    errorMessage = error.message
                                )
                                hidePostponeReasonDialog()
                            }
                        )
                    },
                    onFailure = { error ->
                        Timber.e("更新任务描述失败: ${error.message}")
                        _uiState.value = _uiState.value.copy(
                            errorMessage = error.message
                        )
                        hidePostponeReasonDialog()
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "延期任务异常")
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message
                )
                hidePostponeReasonDialog()
            }
        }
    }
    
    /**
     * 显示放弃任务对话框
     */
    fun showCancelReasonDialog(taskId: String) {
        _uiState.value = _uiState.value.copy(
            showCancelReasonDialog = true,
            cancelTaskId = taskId
        )
    }

    /**
     * 隐藏放弃任务对话框
     */
    fun hideCancelReasonDialog() {
        _uiState.value = _uiState.value.copy(
            showCancelReasonDialog = false,
            cancelTaskId = null
        )
    }

    /**
     * 确认放弃任务，并将放弃原因追加到任务描述中
     */
    fun confirmCancelTask(reason: String) {
        viewModelScope.launch {
            try {
                val taskId = _uiState.value.cancelTaskId
                if (taskId == null) {
                    Timber.w("confirmCancelTask: taskId 为空")
                    return@launch
                }

                val task = _uiState.value.allTasks.find { it.id == taskId }
                if (task == null) {
                    Timber.w("confirmCancelTask: 找不到任务 $taskId")
                    return@launch
                }

                // 将放弃原因追加到任务描述中
                val updatedDescription = if (task.description.isBlank()) {
                    "【放弃原因】\n$reason"
                } else {
                    "${task.description}\n\n【放弃原因】\n$reason"
                }

                Timber.d("放弃任务: ${task.title}, 原因: $reason")

                // 更新任务状态为已取消，并更新描述
                taskUseCases.updateTask(
                    task.copy(
                        status = TaskStatus.CANCELLED,
                        description = updatedDescription,
                        updatedAt = LocalDateTime.now()
                    )
                ).fold(
                    onSuccess = {
                        Timber.d("任务已放弃: ${task.title}")
                        hideCancelReasonDialog()
                        // Flow 会自动更新，不需要手动刷新
                    },
                    onFailure = { error ->
                        Timber.e("放弃任务失败: ${error.message}")
                        _uiState.value = _uiState.value.copy(
                            errorMessage = error.message
                        )
                        hideCancelReasonDialog()
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "放弃任务异常")
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message
                )
                hideCancelReasonDialog()
            }
        }
    }
    
    /**
     * 直接执行延期任务（粒子动画结束后调用，不依赖 uiState 中的 postponeTaskId）
     */
    fun executePostponeTask(taskId: String, reason: String) {
        viewModelScope.launch {
            try {
                val task = _uiState.value.allTasks.find { it.id == taskId }
                if (task == null) {
                    Timber.w("executePostponeTask: 找不到任务 $taskId")
                    return@launch
                }

                val updatedDescription = if (task.description.isBlank()) {
                    "【延期原因】\n$reason"
                } else {
                    "${task.description}\n\n【延期原因】\n$reason"
                }

                Timber.d("延期任务: ${task.title}, 原因: $reason")

                taskUseCases.updateTask(
                    task.copy(
                        description = updatedDescription,
                        updatedAt = LocalDateTime.now()
                    )
                ).fold(
                    onSuccess = {
                        taskUseCases.deferTask(taskId).fold(
                            onSuccess = {
                                Timber.d("任务已延期: ${task.title}")
                            },
                            onFailure = { error ->
                                Timber.e("延期任务失败: ${error.message}")
                                _uiState.value = _uiState.value.copy(errorMessage = error.message)
                            }
                        )
                    },
                    onFailure = { error ->
                        Timber.e("更新任务描述失败: ${error.message}")
                        _uiState.value = _uiState.value.copy(errorMessage = error.message)
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "延期任务异常")
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    /**
     * 直接执行放弃任务（粒子动画结束后调用，不依赖 uiState 中的 cancelTaskId）
     */
    fun executeCancelTask(taskId: String, reason: String) {
        viewModelScope.launch {
            try {
                val task = _uiState.value.allTasks.find { it.id == taskId }
                if (task == null) {
                    Timber.w("executeCancelTask: 找不到任务 $taskId")
                    return@launch
                }

                val updatedDescription = if (task.description.isBlank()) {
                    "【放弃原因】\n$reason"
                } else {
                    "${task.description}\n\n【放弃原因】\n$reason"
                }

                Timber.d("放弃任务: ${task.title}, 原因: $reason")

                taskUseCases.updateTask(
                    task.copy(
                        status = TaskStatus.CANCELLED,
                        description = updatedDescription,
                        updatedAt = LocalDateTime.now()
                    )
                ).fold(
                    onSuccess = {
                        Timber.d("任务已放弃: ${task.title}")
                    },
                    onFailure = { error ->
                        Timber.e("放弃任务失败: ${error.message}")
                        _uiState.value = _uiState.value.copy(errorMessage = error.message)
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "放弃任务异常")
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    fun showCreateTaskDialog() {
        // 快速创建任务功能为可选增强,当前通过底部导航栏"创建"按钮跳转
        // 可扩展:在首页添加快速创建对话框,简化操作流程
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
                currentLocationName = "正在获取位置..."
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
                // 优先网络定位，再GPS定位
                val location = withTimeoutOrNull(35000) { // 35秒超时
                    locationService.getCurrentLocation(forceRefresh = true)
                }

                if (location != null) {
                    // 位置获取成功，更新缓存和UI
                    updateLocationCache(location)
                    _uiState.value = _uiState.value.copy(
                        currentLocation = location,
                        currentLocationName = location.locationName,
                        isLocationLoading = false,
                        locationError = null
                    )
                    Timber.d("手动位置获取成功: ${location.locationName}")

                    // 显示位置更新提示
                    showLocationTooltip()
                } else {
                    // 位置获取失败或超时
                    val errorMsg = "获取位置失败"

                    _uiState.value = _uiState.value.copy(
                        currentLocationName = errorMsg,
                        isLocationLoading = false,
                        locationError = "位置获取失败，请检查权限和位置服务"
                    )
                    Timber.w("手动位置获取失败: $errorMsg")

                    // 显示位置获取帮助对话框
                    _showLocationHelpDialog.value = true
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    currentLocationName = "位置获取异常",
                    isLocationLoading = false,
                    locationError = e.message
                )
                Timber.e(e, "手动位置获取异常")
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
     * 串行初始化位置流程：权限检查 → 加载DB缓存 → 开始位置获取
     * 保证 doLoadCachedFromDB() 完成后才执行 doAutoStartLocationUpdate()，避免竞态条件
     */
    private fun initLocationFlow() {
        viewModelScope.launch {
            performPermissionCheck()
            doLoadCachedFromDB()
            doAutoStartLocationUpdate()
        }
    }

    /**
     * 请求位置权限
     */
    fun requestLocationPermission() {
        _showPermissionDialog.value = true
        Timber.d("显示位置权限请求对话框")
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

    /**
     * 获取天气信息
     */
    fun loadWeatherInfo(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            Timber.d("🌤️ [TodayViewModel] 开始获取天气信息，强制刷新: $forceRefresh")
            
            val currentLocation = _uiState.value.currentLocation
            if (currentLocation == null) {
                Timber.w("❌ [TodayViewModel] 位置信息为空，无法获取天气")
                _uiState.value = _uiState.value.copy(
                    weatherError = "需要位置信息才能获取天气"
                )
                return@launch
            }
            
            Timber.d("📍 [TodayViewModel] 使用位置: ${currentLocation.locationName} (${currentLocation.latitude}, ${currentLocation.longitude})")
            
            _uiState.value = _uiState.value.copy(
                isWeatherLoading = true,
                weatherError = null
            )
            
            try {
                weatherService.getCurrentWeather(currentLocation, forceRefresh).fold(
                    onSuccess = { weatherInfo ->
                        Timber.d("✅ [TodayViewModel] 天气信息获取成功: ${weatherInfo.condition.displayName}, ${weatherInfo.temperature}°C, 湿度: ${weatherInfo.humidity}%")
                        _uiState.value = _uiState.value.copy(
                            weatherInfo = weatherInfo,
                            isWeatherLoading = false,
                            weatherError = null
                        )
                    },
                    onFailure = { error ->
                        Timber.e(error, "❌ [TodayViewModel] 天气信息获取失败")
                        _uiState.value = _uiState.value.copy(
                            isWeatherLoading = false,
                            weatherError = error.message ?: "获取天气信息失败"
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "💥 [TodayViewModel] 天气信息获取异常")
                _uiState.value = _uiState.value.copy(
                    isWeatherLoading = false,
                    weatherError = e.message ?: "获取天气信息异常"
                )
            }
        }
    }

    /**
     * 清除天气错误信息
     */
    fun clearWeatherError() {
        _uiState.value = _uiState.value.copy(weatherError = null)
    }
    

    
    private fun checkLocationPermissionAndStatus() {
        viewModelScope.launch { performPermissionCheck() }
    }

    private suspend fun performPermissionCheck() {
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
            if (isCacheValid()) {
                _uiState.value = _uiState.value.copy(
                    currentLocation = cachedLocationInfo,
                    currentLocationName = cachedLocationInfo?.locationName ?: "未知位置",
                    isLocationLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    currentLocationName = "正在获取位置...",
                    isLocationLoading = true
                )
            }
        }
    }
    

    
    /**
     * 当用户切换到首页时调用
     */
    fun onScreenResumed() {
        Timber.tag("LocationUpdate").d("━━━━━ onScreenResumed 被调用 ━━━━━")

        if (isInitializing) {
            Timber.tag("LocationUpdate").w("⏭️ ViewModel正在初始化中，跳过onScreenResumed更新")
            Timber.tag("LocationUpdate").d("━━━━━ onScreenResumed 结束（跳过）━━━━━")
            return
        }

        viewModelScope.launch {
            val oldPermission = _uiState.value.hasLocationPermission
            val oldLocationEnabled = _uiState.value.isLocationEnabled

            Timber.tag("LocationUpdate").d("旧权限状态: hasPermission=$oldPermission, isEnabled=$oldLocationEnabled")

            // await 权限检查，确保读到最新状态
            performPermissionCheck()

            val newPermission = _uiState.value.hasLocationPermission
            val newLocationEnabled = _uiState.value.isLocationEnabled

            Timber.tag("LocationUpdate").d("新权限状态: hasPermission=$newPermission, isEnabled=$newLocationEnabled")

            if (!oldPermission && newPermission && newLocationEnabled) {
                Timber.tag("LocationUpdate").d("🆕 权限刚被授予 → 调用 doAutoStartLocationUpdate()")
                doAutoStartLocationUpdate()
            } else if (newPermission && newLocationEnabled) {
                Timber.tag("LocationUpdate").d("✅ 有权限 → 调用 silentLocationUpdate()")
                silentLocationUpdate()
            } else {
                Timber.tag("LocationUpdate").w("⚠️ 权限不足，跳过位置更新")
            }

            Timber.tag("LocationUpdate").d("━━━━━ onScreenResumed 结束 ━━━━━")
        }
    }
    
    /**
     * 公开方法：刷新位置信息（如果需要）
     */
    fun refreshLocationIfNeeded() {
        viewModelScope.launch {
            try {
                // 先检查缓存是否有效
                if (isCacheValid()) {
                    Timber.d("缓存位置有效，无需刷新")
                    return@launch
                }
                
                // 缓存无效，进行静默更新
                silentLocationUpdate()
            } catch (e: Exception) {
                Timber.w(e, "位置刷新检查失败")
            }
        }
    }

    /**
     * 自动开始位置更新
     */
    private fun autoStartLocationUpdate() {
        viewModelScope.launch { doAutoStartLocationUpdate() }
    }

    private suspend fun doAutoStartLocationUpdate() {
        Timber.tag("LocationUpdate").d("━━━━━ autoStartLocationUpdate 开始 ━━━━━")
        Timber.tag("LocationUpdate").d("调用来源: 应用初始化或权限授予")

        if (!locationService.hasLocationPermission()) {
            Timber.tag("LocationUpdate").w("❌ 无位置权限，等待用户授权")
            isInitializing = false
            return
        }

        if (!locationService.isLocationEnabled()) {
            Timber.tag("LocationUpdate").w("❌ 位置服务未启用，等待用户开启")
            isInitializing = false
            return
        }

        Timber.tag("LocationUpdate").d("✅ 权限和服务检查通过")

        if (isCacheValid()) {
            Timber.tag("LocationUpdate").d("✅ 缓存有效(<5分钟)，使用缓存位置: ${cachedLocationInfo?.locationName}")
            _uiState.value = _uiState.value.copy(
                currentLocation = cachedLocationInfo,
                currentLocationName = cachedLocationInfo?.locationName ?: "未知位置",
                isLocationLoading = false
            )
            if (_uiState.value.weatherInfo == null || weatherService.shouldRefreshWeather()) {
                Timber.tag("LocationUpdate").d("🌤️ 使用缓存位置，开始调用天气服务...")
                loadWeatherInfo()
            }
            isInitializing = false
            Timber.tag("LocationUpdate").d("🏁 初始化完成（使用缓存）")
            Timber.tag("LocationUpdate").d("━━━━━ autoStartLocationUpdate 结束（使用缓存）━━━━━")
            return
        }

        if (cachedLocationInfo == null) {
            Timber.tag("LocationUpdate").d("⚠️ 无缓存数据，首次获取位置（显示加载动画）")
            startLocationAcquisition()
            Timber.tag("LocationUpdate").d("━━━━━ autoStartLocationUpdate 结束（首次获取）━━━━━")
        } else {
            val cacheAge = (System.currentTimeMillis() - lastLocationUpdateTime) / 1000
            Timber.tag("LocationUpdate").d("⚠️ 缓存已过期(${cacheAge}秒 > 300秒)，进行静默更新")
            Timber.tag("LocationUpdate").d("   当前缓存: ${cachedLocationInfo?.locationName}")

            _uiState.value = _uiState.value.copy(
                currentLocation = cachedLocationInfo,
                currentLocationName = cachedLocationInfo?.locationName ?: "未知位置",
                isLocationLoading = false,
                locationError = null
            )

            isInitializing = false
            Timber.tag("LocationUpdate").d("🏁 初始化完成（显示缓存），后台静默获取新位置")

            viewModelScope.launch {
                try {
                    Timber.tag("LocationUpdate").d("📡 后台静默调用 locationService.getCurrentLocation()")
                    val newLocation = locationService.getCurrentLocation(forceRefresh = false)

                    if (newLocation != null) {
                        val addressChanged = isLocationAddressChanged(cachedLocationInfo, newLocation)
                        Timber.tag("LocationUpdate").d("📊 地址对比: ${if (addressChanged) "已变化" else "未变化"}")
                        updateLocationCache(newLocation)
                        if (addressChanged) {
                            Timber.tag("LocationUpdate").d("🔄 地址变化，更新UI")
                            _uiState.value = _uiState.value.copy(
                                currentLocation = newLocation,
                                currentLocationName = newLocation.locationName,
                                isLocationLoading = false,
                                locationError = null
                            )
                            silentWeatherUpdate(newLocation)
                        } else {
                            Timber.tag("LocationUpdate").d("➡️ 地址未变化，仅更新缓存")
                        }
                    } else {
                        Timber.tag("LocationUpdate").w("❌ 后台获取失败，保持显示缓存")
                    }
                } catch (e: Exception) {
                    Timber.tag("LocationUpdate").e(e, "💥 后台获取异常，保持显示缓存")
                }
            }

            Timber.tag("LocationUpdate").d("━━━━━ autoStartLocationUpdate 结束（静默更新）━━━━━")
        }
    }
    
    /**
     * 开始位置获取（首次获取或缓存无效时）
     */
    private fun startLocationAcquisition() {
        viewModelScope.launch {
            Timber.tag("LocationUpdate").d("━━━━━ startLocationAcquisition 开始 ━━━━━")
            Timber.tag("LocationUpdate").d("📍 开始首次位置获取（显示加载动画）")

            _uiState.value = _uiState.value.copy(
                isLocationLoading = true,
                currentLocationName = "正在获取位置...",
                locationError = null
            )
            Timber.tag("LocationUpdate").d("✅ UI已设置为加载状态")

            try {
                Timber.tag("LocationUpdate").d("📡 调用 locationService.getCurrentLocation(forceRefresh=true)")
                // 优先网络定位，再GPS定位
                val location = withTimeoutOrNull(35000) { // 35秒超时
                    locationService.getCurrentLocation(forceRefresh = true)
                }

                if (location != null) {
                    Timber.tag("LocationUpdate").d("✅ 位置获取成功:")
                    Timber.tag("LocationUpdate").d("   地址: ${location.locationName}")
                    Timber.tag("LocationUpdate").d("   经纬度: (${location.latitude}, ${location.longitude})")

                    // 位置获取成功，更新缓存和UI
                    updateLocationCache(location)

                    _uiState.value = _uiState.value.copy(
                        currentLocation = location,
                        currentLocationName = location.locationName,
                        isLocationLoading = false,
                        locationError = null
                    )
                    Timber.tag("LocationUpdate").d("✅ UI已更新显示新位置（关闭加载动画）")

                    // 自动获取天气信息
                    Timber.tag("LocationUpdate").d("🌤️ 位置获取成功，开始调用天气服务...")
                    loadWeatherInfo()
                } else {
                    // 位置获取失败或超时
                    Timber.tag("LocationUpdate").w("❌ 位置获取超时（返回null）")

                    _uiState.value = _uiState.value.copy(
                        currentLocationName = "获取位置失败",
                        isLocationLoading = false,
                        locationError = "位置获取超时，请检查GPS信号或稍后重试"
                    )
                }
            } catch (e: Exception) {
                Timber.tag("LocationUpdate").e(e, "💥 位置获取异常: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    currentLocationName = "位置获取异常",
                    isLocationLoading = false,
                    locationError = e.message
                )
            } finally {
                // 位置获取完成（成功或失败），清除初始化标志
                if (isInitializing) {
                    isInitializing = false
                    Timber.tag("LocationUpdate").d("🏁 初始化完成（位置获取结束），允许onScreenResumed触发更新")
                }
                Timber.tag("LocationUpdate").d("━━━━━ startLocationAcquisition 结束 ━━━━━")
            }
        }
    }
    
    private fun loadCachedLocationFromDatabase() {
        viewModelScope.launch { doLoadCachedFromDB() }
    }

    private suspend fun doLoadCachedFromDB() {
        Timber.tag("LocationUpdate").d("━━━━━ 从数据库加载缓存位置 ━━━━━")
        try {
            val currentLocation = taskUseCases.locationRepository.getCurrentLocation()
            if (currentLocation != null) {
                Timber.tag("LocationUpdate").d("✅ 数据库中找到缓存位置:")
                Timber.tag("LocationUpdate").d("   地址: ${currentLocation.locationName}")
                Timber.tag("LocationUpdate").d("   经纬度: (${currentLocation.latitude}, ${currentLocation.longitude})")

                cachedLocationInfo = currentLocation
                // 使用位置记录的实际更新时间，避免伪造新鲜度
                lastLocationUpdateTime = currentLocation.updatedAt
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                Timber.tag("LocationUpdate").d("   已更新内存缓存（使用实际时间戳）")

                _uiState.value = _uiState.value.copy(
                    currentLocation = currentLocation,
                    currentLocationName = currentLocation.locationName,
                    isLocationLoading = false
                )
                Timber.tag("LocationUpdate").d("✅ UI已更新显示数据库缓存位置")

                if (_uiState.value.weatherInfo == null || weatherService.shouldRefreshWeather()) {
                    Timber.tag("LocationUpdate").d("🌤️ 使用数据库缓存位置，开始调用天气服务...")
                    loadWeatherInfo()
                }
            } else {
                Timber.tag("LocationUpdate").d("⚠️ 数据库中没有缓存位置")
            }
        } catch (e: Exception) {
            Timber.tag("LocationUpdate").e(e, "❌ 从数据库加载缓存位置失败: ${e.message}")
        } finally {
            Timber.tag("LocationUpdate").d("━━━━━ 数据库加载结束 ━━━━━")
        }
    }

    /**
     * 检查缓存是否有效
     */
    private fun isCacheValid(): Boolean {
        val hasCache = cachedLocationInfo != null
        val cacheAge = System.currentTimeMillis() - lastLocationUpdateTime
        val isValid = hasCache && cacheAge < LOCATION_CACHE_DURATION

        Timber.tag("LocationUpdate").v("🔍 缓存有效性检查:")
        Timber.tag("LocationUpdate").v("   hasCache=$hasCache")
        if (hasCache) {
            Timber.tag("LocationUpdate").v("   缓存地址=${cachedLocationInfo?.locationName}")
            Timber.tag("LocationUpdate").v("   缓存年龄=${cacheAge / 1000}秒")
            Timber.tag("LocationUpdate").v("   有效期=${LOCATION_CACHE_DURATION / 1000}秒")
            Timber.tag("LocationUpdate").v("   是否有效=$isValid")
        }

        return isValid
    }

    /**
     * 检查位置地址是否发生变化（基于地址名称对比）
     */
    private fun isLocationAddressChanged(oldLocation: LocationInfo?, newLocation: LocationInfo): Boolean {
        if (oldLocation == null) return true

        // 对比地址名称（去除空格后对比）
        val oldAddress = oldLocation.locationName.trim()
        val newAddress = newLocation.locationName.trim()

        return oldAddress != newAddress
    }

    /**
     * 更新位置缓存（包括内存和数据库）
     */
    private fun updateLocationCache(locationInfo: LocationInfo) {
        viewModelScope.launch {
            try {
                Timber.tag("LocationUpdate").d("💾 更新位置缓存: ${locationInfo.locationName}")

                // 更新内存缓存
                cachedLocationInfo = locationInfo
                lastLocationUpdateTime = System.currentTimeMillis()
                Timber.tag("LocationUpdate").d("   ✅ 内存缓存已更新")

                // 保存到数据库
                taskUseCases.locationRepository.saveCurrentLocation(locationInfo)
                Timber.tag("LocationUpdate").d("   ✅ 位置已保存到数据库")
            } catch (e: Exception) {
                Timber.tag("LocationUpdate").e(e, "❌ 更新位置缓存失败")
            }
        }
    }
    
    /**
     * 静默更新位置（用户从其他页面切换回来时调用）
     * 设计原则：
     * 1. 5分钟内不触发任何更新
     * 2. 超过5分钟后台静默获取，不显示加载动画
     * 3. 只有地址名称变化才更新UI
     */
    fun silentLocationUpdate() {
        viewModelScope.launch {
            Timber.tag("LocationUpdate").d("━━━━━ 静默位置更新开始 ━━━━━")
            Timber.tag("LocationUpdate").d("当前UI状态: locationName=${_uiState.value.currentLocationName}, isLoading=${_uiState.value.isLocationLoading}")
            Timber.tag("LocationUpdate").d("缓存状态: hasCache=${cachedLocationInfo != null}, cacheLocation=${cachedLocationInfo?.locationName}")

            // 检查权限和服务状态
            if (!locationService.hasLocationPermission()) {
                Timber.tag("LocationUpdate").w("❌ 静默更新失败：无位置权限")
                return@launch
            }

            if (!locationService.isLocationEnabled()) {
                Timber.tag("LocationUpdate").w("❌ 静默更新失败：位置服务未开启")
                return@launch
            }

            Timber.tag("LocationUpdate").d("✅ 权限检查通过")

            // 如果缓存有效（5分钟内），直接使用，不触发任何更新
            val cacheAge = System.currentTimeMillis() - lastLocationUpdateTime
            if (isCacheValid()) {
                Timber.tag("LocationUpdate").d("✅ 静默更新：缓存有效（${cacheAge / 1000}秒前），跳过更新")
                return@launch
            }

            // 缓存过期（超过5分钟），后台静默获取新位置
            Timber.tag("LocationUpdate").d("⏰ 静默更新：缓存过期（${cacheAge / 1000}秒前），后台静默获取新位置")
            try {
                Timber.tag("LocationUpdate").d("📡 后台静默调用 locationService.getCurrentLocation()（不显示加载动画）")
                val newLocation = locationService.getCurrentLocation(forceRefresh = false)

                if (newLocation != null) {
                    Timber.tag("LocationUpdate").d("✅ 位置获取成功:")
                    Timber.tag("LocationUpdate").d("   新地址: ${newLocation.locationName}")
                    Timber.tag("LocationUpdate").d("   新经纬度: (${newLocation.latitude}, ${newLocation.longitude})")

                    // 对比地址名称是否变化
                    val addressChanged = isLocationAddressChanged(cachedLocationInfo, newLocation)
                    Timber.tag("LocationUpdate").d("📊 地址对比检测: addressChanged=$addressChanged")

                    if (cachedLocationInfo != null) {
                        Timber.tag("LocationUpdate").d("   旧地址: ${cachedLocationInfo!!.locationName}")
                        Timber.tag("LocationUpdate").d("   新地址: ${newLocation.locationName}")
                        Timber.tag("LocationUpdate").d("   对比结果: ${if (addressChanged) "地址已变化" else "地址相同"}")
                    }

                    // 无论地址是否变化，都更新缓存和时间戳
                    updateLocationCache(newLocation)

                    if (addressChanged) {
                        Timber.tag("LocationUpdate").d("🔄 地址发生变化，更新UI显示")
                        // 地址变化，更新UI（不显示加载动画）
                        _uiState.value = _uiState.value.copy(
                            currentLocation = newLocation,
                            currentLocationName = newLocation.locationName,
                            isLocationLoading = false,  // 确保不显示加载动画
                            locationError = null
                        )
                        Timber.tag("LocationUpdate").d("✅ UI已更新显示新地址")

                        // 地址变化时，静默刷新天气
                        Timber.tag("LocationUpdate").d("🌤️ 地址变化，触发天气静默更新")
                        silentWeatherUpdate(newLocation)
                    } else {
                        Timber.tag("LocationUpdate").d("➡️ 地址未变化，不更新UI，仅更新缓存")
                        Timber.tag("LocationUpdate").d("   （UI继续显示: ${_uiState.value.currentLocationName}）")
                    }
                } else {
                    Timber.tag("LocationUpdate").w("❌ 静默更新：位置获取失败（返回null）")
                    Timber.tag("LocationUpdate").d("   保持当前UI显示（缓存: ${cachedLocationInfo?.locationName}）")
                    // 失败时什么都不做，UI保持当前状态
                }
            } catch (e: Exception) {
                Timber.tag("LocationUpdate").e(e, "💥 静默更新异常: ${e.message}")
                Timber.tag("LocationUpdate").e("   异常类型: ${e.javaClass.simpleName}")
                Timber.tag("LocationUpdate").d("   保持当前UI显示（缓存: ${cachedLocationInfo?.locationName}）")
                // 异常时什么都不做，UI保持当前状态
            } finally {
                Timber.tag("LocationUpdate").d("━━━━━ 静默位置更新结束 ━━━━━")
                Timber.tag("LocationUpdate").d("最终UI状态: locationName=${_uiState.value.currentLocationName}, isLoading=${_uiState.value.isLocationLoading}")
            }
        }
    }

    /**
     * 静默更新天气（后台获取，只有数据变化才更新UI）
     * 设计原则：
     * 1. 后台静默获取，不显示加载动画
     * 2. 只有天气数据变化才更新UI
     */
    private fun silentWeatherUpdate(location: LocationInfo) {
        viewModelScope.launch {
            Timber.d("━━━━━ 静默天气更新开始 ━━━━━")
            Timber.d("当前天气状态: ${_uiState.value.weatherInfo?.condition?.displayName}, ${_uiState.value.weatherInfo?.temperature}°C")

            try {
                Timber.d("📡 后台静默调用 weatherService.getCurrentWeather()（不显示加载动画）")

                weatherService.getCurrentWeather(location, forceRefresh = false).fold(
                    onSuccess = { newWeather ->
                        Timber.d("✅ 天气获取成功:")
                        Timber.d("   天气: ${newWeather.condition.displayName}")
                        Timber.d("   温度: ${newWeather.temperature}°C")
                        Timber.d("   湿度: ${newWeather.humidity}%")

                        val oldWeather = _uiState.value.weatherInfo

                        // 对比天气数据是否变化
                        val weatherChanged = oldWeather == null ||
                                oldWeather.condition != newWeather.condition ||
                                oldWeather.temperature != newWeather.temperature ||
                                oldWeather.humidity != newWeather.humidity

                        Timber.d("📊 天气对比检测: weatherChanged=$weatherChanged")

                        if (oldWeather != null) {
                            Timber.d("   旧天气: ${oldWeather.condition.displayName}, ${oldWeather.temperature}°C, ${oldWeather.humidity}%")
                            Timber.d("   新天气: ${newWeather.condition.displayName}, ${newWeather.temperature}°C, ${newWeather.humidity}%")
                        }

                        if (weatherChanged) {
                            Timber.d("🔄 天气数据变化，更新UI显示")
                            _uiState.value = _uiState.value.copy(
                                weatherInfo = newWeather,
                                isWeatherLoading = false,  // 确保不显示加载动画
                                weatherError = null
                            )
                            Timber.d("✅ UI已更新显示新天气")
                        } else {
                            Timber.d("➡️ 天气未变化，不更新UI")
                            Timber.d("   （UI继续显示当前天气）")
                        }
                    },
                    onFailure = { error ->
                        Timber.e(error, "❌ 静默天气更新失败: ${error.message}")
                        Timber.d("   保持当前天气显示")
                        // 失败时什么都不做，UI保持当前状态
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "💥 静默天气更新异常: ${e.message}")
                Timber.d("   保持当前天气显示")
                // 异常时什么都不做，UI保持当前状态
            } finally {
                Timber.d("━━━━━ 静默天气更新结束 ━━━━━")
                Timber.d("最终天气状态: ${_uiState.value.weatherInfo?.condition?.displayName}, ${_uiState.value.weatherInfo?.temperature}°C")
            }
        }
    }
} 