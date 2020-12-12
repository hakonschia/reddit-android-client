package com.example.hakonsreader.api.persistence;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.hakonsreader.api.model.RedditMessage;

import java.util.List;

@Dao
public interface RedditMessagesDao {

    /**
     * Inserts a new message into the database
     *
     * The conflict strategy is to replace the record (ie. update it)
     *
     * @param message The message to insert
     * @see #insertAll
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insert(RedditMessage message);

    /**
     * Inserts a list of messages into the database
     *
     * The conflict strategy is to replace the records (ie. update them)
     *
     * @param messages The messages to insert
     * @see #insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insertAll(List<RedditMessage> messages);


    /**
     * Deletes a message from the database
     *
     * @param message The message to delete
     * @see #deleteAll
     */
    @Delete
    public void delete(RedditMessage message);

    /**
     * Deletes all messages from the database
     * 
     * @return The amount of messages deleted
     * @see #delete(RedditMessage) 
     */
    @Query("DELETE FROM messages")
    public int deleteAll();


    /**
     * @return A list of messages marked as "new", ie. not seen be the user
     */
    @Query("SELECT * FROM messages WHERE isNew=1")
    public List<RedditMessage> getNewMessages();
}
