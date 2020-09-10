package com.example.hakonsreader.api.model;

import com.example.hakonsreader.jsonadapters.BooleanPrimitiveAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

/**
 * Base data that is common for all listings
 */
public abstract class ListingData {

    // Which subreddit the post is in
    protected String subreddit;

    // The ID of the post
    protected String id;

    // The title of the post
    protected String title;

    // The author of the psot
    protected String author;

    // The score of the post
    protected int score;

    // The full link to the comments
    protected String permalink;

    // Show spoiler tag?
    protected boolean spoiler;

    // Is the post locked?
    protected boolean locked;

    // The UTC unix timestamp the post was created at
    @SerializedName("created_utc")
    protected float createdAt;

    @SerializedName("likes")
    @JsonAdapter(BooleanPrimitiveAdapter.class)
    protected Boolean liked;
}