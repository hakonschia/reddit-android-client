package com.example.hakonsreader.misc

import java.security.SecureRandom
import java.util.*

object OAuthState {

    /**
     * The current state generated from [generateAndGetOAuthState], or `null` if there is no current
     * state
     */
    var state: String? = null
        private set


    /**
     * Clears the OAuth state.
     *
     * Use this when the state has been verified
     */
    fun clear() {
        state = null
    }

    /**
     * Generates a random string to use for OAuth requests
     *
     * @return A new random string
     */
    private fun generateOAuthState(): String {
        val characters = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        val rnd: Random = SecureRandom()
        val state = StringBuilder()
        for (i in 0..34) {
            state.append(characters[rnd.nextInt(characters.length)])
        }
        return state.toString()
    }

    /**
     * Generates a new OAuth state that is used for validation
     *
     * @return A random string to use in the request for access
     */
    fun generateAndGetOAuthState(): String {
        return generateOAuthState().also {
            state = it
        }
    }

}