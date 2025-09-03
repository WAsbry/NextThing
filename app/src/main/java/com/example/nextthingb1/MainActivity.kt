package com.example.nextthingb1

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.nextthingb1.presentation.navigation.NextThingNavigation
import com.example.nextthingb1.presentation.theme.NextThingB1Theme
import com.example.nextthingb1.util.PermissionHelper
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

// 用于在Compose中访问权限请求器的CompositionLocal
val LocalPermissionLauncher = staticCompositionLocalOf<ActivityResultLauncher<Array<String>>?> { null }

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<Array<String>>
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化位置权限请求器
            locationPermissionLauncher = PermissionHelper.createLocationPermissionLauncher(this) { isGranted ->
        if (isGranted) {
            Timber.d("位置权限已授予")
            // 通知应用权限状态已更改
            sendBroadcast(android.content.Intent("com.example.nextthingb1.LOCATION_PERMISSION_GRANTED"))
        } else {
            Timber.w("位置权限被拒绝")
        }
    }
        
        enableEdgeToEdge()
        setContent {
            NextThingB1Theme {
                CompositionLocalProvider(LocalPermissionLauncher provides locationPermissionLauncher) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        NextThingApp()
                    }
                }
            }
        }
        
        // 启动JWT测试（仅用于调试）
        startJwtTest()
    }
    
    /**
     * 启动JWT测试Activity
     */
    private fun startJwtTest() {
        try {
            val intent = Intent(this, JwtTestActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e, "启动JWT测试失败")
        }
    }
}

@Composable
fun NextThingApp() {
    val navController = rememberNavController()
    
    NextThingNavigation(navController = navController)
}

@Preview(showBackground = true)
@Composable
fun NextThingAppPreview() {
    NextThingB1Theme {
        NextThingApp()
    }
} 