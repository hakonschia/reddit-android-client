package com.example.hakonsreader.views;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.VideoView;

import com.example.hakonsreader.api.model.RedditPost;

public class PostContentVideo extends VideoView {
    private RedditPost post;

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
    public PostContentVideo(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }


    private void updateView() {

        // TODO width should be maximum this to not go outside of the screen
        this.setLayoutParams(new ViewGroup.LayoutParams(post.getVideoWidth(), post.getVideoHeight()));
        this.setVideoURI(Uri.parse(post.getVideoUrl()));
    }
}
