package com.nextthing.app.performance

import android.os.Process
import android.os.SystemClock
import java.util.UUID

object StartupTracker {

    private val checkpoints = LinkedHashMap<String, Long>()
    private var processStartMs: Long = 0
    private var initialized = false
    private var flushedCount = 0

    val sessionId: String = UUID.randomUUID().toString().replace("-", "")

    fun init() {
        if (initialized) return
        initialized = true
        processStartMs = Process.getStartUptimeMillis()
        checkpoints["process_start"] = processStartMs
    }

    fun record(label: String) {
        if (!initialized) init()
        val now = SystemClock.uptimeMillis()
        checkpoints[label] = now
        val elapsed = now - processStartMs
        timber.log.Timber.tag("StartupTrace").d("$label: ${elapsed}ms")
    }

    fun takeNewCheckpoints(): Map<String, Long> {
        val all = LinkedHashMap(checkpoints)
        val new = all.entries.drop(flushedCount).associate { it.key to it.value }
        flushedCount = all.size
        return new
    }

    fun getCheckpointTimestamp(label: String): Long? = checkpoints[label]

    fun getProcessStartMs(): Long = processStartMs

    fun getAllCheckpoints(): Map<String, Long> = LinkedHashMap(checkpoints)

    fun reset() {
        checkpoints.clear()
        initialized = false
    }
}
