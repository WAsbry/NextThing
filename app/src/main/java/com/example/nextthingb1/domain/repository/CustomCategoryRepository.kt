package com.example.nextthingb1.domain.repository

import com.example.nextthingb1.domain.model.CategoryItem
import kotlinx.coroutines.flow.Flow

interface CustomCategoryRepository {
    suspend fun getAllCategories(): Flow<List<CategoryItem>>
    suspend fun createCategory(name: String, colorHex: String = "#9E9E9E"): Result<CategoryItem>
    suspend fun deleteCategory(categoryId: String): Result<Unit>
    suspend fun pinCategory(categoryId: String, isPinned: Boolean): Result<Unit>
    suspend fun initializeSystemCategories(): Result<Unit>
}