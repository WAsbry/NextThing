package com.nextthing.app.data.service

import com.nextthing.app.data.preferences.TokenManager
import com.nextthing.app.data.remote.api.AIChatApi
import com.nextthing.app.data.remote.dto.AIChatRequest
import com.nextthing.app.domain.service.AIStatsAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIStatsAnalyzerService @Inject constructor(
    private val aiChatApi: AIChatApi,
    private val tokenManager: TokenManager
) : AIStatsAnalyzer {

    override suspend fun generateSummary(statsData: String): Result<String> {
        val userId = tokenManager.serverUserId.first()
        if (userId == null) {
            return Result.failure(IllegalStateException("请先登录"))
        }

        return try {
            val message = "请根据以下统计数据生成分析总结（100-200字，先概述整体，再指出亮点和问题，最后给1-2条建议）：\n$statsData"
            val response = withContext(Dispatchers.IO) {
                aiChatApi.chat(AIChatRequest(message))
            }
            if (response.success && !response.reply.isNullOrBlank()) {
                Result.success(response.reply.trim())
            } else {
                Result.failure(Exception(response.reply ?: "AI 返回内容为空"))
            }
        } catch (e: Exception) {
            Timber.tag("AI").e(e, "统计总结失败")
            Result.failure(Exception("AI 分析失败: ${e.message}"))
        }
    }
}
