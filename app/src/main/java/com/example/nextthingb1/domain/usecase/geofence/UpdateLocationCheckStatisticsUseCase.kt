package com.example.nextthingb1.domain.usecase.geofence

import com.example.nextthingb1.domain.repository.GeofenceLocationRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * 更新地理围栏地点的月度检查统计
 *
 * 功能:
 * - 记录每次地理围栏检查
 * - 记录是否命中（用户在围栏内）
 * - 自动处理跨月重置逻辑
 * - 用于计算命中率
 */
class UpdateLocationCheckStatisticsUseCase @Inject constructor(
    private val geofenceLocationRepository: GeofenceLocationRepository
) {
    companion object {
        private const val TAG = "UpdateLocationCheckStatistics"
    }

    /**
     * 更新指定地点的月度检查统计
     *
     * @param locationId 地理围栏地点ID
     * @param isHit 是否命中（用户在围栏内）
     * @return Result<Unit> 更新结果
     */
    suspend operator fun invoke(locationId: String, isHit: Boolean): Result<Unit> {
        return try {
            geofenceLocationRepository.incrementCheckStatistics(locationId, isHit)

            Timber.tag(TAG).d("✅ 月度统计已更新")
            Timber.tag(TAG).d("   地点ID: $locationId")
            Timber.tag(TAG).d("   是否命中: ${if (isHit) "✅ 在围栏内" else "❌ 在围栏外"}")

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "更新月度统计异常")
            Result.failure(e)
        }
    }

    /**
     * 重置所有地点的月度统计
     * 用于每月1号批量重置，或手动重置
     *
     * @return Result<Int> 重置的地点数量
     */
    suspend fun resetAllStatistics(): Result<Int> {
        return try {
            val count = geofenceLocationRepository.resetMonthlyStatistics()

            Timber.tag(TAG).i("✅ 月度统计已重置")
            Timber.tag(TAG).i("   重置地点数: $count")

            Result.success(count)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "重置月度统计失败")
            Result.failure(e)
        }
    }
}
