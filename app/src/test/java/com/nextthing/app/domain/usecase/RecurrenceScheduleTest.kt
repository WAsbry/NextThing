package com.nextthing.app.domain.usecase

import com.nextthing.app.domain.model.RepeatFrequency
import com.nextthing.app.domain.model.RepeatFrequencyType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RecurrenceScheduleTest {

    @Test
    fun `does not generate before configured start date`() {
        assertFalse(
            RecurrenceSchedule.shouldGenerate(
                repeatFrequency = RepeatFrequency(RepeatFrequencyType.DAILY),
                startDate = LocalDate.of(2026, 8, 10),
                targetDate = LocalDate.of(2026, 8, 9)
            )
        )
    }

    @Test
    fun `weekly generation matches selected weekdays`() {
        val repeat = RepeatFrequency(
            type = RepeatFrequencyType.WEEKLY,
            weekdays = setOf(1, 3, 5)
        )

        assertTrue(
            RecurrenceSchedule.shouldGenerate(
                repeat,
                startDate = LocalDate.of(2026, 7, 1),
                targetDate = LocalDate.of(2026, 7, 29)
            )
        )
        assertFalse(
            RecurrenceSchedule.shouldGenerate(
                repeat,
                startDate = LocalDate.of(2026, 7, 1),
                targetDate = LocalDate.of(2026, 7, 30)
            )
        )
    }

    @Test
    fun `monthly day rolls to last day of shorter month`() {
        assertTrue(
            RecurrenceSchedule.shouldGenerate(
                repeatFrequency = RepeatFrequency(
                    type = RepeatFrequencyType.MONTHLY,
                    monthDays = setOf(31)
                ),
                startDate = LocalDate.of(2026, 1, 1),
                targetDate = LocalDate.of(2026, 2, 28)
            )
        )
    }
}
