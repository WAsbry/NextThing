package com.example.nextthingb1.domain.service

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.nextthingb1.receiver.GeofenceBroadcastReceiver
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import timber.log.Timber

/**
 * 地理围栏管理器
 *
 * 负责管理Android系统级地理围栏的注册、注销等操作
 * 使用 Google Play Services Location API
 */
interface GeofenceManager {

    /**
     * 注册地理围栏
     *
     * @param locationId 地点ID（用作geofence的requestId）
     * @param latitude 纬度
     * @param longitude 经度
     * @param radius 半径（米）
     * @return Result<Unit> 成功或失败
     */
    suspend fun registerGeofence(
        locationId: String,
        latitude: Double,
        longitude: Double,
        radius: Float
    ): Result<Unit>

    /**
     * 批量注册地理围栏
     *
     * @param geofences 地理围栏列表
     * @return Result<Int> 成功注册的数量
     */
    suspend fun registerGeofences(
        geofences: List<GeofenceData>
    ): Result<Int>

    /**
     * 移除地理围栏
     *
     * @param locationId 地点ID
     * @return Result<Unit> 成功或失败
     */
    suspend fun removeGeofence(locationId: String): Result<Unit>

    /**
     * 批量移除地理围栏
     *
     * @param locationIds 地点ID列表
     * @return Result<Unit> 成功或失败
     */
    suspend fun removeGeofences(locationIds: List<String>): Result<Unit>

    /**
     * 移除所有地理围栏
     *
     * @return Result<Unit> 成功或失败
     */
    suspend fun removeAllGeofences(): Result<Unit>

    /**
     * 检查位置权限是否已授予
     *
     * @return true 如果有权限
     */
    fun hasLocationPermission(): Boolean

    /**
     * 检查后台位置权限是否已授予（Android 10+）
     *
     * @return true 如果有后台位置权限
     */
    fun hasBackgroundLocationPermission(): Boolean
}

/**
 * 地理围栏数据类
 */
data class GeofenceData(
    val locationId: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Float
)

/**
 * 地理围栏管理器实现
 */
class GeofenceManagerImpl(
    private val context: Context
) : GeofenceManager {

    companion object {
        private const val TAG = "GeofenceManager"

        // 地理围栏过期时间（永不过期）
        private const val GEOFENCE_EXPIRATION_IN_MILLISECONDS = Geofence.NEVER_EXPIRE

        // 地理围栏停留时间（0 = 立即触发）
        private const val GEOFENCE_LOITERING_DELAY = 0

        // PendingIntent 请求码
        private const val GEOFENCE_PENDING_INTENT_REQUEST_CODE = 1001
    }

    private val geofencingClient: GeofencingClient by lazy {
        LocationServices.getGeofencingClient(context)
    }

    // 延迟初始化 PendingIntent
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            GEOFENCE_PENDING_INTENT_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    override suspend fun registerGeofence(
        locationId: String,
        latitude: Double,
        longitude: Double,
        radius: Float
    ): Result<Unit> {
        return try {
            Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Timber.tag(TAG).d("注册地理围栏")
            Timber.tag(TAG).d("  locationId: $locationId")
            Timber.tag(TAG).d("  坐标: ($latitude, $longitude)")
            Timber.tag(TAG).d("  半径: ${radius}米")

            // 1. 检查权限
            if (!hasLocationPermission()) {
                Timber.tag(TAG).e("❌ 缺少位置权限")
                return Result.failure(SecurityException("缺少位置权限"))
            }

            // 2. 构建 Geofence 对象
            val geofence = buildGeofence(locationId, latitude, longitude, radius)

            // 3. 构建 GeofencingRequest
            val geofencingRequest = buildGeofencingRequest(listOf(geofence))

            // 4. 添加地理围栏
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
                .addOnSuccessListener {
                    Timber.tag(TAG).d("✅ 地理围栏注册成功")
                    Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                }
                .addOnFailureListener { e ->
                    Timber.tag(TAG).e(e, "❌ 地理围栏注册失败")
                    Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                }

            Result.success(Unit)
        } catch (e: SecurityException) {
            Timber.tag(TAG).e(e, "❌ 权限异常")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ 注册地理围栏异常")
            Result.failure(e)
        }
    }

    override suspend fun registerGeofences(geofences: List<GeofenceData>): Result<Int> {
        return try {
            Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Timber.tag(TAG).d("批量注册 ${geofences.size} 个地理围栏")

            if (!hasLocationPermission()) {
                Timber.tag(TAG).e("❌ 缺少位置权限")
                return Result.failure(SecurityException("缺少位置权限"))
            }

            if (geofences.isEmpty()) {
                Timber.tag(TAG).w("⚠️ 没有需要注册的地理围栏")
                return Result.success(0)
            }

            val geofenceList = geofences.map { data ->
                buildGeofence(data.locationId, data.latitude, data.longitude, data.radius)
            }

            val geofencingRequest = buildGeofencingRequest(geofenceList)

            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
                .addOnSuccessListener {
                    Timber.tag(TAG).d("✅ 批量注册成功: ${geofences.size} 个")
                    Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                }
                .addOnFailureListener { e ->
                    Timber.tag(TAG).e(e, "❌ 批量注册失败")
                    Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                }

            Result.success(geofences.size)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ 批量注册异常")
            Result.failure(e)
        }
    }

    override suspend fun removeGeofence(locationId: String): Result<Unit> {
        return removeGeofences(listOf(locationId))
    }

    override suspend fun removeGeofences(locationIds: List<String>): Result<Unit> {
        return try {
            Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Timber.tag(TAG).d("移除 ${locationIds.size} 个地理围栏")
            Timber.tag(TAG).d("  IDs: $locationIds")

            geofencingClient.removeGeofences(locationIds)
                .addOnSuccessListener {
                    Timber.tag(TAG).d("✅ 移除成功")
                    Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                }
                .addOnFailureListener { e ->
                    Timber.tag(TAG).e(e, "❌ 移除失败")
                    Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ 移除地理围栏异常")
            Result.failure(e)
        }
    }

    override suspend fun removeAllGeofences(): Result<Unit> {
        return try {
            Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Timber.tag(TAG).d("移除所有地理围栏")

            geofencingClient.removeGeofences(geofencePendingIntent)
                .addOnSuccessListener {
                    Timber.tag(TAG).d("✅ 所有地理围栏已移除")
                    Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                }
                .addOnFailureListener { e ->
                    Timber.tag(TAG).e(e, "❌ 移除所有地理围栏失败")
                    Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ 移除所有地理围栏异常")
            Result.failure(e)
        }
    }

    override fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun hasBackgroundLocationPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 10 以下不需要后台位置权限
            true
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 构建 Geofence 对象
     */
    private fun buildGeofence(
        locationId: String,
        latitude: Double,
        longitude: Double,
        radius: Float
    ): Geofence {
        return Geofence.Builder()
            .setRequestId(locationId)
            .setCircularRegion(
                latitude,
                longitude,
                radius
            )
            .setExpirationDuration(GEOFENCE_EXPIRATION_IN_MILLISECONDS)
            .setLoiteringDelay(GEOFENCE_LOITERING_DELAY)
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER or
                        Geofence.GEOFENCE_TRANSITION_EXIT
            )
            .build()
    }

    /**
     * 构建 GeofencingRequest
     */
    private fun buildGeofencingRequest(geofences: List<Geofence>): GeofencingRequest {
        return GeofencingRequest.Builder().apply {
            // 初始触发器：如果用户已在围栏内，立即触发ENTER事件
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(geofences)
        }.build()
    }
}
