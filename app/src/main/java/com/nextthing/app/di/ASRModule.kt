package com.nextthing.app.di

import com.nextthing.app.data.asr.SherpaASRService
import com.nextthing.app.domain.service.ASRService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ASRModule {

    @Provides
    @Singleton
    fun provideASRService(sherpa: SherpaASRService): ASRService = sherpa
}
