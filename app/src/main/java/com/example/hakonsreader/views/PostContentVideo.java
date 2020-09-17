package com.example.hakonsreader.views;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.widget.VideoView;

import com.example.hakonsreader.api.model.RedditPost;

public class PostContentVideo extends VideoView {
    private RedditPost post;
    private int videoWidth;
    private int videoHeight;

    public PostContentVideo(Context context, RedditPost post) {
        super(context);

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
        this.setVideoSize(post.getVideoWidth(), post.getVideoHeight());
        this.setVideoURI(Uri.parse(post.getVideoUrl()));
        this.start();
    }


    /* Shamelessly stolen from https://stackoverflow.com/a/19075245 */
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
}
