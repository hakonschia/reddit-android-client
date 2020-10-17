package com.example.hakonsreader.views;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.activites.ImageActivity;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.misc.PhotoViewDoubleTapListener;
import com.github.chrisbanes.photoview.PhotoView;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

public class ContentImage extends PhotoView {
    private static final String TAG = "ContentImage";


    private RedditPost post;

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
     * Sets the post the content is for and updates the view
     *
     * <p>If the image is NSFW it is not shown (but a drawable to indicate the post is NSFW is)</p>
     *
     * @param post The post to set
     */
    public void setPost(RedditPost post) {
        this.post = post;

        this.updateView();
    }

    /**
     * Updates the view with the url from {@link ContentImage#post}
     */
    private void updateView() {
        // TODO create a preference for "Enable zooming of images while scrolling"
        this.setOnDoubleTapListener(doubleTapListener);

        // Dont show NSFW images until we are in fullscreen
        if (post.isNsfw()) {
            this.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_baseline_image_nsfw_24));

            // Set a border around to show what is clickable to open the window. Ideally the image would
            // match the screen width, might have to adjust the drawable width somehow to do that
            this.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.border));

            return;
        }

        RequestCreator c = Picasso.get()
                .load(post.getUrl())
                .placeholder(R.drawable.ic_baseline_wifi_tethering_150)
                .error(R.drawable.ic_baseline_wifi_tethering_150)
                // Scale so the image fits the width of the screen
                .resize(App.get().getScreenWidth(), 0);

        // Post is NSFW and user has chosen not to cache NSFW
        if (post.isNsfw() && App.get().dontCacheNSFW()) {
            // Don't store in cache and don't look in cache as this image will never be there
            c = c.networkPolicy(NetworkPolicy.NO_CACHE, NetworkPolicy.NO_STORE);
        }

        c.into(this);
    }

    /**
     * Listener for double taps. For single taps the image is opened in a fullscreen activity, and double
     * tap zooms (with the use of {@link PhotoViewDoubleTapListener}
     */
    private final GestureDetector.OnDoubleTapListener doubleTapListener = new GestureDetector.OnDoubleTapListener() {
        private final PhotoViewDoubleTapListener photoViewDoubleTapListener = new PhotoViewDoubleTapListener(getAttacher());

        // Open image in fullscreen on single tap
        @Override
        public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
            Intent intent = new Intent(getContext(), ImageActivity.class);
            intent.putExtra("imageUrl", post.getUrl());
            getContext().startActivity(intent);
            ((Activity)getContext()).overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent motionEvent) {
            return photoViewDoubleTapListener.onDoubleTap(motionEvent);
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent motionEvent) {
            return false;
        }
    };
}

