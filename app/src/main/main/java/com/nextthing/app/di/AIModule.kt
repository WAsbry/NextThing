package com.nextthing.app.di

import com.nextthing.app.data.service.AITaskParserService
import com.nextthing.app.data.service.AIStatsAnalyzerService
import com.nextthing.app.data.service.AIBriefingGeneratorService
import com.nextthing.app.data.service.AISubtaskGeneratorService
import com.nextthing.app.data.service.AIScheduleAdvisorService
import com.nextthing.app.data.service.AITaskSearcherService
import com.nextthing.app.data.service.AITimeEstimatorService
import com.nextthing.app.data.service.AIProcrastinationDetectorService
import com.nextthing.app.data.service.AIBehaviorAnalyzerService
import com.nextthing.app.data.service.AIWeeklyReporterService
import com.nextthing.app.domain.service.AITaskParser
import com.nextthing.app.domain.service.AIStatsAnalyzer
import com.nextthing.app.domain.service.AIBriefingGenerator
import com.nextthing.app.domain.service.AISubtaskGenerator
import com.nextthing.app.domain.service.AIScheduleAdvisor
import com.nextthing.app.domain.service.AITaskSearcher
import com.nextthing.app.domain.service.AITimeEstimator
import com.nextthing.app.domain.service.AIProcrastinationDetector
import com.nextthing.app.domain.service.AIBehaviorAnalyzer
import com.nextthing.app.domain.service.AIWeeklyReporter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AIModule {

    /**
     * 独立的 AI OkHttpClient：超时时间更长（LLM 响应可能需要 30~60s）
     * Authorization header 由 AITaskParserService 在运行时动态注入（支持用户随时更改 API Key）
     */
    @Provides
    @Singleton
    @Named("ai")
    fun provideAIOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideAITaskParser(service: AITaskParserService): AITaskParser = service

    @Provides
    @Singleton
    fun provideAIStatsAnalyzer(service: AIStatsAnalyzerService): AIStatsAnalyzer = service

    @Provides
    @Singleton
    fun provideAIBriefingGenerator(service: AIBriefingGeneratorService): AIBriefingGenerator = service

    @Provides
    @Singleton
    fun provideAISubtaskGenerator(service: AISubtaskGeneratorService): AISubtaskGenerator = service

    @Provides
    @Singleton
    fun provideAIScheduleAdvisor(service: AIScheduleAdvisorService): AIScheduleAdvisor = service

    @Provides
    @Singleton
    fun provideAITaskSearcher(service: AITaskSearcherService): AITaskSearcher = service

    @Provides
    @Singleton
    fun provideAITimeEstimator(service: AITimeEstimatorService): AITimeEstimator = service

    @Provides
    @Singleton
    fun provideAIProcrastinationDetector(service: AIProcrastinationDetectorService): AIProcrastinationDetector = service

    @Provides
    @Singleton
    fun provideAIBehaviorAnalyzer(service: AIBehaviorAnalyzerService): AIBehaviorAnalyzer = service

    @Provides
    @Singleton
    fun provideAIWeeklyReporter(service: AIWeeklyReporterService): AIWeeklyReporter = service
}
