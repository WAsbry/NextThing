package com.wasbry.nextthing.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wasbry.nextthing.database.model.TodoTask
import com.wasbry.nextthing.viewmodel.TodoTaskViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun TaskListScreen(viewModel: TodoTaskViewModel) {
    val tasks by viewModel.allTodoTasks.collectAsStateWithLifecycle(initialValue = emptyList())
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "任务列表",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(tasks) { task ->
                TaskItem(task = task, context = context)
            }
        }
    }
}

@Composable
fun TaskItem(
    task: TodoTask,
    context: android.content.Context
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val dueDateFormatted = dateFormat.format(task.madeDate)

    Card(
        modifier = Modifier.fillMaxSize(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
//            containerColor = if (task.status == ) {
//                MaterialTheme.colorScheme.tertiaryContainer
//            } else {
//                MaterialTheme.colorScheme.surface
//            }
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = task.description,
                style = MaterialTheme.typography.titleMedium,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )

            Text(
                text = task.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
                overflow = TextOverflow.Ellipsis,
                maxLines = 2
            )

            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    text = "创建日期：$dueDateFormatted",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}