package com.example.hakonsreader.api.utils;

import com.example.hakonsreader.api.enums.ResponseErrors;
import com.example.hakonsreader.api.exceptions.InvalidAccessTokenException;
import com.example.hakonsreader.api.exceptions.RateLimitException;
import com.example.hakonsreader.api.exceptions.ThreadLockedException;
import com.example.hakonsreader.api.interfaces.OnFailure;
import com.example.hakonsreader.api.model.AccessToken;
import com.example.hakonsreader.api.responses.ListingResponse;

import java.util.List;


/**
 * Utility functions for the API
 */
public class Util {

    private Util() { }


    /**
     * Ensures that a given access token is valid for a user that is logged in.
     * Access tokens for non-logged in users do not count.
     *
     * @param accessToken The access token to check
     *
     * @throws InvalidAccessTokenException If the access token has no refresh token (ie. not an actually logged in user)
     */
    public static void verifyLoggedInToken(AccessToken accessToken) throws InvalidAccessTokenException {
        if (accessToken.getRefreshToken() == null) {
            throw new InvalidAccessTokenException("Valid access token was not found");
        }
    }

    /**
     * Create a new throwable with a generic request error message
     *
     * @param code The code of the request
     * @return A throwable with a generic error message and the code
     */
    public static Throwable newThrowable(int code) {
        return new Throwable("Error executing request: " + code);
    }

    /**
     * Handles listing errors (from {@link ListingResponse#hasErrors()}). Based on the error type
     * an attempt to handle the error is done, otherwise a generic error message is sent.
     *
     * @param errors The errors for the response (as retrieved with {@link ListingResponse#getErrors()})
     * @param onFailure The failure handler for the request
     */
    public static void handleListingErrors(List<List<String>> errors, OnFailure onFailure) {
        // There can be more errors, not sure the best way to handle it other than returning the info for the first
        String errorType = errors.get(0).get(0);
        String errorMessage = errors.get(0).get(1);

        // There isn't really a response code for these errors, as the HTTP code is still 200

        if (ResponseErrors.THREAD_LOCKED.getValue().equals(errorType)) {
            // TODO should find out if this is a comment or thread and return different exception/message
            onFailure.onFailure(-1, new ThreadLockedException("The thread has been locked"));
        } else if (ResponseErrors.RATE_LIMIT.getValue().equals(errorType)) {
            onFailure.onFailure(-1, new RateLimitException("The action has been done too many times too fast"));
        } else {
            onFailure.onFailure(-1, new Exception(String.format("Unknown error posting comment: %s; %s", errorType, errorMessage)));
        }
    }

}