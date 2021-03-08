package com.example.hakonsreader.api.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.hakonsreader.api.model.RedditUserInfo

/**
 * A local database for keeping track of user information. This is in a separate database
 * to simplify updating the database without losing the logged in users.
 *
 * This database does allow for main thread queries, but should only be used when necessary (such
 * as when the application is starting)
 */
@Database(version = 2, exportSchema = false, entities = [RedditUserInfo::class])
@TypeConverters(PostConverter::class, EnumConverters::class)
abstract class RedditUserInfoDatabase : RoomDatabase() {
    abstract fun userInfo() : RedditUserInfoDao

    companion object {
        @Volatile
        private var instance: RedditUserInfoDatabase? = null

        /**
         * Retrieves the instance of the database
         * @param context The application context
         * @return The instance of the database
         */
        @Synchronized
        fun getInstance(context: Context): RedditUserInfoDatabase {
            return instance ?: synchronized(this) {
                val i = Room.databaseBuilder(
                        context.applicationContext,
                        RedditUserInfoDatabase::class.java, "local_reddit_user_info_db"
                ).fallbackToDestructiveMigration().allowMainThreadQueries().build()
                instance = i
                i
            }
        }
    }
}