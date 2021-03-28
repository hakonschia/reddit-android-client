package com.example.hakonsreader.views

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.example.hakonsreader.api.model.Image
import com.example.hakonsreader.databinding.ContentGalleryBinding
import com.example.hakonsreader.misc.Settings
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


    // TODO this ViewPager2 causes issues with AppBarLayout. When scrolling vertically on a RecyclerView item
    //  with this view inside it the appbar wont be hidden (it hides when you "fling" the view, but not if you
    //  hold the entire time you scroll)


    // This file and ContentImage is really coupled together, should be fixed to not be so terrible
    private val binding: ContentGalleryBinding = ContentGalleryBinding.inflate(LayoutInflater.from(context), this, true)
    private lateinit var images: List<Image>
    private val galleryViews: MutableList<ContentGalleryImage> = ArrayList()
    private var currentView: ContentGalleryImage? = null

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
        currentView?.viewSelected()
    }

    override fun viewUnselected() {
        super.viewUnselected()
        galleryViews.forEach {
            it.viewUnselected()
        }
    }

    override fun getWantedHeight() = maxHeight

    /**
     * Releases all views in the gallery
     */
    fun release() {
        galleryViews.forEach(Consumer { obj: ContentGalleryImage -> obj.destroy() })
        galleryViews.clear()
        currentView = null
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