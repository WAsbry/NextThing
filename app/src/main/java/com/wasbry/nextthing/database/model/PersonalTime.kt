package com.wasbry.nextthing.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "PersonalTimeTable")
data class PersonalTime(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timeDescription: String, // 时间段的描述
    val startTime: String, // 开始时间：时:分
    val endTime: String, // 结束时间：时:分
    val selfControlDegree: Int, // 时间的自主程度：1-10
    val timeValue: Int, // 时间的价值：1-10
    val iconPath: String // 时间段的icon 的id
)
