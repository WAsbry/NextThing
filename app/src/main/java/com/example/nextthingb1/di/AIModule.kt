package com.example.nextthingb1.di

import com.example.nextthingb1.data.service.AITaskParserService
import com.example.nextthingb1.domain.service.AITaskParser
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
}
