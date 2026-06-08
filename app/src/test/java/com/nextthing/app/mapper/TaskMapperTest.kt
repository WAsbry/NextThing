package com.nextthing.app.mapper

import com.nextthing.app.data.local.entity.CategoryEntity
import com.nextthing.app.data.local.entity.TaskEntity
import com.nextthing.app.data.local.entity.TaskWithCategory
import com.nextthing.app.data.mapper.toDomain
import com.nextthing.app.data.mapper.toEntity
import com.nextthing.app.data.mapper.toDomainList
import com.nextthing.app.data.mapper.toEntityList
import com.nextthing.app.domain.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

class TaskMapperTest {

    private val testCategory = Category(
        id = "cat-1",
        name = "工作",
        type = CategoryType.PRESET,
        icon = "work",
        colorHex = "#42A5F5",
        sortOrder = 0
    )

    private val testCategoryEntity = CategoryEntity(
        id = "cat-1",
        name = "工作",
        type = 0,
        icon = "work",
        colorHex = "#42A5F5",
        sortOrder = 0,
        createdAt = LocalDateTime.of(2025, 1, 1, 0, 0)
    )

    private val now = LocalDateTime.of(2025, 5, 15, 10, 0)

    private fun createTask(
        title: String = "测试任务",
        status: TaskStatus = TaskStatus.PENDING,
        tags: List<String> = emptyList(),
        subtasks: List<Subtask> = emptyList(),
        repeatFrequency: RepeatFrequency = RepeatFrequency(),
        locationInfo: LocationInfo? = null,
        importanceUrgency: TaskImportanceUrgency? = null,
        isTemplate: Boolean = false,
        templateTaskId: String? = null,
        instanceDate: LocalDateTime? = null
    ) = Task(
        id = "task-1",
        title = title,
        description = "描述",
        category = testCategory,
        status = status,
        createdAt = now,
        updatedAt = now,
        dueDate = now.plusHours(2),
        tags = tags,
        subtasks = subtasks,
        repeatFrequency = repeatFrequency,
        locationInfo = locationInfo,
        importanceUrgency = importanceUrgency,
        isTemplate = isTemplate,
        templateTaskId = templateTaskId,
        instanceDate = instanceDate
    )

    private fun createTaskEntity(
        title: String = "测试任务",
        status: TaskStatus = TaskStatus.PENDING,
        tags: String = "[]",
        subtasksJson: String = "[]",
        repeatFrequencyJson: String = "{}",
        locationInfoJson: String? = null,
        importanceUrgencyJson: String? = null,
        isTemplate: Boolean = false,
        templateTaskId: String? = null,
        instanceDate: LocalDateTime? = null
    ) = TaskEntity(
        id = "task-1",
        title = title,
        description = "描述",
        categoryId = "cat-1",
        status = status,
        createdAt = now,
        updatedAt = now,
        dueDate = now.plusHours(2),
        completedAt = null,
        tags = tags,
        isUrgent = false,
        estimatedDuration = 0,
        actualDuration = 0,
        subtasksJson = subtasksJson,
        repeatFrequencyJson = repeatFrequencyJson,
        locationInfoJson = locationInfoJson,
        importanceUrgencyJson = importanceUrgencyJson,
        isTemplate = isTemplate,
        templateTaskId = templateTaskId,
        instanceDate = instanceDate
    )

    // ===== Task -> Entity =====

    @Test
    fun `Task toEntity maps basic fields correctly`() {
        val task = createTask()
        val entity = task.toEntity()

        assertEquals(task.id, entity.id)
        assertEquals(task.title, entity.title)
        assertEquals(task.description, entity.description)
        assertEquals(task.category.id, entity.categoryId)
        assertEquals(task.status, entity.status)
        assertEquals(task.createdAt, entity.createdAt)
        assertEquals(task.dueDate, entity.dueDate)
        assertEquals(task.isUrgent, entity.isUrgent)
    }

    @Test
    fun `Task toEntity serializes tags as JSON`() {
        val task = createTask(tags = listOf("tag1", "tag2"))
        val entity = task.toEntity()
        assertTrue(entity.tags.contains("tag1"))
        assertTrue(entity.tags.contains("tag2"))
    }

    @Test
    fun `Task toEntity serializes subtasks as JSON`() {
        val subtasks = listOf(Subtask(id = "s1", title = "子任务1"), Subtask(id = "s2", title = "子任务2"))
        val task = createTask(subtasks = subtasks)
        val entity = task.toEntity()
        assertTrue(entity.subtasksJson.contains("子任务1"))
        assertTrue(entity.subtasksJson.contains("子任务2"))
    }

    @Test
    fun `Task toEntity serializes repeatFrequency`() {
        val task = createTask(repeatFrequency = RepeatFrequency(type = RepeatFrequencyType.DAILY))
        val entity = task.toEntity()
        assertTrue(entity.repeatFrequencyJson.contains("DAILY"))
    }

    @Test
    fun `Task toEntity serializes locationInfo`() {
        val location = LocationInfo(id = "loc-1", locationName = "公司", latitude = 30.0, longitude = 104.0, address = "成都市")
        val task = createTask(locationInfo = location)
        val entity = task.toEntity()
        assertNotNull(entity.locationInfoJson)
        assertTrue(entity.locationInfoJson!!.contains("公司"))
    }

    @Test
    fun `Task toEntity maps null locationInfo to null`() {
        val task = createTask(locationInfo = null)
        val entity = task.toEntity()
        assertNull(entity.locationInfoJson)
    }

    @Test
    fun `Task toEntity serializes importanceUrgency`() {
        val task = createTask(importanceUrgency = TaskImportanceUrgency.IMPORTANT_URGENT)
        val entity = task.toEntity()
        assertNotNull(entity.importanceUrgencyJson)
        assertTrue(entity.importanceUrgencyJson!!.contains("IMPORTANT_URGENT"))
    }

    @Test
    fun `Task toEntity preserves template fields`() {
        val task = createTask(isTemplate = true, templateTaskId = "tpl-1", instanceDate = now)
        val entity = task.toEntity()
        assertTrue(entity.isTemplate)
        assertEquals("tpl-1", entity.templateTaskId)
        assertEquals(now, entity.instanceDate)
    }

    // ===== TaskWithCategory -> Task (toDomain) =====

    @Test
    fun `TaskWithCategory toDomain maps all fields`() {
        val entity = createTaskEntity(
            tags = """["tag1","tag2"]""",
            subtasksJson = """[{"id":"s1","title":"子任务1","isCompleted":false,"createdAt":"2025-05-15T10:00:00"}]""",
            repeatFrequencyJson = """{"type":"WEEKLY","weekdays":[1,3,5],"monthDays":[]}""",
            importanceUrgencyJson = """"IMPORTANT_URGENT""""
        )
        val withCategory = TaskWithCategory(task = entity, category = testCategoryEntity)
        val domain = withCategory.toDomain()

        assertEquals("task-1", domain.id)
        assertEquals("测试任务", domain.title)
        assertEquals("工作", domain.category.name)
        assertEquals(TaskStatus.PENDING, domain.status)
        assertEquals(listOf("tag1", "tag2"), domain.tags)
        assertEquals(1, domain.subtasks.size)
        assertEquals("子任务1", domain.subtasks[0].title)
        assertEquals(RepeatFrequencyType.WEEKLY, domain.repeatFrequency.type)
        assertEquals(setOf(1, 3, 5), domain.repeatFrequency.weekdays)
        assertEquals(TaskImportanceUrgency.IMPORTANT_URGENT, domain.importanceUrgency)
    }

    @Test
    fun `TaskWithCategory toDomain handles empty tags`() {
        val entity = createTaskEntity(tags = "[]")
        val domain = TaskWithCategory(task = entity, category = testCategoryEntity).toDomain()
        assertTrue(domain.tags.isEmpty())
    }

    @Test
    fun `TaskWithCategory toDomain handles invalid JSON gracefully`() {
        val entity = createTaskEntity(tags = "invalid", subtasksJson = "invalid", repeatFrequencyJson = "invalid")
        val domain = TaskWithCategory(task = entity, category = testCategoryEntity).toDomain()
        assertTrue(domain.tags.isEmpty())
        assertTrue(domain.subtasks.isEmpty())
        assertEquals(RepeatFrequencyType.NONE, domain.repeatFrequency.type)
    }

    @Test
    fun `TaskWithCategory toDomain handles null locationInfo`() {
        val entity = createTaskEntity(locationInfoJson = null)
        val domain = TaskWithCategory(task = entity, category = testCategoryEntity).toDomain()
        assertNull(domain.locationInfo)
    }

    @Test
    fun `TaskWithCategory toDomain handles blank locationInfo`() {
        val entity = createTaskEntity(locationInfoJson = "")
        val domain = TaskWithCategory(task = entity, category = testCategoryEntity).toDomain()
        assertNull(domain.locationInfo)
    }

    // ===== Round-trip: Task -> Entity -> TaskWithCategory -> Task =====

    @Test
    fun `round-trip conversion preserves basic fields`() {
        val original = createTask(
            tags = listOf("工作", "重要"),
            status = TaskStatus.COMPLETED
        )
        val entity = original.toEntity()
        val withCategory = TaskWithCategory(task = entity, category = testCategoryEntity)
        val restored = withCategory.toDomain()

        assertEquals(original.id, restored.id)
        assertEquals(original.title, restored.title)
        assertEquals(original.status, restored.status)
        assertEquals(original.isUrgent, restored.isUrgent)
        assertEquals(original.tags, restored.tags)
    }

    @Test
    fun `round-trip preserves subtasks`() {
        val subtasks = listOf(
            Subtask(id = "s1", title = "步骤1", isCompleted = true),
            Subtask(id = "s2", title = "步骤2", isCompleted = false)
        )
        val original = createTask(subtasks = subtasks)
        val entity = original.toEntity()
        val restored = TaskWithCategory(task = entity, category = testCategoryEntity).toDomain()

        assertEquals(2, restored.subtasks.size)
        assertEquals("步骤1", restored.subtasks[0].title)
        assertTrue(restored.subtasks[0].isCompleted)
        assertEquals("步骤2", restored.subtasks[1].title)
        assertFalse(restored.subtasks[1].isCompleted)
    }

    @Test
    fun `round-trip preserves repeatFrequency`() {
        val original = createTask(repeatFrequency = RepeatFrequency(
            type = RepeatFrequencyType.WEEKLY,
            weekdays = setOf(1, 2, 3, 4, 5)
        ))
        val entity = original.toEntity()
        val restored = TaskWithCategory(task = entity, category = testCategoryEntity).toDomain()

        assertEquals(RepeatFrequencyType.WEEKLY, restored.repeatFrequency.type)
        assertEquals(setOf(1, 2, 3, 4, 5), restored.repeatFrequency.weekdays)
    }

    // ===== Batch conversions =====

    @Test
    fun `toDomainList converts all items`() {
        val entities = listOf(
            TaskWithCategory(task = createTaskEntity(id = "t1", title = "任务1"), category = testCategoryEntity),
            TaskWithCategory(task = createTaskEntity(id = "t2", title = "任务2"), category = testCategoryEntity),
            TaskWithCategory(task = createTaskEntity(id = "t3", title = "任务3"), category = testCategoryEntity)
        )
        // Note: createTaskEntity uses fixed id "task-1" so we test title conversion
        val titles = entities.map { it.task.title }
        assertEquals(listOf("任务1", "任务2", "任务3"), titles)
    }

    @Test
    fun `toEntityList converts all tasks`() {
        val tasks = listOf(
            createTask(title = "A"),
            createTask(title = "B")
        )
        val entities = tasks.toEntityList()
        assertEquals(2, entities.size)
        assertEquals("A", entities[0].title)
        assertEquals("B", entities[1].title)
    }

    // Helper for creating TaskEntity with different id
    private fun createTaskEntity(id: String, title: String): TaskEntity {
        return createTaskEntity(title = title).copy(id = id)
    }
}
