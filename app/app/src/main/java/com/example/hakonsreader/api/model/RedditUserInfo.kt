package com.example.hakonsreader.api.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Wrapper object for storing generic information about a logger in user
 *
 * @param userId The user ID as retrieved from [AccessToken]
 */
@Entity(tableName = "reddit_user_info")
class RedditUserInfo(
    /**
     * The user ID as retrieved from [AccessToken]
     */
    @PrimaryKey
    val userId: String

) {

    /**
     * The user information about the user
     */
    @Embedded
    var userInfo: RedditUser? = null

    /**
     * The IDs of the users subscribed subreddits
     */
    var subscribedSubreddits: List<String>? = null

    /**
     * If true, this account is marked by the user as a NSFW account and appropriate application
     * action should be taken
     */
    var nsfwAccount = false

}