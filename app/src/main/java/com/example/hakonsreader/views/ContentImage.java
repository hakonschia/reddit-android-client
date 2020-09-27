package com.example.hakonsreader.views;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.activites.ImageActivity;
import com.example.hakonsreader.api.model.RedditPost;
import com.squareup.picasso.Picasso;

public class ContentImage extends androidx.appcompat.widget.AppCompatImageView {

    private RedditPost post;

    public ContentImage(Context context, RedditPost post) {
        super(context);

        this.post = post;
        this.updateView();
    }

    public ContentImage(Context context) {
        super(context);
    }
    public ContentImage(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }
    public ContentImage(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    /**
     * Updates the view with the url from {@link ContentImage#post}
     */
    private void updateView() {
        Picasso.get()
                .load(post.getURL())
                .placeholder(R.drawable.ic_baseline_wifi_tethering_150)
                // Scale so the image fits the width of the screen
                .resize(App.getScreenWidth(), 0)
                .into(this);

        // Open image when clicked
        this.setOnClickListener(view -> {
            Intent intent = new Intent(getContext(), ImageActivity.class);
            intent.putExtra("imageUrl", post.getURL());
            getContext().startActivity(intent);
            ((Activity)getContext()).overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });
    }
}

