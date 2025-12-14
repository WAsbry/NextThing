package com.example.nextthingb1.presentation.screens.geofence.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextthingb1.domain.model.GeofenceLocation
import com.example.nextthingb1.domain.model.LocationInfo
import com.example.nextthingb1.domain.usecase.GeofenceUseCases
import com.example.nextthingb1.domain.repository.LocationRepository
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
    val errorMessage: String? = null
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
    }

    private fun loadAvailableLocations() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                // 获取所有位置
                locationRepository.getAllLocations().collect { locations ->
                    // 过滤掉已经添加为地理围栏的位置
                    val geofenceLocations = geofenceUseCases.getGeofenceLocations.getAllOnce()
                    val geofenceLocationIds = geofenceLocations.map { it.locationInfo.id }.toSet()

                    val available = locations.filter { it.id !in geofenceLocationIds }

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
                customRadius = if (use) (it.customRadius ?: 200) else null
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
