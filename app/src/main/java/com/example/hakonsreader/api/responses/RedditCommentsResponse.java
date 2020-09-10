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


    /**
     * Retrieves the list of top level comments
     * Note: Only the direct top level comments are received here, the rest are as replies within the comments
     *
     * @return The list of comments from the response
     */
    public List<RedditComment> getComments() {
        return this.data.comments;
    }


}
