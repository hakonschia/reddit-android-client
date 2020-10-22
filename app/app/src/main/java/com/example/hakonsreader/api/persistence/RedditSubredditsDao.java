package com.example.hakonsreader.api.persistence;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.hakonsreader.api.model.Subreddit;

import java.util.List;

/**
 * Interface to store subreddits in a persistent Room database
 */
@Dao
public interface RedditSubredditsDao {

    /**
     * Inserts a subreddit into the database. If there is a conflict the subreddit is updated
     *
     * @param subreddit The subreddit to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Subreddit subreddit);

    /**
     * Inserts a list of subreddits into the database. If there is a conflict the subreddit is updated
     *
     * @param subreddits The subreddits to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Subreddit> subreddits);

    /**
     * Updates the subreddit
     *
     * @param subreddit The subreddit with new information
     */
    @Update
    void update(Subreddit subreddit);

    /**
     * @return A list of all subreddits stored
     */
    @Query("SELECT * FROM subreddits")
    List<Subreddit> getAll();


    /**
     * Retrieves a list of subreddits based on an array of IDs
     *
     * @param ids The IDs to retrieve
     * @return A list of subreddits based on the IDs
     */
    @Query("SELECT * FROM subreddits WHERE id IN (:ids)")
    List<Subreddit> getSubsById(String[] ids);

    /**
     * Get the subreddit object from its name
     *
     * @param subredditName The name of the subreddit (as equal to {@link Subreddit#getName()}).
     *                      Not this is NOT case sensitive
     * @return The subreddit, or null if not found
     */
    @Query("SELECT * FROM subreddits WHERE name=:subredditName COLLATE NOCASE")
    Subreddit get(String subredditName);
}
