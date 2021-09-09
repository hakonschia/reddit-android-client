package com.example.hakonsreader.views

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.get
import androidx.lifecycle.LifecycleOwner
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
import com.example.hakonsreader.misc.createDoubleImageViewState
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
        @Suppress("UNUSED")
        private const val TAG = "ContentGalleryImage"
    }

    private val binding = ContentGalleryImageBinding.inflate(LayoutInflater.from(context), this, true)

    /**
     * The lifecycle owner used to ensure videos in the gallery are automatically paused and released
     */
    var lifecycleOwner: LifecycleOwner? = null

    /**
     * The Image to display
     */
    var image: GalleryImage? = null
        set(value) {
            field = value
            updateView()
        }

    /**
     * The post the image is for. This is used only to check if the post is a spoiler/nsfw post
     */
    var post: RedditPost? = null

    var bitmap: Bitmap? = null

    private var extras: Bundle? = null

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

    /**
     * Gets a bitmap representation of the view, or null
     */
    fun getImageBitmap(): Bitmap? {
        val view = if (binding.content.childCount > 0) binding.content[0] else return null

        return when (view) {
            is DoubleImageView -> view.getImageBitmap()
            is VideoPlayer -> view.getCurrentFrame()
            else -> null
        }
    }

    /**
     * Gets the extras for the view, or null
     */
    fun getExtras(): Bundle? {
        val view = if (binding.content.childCount > 0) binding.content[0] else return null

        return when (view) {
            is DoubleImageView -> view.extras
            is VideoPlayer -> view.getExtras()
            else -> null
        }
    }

    fun setExtras(bundle: Bundle) {
        this.extras = bundle
    }

    private fun updateView() {
        binding.content.removeAllViews()

        image?.let {
            val view = when (it) {
                is RedditGalleryItem -> asRedditGalleryImage(it)
                is ImgurImage -> asImgurGalleryImage(it)
                is Image -> DoubleImageView(context).apply {
                    state = DoubleImageView.DoubleImageState.OneImage(url = it.url)
                }

                // It is really an error if this happens, should potentially throw an exception
                else -> return@let
            }

            binding.content.addView(view)
        }
    }

    private fun asRedditGalleryImage(galleryItem: RedditGalleryItem): View {
        // For videos only the source will actually provide an MP4 URL
        return if (galleryItem.source.mp4Url != null) {
            asVideo(galleryItem.source)
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

            DoubleImageView(context).apply {
                val redditPost = post
                    ?: throw IllegalStateException("Cannot create an image state for a Reddit gallery without ContentGalleryImage#post set")

                // Currently it seems only one obfuscated resolution is given here, otherwise this would be
                // the lowest res image
                val obfuscated = galleryItem.obfuscated?.firstOrNull()
                val images = getGalleryImages(galleryItem)

                bitmap = this@ContentGalleryImage.bitmap
                this@ContentGalleryImage.extras?.let {
                    extras = it
                }

                state = createDoubleImageViewState(
                    redditPost,
                    normalUrl = images.first.url,
                    lowResUrl = images.second.url,
                    obfuscatedUrl = obfuscated?.url
                )
            }
        }
    }

    private fun asImgurGalleryImage(image: ImgurImage): View {
        return if (image is ImgurGif) {
            asImgurVideo(image)
        } else {
            DoubleImageView(context).apply {
                state = DoubleImageView.DoubleImageState.OneImage(url = image.url)
            }
        }
    }

    /**
     * Returns a [VideoPlayer] for a given [RedditGalleryImage]. The image should be a video
     *
     * @param image The image to create a video for
     * @return A [VideoPlayer]
     */
    private fun asVideo(image: RedditGalleryImage): VideoPlayer {
        val player = LayoutInflater.from(context).inflate(R.layout.video_player_texture_view, null) as VideoPlayer
        return player.apply {
            lifecycleOwner?.lifecycle?.addObserver(this)

            // The height will resize accordingly as long as the width matches the screen
            // I think, I'm pretty lost on this, haven't tested if width of the video is larger
            // than the screen, but I imagine it would scale down
            videoWidth = Resources.getSystem().displayMetrics.widthPixels

            bitmap?.let { setThumbnailBitmap(it) }
            extras?.let { setExtras(it) }

            // This should only be called if the gallery image has an MP4 URL, otherwise it is an error
            url = image.mp4Url!!
        }
    }

    /**
     * Returns a [VideoPlayer] for a given [ImgurGif]
     *
     * @param imgurGif The imgur gif to create a video for
     * @return A [VideoPlayer]
     */
    private fun asImgurVideo(imgurGif: ImgurGif): VideoPlayer {
        val player = LayoutInflater.from(context).inflate(R.layout.video_player_texture_view, null) as VideoPlayer
        return player.apply {
            lifecycleOwner?.lifecycle?.addObserver(this)

            videoWidth = imgurGif.width
            videoHeight = imgurGif.height

            bitmap?.let { setThumbnailBitmap(it) }

            // This should only be called if the gallery image has an MP4 URL, otherwise it is an error
            url = imgurGif.mp4Url
            mp4Video = true
            videoSize = imgurGif.mp4Size
        }
    }

    /**
     * Gets a pair of gallery images where [Pair.first] image width isn't higher than the screen
     * of the device and [Pair.second] is a low resolution image
     */
    private fun getGalleryImages(galleryItem: RedditGalleryItem): Pair<RedditGalleryImage, RedditGalleryImage> {
        val screenWidth = Resources.getSystem().displayMetrics.widthPixels

        var index = -1
        val images = galleryItem.resolutions

        images.forEachIndexed { i, image ->
            if (image.width <= screenWidth) {
                index = i
            }
        }

        return images[index] to images.first()
    }
}