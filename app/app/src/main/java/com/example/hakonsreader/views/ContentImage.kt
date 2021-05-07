package com.example.hakonsreader.views

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.util.Pair
import com.example.hakonsreader.R
import com.example.hakonsreader.activities.ImageActivity
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.databinding.ContentImageBinding
import com.example.hakonsreader.misc.Settings
import com.example.hakonsreader.misc.getImageVariantsForRedditPost
import com.example.hakonsreader.views.util.cache
import com.example.hakonsreader.views.util.openImageInFullscreen
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.lang.Exception

/**
 * Content view for Reddit images posts.
 */
class ContentImage @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : Content(context, attrs, defStyleAttr) {

    // TODO if an image is low res in a list of post, and the post is opened and the HD image
    //  is loaded there, then we should also set that here in the list. Now the old low res image is shown

    companion object {
        private const val TAG = "ContentImage"

        /**
         * The key for the extras that gives the HD image URL, if a low res image is shown
         *
         * The value with this key is a [String]
         */
        const val EXTRAS_HD_IMAGE_URL = "extras_hdImageUrl"

        /**
         * The key for the extras that gives the URL that should be opened in fullscreen. This is used
         * to give a different URL to load when a bitmap is given
         *
         * The value with this key is a [String]
         */
        const val EXTRAS_URL_TO_OPEN = "extras_urlToOpen"
    }

    private val binding: ContentImageBinding = ContentImageBinding.inflate(LayoutInflater.from(context), this, true)

    /**
     * The overridden image URL set with [setWithImageUrl]
     */
    private var imageUrl: String? = null

    /**
     * Sets the post with a different image URL than the one retrieved with [RedditPost.url].
     * This can be used to create a PhotoView with a custom double tap listener
     * that opens the image in fullscreen when single taped, and also respects the users NSFW caching choice
     *
     * @param imageUrl The image URL to set
     */
    fun setWithImageUrl(post: RedditPost?, imageUrl: String?) {
        this.imageUrl = imageUrl
        super.setRedditPost(post)
    }

    /**
     * Gets a bitmap representation of the image being displayed
     */
    override fun getBitmap(): Bitmap? {
        return binding.image.drawable?.toBitmap()
    }

    /**
     * Updates the view with the url from [ContentImage.post]
     */
    override fun updateView() {
        if (bitmap != null) {
            withBitmap(bitmap!!)
        } else {
            withUrls()
        }
    }

    override fun getTransitionViews(): MutableList<Pair<View, String>> {
        return super.getTransitionViews().also {
            it.add(Pair(binding.image, binding.image.transitionName))

            // If we add this when it is not visible it will become visible again, which
            // makes the icon flash for a split second, and will appear again when you exit
            if (binding.hdImage.visibility == View.VISIBLE) {
                it.add(Pair(binding.hdImage, binding.hdImage.transitionName))
            }
        }
    }

    override fun getWantedHeight(): Int {
        val source = redditPost.getSourcePreview()
        val personWhoBroughtMeIntoTheWorld = parent

        if (source == null || personWhoBroughtMeIntoTheWorld !is View) {
            return super.getWantedHeight()
        }

        // This is how Picasso will scale it, which scales it to the match the width of the parent
        // Ie. layout_width=match_parent, and scale with the same aspect ratio to fit that
        val width = source.width
        val height = source.height
        val widthRatio: Float = width.toFloat() / personWhoBroughtMeIntoTheWorld.width
        
        return (height / widthRatio).toInt()
    }

    /**
     * Loads the image with a bitmap
     */
    private fun withBitmap(b: Bitmap) {
        binding.image.setImageBitmap(b)

        val hdUrl = extras.getString(EXTRAS_HD_IMAGE_URL, null)

        // If no HD URL is given, then it is already loaded and showing
        binding.showingHdImage = hdUrl == null

        hdUrl?.let {
            setHdImageClickListener(it)
        }

        // TODO add delay (like with posts) so it doesn't open multiple images when clicked fast
        setOnClickListener {
            val urlToOpen = extras.getString(EXTRAS_URL_TO_OPEN)

            Intent(context, ImageActivity::class.java).run {
                putExtra(ImageActivity.EXTRAS_CACHE_IMAGE, cache)

                if (urlToOpen != null) {
                    putExtra(ImageActivity.EXTRAS_IMAGE_URL, urlToOpen)
                } else {
                    // "b" might be out-of-date at this point, if it pointed to a low res image and an HD was loaded later
                    ImageActivity.BITMAP = getBitmap() ?: b
                }

                if (context is AppCompatActivity) {
                    val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                            context as AppCompatActivity,
                            binding.image,
                            context.getString(R.string.transition_image_fullscreen)
                    )
                    context.startActivity(this, options.toBundle())
                } else {
                    context.startActivity(this)
                }
            }
        }
    }

    /**
     * Loads the image with URLs, either with [imageUrl] or with the image URLs found in [redditPost]
     */
    private fun withUrls() {
        val (normal, normalLowRes, nsfw, spoiler) = getImageVariantsForRedditPost(redditPost)

        binding.showingHdImage = true

        val url: String? = when {
            imageUrl != null -> imageUrl

            redditPost.isNsfw -> nsfw
            redditPost.isSpoiler -> spoiler

            // If the normal and low res are the same we can treat it as an HD image
            Settings.dataSavingEnabled() && normal != normalLowRes -> {
                binding.showingHdImage = false

                val hdUrl = normal ?: redditPost.url

                setHdImageClickListener(hdUrl)
                extras.putString(EXTRAS_HD_IMAGE_URL, hdUrl)

                normalLowRes
            }

            // Use the post URL as a fallback (since this is an image it will point to an image)
            else -> normal ?: redditPost.url
        }

        // TODO this (I think) has caused crashes (at least on Samsung devices) because the canvas is trying
        //  to draw a bitmap too large. It's hard to reproduce since it only seems to happen some times
        //  and when it happens it might not even happen on the same post (and opening the post in the post itself
        //  instead of just when scrolling works
        //  Exception message: java.lang.RuntimeException: Canvas: trying to draw too large(107867520bytes) bitmap.
        //  Since it's hard to reproduce I'm not even sure if wrapping this section in a try catch works or not
        //  The issue at least happens with extremely large images (although it didn't happen with large images the first time)
        //  https://www.reddit.com/r/dataisbeautiful/comments/kji3wx/oc_2020_electoral_map_if_only_voted_breakdown_by/
        try {
            // No image to load, set image drawable directly
            if (url == null) {
                binding.image.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_image_not_supported_200dp))
            } else {
                // This is for NSFW/spoiler posts, which should not open the bitmap directly (since it is potentially blurred)
                val openBitmap = url != nsfw || url != spoiler

                if (!openBitmap) {
                    extras.putString(EXTRAS_URL_TO_OPEN, normal)
                }

                // When opening the image we always want to open the normal
                setOnClickListener {
                    openImageInFullscreen(
                            binding.image,
                            // Prefer the overridden URL as if it is given, "normal" might be null
                            imageUrl ?: normal,
                            cache,
                            // If the URL is for nsfw/spoiler we want to load the actual image
                            // when opened, not the blurred one
                            useBitmapFromView = openBitmap
                    )
                }

                loadImage(url)
            }
        } catch (e: RuntimeException) {
            e.printStackTrace()
            Log.d(TAG, "\n\n\n--------- ERROR LOADING IMAGE ${redditPost.subreddit}, ${redditPost.title} ---------\n\n\n")
        }
    }

    /**
     * Sets the listener for the HD image button
     */
    private fun setHdImageClickListener(imageUrl: String) {
        binding.hdImage.setOnClickListener {
            loadHdImage(imageUrl)
        }
    }

    /**
     * Loads an image from a URL
     */
    private fun loadImage(url: String) {
        Picasso.get()
                .load(url)
                .placeholder(R.drawable.ic_wifi_tethering_150dp)
                .error(R.drawable.ic_wifi_tethering_150dp)
                .cache(cache)
                .into(binding.image)
    }

    /**
     * Loads a high definition image
     */
    private fun loadHdImage(url: String) {
        binding.showingHdImage = true

        Picasso.get()
                .load(url)
                // Don't use a placeholder as we don't want to remove the previous image
                .noPlaceholder()
                .cache(cache)
                .into(binding.image, object : Callback {
                    override fun onSuccess() {
                        extras.remove(EXTRAS_HD_IMAGE_URL)
                    }

                    override fun onError(e: Exception?) {
                        binding.showingHdImage = false
                        Snackbar.make(this@ContentImage, R.string.failedToLoadImage, Snackbar.LENGTH_SHORT).show()
                    }
                })
    }
}