package com.example.hakonsreader.activities

import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.views.VideoPlayer
import com.r0adkll.slidr.Slidr

/**
 * Activity displaying a video from a reddit post, allowing for fullscreen
 */
class VideoActivity : BaseActivity() {

    companion object {
        /**
         * The key used for the post the video belongs to
         */
        const val POST = "post"

        /**
         * The key used to send information about the video playback that this activity should
         * automatically resume.
         *
         * This should be a [Bundle]
         */
        const val EXTRAS = "extras"
    }

    private lateinit var videoPlayer: VideoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)

        videoPlayer = findViewById(R.id.videoPlayer)

        val data = intent.extras
        if (data != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val controller = window.insetsController
                controller?.hide(WindowInsets.Type.statusBars())
            } else {
                window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }

            // Restore from the saved state if possible
            val extras = if (savedInstanceState != null) {
                savedInstanceState.getBundle(EXTRAS)
            } else {
                data.getBundle(EXTRAS)
            } ?: return

            if (!App.get().muteVideoByDefaultInFullscreen()) {
                extras.putBoolean(VideoPlayer.EXTRA_VOLUME, true)
            }

            videoPlayer.run {
                isFullscreen = true
                transitionEnabled = true
                dashVideo = extras.getBoolean(VideoPlayer.EXTRA_IS_DASH)
                url = extras.getString(VideoPlayer.EXTRA_URL) ?: ""
                setExtras(extras)

                fitScreen()
                play()

                fullScreenListener = {
                    finish()
                }
            }
        } else {
            finish()
        }

        val color = getColor(R.color.imageVideoActivityBackground)
        val alpha = color shr 24 and 0xFF
        val alphaPercentage = alpha.toFloat() / 0xFF
        val config = App.get().getVideoAndImageSlidrConfig()
                // To keep the background the same the entire way the alpha is always the same
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
        outState.putBundle(EXTRAS, videoPlayer.getExtras())
    }

    override fun onDestroy() {
        super.onDestroy()
        videoPlayer.release()
    }

    override fun finish() {
        super.finish()
        videoPlayer.release()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

}