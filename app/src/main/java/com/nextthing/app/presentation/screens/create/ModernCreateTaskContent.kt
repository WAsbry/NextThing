package com.nextthing.app.presentation.screens.create

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextthing.app.domain.model.AITaskParseResult
import com.nextthing.app.domain.model.TaskImportanceUrgency
import com.nextthing.app.R
import com.nextthing.app.data.service.AIRouteMode
import com.nextthing.app.presentation.theme.BgCard
import com.nextthing.app.presentation.theme.BgPrimary
import com.nextthing.app.presentation.theme.Border
import com.nextthing.app.presentation.theme.Danger
import com.nextthing.app.presentation.theme.Success
import com.nextthing.app.presentation.theme.TextPrimary
import com.nextthing.app.presentation.theme.TextSecondary
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val ModernBlue = Color(0xFF3478F6)
private val ModernBlueDark = Color(0xFF185CCF)
private val ModernInk = Color(0xFF171A20)
private val ModernMuted = Color(0xFF737A86)
private val ModernSoft = Color(0xFFF5F6F9)
private val ModernLine = Color(0xFFE9ECF2)

private enum class CreateInputMode { Voice, Keyboard }

@Composable
internal fun ModernCreateTaskContent(
    uiState: CreateTaskUiState,
    isRecording: Boolean,
    isModelReady: Boolean,
    asrErrorMessage: String?,
    onBack: () -> Unit,
    onTitleChange: (String) -> Unit,
    onStartRecording: () -> Boolean,
    onStopRecording: () -> Unit,
    onParseWithAI: () -> Unit,
    onNavigateToAIConfig: () -> Unit,
    onSaveManualTask: () -> Unit,
    onApplyAIResult: () -> Unit,
    onSaveAIResult: () -> Unit,
    onDismissAIResult: () -> Unit,
    onToggleAISelection: (Int) -> Unit,
    onSaveSelectedTasks: () -> Unit,
    onDismissError: () -> Unit,
    advancedContent: @Composable () -> Unit
) {
    var inputMode by remember { mutableStateOf(CreateInputMode.Voice) }
    var showAdvanced by remember { mutableStateOf(false) }
    var isDetailedEditing by remember { mutableStateOf(false) }

    val enterDetailedEditing = {
        // applyAIResult writes the parsed values into the same CreateTaskUiState
        // consumed by the complete form below, then exits the confirmation state.
        isDetailedEditing = true
        onApplyAIResult()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
    ) {
        ModernTopBar(onBack = onBack)
        AIAutoParseStatusBar(
            mode = uiState.aiRouteMode,
            status = uiState.aiRouteStatusText,
            detail = uiState.aiRouteDetailText,
            onConfigure = onNavigateToAIConfig
        )

        Box(modifier = Modifier.weight(1f)) {
            when {
                isRecording -> RecordingState(transcript = uiState.title)
                uiState.isAIParsing -> ParsingState(
                    sourceText = uiState.title,
                    voiceEmotionHint = uiState.voiceEmotionHint
                )
                uiState.showAIResult && uiState.aiParseResults.size > 1 -> {
                    MultiTaskConfirmation(
                        results = uiState.aiParseResults,
                        selectedIndexes = uiState.aiSelectedIndexes,
                        voiceEmotionHint = uiState.voiceEmotionHint,
                        showAdvanced = showAdvanced,
                        onToggleAdvanced = { showAdvanced = !showAdvanced },
                        onToggleSelection = onToggleAISelection,
                        advancedContent = advancedContent
                    )
                }
                uiState.showAIResult && uiState.aiParseResult != null -> {
                    SingleTaskConfirmation(
                        result = uiState.aiParseResult,
                        voiceEmotionHint = uiState.voiceEmotionHint,
                        onEdit = enterDetailedEditing,
                    )
                }
                else -> {
                    InitialInputState(
                        title = uiState.title,
                        inputMode = inputMode,
                        selectedDate = uiState.selectedDate,
                        preciseTime = uiState.preciseTime,
                        voiceEmotionHint = uiState.voiceEmotionHint,
                        showTaskDetails = isDetailedEditing,
                        onModeChange = { inputMode = it },
                        onTitleChange = onTitleChange,
                        detailsContent = advancedContent
                    )
                }
            }

            uiState.aiError?.let { error ->
                ErrorBanner(
                    message = error,
                    onDismiss = onDismissError,
                    onConfigure = onNavigateToAIConfig,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }

        when {
            uiState.isAIParsing -> DisabledBottomBar("正在整理任务")
            uiState.showAIResult && uiState.aiParseResults.size > 1 -> {
                ConfirmationBottomBar(
                    secondaryText = "取消",
                    primaryText = "保存 ${uiState.aiSelectedIndexes.size} 个任务",
                    primaryEnabled = uiState.aiSelectedIndexes.isNotEmpty(),
                    onSecondary = onDismissAIResult,
                    onPrimary = onSaveSelectedTasks
                )
            }
            uiState.showAIResult && uiState.aiParseResult != null -> {
                ConfirmationBottomBar(
                    secondaryText = "编辑详情",
                    primaryText = "保存任务",
                    onSecondary = enterDetailedEditing,
                    onPrimary = onSaveAIResult
                )
            }
            else -> {
                VoiceInputBottomBar(
                    isRecording = isRecording,
                    isModelReady = isModelReady,
                    asrErrorMessage = asrErrorMessage,
                    canSave = uiState.title.isNotBlank(),
                    onStartRecording = onStartRecording,
                    onStopRecording = onStopRecording,
                    onParseWithAI = onParseWithAI,
                    onSave = onSaveManualTask
                )
            }
        }
    }
}

@Composable
private fun ModernTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(BgCard)
            .border(width = 0.5.dp, color = ModernLine)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = ModernSoft,
            border = BorderStroke(1.dp, ModernLine)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = ModernInk,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Text(
            text = "创建任务",
            modifier = Modifier.weight(1f),
            color = ModernInk,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.size(40.dp))
    }
}

@Composable
private fun AIAutoParseStatusBar(
    mode: AIRouteMode,
    status: String,
    detail: String,
    onConfigure: () -> Unit
) {
    val cardShape = RoundedCornerShape(16.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(cardShape)
            .clickable(onClick = onConfigure),
        shape = cardShape,
        color = BgCard,
        border = BorderStroke(1.dp, ModernLine)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AI 自动解析",
                    color = ModernInk,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(text = detail, color = ModernMuted, fontSize = 11.sp, lineHeight = 15.sp)
            }

            Column(
                modifier = Modifier.padding(start = 12.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StatusBadge(mode = mode, status = status)
                Text(
                    text = when (mode) {
                        AIRouteMode.ExternalProvider -> "管理  ›"
                        AIRouteMode.BackendFallback -> "AI 设置  ›"
                        AIRouteMode.Unavailable -> "去配置  ›"
                    },
                    color = ModernBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(mode: AIRouteMode, status: String) {
    val isVerifiedAvailable = mode == AIRouteMode.ExternalProvider
    val isPendingVerification = mode == AIRouteMode.BackendFallback
    val statusColor = when {
        isVerifiedAvailable -> Success
        isPendingVerification -> ModernBlue
        else -> Danger
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = status,
            color = statusColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Surface(
            modifier = Modifier.size(18.dp),
            shape = CircleShape,
            color = when {
                isVerifiedAvailable -> Success
                isPendingVerification -> ModernBlue.copy(alpha = 0.12f)
                else -> Danger.copy(alpha = 0.12f)
            },
            border = if (isVerifiedAvailable) null else BorderStroke(1.dp, statusColor)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isVerifiedAvailable) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "AI 已启用",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                } else if (isPendingVerification) {
                    Text(
                        text = "·",
                        color = ModernBlue,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "!",
                        color = Danger,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun InitialInputState(
    title: String,
    inputMode: CreateInputMode,
    selectedDate: LocalDate?,
    preciseTime: Pair<Int, Int>?,
    voiceEmotionHint: String?,
    showTaskDetails: Boolean,
    onModeChange: (CreateInputMode) -> Unit,
    onTitleChange: (String) -> Unit,
    detailsContent: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(10.dp, RoundedCornerShape(22.dp), ambientColor = Color.Black.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(22.dp),
            color = BgCard,
            border = BorderStroke(1.dp, ModernLine)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (inputMode == CreateInputMode.Voice) "说一句话创建任务" else "输入任务内容",
                        modifier = Modifier.weight(1f),
                        color = ModernMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    InputModeSwitch(inputMode, onModeChange)
                }

                Spacer(modifier = Modifier.height(16.dp))

                BasicTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 104.dp),
                    textStyle = TextStyle(
                        color = ModernInk,
                        fontSize = 21.sp,
                        lineHeight = 29.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 5,
                    decorationBox = { innerTextField ->
                        Box {
                            if (title.isBlank()) {
                                Text(
                                    text = if (inputMode == CreateInputMode.Voice) {
                                        "长按下方按钮，实时转成文字"
                                    } else {
                                        "今天要做什么？"
                                    },
                                    color = Color(0xFFA4ABB6),
                                    fontSize = 21.sp,
                                    lineHeight = 29.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                if (title.isBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "试试说：“明天下午三点提醒我开会”",
                        color = ModernBlue,
                        fontSize = 12.sp
                    )
                } else if (!voiceEmotionHint.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    VoiceEmotionHint(text = voiceEmotionHint)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryChip("日期", selectedDate?.let(::formatModernDate) ?: "今天")
            SummaryChip(
                "提醒",
                preciseTime?.let { "%02d:%02d".format(it.first, it.second) } ?: "无"
            )
        }

        if (showTaskDetails) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "编辑任务信息",
                color = ModernInk,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            detailsContent()
        }
    }
}

@Composable
private fun InputModeSwitch(
    selected: CreateInputMode,
    onSelected: (CreateInputMode) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(ModernSoft)
            .border(1.dp, ModernLine, CircleShape)
            .padding(3.dp)
    ) {
        ModeItem(
            label = "语音",
            selected = selected == CreateInputMode.Voice,
            onClick = { onSelected(CreateInputMode.Voice) }
        )
        ModeItem(
            label = "键盘",
            selected = selected == CreateInputMode.Keyboard,
            onClick = { onSelected(CreateInputMode.Keyboard) }
        )
    }
}

@Composable
private fun ModeItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) ModernBlue else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else ModernMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SummaryChip(label: String, value: String) {
    Surface(
        shape = CircleShape,
        color = BgCard,
        border = BorderStroke(1.dp, ModernLine)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(label, color = ModernMuted, fontSize = 11.sp)
            Text(value, color = ModernInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun RecordingState(transcript: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .shadow(24.dp, CircleShape, ambientColor = ModernBlue.copy(alpha = 0.22f))
                .background(ModernBlue, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            VoiceBars()
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(16.dp)
                    .background(Danger, CircleShape)
                    .border(3.dp, BgCard, CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("正在实时转写", color = ModernInk, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = BgCard,
            border = BorderStroke(1.dp, ModernLine),
            shadowElevation = 8.dp
        ) {
            Text(
                text = transcript.ifBlank { "正在聆听..." },
                modifier = Modifier.padding(18.dp),
                color = if (transcript.isBlank()) ModernMuted else ModernInk,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Normal
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("正在识别你的任务", color = ModernMuted, fontSize = 12.sp)
    }
}

@Composable
private fun VoiceBars() {
    Row(
        modifier = Modifier.height(38.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        listOf(16, 28, 38, 24, 18).forEach { height ->
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(height.dp)
                    .background(Color.White, RoundedCornerShape(3.dp))
            )
        }
    }
}

@Composable
private fun ParsingState(
    sourceText: String,
    voiceEmotionHint: String?
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = ModernBlue, strokeWidth = 3.dp)
        Spacer(modifier = Modifier.height(16.dp))
        Text("正在整理任务", color = ModernInk, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        if (sourceText.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = sourceText,
                modifier = Modifier.padding(horizontal = 36.dp),
                color = ModernMuted,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (!voiceEmotionHint.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            VoiceEmotionHint(text = voiceEmotionHint)
        }
    }
}

@Composable
private fun VoiceEmotionHint(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = ModernBlue.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, ModernBlue.copy(alpha = 0.16f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            color = ModernBlueDark,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 17.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SingleTaskConfirmation(
    result: AITaskParseResult,
    voiceEmotionHint: String?,
    onEdit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFF4F8FF),
            border = BorderStroke(1.dp, ModernBlue.copy(alpha = 0.18f)),
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "AI 已整理任务",
                        modifier = Modifier.weight(1f),
                        color = ModernInk,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    StatusPill("检查后保存")
                }
                if (!voiceEmotionHint.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    VoiceEmotionHint(text = voiceEmotionHint)
                }
                Spacer(modifier = Modifier.height(14.dp))
                ConfirmationField("任务标题", result.title)
                ConfirmationField(
                    "日期与时间",
                    result.dueDate?.format(DateTimeFormatter.ofPattern("MM月dd日 HH:mm")) ?: "未设置"
                )
                ConfirmationField("分类", result.categoryName ?: "默认")
                ConfirmationField(
                    label = "重要程度",
                    value = result.importance?.displayName ?: "未设置",
                    valueColor = if (result.importance == TaskImportanceUrgency.IMPORTANT_URGENT) Danger else ModernInk,
                    showDivider = false
                )

            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        TaskAdjustmentSummary(onClick = onEdit)
    }
}

/**
 * AI 确认态只回答“识别结果是否正确”。其余字段进入独立编辑态，避免同一数据
 * 同时以摘要和八个网格控件出现，造成相互矛盾的视觉与操作入口。
 */
@Composable
private fun TaskAdjustmentSummary(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = BgCard,
        border = BorderStroke(1.dp, ModernLine)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "调整更多信息",
                    color = ModernInk,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "提醒未设置 · 单次任务 · 地点未启用",
                    color = ModernMuted,
                    fontSize = 12.sp
                )
            }
            Text(
                text = "编辑",
                color = ModernBlue,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun MultiTaskConfirmation(
    results: List<AITaskParseResult>,
    selectedIndexes: Set<Int>,
    voiceEmotionHint: String?,
    showAdvanced: Boolean,
    onToggleAdvanced: () -> Unit,
    onToggleSelection: (Int) -> Unit,
    advancedContent: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFF4F8FF),
            border = BorderStroke(1.dp, ModernBlue.copy(alpha = 0.18f)),
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "AI 整理出 ${results.size} 个任务",
                        modifier = Modifier.weight(1f),
                        color = ModernInk,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    StatusPill("已选择 ${selectedIndexes.size} 个")
                }

                if (!voiceEmotionHint.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    VoiceEmotionHint(text = voiceEmotionHint)
                }
                Spacer(modifier = Modifier.height(12.dp))
                results.forEachIndexed { index, result ->
                    MultiTaskRow(
                        result = result,
                        selected = index in selectedIndexes,
                        onClick = { onToggleSelection(index) }
                    )
                    if (index != results.lastIndex) Spacer(modifier = Modifier.height(10.dp))
                }

                MoreSettingsRow(
                    label = "批量设置",
                    description = "分类、提醒、重复",
                    expanded = showAdvanced,
                    onClick = onToggleAdvanced
                )
            }
        }

        if (showAdvanced) {
            Spacer(modifier = Modifier.height(12.dp))
            advancedContent()
        }
    }
}

@Composable
private fun ConfirmationField(
    label: String,
    value: String,
    valueColor: Color = ModernInk,
    showDivider: Boolean = true
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = ModernMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, color = valueColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        if (showDivider) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ModernBlue.copy(alpha = 0.08f)))
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun StatusPill(text: String) {
    Surface(shape = CircleShape, color = Success.copy(alpha = 0.12f)) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            color = Success,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MoreSettingsRow(
    label: String = "更多设置",
    description: String = "重要程度、重复、地点",
    expanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.72f))
            .border(1.dp, ModernBlue.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f), color = ModernInk, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text(
            text = if (expanded) "收起" else "$description  ›",
            color = ModernMuted,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun MultiTaskRow(
    result: AITaskParseResult,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .border(1.dp, ModernBlue.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (selected) ModernBlue else Color.Transparent)
                .border(2.dp, if (selected) ModernBlue else ModernLine, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(result.title, color = ModernInk, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(7.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                result.dueDate?.let {
                    MiniTag(it.format(DateTimeFormatter.ofPattern("MM月dd日 HH:mm")))
                }
                result.categoryName?.let { MiniTag(it) }
                result.importance?.let {
                    MiniTag(
                        text = it.displayName,
                        color = if (it == TaskImportanceUrgency.IMPORTANT_URGENT) Danger else ModernBlueDark,
                        backgroundColor = if (it == TaskImportanceUrgency.IMPORTANT_URGENT) {
                            Danger.copy(alpha = 0.10f)
                        } else {
                            ModernBlue.copy(alpha = 0.08f)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniTag(
    text: String,
    color: Color = ModernBlueDark,
    backgroundColor: Color = ModernBlue.copy(alpha = 0.08f)
) {
    Surface(shape = CircleShape, color = backgroundColor) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            color = color,
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun VoiceInputBottomBar(
    isRecording: Boolean,
    isModelReady: Boolean,
    asrErrorMessage: String?,
    canSave: Boolean,
    onStartRecording: () -> Boolean,
    onStopRecording: () -> Unit,
    onParseWithAI: () -> Unit,
    onSave: () -> Unit
) {
    BottomSurface {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (isRecording) {
                Text("松开结束 · 上滑取消", color = ModernMuted, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
            } else if (asrErrorMessage != null) {
                Text(asrErrorMessage, color = Danger, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(8.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            asrErrorMessage != null -> Color(0xFF9AA1AD)
                            isRecording -> Danger
                            else -> ModernBlue
                        }
                    )
                    .pointerInput(isModelReady, asrErrorMessage) {
                        detectTapGestures(
                            onPress = {
                                if (asrErrorMessage == null && isModelReady && onStartRecording()) {
                                    try {
                                        awaitRelease()
                                    } finally {
                                        onStopRecording()
                                    }
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (asrErrorMessage == null && !isModelReady && !isRecording) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else if (asrErrorMessage == null && !isRecording) {
                        Icon(
                            painter = painterResource(R.drawable.mic_on),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        when {
                            asrErrorMessage != null -> "语音资源未安装"
                            isRecording -> "正在转写"
                            isModelReady -> "长按说话"
                            else -> "语音模型加载中"
                        },
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
                Button(
                    onClick = onParseWithAI,
                    enabled = canSave && !isRecording,
                    modifier = Modifier
                        .width(86.dp)
                        .height(56.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ModernBlueDark,
                        disabledContainerColor = Color(0xFFB8BECA)
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("AI", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onSave,
                    enabled = canSave && !isRecording,
                    modifier = Modifier
                        .width(86.dp)
                        .height(56.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ModernInk,
                        disabledContainerColor = Color(0xFFB8BECA)
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("创建", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
        }
        }
    }
}

@Composable
private fun DisabledBottomBar(text: String) {
    BottomSurface {
        Button(
            onClick = {},
            enabled = false,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = CircleShape
        ) {
            Text(text)
        }
    }
}

@Composable
private fun ConfirmationBottomBar(
    secondaryText: String,
    primaryText: String,
    primaryEnabled: Boolean = true,
    onSecondary: () -> Unit,
    onPrimary: () -> Unit
) {
    BottomSurface {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onSecondary,
                modifier = Modifier.width(108.dp).height(56.dp),
                shape = CircleShape,
                border = BorderStroke(1.dp, ModernBlue.copy(alpha = 0.24f))
            ) {
                Text(secondaryText, color = ModernBlue, fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = onPrimary,
                enabled = primaryEnabled,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = ModernInk)
            ) {
                Text(primaryText, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun BottomSurface(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = BgPrimary.copy(alpha = 0.96f),
        border = BorderStroke(0.5.dp, ModernLine)
    ) {
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            content()
        }
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit,
    onConfigure: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canConfigureAI = message.contains("DeepSeek") || message.contains("API Key") || message.contains("AI")
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFFF2F2),
        border = BorderStroke(1.dp, Danger.copy(alpha = 0.18f)),
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = message,
                color = Danger,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canConfigureAI) {
                    Text(
                        text = "去 AI 设置",
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onConfigure)
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        color = ModernBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "关闭",
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    color = Danger,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun formatModernDate(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "今天"
        today.plusDays(1) -> "明天"
        else -> date.format(DateTimeFormatter.ofPattern("M月d日"))
    }
}
