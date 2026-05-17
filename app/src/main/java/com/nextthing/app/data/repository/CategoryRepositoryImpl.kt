package com.nextthing.app.data.repository

import com.nextthing.app.data.local.dao.CategoryDao
import com.nextthing.app.data.mapper.CategoryMapper.toDomain
import com.nextthing.app.data.mapper.CategoryMapper.toDomainList
import com.nextthing.app.data.mapper.CategoryMapper.toEntity
import com.nextthing.app.domain.model.Category
import com.nextthing.app.domain.model.CategoryType
import com.nextthing.app.domain.model.PresetCategories
import com.nextthing.app.data.local.entity.SyncStatus
import com.nextthing.app.domain.repository.CategoryRepository
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

            categoryDao.insertCategory(newCategory.toEntity().copy(syncStatus = SyncStatus.PENDING))
            Timber.d("创建分类成功: $name")
            Result.success(newCategory)
        } catch (e: Exception) {
            Timber.e(e, "创建分类失败: $name")
            Result.failure(e)
        }
    }

    override suspend fun updateCategory(category: Category): Result<Unit> {
        return try {
            categoryDao.updateCategory(category.toEntity().copy(syncStatus = SyncStatus.PENDING))
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

            if (category.type == CategoryType.PRESET.value) {
                return Result.failure(IllegalStateException("不能删除预置分类"))
            }

            categoryDao.updateSyncStatus(categoryId, SyncStatus.PENDING, null)
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
            categoryDao.updateSyncStatus(categoryId, SyncStatus.PENDING, null)
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
            categoryDao.updateCategory(updatedCategory.toEntity().copy(syncStatus = SyncStatus.PENDING))
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

            // 迁移旧 icon 数据：把非 drawable:/content:/file: 格式的 icon 映射为新格式
            migrateOldIcons()

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "初始化预置分类失败")
            Result.failure(e)
        }
    }

    /**
     * 旧 icon 数据迁移：将旧格式 icon 值映射为 "drawable:xxx"
     */
    private suspend fun migrateOldIcons() {
        val legacyIconMap = mapOf(
            "laptop-code" to "drawable:work",
            "dumbbell" to "drawable:life",
            "book" to "drawable:study",
            "heart" to "drawable:sports",
            "user" to "drawable:social",
            "circle" to "drawable:miscellaneous"
        )

        try {
            val allCategories = categoryDao.getAllCategoriesList()
            for (entity in allCategories) {
                val icon = entity.icon
                // 跳过已经是新格式的
                if (icon.startsWith("drawable:") || icon.startsWith("content://") || icon.startsWith("file://")) {
                    continue
                }
                // 颜色值（如 "#9E9E9E"）→ 按分类名称猜测图标
                val newIcon = legacyIconMap[icon]
                    ?: guessIconByName(entity.name)
                    ?: "drawable:miscellaneous"
                categoryDao.updateCategoryIcon(entity.id, newIcon)
                Timber.d("🏷️ 迁移旧图标: ${entity.name} '$icon' → '$newIcon'")
            }
        } catch (e: Exception) {
            Timber.e(e, "🏷️ 迁移旧图标失败")
        }
    }

    /**
     * 根据分类名称猜测对应的预置图标
     */
    private fun guessIconByName(name: String): String? {
        return when {
            name.contains("股票") || name.contains("股市") -> "drawable:stock"
            name.contains("工作") || name.contains("办公") -> "drawable:work"
            name.contains("生活") -> "drawable:life"
            name.contains("学习") || name.contains("读书") || name.contains("教育") -> "drawable:study"
            name.contains("运动") || name.contains("健身") || name.contains("健康") -> "drawable:sports"
            name.contains("娱乐") || name.contains("游戏") -> "drawable:entertainment"
            name.contains("财务") || name.contains("理财") || name.contains("股") || name.contains("金融") -> "drawable:finance"
            name.contains("社交") || name.contains("朋友") -> "drawable:social"
            name.contains("出行") || name.contains("旅行") || name.contains("旅游") -> "drawable:travel"
            name.contains("家庭") || name.contains("家务") -> "drawable:family"
            else -> null
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
