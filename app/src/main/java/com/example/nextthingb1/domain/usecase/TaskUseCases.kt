package com.example.nextthingb1.domain.usecase

import com.example.nextthingb1.domain.model.Task
import com.example.nextthingb1.domain.model.TaskCategory
import com.example.nextthingb1.domain.model.TaskPriority
import com.example.nextthingb1.domain.model.TaskStatistics
import com.example.nextthingb1.domain.model.TaskStatus
import com.example.nextthingb1.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

data class TaskUseCases @Inject constructor(
    val getAllTasks: GetAllTasksUseCase,
    val getTodayTasks: GetTodayTasksUseCase,
    val createTask: CreateTaskUseCase,
    val updateTask: UpdateTaskUseCase,
    val deleteTask: DeleteTaskUseCase,
    val deleteAllTasks: DeleteAllTasksUseCase,
    val toggleTaskStatus: ToggleTaskStatusUseCase,
    val getTaskStatistics: GetTaskStatisticsUseCase,
    val searchTasks: SearchTasksUseCase,
    val getTasksByCategory: GetTasksByCategoryUseCase,
    val getUrgentTasks: GetUrgentTasksUseCase,
    val getEarliestTaskDate: GetEarliestTaskDateUseCase
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

class CreateTaskUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(
        title: String,
        description: String = "",
        priority: TaskPriority = TaskPriority.MEDIUM,
        category: TaskCategory = TaskCategory.WORK,
        dueDate: LocalDateTime? = null,
        tags: List<String> = emptyList(),
        imageUri: String? = null
    ): Result<String> {
        return try {
            if (title.isBlank()) {
                Result.failure(IllegalArgumentException("任务标题不能为空"))
            } else {
                val task = Task(
                    title = title.trim(),
                    description = description.trim(),
                    priority = priority,
                    category = category,
                    dueDate = dueDate,
                    tags = tags,
                    isUrgent = dueDate?.let { it.isBefore(LocalDateTime.now().plusHours(2)) } ?: false,
                    imageUri = imageUri
                )
                val taskId = repository.insertTask(task)
                Result.success(taskId)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class UpdateTaskUseCase @Inject constructor(
    private val repository: TaskRepository
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
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class DeleteTaskUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(taskId: String): Result<Unit> {
        return try {
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
            
            val newStatus = when (task.status) {
                TaskStatus.PENDING -> TaskStatus.COMPLETED
                TaskStatus.IN_PROGRESS -> TaskStatus.COMPLETED
                TaskStatus.COMPLETED -> TaskStatus.PENDING
                TaskStatus.CANCELLED -> TaskStatus.PENDING
                TaskStatus.OVERDUE -> TaskStatus.COMPLETED
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