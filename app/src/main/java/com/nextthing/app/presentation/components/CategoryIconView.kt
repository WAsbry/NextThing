package com.nextthing.app.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * 预置图标：drawable 资源名 → 显示名称
 */
data class PresetIcon(
    val drawableName: String,
    val label: String
)

val PRESET_ICONS = listOf(
    PresetIcon("life", "生活"),
    PresetIcon("work", "工作"),
    PresetIcon("study", "学习"),
    PresetIcon("sports", "运动"),
    PresetIcon("entertainment", "娱乐"),
    PresetIcon("finance", "财务"),
    PresetIcon("social", "社交"),
    PresetIcon("travel", "出行"),
    PresetIcon("family", "家庭"),
    PresetIcon("miscellaneous", "杂项"),
    PresetIcon("stock", "股票")
)

/**
 * 根据 drawable 资源名获取资源 ID
 */
fun getDrawableResId(context: android.content.Context, drawableName: String): Int {
    return context.resources.getIdentifier(drawableName, "drawable", context.packageName)
}

/**
 * 分类图标视图：支持三种形式
 * - "drawable:xxx" → 预置 drawable 图标
 * - "content://" 或 "file://" 开头 → 相册图片 (Coil AsyncImage)
 * - 其他字符串 → emoji Text 渲染
 */
@Composable
fun CategoryIconView(
    icon: String,
    size: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    when {
        icon.startsWith("drawable:") -> {
            val drawableName = icon.removePrefix("drawable:")
            val context = LocalContext.current
            val resId = getDrawableResId(context, drawableName)
            if (resId != 0) {
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = modifier.size(size)
                )
            } else {
                Text(
                    text = "🏷️",
                    fontSize = (size.value * 0.75f).sp,
                    modifier = modifier
                )
            }
        }
        icon.startsWith("content://") || icon.startsWith("file://") -> {
            AsyncImage(
                model = icon,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(4.dp))
            )
        }
        else -> {
            Text(
                text = icon.ifBlank { "🏷️" },
                fontSize = (size.value * 0.75f).sp,
                modifier = modifier
            )
        }
    }
}
