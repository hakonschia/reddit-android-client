package com.example.hakonsreader.activites;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hakonsreader.R;
import com.jsibbold.zoomage.ZoomageView;
import com.squareup.picasso.Picasso;

public class ImageActivity extends AppCompatActivity {
    private static final String TAG = "ImageActivity";
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        Bundle data = getIntent().getExtras();
        if (data != null) {
            String imageUrl = data.getString("imageUrl");
            ZoomageView image = findViewById(R.id.image);

            Log.d(TAG, "onCreate: lmao " + imageUrl);
            Picasso.get().load(imageUrl).into(image);
        }
    }

    @Override
    public void finish() {
        super.finish();

        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}
