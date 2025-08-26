package com.example.nextthingb1.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 根据设计图定义的颜色
val Primary = Color(0xFF4FC3F7)
val PrimaryDark = Color(0xFF29B6F6)
val Success = Color(0xFF4CAF50)
val Warning = Color(0xFFFF9800)
val Danger = Color(0xFFF44336)
val BgPrimary = Color(0xFFF5F7FA)
val BgCard = Color(0xFFFFFFFF)
val BgSecondary = Color(0xFFECF0F1)
val TextPrimary = Color(0xFF2C3E50)
val TextSecondary = Color(0xFF7F8C8D)
val TextMuted = Color(0xFFBDC3C7)
val Border = Color(0xFFE8EDF3)

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    secondary = PrimaryDark,
    tertiary = Success,
    background = Color(0xFF1A1A1A),
    surface = Color(0xFF2D2D2D),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    secondary = PrimaryDark,
    tertiary = Success,
    background = BgPrimary,
    surface = BgCard,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = Danger,
    onError = Color.White
)

@Composable
fun NextThingB1Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
} 