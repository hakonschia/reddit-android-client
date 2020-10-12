package com.example.hakonsreader.api.persistence;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;

import com.example.hakonsreader.api.model.Subreddit;

@Dao
public interface RedditSubredditsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Subreddit subreddit);


}
