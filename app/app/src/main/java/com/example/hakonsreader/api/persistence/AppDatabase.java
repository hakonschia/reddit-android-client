package com.example.hakonsreader.api.persistence;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.hakonsreader.api.model.RedditPost;

/**
 * The global database for the application, holds all entities
 */
@Database(entities = {RedditPost.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance; // The instance of the database
    public abstract RedditPostsDao posts();


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