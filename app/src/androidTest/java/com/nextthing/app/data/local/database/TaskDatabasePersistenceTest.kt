package com.nextthing.app.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nextthing.app.data.local.entity.CategoryEntity
import com.nextthing.app.data.local.entity.SyncStatus
import com.nextthing.app.data.local.entity.TaskEntity
import com.nextthing.app.domain.model.TaskStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class TaskDatabasePersistenceTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val databaseName = "task-persistence-test.db"
    private var database: TaskDatabase? = null

    @Before
    fun setUp() {
        context.deleteDatabase(databaseName)
    }

    @After
    fun tearDown() {
        database?.close()
        database = null
        context.deleteDatabase(databaseName)
    }

    @Test
    fun taskAndCompletionMetadataSurviveDatabaseCloseAndReopen() = runBlocking {
        val dueDate = LocalDateTime.of(2026, 8, 2, 15, 0)
        val completedAt = LocalDateTime.of(2026, 8, 1, 15, 30)
        val original = task(dueDate = dueDate)

        openDatabase().apply {
            categoryDao().insertCategory(category())
            taskDao().insertTask(original)
        }
        reopenDatabase()

        val restoredPending = database!!.taskDao().getTaskById(original.id)
        assertNotNull(restoredPending)
        assertEquals(TaskStatus.PENDING, restoredPending!!.task.status)
        assertEquals(dueDate, restoredPending.task.dueDate)
        assertNull(restoredPending.task.completedAt)

        database!!.taskDao().updateTask(
            restoredPending.task.copy(
                status = TaskStatus.COMPLETED,
                completedAt = completedAt,
                updatedAt = completedAt,
                syncStatus = SyncStatus.PENDING
            )
        )
        reopenDatabase()

        val restoredCompleted = database!!.taskDao().getTaskById(original.id)
        assertNotNull(restoredCompleted)
        assertEquals(TaskStatus.COMPLETED, restoredCompleted!!.task.status)
        assertEquals(completedAt, restoredCompleted.task.completedAt)
        assertEquals(SyncStatus.PENDING, restoredCompleted.task.syncStatus)
    }

    @Test
    fun softDeletedTaskStaysHiddenAndTombstoneSurvivesDatabaseReopen() = runBlocking {
        val original = task(dueDate = null)
        openDatabase().apply {
            categoryDao().insertCategory(category())
            taskDao().insertTask(original)
            taskDao().softDeleteTask(original.id, LocalDateTime.of(2026, 8, 1, 16, 0))
        }
        reopenDatabase()

        assertNull(database!!.taskDao().getTaskById(original.id))
        val tombstone = database!!.taskDao().getTaskEntityByIdIncludingDeleted(original.id)
        assertNotNull(tombstone)
        assertTrue(tombstone!!.deleted)
        assertEquals(SyncStatus.PENDING, tombstone.syncStatus)
    }

    private fun openDatabase(): TaskDatabase {
        return Room.databaseBuilder(context, TaskDatabase::class.java, databaseName)
            .build()
            .also { database = it }
    }

    private fun reopenDatabase() {
        database!!.close()
        database = null
        openDatabase()
    }

    private fun category() = CategoryEntity(
        id = "category-persistence",
        name = "持久化测试",
        type = 1,
        icon = "work",
        colorHex = "#42A5F5",
        sortOrder = 0,
        createdAt = LocalDateTime.of(2026, 8, 1, 12, 0)
    )

    private fun task(dueDate: LocalDateTime?) = TaskEntity(
        id = "task-persistence",
        title = "离线持久化测试任务",
        description = "Room close and reopen",
        categoryId = "category-persistence",
        status = TaskStatus.PENDING,
        createdAt = LocalDateTime.of(2026, 8, 1, 12, 0),
        updatedAt = LocalDateTime.of(2026, 8, 1, 12, 0),
        dueDate = dueDate,
        completedAt = null,
        tags = "[]",
        isUrgent = false,
        estimatedDuration = 0,
        actualDuration = 0,
        subtasksJson = "[]",
        syncStatus = SyncStatus.PENDING
    )
}
