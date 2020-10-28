package com.example.hakonsreader.activites;

import android.os.Build;
import android.os.Bundle;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.views.ContentVideo;
import com.example.hakonsreader.views.Post;
import com.google.gson.Gson;
import com.r0adkll.slidr.Slidr;
import com.r0adkll.slidr.model.SlidrConfig;
import com.r0adkll.slidr.model.SlidrPosition;

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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                final WindowInsetsController controller = getWindow().getInsetsController();

                if (controller != null)
                    controller.hide(WindowInsets.Type.statusBars());
            }
            else {
                //noinspection deprecation
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }

            Gson gson = new Gson();
            RedditPost redditPost = gson.fromJson(getIntent().getExtras().getString(POST), RedditPost.class);

            Bundle extras = getIntent().getExtras().getBundle("extras");

            if (!App.get().muteVideoByDefaultInFullscreen()) {
                extras.putBoolean(ContentVideo.EXTRA_VOLUME, true);
            }

            content = new ContentVideo(this);
            content.setPost(redditPost);
            content.setExtras(extras);
            content.fitScreen();

            FrameLayout video = findViewById(R.id.video);
            video.addView(content);
        } else {
            finish();
        }

        Slidr.attach(this, App.get().getVideoAndImageSlidrConfig());
    }

    @Override
    public void finish() {
        super.finish();
        content.release();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}