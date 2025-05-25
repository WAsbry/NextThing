package com.wasbry.nextthing.ui.screen.AddTask.component.model

/**
 * 健身Icon 的数据类
 * */
data class TimeIconModel(
    val iconResId: Int,   // 图片资源ID（如R.mipmap.icon_fitness_swimming）
    val description: String, // 描述文本
    var type: String // 图标的类型
)
