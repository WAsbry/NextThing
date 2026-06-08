package com.nextthing.app.presentation.screens.splash

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextthing.app.domain.usecase.UserUseCases
import com.nextthing.app.presentation.theme.BgPrimary
import com.nextthing.app.performance.StartupTracker
import com.nextthing.app.presentation.theme.Primary
import com.nextthing.app.presentation.theme.TextSecondary
import com.nextthing.app.presentation.theme.TextMuted
import com.nextthing.app.NextThingApplication
import com.nextthing.app.util.PermissionHelper
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
    var hasNavigated by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.any { it }
        Timber.tag("Splash").d("位置权限结果: ${if (granted) "已授予" else "已拒绝"}")
    }

    // 合并为单个 LaunchedEffect，避免两个 Effect 之间的时序竞争
    LaunchedEffect(Unit) {
        StartupTracker.record("splash_composed")

        val hasPermission = PermissionHelper.hasLocationPermission(context)
        if (!hasPermission) {
            Timber.tag("Splash").d("位置权限未授予，弹出申请对话框")
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            Timber.tag("Splash").d("位置权限已存在，跳过申请")
        }

        // 查询用户状态并导航（防重复）
        if (!hasNavigated) {
            hasNavigated = true
            val currentUser = userUseCases.getCurrentUser().first()
            Timber.tag("Splash").d("跳转: ${if (currentUser != null) "首页" else "登录页"}")
            StartupTracker.record("first_screen_ready")
            NextThingApplication.onFirstScreenReady()
            if (currentUser != null) onNavigateToHome() else onNavigateToLogin()
        }
    }

    // 启动页 UI — NT 图标 + NextThing 文字 + 脉冲光晕
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // NT 图标（带脉冲光晕）
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(22.dp),
                        ambientColor = Primary.copy(alpha = 0.25f),
                        spotColor = Primary.copy(alpha = 0.35f)
                    )
                    .background(
                        color = Primary,
                        shape = RoundedCornerShape(22.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "NT",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = (-1).sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // NextThing 文字
            Text(
                text = "NextThing",
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Primary,
                letterSpacing = (-0.8).sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 副标题
            Text(
                text = "你的 AI 伙伴",
                fontSize = 14.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(60.dp))

            // 加载指示器
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Primary,
                    strokeWidth = 2.dp
                )
                Text(
                    text = "正在准备...",
                    fontSize = 13.sp,
                    color = TextMuted
                )
            }
        }
    }
}
