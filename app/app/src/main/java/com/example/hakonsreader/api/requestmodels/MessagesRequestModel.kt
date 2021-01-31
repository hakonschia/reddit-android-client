package com.example.hakonsreader.api.requestmodels

import com.example.hakonsreader.api.enums.Thing
import com.example.hakonsreader.api.exceptions.InvalidAccessTokenException
import com.example.hakonsreader.api.model.AccessToken
import com.example.hakonsreader.api.model.RedditMessage
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.api.responses.GenericError
import com.example.hakonsreader.api.service.MessageService
import com.example.hakonsreader.api.utils.apiError
import com.example.hakonsreader.api.utils.createFullName
import com.example.hakonsreader.api.utils.verifyLoggedInToken
import java.lang.Exception
import java.lang.StringBuilder

/**
 * Request model for communicating with Reddit messages
 */
class MessagesRequestModel(
        private val accessToken: AccessToken,
        private val api: MessageService
) {

    /**
     * Gets all messages in the inbox (unread and read messages)
     *
     * OAuth scope required: `privatemessages`
     *
     * @see unread
     */
    suspend fun inbox(after: String = "", count: Int = 0, limit: Int = 25) : ApiResponse<List<RedditMessage>> {
        return getInboxMessagesInternal(where = "inbox", after, count, limit)
    }

    /**
     * Gets the unread messages in the inbox
     *
     * OAuth scope required: `privatemessages`
     *
     * @see inbox
     */
    suspend fun unread(after: String = "", count: Int = 0, limit: Int = 25) : ApiResponse<List<RedditMessage>> {
        return getInboxMessagesInternal(where = "unread", after, count, limit)
    }

    /**
     * Gets the sent messages in the inbox
     *
     * OAuth scope required: `privatemessages`
     *
     * @see inbox
     */
    suspend fun sent(after: String = "", count: Int = 0, limit: Int = 25) : ApiResponse<List<RedditMessage>> {
        return getInboxMessagesInternal(where = "sent", after, count, limit)
    }


    /**
     * Internal helper function for inbox messages
     *
     * @param where One of: *inbox*, *unread*, *sent*
     */
    private suspend fun getInboxMessagesInternal(where: String, after: String = "", count: Int = 0, limit: Int = 25) : ApiResponse<List<RedditMessage>> {
        try {
            verifyLoggedInToken(accessToken)
        } catch (e: InvalidAccessTokenException) {
            return ApiResponse.Error(GenericError(-1), InvalidAccessTokenException("Cannot get messages without a logged in user", e))
        }

        return try {
            val response = api.getMessages(where, after, count, limit)
            val messages = response.body()?.getListings()

            if (messages != null) {
                ApiResponse.Success(messages)
            } else {
                apiError(response)
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }
    }


    /**
     * Mark inbox messages as read
     *
     * OAuth scope required: `privatemessages`
     *
     * @param messages The messages to mark as read
     * @return This will not return any success data
     *
     * @see markUnread
     */
    suspend fun markRead(vararg messages: RedditMessage) : ApiResponse<Any?> {
        return markInternal(markRead = true, *messages)
    }

    /**
     * Mark inbox messages as unread
     *
     * OAuth scope required: `privatemessages`
     *
     * @param messages The messages to mark as read
     * @return This will not return any success data
     *
     * @see markRead
     */
    suspend fun markUnread(vararg messages: RedditMessage) : ApiResponse<Any?> {
        return markInternal(markRead = false, *messages)
    }

    /**
     * Internal function to mark inbox messages as either read or unread
     *
     * OAuth scope required: `privatemessages`
     *
     * @param messages The messages to mark as read
     */
    private suspend fun markInternal(markRead: Boolean, vararg messages: RedditMessage) : ApiResponse<Any?> {
        if (messages.isEmpty()) {
            return ApiResponse.Success(null)
        }

        try {
            verifyLoggedInToken(accessToken)
        } catch (e: InvalidAccessTokenException) {
            return ApiResponse.Error(GenericError(-1), InvalidAccessTokenException("Cannot mark messages as read without a logged in user", e))
        }

        return try {
            val fullnames = StringBuilder()

            // Create the fullnames string
            messages.forEach {
                // Comment messages are originally a Thing.COMMENT, but I modify them to be Thing.MESSAGE
                val thing = if (it.wasComment) {
                    Thing.COMMENT
                } else {
                    Thing.MESSAGE
                }

                fullnames.append(createFullName(thing, it.id))
                fullnames.append(",")
            }

            val response = if (markRead) {
                api.markRead(fullnames.toString())
            } else {
                api.markUnread(fullnames.toString())
            }

            if (response.isSuccessful) {
                ApiResponse.Success(null)
            } else {
                apiError(response)
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }
    }

    /**
     * Sends a new private message
     *
     * OAuth scope required: `privatemessages`
     *
     * @param recipient The recipient of the message
     * @param subject The subject of the message
     * @param message The message to send
     */
    suspend fun sendMessage(recipient: String, subject: String, message: String) : ApiResponse<Any?> {
        try {
            verifyLoggedInToken(accessToken)
        } catch (e: InvalidAccessTokenException) {
            return ApiResponse.Error(GenericError(-1), InvalidAccessTokenException("Cannot send messages without a logged in user", e))
        }

        return try {
            val response = api.sendMessage(recipient, subject, message)

            if (response.isSuccessful) {
                ApiResponse.Success(null)
            } else {
                apiError(response)
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }
    }
}