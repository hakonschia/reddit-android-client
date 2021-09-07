package com.example.hakonsreader.views

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.core.util.Pair
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.databinding.ContentImageBinding
import com.example.hakonsreader.enums.ShowNsfwPreview
import com.example.hakonsreader.misc.*

/**
 * Content view for Reddit images posts.
 */
class ContentImage @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : Content(context, attrs, defStyleAttr) {

    private val binding = ContentImageBinding.inflate(LayoutInflater.from(context), this, true)

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
        return binding.root.getImageBitmap()
    }

    override fun recycle() {
        super.recycle()
        binding.root.recycle()
    }

    override fun getTransitionViews(): MutableList<Pair<View, String>> {
        return super.getTransitionViews().also {
            it.addAll(binding.root.getTransitionViews())
        }
    }

    override fun getExtras(): Bundle {
        return binding.root.extras
    }

    /**
     * Updates the view with the url from [ContentImage.post]
     */
    override fun updateView() {
        val (normal, normalLowRes, obfuscated) = getImageVariantsForRedditPost2(redditPost)
        val dataSavingEnabled = Settings.dataSavingEnabled()

        binding.root.cache = cache
        binding.root.extras = extras
        binding.root.bitmap = bitmap

        // The normal URL will be used in all branches
        if (normal == null) {
            return
        }

        // If no branch below matches then ImageState.NoImage is used by default (internally in DoubleImageView)
        when {
            // TODO spoiler and nsfw don't follow data saving since ImageActivity doesn't allow for two images

            redditPost.isSpoiler -> {
                binding.root.state = DoubleImageView.DoubleImageState.PreviewImage(
                    previewUrl = obfuscated, url = normal
                )
            }

            redditPost.isNsfw -> {
                binding.root.state = when (Settings.showNsfwPreview()) {
                    ShowNsfwPreview.NORMAL -> if (dataSavingEnabled && normalLowRes != null) {
                        DoubleImageView.DoubleImageState.HdImage(
                            lowRes = normalLowRes, highRes = normal
                        )
                    } else {
                        DoubleImageView.DoubleImageState.OneImage(url = normal)
                    }

                    ShowNsfwPreview.BLURRED -> DoubleImageView.DoubleImageState.PreviewImage(
                        previewUrl = obfuscated, url = normal
                    )

                    ShowNsfwPreview.NO_IMAGE -> DoubleImageView.DoubleImageState.PreviewImage(
                        previewUrl = null, url = normal
                    )
                }
            }

            else -> {
                if (!dataSavingEnabled) {
                    binding.root.state = DoubleImageView.DoubleImageState.OneImage(url = normal)
                } else {
                    if (normalLowRes != null) {
                        binding.root.state = DoubleImageView.DoubleImageState.HdImage(
                            lowRes = normalLowRes,
                            highRes = normal
                        )
                    }
                }
            }
        }

        /*
        if (bitmap != null) {
            withBitmap(bitmap!!)
        } else {
            withUrls()
        }
         */
    }

    override fun getWantedHeight(): Int {
        val source = redditPost?.getSourcePreview()
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
        /*
        binding.image.setImageBitmap(b)

        val hdUrl = extras.getString(EXTRAS_HD_IMAGE_URL, null)

        // If no HD URL is given, then it is already loaded and showing
        binding.showingHdImage = hdUrl == null

        hdUrl?.let {
            setHdImageClickListener(it)
        }

        setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (imageLastOpened + OPEN_TIMEOUT > currentTime) {
                return@setOnClickListener
            }

            imageLastOpened = currentTime

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
         */
    }

    /**
     * Loads the image with URLs, either with [imageUrl] or with the image URLs found in [redditPost]
     */
    private fun withUrls() {
        val (normal, normalLowRes, nsfw, spoiler) = getImageVariantsForRedditPost(redditPost)

        /*
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
                val currentTime = System.currentTimeMillis()
                if (imageLastOpened + OPEN_TIMEOUT > currentTime) {
                    return@setOnClickListener
                }

                imageLastOpened = currentTime

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
         */
    }

    /**
     * Sets the listener for the HD image button
     */
    private fun setHdImageClickListener(imageUrl: String) {
        //binding.hdImageIcon.setOnClickListener {
        //    loadHdImage(imageUrl)
        //}
    }

    /**
     * Loads an image from a URL
     */
    private fun loadImage(url: String) {
        /*
        fun load(width: Int? = null, height: Int? = null) {
            if (!context.isAvailableForGlide()) {
                return
            }

            val request = Glide.with(binding.image)
                .load(url)
                .diskCacheStrategy(if (cache) DiskCacheStrategy.AUTOMATIC else DiskCacheStrategy.NONE)
                .transition(DrawableTransitionOptions.withCrossFade())
                .error(R.drawable.ic_image_not_supported_200dp)

            if (width != null && height != null) {
                request.override(width, height)
                    .into(binding.image)
            } else {
                request.into(binding.image)
            }
        }

        // post will run the runnable after the layout has been laid out, so we will have access to the
        // width, which can then be used to scale the image correctly
        val posted = binding.image.post {
            val height = wantedHeight
            val width = binding.image.measuredWidth

            // Set the image size now. Not strictly necessary as it will be set be Glide when the image
            // is loaded, but looks weird if the image just suddenly appears and takes more space in the layout
            // If we're scrolling down this makes it better if the connection is slow
            // If we're scrolling up it might cause some weird jumps (which doesn't happen otherwise for some reason)
            // Since most scrolling is downwards I'll keep this (or add it as a setting later)
            binding.image.updateLayoutParams {
                this.height = height
                this.width = width
            }

            load(width, height)
        }

        if (!posted) {
            load()
        }

         */
    }

    /**
     * Loads a high definition image
     */
    private fun loadHdImage(url: String) {
        /*
        if (!context.isAvailableForGlide()) {
            return
        }

        // currentBitmap will be the low res image, this will be used as the placeholder while loading
        // the new image, as otherwise it will flash black since it is removed for until the new is loaded
        val currentBitmap = getBitmap()

        val width: Int
        val height: Int

        if (currentBitmap != null) {
            width = currentBitmap.width
            height = currentBitmap.height
        } else {
            width = Target.SIZE_ORIGINAL
            height = Target.SIZE_ORIGINAL
        }

        binding.showingHdImage = true

        Glide.with(binding.image)
            .load(url)
            .placeholder(currentBitmap?.toDrawable(resources))
            .diskCacheStrategy(if (cache) DiskCacheStrategy.AUTOMATIC else DiskCacheStrategy.NONE)
            .override(width, height)
            .listener(object : RequestListener<Drawable>{
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    binding.showingHdImage = false
                    Snackbar.make(this@ContentImage, R.string.failedToLoadImage, Snackbar.LENGTH_SHORT).show()
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    extras.remove(EXTRAS_HD_IMAGE_URL)
                    return false
                }
            })
            .into(binding.image)
            */
    }
}