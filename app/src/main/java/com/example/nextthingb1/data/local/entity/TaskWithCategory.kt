package com.example.nextthingb1.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Task 与 Category 的联表查询结果
 */
data class TaskWithCategory(
    @Embedded val task: TaskEntity,
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: CategoryEntity
)
