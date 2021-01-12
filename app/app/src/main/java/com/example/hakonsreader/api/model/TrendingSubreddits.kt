package com.example.hakonsreader.api.model

import com.google.gson.annotations.SerializedName

class TrendingSubreddits {

    /**
     * The timestamp of when the trending subreddits were retrieved
     */
    val retrieved = System.currentTimeMillis() / 1000

    /**
     * The list of subreddit names that are trending today
     */
    @SerializedName("subreddit_names")
    var subreddits: List<String>? = null

    /**
     * The amount of comments on the post in r/trendingsubreddits for todays trending subreddits
     *
     * @see commentUrl
     */
    @SerializedName("comment_count")
    var commentCount = 0

    /**
     * The link to the comment page for the post in r/trendingsubreddits
     */
    @SerializedName("comment_url")
    var commentUrl = ""
}