package com.example.hakonsreader.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.hakonsreader.R
import com.example.hakonsreader.misc.Settings
import com.example.hakonsreader.misc.asFullscreenActivity
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.loadOrCueVideo
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.r0adkll.slidr.Slidr
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


/**
 * Activity for displaying a YouTube video in fullscreen
 */
@AndroidEntryPoint
class VideoYoutubeActivity : AppCompatActivity() {
    companion object {
        @Suppress("UNUSED")
        private const val TAG = "VideoYoutubeActivity"


        /**
         * The key used to send to this activity the ID of the YouTube video to play
         *
         * The value with this key should be a [String]
         */
        const val EXTRAS_VIDEO_ID = "extras_VideoYoutubeActivity_videoId"

        /**
         * The key used to send to this activity the timestamp, in seconds, of the video
         *
         * The value with this key should be a [Float]
         */
        const val EXTRAS_TIMESTAMP = "extras_VideoYoutubeActivity_videoTimestamp"
    }

    @Inject
    lateinit var settings: Settings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_youtube)

        val videoId = intent.extras?.getString(EXTRAS_VIDEO_ID)
        val startSeconds = intent.extras?.getFloat(EXTRAS_TIMESTAMP) ?: 0F

        if (videoId == null) {
            finish()
            return
        }

        asFullscreenActivity(
            systemBarsBehaviour = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        )

        findViewById<YouTubePlayerView>(R.id.youtubePlayer).run {
            lifecycle.addObserver(this)

            addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                override fun onReady(youTubePlayer: YouTubePlayer) {
                    youTubePlayer.loadOrCueVideo(lifecycle, videoId, startSeconds)
                }
            })
        }

        val color = ContextCompat.getColor(this, R.color.imageVideoActivityBackground)
        val alpha = color shr 24 and 0xFF
        val alphaPercentage = alpha.toFloat() / 0xFF
        val config = settings.getVideoAndImageSlidrConfig()
                // To keep the background the same the entire way the alpha is always the same
                // Otherwise the background of the activity slides with, which looks weird
                .scrimStartAlpha(alphaPercentage)
                .scrimEndAlpha(alphaPercentage)
                .scrimColor(color)
                .build()

        Slidr.attach(this, config)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}