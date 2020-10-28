package com.example.hakonsreader.api.responses;

import com.example.hakonsreader.api.model.RedditListing;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Response for any request that returns a list of listings
 */
public class ListingResponse  {

    @SerializedName("errors")
    private List<List<String>> errors;

    @SerializedName("data")
    private Data data;
    private static class Data {

        // For "more comments" kind of responses the array is called "things" instead of "children"
        @SerializedName(value = "children", alternate = "things")
        List<? extends RedditListing> listings;
    }

    /**
     * @return The listings retrieved from the response
     */
    public List<? extends RedditListing> getListings() {
        return data.listings;
    }

    /**
     * @return True if the response has errors
     */
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    /**
     * Retrieves the list of errors. The inner list contains various error information. The first
     * object in the inner list contains an enum type that indicates what kind of error it is. The
     * error can be identified with {@link com.example.hakonsreader.api.enums.ResponseErrors}
     *
     * @return The list of errors for the request
     */
    public List<List<String>> getErrors() {
        return errors;
    }
}
