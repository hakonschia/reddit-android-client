package com.example.hakonsreader.api.requestmodels;

import com.example.hakonsreader.api.enums.Thing;
import com.example.hakonsreader.api.enums.VoteType;
import com.example.hakonsreader.api.interfaces.OnFailure;
import com.example.hakonsreader.api.interfaces.OnResponse;
import com.example.hakonsreader.api.interfaces.ReplyableRequest;
import com.example.hakonsreader.api.interfaces.VoteableRequest;
import com.example.hakonsreader.api.model.AccessToken;
import com.example.hakonsreader.api.model.RedditComment;
import com.example.hakonsreader.api.service.CommentService;

import retrofit2.internal.EverythingIsNonNull;

/**
 * Class that provides an interface towards the Reddit API related to comments, such as
 * replying to the comment or voting on it
 */
public class CommentRequest implements VoteableRequest, ReplyableRequest {

    private final String commentId;
    private final VoteableRequestModel voteRequest;
    private final ReplyableRequestModel replyRequest;

    public CommentRequest(String commentId, AccessToken accessToken, CommentService api) {
        this.commentId = commentId;

        this.voteRequest = new VoteableRequestModel(accessToken, api);
        this.replyRequest = new ReplyableRequestModel(accessToken, api);
    }

    /**
     * Submit a new comment as a reply to another comment. Note: The depth of the new comment is not
     * set (it is -1) and must be set manually with {@link RedditComment#setDepth(int)} (as the parents depth + 1)
     *
     * <p>Requires a user access token to be set. {@code onFailure} will be called if no access token is set</p>
     *
     * <p>OAuth scope required: {@code submit} </p>
     *
     * @param comment The comment to submit, formatted as <a href="https://en.wikipedia.org/wiki/Markdown">Markdown</a>
     * @param onResponse Callback for successful responses. Holds the newly created comment
     * @param onFailure Callback for failed requests
     */
    @Override
    @EverythingIsNonNull
    public void reply(String comment, OnResponse<RedditComment> onResponse, OnFailure onFailure) {
        replyRequest.postComment(Thing.COMMENT, commentId, comment, onResponse, onFailure);
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
}
