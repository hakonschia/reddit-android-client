package com.example.hakonsreader.views

import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import com.example.hakonsreader.R
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.databinding.ContentImageBinding
import com.example.hakonsreader.misc.getImageVariantsForRedditPost
import com.example.hakonsreader.views.util.cache
import com.example.hakonsreader.views.util.openImageInFullscreen
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso

/**
 * Content view for Reddit images posts.
 *
 * Images for NSFW posts are automatically blurred or not shown according to the setting from [App.showNsfwPreview]
 */
class ContentImage : Content {
    private val binding: ContentImageBinding = ContentImageBinding.inflate(LayoutInflater.from(context), this, true)
    private var imageUrl: String? = null

    /**
     * The [Callback] to use for when the post is an image and the image has finished loading
     *
     * This must be set before [Post.setRedditPost]
     */
    var imageLoadedCallback: Callback? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

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
     * Updates the view with the url from [ContentImage.post]
     */
    override fun updateView() {
        val (normal, nsfw, spoiler) = getImageVariantsForRedditPost(redditPost)
        val url: String? = when {
            imageUrl != null -> imageUrl

            redditPost.isNsfw -> nsfw
            redditPost.isSpoiler -> spoiler

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
                // When opening the image we always want to open the normal
                setOnClickListener { openImageInFullscreen(binding.image, imageUrl ?: normal, cache) }

                // If we have an obfuscated image, load that here instead
                Picasso.get()
                        .load(url)
                        .placeholder(R.drawable.ic_wifi_tethering_150dp)
                        .error(R.drawable.ic_wifi_tethering_150dp)
                        .cache(cache)
                        // Scale so the image fits the width of the screen
                        .resize(Resources.getSystem().displayMetrics.widthPixels, 0)
                        .into(binding.image, imageLoadedCallback)
            }
        } catch (e: RuntimeException) {
            e.printStackTrace()
            Log.d(TAG, "\n\n\n--------- ERROR LOADING IMAGE ${redditPost.subreddit}, ${redditPost.title} ---------\n\n\n")
        }
    }

    companion object {
        private const val TAG = "ContentImage"
    }

}