package com.example.hakonsreader.di

import android.app.Application
import android.content.Context
import android.util.Log
import com.example.hakonsreader.App
import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.model.RedditUserInfo
import com.example.hakonsreader.api.persistence.*
import com.example.hakonsreader.api.responses.GenericError
import com.example.hakonsreader.constants.NetworkConstants
import com.example.hakonsreader.constants.SharedPreferencesConstants
import com.example.hakonsreader.misc.SharedPreferencesManager
import com.example.hakonsreader.misc.TokenManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.util.*
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object AppModule {

    @Singleton
    @Provides
    fun provideApi(@ApplicationContext context: Context) : RedditApi {
        context as App

        // Weird to do this in here I guess, but preferences have to be set for TokenManager
        val prefs = context.getSharedPreferences(SharedPreferencesConstants.PREFS_NAME, Application.MODE_PRIVATE)
        SharedPreferencesManager.create(prefs)

        // 25MB cache size for network requests to third party
        val thirdPartyCacheSize = 25 * 1024 * 1024L
        val thirdPartyCache = Cache(File(context.cacheDir, "third_party_http_cache"), thirdPartyCacheSize)

        // 1 week cache size (this cache size could really be as long as time itself, the mutable ata
        // in the requests aren't used anyways)
        val thirdPartyCacheAge = 60 * 60 * 24 * 7L

        // If the Imgur client ID is omitted from secrets.properties it is parsed as a string with the value "null"
        val imgurClientId = if (NetworkConstants.IMGUR_CLIENT_ID != "null") {
            NetworkConstants.IMGUR_CLIENT_ID
        } else null

        Log.d("App", "getApi creating api now")
        return RedditApi.create(
                userAgent = NetworkConstants.USER_AGENT,
                clientId = NetworkConstants.CLIENT_ID,

                accessToken = TokenManager.getToken(),
                onNewToken = { newToken -> context.onNewToken(newToken) },
                onInvalidToken = { _: GenericError?, _: Throwable? -> context.onInvalidAccessToken() },

                loggerLevel = HttpLoggingInterceptor.Level.BODY,

                callbackUrl = NetworkConstants.CALLBACK_URL,
                deviceId = UUID.randomUUID().toString(),
                imgurClientId = imgurClientId,

                 thirdPartyCache = thirdPartyCache,
                 thirdPartyCacheAge = thirdPartyCacheAge
        )
    }



    @Singleton
    @Provides
    fun provideUserDatabase(@ApplicationContext context: Context): RedditUserInfoDatabase {
        return RedditUserInfoDatabase.getInstance(context)
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