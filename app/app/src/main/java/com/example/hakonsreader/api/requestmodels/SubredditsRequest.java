package com.example.hakonsreader.api.requestmodels;

import com.example.hakonsreader.api.exceptions.InvalidAccessTokenException;
import com.example.hakonsreader.api.interfaces.OnFailure;
import com.example.hakonsreader.api.interfaces.OnResponse;
import com.example.hakonsreader.api.model.AccessToken;
import com.example.hakonsreader.api.model.Subreddit;
import com.example.hakonsreader.api.responses.ListingResponse;
import com.example.hakonsreader.api.service.SubredditsService;
import com.example.hakonsreader.api.utils.Util;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Class that provides an interface towards the Reddit API related to subreddits.
 * This differs from {@link SubredditRequest} as this is a class for multiple subreddits, not one
 * specific subreddit
 */
public class SubredditsRequest {

    private final AccessToken accessToken;
    private final SubredditsService api;

    public SubredditsRequest(AccessToken accessToken, SubredditsService api) {
        this.accessToken = accessToken;
        this.api = api;
    }


    /**
     * Retrieve the list of subreddits the logged in user is subscribed to
     *
     * <p>OAuth scope required: {@code mysubreddits}</p>
     *
     * @param after The ID of the last subreddit seen (empty string if loading for the first time)
     * @param count The amount of items fetched previously (0 if loading for the first time)
     * @param onResponse The response handler for successful request. Holds the list of subreddits fetched.
     *                   This list is not sorted
     * @param onFailure The response handler for failed requests
     */
    public void subscribedSubreddits(String after, int count, OnResponse<List<Subreddit>> onResponse, OnFailure onFailure) {
        try {
            Util.verifyLoggedInToken(accessToken);
        } catch (InvalidAccessTokenException e) {
            onFailure.onFailure(-1, new InvalidAccessTokenException("Getting subscribed subreddits requires a valid access token for a logged in user", e));
            return;
        }

        api.getSubscribedSubreddits(
                after,
                count,
                100,
                accessToken.generateHeaderString()
        ).enqueue(new Callback<ListingResponse>() {
            @Override
            public void onResponse(Call<ListingResponse> call, Response<ListingResponse> response) {
                ListingResponse body = null;
                if (response.isSuccessful()) {
                    body = response.body();
                }

                if (body != null) {
                    if (!body.hasErrors()) {
                        List<Subreddit> subreddits = (List<Subreddit>) body.getListings();
                        onResponse.onResponse(subreddits);
                    } else {
                        Util.handleListingErrors(body.getErrors(), onFailure);
                    }
                } else {
                    onFailure.onFailure(response.code(), Util.newThrowable(response.code()));
                }
            }

            @Override
            public void onFailure(Call<ListingResponse> call, Throwable t) {
                onFailure.onFailure(-1, t);
            }
        });
    }

    /**
     * Retrieve the list of default subreddits (as selected by reddit)
     *
     * <p>OAuth scope required: {@code read}</p>
     *
     * @param after The ID of the last subreddit seen (empty string if loading for the first time)
     * @param count The amount of items fetched previously (0 if loading for the first time)
     * @param onResponse The response handler for successful request. Holds the list of subreddits fetched.
     *                   This list is not sorted
     * @param onFailure The response handler for failed requests
     */
    public void defaultSubreddits(String after, int count, OnResponse<List<Subreddit>> onResponse, OnFailure onFailure) {
        api.getDefaultSubreddits(
                after,
                count,
                100,
                accessToken.generateHeaderString()
        ).enqueue(new Callback<ListingResponse>() {
            @Override
            public void onResponse(Call<ListingResponse> call, Response<ListingResponse> response) {
                ListingResponse body = null;
                if (response.isSuccessful()) {
                    body = response.body();
                }

                if (body != null) {
                    List<Subreddit> subreddits = (List<Subreddit>) body.getListings();
                    onResponse.onResponse(subreddits);
                } else {
                    onFailure.onFailure(response.code(), Util.newThrowable(response.code()));
                }
            }

            @Override
            public void onFailure(Call<ListingResponse> call, Throwable t) {
                onFailure.onFailure(-1, t);
            }
        });
    }

    /**
     * Retrieves subreddits. If there is a user logged in the users subscribed subreddits are retrieved, if
     * not the default ones are retrieved.
     *
     * <p>See also {@link SubredditsRequest#subscribedSubreddits(String, int, OnResponse, OnFailure)} and
     * {@link SubredditsRequest#defaultSubreddits(String, int, OnResponse, OnFailure)}</p>
     *
     * <ol>
     *     <li>For a users subscribed subreddits: {@code mysubreddits}</li>
     *     <li>For default subreddits: {@code read}</li>
     * </ol>
     * </p>
     *
     * @param after The ID of the last subreddit seen (empty string if loading for the first time)
     * @param count The amount of items fetched previously (0 if loading for the first time)
     * @param onResponse The response handler for successful request. Holds the list of subreddits fetched.
     *                   This list is not sorted
     * @param onFailure The response handler for failed requests
     */
    public void getSubreddits(String after, int count, OnResponse<List<Subreddit>> onResponse, OnFailure onFailure) {
        try {
            Util.verifyLoggedInToken(accessToken);
            this.subscribedSubreddits(after, count, onResponse, onFailure);
        } catch (InvalidAccessTokenException e) {
            this.defaultSubreddits(after, count, onResponse, onFailure);
        }
    }
}
