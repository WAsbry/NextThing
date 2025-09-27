package com.example.nextthingb1.data.service

import android.content.Context
import android.content.SharedPreferences
import com.example.nextthingb1.domain.model.CategoryItem
import com.example.nextthingb1.domain.model.TaskCategory
import com.example.nextthingb1.domain.service.CategoryPreferencesManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryPreferencesManagerImpl @Inject constructor(
    private val context: Context
) : CategoryPreferencesManager {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFERENCE_NAME, Context.MODE_PRIVATE
    )

    override suspend fun getLastSelectedCategoryId(): String {
        return sharedPreferences.getString(KEY_LAST_SELECTED_CATEGORY, TaskCategory.LIFE.name)
            ?: TaskCategory.LIFE.name
    }

    override suspend fun saveSelectedCategoryId(categoryId: String) {
        sharedPreferences.edit()
            .putString(KEY_LAST_SELECTED_CATEGORY, categoryId)
            .apply()
    }

    override suspend fun recordCategoryUsage(categoryId: String) {
        // 记录使用次数
        val currentCount = sharedPreferences.getInt("${KEY_USAGE_COUNT_PREFIX}$categoryId", 0)
        sharedPreferences.edit()
            .putInt("${KEY_USAGE_COUNT_PREFIX}$categoryId", currentCount + 1)
            .apply()

        // 记录最后使用时间
        val currentTime = System.currentTimeMillis()
        sharedPreferences.edit()
            .putLong("${KEY_LAST_USED_TIME_PREFIX}$categoryId", currentTime)
            .apply()

        // 保存为最后选择的分类
        saveSelectedCategoryId(categoryId)
    }

    override suspend fun sortCategoriesByUsage(categories: List<CategoryItem>): List<CategoryItem> {
        return categories.sortedWith { category1, category2 ->
            // 首先按是否置顶排序
            if (category1.isPinned != category2.isPinned) {
                return@sortedWith if (category1.isPinned) -1 else 1
            }

            // 获取最后使用时间
            val lastUsedTime1 = sharedPreferences.getLong("${KEY_LAST_USED_TIME_PREFIX}${category1.id}", 0L)
            val lastUsedTime2 = sharedPreferences.getLong("${KEY_LAST_USED_TIME_PREFIX}${category2.id}", 0L)

            // 如果有最近使用记录，按时间倒序排序
            if (lastUsedTime1 != 0L || lastUsedTime2 != 0L) {
                val timeDiff = lastUsedTime2.compareTo(lastUsedTime1)
                if (timeDiff != 0) return@sortedWith timeDiff
            }

            // 然后按使用次数倒序排序
            val usageCount1 = sharedPreferences.getInt("${KEY_USAGE_COUNT_PREFIX}${category1.id}", 0)
            val usageCount2 = sharedPreferences.getInt("${KEY_USAGE_COUNT_PREFIX}${category2.id}", 0)
            val countDiff = usageCount2.compareTo(usageCount1)
            if (countDiff != 0) return@sortedWith countDiff

            // 最后按名称排序
            category1.displayName.compareTo(category2.displayName)
        }
    }

    companion object {
        private const val PREFERENCE_NAME = "category_preferences"
        private const val KEY_LAST_SELECTED_CATEGORY = "last_selected_category"
        private const val KEY_USAGE_COUNT_PREFIX = "usage_count_"
        private const val KEY_LAST_USED_TIME_PREFIX = "last_used_time_"
    }
}