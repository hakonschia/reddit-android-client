package com.example.hakonsreader.api.persistence;


import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.hakonsreader.api.model.RedditPost;

import java.util.List;

@Dao
public interface RedditPostsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(RedditPost post);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<RedditPost> posts);

    @Delete
    void delete(RedditPost post);

    @Query("DELETE FROM posts")
    void deleteAll();


    @Query("SELECT COUNT() FROM POSTS")
    int getCount();


    @Query("SELECT * FROM posts")
    LiveData<List<RedditPost>> getPosts();

    @Query("SELECT * FROM posts WHERE id IN (:ids)")
    List<RedditPost> getPostsById(List<String> ids);
}
