package com.wasbry.nextthing.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.unit.dp

/**
 * 自定义应用主题 - 基于Material Design 3规范
 * 采用牛奶质感的浅色设计方案
 */

// 1. 自定义颜色方案（浅色为主，牛奶质感）
private val LightColorScheme = lightColorScheme(
    // 主色调 - 柔和的蓝灰色，营造专业感与柔和度
    primary = Color(0xFF64748B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE2E8F0),
    onPrimaryContainer = Color(0xFF1E293B),

    // 辅助色调 - 温暖的米色，与牛奶主题呼应
    secondary = Color(0xFFD4A76A),
    onSecondary = Color(0xFF332200),
    secondaryContainer = Color(0xFFFBF1E6),
    onSecondaryContainer = Color(0xFF442C00),

    // 强调色 - 温暖的珊瑚色，用于突出关键操作
    tertiary = Color(0xFFF29E89),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEEBE6),
    onTertiaryContainer = Color(0xFF491D12),

    // 错误状态颜色
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),

    // 背景色 - 柔和的米白色，类似牛奶的底色
    background = Color(0xFFFEFAF5),
    onBackground = Color(0xFF2D2A27),

    // 表面色 - 各种组件的背景色，比背景色略深
    surface = Color(0xFFFBF7F0),
    onSurface = Color(0xFF2D2A27),

    // 表面变体 - 用于区分不同层次的表面
    surfaceVariant = Color(0xFFE5E0DB),
    onSurfaceVariant = Color(0xFF49454F),

    // 轮廓色 - 用于绘制边框和分隔线
    outline = Color(0xFF79747E),

    // 其他系统定义的颜色...
    inverseSurface = Color(0xFF2D2A27),
    inverseOnSurface = Color(0xFFFBF7F0),
    inversePrimary = Color(0xFFC8D3E6),
)

// 深色模式颜色配置 - 保持牛奶主题的深色变体
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFC8D3E6),
    onPrimary = Color(0xFF334155),
    primaryContainer = Color(0xFF4A5568),
    onPrimaryContainer = Color(0xFFE2E8F0),

    secondary = Color(0xFFE8C69D),
    onSecondary = Color(0xFF553D00),
    secondaryContainer = Color(0xFF7A5C29),
    onSecondaryContainer = Color(0xFFFBF1E6),

    tertiary = Color(0xFFF8C4B4),
    onTertiary = Color(0xFF662919),
    tertiaryContainer = Color(0xFFAD432D),
    onTertiaryContainer = Color(0xFFFEEBE6),

    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),

    // 深色背景 - 温暖的深灰色，保持牛奶主题的温暖感
    background = Color(0xFF2D2A27),
    onBackground = Color(0xFFE6E1DC),

    // 表面色 - 比背景略浅的灰色
    surface = Color(0xFF3B3835),
    onSurface = Color(0xFFE6E1DC),

    // 表面变体 - 用于区分不同层次的表面
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFC9C5CD),

    // 轮廓色 - 用于绘制边框和分隔线
    outline = Color(0xFF8F8F8F),

    // 其他系统定义的颜色...
    inverseSurface = Color(0xFFE6E1DC),
    inverseOnSurface = Color(0xFF4B4844),
    inversePrimary = Color(0xFF64748B),
)

// 2. 自定义字体方案（基于Material Design 3规范）
// 定义应用中所有文本元素的字体样式
private val AppTypography = Typography(
    // 超大标题 - 用于页面标题和重要内容
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.W400,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),

    // 大标题 - 用于页面标题和重要内容
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.W400,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),

    // 中标题 - 用于子标题和重要文本
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.W400,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),

    // 标题大号 - 用于卡片标题和小标题
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.W400,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),

    // 标题中号 - 用于列表项标题和小标题
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.W400,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),

    // 标题小号 - 用于卡片和组件内的小标题
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.W400,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),

    // 标题大号 - 用于卡片和组件内的主要文本
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.W400,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),

    // 标题中号 - 用于列表项和导航元素
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.W500,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),

    // 标题小号 - 用于辅助文本和标签
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.W500,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),

    // 正文字体 - 用于长文本和主要内容
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.W400,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),

    // 正文字体 - 默认文本样式
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),

    // 小正文字体 - 用于辅助文本和次要内容
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.W400,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),

    // 标签字体 - 用于按钮和标签
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.W500,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),

    // 中等标签字体 - 用于芯片和小型按钮
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.W500,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),

    // 小标签字体 - 用于图标标签和微交互
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.W500,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

// 3. 自定义形状方案（如卡片圆角）
// 定义应用中所有组件的边角形状
private val AppShapes = Shapes(
    // 小形状 - 用于小按钮、芯片和图标容器
    small = RoundedCornerShape(4.dp),

    // 中等形状 - 用于卡片、对话框和大多数组件
    medium = RoundedCornerShape(8.dp),

    // 大形状 - 用于底部导航栏、抽屉和全屏组件
    large = RoundedCornerShape(16.dp),

    // 超大形状 - 用于特殊组件和装饰元素
    extraLarge = RoundedCornerShape(24.dp)
)

// 4. 应用主题包装器 - 管理主题配置和应用
@Composable
fun NextThingTheme(
    // 是否启用动态颜色（仅支持Android 12+）
    dynamicColor: Boolean = true,

    // 是否使用深色模式（默认跟随系统设置）
    darkTheme: Boolean = isSystemInDarkTheme(),

    // 内容区域
    content: @Composable () -> Unit
) {
    // 根据配置选择颜色方案
    val colorScheme = when {
        // 动态颜色支持（Android 12+）
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // 自定义深色模式
        darkTheme -> DarkColorScheme
        // 自定义浅色模式
        else -> LightColorScheme
    }

    // 应用主题配置
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}