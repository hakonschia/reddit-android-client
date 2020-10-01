package com.example.hakonsreader.views;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.core.content.ContextCompat;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.activites.VideoActivity;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.api.model.RedditVideo;
import com.example.hakonsreader.constants.NetworkConstants;
import com.example.hakonsreader.misc.VideoCache;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory;
import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

/**
 * The view for video posts
 */
public class ContentVideo extends PlayerView {
    private static final String TAG = "PostContentVideo";

    /**
     * The key used for extra information about the timestamp of the video
     */
    public static final String EXTRA_TIMESTAMP = "videoTimestamp";

    /**
     * The key used for extra information about the playback state of a video
     */
    public static final String EXTRA_IS_PLAYING = "isPlaying";

    /**
     * The key used for extra information about the playback state of a video
     */
    public static final String EXTRA_SHOW_CONTROLS = "showControls";


    /**
     * The lowest width ratio (compared to the screen width) that a video can be, ie. the
     * minimum percentage of the screen the video will take
     */
    private static final float MIN_WIDTH_RATIO = 0.7f;
    /**
     * The max width ratio (compared to the screen width) that a video will be, ie. the
     * maximum percentage of the screen the video will take
     */
    private static final float MAX_WIDTH_RATIO = 1.0f;

    /**
     * The amount of milliseconds it takes before the controller is automatically hidden
     */
    private static final int CONTROLLER_TIMEOUT = 1500;
    /**
     * The amount of milliseconds the controller animation takes
     */
    private static final int CONTROLLER_ANIMATION_DURATION = 200;


    private RedditPost post;
    private RedditVideo redditVideo;

    private ImageView thumbnail;
    private ExoPlayer exoPlayer;
    private MediaSource mediaSource;
    
    /**
     * True if {@link ContentVideo#exoPlayer} has been prepared
     */
    private boolean isPrepared = false;


    public ContentVideo(Context context, RedditPost post) {
        super(context);

        this.post = post;
        this.redditVideo = post.getRedditVideo();
        this.updateView();
    }
    public ContentVideo(Context context) {
        super(context);
    }
    public ContentVideo(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public ContentVideo(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    /**
     * Creates the exo player and updates the view
     */
    private void updateView() {
        this.setSize();

        // Equivalent to "android:animateLayoutChanges="true"", makes the controller fade in/out
        LayoutTransition transition = new LayoutTransition();
        transition.setDuration(CONTROLLER_ANIMATION_DURATION);
        setLayoutTransition(transition);
        setControllerShowTimeoutMs(CONTROLLER_TIMEOUT);

        this.setupExoPlayer();
        setPlayer(exoPlayer);

        /*
        // TODO this doesnt work
        TextView duration = findViewById(R.id.exo_duration);
        duration.setText(String.valueOf(post.getVideoDuration()));

         */

        this.loadThumbnail();
        this.setFullscreenListener();
        this.setVolumeListener();

        // The default volume is on, so if the video should be muted toggle it
        if (App.muteVideoByDefault()) {
            this.toggleVolume();
        }
    }

    /**
     * Sets up {@link ContentVideo#exoPlayer} and {@link ContentVideo#mediaSource}.
     * Use this before calling {@link ContentVideo#setPlayer(Player)}
     */
    private void setupExoPlayer() {
        Context context = getContext();

        // The load control is responsible for how much to buffer at a time
        LoadControl loadControl = new DefaultLoadControl.Builder()
                // Buffer size between 2.5 and 7.5 seconds, with minimum of 1 second for playback to start
                .setBufferDurationsMs(2500, 7500, 1000, 500)
                .createDefaultLoadControl();

        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context, NetworkConstants.USER_AGENT);
        // Convert the data source into a cache source if the user has selected to cache NSFW videos
        if (!(post.isNSFW() && App.dontCacheNSFW())) {
            dataSourceFactory = new CacheDataSourceFactory(VideoCache.getCache(context), dataSourceFactory);
        }

        // With dash video the video gets cached, but it wont play offline (when playing again it doesn't use any
        // network which is the main point)
        mediaSource = new DashMediaSource.Factory(dataSourceFactory)
                .createMediaSource(Uri.parse(redditVideo.getDashURL()));

        exoPlayer = new SimpleExoPlayer.Builder(context)
                .setLoadControl(loadControl)
                .setBandwidthMeter(new DefaultBandwidthMeter.Builder(context).build())
                .setTrackSelector(new DefaultTrackSelector(context, new AdaptiveTrackSelection.Factory()))
                .build();

        // Add listener for buffering changes, playback changes etc.
        exoPlayer.addListener(new Player.EventListener() {
            private ProgressBar loader;

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                // Ensure the thumbnail isn't visible when the video is playing
                if (isPlaying && thumbnail.getVisibility() == VISIBLE) {
                    thumbnail.setVisibility(GONE);
                }
            }

            @Override
            public void onLoadingChanged(boolean isLoading) {
                if (loader == null) {
                    loader = findViewById(R.id.buffering);
                }
                loader.setVisibility((isLoading ? VISIBLE : GONE));
            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                // If the player is trying to play and hasn't yet prepared the video, prepare it
                if (playWhenReady && !isPrepared) {
                    prepare();
                }
            }
        });
    }


    /**
     * Loads the thumbnail for the video into {@link ContentVideo#thumbnail}
     */
    private void loadThumbnail() {
        ViewGroup.LayoutParams params = getLayoutParams();

        thumbnail = findViewById(R.id.thumbnail);
        // Show the thumbnail over the video before it is being played
        Picasso.get()
                .load(post.getThumbnail())
                .resize(params.width, params.height)
                .into(thumbnail);
        // When the thumbnail is shown, clicking it (ie. clicking on the video but not on the controls)
        // "removes" the view so the view turns black
        thumbnail.setOnClickListener(null);
    }

    /**
     * Sets the listener for the fullscreen button.
     * <p>If we are not in a fullscreen video already the video is opened in a {@link VideoActivity}.
     * If we are already in a {@link VideoActivity} the activity is finished to return to the previous screen.
     * The drawable is also changed to an "Exit fullscreen" icon</p>
     */
    private void setFullscreenListener() {
        Context context = getContext();

        ImageButton fullscreen = findViewById(R.id.fullscreen);

        // Open video if we are not in a video activity
        if (!((Activity)context instanceof VideoActivity)) {
            fullscreen.setOnClickListener(view -> {
                // TODO resume at the same point where we ended (for fullscreen and in posts)

                Intent intent = new Intent(context, VideoActivity.class);
                intent.putExtra(VideoActivity.POST, new Gson().toJson(post));
                intent.putExtra("extras", getExtras());

                // Pause the video here so it doesn't play both places
                setPlayback(false);

                context.startActivity(intent);
                ((Activity)context).overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            });
        } else {
            fullscreen.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_baseline_fullscreen_exit_24));
            // If we are in a video activity and press fullscreen, exit instead (should probably have a different icon)
            fullscreen.setOnClickListener(view -> ((Activity)context).finish());
        }
    }

    /**
     * Sets the listener for the volume button
     *
     * <p>This also changes the drawable of the button</p>
     */
    private void setVolumeListener() {
        ImageButton button = findViewById(R.id.volumeButton);

        button.setOnClickListener(view -> {
            Player.AudioComponent audioComponent = exoPlayer.getAudioComponent();
            // No audio, remove the button
            if (audioComponent == null) {
                button.setVisibility(GONE);
            } else {
                toggleVolume();
            }
        });
    }

    /**
     * Toggles the volume on/off
     * <p>The drawable of</p>
     */
    private void toggleVolume() {
        Context context = getContext();
        Player.AudioComponent audioComponent = exoPlayer.getAudioComponent();

        if (audioComponent != null) {
            ImageButton button = findViewById(R.id.volumeButton);
            float volume = audioComponent.getVolume();

            if ((int)volume == 1) {
                audioComponent.setVolume(0f);
                button.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_baseline_volume_off_24));
            } else {
                audioComponent.setVolume(1f);
                button.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_baseline_volume_up_24));
            }
        }
    }



    /**
     * Retrieves the position in the video
     *
     * @return The amount of milliseconds into the video
     */
    public long getCurrentPosition() {
        return exoPlayer.getCurrentPosition();
    }

    /**
     * Retrieve the current playback state
     *
     * @return True if there is a video playing
     */
    public boolean isPlaying() {
        return exoPlayer.getPlayWhenReady();
    }

    /**
     * Retrieve the state of the controllers of the video
     *
     * @return True if the controller is visible
     */
    public boolean isControllerShown() {
        return isControllerVisible();
    }

    /**
     * Releases the video to free up its resources
     */
    public void release() {
        exoPlayer.release();
    }

    /**
     * Prepares the exo player
     */
    public void prepare() {
        if (mediaSource != null && !isPrepared) {
            exoPlayer.prepare(mediaSource);
            isPrepared = true;
        }
    }


    /**
     * Sets the position of the video
     *
     * @param time The amount of milliseconds to go into the video
     */
    public void setPosition(long time) {
        exoPlayer.seekTo(time);
    }

    /**
     * Set if the video should play or not
     *
     * @param play If true the video will start playing
     */
    public void setPlayback(boolean play) {
        exoPlayer.setPlayWhenReady(play);
    }

    /**
     * Set the visibility of the controller of the video
     *
     * @param visible If true the controller will be shown
     */
    public void setControllerVisible(boolean visible) {
        if (visible) {
            showController();
        } else {
            hideController();
        }
    }


    /**
     * Sets the size of the video. Ensures that the video is scaled up if it is too small, and
     * doesn't go too large, while keeping the aspect ratio the same
     *
     * <p>Updates the layout parameters</p>
     */
    private void setSize() {
        // Ensure the video size to screen ratio isn't too large or too small
        float videoRatio = (float) redditVideo.getWidth() / App.getScreenWidth();
        if (videoRatio > MAX_WIDTH_RATIO) {
            videoRatio = MAX_WIDTH_RATIO;
        } else if (videoRatio < MIN_WIDTH_RATIO) {
            videoRatio = MIN_WIDTH_RATIO;
        }

        // Calculate and set the new width and height
        int width = (int)(App.getScreenWidth() * videoRatio);

        // Find how much the width was scaled by and use that to find the new height
        float widthScaledBy = redditVideo.getWidth() / (float)width;
        int height = (int)(redditVideo.getHeight() / widthScaledBy);

        setLayoutParams(new ViewGroup.LayoutParams(width, height));
    }


    /**
     * Retrieve a bundle of information that can be useful for saving the state of the post
     *
     * @return A bundle that might include state variables
     */
    public Bundle getExtras() {
        Bundle extras = new Bundle();

        extras.putLong(ContentVideo.EXTRA_TIMESTAMP, getCurrentPosition());
        extras.putBoolean(ContentVideo.EXTRA_IS_PLAYING, isPlaying());
        Log.d(TAG, "getExtras: " + isPlaying());
        extras.putBoolean(ContentVideo.EXTRA_SHOW_CONTROLS, isControllerShown());

        return extras;
    }
}
