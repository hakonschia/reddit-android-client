package com.example.hakonsreader.api.utils;

import com.example.hakonsreader.api.enums.ResponseErrors;
import com.example.hakonsreader.api.enums.Thing;
import com.example.hakonsreader.api.exceptions.InvalidAccessTokenException;
import com.example.hakonsreader.api.exceptions.RateLimitException;
import com.example.hakonsreader.api.exceptions.ThreadLockedException;
import com.example.hakonsreader.api.interfaces.OnFailure;
import com.example.hakonsreader.api.model.AccessToken;
import com.example.hakonsreader.api.responses.GenericError;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.util.List;

import retrofit2.Response;


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

    public static <T> void handleHttpErrors(Response<T> response, OnFailure onFailure) {
        GenericError error;

        try {
            error = new Gson().fromJson(response.errorBody().string(), GenericError.class);
        } catch (IOException | JsonSyntaxException e) {
            // The code will always be present as the http status code of the response, but message and reason
            // are set by Reddit as the response body so response.message() will be empty

            // In some cases, reddit might return a non-json body (it returns an html error page)
            // which will make Gson throw an exception because it can't parse the response

            error = new GenericError(response.code());
        }

        onFailure.onFailure(error, Util.newThrowable(response.code()));
    }

    /**
     * Creates the fullname for a thing
     *
     * @param thing The type of thing
     * @param thingId The ID of the thing
     * @return The fullname for the thing
     */
    public static String createFullName(Thing thing, String thingId) {
        return thing.getValue() + "_" + thingId;
    }
}
