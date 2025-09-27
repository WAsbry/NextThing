package com.example.nextthingb1.presentation.screens.createnotificationstrategy

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nextthingb1.domain.model.PresetAudio
import com.example.nextthingb1.util.AudioFileHelper
import com.example.nextthingb1.util.AudioFileInfo

@Composable
fun PresetAudioSelector(
    selectedPresetAudio: PresetAudio?,
    onPresetAudioSelected: (PresetAudio) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "选择预置音效",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF424242),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 按类别分组显示
            PresetAudio.getCategories().forEach { category ->
                Text(
                    text = category,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF666666),
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )

                PresetAudio.getByCategory(category).forEach { audio ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPresetAudioSelected(audio) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = audio == selectedPresetAudio,
                            onClick = { onPresetAudioSelected(audio) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFF71CBF4)
                            )
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                text = audio.displayName,
                                fontSize = 14.sp,
                                color = Color(0xFF424242)
                            )
                            Text(
                                text = audio.description,
                                fontSize = 12.sp,
                                color = Color(0xFF9E9E9E)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomAudioSelector(
    customAudioFileInfo: AudioFileInfo?,
    customAudioName: String,
    onCustomAudioSelected: (AudioFileInfo, String) -> Unit,
    onClearCustomAudio: () -> Unit
) {
    var showNameDialog by remember { mutableStateOf(false) }
    var pendingAudioFileInfo by remember { mutableStateOf<AudioFileInfo?>(null) }
    val context = LocalContext.current

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.data?.let { uri ->
            if (AudioFileHelper.isValidAudioFile(context, uri)) {
                val fileName = AudioFileHelper.getFileName(context, uri)
                val duration = AudioFileHelper.getAudioDuration(context, uri)
                val fileSize = AudioFileHelper.getFileSize(context, uri)
                val mimeType = context.contentResolver.getType(uri)

                val audioFileInfo = AudioFileInfo(
                    uri = uri,
                    fileName = fileName ?: "未知文件",
                    displayName = fileName ?: "未知文件",
                    duration = duration,
                    fileSize = fileSize,
                    mimeType = mimeType
                )

                pendingAudioFileInfo = audioFileInfo
                showNameDialog = true
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "自定义音频文件",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF424242),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (customAudioFileInfo == null) {
                OutlinedButton(
                    onClick = {
                        audioPickerLauncher.launch(AudioFileHelper.createAudioPickerIntent())
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF71CBF4)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF71CBF4))
                ) {
                    Text("选择音频文件")
                }
            } else {
                // 显示已选择的文件信息
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFF71CBF4))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = customAudioName.ifEmpty { "未命名音频" },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF424242)
                                )
                                Text(
                                    text = customAudioFileInfo.fileName,
                                    fontSize = 12.sp,
                                    color = Color(0xFF666666),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                                Row(
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text(
                                        text = customAudioFileInfo.durationText,
                                        fontSize = 10.sp,
                                        color = Color(0xFF9E9E9E)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = customAudioFileInfo.fileSizeText,
                                        fontSize = 10.sp,
                                        color = Color(0xFF9E9E9E)
                                    )
                                }
                            }
                            TextButton(
                                onClick = onClearCustomAudio,
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = Color(0xFFFF5252)
                                )
                            ) {
                                Text("删除", fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 重新选择按钮
                OutlinedButton(
                    onClick = {
                        audioPickerLauncher.launch(AudioFileHelper.createAudioPickerIntent())
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF71CBF4)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF71CBF4))
                ) {
                    Text("重新选择", fontSize = 12.sp)
                }
            }
        }
    }

    // 音频备注名称输入对话框
    if (showNameDialog && pendingAudioFileInfo != null) {
        var nameText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = {
                showNameDialog = false
                pendingAudioFileInfo = null
            },
            title = {
                Text("为音频文件命名")
            },
            text = {
                Column {
                    Text(
                        text = "文件: ${pendingAudioFileInfo!!.fileName}",
                        fontSize = 12.sp,
                        color = Color(0xFF666666),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = nameText,
                        onValueChange = { nameText = it },
                        label = { Text("音频名称") },
                        placeholder = { Text("例如：办公铃声、提醒音等") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onCustomAudioSelected(
                            pendingAudioFileInfo!!,
                            nameText.trim().ifEmpty { pendingAudioFileInfo!!.fileName }
                        )
                        showNameDialog = false
                        pendingAudioFileInfo = null
                        nameText = ""
                    }
                ) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showNameDialog = false
                        pendingAudioFileInfo = null
                        nameText = ""
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }
}