package com.nextthing.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.nextthing.app.performance.StartupTraceEntity

@Dao
interface StartupTraceDao {

    @Insert
    suspend fun insert(trace: StartupTraceEntity)

    @Query("SELECT * FROM startup_traces WHERE sessionId = :sessionId ORDER BY elapsedFromProcessStart")
    suspend fun getBySession(sessionId: String): List<StartupTraceEntity>

    @Query("SELECT DISTINCT sessionId FROM startup_traces ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentSessionIds(limit: Int): List<String>

    @Query("DELETE FROM startup_traces WHERE createdAt < :cutoffTime")
    suspend fun deleteOlderThan(cutoffTime: Long): Int

    @Query("DELETE FROM startup_traces")
    suspend fun deleteAll()
}
