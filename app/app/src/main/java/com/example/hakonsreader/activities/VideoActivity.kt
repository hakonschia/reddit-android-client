package com.example.hakonsreader.activities

import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.hakonsreader.R
import com.example.hakonsreader.misc.Settings
import com.example.hakonsreader.misc.asFullscreenActivity
import com.example.hakonsreader.views.VideoPlayer
import com.r0adkll.slidr.Slidr
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Activity displaying a video from a reddit post, allowing for fullscreen
 */
@AndroidEntryPoint
class VideoActivity : BaseActivity() {

    companion object {

        /**
         * The key used to save [videoPlayingWhenActivityPaused]
         */
        private const val SAVED_VIDEO_PLAYING_WHEN_ACTIVITY_PAUSED = "saved_videoPlayingWhenActivityPaused"

        /**
         * The key used to send information about the video playback that this activity should
         * automatically resume.
         *
         * The value sent with this key should be a [Bundle]
         */
        const val EXTRAS_EXTRAS = "extras_VideoActivity_extras"
    }

    @Inject
    lateinit var settings: Settings

    private lateinit var videoPlayer: VideoPlayer
    private var videoPlayingWhenActivityPaused = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)

        videoPlayingWhenActivityPaused = savedInstanceState?.getBoolean(SAVED_VIDEO_PLAYING_WHEN_ACTIVITY_PAUSED) == true

        videoPlayer = findViewById(R.id.videoPlayer)

        val data = intent.extras
        if (data != null) {
            asFullscreenActivity(
                systemBarsBehaviour = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            )

            // Restore from the saved state if possible
            val extras = if (savedInstanceState != null) {
                savedInstanceState.getBundle(EXTRAS_EXTRAS)
            } else {
                data.getBundle(EXTRAS_EXTRAS)
            } ?: return

            if (!settings.muteVideoByDefaultInFullscreen()) {
                extras.putBoolean(VideoPlayer.EXTRA_VOLUME, true)
            }

            videoPlayer.run {
                lifecycle.addObserver(this)

                loopVideo = settings.autoLoopVideos()
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Store the new extras so that we use that to update the video progress instead of
        // the one passed when the activity was started
        outState.putBundle(EXTRAS_EXTRAS, videoPlayer.getExtras())
        outState.putBoolean(SAVED_VIDEO_PLAYING_WHEN_ACTIVITY_PAUSED, videoPlayingWhenActivityPaused)
    }

    override fun onPause() {
        super.onPause()
        videoPlayingWhenActivityPaused = videoPlayer.isPlaying()
    }

    override fun onResume() {
        super.onResume()
        if (videoPlayingWhenActivityPaused) {
            videoPlayer.play()
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}