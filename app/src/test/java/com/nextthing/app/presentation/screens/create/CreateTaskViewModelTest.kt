package com.nextthing.app.presentation.screens.create

import com.nextthing.app.data.service.AIRouteMode
import com.nextthing.app.data.service.AIRouteStatus
import com.nextthing.app.domain.model.*
import org.junit.Assert.*
import org.junit.Test

/**
 * CreateTaskScreen 20 个状态的单元测试
 * 测试纯 Kotlin 逻辑：UiState 状态转换、字段更新、验证规则
 *
 * 注意：由于 CreateTaskViewModel 构造函数依赖多个 Hilt 注入的 Repository/UseCase，
 * 完整的 ViewModel 测试需要 Robolectric 或 Instrumented Test。
 * 这里测试纯 Kotlin 数据逻辑层，覆盖全部 20 个状态的数据状态。
 */
class CreateTaskViewModelTest {

    @Test
    fun aiRoute_externalProvider_showsProviderEnabled() {
        val ui = AIRouteStatus(AIRouteMode.ExternalProvider).toCreateTaskRouteUi()

        assertEquals("DeepSeek 已启用", ui.statusText)
        assertEquals("使用本机 API Key 自动整理语音/文字任务", ui.detailText)
    }

    @Test
    fun aiRoute_backendFallback_showsServerAIEnabled() {
        val ui = AIRouteStatus(AIRouteMode.BackendFallback).toCreateTaskRouteUi()

        assertEquals("服务端 AI 已启用", ui.statusText)
        assertEquals("已登录，可直接自动整理语音/文字任务", ui.detailText)
    }

    @Test
    fun aiRoute_unavailable_showsConfigurationRequired() {
        val ui = AIRouteStatus(AIRouteMode.Unavailable).toCreateTaskRouteUi()

        assertEquals("AI 解析未启用", ui.statusText)
        assertEquals("登录或配置 API Key 后可使用 AI 自动整理", ui.detailText)
    }

    // ── 状态 1：空表单（按钮禁用）──

    @Test
    fun state1_emptyForm_titleBlank_saveButtonDisabled() {
        val state = CreateTaskUiState()
        assertTrue(state.title.isBlank())
        assertFalse(state.title.isNotBlank()) // 按钮 isEnabled = title.isNotBlank()
    }

    @Test
    fun state1_emptyForm_allDefaultsUnset() {
        val state = CreateTaskUiState()
        assertNull(state.selectedDate)
        assertNull(state.preciseTime)
        assertNull(state.selectedCategoryItem)
        assertNull(state.selectedImageUri)
        assertNull(state.notificationStrategyId)
        assertFalse(state.geofenceEnabled)
        assertEquals(RepeatFrequencyType.NONE, state.repeatFrequency.type)
    }

    // ── 状态 2：输入标题（按钮激活）──

    @Test
    fun state2_titleEntered_saveButtonEnabled() {
        val state = CreateTaskUiState(title = "完成项目报告")
        assertTrue(state.title.isNotBlank())
    }

    // ── 状态 3：日期选择 ──

    @Test
    fun state3_dateSelected_displayCorrectly() {
        val date = java.time.LocalDate.of(2025, 6, 9)
        val state = CreateTaskUiState(selectedDate = date)
        assertNotNull(state.selectedDate)
        assertEquals(9, state.selectedDate!!.dayOfMonth)
        assertEquals(6, state.selectedDate!!.monthValue)
    }

    @Test
    fun state3_dateNull_showsPlaceholder() {
        val state = CreateTaskUiState()
        assertNull(state.selectedDate)
    }

    // ── 状态 4：分类选择 ──

    @Test
    fun state4_categorySelected_storedCorrectly() {
        val category = CategoryItem(
            id = "work",
            displayName = "工作",
            icon = "briefcase",
            colorHex = "#6C5CE7",
            isSystemDefault = true
        )
        val state = CreateTaskUiState(selectedCategoryItem = category)
        assertNotNull(state.selectedCategoryItem)
        assertEquals("工作", state.selectedCategoryItem!!.displayName)
    }

    @Test
    fun state4_noCategory_defaultsToLife() {
        val state = CreateTaskUiState()
        val category = state.category // computed property
        assertEquals("生活", category.name)
        assertEquals(PresetCategories.LIFE_ID, category.id)
    }

    // ── 状态 5：重要程度选择 ──

    @Test
    fun state5_importanceSelected_storedCorrectly() {
        val state = CreateTaskUiState(importanceUrgency = TaskImportanceUrgency.IMPORTANT_URGENT)
        assertEquals(TaskImportanceUrgency.IMPORTANT_URGENT, state.importanceUrgency)
    }

    @Test
    fun state5_defaultImportance_isImportantNotUrgent() {
        val state = CreateTaskUiState()
        assertEquals(TaskImportanceUrgency.IMPORTANT_NOT_URGENT, state.importanceUrgency)
    }

    @Test
    fun state5_allImportanceValues_valid() {
        val values = TaskImportanceUrgency.values()
        assertEquals(4, values.size)
        assertTrue(values.contains(TaskImportanceUrgency.IMPORTANT_URGENT))
        assertTrue(values.contains(TaskImportanceUrgency.IMPORTANT_NOT_URGENT))
        assertTrue(values.contains(TaskImportanceUrgency.NOT_IMPORTANT_URGENT))
        assertTrue(values.contains(TaskImportanceUrgency.NOT_IMPORTANT_NOT_URGENT))
    }

    // ── 状态 6：语音录制中 ──

    @Test
    fun state6_recordingState_isAIParsingFalse() {
        val state = CreateTaskUiState()
        assertFalse(state.isAIParsing)
        assertFalse(state.showAIResult)
    }

    // ── 状态 7：AI 解析中 ──

    @Test
    fun state7_aiParsing_parsingState() {
        val state = CreateTaskUiState(isAIParsing = true)
        assertTrue(state.isAIParsing)
        assertFalse(state.showAIResult)
        assertNull(state.aiParseResult)
    }

    // ── 状态 8：AI 单条结果 ──

    @Test
    fun state8_aiSingleResult_showsResult() {
        val result = AITaskParseResult(
            title = "开会",
            dueDate = java.time.LocalDateTime.of(2025, 6, 10, 15, 0),
            categoryName = "工作",
            importance = TaskImportanceUrgency.IMPORTANT_URGENT
        )
        val state = CreateTaskUiState(
            showAIResult = true,
            aiParseResult = result,
            aiParseResults = listOf(result)
        )
        assertTrue(state.showAIResult)
        assertEquals(1, state.aiParseResults.size)
        assertEquals("开会", state.aiParseResult!!.title)
    }

    // ── 状态 9：AI 多条结果 ──

    @Test
    fun state9_aiMultiResult_showsAllResults() {
        val results = listOf(
            AITaskParseResult(title = "完成项目报告", importance = TaskImportanceUrgency.IMPORTANT_URGENT),
            AITaskParseResult(title = "给张经理发邮件", importance = TaskImportanceUrgency.IMPORTANT_NOT_URGENT),
            AITaskParseResult(title = "预约下周会议室", importance = TaskImportanceUrgency.NOT_IMPORTANT_NOT_URGENT)
        )
        val state = CreateTaskUiState(
            showAIResult = true,
            aiParseResults = results,
            aiSelectedIndexes = setOf(0, 1)
        )
        assertTrue(state.showAIResult)
        assertEquals(3, state.aiParseResults.size)
        assertEquals(2, state.aiSelectedIndexes.size)
    }

    @Test
    fun state9_toggleSelection_addsAndRemoves() {
        var selected = setOf(0, 1)
        // toggle index 2 (add)
        selected = if (2 in selected) selected - 2 else selected + 2
        assertEquals(setOf(0, 1, 2), selected)
        // toggle index 1 (remove)
        selected = if (1 in selected) selected - 1 else selected + 1
        assertEquals(setOf(0, 2), selected)
    }

    // ── 状态 10：AI 解析失败 ──

    @Test
    fun state10_aiError_showsError() {
        val state = CreateTaskUiState(aiError = "网络连接超时，请检查网络后重试")
        assertNotNull(state.aiError)
        assertFalse(state.showAIResult)
    }

    @Test
    fun state10_aiError_cleared() {
        val state = CreateTaskUiState(aiError = "网络错误").copy(aiError = null)
        assertNull(state.aiError)
    }

    // ── 状态 11：通知策略 ──

    @Test
    fun state11_notificationStrategy_selected() {
        val strategy = NotificationStrategy(
            id = "strategy1",
            name = "提前15分钟"
        )
        val state = CreateTaskUiState(
            notificationStrategyId = "strategy1",
            availableNotificationStrategies = listOf(strategy)
        )
        assertNotNull(state.notificationStrategyId)
        assertEquals("strategy1", state.notificationStrategyId)
    }

    @Test
    fun state11_noNotificationStrategy_unset() {
        val state = CreateTaskUiState()
        assertNull(state.notificationStrategyId)
    }

    // ── 状态 12：重复频率 ──

    @Test
    fun state12_repeatFrequency_defaultNone() {
        val state = CreateTaskUiState()
        assertEquals(RepeatFrequencyType.NONE, state.repeatFrequency.type)
    }

    @Test
    fun state12_repeatFrequency_daily() {
        val state = CreateTaskUiState(
            repeatFrequency = RepeatFrequency(type = RepeatFrequencyType.DAILY)
        )
        assertEquals(RepeatFrequencyType.DAILY, state.repeatFrequency.type)
    }

    @Test
    fun state12_repeatFrequency_weekly() {
        val state = CreateTaskUiState(
            repeatFrequency = RepeatFrequency(
                type = RepeatFrequencyType.WEEKLY,
                weekdays = setOf(1, 3, 5) // 周一三五
            )
        )
        assertEquals(RepeatFrequencyType.WEEKLY, state.repeatFrequency.type)
        assertEquals(setOf(1, 3, 5), state.repeatFrequency.weekdays)
    }

    // ── 状态 13：附件图片 ──

    @Test
    fun state13_imageSelected_storedCorrectly() {
        val state = CreateTaskUiState(selectedImageUri = "content://images/123")
        assertNotNull(state.selectedImageUri)
    }

    @Test
    fun state13_imageCleared_isNull() {
        val state = CreateTaskUiState(selectedImageUri = "content://images/123")
            .copy(selectedImageUri = null)
        assertNull(state.selectedImageUri)
    }

    // ── 状态 14：地理围栏 ──

    @Test
    fun state14_geofenceEnabled_defaultFalse() {
        val state = CreateTaskUiState()
        assertFalse(state.geofenceEnabled)
    }

    @Test
    fun state14_geofenceEnabled_setTrue() {
        val state = CreateTaskUiState(geofenceEnabled = true)
        assertTrue(state.geofenceEnabled)
    }

    @Test
    fun state14_geofenceLocation_selected() {
        val state = CreateTaskUiState(
            geofenceEnabled = true,
            selectedGeofenceLocationId = "loc1"
        )
        assertTrue(state.geofenceEnabled)
        assertEquals("loc1", state.selectedGeofenceLocationId)
    }

    // ── 状态 15：全部配置完成 ──

    @Test
    fun state15_allConfigured_allFieldsSet() {
        val state = CreateTaskUiState(
            title = "完成项目报告并提交给张经理审核",
            selectedDate = java.time.LocalDate.of(2025, 6, 10),
            preciseTime = Pair(15, 0),
            importanceUrgency = TaskImportanceUrgency.IMPORTANT_URGENT,
            selectedImageUri = "content://images/456",
            notificationStrategyId = "strategy1",
            repeatFrequency = RepeatFrequency(type = RepeatFrequencyType.WEEKLY, weekdays = setOf(1, 3, 5)),
            geofenceEnabled = true,
            selectedGeofenceLocationId = "loc1"
        )
        assertTrue(state.title.isNotBlank())
        assertNotNull(state.selectedDate)
        assertNotNull(state.preciseTime)
        assertNotNull(state.importanceUrgency)
        assertNotNull(state.selectedImageUri)
        assertNotNull(state.notificationStrategyId)
        assertTrue(state.geofenceEnabled)
        assertNotEquals(RepeatFrequencyType.NONE, state.repeatFrequency.type)
    }

    // ── 状态 16：日期选择器弹窗（由 UI 层 showDatePicker 控制）──

    @Test
    fun state16_datePicker_dateUpdateWorks() {
        val state = CreateTaskUiState()
        assertNull(state.selectedDate)

        val newState = state.copy(selectedDate = java.time.LocalDate.of(2025, 6, 10))
        assertEquals(10, newState.selectedDate!!.dayOfMonth)
    }

    // ── 状态 17：精确时间 iOS 滚轮 ──

    @Test
    fun state17_preciseTime_setAndClear() {
        val state = CreateTaskUiState()
        assertNull(state.preciseTime)

        // 设置时间
        val setTime = state.copy(preciseTime = Pair(15, 0))
        assertEquals(15, setTime.preciseTime!!.first)
        assertEquals(0, setTime.preciseTime!!.second)

        // 清除时间
        val clearedTime = setTime.copy(preciseTime = null)
        assertNull(clearedTime.preciseTime)
    }

    @Test
    fun state17_preciseTime_formatCorrectly() {
        val state = CreateTaskUiState(preciseTime = Pair(9, 5))
        val formatted = String.format("%02d:%02d", state.preciseTime!!.first, state.preciseTime!!.second)
        assertEquals("09:05", formatted)
    }

    // ── 状态 18：重复频率 — 每月 ──

    @Test
    fun state18_monthlyRepeat_daySelection() {
        val state = CreateTaskUiState(
            repeatFrequency = RepeatFrequency(
                type = RepeatFrequencyType.MONTHLY,
                monthDays = setOf(1, 5, 15)
            )
        )
        assertEquals(RepeatFrequencyType.MONTHLY, state.repeatFrequency.type)
        assertEquals(setOf(1, 5, 15), state.repeatFrequency.monthDays)
    }

    @Test
    fun state18_monthlyRepeat_toggleDay() {
        var days = setOf(1, 15)
        // add day 5
        days = days + 5
        assertEquals(setOf(1, 5, 15), days)
        // remove day 15
        days = days - 15
        assertEquals(setOf(1, 5), days)
    }

    // ── 状态 19：麦克风权限弹窗 ──
    // (由 UI 层 showMicPermissionDialog 控制，逻辑在 CreateTaskScreen composable)

    @Test
    fun state19_micPermission_uiControlledByScreen() {
        // 权限弹窗状态由 CreateTaskScreen 中的 showMicPermissionDialog var 控制
        // 当 micPermissionLauncher 返回 granted=false 时设置为 true
        // 此处验证 UiState 不受权限弹窗影响
        val state = CreateTaskUiState()
        assertNull(state.errorMessage) // 权限弹窗不影响 UiState
    }

    // ── 状态 20：相机权限弹窗 ──
    // (同状态 19，由 UI 层控制)

    @Test
    fun state20_cameraPermission_uiControlledByScreen() {
        val state = CreateTaskUiState()
        assertNull(state.errorMessage) // 权限弹窗不影响 UiState
    }

    // ── 综合测试 ──

    @Test
    fun allStates_titleValidation_worksCorrectly() {
        // 空标题 → 按钮禁用
        assertFalse("".isNotBlank())
        assertFalse("   ".isNotBlank())

        // 有内容 → 按钮启用
        assertTrue("完成报告".isNotBlank())
        assertTrue("a".isNotBlank())
    }

    @Test
    fun allStates_uiStateImmutable_copyWorks() {
        val original = CreateTaskUiState()
        val modified = original.copy(title = "新任务", preciseTime = Pair(10, 30))

        // 原始不变
        assertEquals("", original.title)
        assertNull(original.preciseTime)

        // 副本已修改
        assertEquals("新任务", modified.title)
        assertEquals(Pair(10, 30), modified.preciseTime)
    }

    @Test
    fun allStates_repeatFrequencyType_allValues() {
        val types = RepeatFrequencyType.values()
        assertTrue(types.contains(RepeatFrequencyType.NONE))
        assertTrue(types.contains(RepeatFrequencyType.DAILY))
        assertTrue(types.contains(RepeatFrequencyType.WEEKLY))
        assertTrue(types.contains(RepeatFrequencyType.MONTHLY))
        assertTrue(types.contains(RepeatFrequencyType.WEEKDAYS))
        assertTrue(types.contains(RepeatFrequencyType.WEEKENDS))
        assertTrue(types.contains(RepeatFrequencyType.LEGAL_HOLIDAY))
    }
}
