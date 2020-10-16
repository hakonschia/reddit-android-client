package com.example.hakonsreader.activites;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hakonsreader.R;
import com.example.hakonsreader.api.utils.LinkUtils;
import com.github.chrisbanes.photoview.PhotoView;
import com.github.chrisbanes.photoview.PhotoViewAttacher;
import com.jsibbold.zoomage.ZoomageView;
import com.r0adkll.slidr.Slidr;
import com.r0adkll.slidr.model.SlidrConfig;
import com.r0adkll.slidr.model.SlidrPosition;
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

            image.setOnDoubleTapListener(new GestureDetector.OnDoubleTapListener() {
                @Override
                public boolean onDoubleTap(MotionEvent motionEvent) {

                    // If we are zoomed in at all on double taps then go back to original size
                    // otherwise zoom in. Default for PhotoView has two zoom states, this removes the
                    // intermediate step (because I don't like that)
                    // Comparing floats like this is probably bad? Doesn't seem to matter though
                    if (attacher.getScale() > 1f) {
                        attacher.setScale(1f, true);
                    } else {
                        attacher.setScale(3f, motionEvent.getX(), motionEvent.getY(), true);
                    }

                    return true;
                }

                @Override
                public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
                    // Not implemented
                    return false;
                }
                @Override
                public boolean onDoubleTapEvent(MotionEvent motionEvent) {
                    // Not implemented
                    return false;
                }
            });

            imageUrl = LinkUtils.convertToDirectUrl(imageUrl);

            Picasso.get().load(imageUrl).into(image);
        } else {
            finish();
        }

        Slidr.attach(this);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}
