package com.wasbry.nextthing.ui.componet.mine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.wasbry.nextthing.database.model.PersonalTime

@Composable
fun PersonalTimeItem(time: PersonalTime) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_report_image), // 替换为实际的 Icon 资源
                contentDescription = null,
                modifier = Modifier.width(24.dp)
            )
            Text(text = time.timeDescription)
            Text(text = time.startTime)
            Text(text = time.endTime)
            Text(text = time.timeValue.toString())
            Text(text = time.selfControlDegree.toString())
        }
    }
}