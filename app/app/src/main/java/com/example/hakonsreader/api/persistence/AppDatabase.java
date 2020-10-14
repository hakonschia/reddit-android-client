package com.example.hakonsreader.api.persistence;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.api.model.Subreddit;

/**
 * The global database for the application, holds {@link RedditPost} and {@link Subreddit} entities
 */
@Database(entities = {RedditPost.class, Subreddit.class}, version = 9)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;
    public abstract RedditPostsDao posts();
    public abstract RedditSubredditsDao subreddits();


    /**
     * Retrieves the instance of the database
     * @param context The application context
     * @return The instance of the database
     */
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class, "app_database"
            ).fallbackToDestructiveMigration().build();
        }
        return instance;
    }
}