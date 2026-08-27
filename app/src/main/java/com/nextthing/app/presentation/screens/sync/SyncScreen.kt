package com.nextthing.app.presentation.screens.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nextthing.app.data.repository.SyncState
import com.nextthing.app.domain.model.SyncResult
import com.nextthing.app.presentation.theme.BgCard
import com.nextthing.app.presentation.theme.BgPrimary
import com.nextthing.app.presentation.theme.Border
import com.nextthing.app.presentation.theme.Danger
import com.nextthing.app.presentation.theme.Primary
import com.nextthing.app.presentation.theme.Success
import com.nextthing.app.presentation.theme.TextPrimary
import com.nextthing.app.presentation.theme.TextSecondary

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(
    viewModel: SyncViewModel = hiltViewModel(),
    onBackPressed: () -> Unit = {},
    onOpenConflicts: () -> Unit = {}
) {
    val syncState by viewModel.syncState.collectAsState()
    val syncResult by viewModel.syncResult.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val conflicts by viewModel.conflicts.collectAsState()
    var showRecoveryConfirm by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = BgPrimary,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "数据同步",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgCard)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgPrimary)
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text("同步状态", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            SyncStatusCard(
                state = syncState,
                isSyncing = isSyncing,
                onSync = viewModel::sync
            )

            if (syncResult != null) {
                SyncResultCard(syncResult!!)
            }

            if (conflicts.isNotEmpty()) {
                ConflictEntryCard(count = conflicts.size, onClick = onOpenConflicts)
            }

            Spacer(Modifier.height(15.dp))
            Text("恢复云端数据", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            RecoveryEntry(onClick = { showRecoveryConfirm = true })
        }
    }

    if (showRecoveryConfirm) {
        RecoveryConfirmationDialog(
            isSyncing = isSyncing,
            onDismiss = { showRecoveryConfirm = false },
            onConfirm = {
                showRecoveryConfirm = false
                viewModel.fullSync()
            }
        )
        /*
        AlertDialog(
            onDismissRequest = { showRecoveryConfirm = false },
            title = { Text("恢复云端数据", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "这会清空本机已同步的任务，并重新下载云端数据。若本机存在未上传内容，恢复将被阻止。",
                    color = TextSecondary,
                    lineHeight = 21.sp
                )
            },
            confirmButton = {
                TextButtonPrimary(text = "继续恢复") {
                    showRecoveryConfirm = false
                    viewModel.fullSync()
                }
            },
            dismissButton = { TextButtonSecondary(text = "取消") { showRecoveryConfirm = false } },
            containerColor = BgCard,
            shape = RoundedCornerShape(8.dp)
        )
        */
    }
}

@Composable
private fun RecoveryConfirmationDialog(
    isSyncing: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isSyncing) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.WarningAmber,
                    contentDescription = null,
                    tint = Danger,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("从云端恢复数据？", fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "将删除本机已同步的任务，并重新下载云端版本。",
                    color = TextSecondary,
                    lineHeight = 21.sp
                )
                Text(
                    "若存在未上传的任务或分类，本次恢复不会执行。请先完成“立即同步”。",
                    color = Danger,
                    lineHeight = 21.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isSyncing) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Danger
                    )
                } else {
                    Text("确认恢复", color = Danger, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSyncing) {
                Text("取消", color = TextSecondary)
            }
        },
        containerColor = BgCard,
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
private fun SyncTopBar(onBackPressed: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = BgCard, shadowElevation = 1.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackPressed, modifier = Modifier.size(40.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = TextPrimary, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text("数据同步", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
    }
}

@Composable
private fun SyncStatusCard(
    state: SyncState,
    isSyncing: Boolean,
    onSync: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().border(1.dp, Border, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp), color = BgCard
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (state) {
                is SyncState.Idle -> {
                    Text("暂未同步", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text("首次同步后，这里会显示最近一次成功时间", fontSize = 13.sp, color = TextSecondary)
                }
                is SyncState.Syncing -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Primary)
                        Spacer(Modifier.width(8.dp))
                        Text("正在同步数据", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Primary)
                    }
                    Text("请保持网络连接，不要重复提交", fontSize = 13.sp, color = TextSecondary)
                }
                is SyncState.Success -> {
                    Text("同步正常", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Success)
                    Text("上次同步：${formatTimestamp(state.timestamp)}", fontSize = 13.sp, color = TextSecondary)
                }
                is SyncState.Error -> {
                    Text("同步失败", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Danger)
                    Text(state.message, fontSize = 13.sp, color = TextSecondary)
                }
            }
            Button(
                onClick = onSync,
                enabled = !isSyncing,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text(if (isSyncing) "同步中…" else if (state is SyncState.Error) "重试同步" else "立即同步", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SyncResultCard(result: SyncResult) {
    Surface(
        modifier = Modifier.fillMaxWidth().border(1.dp, Border, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp), color = BgCard
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("本次同步结果", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ResultMetric("上传", result.uploadedTasks, Primary)
                ResultMetric("下载", result.downloadedTasks, Success)
                ResultMetric("冲突", result.conflicts.size, if (result.conflicts.isEmpty()) TextSecondary else Danger)
            }
        }
    }
}

@Composable
private fun ResultMetric(label: String, value: Int, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, fontSize = 13.sp, color = TextSecondary)
        Text("$value 项", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun ConflictEntryCard(count: Int, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().border(1.dp, Danger.copy(alpha = 0.30f), RoundedCornerShape(8.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp), color = BgCard
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("存在冲突", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text("有 $count 项需要你决定保留本地还是云端版本", fontSize = 13.sp, color = TextSecondary)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun RecoveryEntry(onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().border(1.dp, Border, RoundedCornerShape(8.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp), color = BgCard
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("从云端恢复", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text("重新下载云端数据，覆盖本机已同步内容", fontSize = 13.sp, color = TextSecondary)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun TextButtonPrimary(text: String, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick) { Text(text, color = Primary) }
}

@Composable
private fun TextButtonSecondary(text: String, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick) { Text(text, color = TextSecondary) }
}

private fun formatTimestamp(timestamp: Long): String {
    val dateTime = java.time.Instant.ofEpochMilli(timestamp)
        .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
    return "%d月%d日 %02d:%02d".format(dateTime.monthValue, dateTime.dayOfMonth, dateTime.hour, dateTime.minute)
}
