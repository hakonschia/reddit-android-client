package com.example.hakonsreader.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.example.hakonsreader.api.model.Image
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.databinding.ContentGalleryImageBinding

class ContentGalleryImage : FrameLayout {
    companion object {
        private const val TAG = "ContentGalleryImage"
    }

    private val binding = ContentGalleryImageBinding.inflate(LayoutInflater.from(context), this, true)

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    /**
     * The Image to display
     */
    var image: Image? = null
        set(value) {
            field = value
            updateView()
        }

    var post: RedditPost? = null

    fun destroy() {
        val view = binding.content.getChildAt(0)

        if (view is VideoPlayer) {
            view.release()
        }
    }

    private fun updateView() {
        binding.content.removeAllViews()

        image?.let {
            val view = if (it.isGif) {
                asGif(it)
            } else {
                asImage(it)
            }

            binding.caption.text = it.caption
            binding.caption.visibility = if (it.caption != null) {
                VISIBLE
            } else {
                GONE
            }

            binding.content.addView(view)
        }
    }

    private fun asImage(image: Image) : ContentImage {
        // Use ContentImage as that already has listeners, NSFW caching etc already
        return ContentImage(context).apply {
            setWithImageUrl(post, image.url)
        }
    }

    private fun asGif(image: Image) : VideoPlayer {
        return VideoPlayer(context).apply {
            videoHeight = image.height
            videoWidth = image.width

            // Not sure what to do if this is null
            url = image.mp4Url ?: ""
        }
    }

}