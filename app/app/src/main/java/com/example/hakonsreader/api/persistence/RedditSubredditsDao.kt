package com.example.hakonsreader.api.persistence

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.hakonsreader.api.model.Subreddit
import kotlinx.coroutines.flow.Flow

/**
 * Interface to store subreddits in a persistent Room database
 */
@Dao
interface RedditSubredditsDao {
    /**
     * Inserts a subreddit into the database. If there is a conflict the subreddit is updated
     *
     * @param subreddit The subreddit to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(subreddit: Subreddit)

    /**
     * Inserts a list of subreddits into the database. If there is a conflict the subreddit is updated
     *
     * @param subreddits The subreddits to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(subreddits: List<Subreddit>)

    /**
     * Updates the subreddit
     *
     * @param subreddit The subreddit with new information
     */
    @Update
    fun update(subreddit: Subreddit)

    /**
     * @return A list of all subreddits stored
     */
    @Query("SELECT * FROM subreddits")
    fun getAll(): List<Subreddit>

    /**
     * Retrieves a list of subreddits based on an array of IDs
     *
     * @param ids The IDs to retrieve
     * @return A list of subreddits based on the IDs
     */
    @Query("SELECT * FROM subreddits WHERE id IN (:ids)")
    fun getSubsById(ids: List<String>): LiveData<List<Subreddit>>

    /**
     * Gets an observable for the subreddits where [Subreddit.isSubscribed] is true
     */
    @Query("SELECT * FROM subreddits WHERE isSubscribed=1")
    fun getSubscribedSubreddits(): LiveData<List<Subreddit>>

    /**
     * Gets a list of the subreddits where [Subreddit.isSubscribed] is true
     */
    @Query("SELECT * FROM subreddits WHERE isSubscribed=1")
    fun getSubscribedSubredditsNoObservable(): List<Subreddit>

    /**
     * Get the subreddit object from its name
     *
     * @param subredditName The name of the subreddit (as equal to [Subreddit.getName]).
     * Not this is NOT case sensitive
     * @return A LiveData with the subreddit, or null if not found
     */
    @Query("SELECT * FROM subreddits WHERE name=:subredditName COLLATE NOCASE")
    fun get(subredditName: String): Subreddit?

    /**
     * Get the subreddit object from its name
     *
     * @param subredditName The name of the subreddit (as equal to [Subreddit.getName]).
     * Not this is NOT case sensitive
     * @return A LiveData with the subreddit, or null if not found
     */
    @Query("SELECT * FROM subreddits WHERE name=:subredditName COLLATE NOCASE")
    fun getLive(subredditName: String): LiveData<Subreddit?>

    /**
     * Clears user specific information from all posts. Sets to the values that the Reddit API
     * would return for non-logged in users
     */
    @Query("UPDATE subreddits SET isSubscribed=0, isFavorited=0")
    fun clearUserState()
}