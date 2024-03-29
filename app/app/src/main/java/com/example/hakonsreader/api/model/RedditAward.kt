package com.example.hakonsreader.api.model

import com.example.hakonsreader.api.model.images.RedditImage
import com.google.gson.annotations.SerializedName

/**
 * Class representing a Reddit award (gold, silver etc.)
 */
class RedditAward {

    /**
     * The name of the award
     */
    @SerializedName("name")
    var name = ""

    /**
     * The description of the award
     */
    @SerializedName("description")
    var description = ""

    /**
     * The amount of times this award has been given on a listing
     */
    @SerializedName("count")
    var count = 0

    /**
     * The amount of Reddit coins this award costs
     */
    @SerializedName("coin_price")
    var price = 0

    /**
     * The amount of Reddit coins the receiver of this award gets
     */
    @SerializedName("coin_reward")
    var reward = 0

    /**
     * The amount of Reddit coins the subreddit the post is in is awarded when this is awarded
     */
    @SerializedName("subreddit_coin_reward")
    var subredditReward = 0

    /**
     * A list of resized icons for the award
     */
    @SerializedName("resized_icons")
    var resizedIcons: List<RedditImage>? = null

    /**
     * True if this award is marked as a new award
     */
    @SerializedName("is_new")
    var isNew = false

    /**
     * The amount of days of Reddit premium the receiver of this award gets
     */
    @SerializedName("")
    var daysOfPremium = 0
}