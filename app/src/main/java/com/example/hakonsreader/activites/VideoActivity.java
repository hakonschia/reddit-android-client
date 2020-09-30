package com.example.hakonsreader.activites;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hakonsreader.R;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.views.ContentVideo;
import com.example.hakonsreader.views.Post;
import com.google.gson.Gson;
import com.jsibbold.zoomage.ZoomageView;
import com.squareup.picasso.Picasso;

/**
 * Activity to display a zoomable image taking the entire screen
 */
public class VideoActivity extends AppCompatActivity {
    private static final String TAG = "ImageActivity";

    /**
     * The key used for the post the video belongs to
     */
    public static final String POST = "post";

    private ContentVideo content;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        Bundle data = getIntent().getExtras();
        if (data != null) {
            Gson gson = new Gson();
            RedditPost redditPost = gson.fromJson(getIntent().getExtras().getString(POST), RedditPost.class);

            Bundle extras = getIntent().getExtras().getBundle("extras");

            Post post = new Post(this);
            post.setPostData(redditPost);
            post.resumeVideoPost(extras);

            FrameLayout video = findViewById(R.id.video);

            // Retrieve and extract only the content view
            FrameLayout contentLayout = post.getContentLayout();
            content = (ContentVideo) contentLayout.getChildAt(0);
            contentLayout.removeView(content);

            video.addView(content);
        }
    }

    @Override
    public void finish() {
        super.finish();
        content.release();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}