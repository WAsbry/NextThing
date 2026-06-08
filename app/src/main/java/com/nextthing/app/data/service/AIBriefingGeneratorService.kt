package com.nextthing.app.data.service

import com.nextthing.app.data.preferences.TokenManager
import com.nextthing.app.data.remote.api.AIChatApi
import com.nextthing.app.data.remote.dto.AIChatRequest
import com.nextthing.app.domain.service.AIBriefingGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIBriefingGeneratorService @Inject constructor(
    private val aiChatApi: AIChatApi,
    private val tokenManager: TokenManager
) : AIBriefingGenerator {

    override suspend fun generateBriefing(
        type: AIBriefingGenerator.BriefingType,
        taskData: String
    ): Result<String> {
        val userId = tokenManager.serverUserId.first()
        if (userId == null) {
            return Result.failure(IllegalStateException("请先登录"))
        }

        return try {
            val typeStr = if (type == AIBriefingGenerator.BriefingType.MORNING) "morning" else "evening"
            val message = "请为用户生成${if (type == AIBriefingGenerator.BriefingType.MORNING) "早间" else "晚间"}简报，参考以下任务数据：\n$taskData"
            val response = withContext(Dispatchers.IO) {
                aiChatApi.chat(AIChatRequest(message, "briefing-$typeStr"))
            }
            if (response.success && !response.reply.isNullOrBlank()) {
                Result.success(response.reply.trim())
            } else {
                Result.failure(Exception(response.reply ?: "AI 返回内容为空"))
            }
        } catch (e: Exception) {
            Timber.tag("AI").e(e, "简报生成失败")
            Result.failure(Exception("AI 简报生成失败: ${e.message}"))
        }
    }
}
