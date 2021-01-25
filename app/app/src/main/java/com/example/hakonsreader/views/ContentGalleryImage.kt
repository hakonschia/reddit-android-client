package com.example.hakonsreader.views

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.example.hakonsreader.App
import com.example.hakonsreader.activites.DispatcherActivity
import com.example.hakonsreader.api.model.Image
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.databinding.ContentGalleryImageBinding
import com.example.hakonsreader.misc.InternalLinkMovementMethod

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

    fun viewSelected() {
        val view = binding.content.getChildAt(0)

        if (view is VideoPlayer && App.get().autoPlayVideos()) {
            view.play()
        }
    }

    fun viewUnselected() {
        val view = binding.content.getChildAt(0)

        if (view is VideoPlayer && App.get().autoPlayVideos()) {
            view.pause()
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

            with (binding) {
                caption.text = it.caption
                caption.visibility = if (it.caption != null) {
                    VISIBLE
                } else {
                    GONE
                }

                // Setting the movement method to InternalLinkMovementMethod doesn't work for some reason
                outboundUrl.setOnClickListener { _ ->
                    Intent(context, DispatcherActivity::class.java).apply {
                        putExtra(DispatcherActivity.URL_KEY, it.outboundUrl)
                        context.startActivity(this)
                    }
                }
                outboundUrl.text = it.outboundUrl
                outboundUrl.visibility = if (it.outboundUrl != null) {
                    VISIBLE
                } else {
                    GONE
                }

                content.addView(view)
            }
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
            // The height will resize accordingly as long as the width matches the screen
            // I think, I'm pretty lost on this, haven't tested if width of the video is larger
            // than the screen, but I imagine it would scale down
            videoWidth = App.get().screenWidth

            // Not sure what to do if this is null
            url = image.mp4Url ?: ""
        }
    }

}