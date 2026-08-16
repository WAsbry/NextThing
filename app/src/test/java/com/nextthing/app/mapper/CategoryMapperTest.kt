package com.nextthing.app.mapper

import com.nextthing.app.data.local.entity.CategoryEntity
import com.nextthing.app.data.mapper.CategoryMapper.toDomain
import com.nextthing.app.data.mapper.CategoryMapper.toEntity
import com.nextthing.app.data.mapper.CategoryMapper.toDomainList
import com.nextthing.app.data.mapper.CategoryMapper.toEntityList
import com.nextthing.app.data.mapper.CategoryMapper.toSyncDto
import com.nextthing.app.domain.model.Category
import com.nextthing.app.domain.model.CategoryType
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

class CategoryMapperTest {

    private val now = LocalDateTime.of(2025, 5, 15, 0, 0)

    private val testCategory = Category(
        id = "cat-1",
        name = "工作",
        type = CategoryType.PRESET,
        icon = "work",
        colorHex = "#42A5F5",
        sortOrder = 0,
        createdAt = now
    )

    private val testCategoryEntity = CategoryEntity(
        id = "cat-1",
        name = "工作",
        type = 0,
        icon = "work",
        colorHex = "#42A5F5",
        sortOrder = 0,
        createdAt = now
    )

    // ===== Entity -> Domain =====

    @Test
    fun `CategoryEntity toDomain maps all fields`() {
        val domain = testCategoryEntity.toDomain()

        assertEquals("cat-1", domain.id)
        assertEquals("工作", domain.name)
        assertEquals(CategoryType.PRESET, domain.type)
        assertEquals("work", domain.icon)
        assertEquals("#42A5F5", domain.colorHex)
        assertEquals(0, domain.sortOrder)
        assertEquals(now, domain.createdAt)
        assertTrue(domain.isEnabled)
    }

    @Test
    fun `CategoryEntity toDomain maps CUSTOM type correctly`() {
        val entity = testCategoryEntity.copy(type = 1)
        val domain = entity.toDomain()
        assertEquals(CategoryType.CUSTOM, domain.type)
    }

    // ===== Domain -> Entity =====

    @Test
    fun `Category toEntity maps all fields`() {
        val entity = testCategory.toEntity()

        assertEquals("cat-1", entity.id)
        assertEquals("工作", entity.name)
        assertEquals(0, entity.type)
        assertEquals("work", entity.icon)
        assertEquals("#42A5F5", entity.colorHex)
        assertEquals(0, entity.sortOrder)
        assertEquals(now, entity.createdAt)
    }

    @Test
    fun `Category toEntity maps CUSTOM type to int 1`() {
        val category = testCategory.copy(type = CategoryType.CUSTOM)
        val entity = category.toEntity()
        assertEquals(1, entity.type)
    }

    @Test
    fun `deleted CategoryEntity maps to a sync tombstone`() {
        val dto = testCategoryEntity.copy(deleted = true).toSyncDto()
        assertTrue(dto.deleted)
    }

    // ===== Round-trip =====

    @Test
    fun `round-trip Domain-Entity-Domain preserves all fields`() {
        val entity = testCategory.toEntity()
        val restored = entity.toDomain()

        assertEquals(testCategory.id, restored.id)
        assertEquals(testCategory.name, restored.name)
        assertEquals(testCategory.type, restored.type)
        assertEquals(testCategory.icon, restored.icon)
        assertEquals(testCategory.colorHex, restored.colorHex)
        assertEquals(testCategory.sortOrder, restored.sortOrder)
        assertEquals(testCategory.isEnabled, restored.isEnabled)
    }

    @Test
    fun `round-trip Entity-Domain-Entity preserves all fields`() {
        val domain = testCategoryEntity.toDomain()
        val restored = domain.toEntity()

        assertEquals(testCategoryEntity.id, restored.id)
        assertEquals(testCategoryEntity.name, restored.name)
        assertEquals(testCategoryEntity.type, restored.type)
        assertEquals(testCategoryEntity.icon, restored.icon)
        assertEquals(testCategoryEntity.colorHex, restored.colorHex)
        assertEquals(testCategoryEntity.sortOrder, restored.sortOrder)
    }

    // ===== Batch conversions =====

    @Test
    fun `toDomainList converts all items`() {
        val entities = listOf(
            testCategoryEntity.copy(id = "c1", name = "工作"),
            testCategoryEntity.copy(id = "c2", name = "生活"),
            testCategoryEntity.copy(id = "c3", name = "学习")
        )

        val domains = entities.toDomainList()

        assertEquals(3, domains.size)
        assertEquals("工作", domains[0].name)
        assertEquals("生活", domains[1].name)
        assertEquals("学习", domains[2].name)
    }

    @Test
    fun `toEntityList converts all items`() {
        val domains = listOf(
            testCategory.copy(id = "c1", name = "工作"),
            testCategory.copy(id = "c2", name = "生活")
        )

        val entities = domains.toEntityList()

        assertEquals(2, entities.size)
        assertEquals("工作", entities[0].name)
        assertEquals("生活", entities[1].name)
    }

    @Test
    fun `toDomainList handles empty list`() {
        val result = emptyList<CategoryEntity>().toDomainList()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `toEntityList handles empty list`() {
        val result = emptyList<Category>().toEntityList()
        assertTrue(result.isEmpty())
    }

    // ===== displayName =====

    @Test
    fun `Category displayName equals name`() {
        val category = Category(id = "c1", name = "自定义分类", type = CategoryType.CUSTOM, icon = "star", colorHex = "#FF5722")
        assertEquals("自定义分类", category.displayName)
    }
}
