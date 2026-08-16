package com.nextthing.app.data.service

import com.google.gson.Gson
import com.nextthing.app.domain.model.RepeatFrequencyType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class AITaskParserServiceTest {

    private lateinit var completionClient: AICompletionClient
    private lateinit var parser: AITaskParserService

    @Before
    fun setUp() {
        completionClient = mock()
        parser = AITaskParserService(completionClient, Gson())
    }

    @Test
    fun `blank input is rejected before calling AI`() = runTest {
        val result = parser.parseTaskFromText("   ", emptyList(), emptyList(), null)

        assertTrue(result.isFailure)
        verify(completionClient, never()).complete(any(), isNull(), eq(true))
    }

    @Test
    fun `empty task array is rejected instead of reporting fake success`() = runTest {
        stubReply("[]")

        val result = parser.parseTaskFromText("明天开会", emptyList(), emptyList(), null)

        assertTrue(result.isFailure)
    }

    @Test
    fun `one malformed item rejects whole response instead of silently dropping it`() = runTest {
        stubReply(
            """
            [
              {"title":"准备周报","repeatType":"NONE","confidence":0.9},
              {"title":"  ","repeatType":"NONE","confidence":0.8}
            ]
            """.trimIndent()
        )

        val result = parser.parseTaskFromText("准备周报并发邮件", emptyList(), emptyList(), null)

        assertTrue(result.isFailure)
    }

    @Test
    fun `valid weekly task is parsed with strict repeat weekdays`() = runTest {
        stubReply(
            """
            [{
              "title":"每周复盘",
              "description":"整理本周产出",
              "dueDate":"2026-07-31T20:00:00",
              "categoryName":"工作",
              "importance":"IMPORTANT_NOT_URGENT",
              "repeatType":"WEEKLY",
              "repeatWeekdays":[5],
              "locationName":null,
              "confidence":0.92
            }]
            """.trimIndent()
        )

        val result = parser.parseTaskFromText("每周五晚上复盘", listOf("工作"), emptyList(), null)

        assertTrue(result.isSuccess)
        val task = result.getOrThrow().single()
        assertEquals("每周复盘", task.title)
        assertEquals(RepeatFrequencyType.WEEKLY, task.repeatType)
        assertEquals(setOf(5), task.repeatWeekdays)
        assertEquals(0.92f, task.confidence)
    }

    @Test
    fun `cancellation is propagated instead of converted into parse failure`() = runTest {
        whenever(completionClient.complete(any(), isNull(), eq(true)))
            .thenThrow(CancellationException("screen closed"))

        try {
            parser.parseTaskFromText("明天开会", emptyList(), emptyList(), null)
            fail("CancellationException expected")
        } catch (_: CancellationException) {
            Unit
        }
    }

    private suspend fun stubReply(json: String) {
        whenever(completionClient.complete(any(), isNull(), eq(true)))
            .thenReturn(Result.success(json))
    }
}
