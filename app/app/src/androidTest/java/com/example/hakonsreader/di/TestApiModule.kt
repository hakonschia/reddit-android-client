package com.example.hakonsreader.di

import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.enums.PostTimeSort
import com.example.hakonsreader.api.enums.SortingMethods
import com.example.hakonsreader.api.enums.VoteType
import com.example.hakonsreader.api.model.*
import com.example.hakonsreader.api.model.flairs.RedditFlair
import com.example.hakonsreader.api.model.thirdparty.ThirdPartyOptions
import com.example.hakonsreader.api.requestmodels.*
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.api.responses.GenericError
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
    companion object {
        /**
         * If this is passed a listing ID, a vote will return an error instead of success
         */
        const val VOTE_FAIL = "vote_fail"
    }


    @Singleton
    @Provides
    fun provideApi() : RedditApi {
        return object : RedditApi {
            private var isPrivatelyBrowsing = false

            override fun accessToken(): AccessTokenModel {
                TODO("Not yet implemented")
            }

            override fun post(postId: String): PostRequest {
                return object : PostRequest {
                    override suspend fun comments(sort: SortingMethods, loadThirdParty: Boolean): ApiResponse<PostRequestImpl.CommentsResponse> {
                        TODO("Not yet implemented")
                    }

                    override suspend fun moreComments(children: List<String>, parent: RedditComment?): ApiResponse<List<RedditComment>> {
                        TODO("Not yet implemented")
                    }

                    override suspend fun vote(voteType: VoteType): ApiResponse<Nothing?> {
                        return if (postId == VOTE_FAIL) {
                            ApiResponse.Error(GenericError(403), Throwable("'$VOTE_FAIL' was passed as listing ID"))
                        } else {
                            ApiResponse.Success(null)
                        }
                    }

                    override suspend fun save(): ApiResponse<Nothing?> {
                        TODO("Not yet implemented")
                    }

                    override suspend fun unsave(): ApiResponse<Nothing?> {
                        TODO("Not yet implemented")
                    }

                    override suspend fun distinguishAsMod(): ApiResponse<RedditPost> {
                        TODO("Not yet implemented")
                    }

                    override suspend fun removeModDistinguish(): ApiResponse<RedditPost> {
                        TODO("Not yet implemented")
                    }

                    override suspend fun sticky(): ApiResponse<Any?> {
                        TODO("Not yet implemented")
                    }

                    override suspend fun unsticky(): ApiResponse<Any?> {
                        TODO("Not yet implemented")
                    }

                    override suspend fun info(): ApiResponse<RedditPost?> {
                        TODO("Not yet implemented")
                    }

                    override suspend fun markNsfw(): ApiResponse<Any?> {
                        TODO("Not yet implemented")
                    }

                    override suspend fun unmarkNsfw(): ApiResponse<Any?> {
                        TODO("Not yet implemented")
                    }

                    override suspend fun markSpoiler(): ApiResponse<Any?> {
                        TODO("Not yet implemented")
                    }

                    override suspend fun unmarkSpoiler(): ApiResponse<Any?> {
                        TODO("Not yet implemented")
                    }

                    override suspend fun delete(): ApiResponse<Any?> {
                        TODO("Not yet implemented")
                    }

                    override suspend fun reply(text: String): ApiResponse<RedditComment> {
                        TODO("Not yet implemented")
                    }

                    override suspend fun ignoreReports(): ApiResponse<Any?> {
                        TODO("Not yet implemented")
                    }

                    override suspend fun unignoreReports(): ApiResponse<Any?> {
                        TODO("Not yet implemented")
                    }

                    override suspend fun lock(): ApiResponse<Any?> {
                        TODO("Not yet implemented")
                    }

                    override suspend fun unlock(): ApiResponse<Any?> {
                        TODO("Not yet implemented")
                    }

                }
            }

            override fun comment(commentId: String): CommentRequest {
                TODO("Not yet implemented")
            }

            override fun subreddit(subredditName: String): SubredditRequest {
                return object : SubredditRequest {
                    override suspend fun info(): ApiResponse<Subreddit> {
                        TODO("Not yet implemented")
                    }

                    override suspend fun rules(): ApiResponse<List<SubredditRule>> {
                        TODO("Not yet implemented")
                    }

                    override suspend fun posts(postSort: SortingMethods, timeSort: PostTimeSort, after: String, count: Int, limit: Int): ApiResponse<List<RedditPost>> {
                        val listTypeToken = object : TypeToken<List<RedditPost>>(){}.type
                        // This file includes a raw response from "https://www.reddit.com/.json?raw_json=1&limit=15" (only the "children: []" list)
                        // There are 15 posts in the list
                        val postsData = javaClass.classLoader!!.getResource("api/subreddit-request/posts-default-no-user.json").readText()
                        val posts: List<RedditPost> = Gson().fromJson(postsData, listTypeToken)

                        return ApiResponse.Success(posts)
                    }

                    override suspend fun subscribe(subscribe: Boolean): ApiResponse<Nothing?> {
                        TODO("Not yet implemented")
                    }

                    override suspend fun favorite(favorite: Boolean): ApiResponse<Nothing?> {
                        TODO("Not yet implemented")
                    }

                    override suspend fun submitTextPost(title: String, text: String, nsfw: Boolean, spoiler: Boolean, receiveNotifications: Boolean, flairId: String): ApiResponse<Submission> {
                        TODO("Not yet implemented")
                    }

                    override suspend fun submitLinkPost(title: String, link: String, nsfw: Boolean, spoiler: Boolean, receiveNotifications: Boolean, flairId: String): ApiResponse<Submission> {
                        TODO("Not yet implemented")
                    }

                    override suspend fun submitCrosspost(title: String, crosspostId: String, nsfw: Boolean, spoiler: Boolean, receiveNotifications: Boolean, flairId: String): ApiResponse<Submission> {
                        TODO("Not yet implemented")
                    }

                    override suspend fun submissionFlairs(): ApiResponse<List<RedditFlair>> {
                        TODO("Not yet implemented")
                    }

                    override suspend fun userFlairs(): ApiResponse<List<RedditFlair>> {
                        TODO("Not yet implemented")
                    }

                    override suspend fun selectFlair(username: String, flairId: String?): ApiResponse<Any?> {
                        TODO("Not yet implemented")
                    }

                    override suspend fun enableUserFlair(enable: Boolean): ApiResponse<Any?> {
                        TODO("Not yet implemented")
                    }

                    override suspend fun wiki(page: String): ApiResponse<SubredditWikiPage> {
                        TODO("Not yet implemented")
                    }

                }
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
                        // There are 5 subreddits in the list
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

            override val thirdPartyOptions: ThirdPartyOptions
                get() = ThirdPartyOptions()

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