package com.nextthing.app.data.service

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackendAiRunClientTest {

    @Test
    fun `parser ignores heartbeat and emits complete frame`() {
        val parser = SseFrameParser()

        assertNull(parser.accept(":heartbeat"))
        assertNull(parser.accept("id:3"))
        assertNull(parser.accept("event:TEXT_DELTA"))
        assertNull(parser.accept("data:{\"sequence\":3}"))

        assertEquals(
            SseFrame(3, "TEXT_DELTA", "{\"sequence\":3}"),
            parser.accept("")
        )
    }

    @Test
    fun `collector ignores replayed events and completes ordered text`() {
        val collector = AiRunEventCollector(Gson(), 100)
        val delta = SseFrame(
            3,
            "TEXT_DELTA",
            """{"sequence":3,"type":"TEXT_DELTA","payload":{"text":"SSE-OK"}}"""
        )

        collector.accept(delta)
        collector.accept(delta)
        assertFalse(collector.isTerminal)
        assertEquals(3, collector.lastEventId)

        collector.accept(
            SseFrame(
                4,
                "RUN_STATUS_CHANGED",
                """{"sequence":4,"type":"RUN_STATUS_CHANGED","payload":{"status":"COMPLETED"}}"""
            )
        )

        assertTrue(collector.isTerminal)
        assertEquals("SSE-OK", collector.result())
    }
}
