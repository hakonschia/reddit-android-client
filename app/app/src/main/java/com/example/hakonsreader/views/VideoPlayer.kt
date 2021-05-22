package com.example.hakonsreader.views

import android.animation.LayoutTransition
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.hakonsreader.R
import com.example.hakonsreader.misc.Coordinates
import com.example.hakonsreader.misc.Settings
import com.example.hakonsreader.misc.createVideoDuration
import com.example.hakonsreader.views.util.VideoCache
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.extractor.mp4.Mp4Extractor
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.CacheDataSource

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
class VideoPlayer @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : PlayerView(context, attrs, defStyleAttr) {
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
         * The key used for extra information about the volume of the video
         *
         * The value stored with this key will be a `boolean`
         */
        const val EXTRA_VOLUME = "volume"

        /**
         * The key used for extra information about if the video being played has an audio track
         *
         * The value stored with this key will be a `boolean`
         */
        const val EXTRA_HAS_AUDIO = "hasAudio"

        /**
         * The key used for extra information about the size of the video (in bytes)
         *
         * The value stored with this key will be an `int`
         */
        const val EXTRA_VIDEO_SIZE = "videoSize"


        /**
         * Retrieve the resized size the video player will use if set with given values. Videos are
         * resized to ensure the entire video fits the screen, and this function generate those values
         *
         * @param potentialWidth The width of the potential video (what [videoWidth] would be set to)
         * @param potentialHeight The height of the potential video (what [videoHeight] would be set to)
         */
        fun createResizedVideoSize(potentialWidth: Int, potentialHeight: Int): Coordinates {
            val screenWidth = Resources.getSystem().displayMetrics.widthPixels
            val screenHeight = Resources.getSystem().displayMetrics.heightPixels

            // Ensure the video size to screen ratio isn't too large or too small
            var widthRatio: Float = potentialWidth.toFloat() / screenWidth
            if (widthRatio > MAX_WIDTH_RATIO) {
                widthRatio = MAX_WIDTH_RATIO
            } else if (widthRatio < MIN_WIDTH_RATIO) {
                widthRatio = MIN_WIDTH_RATIO
            }

            // Calculate and set the new width and height
            val width = (screenWidth * widthRatio).toInt()

            // Find how much the width was scaled by and use that to find the new height
            val widthScaledBy = potentialWidth / width.toFloat()
            var height = (potentialHeight / widthScaledBy).toInt()

            var heightRatio: Float = height.toFloat() / screenHeight
            if (heightRatio > MAX_HEIGHT_RATIO) {
                heightRatio = MAX_HEIGHT_RATIO
            }

            height = (screenHeight * heightRatio).toInt()

            return Coordinates(width, height)
        }
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

    /**
     * The drawable to use for the thumbnail. If this and [thumbnailUrl] is set, [thumbnailUrl] has
     * precedence. Setting this value will automatically load the thumbnail
     *
     * By default, an "Image not found" drawable is used
     */
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
     * True if the video and thumbnail should be cached
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
            duration.text = createVideoDuration(field)
        }

    /**
     * The size of the video amount of in bytes. This will show a text in the overlay with the size in MB,
     * if the size given is larger than 0 bytes
     */
    var videoSize = -1
        set(value) {
            field = value
            setVideoSizeView()

            if (field > 0) {
                val videoSizeView: TextView = findViewById(R.id.videoSize)

                videoSizeView.visibility = if (value >= 0) {
                    val sizeInMb = videoSize / (1024 * 1024f)
                    videoSizeView.text = context.getString(R.string.videoSize, sizeInMb)
                    VISIBLE
                } else {
                    GONE
                }
            }
        }

    /**
     * If [videoSize] is set by an estimation rather than a precise value, this should be set to true
     * to give an indicator to the user
     */
    var isVideoSizeEstimated = false
        set(value) {
            field = value
            setVideoSizeView()
        }

    /**
     * The actual video height after it has been resized
     */
    var actualVideoHeight = -1

    /**
     * If set to true the controller will be animated (primarily by fading in/out).
     *
     * This should not be used in RecyclerViews, as scrolling can cause the view to jump. This is
     * not enabled by default
     */
    var transitionEnabled = false
        set(value) {
            field = value
            layoutTransition = if (field) {
                LayoutTransition()
            } else {
                null
            }
        }

    /**
     * Set this to true if the video is in fullscreen. This changes the drawable for the fullscreen button
     */
    var isFullscreen = false
        set(value) {
            field = value

            val drawable = if (field) {
               R.drawable.ic_fullscreen_exit_24dp
            } else {
                R.drawable.ic_fullscreen_24dp
            }

            findViewById<ImageButton>(R.id.fullscreen).setImageDrawable(ContextCompat.getDrawable(context, drawable))
        }

    /**
     * Callback for when a video has been manually paused (ie. the pause button has been clicked)
     *
     * This will not be called when the video is paused by any other way (ie. calls to [pause])
     */
    var onManuallyPaused: (() -> Unit)? = null

    /**
     * Callback for when the fullscreen button has been clicked
     */
    var fullScreenListener: (() -> Unit)? = null


    /**
     * The ExoPlayer displaying the video
     */
    private val exoPlayer = createExoPlayer()

    /**
     * The ImageView displaying [thumbnailUrl]
     */
    private val thumbnail: ImageView = findViewById(R.id.thumbnail)

    /**
     * True if [exoPlayer] has been prepared
     */
    private var isPrepared = false

    init {
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
                .build()

        val player = SimpleExoPlayer.Builder(context)
                .setLoadControl(loadControl)
                .setBandwidthMeter(DefaultBandwidthMeter.Builder(context).build())
                .setTrackSelector(DefaultTrackSelector(context, AdaptiveTrackSelection.Factory()))
                .build()

        if (Settings.autoLoopVideos()) {
            player.repeatMode = Player.REPEAT_MODE_ALL
        }

        val loader: ProgressBar = findViewById(R.id.buffering)
        // Add listener for buffering changes, playback changes etc.
        player.addListener(object : Player.EventListener {

            override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {
                super.onTracksChanged(trackGroups, trackSelections)
                var audioFound = false

                for (i in 0 until trackGroups.length) {
                    // We're only looking for audio tracks, so if we found one we don't have to look any more
                    if (audioFound) {
                        break
                    }

                    val trackGroup = trackGroups[i]
                    for (j in 0 until trackGroup.length) {
                        val track = trackGroup.getFormat(j)
                        val mimeType = track.sampleMimeType
                        if (mimeType?.contains("audio".toRegex()) == true) {
                            audioFound = true
                            break
                        }
                    }
                }

                // Don't set hasAudio directly as if we set it to false first then true in the loop
                // it might cause the icon to flash for a split second
                hasAudio = audioFound
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                // Ensure the thumbnail isn't visible when the video is playing
                if (isPlaying) {
                    thumbnail.visibility = GONE

                    // Hide the controller instantly on playback
                    hideController()
                }
            }

            override fun onLoadingChanged(isLoading: Boolean) {
                loader.visibility = if (isLoading) VISIBLE else GONE
            }

            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                // If the player is trying to play and hasn't yet prepared the video, prepare it
                if (playWhenReady && !isPrepared) {
                    prepare()
                }

                controllerShowTimeoutMs = if (playbackState == Player.STATE_BUFFERING) {
                    // When buffering the controller shouldn't be auto hidden, as the thumbnail is part of
                    // the controller it might disappear on slow connections which makes the "video" black
                    -1
                } else {
                    CONTROLLER_TIMEOUT
                }
            }
        })

        return player
    }

    private fun createMediaSource() : MediaSource {
        // TODO on API 21 emulators, only videos directly from imgur load. Not sure if it's a problem
        //  on physical devices, as the ExoPlayer documentation says some emulators don't work.
        //  The official emulator don't support on API < 23
        //  https://google.github.io/ExoPlayer/supported-devices.html

        // Data source is constant for all media sources
        val dataSourceFactory = if (cacheVideo) {
            CacheDataSource.Factory()
                    .setUpstreamDataSourceFactory(DefaultDataSourceFactory(context))
                    .setCache(VideoCache.getCache(context))
        } else {
            DefaultDataSourceFactory(context)
        }

        Log.d(TAG, "createMediaSource: loading $url")

        val mediaItem = MediaItem.Builder()
                .setUri(url)
                .build()

        return if (dashVideo) {
            DashMediaSource.Factory(dataSourceFactory)
        } else {
            val extractor = if (mp4Video) {
                Mp4Extractor.FACTORY
            } else {
                DefaultExtractorsFactory()
            }

            ProgressiveMediaSource.Factory(dataSourceFactory, extractor)
        }.createMediaSource(mediaItem)
    }

    /**
     * Sets the video size on the view based on [videoSize] and [isVideoSizeEstimated]
     */
    private fun setVideoSizeView() {
        val videoSizeView: TextView = findViewById(R.id.videoSize)
        videoSizeView.visibility = if (videoSize > 0) {
            val sizeInMb = videoSize / (1024 * 1024f)
            videoSizeView.text = if (isVideoSizeEstimated) {
                context.getString(R.string.videoSizeEstimated, sizeInMb)
            } else {
                context.getString(R.string.videoSize, sizeInMb)
            }
            VISIBLE
        } else {
            GONE
        }
    }

    /**
     * Loads the thumbnail into [thumbnail]. If [thumbnailUrl] is not empty, then the URL will be loaded.
     * Otherwise [thumbnailDrawable] will be loaded
     */
    fun loadThumbnail() {
        // Set the background color for the controls as a filter here since the thumbnail is shown
        // over the controls
        thumbnail.setColorFilter(ContextCompat.getColor(context, R.color.videoControlBackground))

        // When the thumbnail is shown, clicking it (ie. clicking on the video but not on the controls)
        // "removes" the view so the view turns black
        thumbnail.setOnClickListener(null)

        if (thumbnailUrl.isNotEmpty()) {
            Glide.with(thumbnail)
                    .load(thumbnailUrl)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .override(layoutParams.width, layoutParams.height)
                    .diskCacheStrategy(if (cacheVideo) DiskCacheStrategy.AUTOMATIC else DiskCacheStrategy.NONE)
                    .into(thumbnail)
        } else if (thumbnailDrawable != -1) {
            Glide.with(thumbnail)
                    .load(thumbnailDrawable)
                    .into(thumbnail)
        }
    }

    /**
     * Updates the size of the view (´this´) by the values in [videoHeight] and [videoWidth]
     */
    private fun updateSize() {
        val (_, height) = createResizedVideoSize(videoWidth, videoHeight)
        
        actualVideoHeight = height

        // I don't even really know why this works, but the actual video player will be in the
        // middle of the screen without being stretched, as I want to, and the controller goes
        // to the screen width, which is also what I want
        layoutParams = layoutParams?.also {
            it.width = ViewGroup.LayoutParams.MATCH_PARENT
            it.height = height
        } ?: ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
    }

    /**
     * Sets the onClickListener for the fullscreen button. This simply sets the fullscreen button
     * to invoke [fullScreenListener] when clicked
     */
    private fun setFullscreenListener() {
        findViewById<ImageButton>(R.id.fullscreen).setOnClickListener {
            fullScreenListener?.invoke()
        }
    }

    /**
     * Sets a custom onClickListener for the pause button.
     *
     * [onManuallyPaused] will be called if not null
     */
    private fun setPauseButtonListener() {
        findViewById<ImageButton>(R.id.exo_pause).setOnClickListener {
            // When overriding the click listener we need to manually pause the video
            pause()
            onManuallyPaused?.invoke()
        }
    }

    /**
     * Sets the listener for the volume button
     */
    private fun setVolumeListener() {
        findViewById<ImageButton>(R.id.volumeButton).apply {
            setOnClickListener {
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
            exoPlayer.setMediaSource(it)
            exoPlayer.prepare()
        }
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
        exoPlayer.audioComponent?.let {
            val volume = it.volume

            // If volume is off (not 1), set it to on
            // Use value of "on" if not null, otherwise toggle
            val volumeOn = on ?: (volume.toInt() != 1)

            val drawable = ContextCompat.getDrawable(context, if (volumeOn) R.drawable.ic_volume_up_24dp else R.drawable.ic_volume_off_24dp)
            val button = findViewById<ImageButton>(R.id.volumeButton)
            button.setImageDrawable(drawable)

            it.volume = if (volumeOn) 1f else 0f
        }
    }

    /**
     * Makes the video fit the screen (sets width=MATCH_PARENT and height=WRAP_CONTENT).
     * The controller will be at the bottom of the video, not the screen
     */
    fun fitScreen() {
        layoutParams?.apply {
            this.width = ViewGroup.LayoutParams.MATCH_PARENT
            this.height = ViewGroup.LayoutParams.WRAP_CONTENT
            layoutParams = this
        }
    }

    /**
     * Gets a bitmap of the current frame displayed, or the bitmap displayed in the thumbnail
     * if the video hasn't played yet
     */
    fun getCurrentFrame(): Bitmap? {
        val bitmap = if (videoSurfaceView is TextureView) {
            (videoSurfaceView as TextureView).bitmap
        } else null

        return bitmap ?: thumbnail.drawable?.toBitmap()
    }

    /**
     * Sets a bitmap to the thumbnail
     */
    fun setThumbnailBitmap(bitmap: Bitmap) {
        thumbnail.setImageBitmap(bitmap)
    }

    fun getExtras() : Bundle {
        return Bundle().also {
            it.putLong(EXTRA_TIMESTAMP, getPosition())
            it.putBoolean(EXTRA_IS_PLAYING, isPlaying())
            it.putBoolean(EXTRA_VOLUME, isAudioOn())

            // Probably have to pass thumbnail?
            it.putString(EXTRA_URL, url)
            it.putBoolean(EXTRA_IS_DASH, dashVideo)
            it.putBoolean(EXTRA_HAS_AUDIO, hasAudio)
            it.putInt(EXTRA_VIDEO_SIZE, videoSize)
        }
    }

    fun setExtras(extras: Bundle) {
        val timestamp = extras.getLong(EXTRA_TIMESTAMP)
        val isPlaying = extras.getBoolean(EXTRA_IS_PLAYING)
        val volumeOn = extras.getBoolean(EXTRA_VOLUME)

        hasAudio = extras.getBoolean(EXTRA_HAS_AUDIO)
        videoSize = extras.getInt(EXTRA_VIDEO_SIZE)

        toggleVolume(volumeOn)

        // Video has been played previously so make sure the player is prepared
        if (timestamp != 0L) {
            prepare()
            setPosition(timestamp)
        } else {
            thumbnail.visibility = VISIBLE
            loadThumbnail()
        }

        if (isPlaying) {
            play()
        } else {
            // Probably unnecessary?
            pause()
        }
    }
}