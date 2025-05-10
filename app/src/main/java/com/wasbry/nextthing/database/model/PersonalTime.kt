package com.wasbry.nextthing.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "PersonalTimeTable")
data class PersonalTime(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timeDescription: String,
    val startTime: String,
    val endTime: String,
    val selfControlDegree: Int,
    val timeValue: Int,
    val iconPath: String
)
