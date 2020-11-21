package com.example.hakonsreader.views;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.api.model.Image;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.databinding.ContentImageBinding;
import com.example.hakonsreader.enums.ShowNsfwPreview;
import com.example.hakonsreader.views.util.ClickHandler;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import java.util.List;

/**
 * Content view for Reddit images posts.
 *
 * <p>Images for NSFW posts are automatically blurred or not shown according to the setting from {@link App#showNsfwPreview()}</p>
 */
public class ContentImage extends Content {
    private static final String TAG = "ContentImage";

    private final ContentImageBinding binding;
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
        binding = ContentImageBinding.inflate(LayoutInflater.from(context), this, true);
    }


    /**
     * Sets the post with a different image URL than the one retrieved with {@link RedditPost#getUrl()}.
     * This can be used to create a PhotoView with a custom double tap listener
     * that opens the image in fullscreen when single taped, and also respects the users NSFW caching choice
     *
     * @param imageUrl The image URL to set
     */
    public void setWithImageUrl(RedditPost post, String imageUrl) {
        this.imageUrl = imageUrl;
        super.setRedditPost(post);
    }

    /**
     * Sets the {@link Callback} to use for when the post is an image and the image has finished loading
     *
     * <p>This must be set before {@link Post#setRedditPost(RedditPost)}</p>
     *
     * @param imageLoadedCallback The callback for when images are finished loading
     */
    public void setImageLoadedCallback(Callback imageLoadedCallback) {
        this.imageLoadedCallback = imageLoadedCallback;
    }

    /**
     * Updates the view with the url from {@link ContentImage#post}
     */
    @Override
    protected void updateView() {
        int screenWidth = App.get().getScreenWidth();

        // Set with setPost() not setWithImageUrl()
        if (imageUrl == null) {
            imageUrl = redditPost.getUrl();

            List<Image> images = redditPost.getPreviewImages();

            // This should be improved and is a pretty poor way of doing it, but this will reduce some
            // unnecessary loading as it will get some lower resolution images (it will be scaled down to
            // the same size later by Picasso, so it won't give loss of image quality)
            for (int i = 0; i < images.size(); i++) {
                Image image = images.get(i);
                if (image.getWidth() == screenWidth) {
                    imageUrl = image.getUrl();
                    break;
                }
            }
        }

        setOnClickListener(v -> ClickHandler.openImageInFullscreen(binding.image, imageUrl));

        String obfuscatedUrl = null;
        int noImageId = -1;
        if (redditPost.isNsfw() ) {
            ShowNsfwPreview show = App.get().showNsfwPreview();

            switch (show) {
                case NORMAL:
                    // Do nothing, load imageUrl as is
                    break;

                case BLURRED:
                    obfuscatedUrl = getObfuscatedUrl();
                    // If we don't have a URL to show then show the NSFW drawable instead as a fallback
                    if (obfuscatedUrl == null) {
                        noImageId = R.drawable.ic_baseline_image_nsfw_200;
                    }
                    break;

                case NO_IMAGE:
                    noImageId = R.drawable.ic_baseline_image_nsfw_200;
                    break;
            }
        } else if (redditPost.isSpoiler()) {
            // Always blur spoilers (if possible)

            obfuscatedUrl = getObfuscatedUrl();
            // If we don't have a URL to show then show the NSFW drawable instead as a fallback
            if (obfuscatedUrl == null) {
                noImageId = R.drawable.ic_baseline_image_nsfw_200;
            }
        }

        // TODO this (I think) has caused crashes (at least on Samsung devices) because the canvas is trying
        //  to draw a bitmap too large. It's hard to reproduce since it only seems to happen some times
        //  and when it happens it might not even happen on the same post (and opening the post in the post itself
        //  instead of just when scrolling works
        //  Exception message: java.lang.RuntimeException: Canvas: trying to draw too large(107867520bytes) bitmap.
        //  Since it's hard to reproduce I'm not even sure if wrapping this section in a try catch works or not
        //  The issue at least happens with extremely large images (although it didn't happen with large images the first time)

        try {
            RequestCreator creator;
            // No image to load, set image drawable directly
            if (noImageId != -1) {
                binding.image.setImageDrawable(ContextCompat.getDrawable(getContext(), noImageId));
            } else {
                // If we have an obfuscated image, load that here instead
                creator = Picasso.get().load(obfuscatedUrl != null ? obfuscatedUrl : imageUrl)
                        .placeholder(R.drawable.ic_baseline_wifi_tethering_150)
                        .error(R.drawable.ic_baseline_wifi_tethering_150)
                        // Scale so the image fits the width of the screen
                        .resize(App.get().getScreenWidth(), 0);

                // Post is NSFW and user has chosen not to cache NSFW
                // TODO this won't work as the actual image is only loaded in fullscreen, what is not cached here
                //  is the obfuscated image, need to pass "dontCache" to ImageActivity
                if (redditPost.isNsfw() && App.get().dontCacheNSFW()) {
                    // Don't store in cache and don't look in cache as this image will never be there
                    creator = creator.networkPolicy(NetworkPolicy.NO_CACHE, NetworkPolicy.NO_STORE);
                }
                creator.into(binding.image, imageLoadedCallback);
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            Log.d(TAG, "updateView:\n\n\n\n--------------------------- ERROR LOADING IMAGE" +
                    "\n\n " + redditPost.getSubreddit() + ", " + redditPost.getTitle() + " ---------------------------\n\n\n\n");
        }
    }


    /**
     * Retrieves the obfuscated image URL to use
     *
     * @return An URL pointing to an image, or {@code null} of no obfuscated images were found
     */
    private String getObfuscatedUrl() {
        List<Image> obfuscatedPreviews = redditPost.getObfuscatedPreviewImages();

        if (obfuscatedPreviews != null && !obfuscatedPreviews.isEmpty()) {
            // Obfuscated previews that are high res are still fairly showing sometimes, so
            // get the lowest quality one as that will not be very easy to tell what it is
            return obfuscatedPreviews.get(0).getUrl();
        }

        return null;
    }
}

