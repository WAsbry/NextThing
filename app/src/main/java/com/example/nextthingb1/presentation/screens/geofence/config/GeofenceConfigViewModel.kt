package com.example.nextthingb1.presentation.screens.geofence.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextthingb1.domain.model.GeofenceLocation
import com.example.nextthingb1.domain.service.GeofenceManager
import com.example.nextthingb1.domain.usecase.GeofenceUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class GeofenceConfigViewModel @Inject constructor(
    private val geofenceUseCases: GeofenceUseCases,
    private val geofenceManager: GeofenceManager
) : ViewModel() {

    companion object {
        private const val TAG = "GeofenceConfigVM"
    }

    private val _uiState = MutableStateFlow(GeofenceConfigUiState())
    val uiState: StateFlow<GeofenceConfigUiState> = _uiState.asStateFlow()

    init {
        checkPermissions()
        loadData()
    }

    // ========== 权限管理 ==========

    /**
     * 检查权限状态
     */
    fun checkPermissions() {
        viewModelScope.launch {
            try {
                val hasLocation = geofenceManager.hasLocationPermission()
                val hasBackground = geofenceManager.hasBackgroundLocationPermission()

                _uiState.update {
                    it.copy(
                        locationPermissionState = if (hasLocation) PermissionState.GRANTED else PermissionState.NOT_REQUESTED,
                        backgroundLocationPermissionState = if (hasBackground) PermissionState.GRANTED else PermissionState.NOT_REQUESTED
                    )
                }

                Timber.tag(TAG).d("权限检查: 位置权限=$hasLocation, 后台权限=$hasBackground")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "❌ 权限检查失败")
            }
        }
    }

    /**
     * 请求显示位置权限说明
     */
    fun requestLocationPermission() {
        _uiState.update { it.copy(showPermissionRationale = true) }
    }

    /**
     * 请求显示后台位置权限说明
     */
    fun requestBackgroundLocationPermission() {
        _uiState.update { it.copy(showBackgroundPermissionRationale = true) }
    }

    /**
     * 关闭权限说明
     */
    fun dismissPermissionRationale() {
        _uiState.update { it.copy(showPermissionRationale = false) }
    }

    /**
     * 关闭后台权限说明
     */
    fun dismissBackgroundPermissionRationale() {
        _uiState.update { it.copy(showBackgroundPermissionRationale = false) }
    }

    /**
     * 处理位置权限请求结果
     *
     * @param granted 是否授予权限
     * @param shouldShowRationale 是否应显示说明（用户拒绝但未选"不再询问"）
     */
    fun onLocationPermissionResult(granted: Boolean, shouldShowRationale: Boolean) {
        _uiState.update {
            it.copy(
                locationPermissionState = when {
                    granted -> PermissionState.GRANTED
                    shouldShowRationale -> PermissionState.DENIED
                    else -> PermissionState.PERMANENTLY_DENIED
                }
            )
        }

        if (granted) {
            Timber.tag(TAG).d("✅ 位置权限已授予")
            showSuccessMessage("位置权限已授予")
        } else {
            Timber.tag(TAG).w("⚠️ 位置权限被拒绝")
            if (shouldShowRationale) {
                showErrorMessage("需要位置权限才能使用地理围栏功能")
            } else {
                showErrorMessage("位置权限被永久拒绝，请在系统设置中手动开启")
            }
        }
    }

    /**
     * 处理后台位置权限请求结果
     *
     * @param granted 是否授予权限
     * @param shouldShowRationale 是否应显示说明
     */
    fun onBackgroundLocationPermissionResult(granted: Boolean, shouldShowRationale: Boolean) {
        _uiState.update {
            it.copy(
                backgroundLocationPermissionState = when {
                    granted -> PermissionState.GRANTED
                    shouldShowRationale -> PermissionState.DENIED
                    else -> PermissionState.PERMANENTLY_DENIED
                }
            )
        }

        if (granted) {
            Timber.tag(TAG).d("✅ 后台位置权限已授予")
            showSuccessMessage("后台位置权限已授予")
        } else {
            Timber.tag(TAG).w("⚠️ 后台位置权限被拒绝")
            if (shouldShowRationale) {
                showErrorMessage("需要后台位置权限才能在后台监控地理围栏")
            } else {
                showErrorMessage("后台位置权限被永久拒绝，请在系统设置中手动开启")
            }
        }
    }

    // ========== 数据加载 ==========

    /**
     * 加载所有数据
     */
    fun loadData() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }

                // 并行加载配置和地点列表
                launch {
                    geofenceUseCases.getGeofenceConfig().collect { config ->
                        if (config != null) {
                            _uiState.update {
                                it.copy(
                                    config = config,
                                    isGlobalEnabled = config.isGlobalEnabled,
                                    defaultRadius = config.defaultRadius,
                                    locationAccuracyThreshold = config.locationAccuracyThreshold,
                                    batteryOptimization = config.batteryOptimization,
                                    notifyWhenOutside = config.notifyWhenOutside
                                )
                            }
                        }
                    }
                }

                launch {
                    geofenceUseCases.getGeofenceLocations().collect { locations ->
                        // 计算月度统计
                        val monthlyCheckCount = locations.sumOf { it.monthlyCheckCount }
                        val averageHitRate = if (locations.isNotEmpty()) {
                            locations.map { it.getHitRate() }.average().toFloat()
                        } else {
                            0f
                        }

                        // 计算系统地理围栏状态
                        val hasPermission = geofenceManager.hasLocationPermission()
                        val systemActive = _uiState.value.isGlobalEnabled && hasPermission

                        _uiState.update {
                            it.copy(
                                locations = locations,
                                totalLocationsCount = locations.size,
                                monthlyCheckCount = monthlyCheckCount,
                                averageHitRate = averageHitRate,
                                systemGeofencesRegistered = locations.size,
                                systemGeofencesActive = systemActive
                            )
                        }
                    }
                }

                launch {
                    geofenceUseCases.getGeofenceLocations.getFrequent().collect { frequentLocations ->
                        _uiState.update {
                            it.copy(
                                frequentLocations = frequentLocations,
                                frequentLocationsCount = frequentLocations.size
                            )
                        }
                    }
                }

                // 加载活跃任务数量
                launch {
                    val activeCount = geofenceUseCases.getTaskGeofence.getAllEnabled()
                        .first()
                        .size
                    _uiState.update {
                        it.copy(activeTasksCount = activeCount)
                    }
                }

                _uiState.update { it.copy(isLoading = false) }
                Timber.tag(TAG).d("✅ 数据加载完成")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "❌ 加载数据失败")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "加载失败: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * 刷新数据
     */
    fun refresh() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isRefreshing = true) }

                // 更新常用地点标记
                geofenceUseCases.updateFrequentLocations()

                _uiState.update { it.copy(isRefreshing = false) }
                Timber.tag(TAG).d("✅ 数据刷新完成")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "❌ 刷新数据失败")
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        errorMessage = "刷新失败: ${e.message}"
                    )
                }
            }
        }
    }

    // ========== 全局配置操作 ==========

    /**
     * 切换全局开关
     */
    fun toggleGlobalEnabled() {
        viewModelScope.launch {
            try {
                // 检查权限
                if (!geofenceManager.hasLocationPermission()) {
                    showErrorMessage("请先授予位置权限")
                    return@launch
                }

                val newValue = !_uiState.value.isGlobalEnabled
                val result = geofenceUseCases.updateGeofenceConfig.updateGlobalEnabled(newValue)

                if (result.isSuccess) {
                    val hasPermission = geofenceManager.hasLocationPermission()
                    val systemActive = newValue && hasPermission

                    _uiState.update {
                        it.copy(
                            isGlobalEnabled = newValue,
                            systemGeofencesActive = systemActive
                        )
                    }
                    showSuccessMessage(if (newValue) "已启用地理围栏" else "已禁用地理围栏")
                    Timber.tag(TAG).d("✅ 全局开关已更新: $newValue, 系统活跃: $systemActive")
                } else {
                    showErrorMessage("更新失败")
                    Timber.tag(TAG).e("❌ 更新全局开关失败")
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "❌ 切换全局开关异常")
                showErrorMessage("操作失败: ${e.message}")
            }
        }
    }

    /**
     * 更新默认半径
     */
    fun updateDefaultRadius(radius: Int) {
        viewModelScope.launch {
            try {
                val result = geofenceUseCases.updateGeofenceConfig.updateDefaultRadius(radius)

                if (result.isSuccess) {
                    _uiState.update { it.copy(defaultRadius = radius) }
                    Timber.tag(TAG).d("✅ 默认半径已更新: ${radius}m")
                } else {
                    showErrorMessage("更新半径失败")
                    Timber.tag(TAG).e("❌ 更新默认半径失败")
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "❌ 更新默认半径异常")
                showErrorMessage("操作失败: ${e.message}")
            }
        }
    }

    /**
     * 更新精度阈值
     */
    fun updateLocationAccuracyThreshold(threshold: Int) {
        viewModelScope.launch {
            try {
                val result = geofenceUseCases.updateGeofenceConfig.updateLocationAccuracyThreshold(threshold)

                if (result.isSuccess) {
                    _uiState.update { it.copy(locationAccuracyThreshold = threshold) }
                    Timber.tag(TAG).d("✅ 精度阈值已更新: ${threshold}m")
                } else {
                    showErrorMessage("更新精度阈值失败")
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "❌ 更新精度阈值异常")
                showErrorMessage("操作失败: ${e.message}")
            }
        }
    }

    /**
     * 切换省电模式
     */
    fun toggleBatteryOptimization() {
        viewModelScope.launch {
            try {
                val newValue = !_uiState.value.batteryOptimization
                val result = geofenceUseCases.updateGeofenceConfig.updateBatteryOptimization(newValue)

                if (result.isSuccess) {
                    _uiState.update { it.copy(batteryOptimization = newValue) }
                    Timber.tag(TAG).d("✅ 省电模式已更新: $newValue")
                } else {
                    showErrorMessage("更新省电模式失败")
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "❌ 切换省电模式异常")
                showErrorMessage("操作失败: ${e.message}")
            }
        }
    }

    /**
     * 切换离开通知
     */
    fun toggleNotifyWhenOutside() {
        viewModelScope.launch {
            try {
                val newValue = !_uiState.value.notifyWhenOutside
                val result = geofenceUseCases.updateGeofenceConfig.updateNotifyWhenOutside(newValue)

                if (result.isSuccess) {
                    _uiState.update { it.copy(notifyWhenOutside = newValue) }
                    Timber.tag(TAG).d("✅ 离开通知已更新: $newValue")
                } else {
                    showErrorMessage("更新离开通知失败")
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "❌ 切换离开通知异常")
                showErrorMessage("操作失败: ${e.message}")
            }
        }
    }

    // ========== 地点操作 ==========

    /**
     * 删除地点（显示确认对话框）
     */
    fun showDeleteConfirmation(location: GeofenceLocation) {
        _uiState.update {
            it.copy(
                showDeleteConfirmation = true,
                locationToDelete = location
            )
        }
    }

    /**
     * 取消删除
     */
    fun cancelDelete() {
        _uiState.update {
            it.copy(
                showDeleteConfirmation = false,
                locationToDelete = null
            )
        }
    }

    /**
     * 确认删除地点
     */
    fun confirmDelete() {
        viewModelScope.launch {
            try {
                val location = _uiState.value.locationToDelete ?: return@launch

                val result = geofenceUseCases.deleteGeofenceLocation(location.id)

                if (result.isSuccess) {
                    showSuccessMessage("地点已删除")
                    Timber.tag(TAG).d("✅ 地点已删除: ${location.id}")
                } else {
                    showErrorMessage("删除失败")
                    Timber.tag(TAG).e("❌ 删除地点失败")
                }

                // 关闭对话框
                _uiState.update {
                    it.copy(
                        showDeleteConfirmation = false,
                        locationToDelete = null
                    )
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "❌ 删除地点异常")
                showErrorMessage("删除失败: ${e.message}")
            }
        }
    }

    /**
     * 切换常用标记
     */
    fun toggleFrequent(location: GeofenceLocation) {
        viewModelScope.launch {
            try {
                val newValue = !location.isFrequent
                val result = geofenceUseCases.updateGeofenceLocation.updateFrequent(
                    location.id,
                    newValue
                )

                if (result.isSuccess) {
                    showSuccessMessage(if (newValue) "已添加到常用" else "已取消常用")
                    Timber.tag(TAG).d("✅ 常用标记已更新: ${location.id} -> $newValue")
                } else {
                    showErrorMessage("更新失败")
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "❌ 切换常用标记异常")
                showErrorMessage("操作失败: ${e.message}")
            }
        }
    }

    // ========== UI 状态操作 ==========

    /**
     * 切换高级设置面板
     */
    fun toggleAdvancedSettings() {
        _uiState.update {
            it.copy(showAdvancedSettings = !it.showAdvancedSettings)
        }
    }

    /**
     * 显示成功消息
     */
    private fun showSuccessMessage(message: String) {
        _uiState.update { it.copy(successMessage = message) }
        // 3秒后自动清除
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            clearSuccessMessage()
        }
    }

    /**
     * 清除成功消息
     */
    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    /**
     * 显示错误消息
     */
    private fun showErrorMessage(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    /**
     * 清除错误消息
     */
    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
