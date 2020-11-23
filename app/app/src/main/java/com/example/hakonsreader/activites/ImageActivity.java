package com.example.hakonsreader.activites;

import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.api.utils.LinkUtils;
import com.example.hakonsreader.databinding.ActivityImageBinding;
import com.example.hakonsreader.views.listeners.PhotoViewDoubleTapListener;
import com.github.chrisbanes.photoview.PhotoView;
import com.github.chrisbanes.photoview.PhotoViewAttacher;
import com.r0adkll.slidr.Slidr;
import com.r0adkll.slidr.model.SlidrConfig;
import com.r0adkll.slidr.model.SlidrInterface;
import com.squareup.picasso.Callback;
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

    private SlidrInterface slidrInterface;
    private ActivityImageBinding binding;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityImageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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
        slidrInterface = Slidr.attach(this, config);

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

            PhotoViewAttacher attacher = binding.image.getAttacher();
            attacher.setMaximumScale(7f);

            binding.image.setOnDoubleTapListener(new PhotoViewDoubleTapListener(attacher, slidrInterface));

            imageUrl = LinkUtils.convertToDirectUrl(imageUrl);

            binding.loadingIcon.onCountChange(true);
            Picasso
                    .get()
                    .load(imageUrl)
                    .resize(App.get().getScreenWidth(), 0)
                    .into(binding.image, new Callback() {
                        @Override
                        public void onSuccess() {
                            binding.loadingIcon.onCountChange(false);
                        }
                        @Override
                        public void onError(Exception e) {
                            binding.loadingIcon.onCountChange(false);
                            e.printStackTrace();
                            new AlertDialog.Builder(ImageActivity.this)
                                    .setTitle(R.string.imageLoadFailedDialogTitle)
                                    .setMessage(R.string.imageLoadFailedDialogContent)
                                    .setOnDismissListener(dialog -> finish())
                                    .show();
                        }
                    });
        } else {
            finish();
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_out, R.anim.fade_out);
    }
}
