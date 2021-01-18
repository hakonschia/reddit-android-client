package com.example.hakonsreader.api.model

import com.google.gson.annotations.SerializedName

/**
 * Class representing a subreddit rule
 */
class SubredditRule {

    /**
     * The subreddit the rule is for
     */
    var subreddit = ""

    /**
     * What kind of rule this is
     */
    @SerializedName("kind")
    var kind = ""

    /**
     * The rule name
     */
    @SerializedName("short_name")
    var name = ""

    /**
     * The violation reason
     */
    @SerializedName("violation_reason")
    var violationReason = ""

    /**
     * The rule description in markdown
     */
    @SerializedName("description")
    var description = ""

    /**
     * The rule description in HTML
     */
    @SerializedName("description_html")
    var descriptionHtml: String? = null

    /**
     * The UTC timestamp the rule was created
     */
    @SerializedName("created_utc")
    var createdAt = 0

    /**
     * The rule priority
     */
    @SerializedName("priority")
    var priority = 0


}