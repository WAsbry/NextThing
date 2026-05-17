package com.nextthing.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.nextthing.app.MainActivity
import com.nextthing.app.data.local.database.TaskDatabase
import com.nextthing.app.data.local.entity.TaskWithCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TaskListWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val tasks = withContext(Dispatchers.IO) {
            try {
                val db = TaskDatabase.getDatabase(context)
                db.taskDao().getTodayTasksAsList()
            } catch (_: Exception) {
                emptyList()
            }
        }

        val intent = Intent(context, MainActivity::class.java)

        provideContent {
            WidgetContent(tasks, intent)
        }
    }

    @Composable
    private fun WidgetContent(tasks: List<TaskWithCategory>, intent: Intent) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(12.dp)
                .clickable(actionStartActivity(intent)),
            verticalAlignment = Alignment.Top
        ) {
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Text(
                    text = "今日待办",
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                    modifier = GlanceModifier.defaultWeight()
                )
                if (tasks.isNotEmpty()) {
                    Text(text = "${tasks.size} 项", style = TextStyle(fontSize = 13.sp))
                }
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            if (tasks.isEmpty()) {
                Box(
                    modifier = GlanceModifier.fillMaxWidth().height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "今天没有待办任务", style = TextStyle(fontSize = 14.sp))
                }
            } else {
                tasks.take(5).forEach { taskWithCategory ->
                    val task = taskWithCategory.task
                    val timeStr = task.dueDate?.let {
                        "${it.hour}:${it.minute.toString().padStart(2, '0')}"
                    } ?: ""

                    Row(
                        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text(text = "• ", style = TextStyle(fontSize = 14.sp))
                        Text(
                            text = task.title,
                            style = TextStyle(fontSize = 14.sp),
                            modifier = GlanceModifier.defaultWeight()
                        )
                        if (timeStr.isNotEmpty()) {
                            Text(text = " $timeStr", style = TextStyle(fontSize = 11.sp))
                        }
                    }
                }
            }
        }
    }
}
