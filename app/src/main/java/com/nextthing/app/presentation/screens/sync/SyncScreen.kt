package com.nextthing.app.presentation.screens.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nextthing.app.data.repository.SyncState
import com.nextthing.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(
    viewModel: SyncViewModel = hiltViewModel(),
    onBackPressed: () -> Unit = {}
) {
    val syncState by viewModel.syncState.collectAsState()
    val syncResult by viewModel.syncResult.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val conflicts by viewModel.conflicts.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("数据同步") },
                navigationIcon = {
                    TextButton(onClick = onBackPressed) {
                        Text("< 返回", color = Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgPrimary)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BgPrimary)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 同步状态卡片
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = BgCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("同步状态", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))

                    when (val state = syncState) {
                        is SyncState.Idle -> {
                            Text("空闲", fontSize = 14.sp, color = TextSecondary)
                        }
                        is SyncState.Syncing -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("同步中...", fontSize = 14.sp, color = Primary)
                            }
                        }
                        is SyncState.Success -> {
                            val date = java.time.Instant.ofEpochMilli(state.timestamp)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDateTime()
                            Text("上次同步: ${date.monthValue}/${date.dayOfMonth} ${date.hour}:${date.minute.toString().padStart(2, '0')}",
                                fontSize = 14.sp, color = Success)
                        }
                        is SyncState.Error -> {
                            Text("同步失败: ${state.message}", fontSize = 14.sp, color = Danger)
                        }
                    }

                    // 同步结果
                    syncResult?.let { result ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("上传 ${result.uploadedTasks} 条 · 下载 ${result.downloadedTasks} 条 · 冲突 ${result.conflicts.size} 条",
                            fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }

            // 操作按钮
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { viewModel.sync() },
                    enabled = !isSyncing,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text(if (isSyncing) "同步中..." else "立即同步", color = androidx.compose.ui.graphics.Color.White)
                }

                OutlinedButton(
                    onClick = { viewModel.fullSync() },
                    enabled = !isSyncing,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("全量同步", color = Primary)
                }
            }

            // 冲突列表
            if (conflicts.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = BgCard),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("冲突 (${conflicts.size})", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Warning)
                        Spacer(modifier = Modifier.height(8.dp))

                        conflicts.forEach { conflict ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(conflict.taskTitle, fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                                    Text(conflict.conflictType.name, fontSize = 12.sp, color = TextSecondary)
                                }
                                TextButton(onClick = { viewModel.resolveConflictUseServer(conflict.taskId) }) {
                                    Text("用服务端", fontSize = 12.sp, color = Primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
