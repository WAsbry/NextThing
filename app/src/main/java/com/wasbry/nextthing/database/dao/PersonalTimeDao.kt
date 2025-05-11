package com.wasbry.nextthing.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wasbry.nextthing.database.model.PersonalTime
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalTimeDao {

    // 插入单个 PersonalTime 记录，并返回插入的 ID
    // onConflict = OnConflictStrategy.REPLACE 表示如果有冲突（例如主键重复），则替换原有数据
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPersonalTime(personalTime: PersonalTime): Long // 不能加suspend 关键字

    // 插入多个 PersonalTime 记录
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPersonalTimes(personalTimes: List<PersonalTime>)

    // 更新单个 PersonalTime 记录
    @Update
    fun updatePersonalTime(personalTime: PersonalTime)

    // 更新多个 PersonalTime 记录
    @Update
    fun updatePersonalTimes(personalTimes: List<PersonalTime>)

    // 删除单个 PersonalTime 记录
    @Delete
    fun deletePersonalTime(personalTime: PersonalTime)

    // 根据 ID 删除单个 PersonalTime 记录
    @Query("DELETE FROM PersonalTimeTable WHERE id = :id")
    fun deletePersonalTimeById(id: Long)

    // 清空所有 PersonalTime 记录
    @Query("DELETE FROM PersonalTimeTable")
    fun deleteAllPersonalTime()

    // 按照 startTime 升序，timeValue 降序，selfControlDegree 降序获取所有 PersonalTime 记录
    @Query("SELECT * FROM PersonalTimeTable ORDER BY startTime ASC, timeValue DESC, selfControlDegree DESC")
    fun getAllPersonalTime(): Flow<List<PersonalTime>>

    // 根据 ID 获取单个 PersonalTime 记录
    @Query("SELECT * FROM PersonalTimeTable WHERE id = :id")
    fun getPersonalTimeById(id: Long): Flow<PersonalTime>

    // 根据 startTime 和 endTime 查询时间段内的 PersonalTime 记录
    @Query("SELECT * FROM PersonalTimeTable WHERE startTime <= :endTime AND endTime >= :startTime")
    fun getPersonalTimeByTimeRange(startTime: String, endTime: String): Flow<List<PersonalTime>>

    // 根据 selfControlDegree 的范围查询 PersonalTime 记录
    @Query("SELECT * FROM PersonalTimeTable WHERE selfControlDegree >= :minDegree AND selfControlDegree <= :maxDegree")
    fun getPersonalTimeBySelfControlDegreeRange(minDegree: Int, maxDegree: Int): Flow<List<PersonalTime>>

    // 根据 timeValue 的范围查询 PersonalTime 记录
    @Query("SELECT * FROM PersonalTimeTable WHERE timeValue >= :minValue AND timeValue <= :maxValue")
    fun getPersonalTimeByTimeValueRange(minValue: Int, maxValue: Int): Flow<List<PersonalTime>>
}