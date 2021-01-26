package com.example.hakonsreader.api.persistence;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.hakonsreader.api.enums.FlairType;
import com.example.hakonsreader.api.model.flairs.RedditFlair;

import java.util.List;

@Dao
public interface RedditFlairsDao {

    /**
     * @param flairs The flairs to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insert(List<RedditFlair> flairs);

    /**
     * Retrieves a LiveData observable for a list of {@link RedditFlair} based on a subreddit and flair type
     *
     * @param subredditName The name of the subreddit to get flairs for
     * @param type The type of flair to get (from {@link FlairType#name()})
     */
    @Query("SELECT * FROM flairs WHERE subreddit=:subredditName AND flairType=:type")
    public LiveData<List<RedditFlair>> getFlairsBySubredditAndType(String subredditName, String type);
}
