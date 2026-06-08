package com.nextthing.app.di

import com.nextthing.app.data.ai.AudioPreprocessorImpl
import com.nextthing.app.data.ai.OnDeviceAIEngineImpl
import com.nextthing.app.data.ai.SERServiceImpl
import com.nextthing.app.data.ai.VoicePipelineImpl
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
import com.nextthing.app.domain.service.AudioPreprocessor
import com.nextthing.app.domain.service.OnDeviceAIEngine
import com.nextthing.app.domain.service.SERService
import com.nextthing.app.domain.service.VoicePipeline
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AIModule {

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

    @Provides
    @Singleton
    fun provideOnDeviceAIEngine(impl: OnDeviceAIEngineImpl): OnDeviceAIEngine = impl

    @Provides
    @Singleton
    fun provideAudioPreprocessor(impl: AudioPreprocessorImpl): AudioPreprocessor = impl

    @Provides
    @Singleton
    fun provideSERService(impl: SERServiceImpl): SERService = impl

    @Provides
    @Singleton
    fun provideVoicePipeline(impl: VoicePipelineImpl): VoicePipeline = impl
}
