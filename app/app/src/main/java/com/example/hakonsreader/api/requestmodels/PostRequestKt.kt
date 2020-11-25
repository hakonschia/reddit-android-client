package com.example.hakonsreader.api.requestmodels

import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.enums.SortingMethods
import com.example.hakonsreader.api.enums.Thing
import com.example.hakonsreader.api.enums.VoteType
import com.example.hakonsreader.api.interfaces.ReplyableRequestKt
import com.example.hakonsreader.api.interfaces.SaveableRequestKt
import com.example.hakonsreader.api.interfaces.VoteableRequestKt
import com.example.hakonsreader.api.model.AccessToken
import com.example.hakonsreader.api.model.RedditComment
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.api.responses.GenericError
import com.example.hakonsreader.api.service.PostServiceKt
import com.example.hakonsreader.api.utils.Util
import com.example.hakonsreader.api.utils.apiError
import java.lang.Exception

class PostRequestKt(
        private val accessToken: AccessToken,
        private val api: PostServiceKt,
        private val postId: String
) : VoteableRequestKt, ReplyableRequestKt, SaveableRequestKt {

    private val voteRequest: VoteableRequestModelKt = VoteableRequestModelKt(accessToken, api)
    private val replyRequest: ReplyableRequestModelKt = ReplyableRequestModelKt(accessToken, api)
    private val saveRequest: SaveableRequestModelKt = SaveableRequestModelKt(accessToken, api)
    private val modRequest: ModRequestModelKt = ModRequestModelKt(accessToken, api)


    class CommentsResponse(val comments: List<RedditComment>, val post: RedditPost)

    /**
     * Get comments for the post
     *
     * OAuth scope required: *read*
     *
     * @param sort How the comments should be sorted. Default to [SortingMethods.HOT]
     */
    suspend fun comments(sort: SortingMethods = SortingMethods.HOT) : ApiResponse<CommentsResponse> {
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

                ApiResponse.Success(CommentsResponse(allComments, post))
            } else {
                apiError(resp)
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }
    }


    /**
     * Retrieves comments initially hidden (from "2 more comments" comments)
     *
     * If an access token is set comments are customized for the user (ie. vote status is set)
     *
     * OAuth scope required: *read*
     *
     * @param children The list of IDs of comments to get (retrieved via [RedditComment.getChildren])
     * @param parent Optional: The parent comment the new comments belong to. If this sets the new comments
     * as replies directly. This is the same as calling [RedditComment.addReplies] afterwards.
     * Note that this is the parent of the new comments, not the comment holding the list children
     * retrieved with [RedditComment.getChildren].
     */
    suspend fun moreComments(children: List<String>, parent: RedditComment? = null) : ApiResponse<List<RedditComment>> {
        // If no children are given, just return an empty list as it's not strictly an error but it will cause an API error later on
        if (children.isEmpty()) {
            return ApiResponse.Success(ArrayList())
        }

        // The query parameter for the children is a list of comma separated IDs
        val childrenJoined = children.joinToString(",")

        return try {
            val resp = api.getMoreComments(
                    childrenJoined,
                    Util.createFullName(Thing.POST, postId),
                    RedditApi.API_TYPE,
                    RedditApi.RAW_JSON
            )

            val comments = resp?.body()?.response?.getListings()
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


    /**
     * Vote on the post
     *
     * OAuth scope required: *vote*
     *
     * @param voteType The type of vote to cast
     * @return An [ApiResponse] with no success data
     */
    override suspend fun vote(voteType: VoteType): ApiResponse<Nothing?> {
        return voteRequest.vote(Thing.POST, postId, voteType)
    }

    /**
     * Submit a new comment as a reply to the post. For replies to other comments use {@link CommentRequest#reply(String, OnResponse, OnFailure)}
     *
     * OAuth scope required: *submit*
     *
     * @param text The comment to submit, formatted as <a href="https://en.wikipedia.org/wiki/Markdown">Markdown</a>
     * @return An [ApiResponse] with the new comment
     */
    override suspend fun reply(text: String): ApiResponse<RedditComment> {
        return replyRequest.postComment(Thing.POST, postId, text)
    }


    /**
     * Save the post
     *
     * OAuth scope required: *save*
     *
     * @return An [ApiResponse] with no success data
     * @see unsave
     */
    override suspend fun save(): ApiResponse<Nothing?> {
        return saveRequest.save(Thing.POST, postId)
    }

    /**
     * Unsave the post
     *
     * OAuth scope required: *save*
     *
     * @return An [ApiResponse] with no success data
     * @see save
     */
    override suspend fun unsave() : ApiResponse<Nothing?> {
        return saveRequest.unsave(Thing.POST, postId)
    }


    /**
     * Distinguish the post as a moderator.
     *
     * If the currently logged in user is not a moderator in the subreddit the post is in this will fail
     *
     * OAuth scope required: *modposts*
     *
     * @return An [ApiResponse] with the updated post
     * @see removeModDistinguish
     */
    suspend fun distinguishAsMod() : ApiResponse<RedditPost> {
        return modRequest.distinguishAsModPost(postId, true)
    }

    /**
     * Remove the mod distinguish on the post.
     *
     * If the currently logged in user is not a moderator in the subreddit the post is in this will fail
     *
     * OAuth scope required: *modposts*
     *
     * @return An [ApiResponse] with the updated post
     * @see removeModDistinguish
     */
    suspend fun removeModDistinguish() : ApiResponse<RedditPost> {
        return modRequest.distinguishAsModPost(postId, false)
    }

    /**
     * Sticky on the post
     *
     * If the currently logged in user is not a moderator in the subreddit the post is in this will fail
     *
     * OAuth scope required: *modposts*
     *
     * @return An [ApiResponse] with no success data
     * @see unsticky
     */
    suspend fun sticky() : ApiResponse<Nothing?> {
        return modRequest.stickyPost(postId, true)
    }

    /**
     * Remove the sticky on the post
     *
     * If the currently logged in user is not a moderator in the subreddit the post is in this will fail
     *
     * OAuth scope required: *modposts*
     *
     * @return An [ApiResponse] with no success data
     * @see sticky
     */
    suspend fun unsticky() : ApiResponse<Nothing?> {
        return modRequest.stickyPost(postId, false)
    }

}