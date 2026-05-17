package com.nextthing.app.data.local.dao

import androidx.room.*
import com.nextthing.app.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

import com.nextthing.app.data.local.entity.SyncStatus

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories WHERE isEnabled = 1 ORDER BY sortOrder ASC, createdAt ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE isEnabled = 1 ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun getAllCategoriesList(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :categoryId")
    suspend fun getCategoryById(categoryId: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE type = :type AND isEnabled = 1 ORDER BY sortOrder ASC")
    fun getCategoriesByType(type: Int): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteCategoryById(categoryId: String)

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

    @Query("UPDATE categories SET syncStatus = :status, serverUpdatedAt = :serverTime WHERE id = :categoryId")
    suspend fun updateSyncStatus(categoryId: String, status: SyncStatus, serverTime: Long? = null)

    @Query("SELECT COUNT(*) FROM categories WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSyncCount(): Int
}
