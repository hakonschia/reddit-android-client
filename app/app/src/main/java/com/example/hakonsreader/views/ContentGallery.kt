package com.example.hakonsreader.views

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.util.Pair
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.example.hakonsreader.api.interfaces.GalleryImage
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.api.model.images.RedditGalleryItem
import com.example.hakonsreader.api.model.thirdparty.imgur.ImgurAlbum
import com.example.hakonsreader.api.model.thirdparty.imgur.ImgurGif
import com.example.hakonsreader.databinding.ContentGalleryBinding
import com.example.hakonsreader.misc.Coordinates
import com.example.hakonsreader.misc.Settings
import java.util.*
import java.util.function.Consumer
import kotlin.collections.ArrayList

/**
 * Class for gallery posts. A gallery post is simply a collection of multiple images or videos
 */
class ContentGallery @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : Content(context, attrs, defStyleAttr) {
    companion object {
        @Suppress("UNUSED")
        private const val TAG = "ContentGallery"

        /**
         * The key for extras in [ContentGallery.getExtras] that tells which image is currently active.
         */
        const val EXTRAS_ACTIVE_IMAGE = "activeImage"


        /**
         * Checks if a reddit post is viewable and can be displayed as a gallery
         */
        fun isRedditPostGalleryViewable(redditPost: RedditPost): Boolean {
            if (redditPost.thirdPartyObject is ImgurAlbum) {
                return true
            } else {
                // Example, in the link below the last image failed (at the time of writing at least)
                // https://www.reddit.com/r/RATS/comments/nqwcun/my_poor_gus_only_last_night_you_were_fishing_for/
                val post = redditPost.crossposts?.firstOrNull() ?: redditPost
                val galleryImages = post.galleryImages ?: return false
                galleryImages.filter { it.status == RedditGalleryItem.STATUS_VALID }

                return galleryImages.isNotEmpty()
            }
        }
    }


    // TODO this ViewPager2 causes issues with AppBarLayout. When scrolling vertically on a RecyclerView item
    //  with this view inside it the appbar wont be hidden (it hides when you "fling" the view, but not if you
    //  hold the entire time you scroll)

    /**
     * The lifecycle owner used to ensure videos in the gallery are automatically paused and released
     */
    var lifecycleOwner: LifecycleOwner? = null

    // This file and ContentImage is really coupled together, should be fixed to not be so terrible
    private val binding: ContentGalleryBinding = ContentGalleryBinding.inflate(LayoutInflater.from(context), this, true)
    private val galleryViews: MutableList<ContentGalleryImage> = ArrayList()
    private var currentView: ContentGalleryImage? = null

    private var maxHeight = -1

    override fun updateView() {
        val images: List<GalleryImage> = if (redditPost.thirdPartyObject is ImgurAlbum) {
            (redditPost.thirdPartyObject as ImgurAlbum).images!!
        } else {
            // Example, in the link below the last image failed (at the time of writing at least)
            // https://www.reddit.com/r/RATS/comments/nqwcun/my_poor_gus_only_last_night_you_were_fishing_for/
            val post = redditPost.crossposts?.firstOrNull() ?: redditPost
            val galleryImages = post.galleryImages ?: return

            galleryImages.filter { it.status == RedditGalleryItem.STATUS_VALID }
        }

        val (maxWidth, maxHeight) = getMaxWidthAndHeight(images)
        this.maxHeight = maxHeight

        // Should scale height to fit with the width as the image will be scaled later
        val screenWidth = Resources.getSystem().displayMetrics.widthPixels
        val widthScale = screenWidth / maxWidth.toFloat()
        layoutParams = ViewGroup.LayoutParams(screenWidth, (maxHeight * widthScale).toInt())

        binding.galleryImages.adapter = Adapter(images)

        // Keep a maximum of 5 items at a time, or 2 when data saving is enabled. This should probably
        // be enough to make large galleries not all load at once which potentially wastes data, and
        // at the same time not have to load items when going through the gallery (unless data saving is on)
        val offscreenLimit = if (Settings.dataSavingEnabled()) 2 else 5
        binding.galleryImages.offscreenPageLimit = offscreenLimit

        binding.galleryImages.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                setActiveImageText(position)

                // Unselect the previous
                currentView?.viewUnselected()

                // Set new and select that
                currentView = findViewWithTag(position)
                currentView?.viewSelected()
            }
        })

        // Set initial state
        val activeImage = extras.getInt(EXTRAS_ACTIVE_IMAGE, 0)
        binding.galleryImages.setCurrentItem(activeImage, false)
    }

    override fun recycle() {
        super.recycle()
        release()
        binding.galleryImages.setCurrentItem(0, false)
        binding.activeImageText.visibility = VISIBLE
    }

    private fun getMaxWidthAndHeight(galleryImages: List<GalleryImage>): Coordinates {
        var maxHeight = -1
        var maxWidth = -1
        var hasVideo = false

        // Find the largest height and width and set the layout to that
        for (image in galleryImages) {
            if (image is RedditGalleryItem && image.mimeType == null) {
                continue
            }

            if (image is ImgurGif || (image is RedditGalleryItem && image.source.mp4Url != null)) {
                hasVideo = true
            }

            val height = image.height
            val width = image.width
            if (height > maxHeight) {
                maxHeight = height
            }
            if (width > maxWidth) {
                maxWidth = width
            }
        }

        return if (hasVideo) {
            VideoPlayer.createResizedVideoSize(maxWidth, maxHeight)
        } else {
            Coordinates(maxWidth, maxHeight)
        }
    }

    /**
     * Updates the text in [ContentGalleryBinding.activeImageText]
     *
     * @param activeImagePos The image position now active.
     */
    private fun setActiveImageText(activeImagePos: Int) {
        // Imgur albums are also handled as galleries, and they might only contain one image, so make it
        // look like only one image by removing the text
        val size = binding.galleryImages.adapter?.itemCount
        if (size == 1) {
            binding.activeImageText.visibility = GONE
        } else {
            binding.activeImageText.text = String.format(Locale.getDefault(), "%d / %d", activeImagePos + 1, size)
        }
    }

    override fun getTransitionViews(): MutableList<Pair<View, String>> {
        return super.getTransitionViews().also { pairs ->
            pairs.add(Pair(binding.parent, binding.parent.transitionName))
        }
    }

    override fun getExtras(): Bundle {
        return super.getExtras().apply {
            putInt(EXTRAS_ACTIVE_IMAGE, binding.galleryImages.currentItem)
        }
    }

    override fun setExtras(extras: Bundle) {
        super.setExtras(extras)
        val activeImage = extras.getInt(EXTRAS_ACTIVE_IMAGE, 0)
        binding.galleryImages.setCurrentItem(activeImage, false)
    }

    override fun viewSelected() {
        currentView?.viewSelected()
    }

    override fun viewUnselected() {
        galleryViews.forEach {
            it.viewUnselected()
        }
    }

    override fun getWantedHeight() = maxHeight

    /**
     * Releases all views in the gallery
     */
    fun release() {
        binding.galleryImages.adapter = null
        galleryViews.forEach(Consumer { obj: ContentGalleryImage -> obj.destroy() })
        galleryViews.clear()
        currentView = null
    }

    private inner class Adapter(private val images: List<GalleryImage>) : RecyclerView.Adapter<Adapter.ViewHolder>() {
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            with (holder.image) {
                // Destroy previous image
                destroy()

                val image = images[position]
                this.image = image
                tag = position
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(ContentGalleryImage(parent.context).apply {
                lifecycleOwner = this@ContentGallery.lifecycleOwner

                // With ViewPager2 the items have to be width=match_parent (although this is how it is in
                // the xml, so not sure why I have to do it here as well)
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                galleryViews.add(this)
            })
        }

        override fun getItemCount() = images.size

        private inner class ViewHolder(val image: ContentGalleryImage) : RecyclerView.ViewHolder(image)
    }
}