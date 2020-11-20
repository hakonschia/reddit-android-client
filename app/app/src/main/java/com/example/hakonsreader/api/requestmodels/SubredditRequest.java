package com.example.hakonsreader.api.requestmodels;

import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.enums.PostTimeSort;
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
import com.example.hakonsreader.api.service.ImgurService;
import com.example.hakonsreader.api.service.SubredditService;
import com.example.hakonsreader.api.utils.Util;

import java.io.IOException;
import java.util.ArrayList;
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
    private final ImgurRequest imgurRequest;
    private final String subredditName;
    private final boolean loadImgurAlbumsAsRedditGalleries;

    /**
     *
     * @param accessToken The access token to use for requests
     * @param api The API service to use for requests
     * @param subredditName The name of the subreddit to make requests towards
     * @param imgurService The service to optionally use for loading Imgur albums directly. Set to
     *                     {@code null} to not load albums.
     */
    public SubredditRequest(AccessToken accessToken, SubredditService api, String subredditName, ImgurService imgurService) {
        this.accessToken = accessToken;
        this.api = api;
        this.subredditName = subredditName;
        this.imgurRequest = new ImgurRequest(imgurService);
        this.loadImgurAlbumsAsRedditGalleries = imgurService != null;
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
        // Ensure the subreddit name is lowercased when matching against standard subs
        if (RedditApi.STANDARD_SUBS.contains(subredditName.toLowerCase())) {
            onFailure.onFailure(new GenericError(-1), new NoSubredditInfoException("The subreddits: " + RedditApi.STANDARD_SUBS.toString() + " do not have any info to retrieve"));
            return;
        }

        api.getSubredditInfo(subredditName).enqueue(new Callback<RedditListing>() {
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
     * NOTE: the response for this request is sent on a background thread
     *
     * <p>Retrieves posts from the subreddit. The posts here are sorted by "hot", if you want to retrieve posts with a
     * different sort use {@link SubredditRequest#posts()}</p>
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
        this.getPosts("hot", null, after, count, onResponse, onFailure);
    }

    /**
     * <p>NOTE: the response for request is sent on a background thread</p>
     *
     * Retrieve an object to make API calls for posts in the subreddit
     *
     * @return An object that can retrieve new, top, and controversial posts for the subreddit
     */
    public SubredditPostsRequets posts() {
        return new SubredditPostsRequets();
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

        api.favoriteSubreddit(subredditName, favorite).enqueue(new Callback<Void>() {
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

        api.subscribeToSubreddit(subscribe ? "sub" : "unsub", subredditName).enqueue(new Callback<Void>() {
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
     * <p>NOTE: the response for this request is sent on a background thread</p>
     *
     * Retrieves posts from the subreddit
     *
     * <p>If an access token is set posts are customized for the user</p>
     *
     * <p>No specific OAuth scope is required</p>
     *
     * @param sort The sort for the posts (new, hot, top, or controversial)
     * @param timeSort How the posts should be time sorted. This only has an affect on top and controversial,
     *                 and can be set to null for new and hot
     * @param after The ID of the last post seen (or an empty string if first time loading)
     * @param count The amount of posts already retrieved (0 if first time loading)
     * @param onResponse The callback for successful requests. Holds a {@link List} of {@link RedditPost} objects
     * @param onFailure The callback for failed requests. If the subreddit doesn't exist this will be called
     */
    private void getPosts(String sort, PostTimeSort timeSort, String after, int count, OnResponse<List<RedditPost>> onResponse, OnFailure onFailure) {
        // Not front page, add r/ prefix
        String subreddit = subredditName;

        if (!subreddit.isEmpty()) {
            subreddit = "r/" + subreddit;
        }

        String finalSubreddit = subreddit;

        // Loading Imgur albums requires API calls inside the callback. If we use "enqueue" and operate
        // on the current thread the RedditPost objects will be updated after the response is given with
        // onResponse, which means the UI potentially wont be correct, so we have to run this entire thing on
        // a background thread
        new Thread(() -> {
            try {
                Response<ListingResponse> response = api.getPosts(
                        finalSubreddit,
                        sort,
                        timeSort == null ? "" : timeSort.getValue(),
                        after,
                        count,
                        RedditApi.RAW_JSON
                ).execute();

                okhttp3.Response prior = response.raw().priorResponse();

                // If the subreddit doesn't exist, Reddit wants to be helpful (or something) and redirects
                // the response to a search request instead. This causes issues as the response being sent back
                // now holds subreddits instead of posts, so if we have a prior request (which is the actual original request)
                // then call the failure handler as the user of the API might want to know that the sub doesn't exist
                // Additionaly, if the search request returned subreddits, body.getListings() will hold a List<Subreddit> which will cause issues
                // This is also an "issue" for SubredditRequest.info(), but it will manage to convert that to a RedditListing
                // and the check for getId() will return null, so it doesn't have to be handled directly
                // We could disable redirects, but I'm afraid of what issues that would cause later
                if (prior != null) {
                    // TODO when new access tokens are retrieved automatically, the prior response is also set which means
                    //  the posts aren't returned
                    onFailure.onFailure(new GenericError(response.code()), new SubredditNotFoundException("No subreddit found with name: " + subredditName));
                    return;
                }

                ListingResponse body = null;
                if (response.isSuccessful()) {
                    body = response.body();
                }

                if (body != null) {
                    List<RedditPost> posts = (List<RedditPost>) body.getListings();

                    if (loadImgurAlbumsAsRedditGalleries) {
                        imgurRequest.loadAlbums(posts);
                    }

                    onResponse.onResponse(posts);
                } else {
                    Util.handleHttpErrors(response, onFailure);
                }
            } catch (IOException e) {
                onFailure.onFailure(new GenericError(-1), e);
            }
        }).start();
    }


    /**
     * Class to retrieve posts from the subreddit. The functions declared here define how to sort the posts
     * (new, hot etc.)
     */
    public class SubredditPostsRequets {

        /**
         * NOTE: the response for this request is sent on a background thread
         *
         * <p>Get the "controversial" posts for the user</p>
         *
         * <p>OAuth scope required: {@code history}</p>
         *
         * @param after The ID of the last post seen (or an empty string if first time loading)
         * @param timeSort The time sort for the posts (of all time, of hour etc.)
         * @param count The amount of posts already retrieved (0 if first time loading)
         * @param onResponse The callback for successful requests. Holds a {@link List} of {@link RedditPost} objects
         * @param onFailure The callback for failed requests
         */
        public void controversial(PostTimeSort timeSort, String after, int count, OnResponse<List<RedditPost>> onResponse, OnFailure onFailure) {
            getPosts("controversial", timeSort, after, count, onResponse, onFailure);
        }

        /**
         * NOTE: the response for this request is sent on a background thread
         *
         * <p>Retrieves new posts from the subreddit</p>
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
        public void newPosts(String after, int count, OnResponse<List<RedditPost>> onResponse, OnFailure onFailure) {
            getPosts("new", null, after, count, onResponse, onFailure);
        }

        /**
         * NOTE: the response for this request is sent on a background thread
         *
         * <p>Retrieves top posts from the subreddit</p>
         *
         * <p>If an access token is set posts are customized for the user</p>
         *
         * <p>No specific OAuth scope is required</p>
         *
         * @param after The ID of the last post seen (or an empty string if first time loading)
         * @param timeSort The time sort for the posts (of all time, of hour etc.)
         * @param count The amount of posts already retrieved (0 if first time loading)
         * @param onResponse The callback for successful requests. Holds a {@link List} of {@link RedditPost} objects
         * @param onFailure The callback for failed requests
         */
        @EverythingIsNonNull
        public void top(PostTimeSort timeSort, String after, int count, OnResponse<List<RedditPost>> onResponse, OnFailure onFailure) {
            getPosts("top", timeSort, after, count, onResponse, onFailure);
        }
    }
}
