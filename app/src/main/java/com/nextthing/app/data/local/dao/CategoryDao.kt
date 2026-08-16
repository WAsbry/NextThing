package com.nextthing.app.data.local.dao

import androidx.room.*
import com.nextthing.app.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

import com.nextthing.app.data.local.entity.SyncStatus

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories WHERE isEnabled = 1 AND deleted = 0 ORDER BY sortOrder ASC, createdAt ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE isEnabled = 1 AND deleted = 0 ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun getAllCategoriesList(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :categoryId AND deleted = 0")
    suspend fun getCategoryById(categoryId: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE type = :type AND isEnabled = 1 AND deleted = 0 ORDER BY sortOrder ASC")
    fun getCategoriesByType(type: Int): Flow<List<CategoryEntity>>

    // REPLACE deletes the existing parent row before inserting it again, which
    // violates the task -> category foreign key whenever a task already uses it.
    @Upsert
    suspend fun insertCategory(category: CategoryEntity)

    @Upsert
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteCategoryById(categoryId: String)

    @Query("""
        UPDATE categories
        SET deleted = 1, syncStatus = 'PENDING', serverUpdatedAt = NULL
        WHERE id = :categoryId
    """)
    suspend fun softDeleteCategory(categoryId: String)

    @Query("UPDATE categories SET isEnabled = :isEnabled WHERE id = :categoryId")
    suspend fun toggleCategoryEnabled(categoryId: String, isEnabled: Boolean)

    @Query("SELECT MAX(sortOrder) FROM categories")
    suspend fun getMaxSortOrder(): Int?

    @Query("SELECT COUNT(*) FROM categories WHERE type = 0")
    suspend fun getPresetCategoryCount(): Int

    @Query("UPDATE categories SET icon = :newIcon WHERE id = :categoryId")
    suspend fun updateCategoryIcon(categoryId: String, newIcon: String)

    // ========== 同步相关查询 ==========

    @Query("SELECT * FROM categories WHERE syncStatus = :status")
    suspend fun getCategoriesBySyncStatus(status: SyncStatus): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSyncCategories(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id IN (:categoryIds)")
    suspend fun getCategoriesByIdsIncludingDeleted(categoryIds: Set<String>): List<CategoryEntity>

    @Query("UPDATE categories SET syncStatus = :status, serverUpdatedAt = :serverTime WHERE id = :categoryId")
    suspend fun updateSyncStatus(categoryId: String, status: SyncStatus, serverTime: Long? = null)

    @Query("SELECT COUNT(*) FROM categories WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSyncCount(): Int
}
