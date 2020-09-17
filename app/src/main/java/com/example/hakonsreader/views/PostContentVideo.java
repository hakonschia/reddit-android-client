package com.example.hakonsreader.views;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.constants.NetworkConstants;
import com.example.hakonsreader.databinding.LayoutPostContentVideoBinding;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;

public class PostContentVideo extends LinearLayout {
    private static final String TAG = "PostContentVideo";

    private LayoutPostContentVideoBinding binding;

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
        // TODO width should be maximum this to not go outside of the screen
        //this.setVideoSize(post.getVideoWidth(), post.getVideoHeight());
        // this.setVideoURI(Uri.parse(post.getVideoUrl()));
        // this.start();

        setLayoutParams(new ViewGroup.LayoutParams(post.getVideoWidth(), post.getVideoHeight()));

        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
        DefaultHttpDataSourceFactory dataSourceFactory = new DefaultHttpDataSourceFactory(NetworkConstants.USER_AGENT);

        MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory, extractorsFactory)
                .createMediaSource(Uri.parse(post.getVideoUrl()));

        SimpleExoPlayer exoPlayer = new SimpleExoPlayer.Builder(getContext())
                .setBandwidthMeter(new DefaultBandwidthMeter.Builder(getContext()).build())
                .setTrackSelector(new DefaultTrackSelector(getContext(), new AdaptiveTrackSelection.Factory()))
                .build();
        exoPlayer.prepare(mediaSource);
        exoPlayer.setPlayWhenReady(true);

        this.binding.player.setPlayer(exoPlayer);
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
