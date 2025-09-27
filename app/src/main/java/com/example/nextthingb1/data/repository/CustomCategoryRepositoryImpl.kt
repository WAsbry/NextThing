package com.example.nextthingb1.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.nextthingb1.domain.model.CategoryItem
import com.example.nextthingb1.domain.model.TaskCategory
import com.example.nextthingb1.domain.repository.CustomCategoryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.customCategoryDataStore: DataStore<Preferences> by preferencesDataStore(name = "categories")

@Serializable
data class CategoryData(
    val id: String,
    val displayName: String,
    val colorHex: String,
    val icon: String,
    val isPinned: Boolean = false,
    val order: Int = 0,
    val isSystemDefault: Boolean = false
)

@Singleton
class CustomCategoryRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : CustomCategoryRepository {

    private val categoriesKey = stringSetPreferencesKey("categories")
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getAllCategories(): Flow<List<CategoryItem>> {
        return context.customCategoryDataStore.data.map { preferences ->
            val categoriesJsonSet = preferences[categoriesKey] ?: emptySet()
            val categories = categoriesJsonSet.mapNotNull { categoryJson ->
                try {
                    val categoryData = json.decodeFromString<CategoryData>(categoryJson)
                    CategoryItem(
                        id = categoryData.id,
                        displayName = categoryData.displayName,
                        colorHex = categoryData.colorHex,
                        icon = categoryData.icon,
                        isPinned = categoryData.isPinned,
                        order = categoryData.order,
                        isSystemDefault = categoryData.isSystemDefault
                    )
                } catch (e: Exception) {
                    null
                }
            }

            // 如果没有分类，初始化系统分类
            if (categories.isEmpty()) {
                val systemCategories = TaskCategory.values().mapIndexed { index, category ->
                    CategoryItem.fromTaskCategory(category, order = index)
                }
                systemCategories
            } else {
                categories.sortedWith(compareByDescending<CategoryItem> { it.isPinned }.thenBy { it.order })
            }
        }
    }

    override suspend fun initializeSystemCategories(): Result<Unit> {
        return try {
            val currentCategories = context.customCategoryDataStore.data.first()[categoriesKey] ?: emptySet()
            if (currentCategories.isEmpty()) {
                context.customCategoryDataStore.edit { preferences ->
                    val systemCategories = TaskCategory.values().mapIndexed { index, category ->
                        val categoryItem = CategoryItem.fromTaskCategory(category, order = index)
                        val categoryData = CategoryData(
                            id = categoryItem.id,
                            displayName = categoryItem.displayName,
                            colorHex = categoryItem.colorHex,
                            icon = categoryItem.icon,
                            isPinned = categoryItem.isPinned,
                            order = categoryItem.order,
                            isSystemDefault = categoryItem.isSystemDefault
                        )
                        json.encodeToString(categoryData)
                    }.toSet()
                    preferences[categoriesKey] = systemCategories
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createCategory(name: String, colorHex: String): Result<CategoryItem> {
        return try {
            val newCategory = CategoryItem(
                id = UUID.randomUUID().toString(),
                displayName = name,
                colorHex = colorHex,
                order = System.currentTimeMillis().toInt(),
                isSystemDefault = false
            )

            context.customCategoryDataStore.edit { preferences ->
                val currentCategories = preferences[categoriesKey] ?: emptySet()
                val categoryData = CategoryData(
                    id = newCategory.id,
                    displayName = newCategory.displayName,
                    colorHex = newCategory.colorHex,
                    icon = newCategory.icon,
                    isPinned = newCategory.isPinned,
                    order = newCategory.order,
                    isSystemDefault = newCategory.isSystemDefault
                )
                val newCategoryJson = json.encodeToString(categoryData)
                preferences[categoriesKey] = currentCategories + newCategoryJson
            }

            Result.success(newCategory)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteCategory(categoryId: String): Result<Unit> {
        return try {
            context.customCategoryDataStore.edit { preferences ->
                val currentCategories = preferences[categoriesKey] ?: emptySet()
                val filteredCategories = currentCategories.filter { categoryJson ->
                    try {
                        val categoryData = json.decodeFromString<CategoryData>(categoryJson)
                        categoryData.id != categoryId
                    } catch (e: Exception) {
                        false
                    }
                }.toSet()
                preferences[categoriesKey] = filteredCategories
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun pinCategory(categoryId: String, isPinned: Boolean): Result<Unit> {
        return try {
            context.customCategoryDataStore.edit { preferences ->
                val currentCategories = preferences[categoriesKey] ?: emptySet()
                val updatedCategories = currentCategories.map { categoryJson ->
                    try {
                        val categoryData = json.decodeFromString<CategoryData>(categoryJson)
                        if (categoryData.id == categoryId) {
                            json.encodeToString(categoryData.copy(isPinned = isPinned))
                        } else {
                            categoryJson
                        }
                    } catch (e: Exception) {
                        categoryJson
                    }
                }.toSet()
                preferences[categoriesKey] = updatedCategories
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}