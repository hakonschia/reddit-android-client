package com.example.hakonsreader.misc;

import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;

import java.util.Map;


public class SharedPreferencesManager {
    private static final String TAG = "SharedPreferencesManage";

    private static SharedPreferences prefs;
    private static Gson gson;


    public static void create(SharedPreferences prefs) {
        SharedPreferencesManager.prefs = prefs;
        gson = new Gson();
    }

    public static void put(String key, Object value) {
        if (prefs == null) {
            throw new RuntimeException("Warning: preferefences have not been created. Use SharedPreferencesManager.create()");
        }

        String asJson = gson.toJson(value);

        Log.d(TAG, "puting " + asJson);

        Map<String, ?> p = prefs.getAll();

        p.forEach((k, v) -> {
            Log.d(TAG, "put: " + k + ": " + v);
        });
        prefs.edit()
                .putString(key, asJson)
                .apply();

        p.forEach((k, v) -> {
            Log.d(TAG, "put: " + k + ": " + v);
        });
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
            throw new RuntimeException("Warning: preferefences have not been created. Use SharedPreferencesManager.create()");
        }

        return gson.fromJson(prefs.getString(key, ""), type);
    }


    public static void remove(String key) {
        if (prefs == null) {
            throw new RuntimeException("Warning: preferefences have not been created. Use SharedPreferencesManager.create()");
        }

        prefs.edit()
                .remove(key)
                .apply();
    }

}
