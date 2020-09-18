package com.example.hakonsreader.misc;

import java.security.SecureRandom;
import java.util.Random;

public class OAuthStateGenerator {
    private static final String CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    /**
     * Generates a random string to use for OAuth requests
     *
     * @return A new random string
     */
    public static String generate() {
        Random rnd = new SecureRandom();
        StringBuilder state = new StringBuilder();

        for (int i = 0; i < 35; i++) {
            state.append(CHARACTERS.charAt(rnd.nextInt(CHARACTERS.length())));
        }

        return state.toString();
    }

}
