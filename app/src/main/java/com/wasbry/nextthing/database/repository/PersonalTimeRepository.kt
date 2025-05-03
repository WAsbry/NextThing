package com.wasbry.nextthing.database.repository

import android.util.Log
import com.wasbry.nextthing.database.TodoDatabase
import com.wasbry.nextthing.database.dao.PersonalTimeDao
import com.wasbry.nextthing.database.dao.TodoTaskDao
import com.wasbry.nextthing.database.model.PersonalTime
import com.wasbry.nextthing.database.model.TodoTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

/**
 * 将dao 层的操作封装起来，供上层调用
 * */
class PersonalTimeRepository(private val personalTimeDao: PersonalTimeDao) {

    suspend fun insertPersonalTime(personalTime: PersonalTime){
        withContext(Dispatchers.IO) {
            personalTimeDao.insertPersonalTime(personalTime)
        }
    }

    suspend fun updatePersonalTime(personalTime: PersonalTime) {
        withContext(Dispatchers.IO) {
            personalTimeDao.updatePersonalTime(personalTime)
        }
    }

    suspend fun deletePersonalTime(personalTime: PersonalTime) {
        withContext(Dispatchers.IO) {
            personalTimeDao.deletePersonalTime(personalTime)
        }
    }

    val allPersonalTime: Flow<List<PersonalTime>> = personalTimeDao.getAllPersonalTime()

//    suspend fun getAllPersonalTime(): Flow<List<PersonalTime>> {
//        withContext(Dispatchers.IO) {
//            return personalTimeDao.getAllPersonalTime()
//        }
//    }
//
//    fun getPersonalTimeById(id: Long): PersonalTime {
//        return personalTimeDao.getPersonalTimeById(id)
//    }
}