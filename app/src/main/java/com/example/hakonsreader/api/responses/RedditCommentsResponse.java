package com.example.hakonsreader.api.responses;

import com.example.hakonsreader.api.model.RedditComment;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class RedditCommentsResponse {

    private Data data;

    // The JSON structure has a "data" object with the posts in an array called "children"
    private static class Data {
        @SerializedName("children")
        private List<RedditComment> comments;
    }


    public List<RedditComment> getComments() {
        return this.data.comments;
    }


}
