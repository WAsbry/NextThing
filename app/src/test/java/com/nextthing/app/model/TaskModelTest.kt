package com.nextthing.app.model

import com.nextthing.app.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class TaskModelTest {

    // ===== TaskStatus =====

    @Test
    fun `Task values have correct defaults`() {
        val task = Task(title = "测试", category = Category(id = "c1", name = "工作", type = CategoryType.PRESET, icon = "work", colorHex = "#42A5F5"))
        assertEquals(TaskStatus.PENDING, task.status)
        assertTrue(task.id.isNotEmpty())
        assertTrue(task.tags.isEmpty())
        assertTrue(task.subtasks.isEmpty())
        assertEquals(RepeatFrequencyType.NONE, task.repeatFrequency.type)
        assertNull(task.locationInfo)
        assertNull(task.importanceUrgency)
        assertFalse(task.isTemplate)
        assertNull(task.templateTaskId)
        assertNull(task.instanceDate)
    }

    // ===== TaskImportanceUrgency =====

    @Test
    fun `fromImportanceAndUrgency returns correct quadrant`() {
        assertEquals(
            TaskImportanceUrgency.IMPORTANT_URGENT,
            TaskImportanceUrgency.fromImportanceAndUrgency(TaskImportance.IMPORTANT, TaskUrgency.URGENT)
        )
        assertEquals(
            TaskImportanceUrgency.IMPORTANT_NOT_URGENT,
            TaskImportanceUrgency.fromImportanceAndUrgency(TaskImportance.IMPORTANT, TaskUrgency.NOT_URGENT)
        )
        assertEquals(
            TaskImportanceUrgency.NOT_IMPORTANT_URGENT,
            TaskImportanceUrgency.fromImportanceAndUrgency(TaskImportance.NOT_IMPORTANT, TaskUrgency.URGENT)
        )
        assertEquals(
            TaskImportanceUrgency.NOT_IMPORTANT_NOT_URGENT,
            TaskImportanceUrgency.fromImportanceAndUrgency(TaskImportance.NOT_IMPORTANT, TaskUrgency.NOT_URGENT)
        )
    }

    @Test
    fun `each quadrant has correct importance and urgency decomposition`() {
        TaskImportanceUrgency.values().forEach { quadrant ->
            val reconstructed = TaskImportanceUrgency.fromImportanceAndUrgency(quadrant.importance, quadrant.urgency)
            assertEquals(quadrant, reconstructed)
        }
    }

    // ===== CategoryType =====

    @Test
    fun `CategoryType fromInt maps correctly`() {
        assertEquals(CategoryType.PRESET, CategoryType.fromInt(0))
        assertEquals(CategoryType.CUSTOM, CategoryType.fromInt(1))
    }

    // ===== PresetCategories =====

    @Test
    fun `default categories contain work and life`() {
        val defaults = PresetCategories.getDefaultCategories()
        assertEquals(2, defaults.size)
        assertEquals("工作", defaults[0].name)
        assertEquals("生活", defaults[1].name)
        assertEquals(PresetCategories.WORK_ID, defaults[0].id)
        assertEquals(PresetCategories.LIFE_ID, defaults[1].id)
    }

    // ===== Subtask =====

    @Test
    fun `Subtask has correct defaults`() {
        val subtask = Subtask(title = "步骤1")
        assertFalse(subtask.isCompleted)
        assertTrue(subtask.id.isNotEmpty())
    }

    @Test
    fun `Subtask copy with isCompleted works`() {
        val original = Subtask(id = "s1", title = "步骤1", isCompleted = false)
        val completed = original.copy(isCompleted = true)
        assertTrue(completed.isCompleted)
        assertEquals("s1", completed.id)
        assertEquals("步骤1", completed.title)
    }

    // ===== TaskStatus =====

    @Test
    fun `TaskStatus has all expected values`() {
        val statuses = TaskStatus.values()
        assertEquals(5, statuses.size)
        assertTrue(statuses.contains(TaskStatus.PENDING))
        assertTrue(statuses.contains(TaskStatus.COMPLETED))
        assertTrue(statuses.contains(TaskStatus.DELAYED))
        assertTrue(statuses.contains(TaskStatus.OVERDUE))
        assertTrue(statuses.contains(TaskStatus.CANCELLED))
    }

    // ===== DeleteMode =====

    @Test
    fun `DeleteMode has two options`() {
        val modes = DeleteMode.values()
        assertEquals(2, modes.size)
        assertTrue(modes.contains(DeleteMode.DELETE_THIS_ONLY))
        assertTrue(modes.contains(DeleteMode.DELETE_ALL_RECURRING))
    }

    // ===== Task data class copy =====

    @Test
    fun `Task copy preserves id and changes status`() {
        val task = Task(title = "测试", category = Category(id = "c1", name = "工作", type = CategoryType.PRESET, icon = "work", colorHex = "#42A5F5"))
        val updated = task.copy(status = TaskStatus.COMPLETED)
        assertEquals(task.id, updated.id)
        assertEquals(TaskStatus.COMPLETED, updated.status)
        assertEquals("测试", updated.title)
    }
}
