package com.example.hakonsreader.api.responses;

import com.example.hakonsreader.api.model.RedditComment;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Response for retrieving more comments in a comment chain
 */
public class MoreCommentsResponse {

    private Json json;
    private static class Json {

        private Data data;
        private static class Data {

            @SerializedName("things")
            private List<RedditComment> comments;
        }
    }

    /**
     * @return The list of more comments
     */
    public List<RedditComment> getComments() {
        return this.json.data.comments;
    }
}
