package com.wasbry.nextthing.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.wasbry.nextthing.database.model.TimeType
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeTypeDao {

    // 查询所有图标类型，按创建时间排序
    @Query("SELECT * FROM TimeTypeTable ORDER BY createTime ASC")
    fun getAllTimeTypes(): Flow<List<TimeType>>

    // 添加查询预置图标的方法
    @Query("SELECT * FROM TimeTypeTable WHERE isUserUploaded = 0")
    fun getPresetTimeTypes(): Flow<List<TimeType>>

    // 根据分类查询图标类型
    @Query("SELECT * FROM TimeTypeTable WHERE category = :category ORDER BY createTime DESC")
    fun getTimeTypesByCategory(category: String): Flow<List<TimeType>>

    // 查询用户上传图标
    @Query("SELECT * FROM TimeTypeTable WHERE isUserUploaded = 1 ORDER BY createTime ASC")
    fun getUserUploadedTimeTypes(): Flow<List<TimeType>>

    // 根据ID查询单个图标类型
    @Query("SELECT * FROM TimeTypeTable WHERE id = :id")
    fun getTimeTypeById(id: Long): TimeType?

    // 插入单个图标类型
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTimeType(timeType: TimeType): Long

    // 插入多个图标类型
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTimeTypes(timeTypes: List<TimeType>)

    // 更新单个图标类型
    @Update
    fun updateTimeType(timeType: TimeType)

    // 更新多个图标类型
    @Update
    fun updateTimeTypes(timeTypes: List<TimeType>)

    // 删除单个图标类型
    @Delete
    fun deleteTimeType(timeType: TimeType)

    // 根据ID删除单个图标类型
    @Query("DELETE FROM TimeTypeTable WHERE id = :id")
    fun deleteTimeTypeById(id: Long)

    // 清空所有图标类型
    @Query("DELETE FROM TimeTypeTable")
    fun deleteAllTimeTypes()

    // 统计各分类下的图标数量
    @Query("SELECT category, COUNT(*) as count FROM TimeTypeTable GROUP BY category")
    fun getCategoryCount(): Flow<List<CategoryCount>>

    // 内部类用于统计查询结果
    data class CategoryCount(
        val category: String,
        val count: Int
    )
}