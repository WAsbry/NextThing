package com.example.nextthingb1

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nextthingb1.presentation.theme.NextThingB1Theme

class JwtTestActivity : ComponentActivity() {
    private val TAG = "QWeatherJWT"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NextThingB1Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    JwtTestScreen()
                }
            }
        }

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            generateJwt()
        }, 1000)
    }
    
    private fun generateJwt() {
        Thread {
            try {
                Log.d(TAG, "开始生成JWT...")
                
                val jwt = QWeatherJwtGenerator.generateJwt(this@JwtTestActivity)
                
                Log.d(TAG, " 生成的和风天气JWT成功：")
                Log.d(TAG, "JWT: $jwt")
                Log.d(TAG, "JWT长度：${jwt.length}字符")
                
                val parts = jwt.split(".")
                if (parts.size == 3) {
                    Log.d(TAG, " JWT格式正确：Header.Payload.Signature")
                    Log.d(TAG, "Header: ${parts[0]}")
                    Log.d(TAG, "Payload: ${parts[1]}")
                    Log.d(TAG, "Signature: ${parts[2]}")
                } else {
                    Log.w(TAG, " JWT格式可能有问题，期望3个部分，实际${parts.size}个部分")
                }

            } catch (e: Exception) {
                Log.e(TAG, " JWT生成失败：", e)
                Log.e(TAG, "错误类型: ${e.javaClass.simpleName}")
                Log.e(TAG, "错误信息: ${e.message}")
                
                if (e.message != null && e.message!!.contains("assets")) {
                    Log.e(TAG, " 解决方案：请确保 ed25519-private.pem 文件已放入 app/src/main/assets/ 目录")
                }
            }
        }.start()
    }
}

@Composable
fun JwtTestScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "JWT生成测试",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "正在生成JWT，请查看Logcat",
            fontSize = 16.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "日志标签：QWeatherJWT",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.secondary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "在Android Studio底部Logcat面板中搜索此标签查看结果",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}