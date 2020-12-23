package com.example.hakonsreader.api.persistence;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.hakonsreader.api.model.RedditMessage;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.api.model.Subreddit;

/**
 * The global database for the application, holds {@link RedditPost} and {@link Subreddit} entities
 */
@Database(entities = {RedditPost.class, Subreddit.class, RedditMessage.class}, version = 8)
public abstract class RedditDatabase extends RoomDatabase {

    private static RedditDatabase instance;
    public abstract RedditPostsDao posts();
    public abstract RedditSubredditsDao subreddits();
    public abstract RedditMessagesDao messages();


    /**
     * Retrieves the instance of the database
     * @param context The application context
     * @return The instance of the database
     */
    public static synchronized RedditDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    RedditDatabase.class, "local_reddit_db"
            ).fallbackToDestructiveMigration().build();
        }
        return instance;
    }

    /**
     * Clears user state from any records in the database
     *
     * <p>This effectively converts the records into what would be returned for non-logged in users.
     * For example, all posts will convert {@link RedditPost#getVoteType()} to {@link com.example.hakonsreader.api.enums.VoteType#NO_VOTE}</p>
     */
    public void clearUserState() {
        posts().clearUserState();
        subreddits().clearUserState();
        messages().deleteAll();
    }
}