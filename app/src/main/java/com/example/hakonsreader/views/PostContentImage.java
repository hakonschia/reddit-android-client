package com.example.hakonsreader.views;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import com.example.hakonsreader.MainActivity;
import com.example.hakonsreader.R;
import com.example.hakonsreader.api.model.RedditPost;
import com.squareup.picasso.Picasso;

public class PostContentImage extends androidx.appcompat.widget.AppCompatImageView {

    private RedditPost post;

    public PostContentImage(Context context, RedditPost post) {
        super(context);

        this.post = post;
        this.updateView();
    }

    public PostContentImage(Context context) {
        super(context);
    }
    public PostContentImage(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }
    public PostContentImage(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    /**
     * Updates the view with the url from {@link PostContentImage#post}
     */
    private void updateView() {
        // TODO when clicked open the image so you can ZOOOOOM
        Picasso.get()
                .load(post.getUrl())
                .placeholder(R.drawable.ic_baseline_wifi_tethering_150)
                // Scale so the image fits the width of the screen
                .resize(MainActivity.SCREEN_WIDTH, 0)
                .into(this);
    }
}
