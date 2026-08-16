package com.nextthing.app.data.service

import com.google.gson.Gson
import com.nextthing.app.domain.model.Task
import com.nextthing.app.domain.service.AIScheduleAdvisor
import com.nextthing.app.domain.service.ScheduleAdvice
import timber.log.Timber
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIScheduleAdvisorService @Inject constructor(
    private val aiCompletionClient: AICompletionClient,
    private val gson: Gson
) : AIScheduleAdvisor {

    private val timeFmt = DateTimeFormatter.ofPattern("MM/dd HH:mm")

    override suspend fun analyzeSchedule(
        overdueTasks: List<Task>,
        todayTasks: List<Task>,
        urgentTasks: List<Task>
    ): Result<ScheduleAdvice> {
        return try {
            val reply = aiCompletionClient.complete(buildPrompt(overdueTasks, todayTasks, urgentTasks)).getOrThrow()
            val obj = AIJsonHelper.parseAIJson(gson, reply)
            Result.success(
                ScheduleAdvice(
                    summary = obj.get("summary")?.asString ?: "",
                    suggestions = obj.getAsJsonArray("suggestions")
                        ?.mapNotNull { runCatching { it.asString }.getOrNull() }
                        ?: emptyList()
                )
            )
        } catch (e: Exception) {
            Timber.tag("AI").e(e, "Schedule analysis failed")
            Result.failure(Exception("AI schedule analysis failed: ${e.message}"))
        }
    }

    private fun buildPrompt(
        overdueTasks: List<Task>,
        todayTasks: List<Task>,
        urgentTasks: List<Task>
    ): String {
        return buildString {
            appendLine("Analyze the user's task schedule in Chinese.")
            appendLine("Look for time conflicts, priority imbalance, overload, and overdue accumulation.")
            appendLine("Return only JSON: {\"summary\":\"...\",\"suggestions\":[\"...\"]}")
            appendLine()
            appendTaskSection("Overdue tasks", overdueTasks)
            appendTaskSection("Today tasks", todayTasks)
            appendTaskSection("Urgent tasks", urgentTasks)
        }
    }

    private fun StringBuilder.appendTaskSection(title: String, tasks: List<Task>) {
        if (tasks.isEmpty()) return
        appendLine()
        appendLine("=== $title ===")
        tasks.forEach { task ->
            appendLine("- ${task.title} | status=${task.status.name} | category=${task.category.name} | due=${task.dueDate?.format(timeFmt) ?: "none"}")
        }
    }
}
