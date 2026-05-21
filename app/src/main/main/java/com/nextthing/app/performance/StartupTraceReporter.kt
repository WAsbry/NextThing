package com.nextthing.app.performance

object StartupTraceReporter {

    fun formatReport(checkpoints: Map<String, Long>, processStartMs: Long): String {
        val sb = StringBuilder()
        sb.appendLine("=== Startup Trace Report ===")
        sb.appendLine("Session: ${StartupTracker.sessionId.take(8)}...")
        sb.appendLine("Checkpoints: ${checkpoints.size}")
        sb.appendLine("---")

        val sorted = checkpoints.toList()
        for ((i, pair) in sorted.withIndex()) {
            val (label, ts) = pair
            val elapsed = if (processStartMs > 0) ts - processStartMs else 0L
            val delta = if (i > 0) ts - sorted[i - 1].second else 0L
            sb.appendLine("  $label: ${elapsed}ms (Δ${delta}ms)")
        }

        if (sorted.size >= 2 && processStartMs > 0) {
            val totalElapsed = sorted.last().second - processStartMs
            sb.appendLine("---")
            sb.appendLine("Total (process_start → ${sorted.last().first}): ${totalElapsed}ms")
        }

        sb.appendLine("=== End Report ===")
        return sb.toString()
    }
}
