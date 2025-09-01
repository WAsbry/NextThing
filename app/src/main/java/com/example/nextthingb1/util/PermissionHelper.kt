package com.example.nextthingb1.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionHelper {
    
    val LOCATION_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    
    /**
     * 检查是否有位置权限（只检查基础权限）
     */
    fun hasLocationPermission(context: Context): Boolean {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val hasPermission = fineLocationGranted || coarseLocationGranted
        
        android.util.Log.d("PermissionHelper", "权限检查 - 精确:$fineLocationGranted, 粗略:$coarseLocationGranted, 结果:$hasPermission")
        
        return hasPermission
    }
    
    /**
     * 检查是否应该显示权限说明
     */
    fun shouldShowLocationPermissionRationale(activity: ComponentActivity): Boolean {
        return LOCATION_PERMISSIONS.any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
    }
    
    /**
     * 创建位置权限请求启动器
     */
    fun createLocationPermissionLauncher(
        activity: ComponentActivity,
        onPermissionResult: (Boolean) -> Unit
    ): ActivityResultLauncher<Array<String>> {
        return activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val isGranted = permissions.values.all { it }
            onPermissionResult(isGranted)
        }
    }
    
    /**
     * 打开应用设置页面
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }
    
    /**
     * 打开位置设置页面
     */
    fun openLocationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        context.startActivity(intent)
    }
} 