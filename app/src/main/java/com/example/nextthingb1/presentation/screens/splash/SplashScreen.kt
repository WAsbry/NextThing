package com.example.nextthingb1.presentation.screens.splash

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nextthingb1.domain.usecase.UserUseCases
import com.example.nextthingb1.presentation.theme.BgPrimary
import com.example.nextthingb1.presentation.theme.Primary
import com.example.nextthingb1.presentation.theme.TextSecondary
import com.example.nextthingb1.util.PermissionHelper
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * 启动页
 *
 * 在进入应用前完成所有权限申请，确保首页进入时位置权限已就绪。
 * 申请完成（无论授予还是拒绝）后，根据登录状态跳转到登录页或首页。
 */
@Composable
fun SplashScreen(
    userUseCases: UserUseCases,
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val context = LocalContext.current
    // true = 权限流程已完成（授予或跳过），可以继续导航
    var permissionFlowDone by remember { mutableStateOf(false) }

    // 在 Composable 内部注册权限请求器（无需 Activity 层传入）
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.any { it }
        Timber.tag("Splash").d("位置权限结果: ${if (granted) "已授予" else "已拒绝"}")
        permissionFlowDone = true
    }

    // 第一步：检查并请求位置权限
    LaunchedEffect(Unit) {
        val hasPermission = PermissionHelper.hasLocationPermission(context)
        if (hasPermission) {
            Timber.tag("Splash").d("位置权限已存在，跳过申请")
            permissionFlowDone = true
        } else {
            Timber.tag("Splash").d("位置权限未授予，弹出申请对话框")
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // 第二步：权限流程完成后，判断登录状态并跳转
    LaunchedEffect(permissionFlowDone) {
        if (!permissionFlowDone) return@LaunchedEffect
        val currentUser = userUseCases.getCurrentUser().first()
        Timber.tag("Splash").d("跳转: ${if (currentUser != null) "首页" else "登录页"}")
        if (currentUser != null) onNavigateToHome() else onNavigateToLogin()
    }

    // 启动页 UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "NextThing",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "正在初始化...",
                fontSize = 14.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator(color = Primary)
        }
    }
}
