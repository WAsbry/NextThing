package com.nextthing.app.performance

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "startup_traces",
    indices = [Index("sessionId")]
)
data class StartupTraceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: String,
    val checkpoint: String,
    val timestampMs: Long,
    val elapsedFromProcessStart: Long,
    val createdAt: Long
)
