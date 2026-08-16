package com.nextthing.app.presentation.screens.today

import com.nextthing.app.domain.model.Category
import com.nextthing.app.domain.model.CategoryType
import com.nextthing.app.domain.model.Task
import com.nextthing.app.domain.model.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class TodayTaskDecisionTest {

    private val now = LocalDateTime.of(2026, 8, 1, 12, 0)
    private val category = Category(
        id = "category-work",
        name = "工作",
        type = CategoryType.CUSTOM,
        icon = "work",
        colorHex = "#42A5F5"
    )

    @Test
    fun `nearest upcoming pending task today becomes NextThing`() {
        val tasks = listOf(
            task("later", dueDate = now.plusHours(3)),
            task("next", dueDate = now.plusMinutes(30)),
            task("middle", dueDate = now.plusHours(1))
        )

        assertEquals("next", selectNextThingTask(tasks, now)?.id)
    }

    @Test
    fun `same due time uses creation time then id for stable selection`() {
        val dueDate = now.plusHours(1)
        val tasks = listOf(
            task("b", dueDate, createdAt = now.minusHours(1)),
            task("a", dueDate, createdAt = now.minusHours(1)),
            task("older", dueDate, createdAt = now.minusHours(2))
        )

        assertEquals("older", selectNextThingTask(tasks, now)?.id)
        assertEquals("a", selectNextThingTask(tasks.dropLast(1), now)?.id)
    }

    @Test
    fun `future day task cannot enter Today NextThing`() {
        val futureTask = task("tomorrow", now.plusDays(1).withHour(9))

        assertNull(selectNextThingTask(listOf(futureTask), now))
        assertFalse(isTodayTaskOverdue(futureTask, now.plusDays(2)))
    }

    @Test
    fun `past task is not selected as NextThing`() {
        val overdue = task("past", now.minusMinutes(10))

        assertNull(selectNextThingTask(listOf(overdue), now))
    }

    @Test
    fun `completed cancelled delayed and untimed tasks are excluded`() {
        val tasks = listOf(
            task("completed", now.plusHours(1), TaskStatus.COMPLETED),
            task("cancelled", now.plusHours(1), TaskStatus.CANCELLED),
            task("delayed", now.plusHours(1), TaskStatus.DELAYED),
            task("untimed", null, TaskStatus.PENDING)
        )

        assertNull(selectNextThingTask(tasks, now))
    }

    @Test
    fun `pending task gets full five minute overdue grace period`() {
        val task = task("grace", now.minusMinutes(5))

        assertFalse(isTodayTaskOverdue(task, now))
        assertTrue(isTodayTaskOverdue(task, now.plusSeconds(1)))
    }

    @Test
    fun `explicit overdue status remains overdue`() {
        val task = task("overdue", null, TaskStatus.OVERDUE)

        assertTrue(isTodayTaskOverdue(task, now))
    }

    @Test
    fun `future task is never classified as overdue`() {
        val futureTask = task("future", now.plusHours(2))

        assertFalse(isTodayTaskOverdue(futureTask, now))
    }

    private fun task(
        id: String,
        dueDate: LocalDateTime?,
        status: TaskStatus = TaskStatus.PENDING,
        createdAt: LocalDateTime = now.minusMinutes(10)
    ) = Task(
        id = id,
        title = id,
        category = category,
        status = status,
        createdAt = createdAt,
        updatedAt = createdAt,
        dueDate = dueDate
    )
}
