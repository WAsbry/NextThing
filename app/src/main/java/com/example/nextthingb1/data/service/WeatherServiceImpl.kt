package com.example.nextthingb1.data.service

import android.content.Context
import com.example.nextthingb1.QWeatherJwtGenerator
import com.example.nextthingb1.domain.model.*
import com.example.nextthingb1.domain.service.WeatherService
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.IOException
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 和风天气API响应数据类
 */
data class QWeatherNowResponse(
    @SerializedName("code") val code: String,
    @SerializedName("now") val now: QWeatherNow?,
    @SerializedName("updateTime") val updateTime: String?,
    @SerializedName("fxLink") val fxLink: String?
)

data class QWeatherNow(
    @SerializedName("obsTime") val obsTime: String,
    @SerializedName("temp") val temp: String,
    @SerializedName("feelsLike") val feelsLike: String,
    @SerializedName("icon") val icon: String,
    @SerializedName("text") val text: String,
    @SerializedName("wind360") val wind360: String,
    @SerializedName("windDir") val windDir: String,
    @SerializedName("windScale") val windScale: String,
    @SerializedName("windSpeed") val windSpeed: String,
    @SerializedName("humidity") val humidity: String,
    @SerializedName("precip") val precip: String,
    @SerializedName("pressure") val pressure: String,
    @SerializedName("vis") val vis: String,
    @SerializedName("cloud") val cloud: String?,
    @SerializedName("dew") val dew: String?
)

@Singleton
class WeatherServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : WeatherService {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    private var lastWeatherUpdateTime: Long = 0
    private var cachedWeather: WeatherInfo? = null
    
    // 天气状态流
    private val _weatherUpdates = MutableStateFlow<WeatherInfo?>(null)
    
    companion object {
        private const val WEATHER_CACHE_DURATION = 15 * 60 * 1000L // 15分钟缓存
        private const val BASE_URL = "https://nj7fbyrtf3.re.qweatherapi.com/v7"
        private const val REQUEST_TIMEOUT = 30000L // 30秒超时
    }

    /**
     * 获取应用的SHA-1证书指纹，用于和风天气API的Android应用限制
     */
    private fun getAppCertificateFingerprint(context: Context): String {
        return try {
            val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val packageInfo = context.packageManager.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
                )
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                val packageInfo = context.packageManager.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.GET_SIGNATURES
                )
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            if (!signatures.isNullOrEmpty()) {
                val cert = signatures[0]
                val digest = java.security.MessageDigest.getInstance("SHA1")
                val certHash = digest.digest(cert.toByteArray())

                val fingerprint = certHash.joinToString(":") { "%02X".format(it) }
                Timber.d("🔐 [WeatherService] 应用证书指纹: $fingerprint")
                fingerprint
            } else {
                Timber.w("⚠️ [WeatherService] 无法获取应用签名")
                ""
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ [WeatherService] 获取证书指纹失败")
            ""
        }
    }

    override suspend fun getCurrentWeather(location: LocationInfo, forceRefresh: Boolean): Result<WeatherInfo> {
        val startTime = System.currentTimeMillis()
        Timber.d("============================================================")
        Timber.d("🌤️ [WeatherService] 🚀 开始获取天气数据")
        Timber.d("🌤️ [WeatherService] 📋 参数详情:")
        Timber.d("   - 位置名称: ${location.locationName}")
        Timber.d("   - 经纬度: (${location.latitude}, ${location.longitude})")
        Timber.d("   - 强制刷新: $forceRefresh")
        Timber.d("   - 开始时间: ${java.text.SimpleDateFormat("HH:mm:ss.SSS").format(java.util.Date())}")

        return withContext(Dispatchers.IO) {
            try {
                // 检查缓存
                Timber.d("📦 [WeatherService] 🔍 检查天气缓存...")
                Timber.d("📦 [WeatherService] 📊 缓存状态:")
                Timber.d("   - 是否有缓存: ${cachedWeather != null}")
                Timber.d("   - 强制刷新: $forceRefresh")
                Timber.d("   - 需要刷新: ${shouldRefreshWeather()}")

                if (!forceRefresh && !shouldRefreshWeather()) {
                    cachedWeather?.let {
                        val cacheAge = (System.currentTimeMillis() - lastWeatherUpdateTime) / 1000
                        Timber.d("📦 [WeatherService] ✅ 使用缓存天气数据")
                        Timber.d("   - 天气状况: ${it.condition.displayName}")
                        Timber.d("   - 温度: ${it.temperature}°C")
                        Timber.d("   - 湿度: ${it.humidity}%")
                        Timber.d("   - 缓存年龄: ${cacheAge}秒")
                        Timber.d("   - 位置名称: ${it.locationName}")
                        return@withContext Result.success(it)
                    }
                }

                Timber.d("📦 [WeatherService] ⏭️ 缓存无效或强制刷新，开始实时获取")
                Timber.d("🌐 [WeatherService] 🔄 开始实时天气请求...")

                // 生成JWT token
                Timber.d("🔑 [WeatherService] 🚀 开始生成JWT token...")
                val jwtStartTime = System.currentTimeMillis()
                val jwtToken = QWeatherJwtGenerator.generateJwt(context)
                val jwtDuration = System.currentTimeMillis() - jwtStartTime

                if (jwtToken.isNullOrBlank()) {
                    Timber.e("❌ [WeatherService] JWT token生成失败")
                    Timber.e("   - 生成耗时: ${jwtDuration}ms")
                    return@withContext Result.failure(Exception("无法生成JWT token"))
                }
                Timber.d("✅ [WeatherService] JWT token生成成功")
                Timber.d("   - token长度: ${jwtToken.length}字符")
                Timber.d("   - 生成耗时: ${jwtDuration}ms")
                Timber.d("   - token前缀: ${jwtToken.take(50)}...")

                // 增强的JWT token验证和详细日志
                Timber.d("🔍 [WeatherService] JWT Token详细验证:")
                Timber.d("   - 完整Token: $jwtToken")

                val jwtParts = jwtToken.split(".")
                if (jwtParts.size == 3) {
                    Timber.d("   - ✅ JWT格式正确 (Header.Payload.Signature)")
                    Timber.d("   - Header部分: ${jwtParts[0]}")
                    Timber.d("   - Payload部分: ${jwtParts[1]}")
                    Timber.d("   - Signature部分: ${jwtParts[2]}")

                    // 解析Header
                    try {
                        val headerJson = String(android.util.Base64.decode(jwtParts[0] + "=".repeat((4 - jwtParts[0].length % 4) % 4), android.util.Base64.URL_SAFE))
                        Timber.d("   - 解析的Header: $headerJson")
                    } catch (e: Exception) {
                        Timber.e("   - ❌ Header解析失败: ${e.message}")
                    }

                    // 解析Payload
                    try {
                        val payloadJson = String(android.util.Base64.decode(jwtParts[1] + "=".repeat((4 - jwtParts[1].length % 4) % 4), android.util.Base64.URL_SAFE))
                        Timber.d("   - 解析的Payload: $payloadJson")

                        // 解析Payload中的关键字段
                        val gson = com.google.gson.Gson()
                        val payload = gson.fromJson(payloadJson, Map::class.java)
                        Timber.d("   - SUB (项目ID): ${payload["sub"]}")
                        Timber.d("   - IAT (签发时间): ${payload["iat"]} (${java.util.Date((payload["iat"] as Double).toLong() * 1000)})")
                        Timber.d("   - EXP (过期时间): ${payload["exp"]} (${java.util.Date((payload["exp"] as Double).toLong() * 1000)})")

                        // 检查token是否即将过期
                        val currentTime = System.currentTimeMillis() / 1000
                        val expTime = (payload["exp"] as Double).toLong()
                        val timeToExpiry = expTime - currentTime
                        if (timeToExpiry < 300) { // 5分钟内过期
                            Timber.w("   - ⚠️ Token将在${timeToExpiry}秒后过期")
                        } else {
                            Timber.d("   - ✅ Token有效期剩余${timeToExpiry}秒")
                        }

                    } catch (e: Exception) {
                        Timber.e("   - ❌ Payload解析失败: ${e.message}")
                    }

                } else {
                    Timber.e("   - ❌ JWT格式错误，期望3部分，实际${jwtParts.size}部分")
                    jwtParts.forEachIndexed { index, part ->
                        Timber.e("     部分${index + 1}: $part")
                    }
                }


                // 调用和风天气API
                Timber.d("🌐 [WeatherService] 📡 开始调用和风天气API...")
                val apiStartTime = System.currentTimeMillis()
                val weatherResult = getQWeatherNow(location, jwtToken)
                val apiDuration = System.currentTimeMillis() - apiStartTime
                val totalTime = System.currentTimeMillis() - startTime

                if (weatherResult.isSuccess) {
                    val weatherInfo = weatherResult.getOrNull()!!
                    Timber.d("🌤️ [WeatherService] ✅ 天气获取成功！")
                    Timber.d("🌤️ [WeatherService] ⏱️ 性能统计:")
                    Timber.d("   - API调用耗时: ${apiDuration}ms")
                    Timber.d("   - 总耗时: ${totalTime}ms")
                    Timber.d("🌤️ [WeatherService] 🌡️ 天气数据:")
                    Timber.d("   - 天气状况: ${weatherInfo.condition.displayName}")
                    Timber.d("   - 当前温度: ${weatherInfo.temperature}°C")
                    Timber.d("   - 湿度: ${weatherInfo.humidity}%")
                    Timber.d("   - 风速: ${weatherInfo.windSpeed}km/h")
                    Timber.d("   - PM2.5: ${weatherInfo.pm25}")
                    Timber.d("   - UV指数: ${weatherInfo.uvIndex}")
                    weatherInfo.suggestion?.let { suggestion ->
                        Timber.d("   - 生活建议: ${suggestion.message} (紧急: ${suggestion.isUrgent})")
                    }

                    // 更新缓存
                    updateWeatherCache(weatherInfo)

                    Timber.d("============================================================")
                    Result.success(weatherInfo)
                } else {
                    Timber.e("❌ [WeatherService] 天气获取失败")
                    Timber.e("   - API调用耗时: ${apiDuration}ms")
                    Timber.e("   - 总耗时: ${totalTime}ms")
                    Timber.e("   - 错误信息: ${weatherResult.exceptionOrNull()?.message}")
                    Timber.d("============================================================")
                    weatherResult
                }

            } catch (e: Exception) {
                val totalTime = System.currentTimeMillis() - startTime
                Timber.e(e, "💥 [WeatherService] 天气获取异常，耗时: ${totalTime}ms")
                Timber.d("============================================================")
                Result.failure(e)
            }
        }
    }


    private suspend fun getQWeatherNow(location: LocationInfo, jwtToken: String): Result<WeatherInfo> = 
        withContext(Dispatchers.IO) {
            try {
                Timber.d("🌐 [WeatherService] 📡 准备和风天气API调用...")
                Timber.d("🌐 [WeatherService] 📍 位置参数:")
                Timber.d("   - 输入经纬度: (纬度=${location.latitude}, 经度=${location.longitude})")
                
                // 构建API请求URL - 注意：和风天气API期望经度在前，纬度在后
                val locationParam = "${location.longitude},${location.latitude}"
                val url = "$BASE_URL/weather/now?location=$locationParam"
                
                Timber.d("   - API位置参数: $locationParam (经度,纬度)")
                Timber.d("🌐 [WeatherService] 🔗 请求详情:")
                Timber.d("   - 完整URL: $url")
                Timber.d("   - 基础URL: $BASE_URL")
                Timber.d("   - 请求超时: ${REQUEST_TIMEOUT/1000}秒")
                
                Timber.d("🌐 [WeatherService] 🔧 构建HTTP请求...")
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $jwtToken")
                    .addHeader("User-Agent", "NextThingB1/1.0 (Android)")
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-Type", "application/json")
                    // Android应用限制所需的请求头
                    .addHeader("X-Android-Package-Name", "com.example.nextthingb1")
                    .addHeader("X-Android-Cert", getAppCertificateFingerprint(context))
                    .build()
                
                Timber.d("🌐 [WeatherService] 📤 请求头信息:")
                Timber.d("   - Authorization: Bearer ${jwtToken.take(20)}...")
                Timber.d("   - User-Agent: NextThingB1/1.0")
                Timber.d("   - 请求方法: GET")
                Timber.d("   - 完整JWT Token: $jwtToken")
                
                Timber.d("🌐 [WeatherService] 🚀 发送HTTP请求...")
                val requestStartTime = System.currentTimeMillis()
                val response = httpClient.newCall(request).execute()
                val requestDuration = System.currentTimeMillis() - requestStartTime
                
                Timber.d("🌐 [WeatherService] 📥 收到HTTP响应")
                Timber.d("   - 响应状态码: ${response.code}")
                Timber.d("   - 响应消息: ${response.message}")
                Timber.d("   - 网络请求耗时: ${requestDuration}ms")
                Timber.d("   - 响应成功: ${response.isSuccessful}")
                
                if (response.isSuccessful) {
                    Timber.d("✅ [WeatherService] HTTP请求成功，开始解析响应...")
                    val parseStartTime = System.currentTimeMillis()
                    val body = response.body?.string()
                    
                    if (body.isNullOrEmpty()) {
                        Timber.e("❌ [WeatherService] API响应体为空")
                        return@withContext Result.failure(IOException("API响应为空"))
                    }
                    
                    Timber.d("🌐 [WeatherService] 📦 响应体分析:")
                    Timber.d("   - 响应长度: ${body.length}字符")
                    Timber.d("   - 响应内容预览: ${body.take(200)}...")
                    if (body.length > 200) {
                        Timber.d("   - 响应尾部预览: ...${body.takeLast(100)}")
                    }
                    
                    Timber.d("🔄 [WeatherService] 开始JSON解析...")
                    val weatherResponse = gson.fromJson(body, QWeatherNowResponse::class.java)
                    val parseDuration = System.currentTimeMillis() - parseStartTime
                    
                    Timber.d("🔄 [WeatherService] JSON解析完成")
                    Timber.d("   - 解析耗时: ${parseDuration}ms")
                    Timber.d("   - API响应码: ${weatherResponse.code}")
                    Timber.d("   - 更新时间: ${weatherResponse.updateTime}")
                    Timber.d("   - FX链接: ${weatherResponse.fxLink}")
                    Timber.d("   - 是否有天气数据: ${weatherResponse.now != null}")
                    
                    if (weatherResponse.code == "200" && weatherResponse.now != null) {
                        Timber.d("✅ [WeatherService] 和风天气API调用成功")
                        
                        val nowData = weatherResponse.now
                        Timber.d("🌡️ [WeatherService] 原始天气数据详情:")
                        Timber.d("   - 观测时间: ${nowData.obsTime}")
                        Timber.d("   - 温度: ${nowData.temp}°C")
                        Timber.d("   - 体感温度: ${nowData.feelsLike}°C")
                        Timber.d("   - 天气状况: ${nowData.text}")
                        Timber.d("   - 天气图标: ${nowData.icon}")
                        Timber.d("   - 风向角度: ${nowData.wind360}°")
                        Timber.d("   - 风向: ${nowData.windDir}")
                        Timber.d("   - 风力等级: ${nowData.windScale}")
                        Timber.d("   - 风速: ${nowData.windSpeed}km/h")
                        Timber.d("   - 相对湿度: ${nowData.humidity}%")
                        Timber.d("   - 降水量: ${nowData.precip}mm")
                        Timber.d("   - 大气压强: ${nowData.pressure}hPa")
                        Timber.d("   - 能见度: ${nowData.vis}km")
                        Timber.d("   - 云量: ${nowData.cloud ?: "无数据"}")
                        Timber.d("   - 露点温度: ${nowData.dew ?: "无数据"}°C")
                        
                        Timber.d("🔄 [WeatherService] 开始数据转换...")
                        val convertStartTime = System.currentTimeMillis()
                        val weatherInfo = convertToWeatherInfo(nowData, location)
                        val convertDuration = System.currentTimeMillis() - convertStartTime
                        Timber.d("✅ [WeatherService] 数据转换完成，耗时: ${convertDuration}ms")
                        
                        Result.success(weatherInfo)
                    } else {
                        val errorMsg = "和风天气API返回错误"
                        Timber.e("❌ [WeatherService] $errorMsg")
                        Timber.e("   - API响应码: ${weatherResponse.code}")
                        Timber.e("   - 是否有数据: ${weatherResponse.now != null}")
                        
                        when (weatherResponse.code) {
                            "204" -> Timber.e("   - 错误含义: 请求成功，但你查询的地区暂时没有你需要的数据")
                            "400" -> Timber.e("   - 错误含义: 请求错误，可能是请求参数错误或缺少必需的请求参数")
                            "401" -> Timber.e("   - 错误含义: 认证失败，可能是用户key错误")
                            "403" -> Timber.e("   - 错误含义: 无访问权限，可能是绑定的PackageName、IP地址不一致或数据权限的问题")
                            "404" -> Timber.e("   - 错误含义: 查询的数据或地区不存在")
                            "429" -> Timber.e("   - 错误含义: 超过访问次数或访问频次限制")
                            "500" -> Timber.e("   - 错误含义: 无响应或超时，接口服务异常请联系我们")
                            else -> Timber.e("   - 错误含义: 未知错误码")
                        }
                        
                        Result.failure(Exception("$errorMsg - 响应码: ${weatherResponse.code}"))
                    }
                } else {
                    val errorMsg = "HTTP请求失败 - 状态码: ${response.code}, 消息: ${response.message}"
                    Timber.e("❌ [WeatherService] $errorMsg")
                    
                    // 详细分析HTTP错误
                    when (response.code) {
                        403 -> {
                            Timber.e("🚨 [WeatherService] HTTP 403 超详细诊断分析:")

                            // 请求信息诊断
                            Timber.e("📊 请求信息诊断:")
                            Timber.e("   - 请求URL: $url")
                            Timber.e("   - JWT Token长度: ${jwtToken.length}字符")
                            Timber.e("   - JWT是否为空: ${jwtToken.isEmpty()}")
                            Timber.e("   - JWT格式检查: ${if (jwtToken.split(".").size == 3) "✅ 正确(3部分)" else "❌ 错误格式"}")

                            // JWT payload解析诊断
                            try {
                                val jwtParts = jwtToken.split(".")
                                if (jwtParts.size >= 2) {
                                    val payloadJson = String(android.util.Base64.decode(jwtParts[1] + "=".repeat((4 - jwtParts[1].length % 4) % 4), android.util.Base64.URL_SAFE))
                                    Timber.e("   - JWT Payload: $payloadJson")
                                }
                            } catch (e: Exception) {
                                Timber.e("   - JWT Payload解析失败: ${e.message}")
                            }

                            // 常见403错误原因分析
                            Timber.e("🚨 可能的403错误原因:")
                            Timber.e("   1. PackageName不匹配: 检查控制台绑定的包名是否为'com.example.nextthingb1'")
                            Timber.e("   2. IP白名单限制: 当前IP地址可能未添加到API控制台白名单")
                            Timber.e("   3. JWT凭据问题: 开发者ID(Q0A6F41742)或Kid(T85DFFFK2W)可能不正确")
                            Timber.e("   4. API权限问题: 实时天气API可能未在控制台开通")
                            Timber.e("   5. 配额耗尽: 可能已超出每日/每月调用限额")
                            Timber.e("   6. 域名问题: 当前使用${BASE_URL}，确认是否正确")

                            // 响应头信息
                            Timber.e("📋 响应头信息:")
                            response.headers.forEach { (name, value) ->
                                Timber.e("   - $name: $value")
                            }

                            // 尝试读取响应体以获取更多错误信息
                            try {
                                val errorBody = response.body?.string()
                                if (!errorBody.isNullOrEmpty()) {
                                    Timber.e("📄 和风天气错误响应体: $errorBody")

                                    // 尝试解析JSON错误信息
                                    try {
                                        val gson = com.google.gson.Gson()
                                        val errorResponse = gson.fromJson(errorBody, Map::class.java)
                                        Timber.e("🔍 解析的错误信息:")
                                        errorResponse.forEach { (key, value) ->
                                            Timber.e("   - $key: $value")
                                        }
                                    } catch (jsonE: Exception) {
                                        Timber.e("   - JSON解析失败，原始响应体: $errorBody")
                                    }
                                } else {
                                    Timber.e("   - 错误响应体为空")
                                }
                            } catch (e: Exception) {
                                Timber.e("   - 无法读取错误响应体: ${e.message}")
                            }

                            Timber.e("💡 解决建议:")
                            Timber.e("   1. 登录和风天气开发者控制台检查项目配置")
                            Timber.e("   2. 确认PackageName绑定: com.example.nextthingb1")
                            Timber.e("   3. 添加当前IP到白名单（如果有IP限制）")
                            Timber.e("   4. 检查API订阅状态和剩余配额")
                            Timber.e("   5. 验证开发者ID(Q0A6F41742)和Kid(T85DFFFK2W)是否正确")
                        }
                        401 -> {
                            Timber.e("🔑 [WeatherService] HTTP 401 认证失败详细分析:")
                            Timber.e("📊 JWT Token诊断:")
                            Timber.e("   - Token长度: ${jwtToken.length}字符")
                            Timber.e("   - Token格式: ${if (jwtToken.split(".").size == 3) "✅ 正确" else "❌ 错误"}")

                            // 解析JWT payload查看凭据
                            try {
                                val jwtParts = jwtToken.split(".")
                                if (jwtParts.size >= 2) {
                                    val payloadJson = String(android.util.Base64.decode(jwtParts[1] + "=".repeat((4 - jwtParts[1].length % 4) % 4), android.util.Base64.URL_SAFE))
                                    val gson = com.google.gson.Gson()
                                    val payload = gson.fromJson(payloadJson, Map::class.java)
                                    Timber.e("   - SUB (项目ID): ${payload["sub"]}")
                                    Timber.e("   - IAT (签发时间): ${payload["iat"]}")
                                    Timber.e("   - EXP (过期时间): ${payload["exp"]}")

                                    val currentTime = System.currentTimeMillis() / 1000
                                    val expTime = (payload["exp"] as Double).toLong()
                                    if (expTime < currentTime) {
                                        Timber.e("   - ❌ Token已过期！过期时间: ${java.util.Date(expTime * 1000)}")
                                    } else {
                                        Timber.e("   - ✅ Token未过期，剩余${expTime - currentTime}秒")
                                    }
                                }
                            } catch (e: Exception) {
                                Timber.e("   - ❌ JWT解析失败: ${e.message}")
                            }

                            Timber.e("🔍 可能的401错误原因:")
                            Timber.e("   1. 项目ID错误: 当前使用3KDX498DD3")
                            Timber.e("   2. 凭据ID错误: 当前使用T85DFFFK2W")
                            Timber.e("   3. 私钥不匹配: 检查ed25519-private.pem文件")
                            Timber.e("   4. JWT签名算法错误: 当前使用EdDSA")
                            Timber.e("   5. Token过期: JWT生成时间有问题")
                            Timber.e("   6. IP地址变化: VPN或网络环境改变导致IP认证失败")
                            Timber.e("   7. 设备认证: 手机设备可能需要重新认证")

                            Timber.e("💡 VPN相关解决方案:")
                            Timber.e("   1. 确保VPN已完全关闭")
                            Timber.e("   2. 重启手机网络连接")
                            Timber.e("   3. 等待10-15分钟让和风服务器更新IP记录")
                            Timber.e("   4. 检查控制台是否有IP白名单限制")

                            // 读取响应体获取更多信息
                            try {
                                val errorBody = response.body?.string()
                                if (!errorBody.isNullOrEmpty()) {
                                    Timber.e("📄 401错误响应: $errorBody")
                                }
                            } catch (e: Exception) {
                                Timber.e("   - 无法读取401错误响应: ${e.message}")
                            }
                        }
                        429 -> Timber.e("   - 请求频率过高，已达到限制")
                        500 -> Timber.e("   - 服务器内部错误")
                    }
                    
                    Result.failure(IOException(errorMsg))
                }
            } catch (e: Exception) {
                Timber.e(e, "💥 [WeatherService] API请求异常")
                Result.failure(e)
            }
        }

    private fun convertToWeatherInfo(nowData: QWeatherNow, location: LocationInfo): WeatherInfo {
        Timber.d("🔄 [WeatherService] 开始详细数据转换...")
        
        // 转换天气状况
        Timber.d("🌤️ [WeatherService] 转换天气状况...")
        val condition = mapWeatherCondition(nowData.text, nowData.icon)
        Timber.d("   - 原始文本: '${nowData.text}'")
        Timber.d("   - 原始图标: '${nowData.icon}'")
        Timber.d("   - 映射结果: ${condition.displayName}")
        Timber.d("   - 图标显示: ${condition.iconRes}")
        Timber.d("   - 颜色代码: 0x${condition.color.toString(16).uppercase()}")
        
        // 安全转换数值，避免空值和格式错误
        Timber.d("🔢 [WeatherService] 转换数值数据...")
        val temperature = nowData.temp.toIntOrNull() ?: 0
        val humidity = nowData.humidity.toIntOrNull() ?: 0
        val windSpeed = nowData.windSpeed.toIntOrNull() ?: 0
        
        Timber.d("🔄 [WeatherService] 数值转换结果:")
        Timber.d("   - 温度: '${nowData.temp}' -> ${temperature}°C")
        Timber.d("   - 湿度: '${nowData.humidity}' -> ${humidity}%")
        Timber.d("   - 风速: '${nowData.windSpeed}' -> ${windSpeed}km/h")
        
        // 检查转换是否有失败
        if (nowData.temp.toIntOrNull() == null) {
            Timber.w("⚠️ [WeatherService] 温度转换失败，原始值: '${nowData.temp}', 使用默认值: 0")
        }
        if (nowData.humidity.toIntOrNull() == null) {
            Timber.w("⚠️ [WeatherService] 湿度转换失败，原始值: '${nowData.humidity}', 使用默认值: 0")
        }
        if (nowData.windSpeed.toIntOrNull() == null) {
            Timber.w("⚠️ [WeatherService] 风速转换失败，原始值: '${nowData.windSpeed}', 使用默认值: 0")
        }
        
        Timber.d("🏗️ [WeatherService] 构建WeatherInfo对象...")
        val weatherInfo = WeatherInfo(
            condition = condition,
            temperature = temperature,
            temperatureMax = temperature + 3, // 临时值，实际需要调用每日天气预报API
            temperatureMin = temperature - 3, // 临时值，实际需要调用每日天气预报API
            humidity = humidity,
            windSpeed = windSpeed,
            pm25 = 50, // 需要调用空气质量API获取，这里设置默认值
            uvIndex = 5, // 需要调用天气指数API获取，这里设置默认值
            suggestion = null, // 将在WeatherInfo的getPrioritySuggestion()中计算
            updateTime = LocalDateTime.now(),
            locationName = location.locationName
        )
        
        Timber.d("🏗️ [WeatherService] WeatherInfo对象创建完成:")
        Timber.d("   - 天气状况: ${weatherInfo.condition.displayName}")
        Timber.d("   - 当前温度: ${weatherInfo.temperature}°C")
        Timber.d("   - 最高温度: ${weatherInfo.temperatureMax}°C (临时估算)")
        Timber.d("   - 最低温度: ${weatherInfo.temperatureMin}°C (临时估算)")
        Timber.d("   - 湿度: ${weatherInfo.humidity}%")
        Timber.d("   - 风速: ${weatherInfo.windSpeed}km/h")
        Timber.d("   - PM2.5: ${weatherInfo.pm25} (默认值)")
        Timber.d("   - UV指数: ${weatherInfo.uvIndex} (默认值)")
        Timber.d("   - 更新时间: ${weatherInfo.updateTime}")
        Timber.d("   - 位置名称: ${weatherInfo.locationName}")
        
        // 自动生成建议
        Timber.d("💡 [WeatherService] 生成生活建议...")
        val suggestion = weatherInfo.getPrioritySuggestion()
        
        val finalWeatherInfo = weatherInfo.copy(suggestion = suggestion)
        
        if (suggestion != null) {
            Timber.d("💡 [WeatherService] 生活建议生成成功:")
            Timber.d("   - 建议类型: ${suggestion.type.displayName}")
            Timber.d("   - 建议内容: ${suggestion.message}")
            Timber.d("   - 是否紧急: ${suggestion.isUrgent}")
            Timber.d("   - 优先级: ${suggestion.type.priority}")
        } else {
            Timber.d("💡 [WeatherService] 当前天气无需特别建议")
        }
        
        Timber.d("✅ [WeatherService] 数据转换完全完成")
        return finalWeatherInfo
    }

    private fun mapWeatherCondition(text: String, icon: String): WeatherCondition {
        return when {
            text.contains("晴") -> WeatherCondition.SUNNY
            text.contains("多云") -> WeatherCondition.PARTLY_CLOUDY
            text.contains("阴") -> WeatherCondition.CLOUDY
            text.contains("雨") -> WeatherCondition.RAINY
            text.contains("雷") -> WeatherCondition.THUNDERSTORM
            text.contains("雪") -> WeatherCondition.SNOWY
            text.contains("雾") || text.contains("霾") -> WeatherCondition.FOGGY
            text.contains("风") -> WeatherCondition.WINDY
            else -> WeatherCondition.UNKNOWN
        }
    }

    override fun observeWeatherUpdates(location: LocationInfo): Flow<WeatherInfo> {
        Timber.d("🔄 [WeatherService] 开始监听天气更新...")
        
        // 启动定期更新
        startPeriodicWeatherUpdates(location)
        
        return _weatherUpdates.filterNotNull()
    }

    private fun startPeriodicWeatherUpdates(location: LocationInfo) {
        // 这里可以实现定期更新逻辑
        // 目前简化处理，可以根据需要添加定时器
        Timber.d("🔄 [WeatherService] 天气监听已启动")
    }

    override fun shouldRefreshWeather(): Boolean {
        return cachedWeather == null || 
               (System.currentTimeMillis() - lastWeatherUpdateTime) > WEATHER_CACHE_DURATION
    }

    override suspend fun getCachedWeather(): WeatherInfo? {
        return cachedWeather
    }

    override suspend fun clearWeatherCache() {
        cachedWeather = null
        lastWeatherUpdateTime = 0
        _weatherUpdates.value = null
        Timber.d("🗑️ [WeatherService] 天气缓存已清除")
    }

    private fun updateWeatherCache(weatherInfo: WeatherInfo) {
        Timber.d("💾 [WeatherService] 开始更新天气缓存...")
        
        val oldCacheTime = lastWeatherUpdateTime
        val currentTime = System.currentTimeMillis()
        
        cachedWeather = weatherInfo
        lastWeatherUpdateTime = currentTime
        _weatherUpdates.value = weatherInfo
        
        val cacheAgeBeforeUpdate = if (oldCacheTime > 0) {
            (currentTime - oldCacheTime) / 1000
        } else {
            -1
        }
        
        Timber.d("💾 [WeatherService] 天气缓存更新完成")
        Timber.d("   - 天气状况: ${weatherInfo.condition.displayName}")
        Timber.d("   - 温度: ${weatherInfo.temperature}°C")
        Timber.d("   - 湿度: ${weatherInfo.humidity}%")
        Timber.d("   - 位置: ${weatherInfo.locationName}")
        Timber.d("   - 更新时间: ${java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date(currentTime))}")
        if (cacheAgeBeforeUpdate >= 0) {
            Timber.d("   - 上次缓存年龄: ${cacheAgeBeforeUpdate}秒")
        } else {
            Timber.d("   - 首次缓存")
        }
        Timber.d("   - 缓存有效期: ${WEATHER_CACHE_DURATION/1000/60}分钟")
        Timber.d("   - StateFlow已通知: ${_weatherUpdates.value != null}")
    }
} 