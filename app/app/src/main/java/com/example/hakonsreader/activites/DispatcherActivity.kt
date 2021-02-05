package com.example.hakonsreader.activites

import android.content.Intent
import android.content.pm.PackageManager
import android.media.Image
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.api.utils.LinkUtils
import com.example.hakonsreader.misc.createIntent
import com.jakewharton.processphoenix.ProcessPhoenix
import java.util.*
import kotlin.collections.ArrayList

class DispatcherActivity : AppCompatActivity() {
    companion object {
        private val TAG = "DispatcherActivity"

        /**
         * The key used to transfer the URL to dispatch
         *
         *
         * Example URL: https://www.reddit.com/r/
         */
        const val URL_KEY = "url"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val startIntent: Intent = intent
        val uri = startIntent.data
        var url: String?

        // Activity started from a URL intent
        url = if (uri != null) {
            uri.toString()
        } else {
            // Activity started from a manual intent
            val data = startIntent.extras
            if (data == null) {
                finish()
                return
            }
            data.getString(URL_KEY)
        }

        if (url == null) {
            finish()
            return
        }

        Log.d(TAG, "Dispatching $url")

        // If the URL can be converted to a direct link (eg. as an image) ensure it is
        url = LinkUtils.convertToDirectUrl(url)

        // URLs sent here might be of "/r/whatever", so assume those are links to within reddit.com
        // and add the full url so it doesn't have to be handled separately, and potential links we don't
        // handle are sent out correctly to the browser
        if (!url.matches("^http(s)?.*".toRegex())) {
            url = "https://reddit.com" + (if (url[0] == '/') "" else "/") + url
        }

        val intent = url?.let { createIntent(it, this) }

        startActivity(intent)

        val packageName = intent?.component?.className
        if (packageName == VideoActivity::class.java.name || packageName == ImageActivity::class.java.name) {
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
    }

    // onResume is called when activity is returned to by exiting another, and when it starts initially
    // onPause is only called when the activity pauses, such as when starting another activity, so if
    // we have paused previously when in onResume we can finish the activity
    var paused = false

    override fun onPause() {
        super.onPause()
        paused = true
    }

    override fun onResume() {
        super.onResume()
        if (paused) {
            finish()
        }
    }
}