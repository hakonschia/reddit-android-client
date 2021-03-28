package com.example.hakonsreader.di

import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.model.AccessToken
import com.example.hakonsreader.api.model.RedditUser
import com.example.hakonsreader.api.model.Subreddit
import com.example.hakonsreader.api.model.TrendingSubreddits
import com.example.hakonsreader.api.requestmodels.*
import com.example.hakonsreader.api.responses.ApiResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
        components = [SingletonComponent::class],
        replaces = [ApiModule::class]
)
class TestApiModule {

    @Singleton
    @Provides
    fun provideApi() : RedditApi {
        return object : RedditApi {
            private var isPrivatelyBrowsing = false

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
                return object : SubredditsRequest {
                    override suspend fun getSubreddits(after: String, count: Int): ApiResponse<List<Subreddit>> {
                        // The actual implementation of this calls either subscribedSubreddits or defaultSubreddits
                        // depending on if a user is logged in. For tests we can just use the default ones
                        return defaultSubreddits(after, count)
                    }

                    override suspend fun subscribedSubreddits(after: String, count: Int): ApiResponse<List<Subreddit>> {
                        val listTypeToken = object : TypeToken<List<Subreddit>>(){}.type
                        // This file includes a raw response from "/subreddits/mine/subscriber?raw_json=1" (only the "children: []" list)
                        // There are 12 subreddits in the list
                        val subredditsData = javaClass.classLoader!!.getResource("api/subreddits-request/subscribed-subreddits.json").readText()
                        val subreddits: List<Subreddit> = Gson().fromJson(subredditsData, listTypeToken)

                        return ApiResponse.Success(subreddits)
                    }

                    override suspend fun defaultSubreddits(after: String, count: Int): ApiResponse<List<Subreddit>> {
                        val listTypeToken = object : TypeToken<List<Subreddit>>(){}.type
                        // This file includes a raw response from "/subreddits/default.json?raw_json=1&limit=10" (only the "children: []" list)
                        // The response was retrieved without a user context
                        // There are 10 subreddits in the list
                        val subredditsData = javaClass.classLoader!!.getResource("api/subreddits-request/default-subreddits.json").readText()
                        val subreddits: List<Subreddit> = Gson().fromJson(subredditsData, listTypeToken)

                        return ApiResponse.Success(subreddits)
                    }

                    override suspend fun search(query: String, includeNsfw: Boolean): ApiResponse<List<Subreddit>> {
                        val listTypeToken = object : TypeToken<List<Subreddit>>(){}.type
                        // This file includes a raw response from "/subreddits/search.json?q=dogs&raw_json=1&limit=5" (only the "children: []" list)
                        // The response was retrieved without a user context
                        // There are 10 subreddits in the list
                        val subredditsData = javaClass.classLoader!!.getResource("api/subreddits-request/search.json").readText()
                        val subreddits: List<Subreddit> = Gson().fromJson(subredditsData, listTypeToken)

                        return ApiResponse.Success(subreddits)
                    }

                    override suspend fun trending(): ApiResponse<TrendingSubreddits> {
                        return ApiResponse.Success(TrendingSubreddits().apply {
                            this.commentCount = 5
                            this.commentUrl = "r/SomeFakeSubreddit/comments/654ge/SomeFakePost/"
                            this.subreddits = listOf(
                                    "Hakonschia",
                                    "GlobalOffensive",
                                    "Norge"
                            )
                        })
                    }
                }
            }

            override fun user(username: String): UserRequests {
                TODO("Not yet implemented")
            }

            override fun user(): UserRequestsLoggedInUser {
                return object : UserRequestsLoggedInUser {
                    override suspend fun info(): ApiResponse<RedditUser> {
                        // The info in this file is a direct copy of a "api/v1/me?raw_json=1" response
                        val userData = javaClass.classLoader!!.getResource("api/user-requests-logged-in-user/arne-rofinn-user-info.json").readText()
                        val user = Gson().fromJson(userData, RedditUser::class.java)

                        return ApiResponse.Success(user)
                    }
                }
            }

            override fun messages(): MessagesRequestModel {
                TODO("Not yet implemented")
            }

            override fun enablePrivateBrowsing(enable: Boolean) {
                isPrivatelyBrowsing = enable
            }

            override fun isPrivatelyBrowsing(): Boolean {
                return isPrivatelyBrowsing
            }

            override fun switchAccessToken(accessToken: AccessToken) {
                // The actual implementation of this would do something with the access token, but
                // is empty here as we don't have anything to do
            }

            override fun logOut() {
                // The actual implementation of this would do something with the access token, but
                // is empty here as we don't have anything to do
            }
        }
    }
}