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
import com.example.hakonsreader.misc.PostImageVariants;
import com.example.hakonsreader.misc.UtilKtKt;
import com.example.hakonsreader.views.util.ClickHandlerKt;
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
        PostImageVariants variants = UtilKtKt.getImageVariantsForRedditPost(redditPost);
        String url;
        if (imageUrl != null) {
            url = imageUrl;
        } else {
            if (redditPost.isNsfw()) {
                url = variants.getNsfw();
            } else if (redditPost.isSpoiler()) {
                url = variants.getSpoiler();
            } else {
                url = variants.getNormal();
                if (url == null) {

                    // Use the post URL as a fallback (since this is an image it will point to an image)
                    url = redditPost.getUrl();
                }
            }
        }

        // TODO this (I think) has caused crashes (at least on Samsung devices) because the canvas is trying
        //  to draw a bitmap too large. It's hard to reproduce since it only seems to happen some times
        //  and when it happens it might not even happen on the same post (and opening the post in the post itself
        //  instead of just when scrolling works
        //  Exception message: java.lang.RuntimeException: Canvas: trying to draw too large(107867520bytes) bitmap.
        //  Since it's hard to reproduce I'm not even sure if wrapping this section in a try catch works or not
        //  The issue at least happens with extremely large images (although it didn't happen with large images the first time)
        //  https://www.reddit.com/r/dataisbeautiful/comments/kji3wx/oc_2020_electoral_map_if_only_voted_breakdown_by/

        try {
            // No image to load, set image drawable directly
            if (url == null) {
                binding.image.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_image_not_supported_200dp));
            } else {
                // When opening the image we always want to open the normal
                setOnClickListener(v -> ClickHandlerKt.openImageInFullscreen(binding.image, variants.getNormal()));

                // If we have an obfuscated image, load that here instead
                RequestCreator creator = Picasso.get()
                        .load(url)
                        .placeholder(R.drawable.ic_wifi_tethering_150dp)
                        .error(R.drawable.ic_wifi_tethering_150dp)
                        // Scale so the image fits the width of the screen
                        .resize(App.Companion.get().getScreenWidth(), 0);

                // Post is NSFW and user has chosen not to cache NSFW
                // TODO this won't work as the actual image is only loaded in fullscreen, what is not cached here
                //  is the obfuscated image, need to pass "dontCache" to ImageActivity
                if (redditPost.isNsfw() && App.Companion.get().dontCacheNSFW()) {
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
}
