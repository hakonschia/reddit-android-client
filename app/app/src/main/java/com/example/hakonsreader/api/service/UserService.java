package com.example.hakonsreader.api.service;

import com.example.hakonsreader.api.model.RedditListing;
import com.example.hakonsreader.api.model.User;
import com.example.hakonsreader.api.responses.ListingResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Service interface to make user related API calls towards Reddit
 */
public interface UserService {

    /**
     * Retrieves information about the logged in user. For information about any user see
     * {@link UserService#getUserInfoOtherUsers(String, String)}
     *
     * <p>OAuth scope required: {@code identity}</p>
     *
     * @param accessToken The type of token + the actual token. Form: "type token"
     * @return A Call with {@link User}
     */
    @GET("api/v1/me?raw_json=1")
    Call<User> getUserInfo(@Header("Authorization") String accessToken);

    /**
     * Retrieves information about a user. This can retrieve information about users that aren't
     * the logged in user
     *
     * @param accessToken The type of token + the actual token. Form: "type token"
     * @return A Call with {@link User}
     */
    @GET("u/{username}/about?raw_json=1")
    Call<RedditListing> getUserInfoOtherUsers(
            @Path("username") String username,

            @Header("Authorization") String accessToken
    );

    /**
     * <p>OAauth scope required: {@code history}</p>
     *
     * @param username The username of the user to get listings for
     * @param what What kind of listings to get. Values possible:
     *             <ol>
     *                  <li>overview - A mix of comments and posts</li>
     *                  <li>submitted - Posts by the user</li>
     *                  <li>comments - Comments by the user</li>
     *                  <li>upvoted - Posts/comments upvoted by the user. This is only accessible for the user itself</li>
     *                  <li>downvoted - Posts/comments downvoted by the user. This is only accessible for the user itself</li>
     *                  <li>hidden - Posts/comments hidden by the user. This is only accessible for the user itself</li>
     *                  <li>saved - Posts/comments saved by the user. This is only accessible for the user itself</li>
     *             </ol>
     * @param sort How to sort the listings (top, new, hot, or controversial)
     * @param timeSort For listings that can be sorted by time, how to sort the listings (use {@link com.example.hakonsreader.api.enums.PostTimeSort}
     *                 and getValue())
     * @param accessToken The type of token + the actual token. Form: "type token"
     * @return A ListingResponse call. The listings returned depend on the value of "what" (comments, post, etc.)
     */
    @GET("user/{username}/{what}?raw_json=1")
    Call<ListingResponse> getListingsFromUser(
            @Path("username") String username,
            @Path("what") String what,
            @Query("sort") String sort,
            @Query("t") String timeSort,

            @Header("Authorization") String accessToken
    );

}
