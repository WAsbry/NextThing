package com.nextthing.app.util

import android.icu.util.ChineseCalendar
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

/**
 * 农历/节气/节日辅助类
 *
 * 基于 Android 内置 ICU 库（android.icu.util.4+），零外部依赖。
 * 返回指定日期的：
 * - 农历全称：乙巳蛇年二月初八
 * - 节气名称：清明（当天为二十四节气时才有）
 * - 节日名称：端午（当天为节日时才有；节气日优先展示节气，不重复显示节日）
 */
object ChineseDateHelper {

    private val GAN        = arrayOf("甲","乙","丙","丁","戊","己","庚","辛","壬","癸")
    private val ZHI        = arrayOf("子","丑","寅","卯","辰","巳","午","未","申","酉","戌","亥")
    private val SHENG_XIAO = arrayOf("鼠","牛","虎","兔","龙","蛇","马","羊","猴","鸡","狗","猪")

    private val LUNAR_MONTHS = arrayOf("正","二","三","四","五","六","七","八","九","十","十一","腊")

    private val LUNAR_DAYS = arrayOf(
        "","初一","初二","初三","初四","初五","初六","初七","初八","初九","初十",
        "十一","十二","十三","十四","十五","十六","十七","十八","十九","二十",
        "廿一","廿二","廿三","廿四","廿五","廿六","廿七","廿八","廿九","三十"
    )

    /** 二十四节气精确日期表（2024–2028） */
    private val SOLAR_TERMS: Map<LocalDate, String> = buildMap {
        // ── 2024 ──────────────────────────────────────
        put(LocalDate.of(2024, 1,  6), "小寒"); put(LocalDate.of(2024, 1, 20), "大寒")
        put(LocalDate.of(2024, 2,  4), "立春"); put(LocalDate.of(2024, 2, 19), "雨水")
        put(LocalDate.of(2024, 3,  5), "惊蛰"); put(LocalDate.of(2024, 3, 20), "春分")
        put(LocalDate.of(2024, 4,  4), "清明"); put(LocalDate.of(2024, 4, 19), "谷雨")
        put(LocalDate.of(2024, 5,  5), "立夏"); put(LocalDate.of(2024, 5, 20), "小满")
        put(LocalDate.of(2024, 6,  5), "芒种"); put(LocalDate.of(2024, 6, 21), "夏至")
        put(LocalDate.of(2024, 7,  6), "小暑"); put(LocalDate.of(2024, 7, 22), "大暑")
        put(LocalDate.of(2024, 8,  7), "立秋"); put(LocalDate.of(2024, 8, 22), "处暑")
        put(LocalDate.of(2024, 9,  7), "白露"); put(LocalDate.of(2024, 9, 22), "秋分")
        put(LocalDate.of(2024,10,  8), "寒露"); put(LocalDate.of(2024,10, 23), "霜降")
        put(LocalDate.of(2024,11,  7), "立冬"); put(LocalDate.of(2024,11, 22), "小雪")
        put(LocalDate.of(2024,12,  6), "大雪"); put(LocalDate.of(2024,12, 21), "冬至")
        // ── 2025 ──────────────────────────────────────
        put(LocalDate.of(2025, 1,  5), "小寒"); put(LocalDate.of(2025, 1, 20), "大寒")
        put(LocalDate.of(2025, 2,  3), "立春"); put(LocalDate.of(2025, 2, 18), "雨水")
        put(LocalDate.of(2025, 3,  5), "惊蛰"); put(LocalDate.of(2025, 3, 20), "春分")
        put(LocalDate.of(2025, 4,  4), "清明"); put(LocalDate.of(2025, 4, 20), "谷雨")
        put(LocalDate.of(2025, 5,  5), "立夏"); put(LocalDate.of(2025, 5, 21), "小满")
        put(LocalDate.of(2025, 6,  5), "芒种"); put(LocalDate.of(2025, 6, 21), "夏至")
        put(LocalDate.of(2025, 7,  7), "小暑"); put(LocalDate.of(2025, 7, 22), "大暑")
        put(LocalDate.of(2025, 8,  7), "立秋"); put(LocalDate.of(2025, 8, 23), "处暑")
        put(LocalDate.of(2025, 9,  7), "白露"); put(LocalDate.of(2025, 9, 23), "秋分")
        put(LocalDate.of(2025,10,  8), "寒露"); put(LocalDate.of(2025,10, 23), "霜降")
        put(LocalDate.of(2025,11,  7), "立冬"); put(LocalDate.of(2025,11, 22), "小雪")
        put(LocalDate.of(2025,12,  7), "大雪"); put(LocalDate.of(2025,12, 22), "冬至")
        // ── 2026 ──────────────────────────────────────
        put(LocalDate.of(2026, 1,  5), "小寒"); put(LocalDate.of(2026, 1, 20), "大寒")
        put(LocalDate.of(2026, 2,  4), "立春"); put(LocalDate.of(2026, 2, 19), "雨水")
        put(LocalDate.of(2026, 3,  6), "惊蛰"); put(LocalDate.of(2026, 3, 20), "春分")
        put(LocalDate.of(2026, 4,  5), "清明"); put(LocalDate.of(2026, 4, 20), "谷雨")
        put(LocalDate.of(2026, 5,  5), "立夏"); put(LocalDate.of(2026, 5, 21), "小满")
        put(LocalDate.of(2026, 6,  6), "芒种"); put(LocalDate.of(2026, 6, 21), "夏至")
        put(LocalDate.of(2026, 7,  7), "小暑"); put(LocalDate.of(2026, 7, 23), "大暑")
        put(LocalDate.of(2026, 8,  7), "立秋"); put(LocalDate.of(2026, 8, 23), "处暑")
        put(LocalDate.of(2026, 9,  8), "白露"); put(LocalDate.of(2026, 9, 23), "秋分")
        put(LocalDate.of(2026,10,  8), "寒露"); put(LocalDate.of(2026,10, 23), "霜降")
        put(LocalDate.of(2026,11,  7), "立冬"); put(LocalDate.of(2026,11, 22), "小雪")
        put(LocalDate.of(2026,12,  7), "大雪"); put(LocalDate.of(2026,12, 22), "冬至")
        // ── 2027 ──────────────────────────────────────
        put(LocalDate.of(2027, 1,  6), "小寒"); put(LocalDate.of(2027, 1, 20), "大寒")
        put(LocalDate.of(2027, 2,  3), "立春"); put(LocalDate.of(2027, 2, 18), "雨水")
        put(LocalDate.of(2027, 3,  6), "惊蛰"); put(LocalDate.of(2027, 3, 21), "春分")
        put(LocalDate.of(2027, 4,  5), "清明"); put(LocalDate.of(2027, 4, 20), "谷雨")
        put(LocalDate.of(2027, 5,  6), "立夏"); put(LocalDate.of(2027, 5, 21), "小满")
        put(LocalDate.of(2027, 6,  6), "芒种"); put(LocalDate.of(2027, 6, 21), "夏至")
        put(LocalDate.of(2027, 7,  7), "小暑"); put(LocalDate.of(2027, 7, 23), "大暑")
        put(LocalDate.of(2027, 8,  8), "立秋"); put(LocalDate.of(2027, 8, 23), "处暑")
        put(LocalDate.of(2027, 9,  8), "白露"); put(LocalDate.of(2027, 9, 23), "秋分")
        put(LocalDate.of(2027,10,  8), "寒露"); put(LocalDate.of(2027,10, 24), "霜降")
        put(LocalDate.of(2027,11,  7), "立冬"); put(LocalDate.of(2027,11, 22), "小雪")
        put(LocalDate.of(2027,12,  7), "大雪"); put(LocalDate.of(2027,12, 22), "冬至")
        // ── 2028 ──────────────────────────────────────
        put(LocalDate.of(2028, 1,  6), "小寒"); put(LocalDate.of(2028, 1, 21), "大寒")
        put(LocalDate.of(2028, 2,  4), "立春"); put(LocalDate.of(2028, 2, 19), "雨水")
        put(LocalDate.of(2028, 3,  5), "惊蛰"); put(LocalDate.of(2028, 3, 20), "春分")
        put(LocalDate.of(2028, 4,  4), "清明"); put(LocalDate.of(2028, 4, 19), "谷雨")
        put(LocalDate.of(2028, 5,  5), "立夏"); put(LocalDate.of(2028, 5, 20), "小满")
        put(LocalDate.of(2028, 6,  5), "芒种"); put(LocalDate.of(2028, 6, 21), "夏至")
        put(LocalDate.of(2028, 7,  6), "小暑"); put(LocalDate.of(2028, 7, 22), "大暑")
        put(LocalDate.of(2028, 8,  7), "立秋"); put(LocalDate.of(2028, 8, 22), "处暑")
        put(LocalDate.of(2028, 9,  7), "白露"); put(LocalDate.of(2028, 9, 22), "秋分")
        put(LocalDate.of(2028,10,  7), "寒露"); put(LocalDate.of(2028,10, 22), "霜降")
        put(LocalDate.of(2028,11,  7), "立冬"); put(LocalDate.of(2028,11, 21), "小雪")
        put(LocalDate.of(2028,12,  6), "大雪"); put(LocalDate.of(2028,12, 21), "冬至")
    }

    /** 公历固定节日：(月, 日) → 名称 */
    private val SOLAR_FIXED: Map<Pair<Int, Int>, String> = mapOf(
        Pair(1,  1)  to "元旦",
        Pair(2,  14) to "情人节",
        Pair(3,  8)  to "妇女节",
        Pair(3,  12) to "植树节",
        Pair(5,  1)  to "劳动节",
        Pair(5,  4)  to "青年节",
        Pair(6,  1)  to "儿童节",
        Pair(7,  1)  to "建党",
        Pair(8,  1)  to "建军",
        Pair(9,  10) to "教师节",
        Pair(10, 1)  to "国庆",
        Pair(10, 31) to "万圣节",
        Pair(12, 24) to "平安夜",
        Pair(12, 25) to "圣诞"
    )

    /** 农历固定节日：(农历月 1-based, 日) → 名称 */
    private val LUNAR_FIXED: Map<Pair<Int, Int>, String> = mapOf(
        Pair(1,  1)  to "春节",
        Pair(1,  15) to "元宵",
        Pair(2,  2)  to "龙抬头",
        Pair(3,  3)  to "上巳",
        Pair(5,  5)  to "端午",
        Pair(7,  7)  to "七夕",
        Pair(7,  15) to "中元",
        Pair(8,  15) to "中秋",
        Pair(9,  9)  to "重阳",
        Pair(10, 1)  to "寒衣",
        Pair(12, 8)  to "腊八",
        Pair(12, 23) to "小年"
    )

    data class ChineseDateInfo(
        /** 农历文字，如"乙巳蛇年二月初八" */
        val lunarText: String,
        /** 节气名，如"清明"；当天非节气则为 null */
        val solarTerm: String?,
        /** 节日名，如"端午"；节气日当天为 null */
        val festival: String?
    ) {
        /**
         * 副标题文字：节气优先；若当天既有节气又有节日，只展示节气。
         * 普通日期返回 null。
         */
        val secondaryText: String? get() = solarTerm ?: festival
    }

    /** 获取今天的农历信息 */
    fun getToday(): ChineseDateInfo = getDate(LocalDate.now())

    /** 获取指定日期的农历信息 */
    fun getDate(date: LocalDate): ChineseDateInfo {
        val cal = ChineseCalendar()
        cal.time = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant())

        val cycleYear   = cal.get(ChineseCalendar.YEAR)          // 1–60，干支纪年
        val lunarMonth0 = cal.get(ChineseCalendar.MONTH)         // 0-based（0 = 正月）
        val lunarDay    = cal.get(ChineseCalendar.DAY_OF_MONTH)  // 1–30
        val isLeap      = cal.get(ChineseCalendar.IS_LEAP_MONTH) == 1
        val lunarMonth  = lunarMonth0 + 1                        // 1-based

        val ganIndex   = (cycleYear - 1) % 10
        val zhiIndex   = (cycleYear - 1) % 12
        val leapPrefix = if (isLeap) "闰" else ""

        val lunarText = "${GAN[ganIndex]}${ZHI[zhiIndex]}${SHENG_XIAO[zhiIndex]}年" +
                "$leapPrefix${LUNAR_MONTHS[lunarMonth0]}月${LUNAR_DAYS[lunarDay]}"

        // ── 节气 ────────────────────────────────────────────────────────────────
        val solarTerm = SOLAR_TERMS[date]

        // ── 节日（节气日不显示节日）─────────────────────────────────────────────
        val festival = if (solarTerm != null) null else {
            // 除夕：明天是农历正月初一（非闰月）
            val isNewYearEve = run {
                val nextCal = ChineseCalendar()
                nextCal.time = Date.from(
                    date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                )
                nextCal.get(ChineseCalendar.MONTH) == 0 &&
                        nextCal.get(ChineseCalendar.DAY_OF_MONTH) == 1 &&
                        nextCal.get(ChineseCalendar.IS_LEAP_MONTH) == 0
            }
            when {
                isNewYearEve -> "除夕"
                LUNAR_FIXED.containsKey(Pair(lunarMonth, lunarDay)) ->
                    LUNAR_FIXED[Pair(lunarMonth, lunarDay)]
                SOLAR_FIXED.containsKey(Pair(date.monthValue, date.dayOfMonth)) ->
                    SOLAR_FIXED[Pair(date.monthValue, date.dayOfMonth)]
                else -> getFloatingFestival(date)
            }
        }

        return ChineseDateInfo(lunarText = lunarText, solarTerm = solarTerm, festival = festival)
    }

    /** 浮动节日：母亲节（5月第2个周日）、父亲节（6月第3个周日）、感恩节（11月第4个周四） */
    private fun getFloatingFestival(date: LocalDate): String? {
        val month = date.monthValue
        val dow   = date.dayOfWeek.value  // 1=周一 … 7=周日
        val dom   = date.dayOfMonth

        // 母亲节：5月第2个周日
        if (month == 5 && dow == 7) {
            val first1 = LocalDate.of(date.year, 5, 1).dayOfWeek.value
            val firstSun = (7 - first1) % 7 + 1
            if (dom == firstSun + 7) return "母亲节"
        }
        // 父亲节：6月第3个周日
        if (month == 6 && dow == 7) {
            val first1 = LocalDate.of(date.year, 6, 1).dayOfWeek.value
            val firstSun = (7 - first1) % 7 + 1
            if (dom == firstSun + 14) return "父亲节"
        }
        // 感恩节：11月第4个周四
        if (month == 11 && dow == 4) {
            val first1 = LocalDate.of(date.year, 11, 1).dayOfWeek.value
            val firstThu = (4 - first1 + 7) % 7 + 1
            if (dom == firstThu + 21) return "感恩节"
        }
        return null
    }
}
