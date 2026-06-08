package com.nextthing.app.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class AppColors(
    val primary: Color,
    val primaryDark: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val accentPurple: Color,
    val bgPrimary: Color,
    val bgCard: Color,
    val bgSecondary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val border: Color,
    val swipeComplete: Color = Color(0xFF8B7FF7),
    val swipePostpone: Color = Color(0xFF6C5CE7),
    val swipeCancel: Color = Color(0xFF4A3BC1)
)

val LightAppColors = AppColors(
    primary = Color(0xFF6C5CE7),
    primaryDark = Color(0xFF5A4BD1),
    success = Color(0xFF4CAF50),
    warning = Color(0xFFFF9800),
    danger = Color(0xFFF44336),
    accentPurple = Color(0xFFAB47BC),
    bgPrimary = Color(0xFFF8F9FC),
    bgCard = Color(0xFFFFFFFF),
    bgSecondary = Color(0xFFF1F3F8),
    textPrimary = Color(0xFF2D3436),
    textSecondary = Color(0xFF636E72),
    textMuted = Color(0xFFB2BEC3),
    border = Color(0xFFE8ECF1)
)

val DarkAppColors = AppColors(
    primary = Color(0xFFA29BFE),
    primaryDark = Color(0xFF6C5CE7),
    success = Color(0xFF66BB6A),
    warning = Color(0xFFFFB74D),
    danger = Color(0xFFEF5350),
    accentPurple = Color(0xFFCE93D8),
    bgPrimary = Color(0xFF121212),
    bgCard = Color(0xFF1E1E1E),
    bgSecondary = Color(0xFF2C2C2C),
    textPrimary = Color(0xFFE0E0E0),
    textSecondary = Color(0xFF9E9E9E),
    textMuted = Color(0xFF616161),
    border = Color(0xFF333333)
)

val LocalAppColors = compositionLocalOf { LightAppColors }

// ── 天气主题配色（柔和淡色系） ───────────────────────────────────
// 公共文字色：所有天气主题统一使用中性文字，避免偏深
private val WTextPrimary   = Color(0xFF3A3A3A)
private val WTextSecondary = Color(0xFF7A7A7A)
private val WTextMuted     = Color(0xFFB0B0B0)
private val WSuccess       = Color(0xFF6DBF6D)
private val WWarning       = Color(0xFFFFAB40)
private val WDanger        = Color(0xFFFF6B6B)

/** ☀️ 晴天：暖杏黄，米白暖背景 */
val WeatherSunnyColors = AppColors(
    primary = Color(0xFFFFCC80),
    primaryDark = Color(0xFFFFB74D),
    success = WSuccess, warning = WWarning, danger = WDanger,
    accentPurple = Color(0xFFCE93D8),
    bgPrimary = Color(0xFFFFFBF2),
    bgCard = Color(0xFFFFFFFF),
    bgSecondary = Color(0xFFFFF3DC),
    textPrimary = WTextPrimary, textSecondary = WTextSecondary,
    textMuted = WTextMuted, border = Color(0xFFFFE0B2)
)

/** ☁️ 阴天：淡蓝灰，清冷白背景 */
val WeatherCloudyColors = AppColors(
    primary = Color(0xFFB0BEC5),
    primaryDark = Color(0xFF90A4AE),
    success = WSuccess, warning = WWarning, danger = WDanger,
    accentPurple = Color(0xFFCE93D8),
    bgPrimary = Color(0xFFF4F6F8),
    bgCard = Color(0xFFFFFFFF),
    bgSecondary = Color(0xFFECEFF1),
    textPrimary = WTextPrimary, textSecondary = WTextSecondary,
    textMuted = WTextMuted, border = Color(0xFFD6DDE3)
)

/** ⛅ 多云：浅天蓝，白蓝背景 */
val WeatherPartlyCloudyColors = AppColors(
    primary = Color(0xFF90CAF9),
    primaryDark = Color(0xFF64B5F6),
    success = WSuccess, warning = WWarning, danger = WDanger,
    accentPurple = Color(0xFFCE93D8),
    bgPrimary = Color(0xFFF5F9FF),
    bgCard = Color(0xFFFFFFFF),
    bgSecondary = Color(0xFFE8F2FF),
    textPrimary = WTextPrimary, textSecondary = WTextSecondary,
    textMuted = WTextMuted, border = Color(0xFFBBDEFB)
)

/** 🌧️ 雨天：浅钢蓝，淡雨背景 */
val WeatherRainyColors = AppColors(
    primary = Color(0xFF81C4F8),
    primaryDark = Color(0xFF5BAFF0),
    success = WSuccess, warning = WWarning, danger = WDanger,
    accentPurple = Color(0xFFCE93D8),
    bgPrimary = Color(0xFFF2F6FF),
    bgCard = Color(0xFFFFFFFF),
    bgSecondary = Color(0xFFE3EDFF),
    textPrimary = WTextPrimary, textSecondary = WTextSecondary,
    textMuted = WTextMuted, border = Color(0xFFBDD5F5)
)

/** ⛈️ 雷雨：淡丁香紫，薰衣草背景 */
val WeatherThunderstormColors = AppColors(
    primary = Color(0xFFCE93D8),
    primaryDark = Color(0xFFBA68C8),
    success = WSuccess, warning = WWarning, danger = WDanger,
    accentPurple = Color(0xFFE1BEE7),
    bgPrimary = Color(0xFFF8F3FF),
    bgCard = Color(0xFFFFFFFF),
    bgSecondary = Color(0xFFF0E6FA),
    textPrimary = WTextPrimary, textSecondary = WTextSecondary,
    textMuted = WTextMuted, border = Color(0xFFE1BEE7)
)

/** ❄️ 雪天：冰青，极浅冰蓝背景 */
val WeatherSnowyColors = AppColors(
    primary = Color(0xFF80DEEA),
    primaryDark = Color(0xFF4DD0E1),
    success = WSuccess, warning = WWarning, danger = WDanger,
    accentPurple = Color(0xFFCE93D8),
    bgPrimary = Color(0xFFF3FBFF),
    bgCard = Color(0xFFFFFFFF),
    bgSecondary = Color(0xFFE5F6FB),
    textPrimary = WTextPrimary, textSecondary = WTextSecondary,
    textMuted = WTextMuted, border = Color(0xFFB3E5FC)
)

/** 🌫️ 雾天：暖米灰，温润米白背景 */
val WeatherFoggyColors = AppColors(
    primary = Color(0xFFCBC8C2),
    primaryDark = Color(0xFFB0ADA7),
    success = WSuccess, warning = WWarning, danger = WDanger,
    accentPurple = Color(0xFFCE93D8),
    bgPrimary = Color(0xFFF7F5F2),
    bgCard = Color(0xFFFFFFFF),
    bgSecondary = Color(0xFFEFECE6),
    textPrimary = WTextPrimary, textSecondary = WTextSecondary,
    textMuted = WTextMuted, border = Color(0xFFE0DDD7)
)

/** 💨 风天：薄荷青，清新白绿背景 */
val WeatherWindyColors = AppColors(
    primary = Color(0xFF80CBC4),
    primaryDark = Color(0xFF4DB6AC),
    success = WSuccess, warning = WWarning, danger = WDanger,
    accentPurple = Color(0xFFCE93D8),
    bgPrimary = Color(0xFFF3FDFB),
    bgCard = Color(0xFFFFFFFF),
    bgSecondary = Color(0xFFE3F8F5),
    textPrimary = WTextPrimary, textSecondary = WTextSecondary,
    textMuted = WTextMuted, border = Color(0xFFB2DFDB)
)

/** 默认/未知天气：同浅色主题 */
val WeatherUnknownColors = LightAppColors

// ── 自定义主色覆盖 ─────────────────────────────────────────────
/** 用自定义 primary 覆盖对应天气配色的主色 */
fun weatherAppColorsWithCustom(base: AppColors, customPrimaryArgb: Long?): AppColors {
    if (customPrimaryArgb == null) return base
    val c = Color(customPrimaryArgb)
    return base.copy(primary = c, primaryDark = c)
}
