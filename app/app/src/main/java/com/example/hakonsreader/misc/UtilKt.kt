package com.example.hakonsreader.misc

import android.widget.ImageView
import com.squareup.picasso.Callback
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import java.lang.Exception

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