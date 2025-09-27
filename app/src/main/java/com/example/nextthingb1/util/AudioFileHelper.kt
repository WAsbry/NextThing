package com.example.nextthingb1.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.File

/**
 * 音频文件处理工具类
 */
object AudioFileHelper {

    /**
     * 支持的音频格式
     */
    val SUPPORTED_AUDIO_TYPES = arrayOf(
        "audio/mpeg",    // MP3
        "audio/mp4",     // MP4/AAC
        "audio/ogg",     // OGG
        "audio/wav",     // WAV
        "audio/x-wav",   // WAV (alternative)
        "audio/3gpp",    // 3GP
        "audio/amr",     // AMR
        "audio/flac"     // FLAC
    )

    /**
     * 创建音频文件选择Intent
     */
    fun createAudioPickerIntent(): Intent {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_MIME_TYPES, SUPPORTED_AUDIO_TYPES)
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_LOCAL_ONLY, true)
        }
        return Intent.createChooser(intent, "选择音频文件")
    }

    /**
     * 从Uri获取文件名
     */
    fun getFileName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else {
                    // 备用方案：从URI路径中提取文件名
                    uri.lastPathSegment?.substringAfterLast("/")
                }
            }
        } catch (e: Exception) {
            uri.lastPathSegment?.substringAfterLast("/")
        }
    }

    /**
     * 从Uri获取音频时长（毫秒）
     */
    fun getAudioDuration(context: Context, uri: Uri): Long? {
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(MediaStore.Audio.Media.DURATION),
                null,
                null,
                null
            )?.use { cursor ->
                val durationIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
                if (durationIndex != -1 && cursor.moveToFirst()) {
                    cursor.getLong(durationIndex)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 从Uri获取文件大小（字节）
     */
    fun getFileSize(context: Context, uri: Uri): Long? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
                if (sizeIndex != -1 && cursor.moveToFirst()) {
                    cursor.getLong(sizeIndex)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 检查音频文件是否有效
     */
    fun isValidAudioFile(context: Context, uri: Uri): Boolean {
        return try {
            val type = context.contentResolver.getType(uri)
            type != null && SUPPORTED_AUDIO_TYPES.contains(type)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 格式化文件大小显示
     */
    fun formatFileSize(sizeInBytes: Long): String {
        return when {
            sizeInBytes < 1024 -> "${sizeInBytes}B"
            sizeInBytes < 1024 * 1024 -> "${sizeInBytes / 1024}KB"
            else -> "${sizeInBytes / (1024 * 1024)}MB"
        }
    }

    /**
     * 格式化音频时长显示
     */
    fun formatDuration(durationInMs: Long): String {
        val seconds = durationInMs / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return if (minutes > 0) {
            "${minutes}:${String.format("%02d", remainingSeconds)}"
        } else {
            "${remainingSeconds}秒"
        }
    }
}

/**
 * 音频文件信息数据类
 */
data class AudioFileInfo(
    val uri: Uri,
    val fileName: String,
    val displayName: String,
    val duration: Long? = null,
    val fileSize: Long? = null,
    val mimeType: String? = null
) {
    val durationText: String
        get() = duration?.let { AudioFileHelper.formatDuration(it) } ?: "未知"

    val fileSizeText: String
        get() = fileSize?.let { AudioFileHelper.formatFileSize(it) } ?: "未知"
}