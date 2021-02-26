package com.example.hakonsreader.api.model

import com.google.gson.annotations.SerializedName

class SubredditWikiPage {

    /**
     * The subreddit the wiki page is for
     */
    var subreddit: String = ""

    /**
     * The ID of the revision of the wiki page
     */
    @SerializedName("revision_id")
    var revisionId: String = ""

    /**
     * The content of the wiki page in Markdown
     */
    @SerializedName("content_md")
    var content: String = ""

    /**
     * The content of the wiki page in HTML
     */
    @SerializedName("content_html")
    var contentHtml: String? = null
}