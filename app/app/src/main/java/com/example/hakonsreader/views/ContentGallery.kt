package com.example.hakonsreader.views

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.example.hakonsreader.App
import com.example.hakonsreader.api.model.Image
import com.example.hakonsreader.databinding.ContentGalleryBinding
import com.example.hakonsreader.interfaces.LockableSlidr
import java.util.*
import java.util.function.Consumer

/**
 * Class for gallery posts. A gallery post is simply a collection of multiple images or videos
 */
class ContentGallery : Content {
    companion object {
        private const val TAG = "ContentGallery"

        /**
         * The key for extras in [ContentGallery.getExtras] that tells which image is currently active.
         */
        const val EXTRAS_ACTIVE_IMAGE = "activeImage"
    }

    // This file and ContentImage is really coupled together, should be fixed to not be so terrible
    private val binding: ContentGalleryBinding = ContentGalleryBinding.inflate(LayoutInflater.from(context), this, true)
    private lateinit var images: List<Image>
    private val galleryViews: MutableList<ContentGalleryImage> = ArrayList()

    /**
     * The current Slidr lock this view has called. This should be checked to make sure duplicate calls
     * to lock a Slidr isn't done
     */
    private var slidrLocked = false

    private var maxHeight = -1
    private var maxWidth = -1

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun updateView() {
        images = redditPost.galleryImages ?: return

        // Find the largest height and width and set the layout to that
        for (image in images) {
            val height = image.height
            val width = image.width
            if (height > maxHeight) {
                maxHeight = height
            }
            if (width > maxWidth) {
                maxWidth = width
            }
        }

        // Should scale height to fit with the width as the image will be scaled later
        val screenWidth = App.get().screenWidth
        val widthScale = screenWidth / maxWidth.toFloat()
        layoutParams = ViewGroup.LayoutParams(screenWidth, (maxHeight * widthScale).toInt())

        binding.galleryImages.adapter = Adapter(images)

        // Keep a maximum of 5 items at a time, or 2 when data saving is enabled. This should probably
        // be enough to make large galleries not all load at once which potentially wastes data, and
        // at the same time not have to load items when going through the gallery (unless data saving is on)
        val offscreenLimit = if (App.get().dataSavingEnabled()) 2 else 5
        binding.galleryImages.offscreenPageLimit = offscreenLimit

        binding.galleryImages.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            var currentView: ContentGalleryImage? = null

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                setActiveImageText(position)

                // Make sure the slidr is locked when not on the first item, so that swiping right will
                // swipe on the gallery, not remove the activity (this would probably be wrong for RTL layouts?)
                lockSlidr(position != 0)

                // Unselect the previous and
                currentView?.viewUnselected()

                // Set new and select that
                currentView = findViewWithTag(position)
                currentView?.viewSelected()
            }
        })

        // Set initial state
        setActiveImageText(0)
    }

    /**
     * Updates the text in [ContentGalleryBinding.activeImageText]
     *
     * @param activeImagePos The image position now active.
     */
    private fun setActiveImageText(activeImagePos: Int) {
        // Imgur albums are also handled as galleries, and they might only contain one image, so make it
        // look like only one image by removing the text
        if (images.size == 1) {
            binding.activeImageText.visibility = GONE
        } else {
            binding.activeImageText.text = String.format(Locale.getDefault(), "%d / %d", activeImagePos + 1, images.size)
        }
    }

    /**
     * Lock or unlock a Slidr.
     *
     * @param lock True to lock, false to unlock
     */
    private fun lockSlidr(lock: Boolean) {
        // Return to avoid duplicate calls on the same type of lock
        if (lock == slidrLocked) {
            return
        }
        slidrLocked = lock
        val context = context

        // This might be bad? The "correct" way of doing it might be to add listeners
        // and be notified that way, but I don't want to add 1000 functions to add the listener
        // all the way up here from an activity
        if (context is LockableSlidr) {
            (context as LockableSlidr).lock(lock)
        }
    }

    override fun getExtras(): Bundle {
        return super.getExtras().apply {
            putInt(EXTRAS_ACTIVE_IMAGE, binding.galleryImages.currentItem)
        }
    }

    override fun setExtras(extras: Bundle) {
        super.setExtras(extras)
        val activeImage = extras.getInt(EXTRAS_ACTIVE_IMAGE, images.size)
        binding.galleryImages.setCurrentItem(activeImage, false)
    }

    override fun viewSelected() {
        super.viewSelected()

        // Send a request to lock the slidr if the view is selected when not on the first image
        if (binding.galleryImages.currentItem != 0) {
            lockSlidr(true)
        }
    }

    override fun viewUnselected() {
        super.viewUnselected()

        // Send a request to unlock the slidr if the view is unselected when not on the first image
        if (binding.galleryImages.currentItem != 0) {
            lockSlidr(false)
        }
    }

    override fun getWantedHeight() = maxHeight

    /**
     * Releases all views in the gallery
     */
    fun release() {
        galleryViews.forEach(Consumer { obj: ContentGalleryImage -> obj.destroy() })
        galleryViews.clear()
    }

    private inner class Adapter(private val images: List<Image>) : RecyclerView.Adapter<Adapter.ViewHolder>() {
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
                post = redditPost
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