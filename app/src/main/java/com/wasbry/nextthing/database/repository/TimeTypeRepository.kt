package com.wasbry.nextthing.database.repository

import android.util.Log
import com.wasbry.nextthing.database.dao.TimeTypeDao
import com.wasbry.nextthing.database.model.TimeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

class TimeTypeRepository(private val timeTypeDao: TimeTypeDao) {

    // 获取所有图标类型的Flow，用于观察数据变化
    val allTimeTypes: Flow<List<TimeType>> = timeTypeDao.getAllTimeTypes()
        .onEach { types ->
            Log.d("TimeTypeRepository", "Received ${types.size} time types: $types")
        }

    // 获取预置图标
    suspend fun getPresetTimeTypes(): Flow<List<TimeType>> {
        return timeTypeDao.getPresetTimeTypes()
    }


    // 按分类获取图标类型
    fun getTimeTypesByCategory(category: String): Flow<List<TimeType>> {
        return timeTypeDao.getTimeTypesByCategory(category)
    }

    // 获取预置图标
    val presetTimeTypes: Flow<List<TimeType>> = timeTypeDao.getPresetTimeTypes()

    // 获取用户上传图标
    val userUploadedTimeTypes: Flow<List<TimeType>> = timeTypeDao.getUserUploadedTimeTypes()

    // 统计各分类图标数量
    val categoryCounts: Flow<List<TimeTypeDao.CategoryCount>> = timeTypeDao.getCategoryCount()

    // 插入单个图标类型
    suspend fun insertTimeType(timeType: TimeType) {
        withContext(Dispatchers.IO) {
            timeTypeDao.insertTimeType(timeType)
        }
    }

    // 插入多个图标类型
    suspend fun insertTimeTypes(timeTypes: List<TimeType>) {
        withContext(Dispatchers.IO) {
            timeTypeDao.insertTimeTypes(timeTypes)
        }
    }

    // 更新单个图标类型
    suspend fun updateTimeType(timeType: TimeType) {
        withContext(Dispatchers.IO) {
            timeTypeDao.updateTimeType(timeType)
        }
    }

    // 更新多个图标类型
    suspend fun updateTimeTypes(timeTypes: List<TimeType>) {
        withContext(Dispatchers.IO) {
            timeTypeDao.updateTimeTypes(timeTypes)
        }
    }

    // 删除单个图标类型
    suspend fun deleteTimeType(timeType: TimeType) {
        withContext(Dispatchers.IO) {
            timeTypeDao.deleteTimeType(timeType)
        }
    }

    // 根据ID删除单个图标类型
    suspend fun deleteTimeTypeById(id: Long) {
        withContext(Dispatchers.IO) {
            timeTypeDao.deleteTimeTypeById(id)
        }
    }

    // 清空所有图标类型
    suspend fun deleteAllTimeTypes() {
        withContext(Dispatchers.IO) {
            timeTypeDao.deleteAllTimeTypes()
        }
    }

    // 根据ID获取单个图标类型
    suspend fun getTimeTypeById(id: Long): TimeType? {
        return withContext(Dispatchers.IO) {
            timeTypeDao.getTimeTypeById(id)
        }
    }
}