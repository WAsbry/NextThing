package com.nextthing.app.presentation.screens.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextthing.app.data.preferences.AIPreferences
import com.nextthing.app.data.preferences.AIProvider
import com.nextthing.app.data.service.AICompletionClient
import com.nextthing.app.data.service.AIRouteMode
import com.nextthing.app.presentation.components.AppToastHost
import com.nextthing.app.presentation.components.AppToastType
import com.nextthing.app.presentation.components.rememberAppToastHostState
import com.nextthing.app.presentation.theme.BgCard
import com.nextthing.app.presentation.theme.BgPrimary
import com.nextthing.app.presentation.theme.Border
import com.nextthing.app.presentation.theme.Primary
import com.nextthing.app.presentation.theme.TextPrimary
import com.nextthing.app.presentation.theme.TextSecondary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named

private val AiStatusBar = Color(0xFFF4EFFF)
private val AiBgStart = Color(0xFFF4EFFF)
private val AiBgMid = Color(0xFFF7F3FF)
private val AiBgEnd = Color(0xFFFBFAFF)
private val AiPurple = Color(0xFF7057F5)
private val AiPurple2 = Color(0xFFB06DFF)
private val AiCyan = Color(0xFF16A8B8)
private val AiInk = Color(0xFF202331)
private val AiDeep = Color(0xFF2F2850)
private val AiSub = Color(0xFF656B78)
private val AiMuted = Color(0xFFA6ACB8)
private val AiLine = Color(0xFFE7E9F1)
private val AiDanger = Color(0xFFE64B55)
private val AiDangerBg = Color(0xFFFFF1F3)
private val AiSuccess = Color(0xFF20A875)

enum class AIValidationState {
    Idle,
    Checking,
    Success,
    Failed
}

data class AIConfigUiState(
    val draftApiKey: String = "",
    val savedApiKey: String = "",
    val isDraftDirty: Boolean = false,
    val validationState: AIValidationState = AIValidationState.Idle,
    val validationMessage: String? = null,
    val actionMessage: String? = null,
    val actionEventId: Long = 0L,
    val routeMode: AIRouteMode = AIRouteMode.Unavailable,
    val routeLabel: String = "AI 暂不可用",
    val routeDescription: String = "填写 DeepSeek API Key 后，任务解析、简报、周报等 AI 能力会直接使用外接模型。"
) {
    val isEnabled: Boolean
        get() = savedApiKey.isNotBlank()

    val hasUnsavedChanges: Boolean
        get() = draftApiKey.trim() != savedApiKey.trim()

    val isAiAvailable: Boolean
        get() = routeMode != AIRouteMode.Unavailable
}

@HiltViewModel
class AIConfigViewModel @Inject constructor(
    private val aiPreferences: AIPreferences,
    private val aiCompletionClient: AICompletionClient,
    @Named("ai") private val aiHttpClient: OkHttpClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(AIConfigUiState())
    val uiState: StateFlow<AIConfigUiState> = _uiState.asStateFlow()

    init {
        observePreferences()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            aiPreferences.apiKey.collect { apiKey ->
                _uiState.update { state ->
                    state.copy(
                        savedApiKey = apiKey,
                        draftApiKey = if (state.isDraftDirty) state.draftApiKey else apiKey,
                        isDraftDirty = state.isDraftDirty && state.draftApiKey.trim() != apiKey.trim()
                    )
                }
                refreshRouteStatus()
            }
        }
    }

    private fun refreshRouteStatus() {
        viewModelScope.launch {
            val status = aiCompletionClient.routeStatus()
            _uiState.update {
                it.copy(
                    routeMode = status.mode,
                    routeLabel = status.userLabel,
                    routeDescription = when (status.mode) {
                        AIRouteMode.ExternalProvider ->
                            "当前优先使用你本机保存的 API Key，直接调用外接模型。"
                        AIRouteMode.BackendFallback ->
                            "当前未配置本机 API Key，会回退到登录态下的服务端 AI。"
                        AIRouteMode.Unavailable ->
                            "当前既没有 API Key，也没有可用登录态；AI 能力暂不可用。"
                    }
                )
            }
        }
    }

    fun updateApiKey(apiKey: String) {
        _uiState.update {
            it.copy(
                draftApiKey = apiKey,
                isDraftDirty = apiKey.trim() != it.savedApiKey.trim()
            )
        }
    }

    fun saveConfig() {
        viewModelScope.launch {
            val state = _uiState.value
            val apiKey = state.draftApiKey.trim()
            if (apiKey.isBlank()) {
                showToast("请先填写 DeepSeek API Key")
                return@launch
            }

            _uiState.update {
                it.copy(
                    validationState = AIValidationState.Checking,
                    validationMessage = "正在连接 DeepSeek，校验 API Key 是否可用"
                )
            }

            val startedAt = System.currentTimeMillis()
            val result = validateDeepSeekApiKey(apiKey)
            val elapsed = System.currentTimeMillis() - startedAt
            if (elapsed < 900L) delay(900L - elapsed)

            result.fold(
                onSuccess = {
                    aiPreferences.setProvider(AIProvider.DEEPSEEK)
                    aiPreferences.setApiKey(apiKey)
                    aiPreferences.setModel("")
                    _uiState.update {
                        it.copy(
                            draftApiKey = apiKey,
                            savedApiKey = apiKey,
                            isDraftDirty = false,
                            validationState = AIValidationState.Success,
                            validationMessage = "校验通过，AI 增强已启用"
                        )
                    }
                    delay(700L)
                    _uiState.update {
                        if (it.validationState == AIValidationState.Success) {
                            it.copy(validationState = AIValidationState.Idle, validationMessage = null)
                        } else {
                            it
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            validationState = AIValidationState.Failed,
                            validationMessage = error.message ?: "校验失败，请稍后再试"
                        )
                    }
                }
            )
        }
    }

    fun disableAI() {
        viewModelScope.launch {
            aiPreferences.setProvider(AIProvider.DEEPSEEK)
            aiPreferences.setApiKey("")
            aiPreferences.setModel("")
            _uiState.update {
                it.copy(
                    draftApiKey = "",
                    savedApiKey = "",
                    isDraftDirty = false,
                    validationState = AIValidationState.Idle,
                    validationMessage = null
                )
            }
            showToast("AI 增强已禁用")
        }
    }

    fun dismissValidationDialog() {
        _uiState.update {
            it.copy(validationState = AIValidationState.Idle, validationMessage = null)
        }
    }

    fun clearActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }

    private fun showToast(message: String) {
        _uiState.update {
            it.copy(
                actionMessage = message,
                actionEventId = it.actionEventId + 1
            )
        }
    }

    private suspend fun validateDeepSeekApiKey(apiKey: String): Result<Unit> = withContext(Dispatchers.IO) {
        val requestBody = """
            {
              "model": "${AIProvider.DEEPSEEK.defaultModel}",
              "messages": [
                {
                  "role": "user",
                  "content": "ping"
                }
              ],
              "max_tokens": 1,
              "temperature": 0
            }
        """.trimIndent().toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url("${AIProvider.DEEPSEEK.baseUrl}chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        try {
            aiHttpClient.newCall(request).execute().use { response ->
                when {
                    response.isSuccessful -> Result.success(Unit)
                    response.code == 401 -> Result.failure(IllegalStateException("API Key 无效或已过期，请检查后重试"))
                    response.code == 402 || response.code == 429 -> Result.failure(IllegalStateException("DeepSeek 额度或调用频率受限，请检查账号状态"))
                    else -> Result.failure(IllegalStateException("DeepSeek 返回异常（${response.code}），请稍后重试"))
                }
            }
        } catch (_: IOException) {
            Result.failure(IllegalStateException("网络连接异常，请检查网络后重试"))
        } catch (_: Exception) {
            Result.failure(IllegalStateException("校验失败，请稍后再试"))
        }
    }
}

private fun Modifier.aiPageBackground(): Modifier = drawBehind {
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(AiBgStart, AiBgMid, AiBgEnd),
            startX = 0f,
            endX = size.width
        )
    )
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(AiPurple2.copy(alpha = 0.18f), Color.Transparent),
            center = Offset(size.width * 0.14f, size.height * 0.04f),
            radius = size.width * 0.45f
        )
    )
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(AiPurple.copy(alpha = 0.12f), Color.Transparent),
            center = Offset(size.width * 0.92f, size.height * 0.18f),
            radius = size.width * 0.48f
        )
    )
}

private fun Modifier.aiAuraFrame(enabled: Boolean, pulse: Float, topOffsetPx: Float): Modifier = drawBehind {
    if (!enabled) return@drawBehind
    val top = topOffsetPx.coerceIn(0f, size.height * 0.20f)
    val height = size.height - top
    if (height <= 0f) return@drawBehind

    val edge = 58.dp.toPx()
    val alpha = 0.58f * pulse

    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF36DFF).copy(alpha = alpha * 0.52f),
                Color(0xFF8C79FF).copy(alpha = alpha * 0.24f),
                Color.Transparent
            ),
            startY = top,
            endY = top + edge
        ),
        topLeft = Offset(0f, top),
        size = Size(size.width, edge)
    )
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color(0xFF39DAFF).copy(alpha = alpha * 0.30f),
                Color(0xFFFF75DA).copy(alpha = alpha * 0.54f)
            ),
            startY = size.height - edge,
            endY = size.height
        ),
        topLeft = Offset(0f, size.height - edge),
        size = Size(size.width, edge)
    )
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color(0xFFB25FFF).copy(alpha = alpha * 0.62f),
                Color(0xFF40D2FF).copy(alpha = alpha * 0.25f),
                Color.Transparent
            ),
            startX = 0f,
            endX = edge
        ),
        topLeft = Offset(0f, top),
        size = Size(edge, height)
    )
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                Color(0xFFFF6FD7).copy(alpha = alpha * 0.24f),
                Color(0xFF2CE0FF).copy(alpha = alpha * 0.64f)
            ),
            startX = size.width - edge,
            endX = size.width
        ),
        topLeft = Offset(size.width - edge, top),
        size = Size(edge, height)
    )
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFED63FF).copy(alpha = alpha * 0.84f), Color.Transparent),
            center = Offset(0f, top),
            radius = edge * 1.55f
        )
    )
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF33E4FF).copy(alpha = alpha * 0.78f), Color.Transparent),
            center = Offset(size.width, top),
            radius = edge * 1.55f
        )
    )
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF766EFF).copy(alpha = alpha * 0.70f), Color.Transparent),
            center = Offset(0f, size.height),
            radius = edge * 1.55f
        )
    )
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFF73D8).copy(alpha = alpha * 0.78f), Color.Transparent),
            center = Offset(size.width, size.height),
            radius = edge * 1.55f
        )
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun AIConfigScreen(
    onBackPressed: () -> Unit,
    viewModel: AIConfigViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val toastHostState = rememberAppToastHostState()

    LaunchedEffect(uiState.actionEventId) {
        uiState.actionMessage?.let { message ->
            toastHostState.showDebounced(message, AppToastType.Info)
            viewModel.clearActionMessage()
        }
    }

    Scaffold(
        containerColor = BgPrimary,
        topBar = { AIConfigTopBar(onBackPressed = onBackPressed) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BgPrimary)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
            ) {
                item {
                    CurrentAiStatusCard(
                        label = uiState.routeLabel,
                        description = uiState.routeDescription,
                        available = uiState.isAiAvailable,
                        usingLocalKey = uiState.isEnabled
                    )
                }

                item {
                    SectionTitle(title = "本机 API Key（可选）", trailing = if (uiState.isEnabled) "已启用" else "可选")
                    ConfigCard {
                        ConfigField(
                            label = "DeepSeek API Key",
                            value = uiState.draftApiKey,
                            placeholder = "sk-...",
                            isSecret = true,
                            editable = !uiState.isEnabled,
                            onValueChange = viewModel::updateApiKey
                        )
                        KeyHelpText(isLocked = uiState.isEnabled)
                    }
                }

                item {
                    SectionTitle(title = "AI 可用于", trailing = "")
                    AiCapabilitySummary()
                }

                item {
                    PrivacyNote()
                    if (uiState.isEnabled && !uiState.hasUnsavedChanges) {
                        DisableAIButton(onDisable = viewModel::disableAI)
                    } else {
                        SaveButton(
                            onSave = viewModel::saveConfig,
                            enabled = uiState.draftApiKey.trim().isNotBlank() &&
                                uiState.hasUnsavedChanges &&
                                uiState.validationState != AIValidationState.Checking
                        )
                    }
                }
            }

            AIValidationDialog(
                state = uiState.validationState,
                message = uiState.validationMessage,
                onRetry = viewModel::saveConfig,
                onDismiss = viewModel::dismissValidationDialog
            )

            AppToastHost(
                hostState = toastHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AIConfigTopBar(onBackPressed: () -> Unit) {
    TopAppBar(
        title = {
            Text("AI 智能助手", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        },
        navigationIcon = {
            IconButton(onClick = onBackPressed) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = TextPrimary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = BgPrimary)
    )
}

@Composable
private fun CurrentAiStatusCard(
    label: String,
    description: String,
    available: Boolean,
    usingLocalKey: Boolean
) {
    ConfigCard {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (available) Primary else AiMuted)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (usingLocalKey) "本机 Key 已启用" else label,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (usingLocalKey) "优先使用本机保存的 DeepSeek API Key" else description,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }
        }
    }
}

@Composable
private fun AIHeroCard(enabled: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    listOf(AiPurple, Color(0xFF9D65FF), AiCyan)
                )
            )
            .drawBehind {
                drawCircle(
                    color = Color.White.copy(alpha = 0.22f),
                    radius = 72.dp.toPx(),
                    center = Offset(size.width * 0.88f, 10.dp.toPx())
                )
            }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                        .border(1.dp, Color.White.copy(alpha = 0.24f), RoundedCornerShape(17.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("AI", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
                }
                Text(
                    text = "DeepSeek AI 增强",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 10.dp)
                )
                Text(
                    text = "粘贴 DeepSeek API Key，通过校验后启用 AI 增强。",
                    color = Color.White.copy(alpha = 0.80f),
                    fontSize = 12.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(top = 5.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = Color.White.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
            ) {
                Text(
                    text = if (enabled) "已启用" else "未配置",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, trailing: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 2.dp, end = 2.dp, top = 14.dp, bottom = 8.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(trailing, color = AiMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ConfigCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(BgCard)
            .border(1.dp, Border, RoundedCornerShape(8.dp))
    ) {
        content()
    }
}

@Composable
private fun ConfigField(
    label: String,
    value: String,
    placeholder: String,
    trailingText: String? = null,
    isSecret: Boolean = false,
    editable: Boolean = true,
    onValueChange: (String) -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    var fieldValue by remember(value) {
        mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length)))
    }

    LaunchedEffect(value) {
        if (value != fieldValue.text) {
            fieldValue = TextFieldValue(text = value, selection = TextRange(value.length))
        }
    }

    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color(0xFF6F6684), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            trailingText?.let {
                Text(it, color = AiMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        OutlinedTextField(
            value = fieldValue,
            onValueChange = {
                if (!editable) return@OutlinedTextField
                val nextValue = it.copy(selection = TextRange(it.text.length))
                fieldValue = nextValue
                onValueChange(nextValue.text)
            },
            enabled = editable,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 7.dp),
            placeholder = { Text(placeholder, color = AiMuted, fontSize = 13.sp) },
            singleLine = true,
            visualTransformation = if (isSecret && !isVisible) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            trailingIcon = if (isSecret) {
                {
                    IconButton(onClick = { isVisible = !isVisible }) {
                        Icon(
                            imageVector = if (isVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (isVisible) "隐藏 API Key" else "显示 API Key",
                            tint = Primary
                        )
                    }
                }
            } else null,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = Border,
                focusedContainerColor = Color(0xFFF6F7FC).copy(alpha = 0.92f),
                unfocusedContainerColor = Color(0xFFF6F7FC).copy(alpha = 0.92f),
                disabledContainerColor = Color(0xFFF6F7FC).copy(alpha = 0.92f),
                focusedTextColor = AiDeep,
                unfocusedTextColor = AiDeep,
                disabledTextColor = AiDeep,
                disabledBorderColor = AiLine,
                disabledPlaceholderColor = AiMuted,
                cursorColor = Primary
            )
        )
    }
}

@Composable
private fun KeyHelpText(isLocked: Boolean) {
    Text(
        text = if (isLocked) {
            "AI 增强已启用。为避免误改，API Key 已锁定；如需更换，请先在底部禁用 AI 增强。"
        } else {
            "配置后优先使用本机 Key；默认模型为 DeepSeek · deepseek-v4-flash。"
        },
        modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 13.dp),
        color = AiSub,
        fontSize = 11.sp,
        lineHeight = 17.sp
    )
}

@Composable
private fun AiCapabilitySummary() {
    ConfigCard {
        Text(
            text = "任务解析 · 统计洞察 · 本周周报 · 早晚报摘要",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
    }
}

@Composable
private fun AdvancedDefaultsCard() {
    ConfigCard {
        AdvancedDefaultRow(label = "服务提供商", value = AIProvider.DEEPSEEK.displayName)
        RowDivider()
        AdvancedDefaultRow(label = "默认模型", value = AIProvider.DEEPSEEK.defaultModel)
    }
}

@Composable
private fun AIRouteStatusCard(
    label: String,
    description: String,
    enabled: Boolean
) {
    ConfigCard {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (enabled) AiSuccess else AiMuted)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(label, color = AiDeep, fontSize = 13.sp, fontWeight = FontWeight.Black)
                Text(description, color = AiSub, fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
    }
}
@Composable
private fun AdvancedDefaultRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = AiSub, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
        Text(value, color = AiDeep, fontSize = 13.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun ScopeGridCard() {
    ConfigCard {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScopeItem("统计洞察", "行为模式、逾期风险与效率建议", Modifier.weight(1f))
                ScopeItem("本周周报", "摘要、亮点、待改进和下周建议", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScopeItem("任务拆解", "把复杂任务拆成可执行步骤", Modifier.weight(1f))
                ScopeItem("早晚报摘要", "结合任务状态生成提醒内容", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ScopeItem(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(Color.White.copy(alpha = 0.72f))
            .border(1.dp, AiPurple.copy(alpha = 0.09f), RoundedCornerShape(15.dp))
            .padding(10.dp)
    ) {
        Text(title, color = AiDeep, fontSize = 13.sp, fontWeight = FontWeight.Black)
        Text(
            subtitle,
            color = AiSub,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            modifier = Modifier.padding(top = 5.dp)
        )
    }
}

@Composable
private fun PrivacyNote() {
    Text(
        text = "API Key 仅保存在本机设备，用于直接调用 DeepSeek。",
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(BgCard)
            .border(1.dp, Border, RoundedCornerShape(8.dp))
            .padding(horizontal = 13.dp, vertical = 12.dp),
        color = TextSecondary,
        fontSize = 11.sp,
        lineHeight = 18.sp
    )
}

@Composable
private fun SaveButton(onSave: () -> Unit, enabled: Boolean) {
    Button(
        onClick = onSave,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .height(48.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Primary,
            contentColor = Color.White,
            disabledContainerColor = Primary.copy(alpha = 0.38f),
            disabledContentColor = Color.White.copy(alpha = 0.78f)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
    ) {
        Text("验证并保存", fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DisableAIButton(onDisable: () -> Unit) {
    Button(
        onClick = onDisable,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .height(46.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AiDangerBg, contentColor = AiDanger),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
    ) {
        Text("移除本机 Key", fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AIValidationDialog(
    state: AIValidationState,
    message: String?,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    if (state == AIValidationState.Idle) return

    val canDismiss = state == AIValidationState.Failed
    Dialog(
        onDismissRequest = { if (canDismiss) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = canDismiss,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color.White.copy(alpha = 0.96f),
            border = BorderStroke(1.dp, AiPurple.copy(alpha = 0.12f)),
            shadowElevation = 18.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ValidationPulseIcon(state = state)
                Text(
                    text = when (state) {
                        AIValidationState.Checking -> "正在校验 API Key"
                        AIValidationState.Success -> "校验成功"
                        AIValidationState.Failed -> "校验失败"
                        AIValidationState.Idle -> ""
                    },
                    color = AiDeep,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 14.dp)
                )
                Text(
                    text = message.orEmpty(),
                    color = AiSub,
                    fontSize = 12.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )

                if (state == AIValidationState.Checking) {
                    Text(
                        text = "请稍等，校验通过后会自动启用 AI 增强。",
                        color = AiMuted,
                        fontSize = 11.sp,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                if (state == AIValidationState.Failed) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("取消", color = AiSub, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onRetry,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AiPurple, contentColor = Color.White),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
                        ) {
                            Text("重新校验", fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ValidationPulseIcon(state: AIValidationState) {
    val transition = rememberInfiniteTransition(label = "ai_validation_pulse")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1250, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )
    val color = when (state) {
        AIValidationState.Success -> AiSuccess
        AIValidationState.Failed -> AiDanger
        else -> AiPurple
    }
    val label = when (state) {
        AIValidationState.Success -> "✓"
        AIValidationState.Failed -> "!"
        else -> "AI"
    }

    Box(
        modifier = Modifier
            .size(76.dp)
            .drawBehind {
                val baseRadius = 26.dp.toPx()
                val pulseRadius = baseRadius + 18.dp.toPx() * pulse
                drawCircle(
                    color = color.copy(alpha = if (state == AIValidationState.Checking) 0.16f * (1f - pulse) else 0.08f),
                    radius = pulseRadius
                )
                drawCircle(
                    color = color.copy(alpha = 0.12f),
                    radius = baseRadius + 8.dp.toPx()
                )
                drawCircle(
                    color = color,
                    radius = baseRadius
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun RowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(AiLine.copy(alpha = 0.86f))
    )
}
