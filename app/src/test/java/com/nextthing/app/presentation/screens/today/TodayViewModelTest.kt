package com.nextthing.app.presentation.screens.today

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nextthing.app.domain.model.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/**
 * TodayViewModel 单元测试（轻量版）
 * 测试纯逻辑：UiState 数据类、Tab 过滤、Task 模型
 *
 * 注意：由于 TodayViewModel 构造函数依赖 Context (ThemePreferences)，
 * 完整的 ViewModel 测试需要 Robolectric 或 Instrumented Test。
 * 这里先测试纯 Kotlin 逻辑层。
 */
class TodayViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    // ── UiState 初始值测试 ──

    @Test
    fun todayUiState_defaultValues() {
        val state = TodayUiState()
        assertEquals(0, state.totalTasks)
        assertEquals(0, state.completedTasks)
        assertEquals(0, state.remainingTasks)
        assertEquals(0f, state.completionRate)
        assertNull(state.errorMessage)
        assertNull(state.weatherError)
        assertFalse(state.isLoading)
        assertEquals(TaskTab.PENDING, state.selectedTab)
    }

    // ── Tab 过滤逻辑测试 ──

    @Test
    fun filterPendingTasks_returnsOnlyPendingAndOverdue() {
        val tasks = listOf(
            createTestTask("1", TaskStatus.PENDING),
            createTestTask("2", TaskStatus.COMPLETED),
            createTestTask("3", TaskStatus.OVERDUE),
            createTestTask("4", TaskStatus.DELAYED)
        )

        val pending = tasks.filter {
            it.status == TaskStatus.PENDING || it.status == TaskStatus.OVERDUE || it.status == TaskStatus.DELAYED
        }

        assertEquals(3, pending.size)
        assertTrue(pending.all { it.id != "2" })
    }

    @Test
    fun filterCompletedTasks_returnsOnlyCompleted() {
        val tasks = listOf(
            createTestTask("1", TaskStatus.PENDING),
            createTestTask("2", TaskStatus.COMPLETED),
            createTestTask("3", TaskStatus.COMPLETED)
        )

        val completed = tasks.filter { it.status == TaskStatus.COMPLETED }

        assertEquals(2, completed.size)
        assertTrue(completed.all { it.status == TaskStatus.COMPLETED })
    }

    // ── 完成率计算测试 ──

    @Test
    fun completionRate_calculation() {
        val total = 5
        val completed = 2
        val rate = completed.toFloat() / total.toFloat()
        assertEquals(0.4f, rate, 0.01f)
    }

    @Test
    fun completionRate_zeroTasks() {
        val total = 0
        val completed = 0
        val rate = if (total == 0) 0f else completed.toFloat() / total.toFloat()
        assertEquals(0f, rate, 0.01f)
    }

    @Test
    fun completionRate_allCompleted() {
        val total = 3
        val completed = 3
        val rate = completed.toFloat() / total.toFloat()
        assertEquals(1.0f, rate, 0.01f)
    }

    // ── UiState copy 测试 ──

    @Test
    fun uiState_selectTab_updatesSelectedTab() {
        val state = TodayUiState(selectedTab = TaskTab.PENDING)
        val updated = state.copy(selectedTab = TaskTab.COMPLETED)
        assertEquals(TaskTab.COMPLETED, updated.selectedTab)
    }

    @Test
    fun uiState_showPostponeDialog() {
        val state = TodayUiState()
        val updated = state.copy(
            showPostponeReasonDialog = true,
            postponeTaskId = "task-123"
        )
        assertTrue(updated.showPostponeReasonDialog)
        assertEquals("task-123", updated.postponeTaskId)
    }

    @Test
    fun uiState_hidePostponeDialog() {
        val state = TodayUiState(
            showPostponeReasonDialog = true,
            postponeTaskId = "task-123"
        )
        val updated = state.copy(
            showPostponeReasonDialog = false,
            postponeTaskId = null
        )
        assertFalse(updated.showPostponeReasonDialog)
        assertNull(updated.postponeTaskId)
    }

    @Test
    fun uiState_errorMessage() {
        val state = TodayUiState()
        val updated = state.copy(errorMessage = "网络错误")
        assertEquals("网络错误", updated.errorMessage)
    }

    @Test
    fun locationEnvironment_doesNotOverwriteTerminalFailureWithLoading() {
        val failed = TodayUiState(
            hasLocationPermission = true,
            isLocationEnabled = true
        )
            .locationRequestStarted()
            .locationRequestFailed(
                locationName = "获取位置失败",
                errorMessage = "位置获取超时，请检查GPS信号或稍后重试"
            )

        val resumed = failed.withLocationEnvironment(
            hasPermission = true,
            isEnabled = true,
            validCachedLocation = null
        )

        assertFalse(resumed.isLocationLoading)
        assertEquals("获取位置失败", resumed.currentLocationName)
        assertEquals("位置获取超时，请检查GPS信号或稍后重试", resumed.locationError)
    }

    @Test
    fun locationEnvironment_keepsLoadingOnlyForActiveRequestState() {
        val loading = TodayUiState(
            hasLocationPermission = true,
            isLocationEnabled = true
        ).locationRequestStarted()

        val refreshed = loading.withLocationEnvironment(
            hasPermission = true,
            isEnabled = true,
            validCachedLocation = null
        )

        assertTrue(refreshed.isLocationLoading)
        assertEquals("正在获取位置...", refreshed.currentLocationName)
    }

    @Test
    fun locationEnvironment_usesValidCacheWithoutStartingLoading() {
        val cachedLocation = LocationInfo(
            id = "cached-location",
            locationName = "测试地点",
            latitude = 30.0,
            longitude = 120.0
        )

        val refreshed = TodayUiState().withLocationEnvironment(
            hasPermission = true,
            isEnabled = true,
            validCachedLocation = cachedLocation
        )

        assertEquals(cachedLocation, refreshed.currentLocation)
        assertEquals("测试地点", refreshed.currentLocationName)
        assertFalse(refreshed.isLocationLoading)
    }

    @Test
    fun locationRequestCancellation_convergesLoadingAndPreservesPermissionState() {
        val permissionDenied = TodayUiState(
            hasLocationPermission = true,
            isLocationEnabled = true
        )
            .locationRequestStarted()
            .withLocationEnvironment(
                hasPermission = false,
                isEnabled = true,
                validCachedLocation = null
            )
            .locationRequestCancelled(
                locationName = "获取位置失败",
                errorMessage = "位置获取失败，请检查权限和位置服务"
            )

        assertFalse(permissionDenied.isLocationLoading)
        assertEquals("需要位置权限", permissionDenied.currentLocationName)
    }

    // ── Task 模型测试 ──

    @Test
    fun task_status_transitions() {
        val task = createTestTask("1", TaskStatus.PENDING)
        assertEquals(TaskStatus.PENDING, task.status)

        // 验证状态枚举值
        val statuses = TaskStatus.values()
        assertTrue(statuses.contains(TaskStatus.PENDING))
        assertTrue(statuses.contains(TaskStatus.COMPLETED))
        assertTrue(statuses.contains(TaskStatus.OVERDUE))
        assertTrue(statuses.contains(TaskStatus.DELAYED))
    }

    // ── Helper ──

    private fun createTestTask(id: String, status: TaskStatus) = Task(
        id = id,
        title = "测试任务 $id",
        description = "",
        status = status,
        category = Category(
            id = "cat-1",
            name = "工作",
            type = CategoryType.PRESET,
            icon = "work",
            colorHex = "#6C5CE7"
        ),
        createdAt = java.time.LocalDateTime.now(),
        updatedAt = java.time.LocalDateTime.now()
    )
}
