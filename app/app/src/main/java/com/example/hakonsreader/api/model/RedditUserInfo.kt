package com.example.hakonsreader.api.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Wrapper object for storing generic information about a logger in user
 *
 * @param accessToken The access token this is for. This MUST be set, if [AccessToken.getUserId]
 * returns [AccessToken.NO_USER_ID] then this will throw an exception
 */
@Entity(tableName = "reddit_user_info")
class RedditUserInfo(
        @Embedded
        var accessToken: AccessToken
) {
    init {
        if (accessToken.userId == AccessToken.NO_USER_ID) {
            throw IllegalStateException("Cannot create wrapper object without a valid user ID")
        }
    }

    /**
     * The user ID as retrieved from [accessToken]. This should not be set manually
     */
    @PrimaryKey
    var userId: String = accessToken.userId

    /**
     * The user information about the user
     */
    @Embedded
    var userInfo: RedditUser? = null

    /**
     * The IDs of the users subscribed subreddits
     */
    var subscribedSubreddits: List<String>? = null

}