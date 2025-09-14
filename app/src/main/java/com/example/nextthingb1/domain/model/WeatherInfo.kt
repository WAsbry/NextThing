package com.example.nextthingb1.domain.model

import java.time.LocalDateTime

/**
 * 天气状况枚举
 */
enum class WeatherCondition(
    val displayName: String,
    val iconRes: String,
    val color: Long
) {
    SUNNY("晴", "☀️", 0xFFFFC107),
    CLOUDY("阴", "☁️", 0xFF9E9E9E),
    PARTLY_CLOUDY("多云", "⛅", 0xFFE0E0E0),
    RAINY("雨", "🌧️", 0xFF2196F3),
    THUNDERSTORM("雷雨", "⛈️", 0xFF3F51B5),
    SNOWY("雪", "❄️", 0xFFB3E5FC),
    FOGGY("雾", "🌫️", 0xFF78909C),
    WINDY("风", "💨", 0xFF607D8B),
    UNKNOWN("未知", "❓", 0xFF9E9E9E)
}

/**
 * 生活建议类型
 */
enum class LifeSuggestionType(
    val priority: Int, // 优先级，数字越小优先级越高
    val displayName: String
) {
    UMBRELLA(1, "带伞"),
    MASK(2, "戴口罩"),
    SUNSCREEN(3, "防晒"),
    CLOTHING(4, "穿衣")
}

/**
 * 生活建议
 */
data class LifeSuggestion(
    val type: LifeSuggestionType,
    val message: String,
    val isUrgent: Boolean = false
)

/**
 * 天气信息数据类
 */
data class WeatherInfo(
    val condition: WeatherCondition,
    val temperature: Int, // 当前温度，整数
    val temperatureMax: Int, // 最高温度
    val temperatureMin: Int, // 最低温度
    val humidity: Int, // 湿度百分比
    val windSpeed: Int, // 风速 km/h
    val pm25: Int, // PM2.5指数
    val uvIndex: Int, // 紫外线指数
    val suggestion: LifeSuggestion?, // 最重要的生活建议
    val updateTime: LocalDateTime,
    val locationName: String
) {
    /**
     * 获取最优先的生活建议
     */
    fun getPrioritySuggestion(): LifeSuggestion? {
        val suggestions = mutableListOf<LifeSuggestion>()
        
        // 降水优先级最高
        if (condition == WeatherCondition.RAINY || condition == WeatherCondition.THUNDERSTORM) {
            suggestions.add(LifeSuggestion(LifeSuggestionType.UMBRELLA, "带伞", true))
        }
        
        // PM2.5检查
        if (pm25 > 75) {
            suggestions.add(LifeSuggestion(LifeSuggestionType.MASK, "戴口罩", pm25 > 150))
        }
        
        // 紫外线检查
        if (uvIndex > 7) {
            suggestions.add(LifeSuggestion(LifeSuggestionType.SUNSCREEN, "防晒", uvIndex > 10))
        }
        
        // 穿衣建议
        if (temperature <= 10) {
            suggestions.add(LifeSuggestion(LifeSuggestionType.CLOTHING, "保暖", temperature <= 0))
        } else if (temperature >= 30) {
            suggestions.add(LifeSuggestion(LifeSuggestionType.CLOTHING, "防热", temperature >= 35))
        }
        
        // 返回优先级最高的建议
        return suggestions.minByOrNull { it.type.priority }
    }
}

/**
 * 天气API响应数据类
 */
data class WeatherResponse(
    val success: Boolean,
    val data: WeatherInfo?,
    val error: String?
) 