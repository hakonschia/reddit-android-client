package com.example.hakonsreader.activites;

import android.os.Build;
import android.os.Bundle;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.views.Content;
import com.example.hakonsreader.views.ContentVideo;
import com.google.gson.Gson;
import com.r0adkll.slidr.Slidr;
import com.r0adkll.slidr.model.SlidrConfig;

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

            Bundle extras;

            // Restore from the saved state if possible
            if (savedInstanceState != null) {
                extras = savedInstanceState.getBundle(Content.EXTRAS);
            } else {
                extras = data.getBundle(Content.EXTRAS);
            }

            if (!App.get().muteVideoByDefaultInFullscreen()) {
                extras.putBoolean(ContentVideo.EXTRA_VOLUME, true);
            }

            content = new ContentVideo(this);
            content.setRedditPost(redditPost);
            content.setExtras(extras);
            content.fitScreen();

            FrameLayout video = findViewById(R.id.video);
            video.addView(content);
        } else {
            finish();
        }

        int color = getColor(R.color.imageVideoActivityBackground);
        int alpha = (color >> 24) & 0xFF;
        float alphaPercentage = (float)alpha / 0xFF;

        SlidrConfig config = App.get().getVideoAndImageSlidrConfig()
                // To keep the background the same the entire way the alpha is always the same
                // Otherwise the background of the activity slides with, which looks weird
                .scrimStartAlpha(alphaPercentage)
                .scrimEndAlpha(alphaPercentage)
                .scrimColor(color)
                .build();

        Slidr.attach(this, config);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Store the new extras so that we use that to update the video progress instead of
        // the one passed when the activity was started
        outState.putBundle(Content.EXTRAS, content.getExtras());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        content.release();
    }

    @Override
    public void finish() {
        super.finish();
        content.release();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}