package com.example.hakonsreader.interfaces

import com.example.hakonsreader.views.ContentVideo

/**
 * Interface for when a video has paused playback manually by the user
 */
@FunctionalInterface
interface OnVideoManuallyPaused {
    /**
     * Called when the post has been manually paused
     *
     * @param contentVideo The view holding the video paused
     */
    fun postPaused(contentVideo: ContentVideo)
}