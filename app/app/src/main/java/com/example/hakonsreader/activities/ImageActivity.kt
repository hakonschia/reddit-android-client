package com.example.hakonsreader.activities

import android.content.res.Resources
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.hakonsreader.R
import com.example.hakonsreader.api.utils.LinkUtils
import com.example.hakonsreader.databinding.ActivityImageBinding
import com.example.hakonsreader.misc.Settings
import com.example.hakonsreader.views.listeners.PhotoViewDoubleTapListener
import com.example.hakonsreader.views.util.cache
import com.github.chrisbanes.photoview.PhotoViewAttacher
import com.r0adkll.slidr.Slidr
import com.r0adkll.slidr.model.SlidrInterface
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso

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
        var imageUrl = data.getString(EXTRAS_IMAGE_URL)
        imageUrl = LinkUtils.convertToDirectUrl(imageUrl)
        binding.loadingIcon.visibility = View.VISIBLE

        val cache = data.getBoolean(EXTRAS_CACHE_IMAGE, true)

        Picasso.get()
                .load(imageUrl)
                .resize(Resources.getSystem().displayMetrics.widthPixels, 0)
                .cache(cache)
                .into(binding.image, object : Callback {
                    override fun onSuccess() {
                        binding.loadingIcon.visibility = View.GONE
                    }

                    override fun onError(e: Exception) {
                        // If the user exits the activity we cannot show a dialog, which would cause a crash
                        if (!isDestroyed) {
                            binding.loadingIcon.visibility = View.GONE
                            e.printStackTrace()
                            AlertDialog.Builder(this@ImageActivity)
                                    .setTitle(R.string.imageLoadFailedDialogTitle)
                                    .setMessage(R.string.imageLoadFailedDialogContent)
                                    .setOnDismissListener { finish() }
                                    .show()
                        }
                    }
                })
    }
}