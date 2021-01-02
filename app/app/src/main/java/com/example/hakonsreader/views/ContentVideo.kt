package com.example.hakonsreader.views

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.core.util.Pair
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.api.model.thirdparty.GfycatGif
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.api.model.thirdparty.ImgurGif
import com.example.hakonsreader.databinding.ContentVideoBinding
import com.example.hakonsreader.enums.ShowNsfwPreview
import com.example.hakonsreader.interfaces.OnVideoManuallyPaused

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
            // TODO YouTube videos can be loaded with the YouTube Android Player API (https://developers.google.com/youtube/android/player)
            return post.getVideo() != null || post.getVideoGif() != null || post.getMp4Source() != null
        }
    }

    private val player: VideoPlayer

    constructor(context: Context?) : super(context) {
        player = ContentVideoBinding.inflate(LayoutInflater.from(context), this, true).player
    }
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        player = ContentVideoBinding.inflate(LayoutInflater.from(context), this, true).player
    }
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        player = ContentVideoBinding.inflate(LayoutInflater.from(context), this, true).player
    }
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        player = ContentVideoBinding.inflate(LayoutInflater.from(context), this, true).player
    }

    override fun updateView() {
        setThumbnailUrl()
        setVideo()

        if (redditPost.isNsfw && App.get().dontCacheNSFW()) {
            player.cacheVideo = false
        }

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

        var url: String? = null

        if (video != null) {
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

        val thirdParty = redditPost.thirdPartyObject
        // TODO I guess this can be made into an Interface to not have to duplicate this
        if (thirdParty is GfycatGif) {
            url = thirdParty.mp4Url
            player.mp4Video = true
            player.dashVideo = false
            player.hasAudio = thirdParty.hasAudio
        } else if (thirdParty is ImgurGif) {
            url = thirdParty.mp4Url
            player.mp4Video = true
            player.dashVideo = false
            player.hasAudio = thirdParty.hasAudio
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
     * Sets the callback for when a video post has been manually paused
     *
     * @param onVideoManuallyPaused The callback
     */
    fun setOnVideoManuallyPaused(onVideoManuallyPaused: OnVideoManuallyPaused?) {
        player.onManuallyPaused = Runnable {
            onVideoManuallyPaused?.postPaused(this@ContentVideo)
        }
    }

    /**
     * Releases the video to free up its resources
     */
    fun release() {
        player.release()
    }

    private fun setThumbnailUrl() {
        // Loading the blurred/no image etc. is very copied from ContentImage and should be
        // generified so it's not duplicated, but cba to fix that right now

        // post.getThumbnail() returns an image which is very low quality, the source preview
        // has the same dimensions as the video itself
        val image = redditPost.getSourcePreview()
        val imageUrl = image?.url

        // Don't show thumbnail for NSFW posts
        var obfuscatedUrl: String? = null
        var noImageId = -1

        if (redditPost.isNsfw) {
            when (App.get().showNsfwPreview()) {
                ShowNsfwPreview.NORMAL -> { }
                ShowNsfwPreview.BLURRED -> {
                    obfuscatedUrl = getObfuscatedUrl()
                    // If we don't have a URL to show then show the NSFW drawable instead as a fallback
                    if (obfuscatedUrl == null) {
                        noImageId = R.drawable.ic_image_not_supported_200dp
                    }
                }
                ShowNsfwPreview.NO_IMAGE -> noImageId = R.drawable.ic_image_not_supported_200dp
            }
        } else if (redditPost.isSpoiler) {
            obfuscatedUrl = getObfuscatedUrl()
            // If we don't have a URL to show then show the NSFW drawable instead as a fallback
            if (obfuscatedUrl == null) {
                noImageId = R.drawable.ic_image_not_supported_200dp
            }
        }

        if (noImageId != -1) {
            player.thumbnailDrawable = noImageId
        } else {
            player.thumbnailUrl = obfuscatedUrl ?: imageUrl!!
        }
    }

    /**
     * Retrieves the obfuscated image URL to use
     *
     * @return An URL pointing to an image, or `null` of no obfuscated images were found
     */
    private fun getObfuscatedUrl(): String? {
        val obfuscatedPreviews = redditPost.getObfuscatedPreviewImages()
        return if (obfuscatedPreviews != null && obfuscatedPreviews.isNotEmpty()) {
            // Obfuscated previews that are high res are still fairly showing sometimes, so
            // get the lowest quality one as that will not be very easy to tell what it is
            obfuscatedPreviews[0].url
        } else null
    }
}