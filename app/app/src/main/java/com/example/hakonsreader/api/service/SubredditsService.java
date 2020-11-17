package com.example.hakonsreader.api.service;

import com.example.hakonsreader.api.responses.ListingResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

/**
 * Service interface to make API calls towards Reddit for multiple subreddits.
 * This differs from {@link SubredditService} as this is a service for multiple subreddits, not one
 * specific subreddit
 */
public interface SubredditsService {
    /**
     * Retrieve a list of the users subscribed subreddits
     *
     * @param after
     * @param count
     * @param limit
     * @return
     */
    @GET("subreddits/mine/subscriber?raw_json=1")
    Call<ListingResponse> getSubscribedSubreddits(
            @Query("after") String after,
            @Query("count") int count,
            @Query("limit") int limit
    );

    /**
     * Retrieve a list of the the default subreddits (as set by reddit)
     *
     * @param after
     * @param count
     * @param limit
     * @return
     */
    @GET("subreddits/default?raw_json=1")
    Call<ListingResponse> getDefaultSubreddits(
            @Query("after") String after,
            @Query("count") int count,
            @Query("limit") int limit
    );


    /**
     * Search for subreddits
     *
     * @param query
     * @return
     */
    @GET("subreddits/search")
    Call<ListingResponse> searchForSubreddits(@Query("q") String query);
}
