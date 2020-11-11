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
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.api.responses.GenericError;
import com.example.hakonsreader.api.responses.ListingResponse;
import com.example.hakonsreader.api.responses.MoreCommentsResponse;
import com.example.hakonsreader.api.service.PostService;
import com.example.hakonsreader.api.utils.Util;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.internal.EverythingIsNonNull;


/**
 * Class that provides an interface towards the Reddit API related to post, such as retrieving
 * information about the post and commenting
 */
public class PostRequest implements VoteableRequest, ReplyableRequest, SaveableRequest {

    private final AccessToken accessToken;
    private final PostService api;
    private final String postId;

    private final VoteableRequestModel voteRequest;
    private final ReplyableRequestModel replyRequest;
    private final SaveableRequestModel saveRequest;

    public PostRequest(AccessToken accessToken, PostService api, String postId) {
        this.accessToken = accessToken;
        this.api = api;
        this.postId = postId;

        this.voteRequest = new VoteableRequestModel(accessToken, api);
        this.replyRequest = new ReplyableRequestModel(accessToken, api);
        this.saveRequest = new SaveableRequestModel(accessToken, api);
    }

    /**
     * Retrieve comments for the post. These comments are sorted by "best", see other functions for other sorts
     *
     * <p>OAuth scopes required:
     * <ol>
     *     <li>To retrieve posts customized for a logged in user (with vote status set etc.): {@code read}</li>
     *     <li>To retrieve generic posts no OAuth scope is required</li>
     * </ol>
     * </p>
     * <p>No specific OAuth scope is required</p>
     *
     * @param onResponse The callback for successful requests. Holds a {@link List} of {@link RedditComment} objects
     * @param onPostResponse This callback is also for successful requests and holds the information about the post the comments are for
     * @param onFailure The callback for failed requests
     */
    @EverythingIsNonNull
    public void comments(OnResponse<List<RedditComment>> onResponse, OnResponse<RedditPost> onPostResponse, OnFailure onFailure) {
        this.getComments("best", onResponse, onPostResponse, onFailure);
    }

    /**
     * Retrieve comments for the post sorted by "new"
     *
     * <p>OAuth scopes required:
     * <ol>
     *     <li>To retrieve posts customized for a logged in user (with vote status set etc.): {@code read}</li>
     *     <li>To retrieve generic posts no OAuth scope is required</li>
     * </ol>
     * </p>
     * <p>No specific OAuth scope is required</p>
     *
     * @param onResponse The callback for successful requests. Holds a {@link List} of {@link RedditComment} objects
     * @param onPostResponse This callback is also for successful requests and holds the information about the post the comments are for
     * @param onFailure The callback for failed requests
     */
    @EverythingIsNonNull
    public void newComments(OnResponse<List<RedditComment>> onResponse, OnResponse<RedditPost> onPostResponse, OnFailure onFailure) {
        this.getComments("new", onResponse, onPostResponse, onFailure);
    }

    /**
     * Retrieve comments for the post sorted by "top"
     *
     * <p>OAuth scopes required:
     * <ol>
     *     <li>To retrieve posts customized for a logged in user (with vote status set etc.): {@code read}</li>
     *     <li>To retrieve generic posts no OAuth scope is required</li>
     * </ol>
     * </p>
     * <p>No specific OAuth scope is required</p>
     *
     * @param onResponse The callback for successful requests. Holds a {@link List} of {@link RedditComment} objects
     * @param onPostResponse This callback is also for successful requests and holds the information about the post the comments are for
     * @param onFailure The callback for failed requests
     */
    @EverythingIsNonNull
    public void topComments(OnResponse<List<RedditComment>> onResponse, OnResponse<RedditPost> onPostResponse, OnFailure onFailure) {
        this.getComments("top", onResponse, onPostResponse, onFailure);
    }

    /**
     * Retrieve comments for the post sorted by "controversial"
     *
     * <p>OAuth scopes required:
     * <ol>
     *     <li>To retrieve posts customized for a logged in user (with vote status set etc.): {@code read}</li>
     *     <li>To retrieve generic posts no OAuth scope is required</li>
     * </ol>
     * </p>
     * <p>No specific OAuth scope is required</p>
     *
     * @param onResponse The callback for successful requests. Holds a {@link List} of {@link RedditComment} objects
     * @param onPostResponse This callback is also for successful requests and holds the information about the post the comments are for
     * @param onFailure The callback for failed requests
     */
    @EverythingIsNonNull
    public void controversialComments(OnResponse<List<RedditComment>> onResponse, OnResponse<RedditPost> onPostResponse, OnFailure onFailure) {
        this.getComments("controversial", onResponse, onPostResponse, onFailure);
    }

    /**
     * Retrieve comments for the post sorted by "old"
     *
     * <p>OAuth scopes required:
     * <ol>
     *     <li>To retrieve posts customized for a logged in user (with vote status set etc.): {@code read}</li>
     *     <li>To retrieve generic posts no OAuth scope is required</li>
     * </ol>
     * </p>
     * <p>No specific OAuth scope is required</p>
     *
     * @param onResponse The callback for successful requests. Holds a {@link List} of {@link RedditComment} objects
     * @param onPostResponse This callback is also for successful requests and holds the information about the post the comments are for
     * @param onFailure The callback for failed requests
     */
    @EverythingIsNonNull
    public void oldComments(OnResponse<List<RedditComment>> onResponse, OnResponse<RedditPost> onPostResponse, OnFailure onFailure) {
        this.getComments("old", onResponse, onPostResponse, onFailure);
    }

    /**
     * Retrieves comments initially hidden (from "2 more comments" comments)
     *
     * <p>If an access token is set comments are customized for the user (ie. vote status is set)</p>
     *
     * <p>OAuth scope required: {@code read}</p>
     *
     * @param children The list of IDs of comments to get (retrieved via {@link RedditComment#getChildren()})
     * @param parent Optional: The parent comment the new comments belong to. If this sets the new comments
     *               as replies directly. This is the same as calling {@link RedditComment#addReplies(List)} afterwards.
     *               Note that this is the parent of the new comments, not the comment holding the list children
     *               retrieved with {@link RedditComment#getChildren()}.
     * @param onResponse The callback for successful requests. Holds a {@link List} of {@link RedditComment} objects
     * @param onFailure The callback for failed requests
     */
    public void moreComments(List<String> children, RedditComment parent, OnResponse<List<RedditComment>> onResponse, OnFailure onFailure) {
        // If no children are given, just return an empty list as it's not strictly an error but it will cause an API error later on
        if (children.isEmpty()) {
            onResponse.onResponse(new ArrayList<>());
            return;
        }

        String postFullname = Thing.POST.getValue() + "_" + postId;

        // The query parameter for the children is a list of comma separated IDs
        String childrenText = String.join(",", children);

        api.getMoreComments(
                childrenText,
                postFullname,
                RedditApi.API_TYPE,
                RedditApi.RAW_JSON,
                accessToken.generateHeaderString()
        ).enqueue(new Callback<MoreCommentsResponse>() {
            @Override
            public void onResponse(Call<MoreCommentsResponse> call, Response<MoreCommentsResponse> response) {
                MoreCommentsResponse body = null;
                if (response.isSuccessful()) {
                    body = response.body();
                }

                if (body != null) {
                    if (!body.hasErrors()) {
                        List<RedditComment> comments = body.getComments();

                        if (parent != null) {
                            parent.addReplies(comments);
                        }
                        onResponse.onResponse(comments);
                    } else {
                        Util.handleListingErrors(body.errors(), onFailure);
                    }
                } else {
                    Util.handleHttpErrors(response, onFailure);
                }
            }

            @Override
            public void onFailure(Call<MoreCommentsResponse> call, Throwable t) {
                onFailure.onFailure(new GenericError(-1), t);
            }
        });
    }

    /**
     * Vote on the post
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
    @EverythingIsNonNull
    @Override
    public void vote(VoteType type, OnResponse<Void> onResponse, OnFailure onFailure) {
        voteRequest.vote(Thing.POST, postId, type, onResponse, onFailure);
    }

    /**
     * Submit a new comment as a reply to the post. For replies to other comments use {@link CommentRequest#reply(String, OnResponse, OnFailure)}
     *
     * <p>Requires a user access token to be set. {@code onFailure} will be called if no access token is set</p>
     *
     * <p>OAuth scope required: {@code submit}</p>
     *
     * @param comment The comment to submit, formatted as <a href="https://en.wikipedia.org/wiki/Markdown">Markdown</a>
     * @param onResponse Callback for successful responses. Holds the newly created comment
     * @param onFailure Callback for failed requests
     */
    @Override
    public void reply(String comment, OnResponse<RedditComment> onResponse, OnFailure onFailure) {
        replyRequest.postComment(Thing.POST, postId, comment, onResponse, onFailure);
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
        saveRequest.save(Thing.POST, postId, onResponse, onFailure);
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
        saveRequest.unsave(Thing.POST, postId, onResponse, onFailure);
    }


    /**
     * Get comments for the post
     *
     * @param sort How to sort the comments (new, best, top, controversial)
     * @param onResponse The response listener for the comments
     * @param onPostResponse The response listener that holds the post information
     * @param onFailure Failure handler
     */
    private void getComments(String sort, OnResponse<List<RedditComment>> onResponse, OnResponse<RedditPost> onPostResponse, OnFailure onFailure) {
        api.getComments(
                postId,
                sort,
                accessToken.generateHeaderString()
        ).enqueue(new Callback<List<ListingResponse>>() {
            @Override
            public void onResponse(Call<List<ListingResponse>> call, Response<List<ListingResponse>> response) {
                List<ListingResponse> body = null;
                if (response.isSuccessful()) {
                    body = response.body();
                }

                if (body != null) {
                    // For comments the first listing object is the post itself and the second its comments
                    RedditPost post = (RedditPost) body.get(0).getListings().get(0);
                    List<RedditComment> topLevelComments = (List<RedditComment>) body.get(1).getListings();

                    List<RedditComment> allComments = new ArrayList<>();
                    topLevelComments.forEach(comment -> {
                        // Add the comment itself and all its replies
                        allComments.add(comment);
                        allComments.addAll(comment.getReplies());
                    });

                    onPostResponse.onResponse(post);
                    onResponse.onResponse(allComments);
                } else {
                    Util.handleHttpErrors(response, onFailure);
                }
            }

            @Override
            public void onFailure(Call<List<ListingResponse>> call, Throwable t) {
                onFailure.onFailure(new GenericError(-1), t);
            }
        });
    }
}
