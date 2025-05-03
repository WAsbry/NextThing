package com.wasbry.nextthing.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.wasbry.nextthing.database.model.PersonalTime
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalTimeDao {
    @Insert
    fun insertPersonalTime(personalTime: PersonalTime): Long

    @Update
    fun updatePersonalTime(personalTime: PersonalTime)

    @Delete
    fun deletePersonalTime(personalTime: PersonalTime)

    @Query("SELECT * FROM PersonalTimeTable ORDER BY startTime ASC, timeValue DESC, selfControlDegree DESC")
    fun getAllPersonalTime(): Flow<List<PersonalTime>>

    @Query("SELECT * FROM PersonalTimeTable WHERE id = :id")
    fun getPersonalTimeById(id: Long): PersonalTime
}