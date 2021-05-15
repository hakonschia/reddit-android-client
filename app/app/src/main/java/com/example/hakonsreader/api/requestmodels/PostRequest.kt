package com.example.hakonsreader.api.requestmodels

import com.example.hakonsreader.api.enums.SortingMethods
import com.example.hakonsreader.api.enums.Thing
import com.example.hakonsreader.api.enums.VoteType
import com.example.hakonsreader.api.exceptions.InvalidAccessTokenException
import com.example.hakonsreader.api.interfaces.*
import com.example.hakonsreader.api.model.AccessToken
import com.example.hakonsreader.api.model.RedditComment
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.api.model.thirdparty.ThirdPartyOptions
import com.example.hakonsreader.api.requestmodels.thirdparty.ThirdPartyRequest
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.api.responses.GenericError
import com.example.hakonsreader.api.service.PostService
import com.example.hakonsreader.api.service.thirdparty.GfycatService
import com.example.hakonsreader.api.service.thirdparty.ImgurService
import com.example.hakonsreader.api.utils.apiError
import com.example.hakonsreader.api.utils.createFullName
import com.example.hakonsreader.api.utils.verifyLoggedInToken
import java.lang.Exception

interface PostRequest : VoteableRequest, ReplyableRequest, SaveableRequest, ReportableRequest, LockableRequest, DistinguishableRequest {
    /**
     * Get comments for the post
     *
     * OAuth scope required: `read`
     *
     * @param sort How the comments should be sorted. Default to [SortingMethods.HOT]
     * @param loadThirdParty If true, third party requests (such as retrieving gifs from Gfycat directly)
     * will be made. This is default to `false`. If only the comments of the post (and potentially updated
     * post information) is needed, consider keeping this to `false` to not make unnecessary API calls.
     * In other words, this should only be set to `true` if the post is loaded for the first time and
     * the content of the post has to be drawn.
     */
    suspend fun comments(sort: SortingMethods = SortingMethods.HOT, loadThirdParty: Boolean = false) : ApiResponse<PostRequestImpl.CommentsResponse>

    /**
     * Retrieves comments initially hidden (from "2 more comments" comments)
     *
     * If an access token is set comments are customized for the user (ie. vote status is set)
     *
     * OAuth scope required: `read`
     *
     * @param children The list of IDs of comments to get (retrieved via [RedditComment.children])
     * @param parent Optional: The parent comment the new comments belong to. If this sets the new comments
     * as replies directly. This is the same as calling [RedditComment.addReplies] afterwards.
     * Note that this is the parent of the new comments, not the comment holding the list children
     * retrieved with [RedditComment.children].
     */
    suspend fun moreComments(children: List<String>, parent: RedditComment? = null) : ApiResponse<List<RedditComment>>

    /**
     * Vote on the post
     *
     * OAuth scope required: `vote`
     *
     * @param voteType The type of vote to cast
     * @return An [ApiResponse] with no success data
     */
    override suspend fun vote(voteType: VoteType): ApiResponse<Unit>

    /**
     * Save the post
     *
     * OAuth scope required: `save`
     *
     * @return An [ApiResponse] with no success data
     * @see unsave
     */
    override suspend fun save(): ApiResponse<Unit>

    /**
     * Unsave the post
     *
     * OAuth scope required: `save`
     *
     * @return An [ApiResponse] with no success data
     * @see save
     */
    override suspend fun unsave() : ApiResponse<Unit>

    /**
     * Distinguish the post as a moderator.
     *
     * If the currently logged in user is not a moderator in the subreddit the post is in this will fail
     *
     * OAuth scope required: `modposts`
     *
     * @return An [ApiResponse] with the updated post
     * @see removeModDistinguish
     */
    override suspend fun distinguishAsMod() : ApiResponse<RedditPost>

    /**
     * Remove the mod distinguish on the post.
     *
     * If the currently logged in user is not a moderator in the subreddit the post is in this will fail
     *
     * OAuth scope required: `modposts`
     *
     * @return An [ApiResponse] with the updated post
     * @see removeModDistinguish
     */
    override suspend fun removeModDistinguish() : ApiResponse<RedditPost>

    /**
     * Sticky on the post
     *
     * If the currently logged in user is not a moderator in the subreddit the post is in this will fail
     *
     * OAuth scope required: `modposts`
     *
     * @return An [ApiResponse] with no success data
     * @see unsticky
     */
    suspend fun sticky() : ApiResponse<Unit>

    /**
     * Remove the sticky on the post
     *
     * If the currently logged in user is not a moderator in the subreddit the post is in this will fail
     *
     * OAuth scope required: `modposts`
     *
     * @return An [ApiResponse] with no success data
     * @see sticky
     */
    suspend fun unsticky() : ApiResponse<Unit>

    /**
     * Retrieves information about the post
     *
     * Retrieving comments also retrieves the post information, only use this if you only want
     * the post information
     *
     * OAuth scope required: `read`
     *
     * @return The post will be returned in the response. If the post wasn't found, this will be *null*
     * @see comments
     */
    suspend fun info() : ApiResponse<RedditPost?>

    /**
     * Mark the post as NSFW
     *
     * OAuth scope required: `modposts`
     *
     * @return An [ApiResponse] with no success data
     * @see unmarkNsfw
     */
    suspend fun markNsfw() : ApiResponse<Unit>

    /**
     * Unmark the post as NSFW
     *
     * OAuth scope required: `modposts`
     *
     * @return An [ApiResponse] with no success data
     * @see markNsfw
     */
    suspend fun unmarkNsfw() : ApiResponse<Unit>

    /**
     * Mark the post as a spoiler
     *
     * OAuth scope required: `modposts`
     *
     * @return An [ApiResponse] with no success data
     * @see unmarkSpoiler
     */
    suspend fun markSpoiler() : ApiResponse<Unit>

    /**
     * Unmark the post as a spoiler
     *
     * OAuth scope required: `modposts`
     *
     * @return An [ApiResponse] with no success data
     * @see markSpoiler
     */
    suspend fun unmarkSpoiler() : ApiResponse<Unit>

    /**
     * Delete the post
     *
     * OAuth scope required: `edit`
     *
     * @return No response data is returned
     */
    suspend fun delete() : ApiResponse<Unit>
}

/**
 * Request model for communicating with Reddit posts
 */
class PostRequestImpl(
        private val accessToken: AccessToken,
        private val api: PostService,
        private val postId: String,
        imgurApi: ImgurService?,
        gfycatApi: GfycatService,
        thirdPartyOptions: ThirdPartyOptions
) : PostRequest {

    private val voteRequest = VoteableRequestModelImpl(accessToken, api)
    private val replyRequest = ReplyableRequestModelImpl(accessToken, api)
    private val saveRequest = SaveableRequestModelImpl(accessToken, api)
    private val modRequest = ModRequestModelImpl(accessToken, api)
    private val thirdPartyRequest = ThirdPartyRequest(imgurApi, gfycatApi, thirdPartyOptions)


    class CommentsResponse(val comments: List<RedditComment>, val post: RedditPost)


    override suspend fun comments(sort: SortingMethods, loadThirdParty: Boolean) : ApiResponse<CommentsResponse> {
        return try {
            val resp = api.getComments(postId, sort.value)

            val body = resp.body()
            if (body != null) {
                val post = body[0].getListings()?.get(0) as RedditPost
                val topLevelComments = body[1].getListings() as List<RedditComment>

                val allComments = ArrayList<RedditComment>()
                topLevelComments.forEach {
                    allComments.add(it)
                    allComments.addAll(it.replies)
                }

                if (loadThirdParty) {
                    thirdPartyRequest.loadAll(post)
                }

                ApiResponse.Success(CommentsResponse(allComments, post))
            } else {
                apiError(resp)
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }
    }


    override suspend fun moreComments(children: List<String>, parent: RedditComment?) : ApiResponse<List<RedditComment>> {
        // If no children are given, just return an empty list as it's not strictly an error but it will cause an API error later on
        if (children.isEmpty()) {
            return ApiResponse.Success(ArrayList())
        }

        // The query parameter for the children is a list of comma separated IDs
        val childrenJoined = children.joinToString(",")

        return try {
            val resp = api.getMoreComments(
                    childrenJoined,
                    createFullName(Thing.POST, postId)
            )

            val comments = resp.body()?.getListings() as List<RedditComment>?
            if (comments != null) {

                parent?.addReplies(comments)
                ApiResponse.Success(comments)
            } else {
                apiError(resp)
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }
    }

    override suspend fun vote(voteType: VoteType): ApiResponse<Unit> {
        return voteRequest.vote(Thing.POST, postId, voteType)
    }

    override suspend fun reply(text: String): ApiResponse<RedditComment> {
        return replyRequest.postComment(Thing.POST, postId, text)
    }


    override suspend fun save(): ApiResponse<Unit> {
        return saveRequest.save(Thing.POST, postId)
    }

    override suspend fun unsave() : ApiResponse<Unit> {
        return saveRequest.unsave(Thing.POST, postId)
    }


    override suspend fun distinguishAsMod() : ApiResponse<RedditPost> {
        return modRequest.distinguishAsModPost(postId, true)
    }

    override suspend fun removeModDistinguish() : ApiResponse<RedditPost> {
        return modRequest.distinguishAsModPost(postId, false)
    }


    override suspend fun sticky() : ApiResponse<Unit> {
        return modRequest.stickyPost(postId, true)
    }

    override suspend fun unsticky() : ApiResponse<Unit> {
        return modRequest.stickyPost(postId, false)
    }


    override suspend fun info() : ApiResponse<RedditPost?> {
        return try {
            val response = api.getInfo(createFullName(Thing.POST, postId))
            val body = response.body()

            if (body != null) {
                val listings = body.getListings()

                if (listings?.isNotEmpty() == true) {
                    val post = listings[0]
                    thirdPartyRequest.loadAll(post)
                    ApiResponse.Success(post)
                } else {
                    ApiResponse.Success(null)
                }
            } else {
                apiError(response)
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }
    }


    override suspend fun ignoreReports() : ApiResponse<Unit> {
        return modRequest.ignoreReports(Thing.POST, postId)
    }

    override suspend fun unignoreReports() : ApiResponse<Unit> {
        return modRequest.unignoreReports(Thing.POST, postId)
    }

    override suspend fun markNsfw() : ApiResponse<Unit> {
        return markNsfwInternal(true)
    }

    override suspend fun unmarkNsfw() : ApiResponse<Unit> {
        return markNsfwInternal(false)
    }

    /**
     * Internal function to mark/unmark the post as NSFW
     *
     * OAuth scope required: `modposts`
     *
     * @return An [ApiResponse] with no success data
     */
    private suspend fun markNsfwInternal(markNsfw: Boolean) : ApiResponse<Unit> {
        try {
            verifyLoggedInToken(accessToken)
        } catch (e: InvalidAccessTokenException) {
            return ApiResponse.Error(GenericError(-1), InvalidAccessTokenException("Cannot adjust NSFW marks on posts without an access token for a logged in user", e))
        }
        val fullname = createFullName(Thing.POST, postId)

        return try {
            val response = if (markNsfw) {
                api.markNsfw(fullname)
            } else {
                api.unmarkNsfw(fullname)
            }

            if (response.isSuccessful) {
                ApiResponse.Success(Unit)
            } else {
                apiError(response)
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }
    }


    override suspend fun markSpoiler() : ApiResponse<Unit> {
        return markSpoilerInternal(true)
    }

    override suspend fun unmarkSpoiler() : ApiResponse<Unit> {
        return markSpoilerInternal(false)
    }

    /**
     * Internal function to mark/unmark the post as NSFW
     *
     * OAuth scope required: `modposts`
     *
     * @param markSpoiler True to mark as spoiler, false to unmark
     * @return An [ApiResponse] with no success data
     */
    private suspend fun markSpoilerInternal(markSpoiler: Boolean) : ApiResponse<Unit> {
        try {
            verifyLoggedInToken(accessToken)
        } catch (e: InvalidAccessTokenException) {
            return ApiResponse.Error(GenericError(-1), InvalidAccessTokenException("Cannot adjust spoiler on posts without an access token for a logged in user", e))
        }
        val fullname = createFullName(Thing.POST, postId)

        return try {
            val response = if (markSpoiler) {
                api.markSpoiler(fullname)
            } else {
                api.unmarkSpoiler(fullname)
            }

            if (response.isSuccessful) {
                ApiResponse.Success(Unit)
            } else {
                apiError(response)
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }
    }

    override suspend fun lock() : ApiResponse<Unit> {
        return modRequest.lock(postId, true)
    }

    override suspend fun unlock() : ApiResponse<Unit> {
        return modRequest.unlock(postId, true)
    }

    override suspend fun delete() : ApiResponse<Unit> {
        try {
            verifyLoggedInToken(accessToken)
        } catch (e: InvalidAccessTokenException) {
            return ApiResponse.Error(GenericError(-1), InvalidAccessTokenException("Cannot delete posts without an access token for a logged in user", e))
        }

        return try {
            val response = api.delete(createFullName(Thing.POST, postId))
            if (response.isSuccessful) {
                ApiResponse.Success(Unit)
            } else {
                apiError(response)
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }
    }
}