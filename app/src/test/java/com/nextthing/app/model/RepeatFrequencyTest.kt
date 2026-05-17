package com.nextthing.app.model

import com.nextthing.app.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class RepeatFrequencyTest {

    // ===== isValid =====

    @Test
    fun `NONE is always valid`() {
        assertTrue(RepeatFrequency(type = RepeatFrequencyType.NONE).isValid())
    }

    @Test
    fun `DAILY is always valid`() {
        assertTrue(RepeatFrequency(type = RepeatFrequencyType.DAILY).isValid())
    }

    @Test
    fun `WEEKDAYS is always valid`() {
        assertTrue(RepeatFrequency(type = RepeatFrequencyType.WEEKDAYS).isValid())
    }

    @Test
    fun `WEEKENDS is always valid`() {
        assertTrue(RepeatFrequency(type = RepeatFrequencyType.WEEKENDS).isValid())
    }

    @Test
    fun `LEGAL_HOLIDAY is always valid`() {
        assertTrue(RepeatFrequency(type = RepeatFrequencyType.LEGAL_HOLIDAY).isValid())
    }

    @Test
    fun `WEEKLY with empty weekdays is invalid`() {
        assertFalse(RepeatFrequency(type = RepeatFrequencyType.WEEKLY, weekdays = emptySet()).isValid())
    }

    @Test
    fun `WEEKLY with selected weekdays is valid`() {
        assertTrue(RepeatFrequency(type = RepeatFrequencyType.WEEKLY, weekdays = setOf(1, 3, 5)).isValid())
    }

    @Test
    fun `MONTHLY with empty monthDays is invalid`() {
        assertFalse(RepeatFrequency(type = RepeatFrequencyType.MONTHLY, monthDays = emptySet()).isValid())
    }

    @Test
    fun `MONTHLY with selected monthDays is valid`() {
        assertTrue(RepeatFrequency(type = RepeatFrequencyType.MONTHLY, monthDays = setOf(1, 15)).isValid())
    }

    // ===== getDisplayText =====

    @Test
    fun `NONE displays as single task`() {
        assertEquals("单次任务", RepeatFrequency(type = RepeatFrequencyType.NONE).getDisplayText())
    }

    @Test
    fun `DAILY displays correctly`() {
        assertEquals("每日", RepeatFrequency(type = RepeatFrequencyType.DAILY).getDisplayText())
    }

    @Test
    fun `WEEKDAYS displays correctly`() {
        assertEquals("工作日", RepeatFrequency(type = RepeatFrequencyType.WEEKDAYS).getDisplayText())
    }

    @Test
    fun `WEEKENDS displays correctly`() {
        assertEquals("周末", RepeatFrequency(type = RepeatFrequencyType.WEEKENDS).getDisplayText())
    }

    @Test
    fun `LEGAL_HOLIDAY displays correctly`() {
        assertEquals("法定节假日", RepeatFrequency(type = RepeatFrequencyType.LEGAL_HOLIDAY).getDisplayText())
    }

    @Test
    fun `WEEKLY with no days shows custom label`() {
        assertEquals("自定义（每周）", RepeatFrequency(type = RepeatFrequencyType.WEEKLY).getDisplayText())
    }

    @Test
    fun `WEEKLY with selected days shows day names`() {
        val freq = RepeatFrequency(type = RepeatFrequencyType.WEEKLY, weekdays = setOf(1, 3, 5))
        assertEquals("每周一、三、五", freq.getDisplayText())
    }

    @Test
    fun `WEEKLY sorts days in order`() {
        val freq = RepeatFrequency(type = RepeatFrequencyType.WEEKLY, weekdays = setOf(5, 1, 3))
        assertEquals("每周一、三、五", freq.getDisplayText())
    }

    @Test
    fun `MONTHLY with no days shows custom label`() {
        assertEquals("自定义（每月）", RepeatFrequency(type = RepeatFrequencyType.MONTHLY).getDisplayText())
    }

    @Test
    fun `MONTHLY with selected days shows dates`() {
        val freq = RepeatFrequency(type = RepeatFrequencyType.MONTHLY, monthDays = setOf(1, 15))
        val text = freq.getDisplayText()
        assertEquals("每月1日、15日", text)
    }

    // ===== WeekdayItem =====

    @Test
    fun `createWeekdays returns 7 items`() {
        val weekdays = WeekdayItem.createWeekdays()
        assertEquals(7, weekdays.size)
    }

    @Test
    fun `createWeekdays maps days correctly`() {
        val weekdays = WeekdayItem.createWeekdays()
        assertEquals(1, weekdays[0].dayOfWeek)
        assertEquals("一", weekdays[0].displayName)
        assertEquals(7, weekdays[6].dayOfWeek)
        assertEquals("日", weekdays[6].displayName)
    }

    @Test
    fun `createWeekdays marks selected days`() {
        val weekdays = WeekdayItem.createWeekdays(selectedDays = setOf(1, 5))
        assertTrue(weekdays[0].isSelected)
        assertFalse(weekdays[1].isSelected)
        assertTrue(weekdays[4].isSelected)
    }

    // ===== MonthDayItem =====

    @Test
    fun `createMonthDays returns 31 items`() {
        val monthDays = MonthDayItem.createMonthDays()
        assertEquals(31, monthDays.size)
    }

    @Test
    fun `createMonthDays starts from day 1`() {
        val monthDays = MonthDayItem.createMonthDays()
        assertEquals(1, monthDays[0].day)
        assertEquals(31, monthDays[30].day)
    }

    @Test
    fun `createMonthDays marks selected days`() {
        val monthDays = MonthDayItem.createMonthDays(selectedDays = setOf(1, 15, 31))
        assertTrue(monthDays[0].isSelected)
        assertTrue(monthDays[14].isSelected)
        assertTrue(monthDays[30].isSelected)
        assertFalse(monthDays[5].isSelected)
    }
}
