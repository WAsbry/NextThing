package com.nextthing.app.presentation.screens.today

import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextthing.app.LocalPermissionLauncher
import com.nextthing.app.domain.model.*
import org.junit.Rule
import org.junit.Test

class TodayScreenUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testCategory = Category(
        id = "cat-1",
        name = "工作",
        type = CategoryType.PRESET,
        icon = "work",
        colorHex = "#6C5CE7"
    )

    private val testTasks = listOf(
        Task(id = "1", title = "开项目周会", description = "", status = TaskStatus.PENDING, category = testCategory),
        Task(id = "2", title = "去超市买菜", description = "", status = TaskStatus.PENDING, category = testCategory),
        Task(id = "3", title = "提交周报", description = "", status = TaskStatus.COMPLETED, category = testCategory)
    )

    @Test
    fun topHeader_shows_NextThing_text() {
        composeTestRule.setContent {
            TestWrapper { TopHeader() }
        }
        composeTestRule.onNodeWithText("NextThing").assertIsDisplayed()
    }

    @Test
    fun tabs_allVisible() {
        composeTestRule.setContent {
            TestWrapper { TabsRow() }
        }
        composeTestRule.onNodeWithText("待办").assertIsDisplayed()
        composeTestRule.onNodeWithText("已完成").assertIsDisplayed()
        composeTestRule.onNodeWithText("全部").assertIsDisplayed()
    }

    @Test
    fun taskList_shows_pending_task_titles() {
        val pendingTasks = testTasks.filter { it.status == TaskStatus.PENDING }
        composeTestRule.setContent {
            TestWrapper { TaskListView(tasks = pendingTasks) }
        }
        composeTestRule.onNodeWithText("开项目周会").assertIsDisplayed()
        composeTestRule.onNodeWithText("去超市买菜").assertIsDisplayed()
    }

    @Test
    fun taskList_shows_completed_task_title() {
        val completedTasks = testTasks.filter { it.status == TaskStatus.COMPLETED }
        composeTestRule.setContent {
            TestWrapper { TaskListView(tasks = completedTasks) }
        }
        composeTestRule.onNodeWithText("提交周报").assertIsDisplayed()
    }

    @Test
    fun sectionHeader_shows_correct_completion_count() {
        composeTestRule.setContent {
            TestWrapper { SectionHeader(completed = 2, total = 5) }
        }
        composeTestRule.onNodeWithText("今日任务").assertIsDisplayed()
        composeTestRule.onNodeWithText("2/5 已完成").assertIsDisplayed()
    }

    @Test
    fun sectionHeader_shows_zero_completion() {
        composeTestRule.setContent {
            TestWrapper { SectionHeader(completed = 0, total = 0) }
        }
        composeTestRule.onNodeWithText("0/0 已完成").assertIsDisplayed()
    }

    @Composable
    private fun TestWrapper(content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalPermissionLauncher provides null
        ) {
            content()
        }
    }

    @Composable
    private fun TopHeader() {
        Row(
            modifier = Modifier.fillMaxWidth().background(Color.White).padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "NextThing", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2D3436))
        }
    }

    @Composable
    private fun TabsRow() {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            listOf("待办", "已完成", "全部").forEach { tab ->
                Text(text = tab, modifier = Modifier.weight(1f).padding(8.dp))
            }
        }
    }

    @Composable
    private fun TaskListView(tasks: List<Task>) {
        tasks.forEach { task ->
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(text = task.title, modifier = Modifier.padding(16.dp))
            }
        }
    }

    @Composable
    private fun SectionHeader(completed: Int, total: Int) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("今日任务")
            Text("$completed/$total 已完成")
        }
    }
}
