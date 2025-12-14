package com.example.nextthingb1.domain.model

import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * 地理围栏地点月度统计历史数据（领域模型）
 */
data class GeofenceLocationStatisticsHistory(
    val id: String,
    val geofenceLocationId: String,
    val month: String, // 格式：YYYY-MM
    val checkCount: Int,
    val hitCount: Int,
    val hitRate: Float,
    val createdAt: LocalDateTime
) {
    /**
     * 获取格式化的月份显示
     * @return "2024年3月"
     */
    fun getFormattedMonth(): String {
        return try {
            val yearMonth = YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyy-MM"))
            "${yearMonth.year}年${yearMonth.monthValue}月"
        } catch (e: Exception) {
            month
        }
    }

    /**
     * 获取格式化的命中率文本
     * @return "85%" 或 "无数据"
     */
    fun getFormattedHitRate(): String {
        return if (checkCount > 0) {
            "${(hitRate * 100).toInt()}%"
        } else {
            "无数据"
        }
    }

    /**
     * 获取月份的年份
     */
    fun getYear(): Int {
        return try {
            YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyy-MM")).year
        } catch (e: Exception) {
            0
        }
    }

    /**
     * 获取月份的月份值
     */
    fun getMonthValue(): Int {
        return try {
            YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyy-MM")).monthValue
        } catch (e: Exception) {
            0
        }
    }
}
