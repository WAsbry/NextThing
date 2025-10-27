package com.example.nextthingb1.data.mapper

import com.example.nextthingb1.data.local.entity.CategoryEntity
import com.example.nextthingb1.domain.model.Category
import com.example.nextthingb1.domain.model.CategoryType

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
}
