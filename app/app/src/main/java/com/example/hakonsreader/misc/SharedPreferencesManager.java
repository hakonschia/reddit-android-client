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

    public static void putNow(String key, Object value) {
        if (prefs == null) {
            throw new IllegalStateException(PREFERENCES_NOT_SET_ERROR_MESSAGE);
        }

        String asJson = gson.toJson(value);

        prefs.edit()
                .putString(key, asJson)
                .commit();
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


    /**
     * Removes a key from the SharedPreferences
     *
     * <p>This call is done in the background with {@link SharedPreferences.Editor#apply()}, for immediate
     * removal use {@link SharedPreferencesManager#removeNow(String)}</p>
     *
     * @param key The key to remove
     */
    public static void remove(String key) {
        if (prefs == null) {
            throw new IllegalStateException(PREFERENCES_NOT_SET_ERROR_MESSAGE);
        }

        prefs.edit()
                .remove(key)
                .apply();
    }

    /**
     * Removes a key from the shared preferences immediately
     *
     * @param key The key to remove
     */
    public static void removeNow(String key) {
        if (prefs == null) {
            throw new IllegalStateException(PREFERENCES_NOT_SET_ERROR_MESSAGE);
        }

        prefs.edit()
                .remove(key)
                .commit();
    }

}
