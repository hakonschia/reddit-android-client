package com.example.hakonsreader.views

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.example.hakonsreader.R
import com.example.hakonsreader.activities.DispatcherActivity
import com.example.hakonsreader.databinding.LinkPreviewBinding
import com.example.hakonsreader.misc.Settings
import com.example.hakonsreader.misc.getAppIconFromUrl
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Class for displaying the preview of a link
 *
 * This shows a text (the hyperlink text of the link) and a link together to show what a link
 * links to
 */
@AndroidEntryPoint
class LinkPreview @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    companion object {
        private const val TAG = "LinkPreview"
    }

    @Inject
    lateinit var settings: Settings

    private val binding = LinkPreviewBinding.inflate(LayoutInflater.from(context), this, true).apply {
        setOnClickListener { openLink() }
        setOnLongClickListener { copyLink(); return@setOnLongClickListener true }

        if (!settings.showEntireLinkInLinkPreview()) {
            linkLink.maxLines = 1
        }
    }


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
        setIcon(link)
    }

    /**
     * Sets the link icon to the icon for the installed application the link resolves to
     */
    private fun setIcon(link: String) {
        val icon = getAppIconFromUrl(context, link)
        if (icon != null) {
            binding.linkSymbol.setImageDrawable(icon)
        }
    }


    /**
     * Opens the link
     */
    private fun openLink() {
        val link = binding.linkLink.text.toString()

        if (link.isNotEmpty()) {
            // Open link
            Intent(context, DispatcherActivity::class.java).run {
                putExtra(DispatcherActivity.EXTRAS_URL_KEY, link)
                context.startActivity(this)
            }
        }
    }

    /**
     * Copies the link to the clipboard
     */
    private fun copyLink() {
        val link = binding.linkLink.text.toString()

        if (link.isNotEmpty()) {
            // At what point do I make a function in a util class for this?
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Comment url", link)
            clipboard.setPrimaryClip(clip)
            Snackbar.make(binding.root, R.string.linkCopied, BaseTransientBottomBar.LENGTH_SHORT).show()
        }
    }
}