package com.example.hakonsreader.api.persistence

import androidx.room.*
import com.example.hakonsreader.api.model.RedditMessage

@Dao
interface RedditMessagesDao {


    /**
     * Inserts a new message into the database
     *
     * The conflict strategy is to replace the record (ie. update it)
     *
     * @param message The message to insert
     * @see insertAll
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(message: RedditMessage)


    /**
     * Inserts a list of messages into the database
     *
     * The conflict strategy is to replace the records (ie. update them)
     *
     * @param messages The messages to insert
     * @see insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(messages: List<RedditMessage>)


    /**
     * Deletes a message from the database
     *
     * @param message The message to delete
     * @see deleteAll
     */
    @Delete
    fun delete(message: RedditMessage)

    /**
     * Deletes all messages from the database
     *
     * @return The number of records deleted
     * @see delete
     */
    // TODO this causes a NPE when building the project, because I don't know
    // @Query("DELETE FROM messages")
    //fun deleteAll() : Int


    /**
     * @return the amount of messages in the database marked as "new", ie. not seen by the user
     */
    @Query("SELECT COUNT() FROM messages WHERE isNew=1")
    fun getNewMessagesCount() : Int
}