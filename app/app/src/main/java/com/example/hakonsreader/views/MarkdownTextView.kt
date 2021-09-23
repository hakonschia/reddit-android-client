package com.example.hakonsreader.views

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.example.hakonsreader.R
import com.example.hakonsreader.api.utils.MarkdownAdjuster
import com.example.hakonsreader.di.AdjusterWithImages
import com.example.hakonsreader.di.AdjusterWithoutImages
import com.example.hakonsreader.di.MarkwonWithImages
import com.example.hakonsreader.di.MarkwonWithoutImages
import com.example.hakonsreader.misc.InternalLinkMovementMethod
import com.example.hakonsreader.misc.Settings
import com.example.hakonsreader.views.util.setLongClickToPeekUrl
import dagger.hilt.android.AndroidEntryPoint
import io.noties.markwon.Markwon
import javax.inject.Inject


/**
 * Extended TextView to parse and set Markdown. Use [setMarkdown] or [setMarkdownNoLongClick], or the
 * XML equivalents `app:markdown=""` and `app:markdownNoLongClick=""`. If both are set, `markdownNoLongClick`
 * has precedence
 */
@AndroidEntryPoint
class MarkdownTextView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    @MarkwonWithImages
    @Inject
    lateinit var markwon: Markwon

    @AdjusterWithImages
    @Inject
    lateinit var adjuster: MarkdownAdjuster

    @MarkwonWithoutImages
    @Inject
    lateinit var markwonWithoutImages: Markwon

    @AdjusterWithoutImages
    @Inject
    lateinit var adjusterWithoutImages: MarkdownAdjuster

    @Inject
    lateinit var settings: Settings

    init {
        val array = context.theme.obtainStyledAttributes(attrs, R.styleable.MarkdownTextView, 0, 0)
        try {
            // If both markdown and markdownNoLongClick are set, prefer noLongClick
            val markdownNoLongClick = array.getString(R.styleable.MarkdownTextView_markdownNoLongClick)
            if (markdownNoLongClick == null) {
                // Set with empty string as a backup
                val markdown = array.getString(R.styleable.MarkdownTextView_markdown) ?: ""
                setMarkdown(markdown)
            } else {
                setMarkdownNoLongClick(markdownNoLongClick)
            }
        } finally {
            array.recycle()
        }
    }

    /**
     * Sets the markdown as well as setting the long click listener on the view to [setLongClickToPeekUrl]
     * with no default
     */
    fun setMarkdown(markdown: String) {
        setLongClickToPeekUrl()
        setMarkdownNoLongClick(markdown)
    }

    /**
     * Sets the markdown without setting [setLongClickToPeekUrl]
     */
    fun setMarkdownNoLongClick(markdown: String) {
        if (movementMethod == null) {
            movementMethod = InternalLinkMovementMethod()
        }

        if (settings.dataSavingEnabled()) {
            markwonWithoutImages.setMarkdown(this, adjusterWithoutImages.adjust(markdown))
        } else {
            markwon.setMarkdown(this, adjuster.adjust(markdown))
        }
    }
}