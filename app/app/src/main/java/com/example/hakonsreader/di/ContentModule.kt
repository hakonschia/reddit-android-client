package com.example.hakonsreader.di

import android.os.Bundle
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object ContentModule {

    @Singleton
    @Provides
    fun provideContentFlow() = MutableSharedFlow<Bundle>(replay = 1)
}