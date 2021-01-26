package com.example.hakonsreader.api.persistence

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.hakonsreader.api.model.RedditPost

@Dao
interface RedditPostsDao {

    /**
     * Inserts a post into the database.
     *
     *
     * The post ID provided by Reddit ([RedditPost.getId] is used as the primary key.
     * If a record already exists with this ID it is replaced
     *
     * @param post The post to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(post: RedditPost)

    /**
     * Inserts a list of posts into th e database
     *
     *
     * The post ID provided by Reddit ([RedditPost.getId] is used as the primary key.
     * If a record already exists with this ID it is replaced
     *
     * @param posts The posts to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(posts: List<RedditPost>)


    /**
     * Deletes a post from the database.
     *
     * @param post The post to delete
     */
    @Delete
    fun delete(post: RedditPost)

    /**
     * Updates a post in the database
     *
     * @param post The post to update
     */
    @Update
    fun update(post: RedditPost)

    /**
     * Deletes all posts from the database
     *
     * @return The amount of posts delete
     */
    @Query("DELETE FROM posts")
    fun deleteAll(): Int

    /**
     * Delete posts from the database that are older than the given age
     *
     * @return The amount of posts deleted
     */
    @Query("DELETE FROM posts WHERE insertedAt + :maxAge - strftime('%s', 'now') < 0")
    fun deleteOld(maxAge: Long): Int


    /**
     * Retrieve the amount of posts currently in the database
     *
     * @return The total amount of posts in the database
     */
    @Query("SELECT COUNT() FROM POSTS")
    fun getCount(): Int


    /**
     * Retrieve all posts from the database
     *
     *
     * Note: If a post has crossposts the crossposts will NOT be returned here. The database
     * stores the list of IDs of the crossposts. To retrieve the crossposts use [RedditPost.getCrosspostIds]
     * and then use [RedditPostsDao.getPostsById] to retrieve the posts
     *
     * @return A list of all the posts in the database
     */
    @Query("SELECT * FROM posts")
    fun getPosts(): List<RedditPost>

    /**
     * Retrieve a list of posts from the database, based on a list of IDs
     *
     *
     * Note: If a post has crossposts the crossposts will NOT be returned here. The database
     * stores the list of IDs of the crossposts. To retrieve the crossposts use [RedditPost.getCrosspostIds]
     * and then use this function to retrieve the posts
     *
     * @param ids The IDs of the posts to retrieve
     * @return A list of posts matching the IDs
     */
    @Query("SELECT * FROM posts WHERE id IN (:ids)")
    fun getPostsById(ids: List<String>): List<RedditPost>

    /**
     * Retrieves a post by ID
     *
     * @param id The ID of the post to retrieve
     * @return A LiveData that can be observed for the post
     */
    @Query("SELECT * FROM posts WHERE id=:id")
    fun getPostById(id: String): LiveData<RedditPost>


    /**
     * Clears user specific information from all posts. Sets to the values that the Reddit API
     * would return for non-logged in users
     */
    @Query("UPDATE posts SET liked=null")
    fun clearUserState()
}