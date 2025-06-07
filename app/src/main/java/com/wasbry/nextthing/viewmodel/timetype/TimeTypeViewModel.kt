package com.wasbry.nextthing.viewmodel.timetype

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wasbry.nextthing.database.dao.TimeTypeDao
import com.wasbry.nextthing.database.model.TimeType
import com.wasbry.nextthing.database.repository.TimeTypeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

class TimeTypeViewModel(private val timeTypeRepository: TimeTypeRepository) : ViewModel() {

    // 获取所有图标类型
    val allTimeTypes: Flow<List<TimeType>> = timeTypeRepository.allTimeTypes

    // 按分类获取图标类型
    fun getTimeTypesByCategory(category: String): Flow<List<TimeType>> {
        return timeTypeRepository.getTimeTypesByCategory(category)
            .distinctUntilChanged()
            .shareIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                replay = 1
            )
    }

    // 根据ID 获取图标资源
    suspend fun getTimeTypeById(id: Long): TimeType? {
        return timeTypeRepository.getTimeTypeById(id)
    }

    // 获取预置图标
    val presetTimeTypes: Flow<List<TimeType>> = timeTypeRepository.presetTimeTypes

    // 获取用户上传图标
    val userUploadedTimeTypes: Flow<List<TimeType>> = timeTypeRepository.userUploadedTimeTypes

    // 统计各分类图标数量
    val categoryCounts: Flow<List<TimeTypeDao.CategoryCount>> = timeTypeRepository.categoryCounts

    // 插入单个图标类型
    fun insertTimeType(timeType: TimeType) = viewModelScope.launch {
        timeTypeRepository.insertTimeType(timeType)
    }

    // 更新单个图标类型
    fun updateTimeType(timeType: TimeType) = viewModelScope.launch {
        timeTypeRepository.updateTimeType(timeType)
    }

    // 删除单个图标类型
    fun deleteTimeType(timeType: TimeType) = viewModelScope.launch {
        timeTypeRepository.deleteTimeType(timeType)
    }

    // 根据ID删除单个图标类型
    fun deleteTimeTypeById(id: Long) = viewModelScope.launch {
        timeTypeRepository.deleteTimeTypeById(id)
    }

    // 清空所有图标类型
    fun deleteAllTimeTypes() = viewModelScope.launch {
        timeTypeRepository.deleteAllTimeTypes()
    }

    // 添加预置图标（示例）
    fun addPresetIcon(iconResName: String, description: String, category: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val timeType = TimeType(
                resPath = iconResName,
                description = description,
                category = category,
                isUserUploaded = false
            )
            timeTypeRepository.insertTimeType(timeType)
            Log.d("TimeTypeViewModel", "Added preset icon: $description")
        }
    }

    // 添加用户上传图标（示例）
    fun addUserUploadedIcon(filePath: String, description: String, category: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val timeType = TimeType(
                resPath = filePath,
                description = description,
                category = category,
                isUserUploaded = true
            )
            timeTypeRepository.insertTimeType(timeType)
            Log.d("TimeTypeViewModel", "Added user uploaded icon: $description")
        }
    }

    // 更新图标分类（示例）
    fun updateIconCategory(timeType: TimeType, newCategory: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedTimeType = timeType.copy(
                category = newCategory
            )
            timeTypeRepository.updateTimeType(updatedTimeType)
            Log.d("TimeTypeViewModel", "Updated icon category: ${timeType.description}")
        }
    }
}