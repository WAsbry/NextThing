package com.wasbry.nextthing.database.model

import android.content.Context
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * 时间类型的数据类
 * */
// model/TimeType.kt
@Entity(tableName = "TimeTypeTable")
data class TimeType(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val resPath: String,           // 资源名（如"icon_fitness_swimming"）
    val description: String,       // 图标描述
    val category: String,          // 分类：健身/工作/生活/娱乐
    val isUserUploaded: Boolean = false, // 是否用户上传
    val createTime: Long = System.currentTimeMillis()
) {
    // 获取图标资源ID的方法（用于UI显示）
    fun getIconResId(context: Context): Int {
        return if (isUserUploaded) {
            // 用户上传图标，使用文件路径加载（需用Glide等库）
            0
        } else {
            // 预置图标，通过资源名获取ID
            context.resources.getIdentifier(
                resPath, "mipmap", context.packageName
            )
        }
    }
}