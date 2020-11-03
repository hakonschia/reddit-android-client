package com.example.hakonsreader.api.requestmodels;

import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.enums.Thing;
import com.example.hakonsreader.api.exceptions.InvalidAccessTokenException;
import com.example.hakonsreader.api.interfaces.OnFailure;
import com.example.hakonsreader.api.interfaces.OnResponse;
import com.example.hakonsreader.api.model.AccessToken;
import com.example.hakonsreader.api.model.RedditComment;
import com.example.hakonsreader.api.responses.GenericError;
import com.example.hakonsreader.api.responses.MoreCommentsResponse;
import com.example.hakonsreader.api.service.ReplyService;
import com.example.hakonsreader.api.utils.Util;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Request class to add a reply (to comment, post etc.). This is a convenience class that other request
 * classes that allow for voting can use, and is not exposed outside the API package
 */
class ReplyableRequestModel {

    private final AccessToken accessToken;
    private final ReplyService api;

    public ReplyableRequestModel(AccessToken accessToken, ReplyService api) {
        this.accessToken = accessToken;
        this.api = api;
    }


    /**
     * Submit a new comment as a reply to another comment. Note: The depth of the new comment for replies is not
     * set (it is -1) and must be set manually with {@link RedditComment#setDepth(int)} (as the parents depth + 1)
     *
     * <p>Requires a user access token to be set. {@code onFailure} will be called if no access token is set</p>
     *
     * <p>OAuth scopes required:
     * <ol>
     *     <li>For comments to post and replies: {@code submit}</li>
     *     <li>For private messages: {@code privatemessage}</li>
     * </ol>
     * </p>
     *
     * @param thing The type of thing to comment on
     * @param thingId The ID of the thing being commented on
     * @param comment The comment to submit, formatted as <a href="https://en.wikipedia.org/wiki/Markdown">Markdown</a>
     * @param onResponse Callback for successful responses. Holds the newly created comment
     * @param onFailure Callback for failed requests
     */
    public void postComment(Thing thing, String thingId, String comment, OnResponse<RedditComment> onResponse, OnFailure onFailure) {
        try {
            Util.verifyLoggedInToken(accessToken);
        } catch (InvalidAccessTokenException e) {
            onFailure.onFailure(new GenericError(-1), new InvalidAccessTokenException("Posting comments requires a valid access token for a logged in user"));
            return;
        }

        api.postComment(
                comment,
                Util.createFullName(thing, thingId),
                RedditApi.API_TYPE,
                false,
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
                        RedditComment newComment = body.getComments().get(0);

                        newComment.setDepth(thing == Thing.POST ? 0 : -1);
                        onResponse.onResponse(newComment);
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
}
