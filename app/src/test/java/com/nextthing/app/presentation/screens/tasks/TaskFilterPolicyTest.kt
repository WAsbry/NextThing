package com.nextthing.app.presentation.screens.tasks

import com.nextthing.app.domain.model.Category
import com.nextthing.app.domain.model.CategoryType
import com.nextthing.app.domain.model.Task
import com.nextthing.app.domain.model.TaskImportanceUrgency
import com.nextthing.app.domain.model.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskFilterPolicyTest {

    private val life = Category("life", "生活", CategoryType.PRESET, "home", "#66BB6A")
    private val work = Category("work", "工作", CategoryType.PRESET, "work", "#42A5F5")

    private val tasks = listOf(
        task("pending", TaskStatus.PENDING, life),
        task("delayed", TaskStatus.DELAYED, life),
        task("completed", TaskStatus.COMPLETED, work),
        task("overdue", TaskStatus.OVERDUE, life),
        task("cancelled", TaskStatus.CANCELLED, work),
        task("important", TaskStatus.PENDING, work, TaskImportanceUrgency.IMPORTANT_URGENT)
    )

    @Test
    fun eachStatusFilter_returnsOnlyItsExpectedStatuses() {
        assertIds(StatusFilter.ALL, "pending", "delayed", "completed", "overdue", "cancelled", "important")
        assertIds(StatusFilter.PENDING, "pending", "delayed", "important")
        assertIds(StatusFilter.COMPLETED, "completed")
        assertIds(StatusFilter.OVERDUE, "overdue")
        assertIds(StatusFilter.CANCELLED, "cancelled")
    }

    @Test
    fun filters_areAppliedAsAnIntersection() {
        val result = TaskFilterPolicy.apply(
            tasks = tasks,
            statusFilter = StatusFilter.PENDING,
            categoryId = work.id,
            priorityFilter = PriorityFilter.IMPORTANT_URGENT
        )

        assertEquals(listOf("important"), result.map(Task::id))
    }

    @Test
    fun aiSearchResultState_distinguishesEmptyAndActiveResults() {
        assertEquals(AISearchState.EMPTY, aiSearchStateForResultCount(0))
        assertEquals(AISearchState.ACTIVE, aiSearchStateForResultCount(1))
        assertEquals(AISearchState.ACTIVE, aiSearchStateForResultCount(5))
    }

    private fun assertIds(filter: StatusFilter, vararg expected: String) {
        val result = TaskFilterPolicy.apply(tasks, filter, null, PriorityFilter.ALL)
        assertEquals(expected.toList(), result.map(Task::id))
    }

    private fun task(
        id: String,
        status: TaskStatus,
        category: Category,
        priority: TaskImportanceUrgency? = null
    ) = Task(
        id = id,
        title = id,
        category = category,
        status = status,
        importanceUrgency = priority
    )
}
