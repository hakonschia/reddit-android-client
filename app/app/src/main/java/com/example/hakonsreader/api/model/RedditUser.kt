package com.example.hakonsreader.api.model

import com.example.hakonsreader.api.enums.Thing
import com.google.gson.annotations.SerializedName

/**
 * Class representing a Reddit user
 */
class RedditUser : RedditListingKt() {

    /**
     * The username of the user
     */
    // The "username" in the JSOn is "name" (which for other listings are the fullname)
    val username
        get() = super.fullname

    // The "name" in the user JSON returned is the username, so manually create the fullname
    override var fullname = ""
        get() = Thing.ACCOUNT.value + "_" + super.id


    /**
     * The amount of comment karma the user has
     */
    @SerializedName("comment_karma")
    var commentKarma = 0

    /**
     * The amount of post/link karma the user has
     */
    @SerializedName("link_karma")
    var postKarma = 0

    /**
     * The amount of awardee karma the user has
     */
    @SerializedName("awardee_karma")
    var awardeeKarma = 0

    /**
     * The amount of awarder karma the user has
     */
    @SerializedName("awarder_karma")
    var awarderKarma = 0

    /**
     * The combined total amount of karma the user has
     */
    @SerializedName("total_karma")
    var totalKarma = 0


    /**
     * The URL pointing to the users profile picture
     */
    @SerializedName("icon_img")
    var profilePicture = ""

    /**
     * True if the user is an admin (employee) at Reddit
     */
    @SerializedName("is_employee")
    var isAdmin = false


    // TODO there is also an inner "subreddit" object which is somewhat identical to the "Subreddit" class
}