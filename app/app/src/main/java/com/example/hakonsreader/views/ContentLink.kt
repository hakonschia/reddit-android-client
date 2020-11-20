package com.example.hakonsreader.views

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.hakonsreader.R
import com.example.hakonsreader.activites.DispatcherActivity
import com.example.hakonsreader.api.model.Image
import com.example.hakonsreader.databinding.ContentLinkBinding
import com.squareup.picasso.Picasso

class ContentLink : Content {

    val binding: ContentLinkBinding

    constructor(context: Context?) : super(context) {
        binding = ContentLinkBinding.inflate(LayoutInflater.from(context), this, true)
    }
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        binding = ContentLinkBinding.inflate(LayoutInflater.from(context), this, true)
    }
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        binding = ContentLinkBinding.inflate(LayoutInflater.from(context), this, true)
    }
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        binding = ContentLinkBinding.inflate(LayoutInflater.from(context), this, true)
    }

    override fun updateView() {
        // The previews will (I believe) never be above 1080p, and that should be fine for most devices
        // TODO although this will use more data, so it might be reasonable to add a data saving setting where
        //  this image quality is reduced

        var previews: List<Image> = redditPost.previewImages

        if (redditPost.isNsfw) {
            val obfuscatedPreviews: List<Image> = redditPost.obfuscatedPreviewImages
            if (!obfuscatedPreviews.isNullOrEmpty()) {
                previews = obfuscatedPreviews
            }
        }

        if (previews.isNotEmpty()) {
            val preview: Image = previews.last()

            if (preview.url.isNotBlank()) {
                Picasso.get()
                        .load(preview.url)
                        .into(binding.thumbnail)
            }
        } else {
            val params: ViewGroup.LayoutParams = binding.thumbnail.layoutParams
            params.height = resources.getDimension(R.dimen.contentLinkNoThumbnailSize).toInt()
            binding.thumbnail.layoutParams = params
        }

        binding.link.text = redditPost.url
    }


    /**
     * Dispatches the link to [DispatcherActivity]
     */
    private fun openLink() {
        val intent = Intent(context, DispatcherActivity::class.java)
        intent.putExtra(DispatcherActivity.URL_KEY, redditPost.url)
        context.startActivity(intent)
    }


}