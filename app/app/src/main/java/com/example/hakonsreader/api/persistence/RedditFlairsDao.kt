package com.example.hakonsreader.api.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.hakonsreader.api.model.flairs.RedditFlair
import kotlinx.coroutines.flow.Flow

@Dao
interface RedditFlairsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(flairs: List<RedditFlair>)

    @Query("SELECT * FROM flairs WHERE subreddit=:subredditName AND flairType=:type")
    fun getFlairsBySubredditAndType(subredditName: String, type: String) : Flow<List<RedditFlair>>

}