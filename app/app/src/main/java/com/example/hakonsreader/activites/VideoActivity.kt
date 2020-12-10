package com.example.hakonsreader.activites

import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.views.Content
import com.example.hakonsreader.views.ContentVideo
import com.google.gson.Gson
import com.r0adkll.slidr.Slidr

/**
 * Activity displaying a video from a reddit post, allowing for fullscreen
 */
class VideoActivity : AppCompatActivity() {

    companion object {
        /**
         * The key used for the post the video belongs to
         */
        const val POST = "post"
    }

    private var content: ContentVideo? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)

        val data = intent.extras
        if (data != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val controller = window.insetsController
                controller?.hide(WindowInsets.Type.statusBars())
            } else {
                window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }
            val gson = Gson()
            val redditPost = gson.fromJson(intent.extras!!.getString(POST), RedditPost::class.java)
            val extras: Bundle?

            // Restore from the saved state if possible
            extras = if (savedInstanceState != null) {
                savedInstanceState.getBundle(Content.EXTRAS)
            } else {
                data.getBundle(Content.EXTRAS)
            }
            if (!App.get().muteVideoByDefaultInFullscreen()) {
                extras!!.putBoolean(ContentVideo.EXTRA_VOLUME, true)
            }
            content = ContentVideo(this)
            content!!.redditPost = redditPost
            content!!.extras = extras!!
            content!!.fitScreen()
            val video = findViewById<FrameLayout>(R.id.video)
            video.addView(content)
        } else {
            finish()
        }
        val color = getColor(R.color.imageVideoActivityBackground)
        val alpha = color shr 24 and 0xFF
        val alphaPercentage = alpha.toFloat() / 0xFF
        val config = App.get().getVideoAndImageSlidrConfig() // To keep the background the same the entire way the alpha is always the same
                // Otherwise the background of the activity slides with, which looks weird
                .scrimStartAlpha(alphaPercentage)
                .scrimEndAlpha(alphaPercentage)
                .scrimColor(color)
                .build()
        Slidr.attach(this, config)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Store the new extras so that we use that to update the video progress instead of
        // the one passed when the activity was started
        outState.putBundle(Content.EXTRAS, content!!.extras)
    }

    override fun onDestroy() {
        super.onDestroy()
        content!!.release()
    }

    override fun finish() {
        super.finish()
        content!!.release()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

}