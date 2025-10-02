package com.example.nextthingb1.data.local.converter

import androidx.room.TypeConverter
import com.example.nextthingb1.domain.model.TaskCategory
import com.example.nextthingb1.domain.model.TaskStatus
import com.example.nextthingb1.domain.model.LocationType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Converters {

    private val gson = Gson()
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    @TypeConverter
    fun fromLocalDateTime(date: LocalDateTime?): String? = date?.format(dateFormatter)

    @TypeConverter
    fun toLocalDateTime(dateString: String?): LocalDateTime? = dateString?.let { LocalDateTime.parse(it, dateFormatter) }

    @TypeConverter
    fun fromStringList(value: List<String>): String = gson.toJson(value)

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    // Enum converters
    @TypeConverter
    fun fromTaskCategory(category: TaskCategory): String = category.name

    @TypeConverter
    fun toTaskCategory(name: String): TaskCategory = TaskCategory.valueOf(name)

    @TypeConverter
    fun fromTaskStatus(status: TaskStatus): String = status.name

    @TypeConverter
    fun toTaskStatus(name: String): TaskStatus = TaskStatus.valueOf(name)

    // LocationType converters
    @TypeConverter
    fun fromLocationType(type: LocationType): String = type.name

    @TypeConverter
    fun toLocationType(name: String): LocationType = LocationType.valueOf(name)
} 