package com.nextthing.app.presentation.screens.stats

import com.nextthing.app.domain.model.Category
import com.nextthing.app.domain.model.CategoryType
import com.nextthing.app.domain.model.Task
import com.nextthing.app.domain.model.TaskImportanceUrgency
import com.nextthing.app.domain.model.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class StatsCalculationsTest {

    private val now = LocalDateTime.of(2026, 8, 1, 12, 0)
    private val category = Category(
        id = "cat-1",
        name = "工作",
        type = CategoryType.PRESET,
        icon = "work",
        colorHex = "#42A5F5"
    )

    @Test
    fun `today statistics keep status counts quadrants and overdue rule consistent`() {
        val tasks = listOf(
            task("future", TaskStatus.PENDING, TaskImportanceUrgency.IMPORTANT_URGENT, now.plusHours(1)),
            task("expired", TaskStatus.PENDING, TaskImportanceUrgency.IMPORTANT_NOT_URGENT, now.minusMinutes(6)),
            task("completed", TaskStatus.COMPLETED, TaskImportanceUrgency.NOT_IMPORTANT_URGENT, now.minusHours(1)),
            task("delayed", TaskStatus.DELAYED, TaskImportanceUrgency.NOT_IMPORTANT_NOT_URGENT, now.plusDays(1)),
            task("cancelled", TaskStatus.CANCELLED, null, now.minusHours(2)),
            task("explicit-overdue", TaskStatus.OVERDUE, null, null)
        )

        val stats = calculateOverviewBasicStats(tasks, OverviewTimeRange.TODAY, now)

        assertEquals(6, stats.total)
        assertEquals(1, stats.pending)
        assertEquals(1, stats.completed)
        assertEquals(1, stats.deferred)
        assertEquals(2, stats.overdue)
        assertEquals(1, stats.cancelled)
        assertEquals(100f / 6f, stats.completionRate, 0.01f)
        assertEquals(1, stats.importantUrgent)
        assertEquals(1, stats.importantNotUrgent)
        assertEquals(1, stats.notImportantUrgent)
        assertEquals(1, stats.notImportantNotUrgent)
        assertEquals(4, stats.corePending)
        assertEquals(2, stats.coreOverdue)
        assertEquals("1", stats.coreProgress)
        assertEquals("count", stats.coreProgressType)
    }

    @Test
    fun `future task and five minute grace are not overdue`() {
        assertFalse(isStatsTaskOverdue(task("future", dueDate = now.plusMinutes(1)), now))
        assertFalse(isStatsTaskOverdue(task("grace", dueDate = now.minusMinutes(5)), now))
        assertTrue(isStatsTaskOverdue(task("expired", dueDate = now.minusMinutes(5).minusSeconds(1)), now))
    }

    @Test
    fun `overdue range excludes future tasks and dates outside selection`() {
        val start = now.toLocalDate()
        val end = start

        assertFalse(isStatsTaskOverdueInRange(task("future", dueDate = now.plusHours(1)), start, end, now))
        assertTrue(isStatsTaskOverdueInRange(task("expired", dueDate = now.minusMinutes(6)), start, end, now))
        assertFalse(isStatsTaskOverdueInRange(task("yesterday", dueDate = now.minusDays(1)), start, end, now))
    }

    @Test
    fun `completed and cancelled tasks never count as overdue`() {
        assertFalse(isStatsTaskOverdue(task("done", TaskStatus.COMPLETED, dueDate = now.minusDays(1)), now))
        assertFalse(isStatsTaskOverdue(task("cancelled", TaskStatus.CANCELLED, dueDate = now.minusDays(1)), now))
    }

    @Test
    fun `today range excludes historical tasks from counts and quadrants`() {
        val todayTask = task("today", TaskStatus.PENDING, TaskImportanceUrgency.IMPORTANT_URGENT, now.plusHours(1))
        val historicalTask = task(
            "history",
            TaskStatus.COMPLETED,
            TaskImportanceUrgency.IMPORTANT_URGENT,
            now.minusDays(1),
            createdAt = now.minusDays(1)
        )

        val stats = calculateOverviewBasicStats(listOf(todayTask, historicalTask), OverviewTimeRange.TODAY, now)

        assertEquals(1, stats.total)
        assertEquals(1, stats.importantUrgent)
        assertEquals(0, stats.importantUrgentCompleted)
        assertEquals(0f, stats.completionRate, 0.01f)
    }

    @Test
    fun `percentage is bounded between zero and one hundred`() {
        assertEquals(0f, safePercentage(1, 0), 0.01f)
        assertEquals(0f, safePercentage(-1, 10), 0.01f)
        assertEquals(70f, safePercentage(7, 10), 0.01f)
        assertEquals(100f, safePercentage(12, 10), 0.01f)
    }

    @Test
    fun `percentage converts to bounded ratio for progress UI`() {
        assertEquals(0f, percentageToRatio(-20f), 0.001f)
        assertEquals(0.5f, percentageToRatio(50f), 0.001f)
        assertEquals(1f, percentageToRatio(100f), 0.001f)
        assertEquals(1f, percentageToRatio(5000f), 0.001f)
    }

    private fun task(
        id: String,
        status: TaskStatus = TaskStatus.PENDING,
        quadrant: TaskImportanceUrgency? = null,
        dueDate: LocalDateTime? = null,
        createdAt: LocalDateTime = now.minusHours(1)
    ) = Task(
        id = id,
        title = id,
        category = category,
        status = status,
        createdAt = createdAt,
        updatedAt = createdAt,
        dueDate = dueDate,
        importanceUrgency = quadrant
    )
}
