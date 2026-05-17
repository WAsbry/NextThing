package com.nextthing.app.data.mapper

import com.nextthing.app.data.local.entity.CategoryEntity
import com.nextthing.app.domain.model.Category
import com.nextthing.app.domain.model.CategoryType

import com.nextthing.app.data.local.entity.SyncStatus
import com.nextthing.app.data.remote.dto.CategorySyncDto
import java.time.Instant
import java.time.ZoneId

/**
 * CategoryEntity 与 Category 领域模型之间的映射器
 */
object CategoryMapper {

    /**
     * 将 CategoryEntity 转换为 Category 领域模型
     */
    fun CategoryEntity.toDomain(): Category {
        return Category(
            id = id,
            name = name,
            type = CategoryType.fromInt(type),
            icon = icon,
            colorHex = colorHex,
            sortOrder = sortOrder,
            createdAt = createdAt,
            isEnabled = isEnabled
        )
    }

    /**
     * 将 Category 领域模型转换为 CategoryEntity
     */
    fun Category.toEntity(): CategoryEntity {
        return CategoryEntity(
            id = id,
            name = name,
            type = type.value,
            icon = icon,
            colorHex = colorHex,
            sortOrder = sortOrder,
            createdAt = createdAt,
            isEnabled = isEnabled
        )
    }

    /**
     * 批量转换 CategoryEntity 列表为 Category 列表
     */
    fun List<CategoryEntity>.toDomainList(): List<Category> {
        return this.map { it.toDomain() }
    }

    /**
     * 批量转换 Category 列表为 CategoryEntity 列表
     */
    fun List<Category>.toEntityList(): List<CategoryEntity> {
        return this.map { it.toEntity() }
    }

    /**
     * 将 Category 转换为同步DTO
     */
    fun Category.toSyncDto(): CategorySyncDto {
        return CategorySyncDto(
            id = id,
            name = name,
            type = type.value,
            icon = icon,
            colorHex = colorHex,
            sortOrder = sortOrder,
            createdAt = createdAt.toTimestamp(),
            isEnabled = isEnabled,
            deleted = false
        )
    }

    /**
     * 将 CategoryEntity 转换为同步DTO
     */
    fun CategoryEntity.toSyncDto(): CategorySyncDto {
        return CategorySyncDto(
            id = id,
            name = name,
            type = type,
            icon = icon,
            colorHex = colorHex,
            sortOrder = sortOrder,
            createdAt = createdAt.toTimestamp(),
            isEnabled = isEnabled,
            deleted = false
        )
    }

    /**
     * 将 CategorySyncDto 转换为 CategoryEntity
     */
    fun CategorySyncDto.toEntity(syncStatus: SyncStatus = SyncStatus.SYNCED): CategoryEntity {
        return CategoryEntity(
            id = id,
            name = name,
            type = type,
            icon = icon,
            colorHex = colorHex,
            sortOrder = sortOrder,
            createdAt = createdAt.toLocalDateTime(),
            isEnabled = isEnabled,
            syncStatus = syncStatus,
            serverUpdatedAt = createdAt
        )
    }

    // ========== 辅助方法 ==========

    private fun java.time.LocalDateTime.toTimestamp(): Long {
        return atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun Long.toLocalDateTime(): java.time.LocalDateTime {
        return java.time.LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())
    }
}
