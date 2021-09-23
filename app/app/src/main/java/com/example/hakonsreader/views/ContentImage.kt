package com.example.hakonsreader.views

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.core.util.Pair
import com.example.hakonsreader.databinding.ContentImageBinding
import com.example.hakonsreader.misc.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Content view for Reddit images posts.
 */
@AndroidEntryPoint
class ContentImage @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : Content(context, attrs, defStyleAttr) {

    @Inject
    lateinit var settings: Settings

    private val binding = ContentImageBinding.inflate(LayoutInflater.from(context), this, true)


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

        // getWantedHeight() needs access to the parent view to return anything useful, which might
        // not be accessible instantly (but it is in post{})
        post {
            binding.root.cache = cache
            binding.root.extras = extras
            binding.root.bitmap = bitmap
            binding.root.predefinedHeight = wantedHeight

            if (normal != null) {
                binding.root.state = createDoubleImageViewState(
                    redditPost,
                    normalUrl = normal,
                    lowResUrl = normalLowRes,
                    obfuscatedUrl = obfuscated,
                    settings = settings
                )
            }
        }
    }

    override fun getWantedHeight(): Int {
        val source = redditPost?.getSourcePreview()
        val personWhoBroughtMeIntoTheWorld = parent

        if (source == null || personWhoBroughtMeIntoTheWorld !is View) {
            return super.getWantedHeight()
        }

        // This is how Glide will scale it, which scales it to the match the width of the parent
        // Ie. layout_width=match_parent, and scale with the same aspect ratio to fit that
        val width = source.width
        val height = source.height
        val widthRatio: Float = width.toFloat() / personWhoBroughtMeIntoTheWorld.width
        
        return (height / widthRatio).toInt()
    }
}