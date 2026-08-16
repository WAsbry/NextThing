package com.nextthing.app.presentation.screens.createlocation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextthing.app.domain.model.LocationInfo
import com.nextthing.app.domain.model.LocationType
import com.nextthing.app.domain.model.GeofenceLocation
import com.nextthing.app.domain.service.LocationService
import com.nextthing.app.domain.usecase.GeofenceUseCases
import com.nextthing.app.domain.usecase.LocationUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CreateLocationViewModel @Inject constructor(
    private val locationUseCases: LocationUseCases,
    private val locationService: LocationService,
    private val geofenceUseCases: GeofenceUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateLocationUiState())
    val uiState: StateFlow<CreateLocationUiState> = _uiState.asStateFlow()

    private var bindTaskId: String? = null

    fun setBindTaskId(taskId: String?) {
        bindTaskId = taskId?.takeIf { it.isNotBlank() }
    }

    fun updateLocationName(name: String) {
        _uiState.value = _uiState.value.copy(locationName = name)
    }

    fun updateSelectedMode(mode: LocationSelectionMode) {
        _uiState.value = _uiState.value.copy(selectedMode = mode)
    }

    fun updateCoordinates(latitude: Double, longitude: Double) {
        _uiState.value = _uiState.value.copy(
            latitude = latitude,
            longitude = longitude
        )
    }

    fun updateAddress(address: String) {
        _uiState.value = _uiState.value.copy(address = address)
    }

    /**
     * 获取实时位置（使用高德SDK）
     */
    fun getCurrentLocation(onResolved: (() -> Unit)? = null) {
        if (_uiState.value.isLoadingLocation) return

        _uiState.value = _uiState.value.copy(isLoadingLocation = true, errorMessage = null)

        viewModelScope.launch {
            try {
                val currentLocation = locationService.getCurrentLocation(forceRefresh = true)
                if (currentLocation != null) {
                    _uiState.value = _uiState.value.copy(
                        latitude = currentLocation.latitude,
                        longitude = currentLocation.longitude,
                        address = if (currentLocation.address.isNotBlank()) {
                            currentLocation.address
                        } else {
                            "${currentLocation.city}${currentLocation.district}"
                        },
                        isLoadingLocation = false,
                        selectedMode = LocationSelectionMode.REAL_TIME,
                        locationMessage = null
                    )
                    Timber.d("Location obtained: ${currentLocation.latitude}, ${currentLocation.longitude}")
                    onResolved?.invoke()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoadingLocation = false,
                        errorMessage = "无法获取当前位置，请检查定位权限",
                        locationMessage = "定位失败，请检查系统定位开关、应用定位权限后重试"
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to get current location")
                _uiState.value = _uiState.value.copy(
                    isLoadingLocation = false,
                    errorMessage = "获取位置失败: ${e.localizedMessage ?: e.message ?: "未知错误"}",
                    locationMessage = "定位失败，请点击“使用当前位置”重试"
                )
            }
        }
    }

    /**
     * 保存地点
     */
    fun saveLocation(onSuccess: (CreatedLocationResult) -> Unit) {
        val currentState = _uiState.value

        if (currentState.locationName.isBlank()) {
            _uiState.value = currentState.copy(errorMessage = "请输入地点名称")
            return
        }

        if (currentState.latitude == null || currentState.longitude == null) {
            if (currentState.selectedMode == LocationSelectionMode.REAL_TIME) {
                getCurrentLocation { saveLocation(onSuccess) }
                return
            }
            _uiState.value = currentState.copy(errorMessage = "请选择位置")
            return
        }

        _uiState.value = currentState.copy(isSaving = true)

        viewModelScope.launch {
            try {
                val locationInfo = LocationInfo(
                    id = UUID.randomUUID().toString(),
                    locationName = currentState.locationName,
                    latitude = currentState.latitude,
                    longitude = currentState.longitude,
                    address = currentState.address,
                    locationType = if (currentState.selectedMode == LocationSelectionMode.REAL_TIME) {
                        LocationType.AUTO
                    } else {
                        LocationType.MANUAL
                    },
                    addedAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                )

                val locationResult = locationUseCases.saveLocation(locationInfo)
                if (locationResult.isSuccess) {
                    val savedLocation = locationResult.getOrThrow()
                    val geofenceResult = geofenceUseCases.createGeofenceLocation(
                        GeofenceLocation(locationInfo = savedLocation)
                    )
                    if (geofenceResult.isFailure) {
                        _uiState.value = currentState.copy(
                            isSaving = false,
                            errorMessage = "地点已保存，但围栏创建失败: ${geofenceResult.exceptionOrNull()?.message}"
                        )
                        return@launch
                    }
                    val geofenceLocationId = geofenceResult.getOrThrow()
                    val taskId = bindTaskId
                    if (taskId != null) {
                        val bindingResult = geofenceUseCases.createTaskGeofence(taskId, geofenceLocationId)
                        if (bindingResult.isFailure) {
                            _uiState.value = currentState.copy(
                                isSaving = false,
                                errorMessage = "地点已创建，但未能绑定当前任务: ${bindingResult.exceptionOrNull()?.message}"
                            )
                            return@launch
                        }
                    }
                    _uiState.value = currentState.copy(isSaving = false)
                    onSuccess(
                        CreatedLocationResult(
                            locationName = currentState.locationName.trim(),
                            geofenceLocationId = geofenceLocationId,
                            boundTaskId = taskId
                        )
                    )
                } else {
                    _uiState.value = currentState.copy(
                        isSaving = false,
                        errorMessage = "保存失败: ${locationResult.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to save location")
                _uiState.value = currentState.copy(
                    isSaving = false,
                    errorMessage = "保存失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 从地图选择器接收位置数据
     */
    fun updateFromMapPicker(latitude: Double, longitude: Double, address: String) {
        Timber.d("📍 从地图选择器更新位置: ($latitude, $longitude), 地址: $address")
        _uiState.value = _uiState.value.copy(
            latitude = latitude,
            longitude = longitude,
            address = address,
            selectedMode = LocationSelectionMode.MAP_SELECT
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

data class CreateLocationUiState(
    val locationName: String = "",
    val selectedMode: LocationSelectionMode = LocationSelectionMode.REAL_TIME,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String = "",
    val isLoadingLocation: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val locationMessage: String? = null
)

enum class LocationSelectionMode {
    REAL_TIME,  // 实时位置
    MAP_SELECT  // 地图选择
}

data class CreatedLocationResult(
    val locationName: String,
    val geofenceLocationId: String,
    val boundTaskId: String?
)
