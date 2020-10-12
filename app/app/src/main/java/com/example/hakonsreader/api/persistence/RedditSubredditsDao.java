package com.example.hakonsreader.api.persistence;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.hakonsreader.api.model.Subreddit;

import java.util.List;

@Dao
public interface RedditSubredditsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Subreddit subreddit);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Subreddit> subreddits);

    @Query("SELECT * FROM subreddits")
    List<Subreddit> getAll();
}
