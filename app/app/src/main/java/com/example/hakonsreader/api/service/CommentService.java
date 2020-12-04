package com.example.hakonsreader.api.service;

import com.example.hakonsreader.api.responses.JsonResponse;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

/**
 * Service interface towards a comment on Reddit
 */
public interface CommentService extends ReplyService, SaveService, ModService {


    /**
     * Edits a comment
     *
     * @param commentFullname The fullname of the comment to edit
     * @param text The updated text
     * @param apiType The string "json"
     * @return A Call ready to execute the request
     */
    @POST("api/editusertext")
    @FormUrlEncoded
    Call<JsonResponse> edit(
            @Field("thing_id") String commentFullname,
            @Field("text") String text,
            @Field("api_type") String apiType
    );

    /**
     * Delete a comment
     *
     * @param commentFullname The fullname of the comment to delete
     * @return A Call ready to execute the request
     */
    @POST("api/del")
    @FormUrlEncoded
    Call<Void> delete(@Field("id") String commentFullname);

}
