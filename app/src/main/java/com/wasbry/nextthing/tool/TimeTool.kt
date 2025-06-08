package com.wasbry.nextthing.tool

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

object TimeTool {
    /** 获取指定日期所在周的开始日期（周一） */
    @RequiresApi(Build.VERSION_CODES.O)
    fun getStartOfWeek(date: LocalDate): LocalDate {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }

    /** 获取指定日期所在周的结束日期（周日） */
    @RequiresApi(Build.VERSION_CODES.O)
    fun getEndOfWeek(date: LocalDate): LocalDate {
        return date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
    }

    /**
     * 获取指定日期所在月的第一天（当月1号）
     * @param date 任意日期（将用于确定月份）
     * @return 当月第一天的LocalDate对象
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun getStartOfMonth(date: LocalDate): LocalDate {
        return date.with(TemporalAdjusters.firstDayOfMonth())
    }

    /**
     * 获取指定日期所在月的最后一天（当月最后一天）
     * @param date 任意日期（将用于确定月份）
     * @return 当月最后一天的LocalDate对象
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun getEndOfMonth(date: LocalDate): LocalDate {
        return date.with(TemporalAdjusters.lastDayOfMonth())
    }
}