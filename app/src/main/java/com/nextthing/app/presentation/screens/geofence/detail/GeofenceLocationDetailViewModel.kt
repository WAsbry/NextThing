package com.nextthing.app.presentation.screens.geofence.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextthing.app.domain.model.GeofenceLocation
import com.nextthing.app.domain.usecase.GeofenceUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class GeofenceLocationDetailUiState(
    val location: GeofenceLocation? = null,
    val relatedTasksCount: Int = 0,
    val monthlyCheckCount: Int = 0,      // 本月检查次数
    val monthlyHitCount: Int = 0,        // 本月命中次数
    val hitRate: Float = 0f,             // 命中率（0.0~1.0）
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val showDeleteDialog: Boolean = false,

    // 编辑模式相关
    val isEditMode: Boolean = false,
    val editLocationName: String = "",
    val editLatitude: Double = 0.0,
    val editLongitude: Double = 0.0,
    val editAddress: String = "",
    val editGeofenceEnabled: Boolean = false,
    val editCustomRadius: Int? = null,
    val editIsFrequent: Boolean = false,
    val isSaving: Boolean = false,

    // 全局配置
    val defaultRadius: Int = 200,
    val isGlobalEnabled: Boolean = false
)

@HiltViewModel
class GeofenceLocationDetailViewModel @Inject constructor(
    private val geofenceUseCases: GeofenceUseCases,
    private val locationRepository: com.nextthing.app.domain.repository.LocationRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val TAG = "GeofenceLocationDetail"
    }

    private val locationId: String = checkNotNull(savedStateHandle["locationId"])

    private val _uiState = MutableStateFlow(GeofenceLocationDetailUiState())
    val uiState: StateFlow<GeofenceLocationDetailUiState> = _uiState.asStateFlow()

    init {
        loadLocation()
        loadDefaultRadius()
    }

    private fun loadDefaultRadius() {
        viewModelScope.launch {
            try {
                geofenceUseCases.getGeofenceConfig().collect { config ->
                    config?.let {
                        _uiState.update { state ->
                            state.copy(
                                defaultRadius = it.defaultRadius,
                                isGlobalEnabled = it.isGlobalEnabled
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "加载全局配置失败，使用默认值 200")
            }
        }
    }

    private fun loadLocation() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                // 合并地点 Flow 与关联任务 Flow，确保 relatedTasksCount 实时同步
                combine(
                    geofenceUseCases.getGeofenceLocations.getById(locationId),
                    geofenceUseCases.getTaskGeofence.getByLocationId(locationId)
                ) { location, taskGeofences ->
                    location to taskGeofences.size
                }.collect { (location, tasksCount) ->
                    if (location != null) {
                        _uiState.update {
                            it.copy(
                                location = location,
                                relatedTasksCount = tasksCount,
                                monthlyCheckCount = location.monthlyCheckCount,
                                monthlyHitCount = location.monthlyHitCount,
                                hitRate = location.getHitRate(),
                                isLoading = false
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "地点不存在"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "加载地点失败")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "加载失败: ${e.message}"
                    )
                }
            }
        }
    }

    fun updateCustomRadius(radius: Int?) {
        _uiState.update { it.copy(editCustomRadius = radius) }
    }

    fun toggleFrequent() {
        _uiState.update { it.copy(editIsFrequent = !it.editIsFrequent) }
    }

    fun updateGeofenceEnabled(enabled: Boolean) {
        _uiState.update {
            it.copy(
                editGeofenceEnabled = enabled,
                editCustomRadius = if (enabled) it.editCustomRadius ?: 200 else null
            )
        }
    }

    fun showDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = true) }
    }

    fun hideDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = false) }
    }

    fun deleteLocation(onDeleted: () -> Unit) {
        viewModelScope.launch {
            try {
                val locationId = _uiState.value.location?.id ?: return@launch
                val result = geofenceUseCases.deleteGeofenceLocation(locationId)

                if (result.isSuccess) {
                    Timber.tag(TAG).d("✅ 地点已删除")
                    onDeleted()
                } else {
                    Timber.tag(TAG).e("❌ 删除地点失败")
                    _uiState.update { it.copy(errorMessage = "删除失败") }
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "删除地点异常")
                _uiState.update { it.copy(errorMessage = "删除失败: ${e.message}") }
            }
        }
    }

    // ========== 编辑功能 ==========

    fun enterEditMode() {
        val location = _uiState.value.location ?: return
        _uiState.update {
            it.copy(
                isEditMode = true,
                editLocationName = location.locationInfo.locationName,
                editLatitude = location.locationInfo.latitude,
                editLongitude = location.locationInfo.longitude,
                editAddress = location.locationInfo.address,
                editGeofenceEnabled = location.isEnabled,
                editCustomRadius = location.customRadius,
                editIsFrequent = location.isFrequent
            )
        }
        Timber.tag(TAG).d("📝 进入编辑模式")
    }

    fun exitEditMode() {
        _uiState.update { it.copy(isEditMode = false) }
        Timber.tag(TAG).d("❌ 退出编辑模式")
    }

    fun updateEditLocationName(name: String) {
        _uiState.update { it.copy(editLocationName = name) }
    }

    fun updateEditPosition(lat: Double, lng: Double, address: String) {
        _uiState.update {
            it.copy(
                editLatitude = lat,
                editLongitude = lng,
                editAddress = address
            )
        }
        Timber.tag(TAG).d("📍 位置已更新: ($lat, $lng)")
    }

    fun saveChanges(onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSaving = true) }

                val currentLocation = _uiState.value.location
                if (currentLocation == null) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = "地点信息不存在"
                        )
                    }
                    return@launch
                }

                val editState = _uiState.value

                // 验证输入
                if (editState.editLocationName.isBlank()) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = "地点名称不能为空"
                        )
                    }
                    return@launch
                }

                // 检查位置是否改变
                val positionChanged = editState.editLatitude != currentLocation.locationInfo.latitude ||
                                    editState.editLongitude != currentLocation.locationInfo.longitude

                Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Timber.tag(TAG).d("💾 开始保存修改")
                Timber.tag(TAG).d("  位置是否改变: $positionChanged")
                Timber.tag(TAG).d("  当前位置: (${currentLocation.locationInfo.latitude}, ${currentLocation.locationInfo.longitude})")
                Timber.tag(TAG).d("  新位置: (${editState.editLatitude}, ${editState.editLongitude})")

                val updatedLocationInfo = if (positionChanged) {
                    // 创建新的 LocationInfo
                    val newLocationInfo = currentLocation.locationInfo.copy(
                        id = java.util.UUID.randomUUID().toString(),
                        locationName = editState.editLocationName,
                        latitude = editState.editLatitude,
                        longitude = editState.editLongitude,
                        address = editState.editAddress,
                        updatedAt = java.time.LocalDateTime.now()
                    )
                    Timber.tag(TAG).d("  创建新 LocationInfo: ${newLocationInfo.id}")
                    newLocationInfo
                } else {
                    // 只更新名称
                    currentLocation.locationInfo.copy(
                        locationName = editState.editLocationName,
                        updatedAt = java.time.LocalDateTime.now()
                    )
                }

                val updatedGeofenceLocation = currentLocation.copy(
                    locationInfo = updatedLocationInfo,
                    isEnabled = editState.editGeofenceEnabled,
                    customRadius = if (editState.editGeofenceEnabled) editState.editCustomRadius else null,
                    isFrequent = editState.editIsFrequent,
                    updatedAt = java.time.LocalDateTime.now()
                )

                // 保存 LocationInfo
                if (positionChanged) {
                    val insertResult = locationRepository.insertLocation(updatedLocationInfo)
                    if (insertResult.isFailure) {
                        Timber.tag(TAG).e("❌ 插入新 LocationInfo 失败")
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                errorMessage = "保存失败: ${insertResult.exceptionOrNull()?.message}"
                            )
                        }
                        return@launch
                    }
                    Timber.tag(TAG).d("✅ 新 LocationInfo 已插入")
                } else {
                    try {
                        locationRepository.updateLocation(updatedLocationInfo)
                        Timber.tag(TAG).d("✅ LocationInfo 已更新")
                    } catch (e: Exception) {
                        Timber.tag(TAG).e(e, "❌ 更新 LocationInfo 失败")
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                errorMessage = "保存失败: ${e.message}"
                            )
                        }
                        return@launch
                    }
                }

                // 保存 GeofenceLocation
                val result = geofenceUseCases.updateGeofenceLocation(updatedGeofenceLocation)

                if (result.isSuccess) {
                    Timber.tag(TAG).d("✅ GeofenceLocation 已更新")
                    Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    _uiState.update {
                        it.copy(isEditMode = false, isSaving = false)
                    }
                    onSaved()
                } else {
                    Timber.tag(TAG).e("❌ 更新 GeofenceLocation 失败")
                    Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = "保存失败: ${result.exceptionOrNull()?.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "❌ 保存修改异常")
                Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "保存失败: ${e.message}"
                    )
                }
            }
        }
    }
}
