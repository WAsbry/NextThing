package com.nextthing.app.presentation.screens.create

import android.util.Log
import com.nextthing.app.data.service.AICompletionClient
import com.nextthing.app.domain.repository.CategoryRepository
import com.nextthing.app.domain.repository.NotificationStrategyRepository
import com.nextthing.app.domain.service.ASRService
import com.nextthing.app.domain.service.AITaskParser
import com.nextthing.app.domain.service.CategoryPreferencesManager
import com.nextthing.app.domain.service.OnDeviceAIEngine
import com.nextthing.app.domain.service.SERService
import com.nextthing.app.domain.usecase.GeofenceUseCases
import com.nextthing.app.domain.usecase.LocationUseCases
import com.nextthing.app.domain.usecase.TaskUseCases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
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
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class CreateTaskVoiceInputTest {

    private val mainDispatcher = UnconfinedTestDispatcher()
    private lateinit var logMock: MockedStatic<Log>

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
        logMock = Mockito.mockStatic(Log::class.java)
    }

    @After
    fun tearDown() {
        logMock.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `start is idempotent and partial then final text remains editable`() = runTest {
        val asr = FakeASRService()
        val viewModel = createViewModel(asr)

        viewModel.startASR()
        viewModel.startASR()

        assertEquals(1, asr.startCalls)
        assertTrue(viewModel.isASRRecording.value)

        asr.emitPartial("完成面试")
        assertEquals("完成面试", viewModel.uiState.value.title)

        asr.emitFinal("完成面试项目整理")
        assertFalse(viewModel.isASRRecording.value)
        assertEquals("完成面试项目整理", viewModel.uiState.value.title)

        viewModel.updateTitle("完成面试项目整理并复盘")
        assertEquals("完成面试项目整理并复盘", viewModel.uiState.value.title)
    }

    @Test
    fun `blank final result stops recording without invoking AI parser`() = runTest {
        val asr = FakeASRService()
        val parser = mock<AITaskParser>()
        val viewModel = createViewModel(asr, parser)

        viewModel.startASR()
        asr.emitFinal("   ")

        assertFalse(viewModel.isASRRecording.value)
        assertTrue(viewModel.uiState.value.title.isBlank())
        verify(parser, never()).parseTaskFromText(any(), any(), any(), anyOrNull())
    }

    @Test
    fun `recognition error stops recording and preserves keyboard fallback`() = runTest {
        val asr = FakeASRService()
        val viewModel = createViewModel(asr)
        viewModel.updateTitle("键盘输入仍然保留")

        viewModel.startASR()
        asr.emitError("语音模型不可用")

        assertFalse(viewModel.isASRRecording.value)
        assertEquals("语音模型不可用", viewModel.uiState.value.aiError)
        assertEquals("键盘输入仍然保留", viewModel.uiState.value.title)
    }

    @Test
    fun `stop delegates to ASR service`() = runTest {
        val asr = FakeASRService()
        val viewModel = createViewModel(asr)

        viewModel.startASR()
        viewModel.stopASR()

        assertEquals(1, asr.stopCalls)
    }

    @Test
    fun `model unavailable does not block keyboard input`() = runTest {
        val asr = FakeASRService(
            ready = false,
            initialError = "语音资源缺失"
        )
        val viewModel = createViewModel(asr)

        viewModel.updateTitle("改用文字创建")

        assertFalse(viewModel.isModelReady.value)
        assertEquals("语音资源缺失", viewModel.asrErrorMessage.value)
        assertEquals("改用文字创建", viewModel.uiState.value.title)
    }

    private fun createViewModel(
        asrService: ASRService,
        aiTaskParser: AITaskParser = mock()
    ): CreateTaskViewModel {
        val categoryRepository = mock<CategoryRepository>()
        whenever(categoryRepository.getAllCategories()).thenReturn(flowOf(emptyList()))

        val notificationStrategyRepository = mock<NotificationStrategyRepository>()
        whenever(notificationStrategyRepository.getAllStrategies()).thenReturn(flowOf(emptyList()))

        runBlocking {
            whenever(
                aiTaskParser.parseTaskFromText(any(), any(), any(), anyOrNull())
            ).thenReturn(Result.failure(IllegalStateException("AI is outside this test scope")))
        }

        return CreateTaskViewModel(
            taskUseCases = mock<TaskUseCases>(),
            categoryRepository = categoryRepository,
            categoryPreferencesManager = mock<CategoryPreferencesManager>(),
            locationUseCases = mock<LocationUseCases>(),
            notificationStrategyRepository = notificationStrategyRepository,
            geofenceUseCases = mock<GeofenceUseCases>(),
            aiTaskParser = aiTaskParser,
            asrService = asrService,
            serService = mock<SERService>(),
            onDeviceAIEngine = mock<OnDeviceAIEngine>(),
            aiCompletionClient = mock<AICompletionClient>()
        )
    }

    private class FakeASRService(
        ready: Boolean = false,
        initialError: String? = null
    ) : ASRService {
        override val isReady: StateFlow<Boolean> = MutableStateFlow(ready)
        override val errorMessage: StateFlow<String?> = MutableStateFlow(initialError)

        var startCalls = 0
        var stopCalls = 0

        private var onPartial: ((String) -> Unit)? = null
        private var onFinal: ((String) -> Unit)? = null
        private var onError: ((String) -> Unit)? = null

        override suspend fun isConfigured(): Boolean = true

        override fun start(
            onPartial: (String) -> Unit,
            onFinal: (String) -> Unit,
            onError: (String) -> Unit,
            onAudioCaptured: (ShortArray, Int) -> Unit
        ) {
            startCalls++
            this.onPartial = onPartial
            this.onFinal = onFinal
            this.onError = onError
        }

        override fun stop() {
            stopCalls++
        }

        fun emitPartial(text: String) = requireNotNull(onPartial).invoke(text)
        fun emitFinal(text: String) = requireNotNull(onFinal).invoke(text)
        fun emitError(message: String) = requireNotNull(onError).invoke(message)
    }
}
