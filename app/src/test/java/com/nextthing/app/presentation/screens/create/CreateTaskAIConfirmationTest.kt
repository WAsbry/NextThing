package com.nextthing.app.presentation.screens.create

import com.nextthing.app.data.service.AICompletionClient
import com.nextthing.app.domain.model.AITaskParseResult
import com.nextthing.app.domain.model.RepeatFrequency
import com.nextthing.app.domain.model.RepeatFrequencyType
import com.nextthing.app.domain.model.TaskImportanceUrgency
import com.nextthing.app.domain.repository.CategoryRepository
import com.nextthing.app.domain.repository.NotificationStrategyRepository
import com.nextthing.app.domain.service.ASRService
import com.nextthing.app.domain.service.AITaskParser
import com.nextthing.app.domain.service.CategoryPreferencesManager
import com.nextthing.app.domain.service.OnDeviceAIEngine
import com.nextthing.app.domain.service.SERService
import com.nextthing.app.domain.usecase.CreateTaskUseCase
import com.nextthing.app.domain.usecase.GeofenceUseCases
import com.nextthing.app.domain.usecase.LocationUseCases
import com.nextthing.app.domain.usecase.TaskUseCases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class CreateTaskAIConfirmationTest {

    private val mainDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `single parsed result is shown then applied only after confirmation`() = runTest {
        val parsed = AITaskParseResult(
            title = "准备字节跳动面试",
            description = "整理项目材料",
            dueDate = LocalDateTime.of(2026, 8, 2, 15, 0),
            importance = TaskImportanceUrgency.IMPORTANT_URGENT,
            repeatType = RepeatFrequencyType.NONE
        )
        val viewModel = createViewModel(Result.success(listOf(parsed))).viewModel
        viewModel.updateTitle("明天下午三点准备面试")

        viewModel.parseWithAI()

        assertTrue(viewModel.uiState.value.showAIResult)
        assertEquals("明天下午三点准备面试", viewModel.uiState.value.title)

        viewModel.applyAIResult()

        assertFalse(viewModel.uiState.value.showAIResult)
        assertEquals("准备字节跳动面试", viewModel.uiState.value.title)
        assertEquals(LocalDateTime.of(2026, 8, 2, 15, 0).toLocalDate(), viewModel.uiState.value.selectedDate)
        assertEquals(15 to 0, viewModel.uiState.value.preciseTime)
        assertEquals(TaskImportanceUrgency.IMPORTANT_URGENT, viewModel.uiState.value.importanceUrgency)
    }

    @Test
    fun `multi result saves only user selected task`() = runTest {
        val first = AITaskParseResult(title = "任务一")
        val second = AITaskParseResult(title = "任务二")
        val harness = createViewModel(Result.success(listOf(first, second)))
        wheneverCreate(harness.createTaskUseCase, Result.success("task-2"))
        harness.viewModel.updateTitle("创建两个任务")

        harness.viewModel.parseWithAI()
        assertEquals(setOf(0, 1), harness.viewModel.uiState.value.aiSelectedIndexes)

        harness.viewModel.toggleAISelection(0)
        harness.viewModel.createSelectedTasks()

        val title = argumentCaptor<String>()
        verify(harness.createTaskUseCase, times(1)).invoke(
            title.capture(), any(), any(), anyOrNull(), any(), anyOrNull(),
            any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()
        )
        assertEquals("任务二", title.firstValue)
        assertFalse(harness.viewModel.uiState.value.isLoading)
        assertTrue(harness.viewModel.uiState.value.aiParseResults.isEmpty())
    }

    @Test
    fun `confirmation save failure remains visible instead of reporting success`() = runTest {
        val harness = createViewModel(Result.success(listOf(AITaskParseResult(title = "失败任务"))))
        wheneverCreate(
            harness.createTaskUseCase,
            Result.failure(IllegalStateException("数据库写入失败"))
        )
        harness.viewModel.updateTitle("创建失败任务")

        harness.viewModel.parseWithAI()
        harness.viewModel.toggleAISelection(0)
        harness.viewModel.createSelectedTasks()

        assertFalse(harness.viewModel.uiState.value.isLoading)
        assertEquals("部分任务创建失败：成功 0 个，失败 1 个", harness.viewModel.uiState.value.errorMessage)
        assertTrue(harness.viewModel.uiState.value.aiParseResults.isNotEmpty())
    }

    private fun createViewModel(
        parserResult: Result<List<AITaskParseResult>>
    ): Harness {
        val parser = object : AITaskParser {
            override suspend fun parseTaskFromText(
                input: String,
                availableCategories: List<String>,
                availableLocations: List<String>,
                voiceContext: String?
            ): Result<List<AITaskParseResult>> = parserResult
        }

        val categoryRepository = mock<CategoryRepository>()
        whenever(categoryRepository.getAllCategories()).thenReturn(flowOf(emptyList()))
        val notificationStrategyRepository = mock<NotificationStrategyRepository>()
        whenever(notificationStrategyRepository.getAllStrategies()).thenReturn(flowOf(emptyList()))

        val asrService = mock<ASRService>()
        whenever(asrService.isReady).thenReturn(MutableStateFlow(false))
        whenever(asrService.errorMessage).thenReturn(MutableStateFlow(null))

        val createTaskUseCase = mock<CreateTaskUseCase>()
        val taskUseCases = mock<TaskUseCases>()
        whenever(taskUseCases.createTask).thenReturn(createTaskUseCase)

        val viewModel = CreateTaskViewModel(
            taskUseCases = taskUseCases,
            categoryRepository = categoryRepository,
            categoryPreferencesManager = mock<CategoryPreferencesManager>(),
            locationUseCases = mock<LocationUseCases>(),
            notificationStrategyRepository = notificationStrategyRepository,
            geofenceUseCases = mock<GeofenceUseCases>(),
            aiTaskParser = parser,
            asrService = asrService,
            serService = mock<SERService>(),
            onDeviceAIEngine = mock<OnDeviceAIEngine>(),
            aiCompletionClient = mock<AICompletionClient>()
        )
        return Harness(viewModel, createTaskUseCase)
    }

    private suspend fun wheneverCreate(
        createTaskUseCase: CreateTaskUseCase,
        result: Result<String>
    ) {
        whenever(
            createTaskUseCase.invoke(
                any(), any(), any(), anyOrNull(), any(), anyOrNull(),
                any<RepeatFrequency>(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()
            )
        ).thenReturn(result)
    }

    private data class Harness(
        val viewModel: CreateTaskViewModel,
        val createTaskUseCase: CreateTaskUseCase
    )
}
