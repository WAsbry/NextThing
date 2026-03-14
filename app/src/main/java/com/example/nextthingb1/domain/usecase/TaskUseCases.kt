package com.example.nextthingb1.domain.usecase

import com.example.nextthingb1.domain.model.Task
import com.example.nextthingb1.domain.model.Category
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
    val getTaskById: GetTaskByIdUseCase,
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
    val generateRecurringTasks: GenerateRecurringTasksUseCase,
    val deleteCompletedTasks: DeleteCompletedTasksUseCase,
    val locationRepository: LocationRepository
)

class GetAllTasksUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    operator fun invoke(): Flow<List<Task>> {
        return repository.getAllTasks()
    }
}

class GetTaskByIdUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(taskId: String): Task? {
        return repository.getTaskById(taskId)
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
        category: Category,
        dueDate: LocalDateTime? = null,
        tags: List<String> = emptyList(),
        imageUri: String? = null,
        repeatFrequency: RepeatFrequency = RepeatFrequency(),
        notificationStrategyId: String? = null,
        importanceUrgency: TaskImportanceUrgency? = null,
        locationInfo: com.example.nextthingb1.domain.model.LocationInfo? = null
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
                // 判断是否为重复任务(需要创建为模板)
                val isRecurringTask = repeatFrequency.type != com.example.nextthingb1.domain.model.RepeatFrequencyType.NONE

                Timber.tag(TAG).d("准备创建Task对象:")
                Timber.tag(TAG).d("   标题: $title")
                Timber.tag(TAG).d("   描述: $description")
                Timber.tag(TAG).d("   分类: ${category.displayName}")
                Timber.tag(TAG).d("   截止时间: ${finalDueDate.format(formatter)}")
                Timber.tag(TAG).d("   通知策略ID: $notificationStrategyId")
                Timber.tag(TAG).d("   重复频率: ${repeatFrequency.type}")
                Timber.tag(TAG).d("   是否重复任务: $isRecurringTask")

                val task = Task(
                    title = title.trim(),
                    description = description.trim(),
                    category = category,
                    dueDate = finalDueDate,
                    tags = tags,
                    isUrgent = dueDate != null && finalDueDate.isBefore(LocalDateTime.now().plusHours(2)),
                    imageUri = imageUri,
                    repeatFrequency = repeatFrequency,
                    locationInfo = locationInfo,
                    notificationStrategyId = notificationStrategyId,
                    importanceUrgency = importanceUrgency,
                    isTemplate = isRecurringTask, // 重复任务创建为模板
                    templateTaskId = null,
                    instanceDate = null
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
    /**
     * 删除任务
     * @param taskId 要删除的任务ID
     * @param deleteMode 删除模式（仅对重复任务有效）
     */
    suspend operator fun invoke(
        taskId: String,
        deleteMode: com.example.nextthingb1.domain.model.DeleteMode =
            com.example.nextthingb1.domain.model.DeleteMode.DELETE_THIS_ONLY
    ): Result<Unit> {
        return try {
            // 获取任务信息
            val task = repository.getTaskById(taskId)

            if (task == null) {
                return Result.failure(Exception("任务不存在"))
            }

            // 取消闹钟
            taskAlarmManager.cancelTaskAlarm(taskId)

            // 判断是否为重复任务的实例
            val isRecurringInstance = task.templateTaskId != null

            when {
                // 情况1：普通任务（非重复任务）
                !isRecurringInstance -> {
                    timber.log.Timber.d("删除普通任务: ${task.title}")
                    repository.deleteTask(taskId)
                }

                // 情况2：重复任务实例 - 仅删除此任务
                isRecurringInstance && deleteMode == com.example.nextthingb1.domain.model.DeleteMode.DELETE_THIS_ONLY -> {
                    timber.log.Timber.d("仅删除重复任务实例: ${task.title}")
                    repository.deleteTask(taskId)
                }

                // 情况3：重复任务实例 - 删除所有重复任务
                isRecurringInstance && deleteMode == com.example.nextthingb1.domain.model.DeleteMode.DELETE_ALL_RECURRING -> {
                    timber.log.Timber.d("删除所有重复任务: ${task.title}")
                    val templateId = task.templateTaskId!!

                    // 获取所有相关实例，取消它们的闹钟
                    val instances = repository.getInstancesByTemplateId(templateId)
                    instances.forEach { instance ->
                        taskAlarmManager.cancelTaskAlarm(instance.id)
                    }

                    // 删除template和所有instances
                    repository.deleteTemplateAndAllInstances(templateId)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            timber.log.Timber.e(e, "删除任务失败")
            Result.failure(e)
        }
    }
}

class ToggleTaskStatusUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(taskId: String): Result<Unit> {
        return try {
            timber.log.Timber.tag("UseCase").d("━━━━━━ ToggleTaskStatus 开始 ━━━━━━")
            timber.log.Timber.tag("UseCase").d("taskId: ${taskId.take(8)}")

            val task = repository.getTaskById(taskId)
                ?: return Result.failure(IllegalArgumentException("任务不存在"))

            timber.log.Timber.tag("UseCase").d("任务标题: ${task.title}")
            timber.log.Timber.tag("UseCase").d("当前状态: ${task.status}")

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

            timber.log.Timber.tag("UseCase").d("新状态: $newStatus")

            val updatedTask = task.copy(
                status = newStatus,
                completedAt = if (newStatus == TaskStatus.COMPLETED) LocalDateTime.now() else null,
                updatedAt = LocalDateTime.now()
            )

            timber.log.Timber.tag("UseCase").d("updatedTask.status: ${updatedTask.status}")
            timber.log.Timber.tag("UseCase").d("准备调用 repository.updateTask()")

            repository.updateTask(updatedTask)

            timber.log.Timber.tag("UseCase").d("✅ repository.updateTask() 完成")
            timber.log.Timber.tag("UseCase").d("━━━━━━ ToggleTaskStatus 结束 ━━━━━━")
            Result.success(Unit)
        } catch (e: Exception) {
            timber.log.Timber.tag("UseCase").e(e, "❌ ToggleTaskStatus 异常")
            Result.failure(e)
        }
    }
}

class DeferTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val taskAlarmManager: com.example.nextthingb1.util.TaskAlarmManager
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

            // 检查是否已过当天 23:59（理论上 PENDING 任务不应有 dueDate 在过去，但做防御性检查）
            if (task.dueDate != null && task.dueDate.toLocalDate().isBefore(today)) {
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

            // 取消旧闹钟，并为新 dueDate 重新调度
            taskAlarmManager.cancelTaskAlarm(taskId)
            if (updatedTask.notificationStrategyId != null) {
                taskAlarmManager.scheduleTaskAlarm(updatedTask)
            }

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
    operator fun invoke(category: Category): Flow<List<Task>> {
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

class DeleteCompletedTasksUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke() {
        repository.deleteCompletedTasks()
    }
}

class GetEarliestTaskDateUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(): LocalDate? {
        return repository.getEarliestTaskDate()
    }
}

/**
 * 生成重复任务实例UseCase
 *
 * 根据模板任务的重复频率,为指定日期生成任务实例
 */
class GenerateRecurringTasksUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val taskAlarmManager: com.example.nextthingb1.util.TaskAlarmManager
) {
    companion object {
        private const val TAG = "RecurringTask"
    }

    /**
     * 为指定日期生成所有需要的重复任务实例
     *
     * @param targetDate 目标日期
     * @return 生成的任务数量
     */
    suspend operator fun invoke(targetDate: LocalDate = LocalDate.now()): Result<Int> {
        return try {
            Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Timber.tag(TAG).d("开始生成重复任务实例")
            Timber.tag(TAG).d("目标日期: $targetDate")

            // 1. 获取所有模板任务
            val templates = repository.getTemplateTasks()
            Timber.tag(TAG).d("找到 ${templates.size} 个模板任务")

            var generatedCount = 0

            // 2. 遍历每个模板任务
            templates.forEach { template ->
                Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Timber.tag(TAG).d("处理模板任务: ${template.title}")
                Timber.tag(TAG).d("  模板ID: ${template.id}")
                Timber.tag(TAG).d("  重复类型: ${template.repeatFrequency.type}")

                // 3. 检查是否需要为这个日期生成任务
                if (shouldGenerateForDate(template, targetDate)) {
                    Timber.tag(TAG).d("  ✅ 需要为 $targetDate 生成任务")

                    // 4. 检查是否已经存在实例
                    val instanceDate = targetDate.atStartOfDay()
                    if (!repository.hasInstanceForDate(template.id, instanceDate)) {
                        Timber.tag(TAG).d("  ✅ 该日期尚未生成实例,开始生成...")

                        // 5. 从模板创建实例任务
                        val instance = createInstanceFromTemplate(template, targetDate)
                        repository.insertTask(instance)

                        // 为新实例调度闹钟（同 CreateTaskUseCase 的逻辑）
                        if (instance.notificationStrategyId != null && instance.dueDate != null) {
                            taskAlarmManager.scheduleTaskAlarm(instance)
                        }

                        generatedCount++
                        Timber.tag(TAG).d("  ✅ 实例任务创建成功")
                        Timber.tag(TAG).d("    实例ID: ${instance.id}")
                        Timber.tag(TAG).d("    实例日期: ${instance.instanceDate}")
                    } else {
                        Timber.tag(TAG).d("  ⏭️ 该日期已存在实例,跳过")
                    }
                } else {
                    Timber.tag(TAG).d("  ⏭️ 不需要为 $targetDate 生成任务")
                }
            }

            Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Timber.tag(TAG).d("✅ 重复任务生成完成")
            Timber.tag(TAG).d("  生成数量: $generatedCount")
            Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            Result.success(generatedCount)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ 生成重复任务失败")
            Result.failure(e)
        }
    }

    /**
     * 判断是否应该为指定日期生成任务
     */
    private fun shouldGenerateForDate(template: Task, date: LocalDate): Boolean {
        val repeatFreq = template.repeatFrequency

        return when (repeatFreq.type) {
            com.example.nextthingb1.domain.model.RepeatFrequencyType.NONE -> {
                // 单次任务,不生成重复实例
                false
            }
            com.example.nextthingb1.domain.model.RepeatFrequencyType.DAILY -> {
                // 每日任务,总是生成
                true
            }
            com.example.nextthingb1.domain.model.RepeatFrequencyType.WEEKLY -> {
                // 每周任务,检查星期几
                val dayOfWeek = date.dayOfWeek.value // 1=周一, 7=周日
                repeatFreq.weekdays.contains(dayOfWeek)
            }
            com.example.nextthingb1.domain.model.RepeatFrequencyType.MONTHLY -> {
                // 每月任务,检查日期
                val dayOfMonth = date.dayOfMonth
                val lengthOfMonth = date.lengthOfMonth() // 当月天数

                // 直接匹配
                if (repeatFreq.monthDays.contains(dayOfMonth)) {
                    return true
                }

                // 处理月末日期: 如果用户选择的日期大于当月天数,在当月最后一天生成
                // 例如: 用户选择31日,2月只有28/29天,则在2月最后一天生成
                val isLastDayOfMonth = dayOfMonth == lengthOfMonth
                if (isLastDayOfMonth) {
                    // 检查是否有任何选择的日期大于当月天数
                    val hasLargerDay = repeatFreq.monthDays.any { it > lengthOfMonth }
                    return hasLargerDay
                }

                false
            }
        }
    }

    /**
     * 从模板创建任务实例
     */
    private fun createInstanceFromTemplate(template: Task, date: LocalDate): Task {
        // 计算实例的截止时间
        val instanceDueDate = if (template.dueDate != null) {
            // 如果模板有截止时间,使用相同的时分秒
            date.atTime(template.dueDate.toLocalTime())
        } else {
            // 否则默认为当天23:59
            date.atTime(23, 59, 59)
        }

        return template.copy(
            id = java.util.UUID.randomUUID().toString(), // 新ID
            isTemplate = false, // 不是模板
            templateTaskId = template.id, // 指向模板
            instanceDate = date.atStartOfDay(), // 实例日期
            dueDate = instanceDueDate, // 实例的截止时间
            status = TaskStatus.PENDING, // 初始状态为待办
            // 重新计算 isUrgent（基于实例实际截止时间，而非模板创建时的状态）
            isUrgent = instanceDueDate.isBefore(LocalDateTime.now().plusHours(2)),
            completedAt = null, // 未完成
            createdAt = LocalDateTime.now(), // 创建时间
            updatedAt = LocalDateTime.now() // 更新时间
        )
    }
} 