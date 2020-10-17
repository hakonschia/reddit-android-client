package com.example.hakonsreader.api.persistence;


import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.hakonsreader.api.model.RedditPost;

import java.util.List;

/**
 * Interface to store posts in a persistent Room database
 */
@Dao
public interface RedditPostsDao {

    /**
     * Inserts a post into the database.
     *
     * <p>The post ID provided by Reddit ({@link RedditPost#getId()} is used as the primary key.
     * If a record already exists with this ID it is replaced</p>
     *
     * @param post The post to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(RedditPost post);

    /**
     * Inserts a list of posts into th e database
     *
     * <p>The post ID provided by Reddit ({@link RedditPost#getId()} is used as the primary key.
     * If a record already exists with this ID it is replaced</p>
     *
     * @param posts The posts to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<RedditPost> posts);


    /**
     * Deletes a post from the database.
     *
     * <p>The post ID provided by Reddit ({@link RedditPost#getId()} is used as the primary key.
     * If a record already exists with this ID it is replaced</p>
     *
     * @param post The post to delete
     */
    @Delete
    void delete(RedditPost post);

    /**
     * Deletes all posts from the database
     *
     * @return The amount of posts delete
     */
    @Query("DELETE FROM posts")
    int deleteAll();

    /**
     * Delete posts from the database that are older than the given age
     *
     * @return The amount of posts deleted
     */
    @Query("DELETE FROM posts WHERE insertedAt + :maxAge - strftime('%s', 'now') < 0")
    int deleteOld(long maxAge);


    /**
     * Retrieve the amount of posts currently in the database
     *
     * @return The total amount of posts in the database
     */
    @Query("SELECT COUNT() FROM POSTS")
    int getCount();


    /**
     * Retrieve all posts from the database
     *
     * <p>Note: If a post has crossposts the crossposts will NOT be returned here. The database
     * stores the list of IDs of the crossposts. To retrieve the crossposts use {@link RedditPost#getCrosspostIds()}
     * and then use {@link RedditPostsDao#getPostsById(List)} to retrieve the posts</p>
     *
     * @return A list of all the posts in the database
     */
    @Query("SELECT * FROM posts")
    List<RedditPost> getPosts();

    /**
     * Retrieve a list of posts from the database, based on a list of IDs
     *
     * <p>Note: If a post has crossposts the crossposts will NOT be returned here. The database
     * stores the list of IDs of the crossposts. To retrieve the crossposts use {@link RedditPost#getCrosspostIds()}
     * and then use this function to retrieve the posts</p>
     *
     * @param ids The IDs of the posts to retrieve
     * @return A list of posts matching the IDs
     */
    @Query("SELECT * FROM posts WHERE id IN (:ids)")
    List<RedditPost> getPostsById(List<String> ids);
}
