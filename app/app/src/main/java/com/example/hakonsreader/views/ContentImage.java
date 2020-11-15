package com.example.hakonsreader.views;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.api.model.Image;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.views.util.ClickHandler;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import java.util.List;

public class ContentImage extends androidx.appcompat.widget.AppCompatImageView {
    private static final String TAG = "ContentImage";

    private RedditPost post;
    private String imageUrl;
    private Callback imageLoadedCallback;

    public ContentImage(Context context) {
        this(context, null, 0);
    }
    public ContentImage(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
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
     * Sets the post with a different image URL than the one retrieved with {@link RedditPost#getUrl()}.
     * This can be used to create a PhotoView with a custom double tap listener
     * that opens the image in fullscreen when single taped, and also respects the users NSFW caching choice
     *
     * @param imageUrl The image URL to set
     */
    public void setWithImageUrl(RedditPost post, String imageUrl) {
        this.post = post;
        this.imageUrl = imageUrl;
        this.updateView();
    }

    /**
     * Sets the {@link Callback} to use for when the post is an image and the image has finished loading
     *
     * <p>This must be set before {@link Post#setPostData(RedditPost)}</p>
     *
     * @param imageLoadedCallback The callback for when images are finished loading
     */
    public void setImageLoadedCallback(Callback imageLoadedCallback) {
        this.imageLoadedCallback = imageLoadedCallback;
    }

    /**
     * Updates the view with the url from {@link ContentImage#post}
     */
    private void updateView() {
        int screenWidth = App.get().getScreenWidth();

        // Set with setPost() not setWithImageUrl()
        if (imageUrl == null) {
            imageUrl = post.getUrl();

            // This should be improved and is a pretty poor way of doing it, but this will reduce some
            // unnecessary loading as it will get some lower resolution images (it will be scaled down to
            // the same size later by Picasso, so it won't give loss of image quality)
            List<Image> images = post.getPreviewImages();
            for (Image image : images) {
                if (image.getWidth() == screenWidth) {
                    imageUrl = image.getUrl();
                    break;
                }
            }
        }

        setOnClickListener(v -> ClickHandler.openImageInFullscreen(this, imageUrl));

        // Dont show NSFW images until we are in fullscreen
        if (post.isNsfw()) {
            this.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_baseline_image_nsfw_200));

            // Set a border around to show what is clickable to open the window. Ideally the image would
            // match the screen width, might have to adjust the drawable width somehow to do that
            this.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.border));

            return;
        }

        // TODO this (I think) has caused crashes (at least on Samsung devices) because the canvas is trying
        //  to draw a bitmap too large. It's hard to reproduce since it only seems to happen some times
        //  and when it happens it might not even happen on the same post (and opening the post in the post itself
        //  instead of just when scrolling works
        //  Exception message: java.lang.RuntimeException: Canvas: trying to draw too large(107867520bytes) bitmap.
        //  Since it's hard to reproduce I'm not even sure if wrapping this section in a try catch works or not
        //  The issue at least happens with extremely large images (although it didn't happen with large images the first time)

        try {
            RequestCreator c = Picasso.get()
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_baseline_wifi_tethering_150)
                    .error(R.drawable.ic_baseline_wifi_tethering_150)
                    // Scale so the image fits the width of the screen
                    .resize(App.get().getScreenWidth(), 0);

            // Post is NSFW and user has chosen not to cache NSFW
            if (post.isNsfw() && App.get().dontCacheNSFW()) {
                // Don't store in cache and don't look in cache as this image will never be there
                c = c.networkPolicy(NetworkPolicy.NO_CACHE, NetworkPolicy.NO_STORE);
            }

            Log.d(TAG, "updateView: " + imageLoadedCallback);

            c.into(this, imageLoadedCallback);
        } catch (RuntimeException e) {
            e.printStackTrace();
            Log.d(TAG, "updateView:\n\n\n\n--------------------------- ERROR LOADING IMAGE" +
                    "\n\n " + post.getSubreddit() + ", " + post.getTitle() + " ---------------------------\n\n\n\n");
        }

    }
}

