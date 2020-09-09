package com.example.hakonsreader.api.model;

import com.example.hakonsreader.api.RedditApi;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Class representing a response of Reddit listings
 *
 * <p>Contains some of the fields needed for a Reddit post. To see all fields available see
 * data returned from https://reddit.com/.json</p>
 */
public class RedditListingResponse {
    private static final String TAG = "RedditListingResponse";

    private Data data;

    // The JSON structure has a "data" object with the posts in an array called "children"
    private static class Data {
        private List<RedditListing> children;
    }

    /**
     * @return The posts retrieved from an API request
     */
    public List<RedditPost> getPosts() {
        List<RedditListing> listings = this.createList(RedditApi.Thing.Post);
        List<RedditPost> posts = new ArrayList<>();

        // Copy the listings as posts
        listings.forEach(listing -> posts.add(RedditPost.createFromListing(listing)));

        return posts;
    }

    /**
     * @return The posts retrieved from an API request
     */
    public List<RedditComment> getComments() {
        List<RedditListing> listings = this.createList(RedditApi.Thing.Comment);
        List<RedditComment> comments = new ArrayList<>();

        // Copy the listings as comments
        listings.forEach(listing -> comments.add(RedditComment.createFromListing(listing)));

        return comments;
    }


    /**
     * Creates a sublist of a specific "thing" of the children received from the API request
     *
     * @param thing The type of thing to create a list of
     * @return The sublist of things
     */
    private List<RedditListing> createList(RedditApi.Thing thing) {
        return this.data.children
                .stream()
                .filter(c -> c.getKind().equals(thing.getValue()))
                .collect(Collectors.toList());
    }
}
