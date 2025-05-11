package com.wasbry.nextthing.database.repository

import android.util.Log
import com.wasbry.nextthing.database.dao.PersonalTimeDao
import com.wasbry.nextthing.database.model.PersonalTime
import com.wasbry.nextthing.database.model.TodoTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

class PersonalTimeRepository(private val personalTimeDao: PersonalTimeDao) {

    // 插入单个 PersonalTime 记录
    suspend fun insertPersonalTime(personalTime: PersonalTime) {
        withContext(Dispatchers.IO) {
            personalTimeDao.insertPersonalTime(personalTime)
        }
    }

    // 插入多个 PersonalTime 记录
    suspend fun insertPersonalTimes(personalTimes: List<PersonalTime>) {
        withContext(Dispatchers.IO) {
            personalTimeDao.insertPersonalTimes(personalTimes)
        }
    }

    // 更新单个 PersonalTime 记录
    suspend fun updatePersonalTime(personalTime: PersonalTime) {
        withContext(Dispatchers.IO) {
            personalTimeDao.updatePersonalTime(personalTime)
        }
    }

    // 更新多个 PersonalTime 记录
    suspend fun updatePersonalTimes(personalTimes: List<PersonalTime>) {
        withContext(Dispatchers.IO) {
            personalTimeDao.updatePersonalTimes(personalTimes)
        }
    }

    // 删除单个 PersonalTime 记录
    suspend fun deletePersonalTime(personalTime: PersonalTime) {
        withContext(Dispatchers.IO) {
            personalTimeDao.deletePersonalTime(personalTime)
        }
    }

    // 清空所有 PersonalTime 记录
    suspend fun deleteAllPersonalTime() {
        withContext(Dispatchers.IO) {
            personalTimeDao.deleteAllPersonalTime()
        }
    }

    // 获取所有 PersonalTime 记录的 Flow
    val allPersonalTime: Flow<List<PersonalTime>> = personalTimeDao.getAllPersonalTime()
        .onEach { times ->
            Log.d("PersonalTimeRepository", "Received ${times.size} PersonalTime records: $times")
        }

    // 获取单个 PersonalTime 记录
    fun getPersonalTimeById(id: Long): Flow<PersonalTime> {
        return personalTimeDao.getPersonalTimeById(id)
    }

    // 根据 startTime 和 endTime 查询时间段内的 PersonalTime 记录
    fun getPersonalTimeByTimeRange(startTime: String, endTime: String): Flow<List<PersonalTime>> {
        return personalTimeDao.getPersonalTimeByTimeRange(startTime, endTime)
    }

    // 根据 selfControlDegree 的范围查询 PersonalTime 记录
    fun getPersonalTimeBySelfControlDegreeRange(minDegree: Int, maxDegree: Int): Flow<List<PersonalTime>> {
        return personalTimeDao.getPersonalTimeBySelfControlDegreeRange(minDegree, maxDegree)
    }

    // 根据 timeValue 的范围查询 PersonalTime 记录
    fun getPersonalTimeByTimeValueRange(minValue: Int, maxValue: Int): Flow<List<PersonalTime>> {
        return personalTimeDao.getPersonalTimeByTimeValueRange(minValue, maxValue)
    }
}