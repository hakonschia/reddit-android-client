package com.example.hakonsreader.di

import android.content.Context
import com.example.hakonsreader.misc.Settings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object SettingsModule {

    @Singleton
    @Provides
    fun provideSettings(@ApplicationContext context: Context): Settings {
        return Settings(context)
    }
}