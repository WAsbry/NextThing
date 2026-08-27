package com.nextthing.app.presentation.screens.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nextthing.app.domain.model.SyncConflict
import com.nextthing.app.presentation.theme.BgCard
import com.nextthing.app.presentation.theme.BgPrimary
import com.nextthing.app.presentation.theme.Border
import com.nextthing.app.presentation.theme.Primary
import com.nextthing.app.presentation.theme.TextPrimary
import com.nextthing.app.presentation.theme.TextSecondary

@Composable
fun SyncConflictScreen(
    viewModel: SyncViewModel = hiltViewModel(),
    onBackPressed: () -> Unit
) {
    val conflicts by viewModel.conflicts.collectAsState()
    Column(Modifier.fillMaxSize().background(BgPrimary)) {
        Surface(modifier = Modifier.fillMaxWidth(), color = BgCard, shadowElevation = 1.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackPressed, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = TextPrimary, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text("处理冲突", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        }
        if (conflicts.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("暂无待处理冲突", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                Text("同步数据保持一致", fontSize = 14.sp, color = TextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                item {
                    Text("以下任务在本机与云端均有修改，请选择保留的版本。", fontSize = 14.sp, color = TextSecondary)
                    Spacer(Modifier.height(10.dp))
                }
                items(conflicts, key = { it.taskId }) { conflict ->
                    ConflictCard(
                        conflict = conflict,
                        onKeepLocal = { viewModel.resolveConflictUseLocal(conflict.taskId) },
                        onUseServer = { viewModel.resolveConflictUseServer(conflict.taskId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConflictCard(conflict: SyncConflict, onKeepLocal: () -> Unit, onUseServer: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().border(1.dp, Border, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp), color = BgCard
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(conflict.taskTitle, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text("本机与云端都发生了修改", fontSize = 13.sp, color = TextSecondary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onKeepLocal, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                    Text("保留本地", color = Primary)
                }
                Button(
                    onClick = onUseServer,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) { Text("使用云端") }
            }
        }
    }
}
