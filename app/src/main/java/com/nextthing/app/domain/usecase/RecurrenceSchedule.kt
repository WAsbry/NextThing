package com.nextthing.app.domain.usecase

import com.nextthing.app.domain.model.RepeatFrequency
import com.nextthing.app.domain.model.RepeatFrequencyType
import com.nextthing.app.util.LegalHolidayHelper
import java.time.LocalDate

object RecurrenceSchedule {

    fun shouldGenerate(
        repeatFrequency: RepeatFrequency,
        startDate: LocalDate,
        targetDate: LocalDate
    ): Boolean {
        if (targetDate.isBefore(startDate)) return false

        return when (repeatFrequency.type) {
            RepeatFrequencyType.NONE -> false
            RepeatFrequencyType.DAILY -> true
            RepeatFrequencyType.WEEKDAYS -> targetDate.dayOfWeek.value in 1..5
            RepeatFrequencyType.WEEKENDS -> targetDate.dayOfWeek.value in 6..7
            RepeatFrequencyType.LEGAL_HOLIDAY -> LegalHolidayHelper.isLegalHoliday(targetDate)
            RepeatFrequencyType.WEEKLY ->
                targetDate.dayOfWeek.value in repeatFrequency.weekdays

            RepeatFrequencyType.MONTHLY -> {
                val targetDay = targetDate.dayOfMonth
                val monthLength = targetDate.lengthOfMonth()
                targetDay in repeatFrequency.monthDays ||
                    (targetDay == monthLength && repeatFrequency.monthDays.any { it > monthLength })
            }
        }
    }
}
