package com.example.hakonsreader.api.requestmodels;

import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.enums.Thing;
import com.example.hakonsreader.api.enums.VoteType;
import com.example.hakonsreader.api.interfaces.OnFailure;
import com.example.hakonsreader.api.interfaces.OnResponse;
import com.example.hakonsreader.api.interfaces.ReplyableRequest;
import com.example.hakonsreader.api.interfaces.SaveableRequest;
import com.example.hakonsreader.api.interfaces.VoteableRequest;
import com.example.hakonsreader.api.model.AccessToken;
import com.example.hakonsreader.api.model.RedditComment;
import com.example.hakonsreader.api.responses.GenericError;
import com.example.hakonsreader.api.responses.JsonResponse;
import com.example.hakonsreader.api.service.CommentService;
import com.example.hakonsreader.api.utils.Util;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.internal.EverythingIsNonNull;

/**
 * Class that provides an interface towards the Reddit API related to comments, such as
 * replying to the comment or voting on it
 */
public class CommentRequest implements VoteableRequest, SaveableRequest {

    private final String commentId;
    private final AccessToken accessToken;
    private final CommentService api;
    private final VoteableRequestModel voteRequest;
    private final SaveableRequestModel saveRequest;
    private final ModRequestModel modRequest;

    public CommentRequest(String commentId, AccessToken accessToken, CommentService api) {
        this.commentId = commentId;

        this.accessToken = accessToken;
        this.api = api;
        this.voteRequest = new VoteableRequestModel(accessToken, api);
        this.saveRequest = new SaveableRequestModel(accessToken, api);
        this.modRequest = new ModRequestModel(accessToken, api);
    }


    /**
     * Save the comment
     *
     * <p>Requires OAuth scope: {@code save}</p>
     *
     * @param onResponse Callback for successful responses. This will never hold any information, but
     *                   will be called when the request is successful
     * @param onFailure Callback for failed requests
     */
    @Override
    public void save(OnResponse<Void> onResponse, OnFailure onFailure) {
        saveRequest.save(Thing.COMMENT, commentId, onResponse, onFailure);
    }

    /**
     * Unsave the comment
     *
     * <p>Requires OAuth scope: {@code save}</p>
     *
     * @param onResponse Callback for successful responses. This will never hold any information, but
     *                   will be called when the request is successful
     * @param onFailure Callback for failed requests
     */
    @Override
    public void unsave(OnResponse<Void> onResponse, OnFailure onFailure) {
        saveRequest.unsave(Thing.COMMENT, commentId, onResponse, onFailure);
    }

    /**
     * Edits the comment
     *
     * @param editedText The edited text
     * @param onResponse Response handler for successful requests. Holds the updated comment
     * @param onFailure Response handler for failed requests
     */
    public void edit(String editedText, OnResponse<RedditComment> onResponse, OnFailure onFailure) {
        api.edit(
                Util.createFullName(Thing.COMMENT, commentId),
                editedText,
                RedditApi.API_TYPE
        ).enqueue(new Callback<JsonResponse>() {
            @Override
            public void onResponse(Call<JsonResponse> call, Response<JsonResponse> response) {
                //TODO this
                /*
                JsonResponse body = null;
                if (response.isSuccessful()) {
                    body = response.body();
                }

                if (body != null) {
                    if (!body.hasErrors()) {
                        RedditComment comment = (RedditComment) body.getListings().get(0);
                        onResponse.onResponse(comment);
                    } else {
                        Util.handleListingErrors(body.errors(), onFailure);
                    }
                } else {
                    Util.handleHttpErrors(response, onFailure);
                }
                 */
            }

            @Override
            public void onFailure(Call<JsonResponse> call, Throwable t) {
                onFailure.onFailure(new GenericError(-1), t);
            }
        });
    }

    /**
     * Delete a comment. Note the comment being deleted must be posted by the user the access token represents.
     *
     * @param onResponse The response handler for successful request
     *                   <p>NOTE: Even if the comment couldn't be deleted because it didn't belong to
     *                   the user, this will be called as Reddit returns 200 OK for every request.
     *                   Make sure this is only called for comments that actually can be deleted</p>
     * @param onFailure Response handler for failed requests
     */
    public void delete(OnResponse<Void> onResponse, OnFailure onFailure) {
        api.delete(
                Util.createFullName(Thing.COMMENT, commentId)
        ).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    onResponse.onResponse(null);
                } else {
                    Util.handleHttpErrors(response, onFailure);
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                onFailure.onFailure(new GenericError(-1), t);
            }
        });
    }

    /**
     * Cast a vote on the comment
     *
     * <p>Requires a user access token to be set. {@code onFailure} will be called if no access token is set</p>
     *
     * <p>OAuth scope required: {@code vote}</p>
     *
     * @param type The type of vote to cast
     * @param onResponse The callback for successful requests. The value returned will always be null
     *                   as this request does not return any data
     * @param onFailure The callback for failed requests
     */
    @Override
    @EverythingIsNonNull
    public void vote(VoteType type, OnResponse<Void> onResponse, OnFailure onFailure) {
        voteRequest.vote(Thing.COMMENT, commentId, type, onResponse, onFailure);
    }


    /**
     * Distinguish the comment as a moderator. If the currently logged in user is not a moderator
     * in the subreddit the post is in this will fail
     *
     * <p>OAuth scope required: {@code modposts}</p>
     *
     * @param onResponse The callback for successful requests. Holds the new comment data
     * @param onFailure The callback for failed requests
     */
    public void distinguishAsMod(OnResponse<RedditComment> onResponse, OnFailure onFailure) {
        // We want to distinguish, but not sticky
        modRequest.distinguishAsModComment(commentId, true, false, onResponse, onFailure);
    }

    /**
     * Remove the distinguish as mod on the comment comment. This will also remove the sticky if
     * the comment is stickied.
     *
     * <p>If the currently logged in user is not a moderator in the subreddit the post is in this will fail</p>
     *
     * <p>OAuth scope required: {@code modposts}</p>
     *
     * @param onResponse The callback for successful requests. Holds the new comment data
     * @param onFailure The callback for failed requests
     */
    public void removeDistinguishAsMod(OnResponse<RedditComment> onResponse, OnFailure onFailure) {
        // We want to distinguish, but not sticky
        modRequest.distinguishAsModComment(commentId, false, false, onResponse, onFailure);
    }

    /**
     * Sticky the comment. This will also distinguish the comment as a moderator. This only works
     * on top-level comments
     *
     * <p>OAuth scope required: {@code modposts}</p>
     *
     * @param onResponse The callback for successful requests. Holds the new comment data
     * @param onFailure The callback for failed requests
     */
    public void sticky(OnResponse<RedditComment> onResponse, OnFailure onFailure) {
        modRequest.distinguishAsModComment(commentId, true, true, onResponse, onFailure);
    }

    /**
     * Removes the sticky on the comment, keeps the distinguish. To remove both use {@link CommentRequest#removeDistinguishAsMod(OnResponse, OnFailure)}
     *
     * <p>If the currently logged in user is not a moderator in the subreddit the post is in this will fail</p>
     *
     * <p>OAuth scope required: {@code modposts}</p>
     *
     * @param onResponse The callback for successful requests. Holds the new comment data
     * @param onFailure The callback for failed requests
     */
    public void removeSticky(OnResponse<RedditComment> onResponse, OnFailure onFailure) {
        modRequest.distinguishAsModComment(commentId, true, false, onResponse, onFailure);
    }
}
