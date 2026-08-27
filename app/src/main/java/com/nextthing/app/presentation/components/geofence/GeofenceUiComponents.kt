package com.nextthing.app.presentation.components.geofence

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nextthing.app.R
import com.nextthing.app.domain.model.GeofenceLocation
import com.nextthing.app.presentation.theme.Primary
import com.nextthing.app.presentation.theme.TextMuted
import com.nextthing.app.presentation.theme.TextPrimary
import com.nextthing.app.presentation.theme.TextSecondary
import com.nextthing.app.presentation.theme.Warning

/**
 * 地理围栏地点在“任务绑定”和“地点管理”中的共享摘要。
 * 页面只负责提供前置选择控件、点击行为和尾部操作。
 */
@Composable
fun GeofenceLocationSummary(
    location: GeofenceLocation,
    defaultRadius: Int,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = location.locationInfo.locationName.ifEmpty { "未命名地点" },
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) Primary else TextPrimary,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (location.isFrequent) {
                Spacer(Modifier.width(6.dp))
                Icon(
                    painter = painterResource(R.drawable.icon_geofence_star),
                    contentDescription = "常用地点",
                    tint = Warning,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (location.locationInfo.address.isNotBlank()) {
            Text(
                text = location.locationInfo.address,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = buildString {
                append("半径 ${location.customRadius ?: defaultRadius} 米")
                if (location.usageCount > 0) append(" · 使用 ${location.usageCount} 次")
            },
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
    }
}

@Composable
fun GeofenceEmptyCopy(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "暂无地点",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = "添加地点后，可用于任务的到达和离开提醒",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}
