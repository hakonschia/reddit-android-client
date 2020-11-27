package com.example.hakonsreader.api.model

import androidx.room.Entity
import androidx.room.TypeConverters
import com.example.hakonsreader.api.persistence.PostConverter
import com.google.gson.annotations.SerializedName

@Entity(tableName = "posts")
@TypeConverters(PostConverter::class)
class RedditPostKt : RedditListingKt() {

    /**
     * The title of the post
     */
    @SerializedName("title")
    var title = ""

    /**
     * The name of the Subreddit the post is in
     */
    @SerializedName("subreddit")
    var subreddit = ""

    /**
     * The text of the post in Markdown if this is a text post
     *
     * @see selftextHtml
     */
    @SerializedName("selftext")
    var selftext = ""

    /**
     * The text of the post in HTML if this is a text post
     *
     * @see selftext
     */
    @SerializedName("selftext_html")
    var selftextHtml = ""


}