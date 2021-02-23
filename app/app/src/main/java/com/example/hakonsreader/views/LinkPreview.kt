package com.example.hakonsreader.views

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.activities.DispatcherActivity
import com.example.hakonsreader.databinding.LinkPreviewBinding
import com.example.hakonsreader.misc.CreateIntentOptions
import com.example.hakonsreader.misc.createIntent
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar

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

    private val binding = LinkPreviewBinding.inflate(LayoutInflater.from(context), this, true).apply {
        setOnClickListener { openLink() }
        setOnLongClickListener { copyLink(); return@setOnLongClickListener true }

        if (!App.get().showEntireLinkInLinkPreview()) {
            linkLink.maxLines = 1
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
        setIcon(link)
    }

    /**
     * Sets the link icon to the icon for the installed application the link resolves to
     */
    private fun setIcon(link: String) {
        // URLs sent here might be of "/r/whatever", so assume those are links to within reddit.com
        // and add the full url so that links that our app resolves will be shown correctly
        val url = if (!link.matches("^http(s)?.*".toRegex())) {
            "https://reddit.com" + (if (link[0] == '/') "" else "/") + link
        } else link

        // Create the intent with internal links as false, otherwise any link would show our app
        // icon as it would resolve to WebViewActivity/VideoYoutubeActivity (if the user had that option enabled)
        val intent = createIntent(url, CreateIntentOptions(openLinksInternally = false, openYoutubeVideosInternally = false), context)

        // Find all activities this intent would resolve to
        val intentActivities = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)

        // Could potentially check if this matches the default browser and not show icon for that, as
        // it will always show something in that case
        if (intentActivities.isNotEmpty()) {
            val packageName = intentActivities[0].activityInfo.packageName
            val icon = context.packageManager.getApplicationIcon(packageName)

            // Kind of want this to be set to icon_color/gray, but setting the tint/colorFilter makes the entire
            // drawable that color
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
                putExtra(DispatcherActivity.URL_KEY, link)
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