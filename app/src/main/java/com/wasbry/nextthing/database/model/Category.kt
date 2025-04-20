package com.wasbry.nextthing.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// 使用 @Entity 注解将该类标记为 Room 数据库的实体类
// tableName 参数指定表名为 "category"
@Entity(tableName = "category")
data class Category(
    // 使用 @PrimaryKey 注解将 id 列标记为主键
    // autoGenerate = true 表示 id 列的值将自动生成
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    // 分类的名称
    val name: String
)