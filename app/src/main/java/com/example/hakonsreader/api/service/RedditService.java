package com.example.hakonsreader.api.service;

import com.example.hakonsreader.api.model.AccessToken;

import retrofit2.Call;
import retrofit2.http.GET;

public class RedditService {

    @GET
    public Call<AccessToken> getAccessToken() {
        
    }
}
