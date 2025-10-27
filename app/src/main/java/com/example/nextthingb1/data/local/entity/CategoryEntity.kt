package com.example.nextthingb1.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: Int, // 0 = 预置分类, 1 = 用户自定义分类
    val icon: String, // 图标标识
    val colorHex: String, // 颜色值（如 #42A5F5）
    val sortOrder: Int, // 排序顺序，数字越小越靠前
    val createdAt: LocalDateTime,
    val isEnabled: Boolean = true // 是否启用（支持隐藏分类）
)
