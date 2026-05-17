package com.nextthing.app.presentation.screens.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextthing.app.domain.model.Task
import com.nextthing.app.domain.model.Category
import com.nextthing.app.domain.model.CategoryType
import com.nextthing.app.domain.model.TaskStatus
import com.nextthing.app.domain.model.CategoryItem
import com.nextthing.app.domain.model.TaskImportanceUrgency
import com.nextthing.app.domain.model.RepeatFrequency
import com.nextthing.app.domain.model.RepeatFrequencyType
import com.nextthing.app.domain.usecase.TaskUseCases
import com.nextthing.app.domain.repository.CategoryRepository
import com.nextthing.app.domain.repository.NotificationStrategyRepository
import com.nextthing.app.domain.service.CategoryPreferencesManager
import com.nextthing.app.domain.usecase.LocationUseCases
import com.nextthing.app.domain.model.LocationInfo
import com.nextthing.app.domain.model.NotificationStrategy
import com.nextthing.app.domain.model.GeofenceLocation
import com.nextthing.app.domain.usecase.GeofenceUseCases
import com.nextthing.app.domain.model.AITaskParseResult
import com.nextthing.app.domain.service.AITaskParser
import com.nextthing.app.domain.service.ASRService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class CreateTaskViewModel @Inject constructor(
    private val taskUseCases: TaskUseCases,
    private val categoryRepository: CategoryRepository,
    private val categoryPreferencesManager: CategoryPreferencesManager,
    private val locationUseCases: LocationUseCases,
    private val notificationStrategyRepository: NotificationStrategyRepository,
    private val geofenceUseCases: GeofenceUseCases,
    private val aiTaskParser: AITaskParser,
    private val asrService: ASRService
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateTaskUiState())
    val uiState: StateFlow<CreateTaskUiState> = _uiState.asStateFlow()

    private val _isASRRecording = MutableStateFlow(false)
    val isASRRecording: StateFlow<Boolean> = _isASRRecording.asStateFlow()

    private val _categories = MutableStateFlow<List<CategoryItem>>(emptyList())
    val categories: StateFlow<List<CategoryItem>> = _categories.asStateFlow()

    private val _savedLocations = MutableStateFlow<List<LocationInfo>>(emptyList())
    val savedLocations: StateFlow<List<LocationInfo>> = _savedLocations.asStateFlow()

    private val _availableGeofenceLocations = MutableStateFlow<List<GeofenceLocation>>(emptyList())
    val availableGeofenceLocations: StateFlow<List<GeofenceLocation>> = _availableGeofenceLocations.asStateFlow()

    init {
        initializeDefaultDateTime()
        initializeCategories()
        loadSavedLocations()
        loadNotificationStrategies()
        loadAvailableGeofenceLocations()
        loadDefaultRadius()
        loadLastSelectedNotificationStrategy()
        loadLastSelectedGeofenceLocation()
    }

    private fun initializeDefaultDateTime() {
        val now = LocalDateTime.now()
        val currentDate = now.toLocalDate()
        val currentTime = Pair(now.hour, now.minute)

        // 格式化显示字符串，例如 "2026-01-12 14:30"
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val dateTimeString = now.format(formatter)

        _uiState.value = _uiState.value.copy(
            selectedDate = currentDate,
            preciseTime = currentTime,
            dueDate = dateTimeString
        )

        Timber.d("初始化默认时间: selectedDate=$currentDate, preciseTime=$currentTime, dueDate=$dateTimeString")
    }

    private fun loadDefaultRadius() {
        viewModelScope.launch {
            try {
                val config = geofenceUseCases.getGeofenceConfig().first()
                config?.let {
                    _uiState.value = _uiState.value.copy(defaultRadius = it.defaultRadius)
                }
            } catch (e: Exception) {
                Timber.e(e, "加载全局配置失败，使用默认值 200")
            }
        }
    }

    private fun loadLastSelectedNotificationStrategy() {
        viewModelScope.launch {
            try {
                val lastStrategyId = categoryPreferencesManager.getLastSelectedNotificationStrategyId()
                if (lastStrategyId != null) {
                    // 验证策略是否仍然存在
                    val strategies = notificationStrategyRepository.getAllStrategies().first()
                    if (strategies.any { it.id == lastStrategyId }) {
                        _uiState.value = _uiState.value.copy(notificationStrategyId = lastStrategyId)
                        Timber.d("加载上次通知策略: $lastStrategyId")
                    } else {
                        // 策略已被删除，清除记录
                        categoryPreferencesManager.saveLastSelectedNotificationStrategyId(null)
                        Timber.d("上次通知策略已被删除，已清除记录")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "加载上次通知策略失败")
            }
        }
    }

    private fun loadLastSelectedGeofenceLocation() {
        viewModelScope.launch {
            try {
                val lastLocationId = categoryPreferencesManager.getLastSelectedGeofenceLocationId()
                if (lastLocationId != null) {
                    // 验证地点是否仍然存在
                    val locations = geofenceUseCases.getGeofenceLocations().first()
                    if (locations.any { it.id == lastLocationId }) {
                        _uiState.value = _uiState.value.copy(
                            geofenceEnabled = true,
                            selectedGeofenceLocationId = lastLocationId
                        )
                        Timber.d("加载上次地理围栏地点: $lastLocationId")
                    } else {
                        // 地点已被删除，清除记录
                        categoryPreferencesManager.saveLastSelectedGeofenceLocationId(null)
                        Timber.d("上次地理围栏地点已被删除，已清除记录")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "加载上次地理围栏地点失败")
            }
        }
    }

    private fun initializeCategories() {
        viewModelScope.launch {
            try {
                categoryRepository.initializeSystemCategories()

                // 首先获取上次选择的分类
                val lastSelectedCategoryId = categoryPreferencesManager.getLastSelectedCategoryId()

                var defaultCategorySet = false

                categoryRepository.getAllCategories().collect { categories ->
                    // 将Category转换为CategoryItem
                    val categoryItems = categories.map { category ->
                        CategoryItem(
                            id = category.id,
                            displayName = category.name,
                            colorHex = category.colorHex,
                            icon = category.icon,
                            isPinned = false,
                            order = category.sortOrder,
                            isSystemDefault = (category.type == CategoryType.PRESET)
                        )
                    }

                    // 按 sortOrder 排序（与分类管理页一致）
                    _categories.value = categoryItems

                    // 仅在第一次收到数据时设置默认分类；后续 DB 变化不覆盖用户的当前选择
                    if (!defaultCategorySet) {
                        loadLastSelectedCategory(lastSelectedCategoryId)
                        defaultCategorySet = true
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize categories")
            }
        }
    }

    private fun loadLastSelectedCategory(categoryId: String) {
        try {
            // 在分类列表中查找对应的CategoryItem
            val categoryItem = _categories.value.find { it.id == categoryId }
                ?: _categories.value.find { it.displayName == "生活" } // 默认生活分类
                ?: _categories.value.firstOrNull() // 使用第一个分类作为备用

            _uiState.value = _uiState.value.copy(selectedCategoryItem = categoryItem)
        } catch (e: Exception) {
            Timber.e(e, "Failed to load last selected category")
            // 出错时使用第一个分类
            val defaultCategory = _categories.value.firstOrNull()
            _uiState.value = _uiState.value.copy(selectedCategoryItem = defaultCategory)
        }
    }

    private fun loadSavedLocations() {
        viewModelScope.launch {
            try {
                locationUseCases.getAllSavedLocations().collect { locations ->
                    _savedLocations.value = locations
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load saved locations")
            }
        }
    }

    private fun loadNotificationStrategies() {
        viewModelScope.launch {
            try {
                notificationStrategyRepository.getAllStrategies().collect { strategies ->
                    _uiState.value = _uiState.value.copy(availableNotificationStrategies = strategies)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load notification strategies")
            }
        }
    }

    private fun loadAvailableGeofenceLocations() {
        viewModelScope.launch {
            try {
                geofenceUseCases.getGeofenceLocations().collect { locations ->
                    _availableGeofenceLocations.value = locations
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load geofence locations")
            }
        }
    }

    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
    }

    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun updateSelectedCategory(categoryItem: CategoryItem) {
        // 记录分类使用
        viewModelScope.launch {
            try {
                categoryPreferencesManager.recordCategoryUsage(categoryItem.id)
            } catch (e: Exception) {
                Timber.e(e, "Failed to record category usage")
            }
        }

        // 直接设置选中的CategoryItem
        _uiState.value = _uiState.value.copy(selectedCategoryItem = categoryItem)
    }

    fun updateDueDate(dueDate: String) {
        _uiState.value = _uiState.value.copy(dueDate = dueDate)
    }

    fun updateImportanceUrgency(importanceUrgency: TaskImportanceUrgency?) {
        _uiState.value = _uiState.value.copy(importanceUrgency = importanceUrgency)
    }

    fun updateSelectedImage(imageUri: String?) {
        _uiState.value = _uiState.value.copy(selectedImageUri = imageUri)
    }

    fun clearSelectedImage() {
        _uiState.value = _uiState.value.copy(selectedImageUri = null)
    }

    fun updateRepeatFrequency(repeatFrequency: RepeatFrequency) {
        _uiState.value = _uiState.value.copy(repeatFrequency = repeatFrequency)
    }

    fun updateRepeatFrequencyType(type: RepeatFrequencyType) {
        val currentRepeat = _uiState.value.repeatFrequency
        val newRepeat = when (type) {
            RepeatFrequencyType.NONE, RepeatFrequencyType.DAILY,
            RepeatFrequencyType.WEEKDAYS, RepeatFrequencyType.WEEKENDS,
            RepeatFrequencyType.LEGAL_HOLIDAY -> {
                RepeatFrequency(type = type)
            }
            RepeatFrequencyType.WEEKLY -> {
                RepeatFrequency(type = type, weekdays = currentRepeat.weekdays)
            }
            RepeatFrequencyType.MONTHLY -> {
                RepeatFrequency(type = type, monthDays = currentRepeat.monthDays)
            }
        }
        _uiState.value = _uiState.value.copy(repeatFrequency = newRepeat)
    }

    fun updateRepeatWeekdays(weekdays: Set<Int>) {
        val currentRepeat = _uiState.value.repeatFrequency
        if (currentRepeat.type == RepeatFrequencyType.WEEKLY) {
            val newRepeat = currentRepeat.copy(weekdays = weekdays)
            _uiState.value = _uiState.value.copy(repeatFrequency = newRepeat)
        }
    }

    fun updateRepeatMonthDays(monthDays: Set<Int>) {
        val currentRepeat = _uiState.value.repeatFrequency
        if (currentRepeat.type == RepeatFrequencyType.MONTHLY) {
            val newRepeat = currentRepeat.copy(monthDays = monthDays)
            _uiState.value = _uiState.value.copy(repeatFrequency = newRepeat)
        }
    }

    fun updatePreciseTime(preciseTime: Pair<Int, Int>?) {
        Timber.tag("NotificationTask").d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Timber.tag("NotificationTask").d("【ViewModel】updatePreciseTime 被调用")
        Timber.tag("NotificationTask").d("  传入的值: $preciseTime")
        Timber.tag("NotificationTask").d("  当前值: ${_uiState.value.preciseTime}")

        _uiState.value = _uiState.value.copy(preciseTime = preciseTime)

        Timber.tag("NotificationTask").d("  更新后的值: ${_uiState.value.preciseTime}")
        Timber.tag("NotificationTask").d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    fun updateSelectedDate(date: LocalDate?) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
    }

    fun updateNotificationStrategy(strategyId: String?) {
        _uiState.value = _uiState.value.copy(notificationStrategyId = strategyId)
        // 保存为上次选择的通知策略
        viewModelScope.launch {
            try {
                categoryPreferencesManager.saveLastSelectedNotificationStrategyId(strategyId)
            } catch (e: Exception) {
                Timber.e(e, "保存上次通知策略失败")
            }
        }
    }

    fun deleteNotificationStrategy(strategyId: String) {
        viewModelScope.launch {
            try {
                // 如果删除的是当前选中的策略，清除选中状态
                if (_uiState.value.notificationStrategyId == strategyId) {
                    _uiState.value = _uiState.value.copy(notificationStrategyId = null)
                }
                notificationStrategyRepository.deleteStrategy(strategyId)
                Timber.d("通知策略删除成功: $strategyId")
            } catch (e: Exception) {
                Timber.e(e, "删除通知策略失败: $strategyId")
            }
        }
    }

    fun updateGeofenceEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(geofenceEnabled = enabled)
        // 如果禁用，清除选中的地点并清除保存的上次地点
        if (!enabled) {
            _uiState.value = _uiState.value.copy(selectedGeofenceLocationId = null)
            viewModelScope.launch {
                try {
                    categoryPreferencesManager.saveLastSelectedGeofenceLocationId(null)
                } catch (e: Exception) {
                    Timber.e(e, "清除上次地理围栏地点失败")
                }
            }
        }
    }

    fun updateSelectedGeofenceLocation(locationId: String?) {
        _uiState.value = _uiState.value.copy(
            selectedGeofenceLocationId = locationId,
            // 当选择地点时，自动启用地理围栏；清除地点时，保持当前启用状态
            geofenceEnabled = if (locationId != null) true else _uiState.value.geofenceEnabled
        )
        Timber.tag("TaskGeofence").d("选择地理围栏地点: locationId=$locationId, geofenceEnabled=${_uiState.value.geofenceEnabled}")
        // 保存为上次选择的地理围栏地点
        viewModelScope.launch {
            try {
                categoryPreferencesManager.saveLastSelectedGeofenceLocationId(locationId)
            } catch (e: Exception) {
                Timber.e(e, "保存上次地理围栏地点失败")
            }
        }
    }

    // ── AI 自然语言解析 ────────────────────────────────────

    fun updateAIInputText(text: String) {
        _uiState.value = _uiState.value.copy(aiInputText = text)
    }

    fun parseWithAI() {
        val text = _uiState.value.aiInputText.trim()
        if (text.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isAIParsing = true, aiError = null, aiParseResult = null, showAIResult = false
            )
            val categoryNames = _categories.value.map { it.displayName }
            val locationNames = _availableGeofenceLocations.value.map { it.locationInfo.locationName }
            aiTaskParser.parseTaskFromText(text, categoryNames, locationNames)
                .onSuccess { parsed ->
                    _uiState.value = _uiState.value.copy(
                        isAIParsing = false, aiParseResult = parsed, showAIResult = true
                    )
                    Timber.tag("AI").d("解析成功: title=${parsed.title}, confidence=${parsed.confidence}")
                }
                .onFailure { error ->
                    Timber.tag("AI").e(error, "AI解析失败")
                    _uiState.value = _uiState.value.copy(
                        isAIParsing = false,
                        aiError = error.message ?: "AI解析失败，请检查设置中的 API Key"
                    )
                }
        }
    }

    fun applyAIResult() {
        val result = _uiState.value.aiParseResult ?: return
        updateTitle(result.title)
        result.description?.let { updateDescription(it) }
        result.dueDate?.let { dt ->
            updateSelectedDate(dt.toLocalDate())
            updatePreciseTime(Pair(dt.hour, dt.minute))
            updateDueDate(dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
        }
        result.categoryName?.let { name ->
            _categories.value.find { it.displayName == name }?.let { updateSelectedCategory(it) }
        }
        result.importance?.let { updateImportanceUrgency(it) }
        result.repeatType?.let { type ->
            updateRepeatFrequencyType(type)
            if (type == RepeatFrequencyType.WEEKLY) {
                result.repeatWeekdays?.let { updateRepeatWeekdays(it) }
            }
        }
        _uiState.value = _uiState.value.copy(showAIResult = false, aiInputText = "")
    }

    fun applyAIResultAndCreate() {
        applyAIResult()
        createTask()
    }

    fun dismissAIResult() {
        _uiState.value = _uiState.value.copy(showAIResult = false, aiParseResult = null)
    }

    fun clearAIError() {
        _uiState.value = _uiState.value.copy(aiError = null)
    }

    // ── 讯飞 ASR 语音识别 ─────────────────────────────────────

    fun startASR() {
        if (_isASRRecording.value) return
        _isASRRecording.value = true
        _uiState.value = _uiState.value.copy(aiError = null)

        asrService.start(
            onPartial = { text ->
                // 实时显示中间结果
                _uiState.value = _uiState.value.copy(aiInputText = text)
            },
            onFinal = { text ->
                _isASRRecording.value = false
                _uiState.value = _uiState.value.copy(aiInputText = text)
                Timber.tag("ASR").d("最终识别结果: $text")
                if (text.isNotBlank()) {
                    parseWithAI()
                }
            },
            onError = { error ->
                _isASRRecording.value = false
                _uiState.value = _uiState.value.copy(aiError = error)
            }
        )
    }

    fun stopASR() {
        asrService.stop()
        // isASRRecording 会在 onFinal/onError 回调后置为 false
    }

    // ─────────────────────────────────────────────────────

    fun createTask() {
        val currentState = _uiState.value
        if (currentState.title.isBlank()) {
            Timber.w("Cannot create task with empty title")
            return
        }

        viewModelScope.launch {
            // ⚠️ NonCancellable: 即使 ViewModel 被销毁（页面返回）也必须完成 DB 写入
            // 否则 onBackPressed() 取消协程导致任务丢失
            withContext(NonCancellable) {
                try {
                    Timber.tag("NotificationTask").d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    Timber.tag("NotificationTask").d("【ViewModel】createTask 被调用")
                    Timber.tag("NotificationTask").d("任务信息：")
                    Timber.tag("NotificationTask").d("  标题: ${currentState.title}")
                    Timber.tag("NotificationTask").d("  描述: ${currentState.description}")
                    Timber.tag("NotificationTask").d("  分类: ${currentState.category}")
                    Timber.tag("NotificationTask").d("  selectedDate: ${currentState.selectedDate}")
                    Timber.tag("NotificationTask").d("  preciseTime: ${currentState.preciseTime}")
                    Timber.tag("NotificationTask").d("  notificationStrategyId: ${currentState.notificationStrategyId}")

                // 计算截止时间
                val dueDateTime = when {
                    // 情况1：选择了日期
                    currentState.selectedDate != null -> {
                        val baseDate = currentState.selectedDate.atStartOfDay()
                        if (currentState.preciseTime != null) {
                            // 如果设置了精确时间，使用设置的时间
                            val time = baseDate.withHour(currentState.preciseTime.first)
                                .withMinute(currentState.preciseTime.second)
                                .withSecond(0)
                                .withNano(0)
                            Timber.tag("NotificationTask").d("计算得到的dueDateTime (选择日期+精确时间): $time")
                            time
                        } else {
                            // 如果没有设置精确时间，默认为当天23:59
                            val time = baseDate.withHour(23)
                                .withMinute(59)
                                .withSecond(59)
                                .withNano(0)
                            Timber.tag("NotificationTask").d("计算得到的dueDateTime (选择日期+默认23:59): $time")
                            time
                        }
                    }
                    // 情况2：没有选择日期，但设置了精确时间 - 使用今天+精确时间
                    currentState.preciseTime != null -> {
                        val today = java.time.LocalDate.now()
                        val time = today.atTime(
                            currentState.preciseTime.first,
                            currentState.preciseTime.second,
                            0,
                            0
                        )
                        Timber.tag("NotificationTask").d("计算得到的dueDateTime (今天+精确时间): $time")
                        time
                    }
                    // 情况3：都没有设置
                    else -> {
                        Timber.tag("NotificationTask").d("未选择日期和精确时间，dueDateTime = null")
                        null
                    }
                }

                // 确定要保存的位置信息
                // 只从地理围栏地点获取locationInfo
                Timber.tag("TaskGeofence").d("━━━━━━ 地理围栏信息检查 ━━━━━━")
                Timber.tag("TaskGeofence").d("  geofenceEnabled: ${currentState.geofenceEnabled}")
                Timber.tag("TaskGeofence").d("  selectedGeofenceLocationId: ${currentState.selectedGeofenceLocationId}")
                Timber.tag("TaskGeofence").d("  可用地理围栏地点数量: ${_availableGeofenceLocations.value.size}")

                val locationInfoToSave = if (currentState.geofenceEnabled && currentState.selectedGeofenceLocationId != null) {
                    // 从可用的地理围栏地点列表中查找选中的地点
                    val selectedLocation = _availableGeofenceLocations.value.find { it.id == currentState.selectedGeofenceLocationId }
                    Timber.tag("TaskGeofence").d("  找到的地点: ${selectedLocation?.locationInfo?.locationName ?: "null"}")
                    selectedLocation?.locationInfo
                } else {
                    Timber.tag("TaskGeofence").d("  地理围栏未启用或未选择地点，不保存locationInfo")
                    null
                }
                Timber.tag("TaskGeofence").d("━━━━━━━━━━━━━━━━━━━━━━━━━")

                Timber.tag("NotificationTask").d("准备调用 taskUseCases.createTask()...")
                Timber.tag("NotificationTask").d("  locationInfo: ${locationInfoToSave?.locationName ?: "null"}")

                val result = taskUseCases.createTask(
                    title = currentState.title,
                    description = currentState.description,
                    category = currentState.category,
                    dueDate = dueDateTime,
                    imageUri = currentState.selectedImageUri,
                    repeatFrequency = currentState.repeatFrequency,
                    notificationStrategyId = currentState.notificationStrategyId,
                    importanceUrgency = currentState.importanceUrgency,
                    locationInfo = locationInfoToSave
                )

                if (result.isSuccess) {
                    val taskId = result.getOrNull()
                    Timber.tag("NotificationTask").d("✅ 任务创建成功")
                    Timber.tag("NotificationTask").d("  任务ID: $taskId")

                    // 如果启用了地理围栏且选择了地点，创建TaskGeofence关联
                    if (taskId != null && currentState.geofenceEnabled && currentState.selectedGeofenceLocationId != null) {
                        try {
                            val geofenceResult = geofenceUseCases.createTaskGeofence.invoke(
                                taskId = taskId,
                                geofenceLocationId = currentState.selectedGeofenceLocationId
                            )
                            if (geofenceResult.isSuccess) {
                                Timber.tag("TaskGeofence").d("✅ 任务地理围栏关联创建成功")
                            } else {
                                Timber.tag("TaskGeofence").e("❌ 任务地理围栏关联创建失败")
                            }
                        } catch (e: Exception) {
                            Timber.tag("TaskGeofence").e(e, "创建任务地理围栏关联异常")
                        }
                    }

                    Timber.tag("NotificationTask").d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    // 重置表单
                    _uiState.value = CreateTaskUiState()
                } else {
                    Timber.tag("NotificationTask").e("❌ 任务创建失败: ${result.exceptionOrNull()?.message}")
                    Timber.tag("NotificationTask").d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to create task")
            }
            } // withContext(NonCancellable)
        }
    }
}

data class CreateTaskUiState(
    val title: String = "",
    val description: String = "",
    val selectedCategoryItem: CategoryItem? = null,
    val dueDate: String = "",
    val selectedDate: LocalDate? = null, // 选择的日期
    val preciseTime: Pair<Int, Int>? = null, // 精确时间（小时, 分钟），null表示未设置
    val isLoading: Boolean = false,
    val importanceUrgency: TaskImportanceUrgency? = TaskImportanceUrgency.IMPORTANT_NOT_URGENT,
    val selectedImageUri: String? = null,
    val repeatFrequency: RepeatFrequency = RepeatFrequency(),
    val notificationStrategyId: String? = null, // 通知策略ID
    val availableNotificationStrategies: List<NotificationStrategy> = emptyList(), // 可用的通知策略列表
    val geofenceEnabled: Boolean = false, // 是否启用地理围栏
    val selectedGeofenceLocationId: String? = null, // 选中的地理围栏地点ID
    val defaultRadius: Int = 200, // 全局默认半径
    val errorMessage: String? = null,
    // AI 自然语言解析
    val aiInputText: String = "",
    val isAIParsing: Boolean = false,
    val aiParseResult: AITaskParseResult? = null,
    val aiError: String? = null,
    val showAIResult: Boolean = false
) {
    // 获取对应的Category，用于创建任务
    val category: Category
        get() = selectedCategoryItem?.let { categoryItem ->
            Category(
                id = categoryItem.id,
                name = categoryItem.displayName,
                type = if (categoryItem.isSystemDefault) CategoryType.PRESET else CategoryType.CUSTOM,
                icon = categoryItem.icon,
                colorHex = categoryItem.colorHex
            )
        } ?: Category(
            id = "LIFE",
            name = "生活",
            type = CategoryType.PRESET,
            icon = "life",
            colorHex = "#4CAF50"
        )
} 