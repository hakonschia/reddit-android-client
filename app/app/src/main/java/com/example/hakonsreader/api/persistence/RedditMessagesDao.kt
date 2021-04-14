package com.example.hakonsreader.api.persistence

import androidx.lifecycle.LiveData
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
     * @return The amount of messages deleted
     * @see delete
     */
    @Query("DELETE FROM messages")
    fun deleteAll(): Int

    /**
     * @return A list of all messages stored in the database
     * @see getUnreadMessages
     */
    @Query("SELECT * FROM messages ORDER BY createdAt DESC")
    fun getAllMessages() : LiveData<List<RedditMessage>>

    /**
     * @return A list of messages marked as "new", ie. not seen be the user
     * @see getAllMessages
     */
    @Query("SELECT * FROM messages WHERE isNew=1 ORDER BY createdAt DESC")
    fun getUnreadMessages(): LiveData<List<RedditMessage>>

    /**
     * Gets the direct list of message marked as new. For an observable version of this using [LiveData]
     * use [.getUnreadMessages]
     *
     * @return A list of messages marked as "new", ie. not seen be the user
     * @see .getUnreadMessages
     */
    @Query("SELECT * FROM messages WHERE isNew=1 ORDER BY createdAt DESC")
    fun getUnreadMessagesNoObservable(): List<RedditMessage>

    // TODO this should take a List<RedditMessage> ?
    /**
     * Marks all messages in the database as read
     */
    @Query("UPDATE messages SET isNew=0")
    fun markRead()

    /**
     * Retrieve a list of message based on a list of IDs
     */
    @Query("SELECT * FROM messages WHERE id in (:ids)")
    fun getMessagesById(ids: List<String>): List<RedditMessage>
}