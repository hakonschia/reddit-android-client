package com.example.hakonsreader.activites;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hakonsreader.R;
import com.jsibbold.zoomage.ZoomageView;
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
            String imageUrl = data.getString(IMAGE_URL);
            ZoomageView image = findViewById(R.id.image);

            if (imageUrl.matches("^https://imgur.com/[A-Za-z0-9]{5,7}$")) {
                imageUrl += ".png";
            }

            Picasso.get().load(imageUrl).into(image);
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}
