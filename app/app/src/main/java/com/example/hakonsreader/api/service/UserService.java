package com.example.hakonsreader.api.service;

import com.example.hakonsreader.api.model.RedditUser;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Service interface to make user related API calls towards Reddit
 */
public interface UserService {

    /**
     * Retrieves information about the logged in user. For information about any user see
     *
     * <p>OAuth scope required: {@code identity}</p>
     *
     * @return A Call with {@link RedditUser}
     */
    @GET("api/v1/me?raw_json=1")
    Call<RedditUser> getUserInfo();

}
