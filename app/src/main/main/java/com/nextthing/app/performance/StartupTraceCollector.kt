package com.nextthing.app.performance

import com.nextthing.app.data.local.dao.StartupTraceDao
import timber.log.Timber

object StartupTraceCollector {

    suspend fun flushNewToDatabase(dao: StartupTraceDao) {
        val checkpoints = StartupTracker.takeNewCheckpoints()
        if (checkpoints.isEmpty()) return

        val sessionId = StartupTracker.sessionId
        val processStartMs = StartupTracker.getProcessStartMs()
        val now = System.currentTimeMillis()

        checkpoints.forEach { (label, timestampMs) ->
            val entity = StartupTraceEntity(
                sessionId = sessionId,
                checkpoint = label,
                timestampMs = timestampMs,
                elapsedFromProcessStart = if (processStartMs > 0) timestampMs - processStartMs else 0L,
                createdAt = now
            )
            dao.insert(entity)
        }

        val allCheckpoints = StartupTracker.getAllCheckpoints()
        val report = StartupTraceReporter.formatReport(allCheckpoints, processStartMs)
        Timber.tag("StartupReport").d(report)
    }
}
