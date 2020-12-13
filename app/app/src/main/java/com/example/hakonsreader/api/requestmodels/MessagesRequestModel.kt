package com.example.hakonsreader.api.requestmodels

import com.example.hakonsreader.api.exceptions.InvalidAccessTokenException
import com.example.hakonsreader.api.model.AccessToken
import com.example.hakonsreader.api.model.RedditMessage
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.api.responses.GenericError
import com.example.hakonsreader.api.service.MessageService
import com.example.hakonsreader.api.utils.Util
import com.example.hakonsreader.api.utils.apiError
import java.lang.Exception

class MessagesRequestModel(
        private val accessToken: AccessToken,
        private val api: MessageService
) {

    /**
     * Gets all messages in the inbox (unread and read messages)
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

}