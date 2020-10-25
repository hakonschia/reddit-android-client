package com.example.hakonsreader.misc;

import android.content.SharedPreferences;

import com.google.gson.Gson;


public class SharedPreferencesManager {
    private static final String TAG = "SharedPreferencesManage";
    private static final String PREFERENCES_NOT_SET_ERROR_MESSAGE = "Warning: preferences have not been created. Use SharedPreferencesManager.create()";


    private static SharedPreferences prefs;
    private static Gson gson;


    public static void create(SharedPreferences prefs) {
        SharedPreferencesManager.prefs = prefs;
        gson = new Gson();
    }

    public static void put(String key, Object value) {
        if (prefs == null) {
            throw new IllegalStateException(PREFERENCES_NOT_SET_ERROR_MESSAGE);
        }

        String asJson = gson.toJson(value);

        prefs.edit()
                .putString(key, asJson)
                .apply();
    }

    /**
     * Retrieves the requested key from the set SharedPreferences
     *
     * @param key The key to retrieve
     * @param type The type of value the key refers to
     * @param <T> The type
     * @return The value associated with the key
     */
    public static <T> T get(String key, Class<T> type) {
        if (prefs == null) {
            throw new IllegalStateException(PREFERENCES_NOT_SET_ERROR_MESSAGE);
        }

        return gson.fromJson(prefs.getString(key, ""), type);
    }


    public static void remove(String key) {
        if (prefs == null) {
            throw new IllegalStateException(PREFERENCES_NOT_SET_ERROR_MESSAGE);
        }

        prefs.edit()
                .remove(key)
                .apply();
    }

}
