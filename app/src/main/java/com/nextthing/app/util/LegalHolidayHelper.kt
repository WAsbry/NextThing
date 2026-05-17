package com.nextthing.app.util

import java.time.LocalDate

/**
 * 中国法定节假日辅助类
 *
 * 内置2025-2027年法定节假日数据（含调休后的实际放假日期）。
 * 2026-2027年数据以国务院发布的公告为准，此处为预估值，后续可更新。
 */
object LegalHolidayHelper {

    private val holidays: Set<LocalDate> = buildSet {
        // ── 2025年 ──────────────────────────────────────
        // 元旦
        add(LocalDate.of(2025, 1, 1))
        // 春节（1月28日～2月4日）
        addAll(datesOf(2025, 1, 28, 2025, 2, 4))
        // 清明节
        add(LocalDate.of(2025, 4, 4))
        add(LocalDate.of(2025, 4, 5))
        add(LocalDate.of(2025, 4, 6))
        // 劳动节（5月1日～5日）
        addAll(datesOf(2025, 5, 1, 2025, 5, 5))
        // 端午节
        add(LocalDate.of(2025, 5, 31))
        add(LocalDate.of(2025, 6, 1))
        add(LocalDate.of(2025, 6, 2))
        // 国庆节 + 中秋（10月1日～8日）
        addAll(datesOf(2025, 10, 1, 2025, 10, 8))

        // ── 2026年（预估，待政府公告更新） ────────────────
        // 元旦
        add(LocalDate.of(2026, 1, 1))
        // 春节（约2月17日～23日）
        addAll(datesOf(2026, 2, 17, 2026, 2, 23))
        // 清明节
        add(LocalDate.of(2026, 4, 5))
        add(LocalDate.of(2026, 4, 6))
        add(LocalDate.of(2026, 4, 7))
        // 劳动节
        addAll(datesOf(2026, 5, 1, 2026, 5, 5))
        // 端午节
        add(LocalDate.of(2026, 6, 19))
        add(LocalDate.of(2026, 6, 20))
        add(LocalDate.of(2026, 6, 21))
        // 中秋节
        add(LocalDate.of(2026, 9, 24))
        add(LocalDate.of(2026, 9, 25))
        add(LocalDate.of(2026, 9, 26))
        // 国庆节
        addAll(datesOf(2026, 10, 1, 2026, 10, 7))

        // ── 2027年（预估，待政府公告更新） ────────────────
        // 元旦
        add(LocalDate.of(2027, 1, 1))
        // 春节（约2月6日～12日）
        addAll(datesOf(2027, 2, 6, 2027, 2, 12))
        // 清明节
        add(LocalDate.of(2027, 4, 5))
        add(LocalDate.of(2027, 4, 6))
        add(LocalDate.of(2027, 4, 7))
        // 劳动节
        addAll(datesOf(2027, 5, 1, 2027, 5, 5))
        // 端午节（约6月9日）
        add(LocalDate.of(2027, 6, 8))
        add(LocalDate.of(2027, 6, 9))
        add(LocalDate.of(2027, 6, 10))
        // 中秋节（约9月15日）
        add(LocalDate.of(2027, 9, 14))
        add(LocalDate.of(2027, 9, 15))
        add(LocalDate.of(2027, 9, 16))
        // 国庆节
        addAll(datesOf(2027, 10, 1, 2027, 10, 7))
    }

    /** 判断指定日期是否为法定节假日 */
    fun isLegalHoliday(date: LocalDate): Boolean = holidays.contains(date)

    /** 生成从 [startYear-startMonth-startDay] 到 [endYear-endMonth-endDay] 的所有日期（含两端） */
    private fun datesOf(
        startYear: Int, startMonth: Int, startDay: Int,
        endYear: Int, endMonth: Int, endDay: Int
    ): List<LocalDate> {
        val result = mutableListOf<LocalDate>()
        var current = LocalDate.of(startYear, startMonth, startDay)
        val end = LocalDate.of(endYear, endMonth, endDay)
        while (!current.isAfter(end)) {
            result.add(current)
            current = current.plusDays(1)
        }
        return result
    }
}
