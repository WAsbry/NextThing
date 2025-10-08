package com.example.nextthingb1.domain.usecase

import com.example.nextthingb1.domain.model.Task
import com.example.nextthingb1.domain.model.TaskCategory
import com.example.nextthingb1.domain.model.TaskStatistics
import com.example.nextthingb1.domain.model.TaskStatus
import com.example.nextthingb1.domain.model.RepeatFrequency
import com.example.nextthingb1.domain.model.TaskImportanceUrgency
import com.example.nextthingb1.domain.repository.TaskRepository
import com.example.nextthingb1.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class TaskUseCases @Inject constructor(
    val getAllTasks: GetAllTasksUseCase,
    val getTodayTasks: GetTodayTasksUseCase,
    val createTask: CreateTaskUseCase,
    val updateTask: UpdateTaskUseCase,
    val deleteTask: DeleteTaskUseCase,
    val deleteAllTasks: DeleteAllTasksUseCase,
    val toggleTaskStatus: ToggleTaskStatusUseCase,
    val deferTask: DeferTaskUseCase,
    val getTaskStatistics: GetTaskStatisticsUseCase,
    val searchTasks: SearchTasksUseCase,
    val getTasksByCategory: GetTasksByCategoryUseCase,
    val getUrgentTasks: GetUrgentTasksUseCase,
    val getEarliestTaskDate: GetEarliestTaskDateUseCase,
    val locationRepository: LocationRepository
)

class GetAllTasksUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    operator fun invoke(): Flow<List<Task>> {
        return repository.getAllTasks()
    }
}

class GetTodayTasksUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    operator fun invoke(): Flow<List<Task>> {
        return repository.getTodayTasks()
    }
}

/**
 * 创建任务用例
 *
 * 【NotificationTest】通知流程 - 第1步：创建任务
 * 当用户创建任务时，此用例会被调用
 */
class CreateTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val taskAlarmManager: com.example.nextthingb1.util.TaskAlarmManager
) {
    companion object {
        private const val TAG = "NotificationTask"
    }

    suspend operator fun invoke(
        title: String,
        description: String = "",
        category: TaskCategory = TaskCategory.WORK,
        dueDate: LocalDateTime? = null,
        tags: List<String> = emptyList(),
        imageUri: String? = null,
        repeatFrequency: RepeatFrequency = RepeatFrequency(),
        notificationStrategyId: String? = null,
        importanceUrgency: TaskImportanceUrgency? = null
    ): Result<String> {
        Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Timber.tag(TAG).d("【UseCase】CreateTaskUseCase 开始执行")
        Timber.tag(TAG).d("接收到的参数：")
        Timber.tag(TAG).d("  title: $title")
        Timber.tag(TAG).d("  description: $description")
        Timber.tag(TAG).d("  category: ${category.displayName}")
        Timber.tag(TAG).d("  dueDate: $dueDate")
        Timber.tag(TAG).d("  notificationStrategyId: $notificationStrategyId")

        return try {
            if (title.isBlank()) {
                Timber.tag(TAG).e("❌ 任务标题为空，创建失败")
                Result.failure(IllegalArgumentException("任务标题不能为空"))
            } else {
                // 如果未设置截止时间，默认为今天23:59:59
                // 这样确保所有任务都有截止时间，符合逾期检测逻辑
                val finalDueDate = dueDate ?: LocalDateTime.now().toLocalDate().atTime(23, 59, 59)

                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                Timber.tag(TAG).d("准备创建Task对象:")
                Timber.tag(TAG).d("   标题: $title")
                Timber.tag(TAG).d("   描述: $description")
                Timber.tag(TAG).d("   分类: ${category.displayName}")
                Timber.tag(TAG).d("   截止时间: ${finalDueDate.format(formatter)}")
                Timber.tag(TAG).d("   通知策略ID: $notificationStrategyId")
                Timber.tag(TAG).d("   重复频率: ${repeatFrequency.type}")

                val task = Task(
                    title = title.trim(),
                    description = description.trim(),
                    category = category,
                    dueDate = finalDueDate,
                    tags = tags,
                    isUrgent = finalDueDate.isBefore(LocalDateTime.now().plusHours(2)),
                    imageUri = imageUri,
                    repeatFrequency = repeatFrequency,
                    notificationStrategyId = notificationStrategyId,
                    importanceUrgency = importanceUrgency
                )

                Timber.tag(TAG).d("正在保存任务到数据库...")
                val taskId = repository.insertTask(task)
                Timber.tag(TAG).d("✅ 任务已保存，ID: $taskId")

                // 如果任务设置了通知策略和截止时间，注册闹钟
                if (task.notificationStrategyId != null && task.dueDate != null) {
                    Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    Timber.tag(TAG).d("检测到任务设置了通知策略，准备调度闹钟...")
                    Timber.tag(TAG).d("   任务ID: $taskId")
                    Timber.tag(TAG).d("   截止时间: ${task.dueDate!!.format(formatter)}")
                    Timber.tag(TAG).d("   通知策略ID: ${task.notificationStrategyId}")
                    Timber.tag(TAG).d("调用 TaskAlarmManager.scheduleTaskAlarm()")

                    taskAlarmManager.scheduleTaskAlarm(task)
                } else {
                    Timber.tag(TAG).d("任务未设置通知策略或截止时间，跳过闹钟调度")
                    Timber.tag(TAG).d("   notificationStrategyId: $notificationStrategyId")
                    Timber.tag(TAG).d("   dueDate: $finalDueDate")
                }

                Timber.tag(TAG).d("✅ CreateTaskUseCase 执行完成")
                Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Result.success(taskId)
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e("❌ 创建任务时发生异常")
            Timber.tag(TAG).e("   异常类型: ${e.javaClass.simpleName}")
            Timber.tag(TAG).e("   异常信息: ${e.message}")
            e.printStackTrace()
            Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Result.failure(e)
        }
    }
}

class UpdateTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val taskAlarmManager: com.example.nextthingb1.util.TaskAlarmManager
) {
    suspend operator fun invoke(task: Task): Result<Unit> {
        return try {
            if (task.title.isBlank()) {
                Result.failure(IllegalArgumentException("任务标题不能为空"))
            } else {
                val updatedTask = task.copy(
                    updatedAt = LocalDateTime.now(),
                    isUrgent = task.dueDate?.let { it.isBefore(LocalDateTime.now().plusHours(2)) } ?: false
                )
                repository.updateTask(updatedTask)

                // 取消旧闹钟并重新设置（如果有通知策略）
                taskAlarmManager.cancelTaskAlarm(updatedTask.id)
                if (updatedTask.notificationStrategyId != null && updatedTask.dueDate != null) {
                    taskAlarmManager.scheduleTaskAlarm(updatedTask)
                }

                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class DeleteTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val taskAlarmManager: com.example.nextthingb1.util.TaskAlarmManager
) {
    suspend operator fun invoke(taskId: String): Result<Unit> {
        return try {
            // 取消闹钟
            taskAlarmManager.cancelTaskAlarm(taskId)
            // 删除任务
            repository.deleteTask(taskId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class ToggleTaskStatusUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(taskId: String): Result<Unit> {
        return try {
            val task = repository.getTaskById(taskId)
                ?: return Result.failure(IllegalArgumentException("任务不存在"))

            // 严格的状态转换规则
            val newStatus = when (task.status) {
                TaskStatus.PENDING -> TaskStatus.COMPLETED
                TaskStatus.DELAYED -> TaskStatus.COMPLETED
                TaskStatus.OVERDUE -> TaskStatus.COMPLETED
                TaskStatus.CANCELLED -> {
                    // 放弃的任务可以恢复为未完成（需二次确认，由UI层处理）
                    TaskStatus.PENDING
                }
                TaskStatus.COMPLETED -> {
                    // 已完成的任务可以取消完成（仅支持7天内，此处暂不限制，由UI层控制）
                    TaskStatus.PENDING
                }
            }

            val updatedTask = task.copy(
                status = newStatus,
                completedAt = if (newStatus == TaskStatus.COMPLETED) LocalDateTime.now() else null,
                updatedAt = LocalDateTime.now()
            )

            repository.updateTask(updatedTask)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class DeferTaskUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(taskId: String): Result<Unit> {
        return try {
            val task = repository.getTaskById(taskId)
                ?: return Result.failure(IllegalArgumentException("任务不存在"))

            // 严格的延期规则：仅允许 PENDING 状态的任务延期
            if (task.status != TaskStatus.PENDING) {
                return Result.failure(IllegalStateException("仅未完成（当天）的任务可以延期"))
            }

            val now = LocalDateTime.now()
            val today = now.toLocalDate()

            // 检查是否在当天 23:59:59 之前
            if (now.toLocalDate().isAfter(today)) {
                return Result.failure(IllegalStateException("已过当天，无法延期"))
            }

            // 设置截止时间为次日的 23:59:59
            val tomorrowEnd = today.plusDays(1).atTime(23, 59, 59)

            val updatedTask = task.copy(
                status = TaskStatus.DELAYED,
                dueDate = tomorrowEnd,
                updatedAt = now
            )

            repository.updateTask(updatedTask)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class GetTaskStatisticsUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(): TaskStatistics {
        return repository.getTaskStatistics()
    }
}

class SearchTasksUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    operator fun invoke(query: String): Flow<List<Task>> {
        return repository.searchTasks(query)
    }
}

class GetTasksByCategoryUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    operator fun invoke(category: TaskCategory): Flow<List<Task>> {
        return repository.getTasksByCategory(category)
    }
}

class GetUrgentTasksUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    operator fun invoke(): Flow<List<Task>> {
        return repository.getUrgentTasks()
    }
}

class DeleteAllTasksUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return try {
            repository.deleteAllTasks()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class GetEarliestTaskDateUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(): LocalDate? {
        return repository.getEarliestTaskDate()
    }
} 