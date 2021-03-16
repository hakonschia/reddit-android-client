package com.example.hakonsreader.api.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.hakonsreader.api.model.*
import com.example.hakonsreader.api.model.flairs.RedditFlair

/**
 * The global database for the application, holds [RedditPost] and [Subreddit] entities
 */
@Database(version = 26, exportSchema = false,
        entities = [
            RedditPost::class,
            Subreddit::class,
            SubredditRule::class,
            RedditMessage::class,
            RedditFlair::class,
        ]
)
@TypeConverters(PostConverter::class, EnumConverters::class)
abstract class RedditDatabase : RoomDatabase() {

    abstract fun posts(): RedditPostsDao
    abstract fun subreddits(): RedditSubredditsDao
    abstract fun messages(): RedditMessagesDao
    abstract fun rules(): RedditSubredditRulesDao
    abstract fun flairs(): RedditFlairsDao

    /**
     * Clears user state from any records in the database
     *
     *
     * This effectively converts the records into what would be returned for non-logged in users.
     * For example, all posts will convert [RedditPost.getVoteType] to [com.example.hakonsreader.api.enums.VoteType.NO_VOTE]
     */
    fun clearUserState() {
        posts().clearUserState()
        subreddits().clearUserState()
        messages().deleteAll()
    }

    companion object {
        @Volatile
        private var instance: RedditDatabase? = null

        /**
         * Retrieves the instance of the database
         * @param context The application context
         * @return The instance of the database
         */
        @Synchronized
        fun getInstance(context: Context): RedditDatabase {
            return instance ?: synchronized(this) {
                val i = Room.databaseBuilder(
                        context.applicationContext,
                        RedditDatabase::class.java, "local_reddit_db"
                ).fallbackToDestructiveMigration().build()
                instance = i
                i
            }
        }
    }
}