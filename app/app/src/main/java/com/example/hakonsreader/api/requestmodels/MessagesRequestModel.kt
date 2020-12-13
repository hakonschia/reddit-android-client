package com.example.hakonsreader.api.requestmodels

import com.example.hakonsreader.api.enums.Thing
import com.example.hakonsreader.api.exceptions.InvalidAccessTokenException
import com.example.hakonsreader.api.model.AccessToken
import com.example.hakonsreader.api.model.RedditMessage
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.api.responses.GenericError
import com.example.hakonsreader.api.service.MessageService
import com.example.hakonsreader.api.utils.Util
import com.example.hakonsreader.api.utils.apiError
import retrofit2.http.Field
import java.lang.Exception
import java.lang.StringBuilder

class MessagesRequestModel(
        private val accessToken: AccessToken,
        private val api: MessageService
) {

    /**
     * Gets all messages in the inbox (unread and read messages)
     *
     * OAuth scope required: `privatemessages`
     *
     */
    suspend fun inbox(after: String = "", count: Int = 0, limit: Int = 25) : ApiResponse<List<RedditMessage>> {
        try {
            Util.verifyLoggedInToken(accessToken)
        } catch (e: InvalidAccessTokenException) {
            return ApiResponse.Error(GenericError(-1), InvalidAccessTokenException("Cannot get messages without a logged in user", e))
        }

        return try {
            val response = api.getMessages(where = "inbox", after, count, limit)
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
     */
    suspend fun markRead(vararg messages: RedditMessage) : ApiResponse<Any?> {
        if (messages.isEmpty()) {
            return ApiResponse.Success(null)
        }

        try {
            Util.verifyLoggedInToken(accessToken)
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

                fullnames.append(Util.createFullName(thing, it.id))
                fullnames.append(",")
            }

            val response = api.markRead(fullnames.toString())

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