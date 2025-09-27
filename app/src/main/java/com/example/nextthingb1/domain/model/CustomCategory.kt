package com.example.nextthingb1.domain.model

data class CustomCategory(
    val id: String,
    val name: String,
    val colorHex: String = "#9E9E9E",
    val icon: String = "circle",
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val order: Int = 0
)

// 扩展的分类项，统一管理系统分类和自定义分类
data class CategoryItem(
    val id: String,
    val displayName: String,
    val colorHex: String = "#9E9E9E",
    val icon: String = "circle",
    val isPinned: Boolean = false,
    val order: Int = 0,
    val isSystemDefault: Boolean = false // 标记是否为系统默认分类，用于初始化
) {
    companion object {
        // 从系统TaskCategory创建CategoryItem
        fun fromTaskCategory(category: TaskCategory, isPinned: Boolean = false, order: Int = 0): CategoryItem {
            return CategoryItem(
                id = category.name,
                displayName = category.displayName,
                colorHex = category.colorHex,
                icon = category.icon,
                isPinned = isPinned,
                order = order,
                isSystemDefault = true
            )
        }

        // 从CustomCategory创建CategoryItem
        fun fromCustomCategory(customCategory: CustomCategory): CategoryItem {
            return CategoryItem(
                id = customCategory.id,
                displayName = customCategory.name,
                colorHex = customCategory.colorHex,
                icon = customCategory.icon,
                isPinned = customCategory.isPinned,
                order = customCategory.order,
                isSystemDefault = false
            )
        }
    }
}