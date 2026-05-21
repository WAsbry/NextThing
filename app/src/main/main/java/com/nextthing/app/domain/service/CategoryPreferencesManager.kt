package com.nextthing.app.domain.service

import com.nextthing.app.domain.model.CategoryItem

interface CategoryPreferencesManager {
    /**
     * 获取上次选择的分类ID，首次使用返回"LIFE"
     */
    suspend fun getLastSelectedCategoryId(): String

    /**
     * 保存选择的分类ID
     */
    suspend fun saveSelectedCategoryId(categoryId: String)

    /**
     * 记录分类使用次数
     */
    suspend fun recordCategoryUsage(categoryId: String)

    /**
     * 根据使用频率对分类进行排序
     * 最近使用的排在前面，然后按使用次数排序
     */
    suspend fun sortCategoriesByUsage(categories: List<CategoryItem>): List<CategoryItem>

    /**
     * 获取上次选择的通知策略ID，首次使用返回null
     */
    suspend fun getLastSelectedNotificationStrategyId(): String?

    /**
     * 保存选择的通知策略ID
     */
    suspend fun saveLastSelectedNotificationStrategyId(strategyId: String?)

    /**
     * 获取上次选择的地理围栏地点ID，首次使用返回null
     */
    suspend fun getLastSelectedGeofenceLocationId(): String?

    /**
     * 保存选择的地理围栏地点ID
     */
    suspend fun saveLastSelectedGeofenceLocationId(locationId: String?)
}