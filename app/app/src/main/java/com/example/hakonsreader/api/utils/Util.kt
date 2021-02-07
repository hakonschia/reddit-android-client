package com.example.hakonsreader.api.utils

import com.example.hakonsreader.api.enums.ResponseErrors
import com.example.hakonsreader.api.enums.Thing
import com.example.hakonsreader.api.exceptions.ArchivedException
import com.example.hakonsreader.api.exceptions.InvalidAccessTokenException
import com.example.hakonsreader.api.exceptions.RateLimitException
import com.example.hakonsreader.api.exceptions.ThreadLockedException
import com.example.hakonsreader.api.model.AccessToken
import com.example.hakonsreader.api.model.thirdparty.GfycatGif
import com.example.hakonsreader.api.model.thirdparty.ImgurGif
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.api.responses.GenericError
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import retrofit2.Response


/**
 * Convenience method for returning an [ApiResponse.Error]. This takes in a [Response] and
 * converts the error body to a [GenericError] which is sent back in an [ApiResponse.Error], alongside
 * a generic throwable
 *
 * @param resp The response that failed
 */
fun <T> apiError(resp: Response<T>) : ApiResponse.Error {
    return try {
        // Reddit might return an HTML page on errors, which makes the json parsing fail
        val errorBody = Gson().fromJson(resp.errorBody()?.string(), GenericError::class.java)
        ApiResponse.Error(errorBody, Throwable("Error executing request: ${resp.code()}"))
    } catch (e: Exception) {
        ApiResponse.Error(GenericError(resp.code()), Throwable("Error executing request: ${resp.code()}"))
    }
}

/**
 * Handles Reddit errors in the form of:
 * "json": {
 *     "errors": []
 * }
 */
fun apiListingErrors(errors: List<List<String>>)  : ApiResponse.Error {
    // There can be more errors, not sure the best way to handle it other than returning the info for the first
    val errorType: String = errors[0][0]
    val errorMessage: String = errors[0][1]

    // There isn't really a response code for these errors, as the HTTP code is still 200
    return when {
        // TODO should find out if this is a comment or thread and return different exception/message
        ResponseErrors.THREAD_LOCKED.value == errorType -> ApiResponse.Error(GenericError(-1), ThreadLockedException("The thread has been locked"))
        ResponseErrors.RATE_LIMIT.value == errorType -> ApiResponse.Error(GenericError(-1), RateLimitException("The action has been done too many times too fast"))
        ResponseErrors.ARCHIVED.value == errorType -> ApiResponse.Error(GenericError(-1), ArchivedException("The listing has been archived"))
        else -> ApiResponse.Error(GenericError(-1), Exception(String.format("Unknown error: %s; %s", errorType, errorMessage)))
    }
}

/**
 * Ensures that a given access token is valid for a user that is logged in.
 * Access tokens for non-logged in users do not count.
 *
 * @param accessToken The access token to check
 *
 * @throws InvalidAccessTokenException If the access token has no refresh token (ie. not an actually logged in user)
 */
@Throws(InvalidAccessTokenException::class)
fun verifyLoggedInToken(accessToken: AccessToken) {
    if (accessToken.refreshToken == null) {
        throw InvalidAccessTokenException("Valid access token was not found")
    }
}

/**
 * Creates the fullname for a thing
 *
 * @param thing The type of thing
 * @param thingId The ID of the thing
 * @return The fullname for the thing
 */
fun createFullName(thing: Thing, thingId: String): String {
    return thing.value + "_" + thingId
}

fun thirdPartyObjectFromJsonString(jsonString: String?) : Any? {
    jsonString ?: return null

    val asJsonObject = try {
        JsonParser.parseString(jsonString).asJsonObject
    } catch (e: IllegalStateException) {
        // This happens if .asJsonObject fails
        return null
    } catch (e: JsonParseException) {
        return null
    }

    val type = when (asJsonObject?.get("type")?.asString) {
        ImgurGif::class.java.typeName -> {
            ImgurGif::class.java
        }

        GfycatGif::class.java.typeName -> {
            GfycatGif::class.java
        }

        else -> return null
    }

    return Gson().fromJson(jsonString, type)
}