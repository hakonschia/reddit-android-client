package com.example.hakonsreader.views.util

import android.text.style.URLSpan
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.toSpannable
import com.example.hakonsreader.misc.InternalLinkMovementMethod
import com.example.hakonsreader.misc.showPeekUrlBottomSheet
import com.squareup.picasso.Callback
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import com.squareup.picasso.RequestCreator


/**
 * Sets a long click listener on a TextView that opens a bottom sheet to peek a URL long pressed, if
 * a selection of the TextView was long pressed.
 *
 * @param default If the TextView is pressed on a part of the view not a URL, then this will be invoked
 * instead, if set.
 */
fun TextView.setLongClickToPeekUrl(default: (() -> Unit)? = null) {
    setOnLongClickListener {
        val start = selectionStart
        val end = selectionEnd

        // Selection of the text has been made, ie
        if (start != -1 && end != -1) {
            val method = movementMethod
            if (method is InternalLinkMovementMethod) {
                method.ignoreNextClick()
            }

            val spans = text.toSpannable().getSpans(start, end, URLSpan::class.java)
            if (spans.isNotEmpty()) {
                val span = spans.first()

                val text = text.subSequence(start, end).toString()
                val url = span.url

                if (context is AppCompatActivity) {
                    showPeekUrlBottomSheet(context as AppCompatActivity, text, url)
                }
            } else {
                default?.invoke()
            }
        } else {
            default?.invoke()
        }
        true
    }
}

/**
 * Loads an image from a network URL with a preferred and a backup URL. The backup URL will only
 * be loaded if the preferred image is not cached
 *
 * @param preferredUrl The preferred URL to load, which will only be loaded if it is
 * in cache
 * @param backupUrl The backup URL that will be loaded if [preferredUrl] is not cached
 * @param into The ImageView to load the image into
 */
fun Picasso.loadIf(preferredUrl: String?, backupUrl: String?, into: ImageView) {
    this.load(preferredUrl).networkPolicy(NetworkPolicy.OFFLINE).into(into, object : Callback {
        override fun onSuccess() {
            // Not implemented
        }

        override fun onError(e: Exception?) {
            this@loadIf.load(backupUrl).into(into)
        }
    })
}

/**
 * Enable or disable cache when loading images with Picasso
 *
 * @param cache If true the image will be cached
 * @return A RequestCreator that will set the [RequestCreator.networkPolicy] with [NetworkPolicy.NO_STORE]
 * if [cache] is false. Otherwise, the creator is returned as is
 */
fun RequestCreator.cache(cache: Boolean) : RequestCreator {
    return if (cache) {
        this
    } else {
        this.networkPolicy(NetworkPolicy.NO_STORE)
    }
}