package com.wasbry.nextthing.ui.componet.mine

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wasbry.nextthing.ui.componet.mine.PersonalTimeItem
import com.wasbry.nextthing.viewmodel.personalTime.PersonalTimeViewModel
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.wasbry.nextthing.database.model.PersonalTime
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 个人时间管理控件
 * */
@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun PersonalTimeControl(viewModel: PersonalTimeViewModel = viewModel()) {
    var showDialog by remember { mutableStateOf(false) }
    var timeDescription by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var selfControlDegree by remember { mutableStateOf(1) }
    var timeValue by remember { mutableStateOf(1) }
    var iconPath by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "个人时间管理",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Row {
                IconButton(onClick = { showDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "添加"
                    )
                }
                IconButton(onClick = {
                    viewModel.toggleExpansion()
                }) {
                    Icon(
                        imageVector = if (viewModel.isExpanded.value) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (viewModel.isExpanded.value) "折叠" else "展开"
                    )
                }
            }
        }

        if (showDialog) {
            AddPersonalTimeDialog(
                onDismiss = { showDialog = false },
                onAdd = {
                    val newTime = PersonalTime(
                        timeDescription = timeDescription,
                        startTime = startTime,
                        endTime = endTime,
                        selfControlDegree = selfControlDegree,
                        timeValue = timeValue,
                        iconPath = iconPath
                    )
                    viewModel.insertPersonalTime(newTime)
                    showDialog = false
                },
                timeDescription = timeDescription,
                onTimeDescriptionChange = { timeDescription = it },
                startTime = startTime,
                onStartTimeChange = { startTime = it },
                endTime = endTime,
                onEndTimeChange = { endTime = it },
                selfControlDegree = selfControlDegree,
                onSelfControlDegreeChange = { selfControlDegree = it },
                timeValue = timeValue,
                onTimeValueChange = { timeValue = it },
                iconPath = iconPath,
                onIconPathChange = { iconPath = it }
            )
        }

        val isExpanded by viewModel.isExpanded
        if (isExpanded) {
            val personalTimes by viewModel.personalTimes
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(personalTimes) { time ->
                    PersonalTimeItem(time)
                }
            }
        }
    }
}