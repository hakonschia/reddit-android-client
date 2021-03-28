package com.example.hakonsreader.di

import android.content.Context
import com.example.hakonsreader.api.persistence.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


/**
 * Module providing databases and DAOs
 */
@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {

    @Singleton
    @Provides
    fun provideUserDatabase(@ApplicationContext context: Context): RedditUserInfoDatabase {
        return RedditUserInfoDatabase.getInstance(context)
    }

    @Singleton
    @Provides
    fun provideUserInfoDao(database: RedditUserInfoDatabase): RedditUserInfoDao {
        return database.userInfo()
    }


    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): RedditDatabase {
        return RedditDatabase.getInstance(context)
    }

    @Singleton
    @Provides
    fun providePostsDao(database: RedditDatabase) : RedditPostsDao {
        return database.posts()
    }

    @Singleton
    @Provides
    fun provideSubredditsDao(database: RedditDatabase) : RedditSubredditsDao {
        return database.subreddits()
    }

    @Singleton
    @Provides
    fun provideFlairsDao(database: RedditDatabase) : RedditFlairsDao {
        return database.flairs()
    }

    @Singleton
    @Provides
    fun provideRulesDao(database: RedditDatabase) : RedditSubredditRulesDao {
        return database.rules()
    }

    @Singleton
    @Provides
    fun provideMessagesDao(database: RedditDatabase) : RedditMessagesDao {
        return database.messages()
    }

}