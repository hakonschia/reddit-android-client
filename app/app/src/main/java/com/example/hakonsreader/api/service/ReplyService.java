package com.example.hakonsreader.api.service;

import com.example.hakonsreader.api.model.RedditComment;
import com.example.hakonsreader.api.responses.JsonResponse;
import com.example.hakonsreader.api.responses.JsonResponseKt;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Header;
import retrofit2.http.POST;

/**
 * Service interface for listings that can be replied to
 */
public interface ReplyService {

    /**
     * Submit a new comment to either a post, another comment (reply), or a private message.
     *
     * @param comment The raw markdown of the comment
     * @param parentId The fullname of the thing being replied to
     * @param apiType The string "json"
     * @param returnJson The boolean value "false". MUST be set to false
     * @return A call that holds the newly created comment
     */
    @POST("api/comment")
    @FormUrlEncoded
    Call<JsonResponseKt<RedditComment>> postComment(
            @Field("text") String comment,
            @Field("thing_id") String parentId,
            @Field("api_type") String apiType,
            @Field("return_rtjson") boolean returnJson
    );
}
