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
}