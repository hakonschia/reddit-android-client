package com.example.hakonsreader.api.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.hakonsreader.api.model.SubredditRule
import kotlinx.coroutines.flow.Flow

@Dao
interface RedditSubredditRulesDao {
    /**
     * Inserts a new subreddit rule
     *
     * @param rule The rule to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(rule: SubredditRule)

    /**
     * Inserts a list of subreddit rules
     *
     * @param rules The list of rules to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(rules: List<SubredditRule>)

    /**
     * Retrieves all rules from a given subreddit, sorted by ascending priority
     *
     * @param subreddit The name of the subreddit to retrieve rules for
     */
    @Query("SELECT * FROM subreddit_rules WHERE subreddit=:subreddit COLLATE NOCASE ORDER BY priority ASC")
    fun getAllRules(subreddit: String): Flow<List<SubredditRule>>
}