package com.example.hakonsreader.api.persistence;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.hakonsreader.api.model.SubredditRule;

import java.util.List;

@Dao
public interface RedditSubredditRulesDao {
    /**
     * Inserts a new subreddit rule
     *
     * @param rule The rule to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insert(SubredditRule rule);

    /**
     * Inserts a list of subreddit rules
     *
     * @param rules The list of rules to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insertAll(List<SubredditRule> rules);

    /**
     * Retrieves all rules from a given subreddit, sorted by ascending priority
     *
     * @param subreddit The name of the subreddit to retrieve rules for
     */
    @Query("SELECT * FROM subreddit_rules WHERE subreddit=:subreddit COLLATE NOCASE ORDER BY priority ASC")
    public LiveData<List<SubredditRule>> getAllRules(String subreddit);
}
