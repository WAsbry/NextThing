package com.wasbry.nextthing.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * 时间类型的数据类
 * */
@Entity(tableName = "TimeTypeTable")
data class TimeType(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val resPath: String = "", // 预置图标：资源名(如"ic_fitness")；用户图标：文件路径
    val description: String,  // 图标描述

    val category: String,     // 分类：健身/工作/生活/娱乐
    val isUserUploaded: Boolean = false, // 是否用户上传

    var createTime: Long = System.currentTimeMillis(), // 创建时间戳
    var count: Long = 0
)