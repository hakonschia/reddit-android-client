package com.example.hakonsreader.views

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
import androidx.viewbinding.ViewBinding
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.activities.DispatcherActivity
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.databinding.ContentLinkBinding
import com.example.hakonsreader.databinding.ContentLinkSimpleBinding
import com.example.hakonsreader.misc.Settings
import com.example.hakonsreader.misc.getImageVariantsForRedditPost
import com.example.hakonsreader.views.util.cache
import com.squareup.picasso.Picasso

/**
 * View for displaying links from a [RedditPost]. This will display different views depending on [App.dataSavingEnabled]
 */
class ContentLink : Content {

    val binding: ViewBinding = getCorrectBinding()

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    /**
     * Retrieves and inflates the correct binding to use for the layout based on the users
     * data saving setting
     */
    private fun getCorrectBinding() : ViewBinding {
        return if (Settings.dataSavingEnabled()) {
            ContentLinkSimpleBinding.inflate(LayoutInflater.from(context), this, true)
        } else {
            ContentLinkBinding.inflate(LayoutInflater.from(context), this, true)
        }
    }


    override fun updateView() {
        if (binding is ContentLinkBinding) {
            this.updateViewNormal()
        } else {
            this.updateViewSimple()
        }

        setOnClickListener { openLink() }
    }

    override fun getTransitionViews(): MutableList<Pair<View, String>> {
        return super.getTransitionViews().also {
            it.add(Pair(binding.root, binding.root.transitionName))
        }
    }

    /**
     * Updates the view for the normal
     */
    private fun updateViewNormal() {
        binding as ContentLinkBinding

        val variants = getImageVariantsForRedditPost(redditPost)

        val url = when {
            redditPost.isNsfw -> variants.nsfw
            redditPost.isSpoiler -> variants.spoiler
            else -> variants.normal
        }

        if (url != null) {
            Picasso.get()
                    .load(url)
                    .cache(cache)
                    .into(binding.thumbnail)
        } else {
            val params: ViewGroup.LayoutParams = binding.thumbnail.layoutParams
            params.height = resources.getDimension(R.dimen.contentLinkNoThumbnailSize).toInt()
            binding.thumbnail.layoutParams = params
        }

        binding.link.text = redditPost.url
    }

    /**
     * Updates the view for a simple view. This view uses only the thumbnail for the post, which
     * will be a smaller image and use less data
     */
    private fun updateViewSimple() {
        binding as ContentLinkSimpleBinding

        val thumbnail = redditPost.thumbnail
        // If no thumbnail is given, reddit might give it as "default"
        if (thumbnail.isNotBlank() && thumbnail != "default") {
            binding.thumbnail.scaleType = ImageView.ScaleType.CENTER_CROP
            Picasso.get()
                    .load(thumbnail)
                    .into(binding.thumbnail)
        } else {
            // No thumbnail, set default link symbol
            // This should only be centered, not cropped to fit (as this would stretch the icon)
            binding.thumbnail.scaleType = ImageView.ScaleType.CENTER
            binding.thumbnail.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_baseline_link_24))
        }

        binding.link.text = redditPost.url
    }


    /**
     * Dispatches the link to [DispatcherActivity]
     */
    private fun openLink() {
        val intent = Intent(context, DispatcherActivity::class.java)
        intent.putExtra(DispatcherActivity.EXTRAS_URL_KEY, redditPost.url)
        context.startActivity(intent)
    }


}