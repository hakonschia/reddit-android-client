package com.example.hakonsreader.activites;

import android.os.Build;
import android.os.Bundle;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.api.utils.LinkUtils;
import com.example.hakonsreader.views.listeners.PhotoViewDoubleTapListener;
import com.github.chrisbanes.photoview.PhotoView;
import com.github.chrisbanes.photoview.PhotoViewAttacher;
import com.r0adkll.slidr.Slidr;
import com.r0adkll.slidr.model.SlidrConfig;
import com.squareup.picasso.Picasso;

/**
 * Activity to display a zoomable image taking the entire screen
 */
public class ImageActivity extends AppCompatActivity {
    private static final String TAG = "ImageActivity";

    /**
     * The key used for the URL of the image to display
     */
    public static final String IMAGE_URL = "imageUrl";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

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

            String imageUrl = data.getString(IMAGE_URL);

            PhotoView image = findViewById(R.id.image);
            PhotoViewAttacher attacher = image.getAttacher();
            attacher.setMaximumScale(7f);

            image.setOnDoubleTapListener(new PhotoViewDoubleTapListener(attacher));

            imageUrl = LinkUtils.convertToDirectUrl(imageUrl);

            // This needs to be resized or else we will get "Canvas: Trying to draw too large bitmap"
            // TODO since we're resizing we can probably try to get a closer match in images from the
            //  preview images (also for ContentImage)
            Picasso
                    .get()
                    .load(imageUrl)
                    .resize(App.get().getScreenWidth(), 0)
                    .into(image);
        } else {
            finish();
        }

        // The color retrieved is "0x<alpha><red><green><blue>" (each one byte, 8 bits)
        int color = getColor(R.color.imageVideoActivityBackground);

        // Offset 3 bytes and get the value there to find the alpha
        int alpha = (color >> 8 * 3) & 0xFF;
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
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_out, R.anim.fade_out);
    }
}
