package com.example.hakonsreader.misc;

import android.content.Context;

import com.google.android.exoplayer2.database.ExoDatabaseProvider;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;

/**
 * Class for video caching
 */
public class VideoCache {
    private static SimpleCache cache;

    private VideoCache() { }

    /**
     * Retrieves the singleton instance of the video cache
     *
     * @param context The context for the cache
     * @return The cache object
     */
    public static SimpleCache getCache(Context context) {
        if (cache == null) {
            // 50 MB cache
            LeastRecentlyUsedCacheEvictor evictor = new LeastRecentlyUsedCacheEvictor(50 * 1024 * 1024);
            cache = new SimpleCache(context.getCacheDir(), evictor, new ExoDatabaseProvider(context));
        }

        return cache;
    }
}
