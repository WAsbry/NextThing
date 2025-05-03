package com.wasbry.nextthing.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

// 定义任务重要程度的枚举类
enum class TaskImportance {
    UNIMPORTANT_NOT_URGENT, // 不重要不紧急
    UNIMPORTANT_BUT_URGENT, // 不重要但紧急
    IMPORTANT_NOT_URGENT,   // 重要但不紧急
    IMPORTANT_AND_URGENT    // 重要且紧急
}

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
    val title: String, // 待办任务的标题
    val description: String, // 待办任务的详细描述
    val madeDate: Date, // 任务的制定日期,以年月日进行存储
    val dueDate: Long,
    // 待办任务是否已完成的标志
    val isCompleted: Boolean,
    // 待办任务所属分类的 ID，外键关联到 Category 表的 id 列，设为可空类型
    val categoryId: Long? = null,
    // 新增任务重要程度属性
    val importance: TaskImportance
)