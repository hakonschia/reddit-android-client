package com.example.hakonsreader.api.model

import com.google.gson.annotations.SerializedName

class RedditUser : RedditListingKt() {

    /**
     * The amount of comment karma the user has
     */
    @SerializedName("comment_karma")
    var commentKarma: Int = 0

    /**
     * The amount of post/link karma the user has
     */
    @SerializedName("link_karma")
    var postKarma: Int = 0

    /**
     * The amount of awarder karma the user has
     */
    @SerializedName("awarder_karma")
    var awarderKarma: Int = 0

    /**
     * The combined total karma the user has
     */
    @SerializedName("total_karma")
    var totalKarma: Int = 0


    /**
     * The URL linking to the users profile picture
     */
    @SerializedName("icon_img")
    lateinit var profilePictureUrl: String

    /**
     * The URL linking to the users snoovatar image
     */
    @SerializedName("snoovatar_img")
    lateinit var snoovatarImageUrl: String


    /**
     * This value represents if the user is an admin at Reddit (employee)
     */
    @SerializedName("is_employee")
    var isAdmin: Boolean = false

}