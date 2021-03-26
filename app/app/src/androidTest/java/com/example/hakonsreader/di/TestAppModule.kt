package com.example.hakonsreader.di

import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.model.AccessToken
import com.example.hakonsreader.api.model.RedditUser
import com.example.hakonsreader.api.requestmodels.*
import com.example.hakonsreader.api.responses.ApiResponse
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
        components = [SingletonComponent::class],
        replaces = [AppModule::class]
)
class TestAppModule {

    @Singleton
    @Provides
    fun provideApi() : RedditApi {
        return object : RedditApi {
            override fun accessToken(): AccessTokenModel {
                TODO("Not yet implemented")
            }

            override fun post(postId: String): PostRequest {
                TODO("Not yet implemented")
            }

            override fun comment(commentId: String): CommentRequest {
                TODO("Not yet implemented")
            }

            override fun subreddit(subredditName: String): SubredditRequest {
                TODO("Not yet implemented")
            }

            override fun subreditts(): SubredditsRequest {
                TODO("Not yet implemented")
            }

            override fun user(username: String): UserRequests {
                TODO("Not yet implemented")
            }

            override fun user(): UserRequestsLoggedInUser {
                return object : UserRequestsLoggedInUser {
                    override suspend fun info(): ApiResponse<RedditUser> {
                        return ApiResponse.Success(RedditUser().apply {
                            username = "Hakonschia"
                        })
                    }
                }
            }

            override fun messages(): MessagesRequestModel {
                TODO("Not yet implemented")
            }

            override fun enablePrivateBrowsing(enable: Boolean) {
                TODO("Not yet implemented")
            }

            override fun isPrivatelyBrowsing(): Boolean {
                TODO("Not yet implemented")
            }

            override fun switchAccessToken(accessToken: AccessToken) {
                TODO("Not yet implemented")
            }

            override fun logOut() {
                TODO("Not yet implemented")
            }

        }
    }
}