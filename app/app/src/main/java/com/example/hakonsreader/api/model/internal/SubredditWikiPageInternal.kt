package com.example.hakonsreader.api.model.internal

import com.example.hakonsreader.api.model.SubredditWikiPage
import com.google.gson.annotations.SerializedName

class SubredditWikiPageInternal {

    // Subreddit wiki pages are sort of like a listing with "kind" and "data", but they don't have
    // any common fields with other listings so it's weird to have it as extending from RedditListing

    @SerializedName("data")
    val data: SubredditWikiPage? = null

}