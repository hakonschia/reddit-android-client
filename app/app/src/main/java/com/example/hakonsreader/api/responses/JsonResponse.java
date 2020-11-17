package com.example.hakonsreader.api.responses;

import com.example.hakonsreader.api.model.RedditListing;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Handles responses in the form of:
 * <pre>
 * "json": {
 *     "errors": [],
 *     "data:" {
 *
 *     }
 * }
 * </pre>
 */
public class JsonResponse {

    @SerializedName("json")
    private ListingResponse response;

    /**
     * @return The list of more response
     */
    public List<? extends RedditListing> getListings() {
        return response.getListings();
    }

    public boolean hasErrors() {
        return response.hasErrors();
    }

    public List<List<String>> errors() {
        return response.getErrors();
    }
}
