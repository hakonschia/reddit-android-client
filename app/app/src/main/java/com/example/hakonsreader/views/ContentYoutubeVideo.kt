package com.example.hakonsreader.views

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.example.hakonsreader.R
import com.example.hakonsreader.activites.VideoYoutubeActivity
import com.example.hakonsreader.api.model.Image
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.databinding.ContentYoutubeVideoBinding
import com.squareup.picasso.Picasso

class ContentYoutubeVideo : Content {
    // This class only serves as a redirect to VideoYoutubeActivity
    // The Youtube player library uses a WebView, which when used in a RecyclerView with many views
    // makes it extremely laggy

    private val binding: ContentYoutubeVideoBinding = ContentYoutubeVideoBinding.inflate(LayoutInflater.from(context), this, true)

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    override fun updateView() {
        // The previews will (I believe) never be above 1080p, and that should be fine for most devices
        var previews: List<Image>? = redditPost.getPreviewImages()

        // TODO spoiler
        if (redditPost.isNsfw) {
            val obfuscatedPreviews: List<Image>? = redditPost.getObfuscatedPreviewImages()
            if (!obfuscatedPreviews.isNullOrEmpty()) {
                previews = obfuscatedPreviews
            }
        }

        if (!previews.isNullOrEmpty()) {
            val preview: Image = previews.last()

            if (preview.url?.isNotBlank() == true) {
                Picasso.get()
                        .load(preview.url)
                        .into(binding.thumbnail)
            }
        } else {
            val params: ViewGroup.LayoutParams = binding.thumbnail.layoutParams
            params.height = resources.getDimension(R.dimen.contentLinkNoThumbnailSize).toInt()
            binding.thumbnail.layoutParams = params
        }

        binding.root.setOnClickListener {
            val intent = Intent(context, VideoYoutubeActivity::class.java).apply {
                putExtra(VideoYoutubeActivity.VIDEO_ID, getVideoId())
                putExtra(VideoYoutubeActivity.TIMESTAMP, getTimestamp())
            }
            (context as AppCompatActivity).overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            context.startActivity(intent)
        }
    }

    /**
     * Returns the video ID of the post based on [RedditPost.url]
     */
    private fun getVideoId() : String? {
        val asUri = Uri.parse(redditPost.url)

        return if (redditPost.domain == "youtube.com") {
            // https://www.youtube.com/watch?v=90X5NJleYJQ
            asUri.getQueryParameter("v")
        } else {
            // https://youtu.be/90X5NJleYJQ
            asUri.pathSegments.first()
        }
    }

    /**
     * Returns the timestamp of the video based on [RedditPost.url]
     */
    private fun getTimestamp() : Float {
        val asUri = Uri.parse(redditPost.url)

        // https://youtu.be/YOiCkhIZyzs?t=136
        // https://www.youtube.com/watch?v=YOiCkhIZyzs&t=136
        val t = asUri.getQueryParameter("t")?.toInt()

        return t?.toFloat() ?: 0F
    }
}