package com.example.nextthingb1.domain.model

import java.time.LocalDateTime
import java.util.UUID

/**
 * 分类领域模型
 */
data class Category(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: CategoryType,
    val icon: String,
    val colorHex: String,
    val sortOrder: Int = 0,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val isEnabled: Boolean = true
) {
    // 用于显示的名称，与name相同
    val displayName: String
        get() = name
}

/**
 * 分类类型枚举
 */
enum class CategoryType(val value: Int) {
    PRESET(0),      // 预置分类
    CUSTOM(1);      // 用户自定义分类

    companion object {
        fun fromInt(value: Int) = values().first { it.value == value }
    }
}

/**
 * 预置分类常量
 */
object PresetCategories {
    const val WORK_ID = "preset_work"
    const val LIFE_ID = "preset_life"

    fun getDefaultCategories(): List<Category> {
        return listOf(
            Category(
                id = WORK_ID,
                name = "工作",
                type = CategoryType.PRESET,
                icon = "laptop-code",
                colorHex = "#42A5F5",
                sortOrder = 0,
                createdAt = LocalDateTime.now()
            ),
            Category(
                id = LIFE_ID,
                name = "生活",
                type = CategoryType.PRESET,
                icon = "dumbbell",
                colorHex = "#66BB6A",
                sortOrder = 1,
                createdAt = LocalDateTime.now()
            )
        )
    }
}
