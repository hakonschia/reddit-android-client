package com.example.hakonsreader.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.util.Pair
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.hakonsreader.R
import com.example.hakonsreader.databinding.DoubleImageViewBinding
import com.example.hakonsreader.misc.isAvailableForGlide
import com.example.hakonsreader.views.util.goneIf
import com.example.hakonsreader.views.util.openImageInFullscreen
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch


/**
 * A View that supports the display of multiple images that can change dynamically
 */
class DoubleImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        /**
         * The amount of milliseconds that should the used to wait to open the image when clicked, to avoid
         * accidentally open the image twice by clicking fast
         */
        private const val OPEN_TIMEOUT = 1250L

        /**
         * The key for the extras that says if the HD image is loaded.
         *
         * The value with this key is a [Boolean]. A value of `false` does not imply that
         * [state] is [DoubleImageView.DoubleImageState.HdImage], but `true` does.
         */
        const val EXTRAS_HD_IMAGE_LOADED = "extras_hdImageLoaded"
    }

    /**
     * The states the image can have
     */
    sealed class DoubleImageState {
        /**
         * No image will be loaded. A "no image found" icon will be shown
         */
        object NoImage : DoubleImageState()

        /**
         * Only one image will be shown. This will behave like a normal ImageView
         */
        class OneImage(val url: String) : DoubleImageState()

        /**
         * Two images can be loaded. [lowRes] points to a low resolution URL of the image and [highRes]
         * points to the HD image.
         *
         * [lowRes] will be loaded initially, and an icon will be shown that allows the user to
         * manually load [highRes]
         */
        class HdImage(val lowRes: String, val highRes: String) : DoubleImageState()

        /**
         * Show a preview image which will open into another image.
         *
         * If [previewUrl] is ´null´ then a "no image found" icon will be shown instead.
         * [url] points to the image that will be shown when the image is clicked and opened in
         * fullscreen
         */
        class PreviewImage(val previewUrl: String?, val url: String) : DoubleImageState()
    }


    private val binding = DoubleImageViewBinding.inflate(LayoutInflater.from(context), this, true)

    /**
     * The timestamp the last time the image was opened (or -1 if no image has been opened)
     */
    private var imageLastOpened: Long = -1

    /**
     * The state of the image. Default is [DoubleImageState.NoImage]
     *
     * Setting this will automatically update the view.
     */
    var state: DoubleImageState = DoubleImageState.NoImage
        set(value) {
            field = value
            updateView(value)
        }

    /**
     * Set to false to disable caching of the images
     */
    var cache: Boolean = true

    /**
     * Set this bitmap to override everything else and not load an image with Glide
     */
    var bitmap: Bitmap? = null
        set(value) {
            field = value
            if (value != null) {
                binding.image.setImageBitmap(value)
            }
        }

    var extras: Bundle = Bundle()


    /**
     * Recycles the view
     */
    fun recycle() {
        state = DoubleImageState.NoImage
        cache = true
        bitmap = null
        imageLastOpened = -1
        extras = Bundle()
    }

    /**
     * Gets a bitmap representation of the image currently being displayed
     */
    fun getImageBitmap(): Bitmap? {
        return binding.image.drawable?.toBitmap()
    }

    /**
     * @return A list of pairs that can be used when opening new activities with a transition
     */
    fun getTransitionViews(): List<Pair<View, String>> {
        return mutableListOf<Pair<View, String>>().apply {
            // I can't just add this in the constructor because it can't infer the type? Dunno
            add(Pair(binding.image, binding.image.transitionName))

            // If we add this when it is not visible it will become visible again, which
            // makes the icon flash for a split second, and will appear again when you exit
            if (binding.hdImageIcon.visibility == View.VISIBLE) {
                add(Pair(binding.hdImageIcon, binding.hdImageIcon.transitionName))
            }
        }
    }

    /**
     * Updates the view
     *
     * @param imageState The state to use. [state] will not be used
     */
    private fun updateView(imageState: DoubleImageState) {
        when (imageState) {
            DoubleImageState.NoImage -> asNoImage()
            is DoubleImageState.OneImage -> asOneImage(imageState)
            is DoubleImageState.HdImage -> asHdImage(imageState)
            is DoubleImageState.PreviewImage -> asPreviewImage(imageState)
        }
    }


    /**
     * Loads a url into the ImageView in [binding]
     *
     * This checks that [getContext] is available for Glide with [isAvailableForGlide] before
     * attempting to load the image
     *
     * @param url The image URL to load
     * @param transition If true a cross fade transition will be used to load the image, otherwise no
     * transition will be used. Default to true
     * @param placeholder An optional drawable to use as the placeholder
     * @param onlyLoadFromCache If true no network request will be made and the image will only be loaded
     * if it has been previously loaded and is available from cache. Default to false
     * @param listener An optional request listener for when the image has been loaded/failed to load
     */
    private fun loadUrl(
        url: String,
        transition: Boolean = true,
        placeholder: Drawable? = null,
        onlyLoadFromCache: Boolean = false,
        listener: RequestListener<Drawable>? = null
    ) {
        if (!context.isAvailableForGlide()) {
            return
        }

        var request = Glide.with(binding.image)
            .load(url)
            .diskCacheStrategy(if (cache) DiskCacheStrategy.AUTOMATIC else DiskCacheStrategy.NONE)
            // We cannot just use the resource ID here, as it doesn't respect the theme
            .error(ContextCompat.getDrawable(context, R.drawable.ic_image_not_supported_200dp))
            .placeholder(placeholder)
            .onlyRetrieveFromCache(onlyLoadFromCache)
            .listener(listener)

        if (transition) {
            request = request.transition(DrawableTransitionOptions.withCrossFade())
        }

        request.into(binding.image)
    }

    /**
     * Loads a "no image found" icon
     */
    private fun asNoImage() {
        binding.hdImageIcon.visibility = GONE

        binding.image.setImageResource(R.drawable.ic_image_not_supported_200dp)
    }

    /**
     * Loads the URL given by [DoubleImageState.OneImage.url]
     */
    private fun asOneImage(imageState: DoubleImageState.OneImage) {
        binding.hdImageIcon.visibility = GONE

        if (bitmap == null) {
            loadUrl(imageState.url)
        }

        setClickListener(
            url = imageState.url,
            useBitmapFromView = true
        )
    }

    /**
     * Loads [DoubleImageState.HdImage.lowRes] initially. The HD image icon is shown and will load
     * [DoubleImageState.HdImage.highRes] when clicked.
     */
    private fun asHdImage(imageState: DoubleImageState.HdImage) {
        /**
         * Removes the HD icon, updates [extras], and sets the click listener to use the high res image
         */
        fun hdImageIsLoaded() {
            binding.hdImageIcon.visibility = GONE

            extras.putBoolean(EXTRAS_HD_IMAGE_LOADED, true)

            setClickListener(
                url = imageState.highRes,
                useBitmapFromView = true
            )
        }

        val hdImageLoaded = extras.getBoolean(EXTRAS_HD_IMAGE_LOADED)

        binding.hdImageIcon.goneIf(hdImageLoaded)

        if (bitmap == null) {
            // Attempt to load the high res image first from cache
            loadUrl(imageState.highRes, onlyLoadFromCache = true, listener = object : RequestListener<Drawable>{
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    // Couldn't load high res from cache, load low res instead
                    // Glide throws an exception if you initiate a load from inside a load, so call it
                    // from the main thread
                    MainScope().launch {
                        loadUrl(imageState.lowRes)
                    }
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    hdImageIsLoaded()
                    return false
                }
            })
        }

        setClickListener(
            url = if (hdImageLoaded) imageState.highRes else imageState.lowRes,
            useBitmapFromView = true
        )

        binding.hdImageIcon.setOnClickListener {
            // Use the current bitmap (the low res image) as the placeholder, otherwise it will flash black
            // since the image is removed when a new one is loaded by Glide
            val placeholder = getImageBitmap()?.toDrawable(resources)

            loadUrl(imageState.highRes, transition = false, placeholder = placeholder, listener = object : RequestListener<Drawable>{
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    Snackbar.make(this@DoubleImageView, R.string.failedToLoadImage, Snackbar.LENGTH_SHORT).show()
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    hdImageIsLoaded()
                    return false
                }
            })
        }
    }

    /**
     * Loads the URL given by [DoubleImageState.PreviewImage.previewUrl] if it is not null and sets the
     * click listener of the view to open [DoubleImageState.PreviewImage.url]. If the preview URL is null
     * then a "no image found" icon is shown
     */
    private fun asPreviewImage(imageState: DoubleImageState.PreviewImage) {
        binding.hdImageIcon.visibility = GONE

        if (imageState.previewUrl != null) {
            if (bitmap == null) {
                loadUrl(imageState.previewUrl)
            }
        } else {
            Glide.with(binding.image)
                .load(R.drawable.ic_image_not_supported_200dp)
                .into(binding.image)
        }

        setClickListener(
            url = imageState.url,
            // The bitmap will be the preview (the actual image should be opened)
            useBitmapFromView = false
        )
    }

    /**
     * Sets the click listener for the view to open the image in fullscreen. The listener set
     * ensures that the image cannot be opened multiple times in short succession
     *
     * @param url The URL of the image that should be opened
     * @param useBitmapFromView If set to true the bitmap in the image will be used
     * directly. If false then [url] will be used
     */
    private fun setClickListener(url: String, useBitmapFromView: Boolean) {
        setOnClickListener {
            val currentTime = System.currentTimeMillis()

            if (imageLastOpened + OPEN_TIMEOUT <= currentTime) {
                imageLastOpened = currentTime

                openImageInFullscreen(
                    binding.image,
                    url,
                    cache,
                    useBitmapFromView
                )
            }
        }
    }
}