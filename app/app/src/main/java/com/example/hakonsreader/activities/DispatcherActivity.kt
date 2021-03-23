package com.example.hakonsreader.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.misc.CreateIntentOptions
import com.example.hakonsreader.misc.createIntent

class DispatcherActivity : AppCompatActivity() {
    companion object {
        private val TAG = "DispatcherActivity"

        /**
         * The key used to transfer the URL to dispatch
         *
         * The value with should be a [String]
         */
        const val EXTRAS_URL_KEY = "url"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val startIntent: Intent = intent
        val uri = startIntent.data

        // Activity started from a URL intent
        val url: String? = if (uri != null) {
            uri.toString()
        } else {
            // Activity started from a manual intent
            val data = startIntent.extras
            if (data == null) {
                finish()
                return
            }
            data.getString(EXTRAS_URL_KEY)
        }

        if (url == null) {
            finish()
            return
        }

        Log.d(TAG, "Dispatching $url")

        val options = CreateIntentOptions(
                openLinksInternally = App.get().openLinksInApp(),
                openYoutubeVideosInternally = App.get().openYouTubeVideosInApp()
        )

        val intent = createIntent(url, options,this)

        startActivity(intent)

        // Fade in/out videos and images
        val fadeActivityNames = listOf<String>(VideoYoutubeActivity::class.java.name, VideoActivity::class.java.name, ImageActivity::class.java.name)
        val packageName = intent.component?.className
        if (fadeActivityNames.contains(packageName)) {
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