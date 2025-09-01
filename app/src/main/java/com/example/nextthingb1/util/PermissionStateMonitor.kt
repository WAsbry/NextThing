package com.example.nextthingb1.util

import android.content.Context
import android.location.LocationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionStateMonitor @Inject constructor(
    private val context: Context
) {
    
    private val _hasLocationPermission = MutableStateFlow(false)
    val hasLocationPermission: StateFlow<Boolean> = _hasLocationPermission.asStateFlow()
    
    private val _isLocationEnabled = MutableStateFlow(false)
    val isLocationEnabled: StateFlow<Boolean> = _isLocationEnabled.asStateFlow()
    
    init {
        updatePermissionStates()
    }
    
    /**
     * 更新权限状态
     */
    fun updatePermissionStates() {
        val oldPermission = _hasLocationPermission.value
        val oldLocationEnabled = _isLocationEnabled.value
        
        val newPermission = PermissionHelper.hasLocationPermission(context)
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val newLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        
        _hasLocationPermission.value = newPermission
        _isLocationEnabled.value = newLocationEnabled
        
        if (oldPermission != newPermission || oldLocationEnabled != newLocationEnabled) {
            Timber.d("权限状态变化 - 权限: $oldPermission -> $newPermission, 位置服务: $oldLocationEnabled -> $newLocationEnabled")
        }
    }
    
    /**
     * 检查权限是否刚被授予
     */
    fun wasPermissionJustGranted(): Boolean {
        val currentPermission = PermissionHelper.hasLocationPermission(context)
        val wasJustGranted = !_hasLocationPermission.value && currentPermission
        
        if (wasJustGranted) {
            Timber.d("检测到权限刚被授予")
            updatePermissionStates()
        }
        
        return wasJustGranted
    }
} 