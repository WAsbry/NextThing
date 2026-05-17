package com.nextthing.app.presentation.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nextthing.app.domain.model.Task
import com.nextthing.app.domain.model.TaskStatus
import com.nextthing.app.presentation.theme.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel(),
    onNavigateToTaskDetail: (String) -> Unit = {},
    onBackPressed: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("日历") },
                navigationIcon = {
                    TextButton(onClick = onBackPressed) {
                        Text("< 返回", color = Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgPrimary)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BgPrimary)
        ) {
            MonthHeader(
                currentMonth = uiState.currentMonth,
                onPrevious = { viewModel.onMonthChanged(uiState.currentMonth.minusMonths(1)) },
                onNext = { viewModel.onMonthChanged(uiState.currentMonth.plusMonths(1)) }
            )

            WeekDayHeader()

            MonthGrid(
                currentMonth = uiState.currentMonth,
                selectedDate = uiState.selectedDate,
                datesWithTasks = uiState.datesWithTasks,
                onDateSelected = { viewModel.onDateSelected(it) }
            )

            HorizontalDivider(color = Border, thickness = 1.dp)

            TaskListSection(
                selectedDate = uiState.selectedDate,
                tasks = uiState.tasksForSelectedDate,
                onTaskClick = { onNavigateToTaskDetail(it) }
            )
        }
    }
}

@Composable
private fun MonthHeader(
    currentMonth: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextButton(onClick = onPrevious, contentPadding = PaddingValues(8.dp)) {
            Text("<", fontSize = 18.sp, color = Primary)
        }
        Text(
            text = "${currentMonth.year}年${currentMonth.monthValue}月",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        TextButton(onClick = onNext, contentPadding = PaddingValues(8.dp)) {
            Text(">", fontSize = 18.sp, color = Primary)
        }
    }
}

@Composable
private fun WeekDayHeader() {
    val weekDays = listOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
    )
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        weekDays.forEach { day ->
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day.getDisplayName(TextStyle.SHORT, Locale.CHINESE),
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun MonthGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    datesWithTasks: Set<LocalDate>,
    onDateSelected: (LocalDate) -> Unit
) {
    val firstDayOfMonth = currentMonth.atDay(1)
    val daysInMonth = currentMonth.lengthOfMonth()
    // Monday=0, Sunday=6
    val firstDayOffset = (firstDayOfMonth.dayOfWeek.value - 1) % 7
    val today = LocalDate.now()

    Column(modifier = Modifier.padding(horizontal = 4.dp)) {
        var dayCounter = 1 - firstDayOffset
        for (week in 0..5) {
            if (dayCounter > daysInMonth) break
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val day = dayCounter
                    val isValid = day in 1..daysInMonth
                    val date = if (isValid) currentMonth.atDay(day) else null
                    val isToday = date == today
                    val isSelected = date == selectedDate
                    val hasTasks = date != null && datesWithTasks.contains(date)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isSelected -> Primary
                                    isToday -> Primary.copy(alpha = 0.15f)
                                    else -> Color.Transparent
                                }
                            )
                            .clickable(enabled = isValid && date != null) {
                                date?.let { onDateSelected(it) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (isValid) {
                                Text(
                                    text = "$day",
                                    fontSize = 14.sp,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = when {
                                        isSelected -> Color.White
                                        isToday -> Primary
                                        else -> TextPrimary
                                    }
                                )
                                if (hasTasks && !isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 1.dp)
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(Primary)
                                    )
                                }
                            }
                        }
                    }
                    dayCounter++
                }
            }
        }
    }
}

@Composable
private fun TaskListSection(
    selectedDate: LocalDate,
    tasks: List<Task>,
    onTaskClick: (String) -> Unit
) {
    val dateLabel = "${selectedDate.monthValue}月${selectedDate.dayOfMonth}日"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$dateLabel 任务",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
        Text(
            text = "${tasks.size} 项",
            fontSize = 13.sp,
            color = TextSecondary
        )
    }

    if (tasks.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("当天没有任务", color = TextMuted, fontSize = 14.sp)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tasks, key = { it.id }) { task ->
                TaskCalendarItem(
                    task = task,
                    onClick = { onTaskClick(task.id) }
                )
            }
        }
    }
}

@Composable
private fun TaskCalendarItem(
    task: Task,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category color indicator
            Box(
                modifier = Modifier
                    .size(6.dp, 32.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        try { Color(android.graphics.Color.parseColor(task.category.colorHex)) }
                        catch (_: Exception) { Primary }
                    )
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = when (task.status) {
                        TaskStatus.COMPLETED -> TextMuted
                        TaskStatus.OVERDUE -> Color(0xFFE53935)
                        else -> TextPrimary
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (task.dueDate != null) {
                    val timeStr = "${task.dueDate.hour}:${task.dueDate.minute.toString().padStart(2, '0')}"
                    Text(
                        text = timeStr,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            // Status indicator
            when (task.status) {
                TaskStatus.COMPLETED -> Text("✓", color = Success, fontSize = 16.sp)
                TaskStatus.OVERDUE -> Text("!", color = Color(0xFFE53935), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                else -> {}
            }
        }
    }
}
