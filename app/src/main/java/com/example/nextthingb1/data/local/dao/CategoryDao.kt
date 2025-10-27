package com.example.nextthingb1.data.local.dao

import androidx.room.*
import com.example.nextthingb1.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

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

    @Query("UPDATE categories SET isEnabled = :isEnabled WHERE id = :categoryId")
    suspend fun toggleCategoryEnabled(categoryId: String, isEnabled: Boolean)

    @Query("SELECT MAX(sortOrder) FROM categories")
    suspend fun getMaxSortOrder(): Int?

    @Query("SELECT COUNT(*) FROM categories WHERE type = 0")
    suspend fun getPresetCategoryCount(): Int
}
