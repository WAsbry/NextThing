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

// 定义任务状态的枚举类
enum class TaskStatus {
    // 未完成状态
    INCOMPLETE,
    // 已完成状态
    COMPLETED,
    // 放弃状态
    ABANDONED,
    // 延期状态
    POSTPONED
}

// 使用 @Entity 注解将该类标记为 Room 数据库的实体类
// tableName 参数指定表名为 "todoTask"
// foreignKeys 参数指定外键关联，这里表示 TodoTask 表的 categoryId 列关联到 Category 表的 id 列
// indices 参数指定需要创建的索引，这里为 categoryId 列创建索引
@Entity(tableName = "TodoTaskTable")
data class TodoTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    // 新增关联 PersonalTime 的外键
    val personalTimeId: Long? = null, // 任务的分类噻
    val description: String, // 任务的描述噻
    val duration: Int, // 任务的持续时间，min
    val importance: TaskImportance, // 任务的重要程度
    val madeDate: Date, // 任务的制定日期
    val status: TaskStatus // 任务的状态
)