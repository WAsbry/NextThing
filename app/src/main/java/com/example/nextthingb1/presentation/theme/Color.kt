package com.example.nextthingb1.presentation.theme

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
    val bgPrimary: Color,
    val bgCard: Color,
    val bgSecondary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val border: Color
)

val LightAppColors = AppColors(
    primary = Color(0xFF4FC3F7),
    primaryDark = Color(0xFF29B6F6),
    success = Color(0xFF4CAF50),
    warning = Color(0xFFFF9800),
    danger = Color(0xFFF44336),
    bgPrimary = Color(0xFFF5F7FA),
    bgCard = Color(0xFFFFFFFF),
    bgSecondary = Color(0xFFECF0F1),
    textPrimary = Color(0xFF2C3E50),
    textSecondary = Color(0xFF7F8C8D),
    textMuted = Color(0xFFBDC3C7),
    border = Color(0xFFE8EDF3)
)

val DarkAppColors = AppColors(
    primary = Color(0xFF4FC3F7),
    primaryDark = Color(0xFF29B6F6),
    success = Color(0xFF66BB6A),
    warning = Color(0xFFFFB74D),
    danger = Color(0xFFEF5350),
    bgPrimary = Color(0xFF121212),
    bgCard = Color(0xFF1E1E1E),
    bgSecondary = Color(0xFF2C2C2C),
    textPrimary = Color(0xFFE0E0E0),
    textSecondary = Color(0xFF9E9E9E),
    textMuted = Color(0xFF616161),
    border = Color(0xFF333333)
)

val LocalAppColors = compositionLocalOf { LightAppColors }
