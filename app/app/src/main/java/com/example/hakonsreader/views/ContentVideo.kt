package com.example.hakonsreader.views

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.core.util.Pair
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.api.interfaces.ThirdPartyGif
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.databinding.ContentVideoBinding
import com.example.hakonsreader.misc.getImageVariantsForRedditPost

/**
 * View for displaying videos from a [RedditPost]
 */
class ContentVideo : Content {
    companion object {
        private const val TAG = "PostContentVideo"

        /**
         * Checks if a [RedditPost] is possible to play as a video. Even if [RedditPost.getPostType]
         * indicates that the post is a video, it might not include any supported video formats since
         * old posts might have different content.
         *
         * @param post The post to check
         * @return True if the post can be played as a video, false otherwise
         */
        fun isRedditPostVideoPlayable(post: RedditPost): Boolean {
            return post.getVideo() != null || post.getVideoGif() != null || post.getMp4Source() != null
                    || post.thirdPartyObject is ThirdPartyGif
        }
    }

    private val player = ContentVideoBinding.inflate(LayoutInflater.from(context), this, true).player

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    override fun updateView() {
        setThumbnailUrl()
        setVideo()

        player.cacheVideo = cache

        if (App.get().muteVideoByDefault()) {
            player.toggleVolume(false)
        }

        // Kind of a really bad way to make the video resize :)  When the post is opened
        // the video player won't automatically resize, so if the height of the view has been updated
        // manually (> 0, ie. not wrap_content or match_parent), set those params on the player as well
        // It would probably be better if this could be done in VideoPlayer instead automatically
        viewTreeObserver.addOnGlobalLayoutListener {
            val layoutParams = layoutParams
            if (layoutParams.height > 0) {
                player.layoutParams = layoutParams
            }
        }
    }

    /**
     * Sets the URL on [.player] and updates the size/duration if possible
     */
    private fun setVideo() {
        // Get either the video, or the GIF
        var video = redditPost.getVideo()
        if (video == null) {
            video = redditPost.getVideoGif()
        }

        val thirdParty = redditPost.thirdPartyObject
        var url: String? = null

        if (thirdParty is ThirdPartyGif) {
            url = thirdParty.mp4Url
            player.mp4Video = true
            player.hasAudio = thirdParty.hasAudio
            player.videoSize = thirdParty.mp4Size
            player.videoWidth = thirdParty.width
            player.videoHeight = thirdParty.height
        } else if (video != null) {
            url = video.dashUrl

            // If we have a "RedditVideo" we can set the duration now
            player.videoDuration = video.duration
            player.dashVideo = true
            player.videoWidth = video.width
            player.videoHeight = video.height
        } else {
            val gif = redditPost.getMp4Source()
            if (gif != null) {
                url = gif.url
                player.videoWidth = gif.width
                player.videoHeight = gif.height
            }
        }

        if (url != null) {
            player.url = url
        } else {
            // Show some sort of error
        }
    }

    override fun getTransitionViews(): List<Pair<View, String>>? {
        return super.getTransitionViews().also {
            it.add(Pair(player, player.transitionName))
        }
    }

    /**
     * Called when the video has been selected. If the user has enabled auto play the video will start playing
     *
     * If the users setting allows for autoplay then it is autoplayed, if the video is marked as a
     * spoiler it will never play, if marked as NSFW it will only play if the user has allowed NSFW autoplay
     */
    override fun viewSelected() {
        // TODO if the video has already been played, then we can resume (for all) (should this be a setting? maybe)
        if (redditPost.isSpoiler) {
            return
        }
        if (redditPost.isNsfw) {
            if (App.get().autoPlayNsfwVideos()) {
                player.play()
            }
        } else if (App.get().autoPlayVideos()) {
            player.play()
        }
    }

    /**
     * Pauses the video playback
     */
    override fun viewUnselected() {
        player.pause()
    }

    /**
     * Retrieve a bundle of information that can be useful for saving the state of the post
     *
     * @return A bundle that might include state variables
     */
    override fun getExtras() = player.getExtras()

    /**
     * Sets the extras for the video.
     *
     * @param extras The bundle of data to use. This should be the same bundle as retrieved with
     * [ContentVideo.getExtras]
     */
    override fun setExtras(extras: Bundle) = player.setExtras(extras)

    /**
     * Gets the resized height of the video player (ie. the size the video actually is when fully
     * expanded)
     */
    override fun getWantedHeight() = player.actualVideoHeight

    /**
     * Sets the callback for when a video post has been manually paused
     *
     * @param onVideoManuallyPaused The callback
     */
    fun setOnVideoManuallyPaused(onVideoManuallyPaused: (ContentVideo) -> Unit) {
        player.onManuallyPaused = {
            onVideoManuallyPaused.invoke(this)
        }
    }

    /**
     * Sets the callback for when the video post has entered fullscreen
     *
     * @param onVideoFullscreenListener The callback
     */
    fun setOnVideoFullscreenListener(onVideoFullscreenListener: (ContentVideo) -> Unit) {
        player.fullScreenListener = {
            onVideoFullscreenListener.invoke(this)
        }
    }

    /**
     * Releases the video to free up its resources
     */
    fun release() {
        player.release()
    }

    /**
     * If set to true the controller will be animated (primarily by fading in/out).
     *
     * This should not be used in RecyclerViews, as scrolling can cause the view to jump
     *
     * @param enable True to enable transitions
     */
    fun enableControllerTransitions(enable: Boolean) {
        player.transitionEnabled = enable
    }

    /**
     * Gets a bitmap of the current frame displayed, or null if the video hasn't yet been loaded
     */
    fun getCurrentFrame(): Bitmap {
        return player.getCurrentFrame()
    }

    /**
     * Sets a bitmap to the thumbnail
     */
    fun setThumbnailBitmap(bitmap: Bitmap) {
        player.setThumbnailBitmap(bitmap)
    }

    /**
     * Loads the default thumbnail on the video
     */
    fun loadThumbnail() {
        player.loadThumbnail()
    }

    private fun setThumbnailUrl() {
        val variants = getImageVariantsForRedditPost(redditPost)

        val url = when {
            redditPost.isNsfw -> variants.nsfw
            redditPost.isSpoiler -> variants.spoiler
            else -> variants.normal
        }

        if (url != null) {
            player.thumbnailUrl = url
        } else {
            player.thumbnailDrawable = R.drawable.ic_image_not_supported_200dp
        }
    }
}