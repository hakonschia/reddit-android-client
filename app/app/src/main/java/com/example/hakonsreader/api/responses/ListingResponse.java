package com.example.hakonsreader.api.responses;

import com.example.hakonsreader.api.model.RedditListing;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Response for any request that returns a list of listings
 */
public class ListingResponse extends RedditListing {

    private Data data;
    private static class Data {

        // For "more comments" kind of responses the array is called "things" instead of "children"
        @SerializedName(value = "children", alternate = "things")
        List<? extends RedditListing> listings;
    }

    public List<? extends RedditListing> getListings() {
        return data.listings;
    }

}
