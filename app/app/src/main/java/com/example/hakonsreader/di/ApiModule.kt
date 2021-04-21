package com.example.hakonsreader.di

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.model.thirdparty.ThirdPartyOptions
import com.example.hakonsreader.api.responses.GenericError
import com.example.hakonsreader.constants.NetworkConstants
import com.example.hakonsreader.constants.SharedPreferencesConstants
import com.example.hakonsreader.states.AppState
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
import javax.inject.Singleton


/**
 * Module for injecting a [RedditApi] instance. This module should not be used during testing
 */
@InstallIn(SingletonComponent::class)
@Module
object ApiModule {

    @Singleton
    @Provides
    fun provideApi(@ApplicationContext context: Context) : RedditApi {
        // This module is only for production, so the application context will always be App
        context as App

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

        val privatelyBrowsing = SharedPreferencesManager.get(SharedPreferencesConstants.PRIVATELY_BROWSING, Boolean::class.java) ?: false

        return RedditApi.create(
                userAgent = NetworkConstants.USER_AGENT,
                clientId = NetworkConstants.CLIENT_ID,

                accessToken = TokenManager.getToken(),
                onNewToken = { newToken -> AppState.onNewToken(newToken) },
                onInvalidToken = { _: GenericError?, _: Throwable? -> context.onInvalidAccessToken() },

                //loggerLevel = HttpLoggingInterceptor.Level.BODY,

                callbackUrl = NetworkConstants.CALLBACK_URL,
                //deviceId = UUID.randomUUID().toString(),
                imgurClientId = imgurClientId,

                thirdPartyCache = thirdPartyCache,
                thirdPartyCacheAge = thirdPartyCacheAge,

                thirdPartyOptions = getThirdPartyOptions(PreferenceManager.getDefaultSharedPreferences(context), context)
        ).apply {
            enablePrivateBrowsing(privatelyBrowsing)
        }
    }

    private fun getThirdPartyOptions(preferences: SharedPreferences, context: Context): ThirdPartyOptions {
        return ThirdPartyOptions(
                loadGfycatGifs = preferences.getBoolean(
                        context.getString(R.string.prefs_key_third_party_load_gfycat_gifs),
                        context.resources.getBoolean(R.bool.prefs_default_third_party_load_gfycat_gifs)
                ),
                loadImgurGifs = preferences.getBoolean(
                        context.getString(R.string.prefs_key_third_party_load_imgur_gifs),
                        context.resources.getBoolean(R.bool.prefs_default_third_party_load_imgur_gifs)
                ),
                loadImgurAlbums = preferences.getBoolean(
                        context.getString(R.string.prefs_key_third_party_load_imgur_albums),
                        context.resources.getBoolean(R.bool.prefs_default_third_party_load_imgur_albums)
                )
        )
    }
}