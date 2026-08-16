package com.nextthing.app.work

import com.nextthing.app.domain.service.AIBriefingGenerator.BriefingType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class BriefingTypeResolverTest {

    @Test
    fun `scheduled type wins even when work is delayed`() {
        val result = BriefingTypeResolver.resolve(
            scheduledType = BriefingType.MORNING.name,
            now = LocalTime.of(15, 30),
            morningTime = LocalTime.of(8, 0),
            eveningTime = LocalTime.of(21, 0)
        )

        assertEquals(BriefingType.MORNING, result)
    }

    @Test
    fun `immediate briefing uses nearest configured time`() {
        assertEquals(
            BriefingType.MORNING,
            BriefingTypeResolver.resolve(
                scheduledType = null,
                now = LocalTime.of(9, 0),
                morningTime = LocalTime.of(8, 0),
                eveningTime = LocalTime.of(21, 0)
            )
        )
        assertEquals(
            BriefingType.EVENING,
            BriefingTypeResolver.resolve(
                scheduledType = null,
                now = LocalTime.of(20, 0),
                morningTime = LocalTime.of(8, 0),
                eveningTime = LocalTime.of(21, 0)
            )
        )
    }

    @Test
    fun `immediate briefing handles midnight wraparound`() {
        val result = BriefingTypeResolver.resolve(
            scheduledType = null,
            now = LocalTime.of(23, 55),
            morningTime = LocalTime.of(0, 10),
            eveningTime = LocalTime.of(18, 0)
        )

        assertEquals(BriefingType.MORNING, result)
    }
}
