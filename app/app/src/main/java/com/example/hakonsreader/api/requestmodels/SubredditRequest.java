package com.example.hakonsreader.api.requestmodels;

import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.exceptions.InvalidAccessTokenException;
import com.example.hakonsreader.api.exceptions.NoSubredditInfoException;
import com.example.hakonsreader.api.exceptions.SubredditNotFoundException;
import com.example.hakonsreader.api.interfaces.OnFailure;
import com.example.hakonsreader.api.interfaces.OnResponse;
import com.example.hakonsreader.api.model.AccessToken;
import com.example.hakonsreader.api.model.RedditListing;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.api.model.Subreddit;
import com.example.hakonsreader.api.responses.GenericError;
import com.example.hakonsreader.api.responses.ListingResponse;
import com.example.hakonsreader.api.service.SubredditService;
import com.example.hakonsreader.api.utils.Util;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.internal.EverythingIsNonNull;

/**
 * Class that provides an interface towards the Reddit API related to subreddits, such as retrieving
 * posts for the subreddit and subscribing to the subreddit
 */
public class SubredditRequest {

    private final AccessToken accessToken;
    private final SubredditService api;
    private final String subredditName;

    public SubredditRequest(AccessToken accessToken, SubredditService api, String subredditName) {
        this.accessToken = accessToken;
        this.api = api;
        this.subredditName = subredditName;
    }

    /**
     * Retrieve information about the subreddit
     *
     * <p>OAuth scope required: {@code read}</p>
     *
     * @param onResponse The response handler for successful requests. Holds the {@link Subreddit} retrieved
     * @param onFailure The response handler for failed requests. If the function is called with a "standard"
     *                  subreddit (front page, popular, all) this will be called
     */
    public void info(OnResponse<Subreddit> onResponse, OnFailure onFailure) {
        if (RedditApi.STANDARD_SUBS.contains(subredditName)) {
            onFailure.onFailure(new GenericError(-1), new NoSubredditInfoException("The subreddits: " + RedditApi.STANDARD_SUBS.toString() + " do not have any info to retrieve"));
            return;
        }

        api.getSubredditInfo(subredditName, accessToken.generateHeaderString()).enqueue(new Callback<RedditListing>() {
            @Override
            public void onResponse(Call<RedditListing> call, Response<RedditListing> response) {
                RedditListing body = null;
                if (response.isSuccessful()) {
                    body = response.body();
                }

                if (body != null) {
                    if (body.getId() != null) {
                        onResponse.onResponse((Subreddit) body);
                    } else {
                        onFailure.onFailure(new GenericError(response.code()), new SubredditNotFoundException("No subreddit found with name: " + subredditName));
                    }
                } else {
                    Util.handleHttpErrors(response, onFailure);
                }
            }

            @Override
            public void onFailure(Call<RedditListing> call, Throwable t) {
                onFailure.onFailure(new GenericError(-1), t);
            }
        });
    }

    /**
     * Asynchronously retrieves posts from the subreddit
     *
     * <p>If an access token is set posts are customized for the user</p>
     *
     * <p>No specific OAuth scope is required</p>
     *
     * @param after The ID of the last post seen (or an empty string if first time loading)
     * @param count The amount of posts already retrieved (0 if first time loading)
     * @param onResponse The callback for successful requests. Holds a {@link List} of {@link RedditPost} objects
     * @param onFailure The callback for failed requests
     */
    @EverythingIsNonNull
    public void posts(String after, int count, OnResponse<List<RedditPost>> onResponse, OnFailure onFailure) {
        // Not front page, add r/ prefix
        String subreddit = subredditName;

        if (!subreddit.isEmpty()) {
            subreddit = "r/" + subreddit;
        }

        api.getPosts(
                subreddit,
                "hot",
                after,
                count,
                RedditApi.RAW_JSON,
                accessToken.generateHeaderString()
        ).enqueue(new Callback<ListingResponse>() {
            @Override
            public void onResponse(Call<ListingResponse> call, Response<ListingResponse> response) {
                ListingResponse body = null;
                if (response.isSuccessful()) {
                    body = response.body();
                }

                if (body != null) {
                    List<RedditPost> posts = (List<RedditPost>) body.getListings();
                    onResponse.onResponse(posts);
                } else {
                    Util.handleHttpErrors(response, onFailure);
                }
            }

            @Override
            public void onFailure(Call<ListingResponse> call, Throwable t) {
                onFailure.onFailure(new GenericError(-1), t);
            }
        });
    }


    /**
     * Favorite or un-favorite a subreddit
     *
     * @param favorite True if the action should be to favorite, false to un-favorite
     * @param onResponse The response handler for successful requests. Does not hold any data, but will
     *                   be called when the request succeeds.
     * @param onFailure Callback for failed requests
     */
    public void favorite(boolean favorite, OnResponse<Void> onResponse, OnFailure onFailure) {
        try {
            Util.verifyLoggedInToken(accessToken);
        } catch (InvalidAccessTokenException e) {
            onFailure.onFailure(new GenericError(-1), new InvalidAccessTokenException("Favoriting a subreddit requires a valid access token for a logged in user", e));
            return;
        }

        api.favoriteSubreddit(subredditName, favorite, accessToken.generateHeaderString()).enqueue(new Callback<Void>() {
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
     * Subscribe or unsubscribe to a subreddit
     *
     * <p>OAuth scope required: {@code subscribe}</p>
     *
     * @param subscribe True if the action should be to subscribe, false to unsubscribe
     * @param onResponse The response handler for successful requests. Does not hold any data, but will
     *                   be called when the request succeeds.
     * @param onFailure Callback for failed requests
     */
    public void subscribe(boolean subscribe, OnResponse<Void> onResponse, OnFailure onFailure) {
        try {
            Util.verifyLoggedInToken(accessToken);
        } catch (InvalidAccessTokenException e) {
            onFailure.onFailure(new GenericError(-1), new InvalidAccessTokenException("Subscribing to a subreddit requires a valid access token for a logged in user", e));
            return;
        }

        api.subscribeToSubreddit(subscribe ? "sub" : "unsub", subredditName, accessToken.generateHeaderString()).enqueue(new Callback<Void>() {
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
}
