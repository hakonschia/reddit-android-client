package com.example.hakonsreader.api.requestmodels;

import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.enums.Thing;
import com.example.hakonsreader.api.interfaces.OnFailure;
import com.example.hakonsreader.api.interfaces.OnResponse;
import com.example.hakonsreader.api.model.AccessToken;
import com.example.hakonsreader.api.model.RedditComment;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.api.responses.GenericError;
import com.example.hakonsreader.api.responses.JsonResponse;
import com.example.hakonsreader.api.service.ModService;
import com.example.hakonsreader.api.utils.Util;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ModRequestModel {

    private final AccessToken accessToken;
    private final ModService api;

    public ModRequestModel(AccessToken accessToken, ModService api) {
        this.accessToken = accessToken;
        this.api = api;
    }

    /**
     * Distinguish a comment as mod, and optionally sticky it
     *
     * @param id The ID of the comment to distinguish
     * @param distinguish True to distinguish as mod, false to remove previous distinguish
     * @param sticky True to also sticky the comment. This is only possible on top-level comments
     * @param onResponse Callback for successful requests. Holds the updated comment info
     * @param onFailure Callback for failed requests.
     */
    public void distinguishAsModComment(String id, boolean distinguish, boolean sticky, OnResponse<RedditComment> onResponse, OnFailure onFailure) {
        api.distinguishAsModComment(
                Util.createFullName(Thing.COMMENT, id),
                distinguish ? "yes" : "no",
                sticky,
                RedditApi.API_TYPE,
                accessToken.generateHeaderString()
        ).enqueue(new Callback<JsonResponse>() {
            @Override
            public void onResponse(Call<JsonResponse> call, Response<JsonResponse> response) {
                if (response.isSuccessful()) {
                    JsonResponse body = response.body();

                    if (body != null) {
                        RedditComment comment = (RedditComment) body.getListings().get(0);
                        onResponse.onResponse(comment);
                    } else {
                        Util.handleHttpErrors(response, onFailure);
                    }
                } else {
                    Util.handleHttpErrors(response, onFailure);
                }
            }

            @Override
            public void onFailure(Call<JsonResponse> call, Throwable t) {
                onFailure.onFailure(new GenericError(-1), t);
            }
        });
    }

    /**
     * Distinguish a post as mod
     *
     * @param id The ID of the comment to distinguish
     * @param distinguish True to distinguish as mod, false to remove previous distinguish
     * @param onResponse Callback for successful requests. Holds the updated comment info
     * @param onFailure Callback for failed requests.
     */
    public void distinguishAsModPost(String id, boolean distinguish, OnResponse<RedditPost> onResponse, OnFailure onFailure) {
        api.distinguishAsModPost(
                Util.createFullName(Thing.POST, id),
                distinguish ? "yes" : "no",
                RedditApi.API_TYPE,
                accessToken.generateHeaderString()
        ).enqueue(new Callback<JsonResponse>() {
            @Override
            public void onResponse(Call<JsonResponse> call, Response<JsonResponse> response) {
                if (response.isSuccessful()) {
                    JsonResponse body = response.body();

                    if (body != null) {
                        RedditPost comment = (RedditPost) body.getListings().get(0);
                        onResponse.onResponse(comment);
                    } else {
                        Util.handleHttpErrors(response, onFailure);
                    }
                } else {
                    Util.handleHttpErrors(response, onFailure);
                }
            }

            @Override
            public void onFailure(Call<JsonResponse> call, Throwable t) {
                onFailure.onFailure(new GenericError(-1), t);
            }
        });
    }

    /**
     * Sticky or unsticky a post
     *
     * @param id The id of the post
     * @param sticky True to sticky, false to unsticky. If the post is already stickied and this is true,
     *               a 409 Conflict error will occur
     * @param onResponse The callback for successful requests. Nothing is returned here, but the callback
     *                   will be called when the request is successful
     * @param onFailure The callback for failed requests
     */
    public void stickyPost(String id, boolean sticky, OnResponse<Void> onResponse, OnFailure onFailure) {
        api.stickyPost(
                Util.createFullName(Thing.POST, id),
                sticky,
                RedditApi.API_TYPE,
                accessToken.generateHeaderString()
        ).enqueue(new Callback<JsonResponse>() {
            @Override
            public void onResponse(Call<JsonResponse> call, Response<JsonResponse> response) {
                if (response.isSuccessful()) {
                    JsonResponse body = response.body();

                    if (body != null) {
                        // There is no actual data in the response
                        onResponse.onResponse(null);
                    } else {
                        Util.handleHttpErrors(response, onFailure);
                    }
                } else {
                    Util.handleHttpErrors(response, onFailure);
                }
            }

            @Override
            public void onFailure(Call<JsonResponse> call, Throwable t) {
                onFailure.onFailure(new GenericError(-1), t);
            }
        });
    }
}
