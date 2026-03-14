package com.example.nextthingb1.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.example.nextthingb1.domain.model.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4FC3F7),
    secondary = Color(0xFF29B6F6),
    tertiary = Color(0xFF66BB6A),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4FC3F7),
    secondary = Color(0xFF29B6F6),
    tertiary = Color(0xFF4CAF50),
    background = Color(0xFFF5F7FA),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF2C3E50),
    onSurface = Color(0xFF2C3E50),
    error = Color(0xFFF44336),
    onError = Color.White
)

// ── 向后兼容的 Composable getter（所有现有屏幕无需修改）──

val Primary: Color
    @Composable @ReadOnlyComposable get() = LocalAppColors.current.primary

val PrimaryDark: Color
    @Composable @ReadOnlyComposable get() = LocalAppColors.current.primaryDark

val Success: Color
    @Composable @ReadOnlyComposable get() = LocalAppColors.current.success

val Warning: Color
    @Composable @ReadOnlyComposable get() = LocalAppColors.current.warning

val Danger: Color
    @Composable @ReadOnlyComposable get() = LocalAppColors.current.danger

val BgPrimary: Color
    @Composable @ReadOnlyComposable get() = LocalAppColors.current.bgPrimary

val BgCard: Color
    @Composable @ReadOnlyComposable get() = LocalAppColors.current.bgCard

val BgSecondary: Color
    @Composable @ReadOnlyComposable get() = LocalAppColors.current.bgSecondary

val TextPrimary: Color
    @Composable @ReadOnlyComposable get() = LocalAppColors.current.textPrimary

val TextSecondary: Color
    @Composable @ReadOnlyComposable get() = LocalAppColors.current.textSecondary

val TextMuted: Color
    @Composable @ReadOnlyComposable get() = LocalAppColors.current.textMuted

val Border: Color
    @Composable @ReadOnlyComposable get() = LocalAppColors.current.border

@Composable
fun NextThingB1Theme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    val appColors = if (isDark) DarkAppColors else LightAppColors

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
