package com.example.hakonsreader.views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.example.hakonsreader.App
import com.example.hakonsreader.databinding.LinkPreviewBinding

/**
 * Class for displaying the preview of a link
 *
 * This shows a text (the hyperlink text of the link) and a link together to show what a link
 * links to
 */
class LinkPreview : FrameLayout {
    companion object {
        private const val TAG = "LinkPreview"
    }

    private val binding: LinkPreviewBinding = LinkPreviewBinding.inflate(LayoutInflater.from(context), this, true).also {
        setOnClickListener { openLink() }
        setOnLongClickListener { copyLink(); return@setOnLongClickListener true }

        if (!App.get().showEntireLinkInLinkPreview()) {
            it.linkLink.maxLines = 2
        }
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)


    /**
     * Sets the text to use for the link preview. This should be the text shown as the hyperlink,
     * not the link itself
     *
     * @param text The text of the link. If this is empty then the view of the text is set to [GONE]
     * @see setLink
     */
    fun setText(text: String) {
        if (text.isNotEmpty()) {
            binding.linkText.text = text
        } else {
            binding.linkText.visibility = GONE
        }
    }

    /**
     * Sets the link to use for the link preview. This should be the actual link
     *
     * @param link The link the preview is for
     * @see setText
     */
    fun setLink(link: String) {
        binding.linkLink.text = link
    }

    private fun openLink() {
        val link = binding.linkLink.text.toString()

        if (link.isNotEmpty()) {
            // Open link
            Log.d(TAG, "openLink: opening $link")
        }
    }

    private fun copyLink() {
        val link = binding.linkLink.text.toString()

        if (link.isNotEmpty()) {
            // Open link
            Log.d(TAG, "copyLink: copying $link")
        }
    }
}