package com.example.hakonsreader.views

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.activites.VideoActivity
import com.example.hakonsreader.constants.NetworkConstants
import com.example.hakonsreader.misc.Util
import com.example.hakonsreader.views.util.VideoCache
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.extractor.mp4.Mp4Extractor
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory
import com.squareup.picasso.Picasso

/**
 * Class for wrapping an [ExoPlayer] and a [PlayerView] in one class. The size of the video should
 * be set with [videoWidth] and [videoHeight]. This will automatically resize the video view to fit
 * within the screen in the best way possible. The controller for the video will always match the
 * width of the screen.
 *
 * To control if the video should be cached, use [cacheVideo]
 *
 * The video played is set with [url], and optionally if the URL is pointing to a DASH video,
 * set [dashVideo] to `true`.
 *
 * A thumbnail can be displayed with [thumbnailUrl], which will be shown before the video is played.
 * The thumbnail can also be a drawable set with [thumbnailDrawable]. By default, a drawable for
 * "No thumbnail" is shown if no thumbnail URL or drawable is given
 */
class VideoPlayer : PlayerView {
    companion object {
        private const val TAG = "VideoPlayer"

        /**
         * The amount of milliseconds it takes before the controller is automatically hidden
         */
        private const val CONTROLLER_TIMEOUT = 1500

        /**
         * The lowest width ratio (compared to the screen width) that a video can be, ie. the
         * minimum percentage of the screen the video will take
         */
        private const val MIN_WIDTH_RATIO = 1.0f

        /**
         * The max width ratio (compared to the screen width) that a video will be, ie. the
         * maximum percentage of the screen the video will take
         */
        private const val MAX_WIDTH_RATIO = 1.0f

        /**
         * The max height ratio (compared to the screen height) that a video will be, ie. the maximum
         * percentage of the height the video will take
         */
        private const val MAX_HEIGHT_RATIO = 0.65f


        /**
         * The key used in extras for sending which URL is being played in the video
         *
         * The value stored with this key will be a `string`
         */
        const val EXTRA_URL = "videoUrl"

        /**
         * The key used in extras for saying if the video displayed is a DASH video
         *
         * The value stored with this key will be a `boolean`
         */
        const val EXTRA_IS_DASH = "videoIsDash"

        /**
         * The key used for extra information about the timestamp of the video
         *
         * The value stored with this key will be a `long`
         */
        const val EXTRA_TIMESTAMP = "videoTimestamp"

        /**
         * The key used for extra information about the playback state of a video
         *
         * The value stored with this key will be a `boolean`
         */
        const val EXTRA_IS_PLAYING = "isPlaying"

        /**
         * The key used for extra information about the playback state of a video
         *
         * The value stored with this key will be a `boolean`
         */
        const val EXTRA_SHOW_CONTROLS = "showControls"

        /**
         * The key used for extra information about the volume of the video
         *
         * The value stored with this key will be a `boolean`
         */
        const val EXTRA_VOLUME = "volume"

        /**
         * The key used for extra information about the size of the video (in bytes)
         *
         * The value stored with this key will be an `int`
         */
        const val EXTRA_VIDEO_SIZE = "videoSize"
    }

    /**
     * The URL to the video to play
     */
    var url = ""

    /**
     * The URL linking to the thumbnail to use for the video. The thumbnail is shown
     * before the video has been prepared, and is removed afterwards.
     *
     * Setting this value will automatically load the thumbnail
     */
    var thumbnailUrl = ""
        set(value) {
            field = value
            loadThumbnail()
        }

    var thumbnailDrawable = R.drawable.ic_image_not_supported_200dp
        set(value) {
            field = value
            loadThumbnail()
        }

    /**
     * True if [url] points to a DASH format video. This must be set before an attempt to
     * load the video is made
     *
     * Default to `false`
     */
    var dashVideo = false

    /**
     * True if [url] points to a video that should be extracted as MP4. This must be set before an attempt to
     * load the video is made
     *
     * Default to `false`
     */
    var mp4Video = false

    /**
     * True if the video should be cached
     *
     * Default to `true`
     */
    var cacheVideo = true

    /**
     * True if it is known before the video loads if the video has audio. Setting this to false
     * will remove the audio button
     *
     * Default to `true`
     */
    var hasAudio = true
        set(value) {
            field = value
            findViewById<View>(R.id.volumeButton).visibility = if (value) {
                VISIBLE
            } else {
                GONE
            }
        }

    /**
     * The width of the video. Setting this will automatically resize the view
     *
     * By default this will match the screen width
     */
    var videoWidth = -1
        set(value) {
            field = value
            updateSize()
        }

    /**
     * The height of the video. Setting this will automatically resize the view
     */
    var videoHeight = -1
        set(value) {
            field = value
            updateSize()
        }

    /**
     * The duration of the video. This should be set ahead of time if the video duration is known.
     * If the video duration is not known, the duration will be set when the video is loaded
     */
    var videoDuration = -1
        set(value) {
            field = value

            // Use our own view for the duration as we can set it before the video loads
            findViewById<View>(R.id.exo_duration).visibility = GONE
            val duration = findViewById<TextView>(R.id.duration)
            duration.visibility = VISIBLE
            duration.text = Util.createVideoDuration(field)
        }

    /**
     * The size of the video in Megabytes. This will show a text in the overlay with the size in MB
     */
    var videoSize = -1
        set(value) {
            field = value

            if (value >= 0) {
                val videoSizeView: TextView = findViewById(R.id.videoSize)
                val sizeInMb = videoSize / (1024 * 1024f)
                videoSizeView.text = context.getString(R.string.videoSize, sizeInMb)
            }
        }

    /**
     * Callback for when a video has been manually paused (ie. the pause button has been clicked)
     *
     * This will not be called when the video is paused by any other way (ie. calls to [pause])
     */
    var onManuallyPaused: Runnable? = null

    /**
     * The ExoPlayer displaying the video
     */
    private var exoPlayer = createExoPlayer()

    /**
     * The ImageView displaying [thumbnailUrl]
     */
    private var thumbnail: ImageView = findViewById(R.id.thumbnail)

    /**
     * True if [exoPlayer] has been prepared
     */
    private var isPrepared = false


    constructor(context: Context) : super(context) { init() }
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) { init() }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) { init() }

    private fun init() {
        controllerShowTimeoutMs = CONTROLLER_TIMEOUT

        // By default assume we don't know the duration, so use the default ExoPlayer duration which is
        // set when the video loads
        findViewById<View>(R.id.exo_duration).visibility = VISIBLE
        findViewById<TextView>(R.id.duration).visibility = GONE

        player = exoPlayer

        setFullscreenListener()
        setPauseButtonListener()
        setVolumeListener()
    }

    /**
     * Creates a new [ExoPlayer]
     */
    private fun createExoPlayer() : ExoPlayer {
        // The load control is responsible for how much to buffer at a time
        val loadControl: LoadControl = DefaultLoadControl.Builder()
                // Buffer size between 2.5 and 7.5 seconds, with minimum of 1 second for playback to start
                .setBufferDurationsMs(2500, 7500, 1000, 500)
                .createDefaultLoadControl()

        val player = SimpleExoPlayer.Builder(context)
                .setLoadControl(loadControl)
                .setBandwidthMeter(DefaultBandwidthMeter.Builder(context).build())
                .setTrackSelector(DefaultTrackSelector(context, AdaptiveTrackSelection.Factory()))
                .build()

        if (App.get().autoLoopVideos()) {
            player.repeatMode = Player.REPEAT_MODE_ALL
        }

        // Add listener for buffering changes, playback changes etc.
        player.addListener(object : Player.EventListener {
            var loader: ProgressBar = findViewById(R.id.buffering)

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                // Ensure the thumbnail isn't visible when the video is playing
                if (isPlaying) {
                    thumbnail.visibility = GONE

                    // Hide the controller instantly when the state changes
                    hideController()
                }
            }

            override fun onLoadingChanged(isLoading: Boolean) {
                // TODO is this even visible? If the controller isn't visible this wont be visisble
                //  it should be a part of the entire View, not just the controller view
                loader.visibility = if (isLoading) VISIBLE else GONE
            }

            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                // If the player is trying to play and hasn't yet prepared the video, prepare it
                if (playWhenReady && !isPrepared) {
                    prepare()
                }
            }
        })

        return player
    }

    /**
     * Creates a media source with [url] as its source
     *
     * This will check [cacheVideo] and [dashVideo] and create the media source accordingly
     */
    private fun createMediaSource() : MediaSource {
        // Data source is constant for all media sources
        val dataSourceFactory = if (cacheVideo) {
            val defaultFactory = DefaultDataSourceFactory(context, NetworkConstants.USER_AGENT)
            CacheDataSourceFactory(VideoCache.getCache(context), defaultFactory)
        } else {
            DefaultDataSourceFactory(context, NetworkConstants.USER_AGENT)
        }

        Log.d(TAG, "createMediaSource: loading $url")
        return if (dashVideo) {
            DashMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(Uri.parse(url))
        } else {
            if (mp4Video) {
                ProgressiveMediaSource.Factory(dataSourceFactory, Mp4Extractor.FACTORY)
                        .createMediaSource(Uri.parse(url))
            } else {
                ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(Uri.parse(url))
            }
        }
    }

    /**
     * Loads the thumbnail into [thumbnail]. If [thumbnailUrl] is not empty, then the URL will be loaded.
     * Otherwise [thumbnailDrawable] will be loaded
     */
    private fun loadThumbnail() {
        // Set the background color for the controls as a filter here since the thumbnail is shown
        // over the controls
        thumbnail.setColorFilter(ContextCompat.getColor(context, R.color.videoControlBackground))

        // When the thumbnail is shown, clicking it (ie. clicking on the video but not on the controls)
        // "removes" the view so the view turns black
        thumbnail.setOnClickListener(null)

        if (thumbnailUrl.isNotEmpty()) {
            Picasso.get()
                    .load(thumbnailUrl)
                    // .resize(params.width, params.height)
                    .into(thumbnail)
        } else {
            Picasso.get()
                    .load(thumbnailDrawable)
                    .into(thumbnail)
        }
    }

    /**
     * Updates the size of the view (´this´) by the values in [videoHeight] and [videoWidth]
     */
    private fun updateSize() {
        val app = App.get()

        // Ensure the video size to screen ratio isn't too large or too small
        var widthRatio: Float = videoWidth.toFloat() / app.screenWidth
        if (widthRatio > MAX_WIDTH_RATIO) {
            widthRatio = MAX_WIDTH_RATIO
        } else if (widthRatio < MIN_WIDTH_RATIO) {
            widthRatio = MIN_WIDTH_RATIO
        }

        // Calculate and set the new width and height
        val width = (app.screenWidth * widthRatio).toInt()

        // Find how much the width was scaled by and use that to find the new height
        val widthScaledBy = videoWidth / width.toFloat()
        var height = (videoHeight / widthScaledBy).toInt()

        var heightRatio: Float = height.toFloat() / app.screenHeight
        if (heightRatio > MAX_HEIGHT_RATIO) {
            heightRatio = MAX_HEIGHT_RATIO
        }

        height = (app.screenHeight * heightRatio).toInt()


        // I don't even really know why this works, but the actual video player will be in the
        // middle of the screen without being stretched, as I want to, and the controller goes
        // to the screen width, which is also what I want to
        // Kinda weird to create the params like this
        val params = layoutParams ?: ViewGroup.LayoutParams(app.screenWidth, height)
        params.width = app.screenWidth
        params.height = height

        layoutParams = params
    }

    /**
     * Sets the onClickListener for the fullscreen button. This will open a [VideoActivity] with
     * the video. If the activity is in a [VideoActivity] already, this will be a "Close" button instead,
     * to get out of the fullscreen
     */
    private fun setFullscreenListener() {
        val context = context

        val fullscreen = findViewById<ImageButton>(R.id.fullscreen)

        // Open video if we are not in a video activity
        if (context !is VideoActivity) {
            fullscreen.setOnClickListener {
                val intent = Intent(context, VideoActivity::class.java)
                intent.putExtra(VideoActivity.EXTRAS, getExtras())

                // Pause the video here so it doesn't play both places
                pause()
                context.startActivity(intent)
                (context as Activity).overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            }
        } else {
            // If we are in a video activity and press fullscreen, show an "Exit fullscreen" icon and exit the activity
            fullscreen.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_fullscreen_exit_24dp))
            fullscreen.setOnClickListener { (context as Activity).finish() }
        }
    }

    /**
     * Sets a custom onClickListener for the pause button.
     *
     * [onManuallyPaused] will be called if not null
     */
    private fun setPauseButtonListener() {
        val pauseButton: ImageButton = findViewById(R.id.exo_pause)
        pauseButton.setOnClickListener {
            // When overriding the click listener we need to manually pause the video
            pause()
            onManuallyPaused?.run()
        }
    }

    /**
     * Sets the listener for the volume button
     */
    private fun setVolumeListener() {
        val volumeButton: ImageButton = findViewById(R.id.volumeButton)

        volumeButton.setOnClickListener {
            // TODO should this be in the listener? makes no sense really
            val audioComponent = exoPlayer.audioComponent
            // No audio, remove the button
            if (audioComponent == null) {
                it.visibility = GONE
            } else {
                toggleVolume()
            }
        }
    }


    /**
     * Prepares [exoPlayer] for playback
     */
    fun prepare() {
        if (isPrepared) {
            return
        }

        // Create the media source and prepare the exoPlayer
        createMediaSource().also {
            isPrepared = true
            exoPlayer.prepare(it)
        }
    }

    /**
     * Removes the thumbnail from being shown
     */
    fun removeThumbnail() {
        thumbnail.visibility = GONE
    }

    /**
     * Releases the video to free up its resources
     */
    fun release() {
        exoPlayer.release()
    }

    /**
     * Plays the video
     * 
     * @see pause
     */
    fun play() {
        exoPlayer.playWhenReady = true
    }

    /**
     * Pauses the video
     *
     * @see play
     */
    fun pause() {
        exoPlayer.playWhenReady = false
    }

    /**
     * Returns if the video is currently playing
     */
    fun isPlaying() = exoPlayer.playWhenReady

    /**
     * Gets the current timestamp of the video
     * @see setPosition
     */
    fun getPosition() = exoPlayer.currentPosition

    /**
     * Sets the timestamp to seek to in the video
     * @see getPosition
     */
    fun setPosition(timestamp: Long) = exoPlayer.seekTo(timestamp)

    /**
     * Returns if the audio is currently enabled on the video
     */
    fun isAudioOn() = exoPlayer.audioComponent?.volume?.toInt() == 1

    /**
     * Toggles the volume on/off
     *
     * @param on Optional parameter: Set to force volume on or off. If set to true the volume is turned on,
     * if false the volume is turned off. If this is null, the volume will be toggled based on the current state
     */
    fun toggleVolume(on: Boolean? = null) {
        val audioComponent = exoPlayer.audioComponent
        if (audioComponent != null) {
            val volume = audioComponent.volume

            // If volume is off (not 1), set it to on
            // Use value of "on" if not null, otherwise toggle
            val volumeOn = on ?: (volume.toInt() != 1)

            val drawable = ContextCompat.getDrawable(context, if (volumeOn) R.drawable.ic_volume_up_24dp else R.drawable.ic_volume_off_24dp)
            val button = findViewById<ImageButton>(R.id.volumeButton)
            button.setImageDrawable(drawable)

            audioComponent.volume = if (volumeOn) 1f else 0f
        }
    }

    fun fitScreen() {
        // TODO this is a pretty bad way of doing it as the controls get pushed to the bottom of the screen even
        //  if the video itself isn't
        // Can probably use videoWidth to the width the screen width?
        val params = layoutParams
        params.width = ViewGroup.LayoutParams.MATCH_PARENT
        params.height = ViewGroup.LayoutParams.MATCH_PARENT
        layoutParams = params
    }

    fun getExtras() : Bundle {
        return Bundle().also {
            it.putLong(EXTRA_TIMESTAMP, getPosition())
            it.putBoolean(EXTRA_IS_PLAYING, isPlaying())
            it.putBoolean(EXTRA_SHOW_CONTROLS, isControllerVisible)
            it.putBoolean(EXTRA_VOLUME, isAudioOn())

            // Probably have to pass thumbnail?
            it.putString(EXTRA_URL, url)
            it.putBoolean(EXTRA_IS_DASH, dashVideo)
            it.putInt(EXTRA_VIDEO_SIZE, videoSize)
        }
    }

    fun setExtras(extras: Bundle) {
        val timestamp = extras.getLong(EXTRA_TIMESTAMP)
        val isPlaying = extras.getBoolean(EXTRA_IS_PLAYING)
        val showController = extras.getBoolean(EXTRA_SHOW_CONTROLS)
        val volumeOn = extras.getBoolean(EXTRA_VOLUME)

        videoSize = extras.getInt(EXTRA_VIDEO_SIZE)

        toggleVolume(volumeOn)

        // Video has been played previously so make sure the player is prepared
        if (timestamp != 0L) {
            prepare()
            setPosition(timestamp)

            // If the video was paused, remove the thumbnail so it shows the correct frame
            if (!isPlaying) {
                removeThumbnail()
            }
        }

        if (isPlaying) {
            play()
        } else {
            // Probably unnecessary?
            pause()
        }

        if (showController) {
            showController()
        } else {
            hideController()
        }
    }
}