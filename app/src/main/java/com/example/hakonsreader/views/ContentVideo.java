package com.example.hakonsreader.views;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.activites.VideoActivity;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.api.model.RedditVideo;
import com.example.hakonsreader.api.utils.LinkUtils;
import com.example.hakonsreader.constants.NetworkConstants;
import com.example.hakonsreader.misc.Util;
import com.example.hakonsreader.misc.VideoCache;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
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
 * The view for video posts (also includes GIFs)
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
     * The key used for extra information about the volume of the video
     */
    public static final String EXTRA_VOLUME = "volume";


    /**
     * The lowest width ratio (compared to the screen width) that a video can be, ie. the
     * minimum percentage of the screen the video will take
     */
    private static final float MIN_WIDTH_RATIO = 1.0f;
    /**
     * The max width ratio (compared to the screen width) that a video will be, ie. the
     * maximum percentage of the screen the video will take
     */
    private static final float MAX_WIDTH_RATIO = 1.0f;

    /**
     * The max height ratio (compared to the screen height) that a video will be, ie. the maximum
     * percentage of the height the video will take
     */
    private static final float MAX_HEIGHT_RATIO = 0.65f;

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
        this.setVideo();
        this.setSize();

        // Equivalent to "android:animateLayoutChanges="true"", makes the controller fade in/out
        // TODO this creates some weird issues with the lists, see: https://stackoverflow.com/a/32692703/7750841
        LayoutTransition transition = new LayoutTransition();
        transition.setDuration(CONTROLLER_ANIMATION_DURATION);
        setLayoutTransition(transition);
        setControllerShowTimeoutMs(CONTROLLER_TIMEOUT);

        this.setupExoPlayer();
        setPlayer(exoPlayer);

        this.loadThumbnail();
        this.setFullscreenListener();
        this.setVolumeListener();

        if (App.get().muteVideoByDefault()) {
            this.toggleVolume(false);
        }
    }

    /**
     * Sets {@link ContentVideo#redditVideo} if one is available for the post. If not, it will be null.
     * The duration text is also updated here if it is available. If there is no duration available
     * the default exo_duration is made visible to show the duration when the video is loaded.
     */
    private void setVideo() {
        TextView duration = findViewById(R.id.duration);

        redditVideo = post.getVideo();
        if (redditVideo == null) {
            redditVideo = post.getVideoGif();
        }
        // Not all GIFs are returned as a RedditVideo, but if it is we can set the duration now
        if (redditVideo != null) {
            duration.setText(Util.createVideoDuration(redditVideo.getDuration()));
        } else {
            // If we have a GIF with no information about the length of the GIF make it set automatically
            // when the GIf loads with the default duration
            findViewById(R.id.exo_duration).setVisibility(VISIBLE);
            duration.setVisibility(GONE);
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

        mediaSource = this.createMediaSource();

        exoPlayer = new SimpleExoPlayer.Builder(context)
                .setLoadControl(loadControl)
                .setBandwidthMeter(new DefaultBandwidthMeter.Builder(context).build())
                .setTrackSelector(new DefaultTrackSelector(context, new AdaptiveTrackSelection.Factory()))
                .build();

        if (App.get().autoLoopVideos()) {
            exoPlayer.setRepeatMode(Player.REPEAT_MODE_ALL);
        }

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
     * Creates the correct media source for the content depending on what type of video should
     * be displayed
     *
     * @return A {@link MediaSource} object that can be used to display a video
     */
    private MediaSource createMediaSource() {
        Context context = getContext();
        MediaSource media;

        // Data source is constant for all media sources
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context, NetworkConstants.USER_AGENT);
        // Convert the data source into a cache source if the user has selected to cache NSFW videos
        if (!(post.isNSFW() && App.get().dontCacheNSFW())) {
            dataSourceFactory = new CacheDataSourceFactory(VideoCache.getCache(context), dataSourceFactory);
        }

        // If the video is a standard reddit video create as a DASH source
        if (redditVideo != null) {
            // With dash video the video gets cached, but it wont play offline (but when playing again
            // it doesn't use any network which is the main point)
            media = new DashMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(Uri.parse(redditVideo.getDashUrl()));
        } else {
            String url = post.getUrl();

            // Gif uploaded to reddit directly
            if (url.matches("^https://i.redd.it/.*")) {
                url = post.getMp4Source().getUrl();
            } else {
                url = LinkUtils.convertToDirectUrl(url);
            }

            Log.d(TAG, "createMediaSource: " + url);
            media = new ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(Uri.parse(url));

            // Progressive media sources won't have audio to play, so remove the volume button
            findViewById(R.id.volumeButton).setVisibility(GONE);
        }

        return media;
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
        if (!(context instanceof VideoActivity)) {
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
            // If we are in a video activity and press fullscreen, show an "Exit fullscreen" icon and exit the activity
            fullscreen.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_baseline_fullscreen_exit_24));
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
                toggleVolume(null);
            }
        });
    }

    /**
     * Toggles the volume on/off
     *
     * @param on Optional parameter: If set to true the volume is turned on, if false the volume is turned off.
     *           If this is null, the volume will be toggled based on the current state
     */
    private void toggleVolume(@Nullable Boolean on) {
        Player.AudioComponent audioComponent = exoPlayer.getAudioComponent();

        if (audioComponent != null) {
            float volume = audioComponent.getVolume();

            // If volume is off (not 1), set it to on
            if (on == null) {
                on = (int) volume != 1;
            }
            ImageButton button = findViewById(R.id.volumeButton);

            Drawable drawable = ContextCompat.getDrawable(getContext(), on ? R.drawable.ic_baseline_volume_up_24 : R.drawable.ic_baseline_volume_off_24);
            button.setImageDrawable(drawable);

            audioComponent.setVolume(on ? 1f : 0f);
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
     */
    private void setSize() {
        int videoWidth;
        int videoHeight;

        // Video uploaded to reddit directly
        if (redditVideo != null) {
            videoWidth = redditVideo.getWidth();
            videoHeight = redditVideo.getHeight();
        } else {
            // Video not uploaded to reddit (
            // Get width and height from the preview thing

            // If the gif is uploaded to reddit directly the width/height is found in the source preview image
            videoWidth = post.getSourcePreview().getWidth();
            videoHeight = post.getSourcePreview().getHeight();
        }

        // Ensure the video size to screen ratio isn't too large or too small
        float widthRatio = (float) videoWidth / App.get().getScreenWidth();
        if (widthRatio > MAX_WIDTH_RATIO) {
            widthRatio = MAX_WIDTH_RATIO;
        } else if (widthRatio < MIN_WIDTH_RATIO) {
            widthRatio = MIN_WIDTH_RATIO;
        }

        // Calculate and set the new width and height
        int width = (int)(App.get().getScreenWidth() * widthRatio);

        // Find how much the width was scaled by and use that to find the new height
        float widthScaledBy = videoWidth / (float)width;
        int height = (int)(videoHeight / widthScaledBy);


        float heightRatio = (float) height / App.get().getScreenHeight();
        if (heightRatio > MAX_HEIGHT_RATIO) {
            heightRatio = MAX_HEIGHT_RATIO;
        }

        height = (int)(App.get().getScreenHeight() * heightRatio);

        float heightScaledBy = videoHeight / (float)height;
        width = (int)(videoWidth / heightScaledBy);

        setLayoutParams(new ViewGroup.LayoutParams(width, height));
    }


    /**
     * Retrieve a bundle of information that can be useful for saving the state of the post
     *
     * @return A bundle that might include state variables
     */
    public Bundle getExtras() {
        Bundle extras = new Bundle();

        extras.putLong(EXTRA_TIMESTAMP, getCurrentPosition());
        extras.putBoolean(EXTRA_IS_PLAYING, isPlaying());
        extras.putBoolean(EXTRA_SHOW_CONTROLS, isControllerShown());

        Player.AudioComponent audioComponent = exoPlayer.getAudioComponent();
        if (audioComponent != null) {
            extras.putBoolean(EXTRA_VOLUME, (int)audioComponent.getVolume() == 1);
        } else {
            extras.putBoolean(EXTRA_VOLUME, false);
        }

        return extras;
    }

    /**
     * Sets the extras for the video.
     *
     * @param extras The bundle of data to use. This should be the same bundle as retrieved with
     *               {@link ContentVideo#getExtras()}
     */
    public void setExtras(Bundle extras) {
        long timestamp = extras.getLong(EXTRA_TIMESTAMP);
        boolean isPlaying = extras.getBoolean(EXTRA_IS_PLAYING);
        boolean showController = extras.getBoolean(EXTRA_SHOW_CONTROLS);
        boolean volumeOn = extras.getBoolean(EXTRA_VOLUME);

        // Video has been played previously so make sure the player is prepared
        if (timestamp != 0) {
            prepare();
        }
        setPosition(timestamp);
        setPlayback(isPlaying);
        setControllerVisible(showController);
        toggleVolume(volumeOn);
    }
}
