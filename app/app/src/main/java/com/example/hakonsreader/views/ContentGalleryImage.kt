package com.example.hakonsreader.views

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.example.hakonsreader.R
import com.example.hakonsreader.activities.DispatcherActivity
import com.example.hakonsreader.api.interfaces.GalleryImage
import com.example.hakonsreader.api.interfaces.Image
import com.example.hakonsreader.api.model.images.RedditGalleryItem
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.api.model.images.RedditGalleryImage
import com.example.hakonsreader.api.model.thirdparty.imgur.ImgurGif
import com.example.hakonsreader.api.model.thirdparty.imgur.ImgurImage
import com.example.hakonsreader.databinding.ContentGalleryImageBinding
import com.example.hakonsreader.misc.Settings
import com.example.hakonsreader.views.util.goneIf

/**
 * View for displaying in a single gallery item in [ContentGallery]
 */
class ContentGalleryImage @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "ContentGalleryImage"
    }

    private val binding = ContentGalleryImageBinding.inflate(LayoutInflater.from(context), this, true)


    /**
     * The Image to display
     */
    var image: GalleryImage? = null
        set(value) {
            field = value
            updateView()
        }

    /**
     * If [image] is an image, this should be set to the post the image is for
     */
    var post: RedditPost? = null

    fun destroy() {
        val view = binding.content.getChildAt(0)

        if (view is VideoPlayer) {
            view.release()
        }
    }

    fun viewSelected() {
        val view = binding.content.getChildAt(0)

        if (view is VideoPlayer && Settings.autoPlayVideos()) {
            view.play()
        }
    }

    fun viewUnselected() {
        val view = binding.content.getChildAt(0)

        if (view is VideoPlayer) {
            view.pause()
        }
    }


    private fun updateView() {
        binding.content.removeAllViews()

        image?.let {
            val view = when (it) {
                is RedditGalleryItem -> asRedditGalleryImage(it)
                is ImgurImage -> asImgurGalleryImage(it)
                is Image -> asImage(it)

                // It is really an error if this happens, should potentially throw an exception
                else -> return@let
            }

            binding.content.addView(view)
        }
    }

    private fun asRedditGalleryImage(galleryItem: RedditGalleryItem): View {
        val image = getGalleryImage(galleryItem)
        return if (image.mp4Url != null) {
            asVideo(image)
        } else {
            with (binding) {
                caption.text = galleryItem.caption
                caption.goneIf(galleryItem.caption == null)

                // Setting the movement method to InternalLinkMovementMethod doesn't work for some reason
                outboundUrl.setOnClickListener {
                    Intent(context, DispatcherActivity::class.java).apply {
                        putExtra(DispatcherActivity.EXTRAS_URL_KEY, galleryItem.outboundUrl)
                        context.startActivity(this)
                    }
                }

                outboundUrl.text = galleryItem.outboundUrl
                outboundUrl.goneIf(galleryItem.outboundUrl == null)
            }

            asImage(image)
        }
    }

    private fun asImgurGalleryImage(image: ImgurImage): View {
        return if (image is ImgurGif) {
            asImgurVideo(image)
        } else {
            asImage(image)
        }
    }

    /**
     * Returns a [ContentImage] for a given [Image]. The image should be a image (not a video)
     *
     * @param image The image to create an image view for
     * @return A [ContentImage]
     * @see asVideo
     */
    private fun asImage(image: Image): ContentImage {
        // Use ContentImage as that already has listeners, NSFW caching etc already
        return ContentImage(context).apply {
            setWithImageUrl(post, image.url)
        }
    }

    /**
     * Returns a [VideoPlayer] for a given [RedditGalleryImage]. The image should be a video
     *
     * @param image The image to create a video for
     * @return A [VideoPlayer]
     * @see asImage
     */
    private fun asVideo(image: RedditGalleryImage): VideoPlayer {
        val player = LayoutInflater.from(context).inflate(R.layout.video_player_texture_view, null) as VideoPlayer
        return player.apply {
            // The height will resize accordingly as long as the width matches the screen
            // I think, I'm pretty lost on this, haven't tested if width of the video is larger
            // than the screen, but I imagine it would scale down
            videoWidth = Resources.getSystem().displayMetrics.widthPixels

            // This should only be called if the gallery image has an MP4 URL, otherwise it is an error
            url = image.mp4Url!!
        }
    }

    /**
     * Returns a [VideoPlayer] for a given [ImgurGif]
     *
     * @param imgurGif The imgur gif to create a video for
     * @return A [VideoPlayer]
     * @see asImage
     */
    private fun asImgurVideo(imgurGif: ImgurGif): VideoPlayer {
        val player = LayoutInflater.from(context).inflate(R.layout.video_player_texture_view, null) as VideoPlayer
        return player.apply {
            videoWidth = imgurGif.width
            videoHeight = imgurGif.height

            // This should only be called if the gallery image has an MP4 URL, otherwise it is an error
            url = imgurGif.mp4Url
            mp4Video = true
            videoSize = imgurGif.mp4Size
        }
    }

    /**
     * Gets a gallery where the image width isn't higher than the screen of the device
     */
    private fun getGalleryImage(galleryItem: RedditGalleryItem): RedditGalleryImage {
        // For videos only the source will actually provide an MP4 URL
        if (galleryItem.source.mp4Url != null) {
            return galleryItem.source
        }

        val screenWidth = Resources.getSystem().displayMetrics.widthPixels

        var index = -1
        val images = galleryItem.resolutions

        images.forEachIndexed { i, image ->
            if (image.width <= screenWidth) {
                index = i
            }
        }

        return images[index]
    }
}