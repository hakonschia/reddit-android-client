package com.example.hakonsreader.api.service;

import com.example.hakonsreader.api.responses.JsonResponse;
import com.example.hakonsreader.api.responses.ListingResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Service interface to make post related API calls towards Reddit
 */
public interface PostService extends VoteService, ReplyService, SaveService, ModService {
    /**
     * Retrieves comments for a post
     *
     * @param postID The ID of the post to retrieve comments for
     * @param sort How to sort the comments (new, hot, top etc.)
     * @param accessToken The type of token + the actual token. Form: "type token". This can be omitted (an empty string)
     *                    to retrieve comments without a logged in user
     * @return A list of {@link ListingResponse}. The first item in this list is the post itself.
     * The actual comments is found in the second element of the list
     */
    @GET("comments/{postID}?raw_json=1")
    Call<List<ListingResponse>> getComments(
            @Path("postID") String postID,
            @Query("sort") String sort,

            @Header("Authorization") String accessToken
    );

    /**
     * Retrieves more comments (from "4 more comments" comments)
     *
     * @param children A comma separated string of the IDs of the comments to load
     * @param linkId The fullname of the post the comments are in
     * @param apiType The string "json"
     * @param rawJson Set to 1 if the response should be raw JSON
     * @param accessToken The type of token + the actual token. Form: "type token". This can be omitted (an empty string)
     *                    to retrieve comments without a logged in user
     * @return A call with {@link JsonResponse}
     */
    @POST("api/morechildren")
    @FormUrlEncoded
    Call<JsonResponse> getMoreComments(
            @Field("children") String children,
            @Field("link_id") String linkId,
            @Field("api_type") String apiType,
            @Field("raw_json") int rawJson,

            @Header("Authorization") String accessToken
    );
}
