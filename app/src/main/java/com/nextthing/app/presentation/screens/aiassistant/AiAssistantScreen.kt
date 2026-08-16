package com.nextthing.app.presentation.screens.aiassistant

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.nextthing.app.domain.model.AITaskParseResult
import com.nextthing.app.domain.model.TaskImportanceUrgency
import com.nextthing.app.presentation.theme.BgCard
import com.nextthing.app.presentation.theme.BgPrimary
import com.nextthing.app.presentation.theme.BgSecondary
import com.nextthing.app.presentation.theme.Border
import com.nextthing.app.presentation.theme.Danger
import com.nextthing.app.presentation.theme.Primary
import com.nextthing.app.presentation.theme.Success
import com.nextthing.app.presentation.theme.TextMuted
import com.nextthing.app.presentation.theme.TextPrimary
import com.nextthing.app.presentation.theme.TextSecondary
import java.time.format.DateTimeFormatter

@Composable
fun AiAssistantScreen(
    viewModel: AiAssistantViewModel = hiltViewModel(),
    onBackPressed: () -> Unit,
    onNavigateToAIConfig: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
    ) {
        AiAssistantTopBar(
            routeStatus = uiState.routeStatusText,
            onBackPressed = onBackPressed,
            onNavigateToAIConfig = onNavigateToAIConfig
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AiInputCard(
                text = uiState.inputText,
                isParsing = uiState.isParsing,
                onTextChange = viewModel::updateInput,
                onParse = viewModel::parseInput
            )

            uiState.errorMessage?.let {
                InlineMessage(text = it, color = Danger, onDismiss = viewModel::clearMessage)
            }
            uiState.statusMessage?.let {
                InlineMessage(text = it, color = Success, onDismiss = viewModel::clearMessage)
            }

            if (uiState.parseResults.isNotEmpty()) {
                ParsedResultsSection(
                    results = uiState.parseResults,
                    selectedIndexes = uiState.selectedIndexes,
                    isSaving = uiState.isSaving,
                    onToggleSelection = viewModel::toggleSelection,
                    onSaveSelected = viewModel::saveSelectedTasks
                )
            } else {
                EmptyAssistantState()
            }
        }
    }
}

@Composable
private fun AiAssistantTopBar(
    routeStatus: String,
    onBackPressed: () -> Unit,
    onNavigateToAIConfig: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgCard)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackPressed) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = TextPrimary
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "AI 助手",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = routeStatus,
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        OutlinedButton(
            onClick = onNavigateToAIConfig,
            border = BorderStroke(1.dp, Border)
        ) {
            Text(text = "设置", color = TextPrimary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun AiInputCard(
    text: String,
    isParsing: Boolean,
    onTextChange: (String) -> Unit,
    onParse: () -> Unit
) {
    Surface(
        color = BgCard,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Border)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "自然语言输入",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp),
                placeholder = { Text("例如：周三下午三点提醒我整理 NextThing 项目问题") },
                enabled = !isParsing,
                minLines = 4,
                maxLines = 6,
                shape = RoundedCornerShape(10.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = onParse,
                    enabled = text.isNotBlank() && !isParsing,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    if (isParsing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(text = "解析", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun ParsedResultsSection(
    results: List<AITaskParseResult>,
    selectedIndexes: Set<Int>,
    isSaving: Boolean,
    onToggleSelection: (Int) -> Unit,
    onSaveSelected: () -> Unit
) {
    Surface(
        color = BgCard,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Border)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "解析结果",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "已选 ${selectedIndexes.size}/${results.size}",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            results.forEachIndexed { index, result ->
                ParsedTaskCard(
                    result = result,
                    checked = index in selectedIndexes,
                    onClick = { onToggleSelection(index) }
                )
            }

            Button(
                onClick = onSaveSelected,
                enabled = selectedIndexes.isNotEmpty() && !isSaving,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = "保存选中任务", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ParsedTaskCard(
    result: AITaskParseResult,
    checked: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (checked) Primary.copy(alpha = 0.08f) else BgSecondary)
            .border(1.dp, if (checked) Primary.copy(alpha = 0.35f) else Border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Checkbox(checked = checked, onCheckedChange = { onClick() })
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = result.title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            result.description?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = result.toMetaText(),
                color = TextMuted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun InlineMessage(
    text: String,
    color: Color,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .clickable(onClick = onDismiss)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.size(10.dp))
        Text(text = text, color = TextPrimary, fontSize = 13.sp)
    }
}

@Composable
private fun EmptyAssistantState() {
    Surface(
        color = BgCard,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "待解析",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(text = "暂无解析结果", color = TextSecondary, fontSize = 13.sp)
        }
    }
}

private fun AITaskParseResult.toMetaText(): String {
    val formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")
    val parts = listOfNotNull(
        dueDate?.format(formatter),
        categoryName,
        importance?.toShortLabel(),
        if (confidence > 0f) "置信度 ${(confidence * 100).toInt()}%" else null
    )
    return if (parts.isEmpty()) "未设置时间和分类" else parts.joinToString(" · ")
}

private fun TaskImportanceUrgency.toShortLabel(): String {
    return when (this) {
        TaskImportanceUrgency.IMPORTANT_URGENT -> "重要紧急"
        TaskImportanceUrgency.IMPORTANT_NOT_URGENT -> "重要不紧急"
        TaskImportanceUrgency.NOT_IMPORTANT_URGENT -> "不重要紧急"
        TaskImportanceUrgency.NOT_IMPORTANT_NOT_URGENT -> "不重要不紧急"
    }
}
