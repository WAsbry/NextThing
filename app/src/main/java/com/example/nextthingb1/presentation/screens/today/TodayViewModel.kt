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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject
import com.example.nextthingb1.domain.model.TaskTab

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
    val weatherError: String? = null
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
        loadTodayTasks()
        checkLocationPermissionAndStatus()
        // 自动开始获取位置
        autoStartLocationUpdate()
    }
    
    // 位置缓存
    private var cachedLocationInfo: LocationInfo? = null
    private var lastLocationUpdateTime: Long = 0
    private val LOCATION_CACHE_DURATION = 5 * 60 * 1000L // 5分钟缓存
    
    private fun loadTodayTasks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                taskUseCases.getTodayTasks().collect { tasks ->
                    val completed = tasks.filter { it.status == TaskStatus.COMPLETED }
                    val pending = tasks.filter {
                    it.status == TaskStatus.PENDING
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
                it.status == TaskStatus.PENDING
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
                // 延期任务：使用 DeferTaskUseCase 将截止日期推迟一天并更新状态为 DEFERRED
                taskUseCases.deferTask(taskId).fold(
                    onSuccess = {
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
            // 检查权限状态，如果有权限则进行静默更新
            if (_uiState.value.hasLocationPermission && _uiState.value.isLocationEnabled) {
                if (_uiState.value.currentLocationName == "需要位置权限" || 
                    _uiState.value.currentLocationName == "请开启位置服务") {
                    // 更新状态为可点击
                    _uiState.value = _uiState.value.copy(
                        currentLocationName = "正在获取位置...",
                        isLocationLoading = false
                    )
                    // 开始静默更新
                    silentLocationUpdate()
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
                // 有权限但没有位置信息时，检查缓存
                if (isCacheValid()) {
                    // 缓存有效，使用缓存位置
                    _uiState.value = _uiState.value.copy(
                        currentLocation = cachedLocationInfo,
                        currentLocationName = cachedLocationInfo?.locationName ?: "未知位置",
                        isLocationLoading = false
                    )
                } else {
                    // 缓存无效，显示正在获取状态
                    _uiState.value = _uiState.value.copy(
                        currentLocationName = "正在获取位置...",
                        isLocationLoading = true
                    )
                }
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
            // 权限刚被授予，自动开始获取位置
            Timber.d("检测到权限刚被授予，自动开始获取位置")
            autoStartLocationUpdate()
        } else if (newPermission && newLocationEnabled) {
            // 有权限时进行静默更新
            silentLocationUpdate()
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
        viewModelScope.launch {
            // 检查权限和服务状态
            if (!locationService.hasLocationPermission()) {
                Timber.d("无位置权限，等待用户授权")
                return@launch
            }
            
            if (!locationService.isLocationEnabled()) {
                Timber.d("位置服务未启用，等待用户开启")
                return@launch
            }
            
            // 检查缓存是否有效
            if (isCacheValid()) {
                Timber.d("使用缓存位置: ${cachedLocationInfo?.locationName}")
                _uiState.value = _uiState.value.copy(
                    currentLocation = cachedLocationInfo,
                    currentLocationName = cachedLocationInfo?.locationName ?: "未知位置",
                    isLocationLoading = false
                )
                // 位置可用时，检查是否需要获取天气
                if (_uiState.value.weatherInfo == null || weatherService.shouldRefreshWeather()) {
                    Timber.d("🌤️ [TodayViewModel] 使用缓存位置，开始调用天气服务...")
                    loadWeatherInfo()
                }
                return@launch
            }
            
            // 开始获取位置
            startLocationAcquisition()
        }
    }
    
    /**
     * 开始位置获取
     */
    private fun startLocationAcquisition() {
        viewModelScope.launch {
            Timber.d("开始获取位置")
            _uiState.value = _uiState.value.copy(
                isLocationLoading = true,
                currentLocationName = "正在获取位置...",
                locationError = null
            )
            
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
                    Timber.d("位置获取成功: ${location.locationName}")
                    // 自动获取天气信息
                    Timber.d("🌤️ [TodayViewModel] 位置获取成功，开始调用天气服务...")
                    loadWeatherInfo()
                } else {
                    // 位置获取失败或超时
                    val errorMsg = "获取位置失败"

                    _uiState.value = _uiState.value.copy(
                        currentLocationName = errorMsg,
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
     * 检查缓存是否有效
     */
    private fun isCacheValid(): Boolean {
        return cachedLocationInfo != null && 
               (System.currentTimeMillis() - lastLocationUpdateTime) < LOCATION_CACHE_DURATION
    }
    
    /**
     * 更新位置缓存
     */
    private fun updateLocationCache(locationInfo: LocationInfo) {
        // 如果缓存的位置与新位置不同，则更新
        if (cachedLocationInfo == null || 
            cachedLocationInfo?.latitude != locationInfo.latitude || 
            cachedLocationInfo?.longitude != locationInfo.longitude) {
            cachedLocationInfo = locationInfo
            lastLocationUpdateTime = System.currentTimeMillis()
            Timber.d("位置缓存已更新: ${locationInfo.locationName}")
        }
    }
    
    /**
     * 静默更新位置（用户从其他页面切换回来时调用）
     */
    fun silentLocationUpdate() {
        viewModelScope.launch {
            // 检查权限和服务状态
            if (!locationService.hasLocationPermission() || !locationService.isLocationEnabled()) {
                return@launch
            }
            
            // 如果缓存有效，直接使用
            if (isCacheValid()) {
                Timber.d("静默更新：使用缓存位置")
                return@launch
            }
            
            // 缓存无效，静默获取新位置
            Timber.d("静默更新：开始获取新位置")
            try {
                val location = locationService.getCurrentLocation(forceRefresh = false)
                if (location != null) {
                    updateLocationCache(location)
                    // 静默更新UI，不显示加载状态
                    _uiState.value = _uiState.value.copy(
                        currentLocation = location,
                        currentLocationName = location.locationName,
                        isLocationLoading = false // 确保加载状态为false
                    )
                    Timber.d("静默位置更新成功: ${location.locationName}")
                } else {
                    Timber.w("静默位置更新失败")
                    // 失败时也要确保加载状态为false
                    _uiState.value = _uiState.value.copy(
                        isLocationLoading = false
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "静默位置更新异常")
            }
        }
    }
} 