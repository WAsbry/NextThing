package com.wasbry.nextthing

import android.app.Application
import android.util.Log
import com.wasbry.nextthing.database.TodoDatabase
import com.wasbry.nextthing.database.repository.PresetIconImporter
import com.wasbry.nextthing.database.repository.TimeTypeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// MyApplication.kt
class MyApplication : Application() {

    val tag = "MyApplication"

    // 依赖注入（简化版，实际项目建议使用Hilt）
    val database by lazy { TodoDatabase.getInstance(this) }

    val timeTypeRepository by lazy { TimeTypeRepository(database.timeTypeDao()) }
    val presetIconImporter by lazy { PresetIconImporter(this, timeTypeRepository) }

    override fun onCreate() {
        super.onCreate()
        // 在后台线程导入预置图标
        CoroutineScope(Dispatchers.IO).launch {
            Log.d(tag,"应用启动，在后台导入预置图标")
            presetIconImporter.importPresetIcons()
        }
    }
}