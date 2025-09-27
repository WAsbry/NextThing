package com.example.nextthingb1.presentation.screens.createlocation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextthingb1.domain.model.LocationInfo
import com.example.nextthingb1.domain.model.LocationType
import com.example.nextthingb1.domain.service.LocationService
import com.example.nextthingb1.domain.usecase.LocationUseCases
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
    private val locationService: LocationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateLocationUiState())
    val uiState: StateFlow<CreateLocationUiState> = _uiState.asStateFlow()

    init {
        // 进入页面时自动获取实时位置
        getCurrentLocation()
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
    fun getCurrentLocation() {
        if (_uiState.value.isLoadingLocation) return

        _uiState.value = _uiState.value.copy(isLoadingLocation = true, errorMessage = null)

        viewModelScope.launch {
            try {
                val currentLocation = locationService.getCurrentLocation()
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
                        selectedMode = LocationSelectionMode.REAL_TIME
                    )
                    Timber.d("Location obtained: ${currentLocation.latitude}, ${currentLocation.longitude}")
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoadingLocation = false,
                        errorMessage = "无法获取当前位置，请检查定位权限"
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to get current location")
                _uiState.value = _uiState.value.copy(
                    isLoadingLocation = false,
                    errorMessage = "获取位置失败: ${e.localizedMessage ?: e.message ?: "未知错误"}"
                )
            }
        }
    }

    /**
     * 保存地点
     */
    fun saveLocation(onSuccess: () -> Unit) {
        val currentState = _uiState.value

        if (currentState.locationName.isBlank()) {
            _uiState.value = currentState.copy(errorMessage = "请输入地点名称")
            return
        }

        if (currentState.latitude == null || currentState.longitude == null) {
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
                    locationType = LocationType.MANUAL,
                    addedAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                )

                val result = locationUseCases.saveLocation(locationInfo)
                if (result.isSuccess) {
                    _uiState.value = currentState.copy(isSaving = false)
                    onSuccess()
                } else {
                    _uiState.value = currentState.copy(
                        isSaving = false,
                        errorMessage = "保存失败: ${result.exceptionOrNull()?.message}"
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
    val errorMessage: String? = null
)

enum class LocationSelectionMode {
    REAL_TIME,  // 实时位置
    MAP_SELECT  // 地图选择
}