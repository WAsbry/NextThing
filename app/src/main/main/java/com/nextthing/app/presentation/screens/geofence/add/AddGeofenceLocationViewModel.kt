package com.nextthing.app.presentation.screens.geofence.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextthing.app.domain.model.GeofenceLocation
import com.nextthing.app.domain.model.LocationInfo
import com.nextthing.app.domain.usecase.GeofenceUseCases
import com.nextthing.app.domain.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class AddGeofenceLocationUiState(
    val availableLocations: List<LocationInfo> = emptyList(),
    val selectedLocation: LocationInfo? = null,
    val customRadius: Int? = null,
    val useCustomRadius: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val defaultRadius: Int = 200  // 全局默认半径
)

@HiltViewModel
class AddGeofenceLocationViewModel @Inject constructor(
    private val geofenceUseCases: GeofenceUseCases,
    private val locationRepository: LocationRepository
) : ViewModel() {

    companion object {
        private const val TAG = "AddGeofenceLocation"
    }

    private val _uiState = MutableStateFlow(AddGeofenceLocationUiState())
    val uiState: StateFlow<AddGeofenceLocationUiState> = _uiState.asStateFlow()

    init {
        loadAvailableLocations()
        loadDefaultRadius()
    }

    private fun loadDefaultRadius() {
        viewModelScope.launch {
            try {
                val config = geofenceUseCases.getGeofenceConfig().first()
                config?.let {
                    _uiState.update { state -> state.copy(defaultRadius = it.defaultRadius) }
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "加载全局配置失败，使用默认值 200")
            }
        }
    }

    private fun loadAvailableLocations() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                // 同时订阅两个 Flow，确保原子性过滤：已设为围栏的地点不显示
                combine(
                    locationRepository.getAllLocations(),
                    geofenceUseCases.getGeofenceLocations()
                ) { locations, geofenceLocations ->
                    val geofenceLocationIds = geofenceLocations.map { it.locationInfo.id }.toSet()
                    locations.filter { it.id !in geofenceLocationIds }
                }.collect { available ->
                    _uiState.update {
                        it.copy(
                            availableLocations = available,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "加载可用位置失败")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "加载失败: ${e.message}"
                    )
                }
            }
        }
    }

    fun selectLocation(location: LocationInfo) {
        _uiState.update { it.copy(selectedLocation = location) }
    }

    fun toggleUseCustomRadius(use: Boolean) {
        _uiState.update {
            it.copy(
                useCustomRadius = use,
                customRadius = if (use) (it.customRadius ?: it.defaultRadius) else null
            )
        }
    }

    fun updateCustomRadius(radius: Int) {
        _uiState.update { it.copy(customRadius = radius) }
    }

    fun saveGeofenceLocation(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                val location = state.selectedLocation

                if (location == null) {
                    _uiState.update { it.copy(errorMessage = "请选择一个地点") }
                    return@launch
                }

                _uiState.update { it.copy(isSaving = true) }

                val geofenceLocation = GeofenceLocation(
                    locationInfo = location,
                    customRadius = if (state.useCustomRadius) state.customRadius else null,
                    isFrequent = false,
                    usageCount = 0,
                    lastUsed = null
                )

                val result = geofenceUseCases.createGeofenceLocation(geofenceLocation)

                if (result.isSuccess) {
                    Timber.tag(TAG).d("✅ 地理围栏地点已创建")
                    onSuccess()
                } else {
                    Timber.tag(TAG).e("❌ 创建地理围栏地点失败")
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = "保存失败"
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "保存地理围栏地点异常")
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "保存失败: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
