package com.example.nextthingb1.domain.model

/**
 * 重复频次类型
 */
enum class RepeatFrequencyType {
    NONE,        // 不重复
    DAILY,       // 每天
    WEEKLY,      // 每周（自定义星期）
    MONTHLY      // 每月（自定义日期）
}

/**
 * 重复频次数据类
 */
data class RepeatFrequency(
    val type: RepeatFrequencyType = RepeatFrequencyType.NONE,
    val weekdays: Set<Int> = emptySet(),  // 星期选择 (1=周一, 2=周二, ..., 7=周日)
    val monthDays: Set<Int> = emptySet()  // 月份日期选择 (1-31)
) {
    /**
     * 获取显示文本
     */
    fun getDisplayText(): String {
        return when (type) {
            RepeatFrequencyType.NONE -> "单次任务"
            RepeatFrequencyType.DAILY -> "每日任务"
            RepeatFrequencyType.WEEKLY -> {
                if (weekdays.isEmpty()) {
                    "自定义（每周）"
                } else {
                    val weekdayNames = weekdays.sorted().map { dayOfWeek ->
                        when (dayOfWeek) {
                            1 -> "一"
                            2 -> "二"
                            3 -> "三"
                            4 -> "四"
                            5 -> "五"
                            6 -> "六"
                            7 -> "日"
                            else -> ""
                        }
                    }
                    "每周${weekdayNames.joinToString("、")}"
                }
            }
            RepeatFrequencyType.MONTHLY -> {
                if (monthDays.isEmpty()) {
                    "自定义（每月）"
                } else {
                    val sortedDays = monthDays.sorted()
                    "每月${sortedDays.joinToString("、") { "${it}日" }}"
                }
            }
        }
    }

    /**
     * 检查是否有效配置
     */
    fun isValid(): Boolean {
        return when (type) {
            RepeatFrequencyType.NONE, RepeatFrequencyType.DAILY -> true
            RepeatFrequencyType.WEEKLY -> weekdays.isNotEmpty()
            RepeatFrequencyType.MONTHLY -> monthDays.isNotEmpty()
        }
    }
}

/**
 * 星期数据类
 */
data class WeekdayItem(
    val dayOfWeek: Int,     // 1-7 (周一到周日)
    val displayName: String, // "一", "二", ..., "日"
    val isSelected: Boolean = false
) {
    companion object {
        fun createWeekdays(selectedDays: Set<Int> = emptySet()): List<WeekdayItem> {
            return (1..7).map { day ->
                val displayName = when (day) {
                    1 -> "一"
                    2 -> "二"
                    3 -> "三"
                    4 -> "四"
                    5 -> "五"
                    6 -> "六"
                    7 -> "日"
                    else -> ""
                }
                WeekdayItem(
                    dayOfWeek = day,
                    displayName = displayName,
                    isSelected = selectedDays.contains(day)
                )
            }
        }
    }
}

/**
 * 月份日期数据类
 */
data class MonthDayItem(
    val day: Int,           // 1-31
    val isSelected: Boolean = false
) {
    companion object {
        fun createMonthDays(selectedDays: Set<Int> = emptySet()): List<MonthDayItem> {
            return (1..31).map { day ->
                MonthDayItem(
                    day = day,
                    isSelected = selectedDays.contains(day)
                )
            }
        }
    }
}