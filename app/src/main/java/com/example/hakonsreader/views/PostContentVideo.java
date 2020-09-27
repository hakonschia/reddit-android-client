package com.example.hakonsreader.views;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.hakonsreader.App;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.constants.NetworkConstants;
import com.example.hakonsreader.databinding.LayoutPostContentLinkBinding;
import com.example.hakonsreader.databinding.LayoutPostContentVideoBinding;
import com.example.hakonsreader.misc.VideoCache;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory;
import com.squareup.picasso.Picasso;

public class PostContentVideo extends ConstraintLayout {
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

    private ExoPlayer exoPlayer;
    private MediaSource mediaSource;
    private RedditPost post;

    private LayoutPostContentVideoBinding binding;


    public PostContentVideo(Context context, RedditPost post) {
        super(context);
        this.binding = LayoutPostContentVideoBinding.inflate(LayoutInflater.from(context), this, true);

        this.post = post;
        this.updateView();
    }

    public PostContentVideo(Context context) {
        super(context);
    }
    public PostContentVideo(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public PostContentVideo(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    private void updateView() {
        this.setSize();

        ViewGroup.LayoutParams params = getLayoutParams();
        // Show the thumbnail over the video before it is being played
        Picasso.get()
                .load(post.getThumbnail())
                .resize(params.width, params.height)
                .into(binding.thumbnail);

        Context context = getContext();

        // Create the player
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context, NetworkConstants.USER_AGENT);
        CacheDataSourceFactory cacheFactory = new CacheDataSourceFactory(VideoCache.getCache(context), dataSourceFactory);

        mediaSource = new ProgressiveMediaSource.Factory(cacheFactory, extractorsFactory)
                .createMediaSource(Uri.parse(post.getVideoUrl()));

        exoPlayer = new SimpleExoPlayer.Builder(context)
                .setBandwidthMeter(new DefaultBandwidthMeter.Builder(context).build())
                .setTrackSelector(new DefaultTrackSelector(context, new AdaptiveTrackSelection.Factory()))
                .build();

        exoPlayer.addListener(new Player.EventListener() {
            boolean isPrepared = false;

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                // If the player is trying to play and hasn't yet prepared the video, prepare it
                if (playWhenReady && !isPrepared) {
                    // Remove the thumbnail
                    binding.thumbnail.setVisibility(GONE);
                    exoPlayer.prepare(mediaSource);
                    isPrepared = true;
                }
            }
        });

        binding.player.setPlayer(exoPlayer);
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
        return binding.player.isControllerVisible();
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
        if (mediaSource != null) {
            exoPlayer.prepare(mediaSource);
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
        if (play) {
            binding.thumbnail.setVisibility(GONE);
        }
        exoPlayer.setPlayWhenReady(play);
    }

    /**
     * Set the visibility of the controller of the video
     *
     * @param visible If true the controller will be shown
     */
    public void setControllerVisible(boolean visible) {
        if (visible) {
            binding.player.showController();
        } else {
            binding.player.hideController();
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
        float videoRatio = (float) post.getVideoWidth() / App.getScreenWidth();
        if (videoRatio > MAX_WIDTH_RATIO) {
            videoRatio = MAX_WIDTH_RATIO;
        } else if (videoRatio < MIN_WIDTH_RATIO) {
            videoRatio = MIN_WIDTH_RATIO;
        }

        // Calculate and set the new width and height
        int width = (int)(App.getScreenWidth() * videoRatio);

        // Find how much the width was scaled by and use that to find the new height
        float widthScaledBy = post.getVideoWidth() / (float)width;
        int height = (int)(post.getVideoHeight() / widthScaledBy);

        setLayoutParams(new ViewGroup.LayoutParams(width, height));
    }

    /**
     * Updates the height of the video, keeps the aspect ratio
     *
     * @param height The height to set
     */
    public void updateHeight(int height) {
        ViewGroup.LayoutParams params = getLayoutParams();

        int ratio = params.height / height;

        params.height /= ratio;
        params.width /= ratio;

        setLayoutParams(params);
    }
}
