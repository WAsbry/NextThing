package com.wasbry.nextthing.database.repository

import android.content.Context
import android.util.Log
import com.wasbry.nextthing.database.data.PresetIcons
import kotlinx.coroutines.flow.first

// repository/PresetIconImporter.kt
class PresetIconImporter(
    private val context: Context,
    private val timeTypeRepository: TimeTypeRepository
) {
    // 导入所有预置图标
    suspend fun importPresetIcons() {
        // 检查是否已导入（避免重复）
        if (timeTypeRepository.getPresetTimeTypes().first().isNotEmpty()) {
            Log.d("PresetIconImporter", "预置图标已导入，跳过操作")
            return
        }

        // 生成预置TimeType对象
        val presetTimeTypes = PresetIcons.generatePresetTimeTypes(context)

        // 批量插入数据库
        timeTypeRepository.insertTimeTypes(presetTimeTypes)

        Log.d("PresetIconImporter", "成功导入${presetTimeTypes.size}个预置图标")
    }
}