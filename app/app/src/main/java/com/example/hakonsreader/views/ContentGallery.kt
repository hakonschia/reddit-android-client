package com.example.hakonsreader.views

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.util.Pair
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.example.hakonsreader.activities.PostActivity
import com.example.hakonsreader.api.interfaces.GalleryImage
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.api.model.images.RedditGalleryItem
import com.example.hakonsreader.api.model.thirdparty.imgur.ImgurAlbum
import com.example.hakonsreader.api.model.thirdparty.imgur.ImgurGif
import com.example.hakonsreader.databinding.ContentGalleryBinding
import com.example.hakonsreader.misc.Coordinates
import com.example.hakonsreader.misc.Settings
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.concurrent.fixedRateTimer

/**
 * Class for gallery posts. A gallery post is simply a collection of multiple images or videos
 */
@AndroidEntryPoint
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

    private val binding: ContentGalleryBinding = ContentGalleryBinding.inflate(LayoutInflater.from(context), this, true)

    /**
     * The list of the gallery ViewHolders created by the ViewPager adapter
     */
    private val galleryViewHolders: MutableList<Adapter.ViewHolder> = ArrayList()

    /**
     * The current view visible in the gallery
     */
    private var currentView: ContentGalleryImage? = null

    /**
     * A map of the extras the gallery views have produced so far.
     *
     * The Int key should be the position of view
     */
    private var viewExtras = mutableMapOf<Int, Bundle>()

    private var maxHeight = -1

    @Inject
    lateinit var extrasFlow: MutableSharedFlow<Bundle>

    init {
        // Probably not needed, but this flow is only used to update views when the extras change
        // inside an opened post, so it isn't necessary to set the extras where it's just been changed
        if (context !is PostActivity) {
            (context as? LifecycleOwner)?.lifecycleScope?.launchWhenCreated {
                extrasFlow.collect {
                    if (redditPost?.id == it.getString(EXTRAS_POST_ID, "invalid_id")) {
                        setExtras(it)
                    }
                }
            }
        }
    }

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

        val activeImage = extras.getInt(EXTRAS_ACTIVE_IMAGE, -1)
        val (maxWidth, maxHeight) = getMaxWidthAndHeight(images)
        this.maxHeight = maxHeight

        // Should scale height to fit with the width as the image will be scaled later
        val screenWidth = Resources.getSystem().displayMetrics.widthPixels
        val widthScale = screenWidth / maxWidth.toFloat()
        layoutParams = ViewGroup.LayoutParams(screenWidth, (maxHeight * widthScale).toInt())

        binding.galleryImages.adapter = Adapter(images, activeImage)

        // On data saving use the default which won't load extra pages, so it doesn't waste data by loading
        // images until they are actually selected. Otherwise allow some images to be loaded in advance
        // to be more responsive
        binding.galleryImages.offscreenPageLimit = if (settings.dataSavingEnabled()) {
            ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT
        } else {
            2
        }

        binding.galleryImages.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                setActiveImageText(position)

                // Unselect the previous
                currentView?.viewUnselected()

                // Set new and select that
                currentView = findViewWithTag(position)
                currentView?.viewSelected()

                if (context is PostActivity) {
                    extrasFlow.tryEmit(getExtras())
                }
            }
        })

        // Set initial state
        if (activeImage == -1) {
            binding.galleryImages.setCurrentItem(0, false)
        } else {
            binding.galleryImages.setCurrentItem(activeImage, false)
        }
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

            // Ensure this is up-to-date
            galleryViewHolders.forEach {
                val extras = it.image.getExtras()
                if (extras != null) {
                    viewExtras[it.absoluteAdapterPosition] = extras
                }
            }

            // Use the position of the view (the key in the map) as the key in the bundle
            viewExtras.forEach { (key, value) ->
                putBundle(key.toString(), value)
            }
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
        galleryViewHolders.forEach {
            it.image.viewUnselected()
        }
    }

    override fun getBitmap(): Bitmap? {
        return currentView?.getImageBitmap()
    }

    override fun getWantedHeight() = maxHeight

    /**
     * Releases all views in the gallery
     */
    fun release() {
        binding.galleryImages.adapter = null
        galleryViewHolders.forEach { obj ->
            obj.image.destroy()
        }
        galleryViewHolders.clear()
        currentView = null
    }


    /**
     * @param activeImagePos The position of the image that was active when this content view
     * was opened. If this is not applicable a negative value should be used.
     */
    private inner class Adapter(
        private val images: List<GalleryImage>,
        private val activeImagePos: Int
        ) : RecyclerView.Adapter<Adapter.ViewHolder>() {

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            with (holder.image) {
                // Destroy previous image
                destroy()

                // If bitmap is a low res image and the high res is loaded then this would be out of sync
                // but it's not a big issue so it should be fine
                if (activeImagePos == position) {
                    this.bitmap = this@ContentGallery.bitmap
                }

                // Prefer using the viewExtras, as that will be most up-to-date
                val viewExtras = viewExtras[position] ?: extras.getBundle(position.toString())
                if (viewExtras != null) {
                    setExtras(viewExtras)
                } else {
                    // To ensure the old extras aren't stored
                    setExtras(Bundle())
                }

                val image = images[position]
                this.image = image
                tag = position
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(ContentGalleryImage(parent.context).apply {
                lifecycleOwner = this@ContentGallery.lifecycleOwner

                post = redditPost

                // With ViewPager2 the items have to be width=match_parent (although this is how it is in
                // the xml, so not sure why I have to do it here as well)
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            }).apply {
                galleryViewHolders.add(this)
            }
        }

        override fun onViewRecycled(holder: ViewHolder) {
            val extras = holder.image.getExtras() ?: return
            viewExtras[holder.absoluteAdapterPosition] = extras
        }

        override fun getItemCount() = images.size

        inner class ViewHolder(val image: ContentGalleryImage) : RecyclerView.ViewHolder(image)
    }
}