package com.example.nextthingb1.domain.usecase.geofence

import com.example.nextthingb1.domain.model.GeofenceCheckResult
import com.example.nextthingb1.domain.repository.TaskGeofenceRepository
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * 更新任务地理围栏的检查结果
 *
 * 功能:
 * - 记录最后检查时间
 * - 记录检查结果 (INSIDE/OUTSIDE/UNAVAILABLE等)
 * - 用于追踪任务的地理围栏检查历史
 */
class UpdateTaskGeofenceCheckResultUseCase @Inject constructor(
    private val taskGeofenceRepository: TaskGeofenceRepository
) {
    companion object {
        private const val TAG = "UpdateTaskGeofenceCheckResult"
    }

    /**
     * 更新指定任务的地理围栏检查结果
     *
     * @param taskId 任务ID
     * @param checkTime 检查时间
     * @param checkResult 检查结果
     * @return Result<Unit> 更新结果
     */
    suspend operator fun invoke(
        taskId: String,
        checkTime: LocalDateTime,
        checkResult: GeofenceCheckResult
    ): Result<Unit> {
        return try {
            // 1. 获取任务的地理围栏关联
            val taskGeofence = taskGeofenceRepository.getByTaskId(taskId).first()
                ?: return Result.failure(Exception("任务未关联地理围栏: $taskId"))

            // 2. 更新检查结果
            val updated = taskGeofence.copy(
                lastCheckTime = checkTime,
                lastCheckResult = checkResult
            )

            taskGeofenceRepository.update(updated)

            Timber.tag(TAG).d("✅ 检查结果已记录")
            Timber.tag(TAG).d("   任务ID: $taskId")
            Timber.tag(TAG).d("   检查时间: $checkTime")
            Timber.tag(TAG).d("   检查结果: $checkResult")

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "更新检查结果异常")
            Result.failure(e)
        }
    }
}
