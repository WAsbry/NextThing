package com.nextthing.app.presentation.screens.stats

import com.nextthing.app.domain.model.Task
import com.nextthing.app.domain.model.TaskImportanceUrgency
import com.nextthing.app.domain.model.TaskStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

private const val OVERDUE_GRACE_MINUTES = 5L

internal data class OverviewBasicStats(
    val total: Int,
    val pending: Int,
    val completed: Int,
    val deferred: Int,
    val overdue: Int,
    val cancelled: Int,
    val completionRate: Float,
    val importantUrgent: Int,
    val importantNotUrgent: Int,
    val notImportantUrgent: Int,
    val notImportantNotUrgent: Int,
    val importantUrgentCompleted: Int,
    val corePending: Int,
    val coreImportantUrgent: Int,
    val coreOverdue: Int,
    val coreProgress: String,
    val coreProgressType: String
)

internal fun calculateOverviewBasicStats(
    tasks: List<Task>,
    timeRange: OverviewTimeRange,
    now: LocalDateTime = LocalDateTime.now()
): OverviewBasicStats {
    val today = now.toLocalDate()
    val (rangeStart, rangeEnd) = overviewDateRange(tasks, timeRange, today)
    val createdInRange = tasks.filter { it.createdAt.toLocalDate() in rangeStart..rangeEnd }

    val completed = createdInRange.count { it.status == TaskStatus.COMPLETED }
    val deferred = createdInRange.count { it.status == TaskStatus.DELAYED }
    val cancelled = createdInRange.count { it.status == TaskStatus.CANCELLED }
    val overdue = createdInRange.count { isStatsTaskOverdue(it, now) }
    // A stale PENDING row whose grace period has expired is shown as overdue, not twice.
    val pending = createdInRange.count {
        it.status == TaskStatus.PENDING && !isStatsTaskOverdue(it, now)
    }

    val importantUrgent = createdInRange.count {
        it.importanceUrgency == TaskImportanceUrgency.IMPORTANT_URGENT
    }
    val importantNotUrgent = createdInRange.count {
        it.importanceUrgency == TaskImportanceUrgency.IMPORTANT_NOT_URGENT
    }
    val notImportantUrgent = createdInRange.count {
        it.importanceUrgency == TaskImportanceUrgency.NOT_IMPORTANT_URGENT
    }
    val notImportantNotUrgent = createdInRange.count {
        it.importanceUrgency == TaskImportanceUrgency.NOT_IMPORTANT_NOT_URGENT
    }
    val importantUrgentCompleted = createdInRange.count {
        it.importanceUrgency == TaskImportanceUrgency.IMPORTANT_URGENT &&
            it.status == TaskStatus.COMPLETED
    }

    val corePending = createdInRange.count {
        it.status != TaskStatus.COMPLETED && it.status != TaskStatus.CANCELLED
    }
    val coreImportantUrgent = createdInRange.count {
        it.importanceUrgency == TaskImportanceUrgency.IMPORTANT_URGENT &&
            it.status != TaskStatus.COMPLETED &&
            it.status != TaskStatus.CANCELLED
    }
    val coreOverdue = tasks.count { isStatsTaskOverdueInRange(it, rangeStart, rangeEnd, now) }

    val completionRate = safePercentage(completed, createdInRange.size)
    val (progress, progressType) = if (timeRange == OverviewTimeRange.TODAY) {
        completed.toString() to "count"
    } else {
        "${completionRate.toInt()}%" to "rate"
    }

    return OverviewBasicStats(
        total = createdInRange.size,
        pending = pending,
        completed = completed,
        deferred = deferred,
        overdue = overdue,
        cancelled = cancelled,
        completionRate = completionRate,
        importantUrgent = importantUrgent,
        importantNotUrgent = importantNotUrgent,
        notImportantUrgent = notImportantUrgent,
        notImportantNotUrgent = notImportantNotUrgent,
        importantUrgentCompleted = importantUrgentCompleted,
        corePending = corePending,
        coreImportantUrgent = coreImportantUrgent,
        coreOverdue = coreOverdue,
        coreProgress = progress,
        coreProgressType = progressType
    )
}

internal fun isStatsTaskOverdue(task: Task, now: LocalDateTime): Boolean {
    if (task.status == TaskStatus.COMPLETED || task.status == TaskStatus.CANCELLED) return false
    if (task.status == TaskStatus.OVERDUE) return true
    val dueDate = task.dueDate ?: return false
    return task.status == TaskStatus.PENDING && now.isAfter(dueDate.plusMinutes(OVERDUE_GRACE_MINUTES))
}

internal fun isStatsTaskOverdueInRange(
    task: Task,
    rangeStart: LocalDate,
    rangeEnd: LocalDate,
    now: LocalDateTime
): Boolean {
    val belongsToRange = task.dueDate?.toLocalDate()?.let { it in rangeStart..rangeEnd }
        ?: (task.status == TaskStatus.OVERDUE && task.createdAt.toLocalDate() in rangeStart..rangeEnd)
    return belongsToRange && isStatsTaskOverdue(task, now)
}

internal fun safePercentage(numerator: Int, denominator: Int): Float {
    if (denominator <= 0) return 0f
    return ((numerator.coerceAtLeast(0).toFloat() / denominator) * 100f).coerceIn(0f, 100f)
}

internal fun percentageToRatio(percentage: Float): Float =
    (percentage / 100f).coerceIn(0f, 1f)

private fun overviewDateRange(
    tasks: List<Task>,
    timeRange: OverviewTimeRange,
    today: LocalDate
): Pair<LocalDate, LocalDate> = when (timeRange) {
    OverviewTimeRange.TODAY -> today to today
    OverviewTimeRange.THIS_WEEK -> today.with(DayOfWeek.MONDAY) to today.with(DayOfWeek.SUNDAY)
    OverviewTimeRange.THIS_MONTH -> today.withDayOfMonth(1) to today.withDayOfMonth(today.lengthOfMonth())
    OverviewTimeRange.ALL -> {
        val earliest = tasks.minOfOrNull { it.createdAt.toLocalDate() } ?: today
        earliest to today
    }
}
