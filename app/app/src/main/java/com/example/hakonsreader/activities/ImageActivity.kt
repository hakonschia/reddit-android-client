package com.example.hakonsreader.activities

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.hakonsreader.R
import com.example.hakonsreader.api.utils.LinkUtils
import com.example.hakonsreader.databinding.ActivityImageBinding
import com.example.hakonsreader.misc.Settings
import com.example.hakonsreader.misc.isAvailableForGlide
import com.example.hakonsreader.views.listeners.PhotoViewDoubleTapListener
import com.github.chrisbanes.photoview.PhotoViewAttacher
import com.r0adkll.slidr.Slidr
import com.r0adkll.slidr.model.SlidrInterface

/**
 * Activity to display an image in fullscreen
 *
 * The URL to the image should be passed to the activity with the key [EXTRAS_IMAGE_URL]
 */
class ImageActivity : BaseActivity() {

    companion object {
        /**
         * The key used for the URL of the image to display
         *
         * The value with this key should be a [String]
         */
        const val EXTRAS_IMAGE_URL = "extras_ImageActivity_imageUrl"

        /**
         * The key used for to tell if the image being loaded should be cached
         *
         * The value with this key should be a [Boolean]
         */
        const val EXTRAS_CACHE_IMAGE = "extras_ImageActivity_cacheImage"

        /**
         * The bitmap to display. This can be used instead of passing an URL if the image has already
         * been loaded and is in memory.
         *
         * Using this when possible has the benefit of not asking Picasso to reload the image, which can
         * have a small (and very noticeable) delay even without having to do a network request
         *
         * This should be set just before the activity is started and will be nulled when the activity finishes
         */
        var BITMAP: Bitmap? = null
    }

    private var slidrInterface: SlidrInterface? = null
    private lateinit var binding: ActivityImageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // The color retrieved is "0x<alpha><red><green><blue>" (each one byte, 8 bits)
        val color = ContextCompat.getColor(this, R.color.imageVideoActivityBackground)

        // Offset 3 bytes and get the value there to find the alpha
        val alpha = color shr 8 * 3 and 0xFF
        val alphaPercentage = alpha.toFloat() / 0xFF
        val config = Settings.getVideoAndImageSlidrConfig() // To keep the background the same the entire way the alpha is always the same
                // Otherwise the background of the activity slides with, which looks weird
                .scrimStartAlpha(alphaPercentage)
                .scrimEndAlpha(alphaPercentage)
                .scrimColor(color)
                .build()
        slidrInterface = Slidr.attach(this, config)

        val data = intent.extras
        if (data != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val controller = window.insetsController
                controller?.hide(WindowInsets.Type.statusBars())
            } else {
                window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }

            val attacher: PhotoViewAttacher = binding.image.attacher
            attacher.maximumScale = 7f

            binding.image.setOnDoubleTapListener(PhotoViewDoubleTapListener(attacher, slidrInterface))

            if (BITMAP != null) {
                binding.image.setImageBitmap(BITMAP)
            } else {
                loadImageByUrl(data)
            }
        } else {
            finish()
        }
    }

    override fun finish() {
        super.finish()
        BITMAP = null
        overridePendingTransition(R.anim.fade_out, R.anim.fade_out)
    }

    private fun loadImageByUrl(data: Bundle) {
        if (!this.isAvailableForGlide()) {
            return
        }

        var imageUrl = data.getString(EXTRAS_IMAGE_URL)
        imageUrl = LinkUtils.convertToDirectUrl(imageUrl)
        binding.loadingIcon.visibility = View.VISIBLE

        val cache = data.getBoolean(EXTRAS_CACHE_IMAGE, true)

        Glide.with(this)
            .load(imageUrl)
            .diskCacheStrategy(if (cache) DiskCacheStrategy.AUTOMATIC else DiskCacheStrategy.NONE)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    binding.loadingIcon.visibility = View.GONE
                    // If the user exits the activity we cannot show a dialog, which would cause a crash
                    // ^This was the case with Picasso, not sure if it applies for Glide since it uses the activity
                    // for the lifecycle already?
                    if (!isDestroyed) {
                        binding.loadingIcon.visibility = View.GONE
                        AlertDialog.Builder(this@ImageActivity)
                            .setTitle(R.string.imageLoadFailedDialogTitle)
                            .setMessage(R.string.imageLoadFailedDialogContent)
                            .setOnDismissListener { finish() }
                            .show()
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
                    binding.loadingIcon.visibility = View.GONE

                    return false
                }
            })
            .into(binding.image)
    }
}