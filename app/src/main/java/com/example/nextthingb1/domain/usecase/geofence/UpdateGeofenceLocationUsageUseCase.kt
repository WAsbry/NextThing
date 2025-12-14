package com.example.nextthingb1.domain.usecase.geofence

import com.example.nextthingb1.domain.repository.GeofenceLocationRepository
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * 更新地理围栏地点的使用统计
 *
 * 功能:
 * - 增加使用次数 (usageCount++)
 * - 更新最后使用时间 (lastUsed = now)
 * - 自动识别常用地点 (usageCount >= 3 且最近30天使用过)
 */
class UpdateGeofenceLocationUsageUseCase @Inject constructor(
    private val geofenceLocationRepository: GeofenceLocationRepository
) {
    companion object {
        private const val TAG = "UpdateGeofenceLocationUsage"
        private const val FREQUENT_THRESHOLD = 3 // 使用3次以上标记为常用
        private const val FREQUENT_DAYS = 30L // 30天内使用过才算常用
    }

    /**
     * 更新指定地点的使用统计
     *
     * @param locationId 地理围栏地点ID
     * @return Result<Unit> 更新结果
     */
    suspend operator fun invoke(locationId: String): Result<Unit> {
        return try {
            // 1. 获取当前地点信息
            val location = geofenceLocationRepository.getLocationById(locationId).first()
                ?: return Result.failure(Exception("地点不存在: $locationId"))

            val now = LocalDateTime.now()

            // 2. 更新使用统计
            val updatedUsageCount = location.usageCount + 1
            val updatedLastUsed = now

            // 3. 判断是否应标记为常用地点
            // 规则: 使用次数 >= 3 且最近30天使用过
            val shouldBeFrequent = updatedUsageCount >= FREQUENT_THRESHOLD &&
                    (location.lastUsed == null || location.lastUsed.isAfter(now.minusDays(FREQUENT_DAYS)))

            // 4. 更新地点
            val updatedLocation = location.copy(
                usageCount = updatedUsageCount,
                lastUsed = updatedLastUsed,
                isFrequent = shouldBeFrequent
            )

            geofenceLocationRepository.update(updatedLocation)

            Timber.tag(TAG).d("✅ 使用统计已更新: ${location.locationInfo.locationName}")
            Timber.tag(TAG).d("   使用次数: ${location.usageCount} → $updatedUsageCount")
            if (shouldBeFrequent && !location.isFrequent) {
                Timber.tag(TAG).d("   ⭐ 自动标记为常用地点")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "更新使用统计异常")
            Result.failure(e)
        }
    }

    /**
     * 批量更新常用地点标记
     * 用于定时任务或数据修复
     */
    suspend fun updateAllFrequentFlags(): Result<Int> {
        return try {
            val locations = geofenceLocationRepository.getAllLocations().first()
            val now = LocalDateTime.now()
            val thirtyDaysAgo = now.minusDays(FREQUENT_DAYS)
            var updatedCount = 0

            locations.forEach { location ->
                val shouldBeFrequent = location.usageCount >= FREQUENT_THRESHOLD &&
                        location.lastUsed != null &&
                        location.lastUsed.isAfter(thirtyDaysAgo)

                if (location.isFrequent != shouldBeFrequent) {
                    val updated = location.copy(isFrequent = shouldBeFrequent)
                    geofenceLocationRepository.update(updated)
                    updatedCount++

                    Timber.tag(TAG).d(
                        "${if (shouldBeFrequent) "⭐ 标记" else "❌ 取消"}常用: ${location.locationInfo.locationName}"
                    )
                }
            }

            Timber.tag(TAG).i("✅ 批量更新完成，共更新 $updatedCount 个地点")
            Result.success(updatedCount)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "批量更新常用地点失败")
            Result.failure(e)
        }
    }
}
