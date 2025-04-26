package com.wasbry.nextthing.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// 使用 @Entity 注解将该类标记为 Room 数据库的实体类
// tableName 参数指定表名为 "todoTask"
// foreignKeys 参数指定外键关联，这里表示 TodoTask 表的 categoryId 列关联到 Category 表的 id 列
// indices 参数指定需要创建的索引，这里为 categoryId 列创建索引
@Entity(
    tableName = "todoTask",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["categoryId"])]
)
data class TodoTask(
    // 使用 @PrimaryKey 注解将 id 列标记为主键
    // autoGenerate = true 表示 id 列的值将自动生成
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    // 待办任务的标题
    val title: String,
    // 待办任务的详细描述
    val description: String,
    // 待办任务的截止日期，以毫秒为单位存储
    val dueDate: Long,
    // 待办任务是否已完成的标志
    val isCompleted: Boolean,
    // 待办任务所属分类的 ID，外键关联到 Category 表的 id 列，设为可空类型
    val categoryId: Long? = null
)