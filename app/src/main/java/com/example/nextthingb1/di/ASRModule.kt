package com.example.nextthingb1.di

import com.example.nextthingb1.data.asr.IFlyASRService
import com.example.nextthingb1.domain.service.ASRService
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
    fun provideASRService(service: IFlyASRService): ASRService = service
}
