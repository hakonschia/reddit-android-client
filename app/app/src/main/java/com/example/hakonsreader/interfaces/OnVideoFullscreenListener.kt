package com.example.hakonsreader.interfaces

import com.example.hakonsreader.views.ContentVideo

/**
 * Interface for when a video has entered fullscreen
 */
fun interface OnVideoFullscreenListener {

    /**
     * Called when the post has entered fullscreen paused
     *
     * @param contentVideo The view holding the video that entered fullscreen
     */
    fun onFullscreen(contentVideo: ContentVideo)

}