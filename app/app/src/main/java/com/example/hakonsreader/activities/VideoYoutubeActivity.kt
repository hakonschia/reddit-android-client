package com.example.hakonsreader.activities

import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.r0adkll.slidr.Slidr


/**
 * Activity for displaying a YouTube video in fullscreen
 */
class VideoYoutubeActivity : BaseActivity() {
    companion object {
        private const val TAG = "VideoYoutubeActivity"


        /**
         * The key used to send to this activity the ID of the YouTube video to play
         *
         * The value with this key should be a [String]
         */
        const val VIDEO_ID = "videoId"

        /**
         * The key used to send to this activity the timestamp, in seconds, of the video
         *
         * The value with this key should be a [Float]
         */
        const val TIMESTAMP = "videoTimestamp"


        /**
         * The key used to store the timestamp of the video when the activity was recreated
         *
         * The value with this key should be a [Float]
         */
        private const val SAVED_TIMESTAMP = "savedVideoTimestamp"
    }

    private var currentTimestamp = 0F

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_youtube)

        val videoId = intent.extras?.getString(VIDEO_ID)

        // Prefer the timestamp saved in onSaveInstanceState, then the value passed to the activity
        val startSeconds = savedInstanceState?.getFloat(SAVED_TIMESTAMP) ?: intent.extras?.getFloat(TIMESTAMP) ?: 0F

        if (videoId == null) {
            finish()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = window.insetsController
            controller?.hide(WindowInsets.Type.statusBars())
        } else {
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
        
        findViewById<YouTubePlayerView>(R.id.youtubePlayer).run {
            lifecycle.addObserver(this)

            addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                override fun onReady(youTubePlayer: YouTubePlayer) {
                    youTubePlayer.loadVideo(videoId, startSeconds)
                }

                override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                    super.onCurrentSecond(youTubePlayer, second)
                    currentTimestamp = second
                }
            })
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
        outState.putFloat(SAVED_TIMESTAMP, currentTimestamp)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}