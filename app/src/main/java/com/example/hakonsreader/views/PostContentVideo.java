package com.example.hakonsreader.views;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.example.hakonsreader.App;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.constants.NetworkConstants;
import com.example.hakonsreader.databinding.LayoutPostContentVideoBinding;
import com.example.hakonsreader.misc.VideoCache;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory;

public class PostContentVideo extends LinearLayout {
    private static final String TAG = "PostContentVideo";

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


    private LayoutPostContentVideoBinding binding;
    private ExoPlayer exoPlayer;

    private RedditPost post;
    private int videoWidth;
    private int videoHeight;

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
        Context context = getContext();

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

        // Create the player
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context, NetworkConstants.USER_AGENT);
        CacheDataSourceFactory cacheFactory = new CacheDataSourceFactory(VideoCache.getCache(context), dataSourceFactory);

        MediaSource mediaSource = new ProgressiveMediaSource.Factory(cacheFactory, extractorsFactory)
                .createMediaSource(Uri.parse(post.getVideoUrl()));

        exoPlayer = new SimpleExoPlayer.Builder(context)
                .setBandwidthMeter(new DefaultBandwidthMeter.Builder(context).build())
                .setTrackSelector(new DefaultTrackSelector(context, new AdaptiveTrackSelection.Factory()))
                .build();
        exoPlayer.prepare(mediaSource);
        exoPlayer.setPlayWhenReady(false);

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
     * Sets the position of the video
     *
     * @param time The amount of milliseconds to go into the video
     */
    public void setPosition(long time) {
        exoPlayer.seekTo(time);
    }

    /**
     * Releases the video to free up its resources
     */
    public void release() {
        exoPlayer.release();
    }

    /**
     * Plays the video
     */
    public void play() {
        exoPlayer.setPlayWhenReady(true);
    }

    /**
     * Pauses the video
     */
    public void pause() {
        exoPlayer.setPlayWhenReady(false);
    }

    /* Shamelessly stolen from https://stackoverflow.com/a/19075245 */
    /*
    public void setVideoSize(int width, int height) {
        videoWidth = width;
        videoHeight = height;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Log.i("@@@", "onMeasure");
        int width = getDefaultSize(videoWidth, widthMeasureSpec);
        int height = getDefaultSize(videoHeight, heightMeasureSpec);
        if (videoWidth > 0 && videoHeight > 0) {
            if (videoWidth * height > width * videoHeight) {
                // Log.i("@@@", "image too tall, correcting");
                height = width * videoHeight / videoWidth;
            } else if (videoWidth * height < width * videoHeight) {
                // Log.i("@@@", "image too wide, correcting");
                width = height * videoWidth / videoHeight;
            } else {
                // Log.i("@@@", "aspect ratio is correct: " +
                // width+"/"+height+"="+
                // mVideoWidth+"/"+mVideoHeight);
            }
        }
        // Log.i("@@@", "setting size: " + width + 'x' + height);
        setMeasuredDimension(width, height);
    }

     */
}
