package com.example.nextthingb1.data.repository

import com.example.nextthingb1.data.local.dao.CategoryDao
import com.example.nextthingb1.data.mapper.CategoryMapper.toDomain
import com.example.nextthingb1.data.mapper.CategoryMapper.toDomainList
import com.example.nextthingb1.data.mapper.CategoryMapper.toEntity
import com.example.nextthingb1.domain.model.Category
import com.example.nextthingb1.domain.model.CategoryType
import com.example.nextthingb1.domain.model.PresetCategories
import com.example.nextthingb1.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories().map { entities ->
            entities.toDomainList()
        }
    }

    override suspend fun getCategoryById(categoryId: String): Category? {
        return try {
            categoryDao.getCategoryById(categoryId)?.toDomain()
        } catch (e: Exception) {
            Timber.e(e, "获取分类失败: $categoryId")
            null
        }
    }

    override fun getCategoriesByType(type: CategoryType): Flow<List<Category>> {
        return categoryDao.getCategoriesByType(type.value).map { entities ->
            entities.toDomainList()
        }
    }

    override suspend fun createCategory(
        name: String,
        icon: String,
        colorHex: String
    ): Result<Category> {
        return try {
            // 获取当前最大排序值
            val maxSortOrder = categoryDao.getMaxSortOrder() ?: -1

            val newCategory = Category(
                id = UUID.randomUUID().toString(),
                name = name,
                type = CategoryType.CUSTOM,
                icon = icon,
                colorHex = colorHex,
                sortOrder = maxSortOrder + 1
            )

            categoryDao.insertCategory(newCategory.toEntity())
            Timber.d("创建分类成功: $name")
            Result.success(newCategory)
        } catch (e: Exception) {
            Timber.e(e, "创建分类失败: $name")
            Result.failure(e)
        }
    }

    override suspend fun updateCategory(category: Category): Result<Unit> {
        return try {
            categoryDao.updateCategory(category.toEntity())
            Timber.d("更新分类成功: ${category.name}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "更新分类失败: ${category.name}")
            Result.failure(e)
        }
    }

    override suspend fun deleteCategory(categoryId: String): Result<Unit> {
        return try {
            val category = categoryDao.getCategoryById(categoryId)
            if (category == null) {
                return Result.failure(IllegalArgumentException("分类不存在"))
            }

            // 防止删除预置分类
            if (category.type == CategoryType.PRESET.value) {
                return Result.failure(IllegalStateException("不能删除预置分类"))
            }

            categoryDao.deleteCategory(category)
            Timber.d("删除分类成功: $categoryId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "删除分类失败: $categoryId")
            Result.failure(e)
        }
    }

    override suspend fun toggleCategoryEnabled(categoryId: String, isEnabled: Boolean): Result<Unit> {
        return try {
            categoryDao.toggleCategoryEnabled(categoryId, isEnabled)
            Timber.d("切换分类状态成功: $categoryId -> $isEnabled")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "切换分类状态失败: $categoryId")
            Result.failure(e)
        }
    }

    override suspend fun updateCategorySortOrder(categoryId: String, newSortOrder: Int): Result<Unit> {
        return try {
            val category = categoryDao.getCategoryById(categoryId)?.toDomain()
            if (category == null) {
                return Result.failure(IllegalArgumentException("分类不存在"))
            }

            val updatedCategory = category.copy(sortOrder = newSortOrder)
            categoryDao.updateCategory(updatedCategory.toEntity())
            Timber.d("更新分类排序成功: $categoryId -> $newSortOrder")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "更新分类排序失败: $categoryId")
            Result.failure(e)
        }
    }

    override suspend fun ensurePresetCategories(): Result<Unit> {
        return try {
            val presetCount = categoryDao.getPresetCategoryCount()
            if (presetCount == 0) {
                // 初始化预置分类
                val presetCategories = PresetCategories.getDefaultCategories()
                categoryDao.insertCategories(presetCategories.map { it.toEntity() })
                Timber.d("初始化预置分类成功: ${presetCategories.map { it.name }}")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "初始化预置分类失败")
            Result.failure(e)
        }
    }

    override suspend fun pinCategory(categoryId: String, isPinned: Boolean): Result<Unit> {
        return try {
            // Note: Category实体不包含isPinned字段，这个方法保留用于向后兼容
            // 实际的pin功能需要在CategoryEntity中添加isPinned字段才能实现
            Timber.d("Pin category: $categoryId -> $isPinned (功能待实现)")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Pin category失败: $categoryId")
            Result.failure(e)
        }
    }
}
