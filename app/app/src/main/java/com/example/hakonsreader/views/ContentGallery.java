package com.example.hakonsreader.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.example.hakonsreader.App;
import com.example.hakonsreader.api.model.Image;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.databinding.ContentGalleryBinding;
import com.example.hakonsreader.interfaces.LockableSlidr;

import java.util.List;
import java.util.Locale;


/**
 * Class for gallery posts. A gallery post is simply a collection of multiple images
 */
public class ContentGallery extends LinearLayout {
    private static final String TAG = "ContentGallery";

    // This file and ContentImage is really coupled together, should be fixed to not be so terrible


    private final ContentGalleryBinding binding;
    private RedditPost post;
    private List<Image> images;

    public ContentGallery(Context context) {
        this(context, null, 0, 0);
    }
    public ContentGallery(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }
    public ContentGallery(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }
    public ContentGallery(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        binding = ContentGalleryBinding.inflate(LayoutInflater.from(context), this, true);
    }

    public void setPost(RedditPost post) {
        this.post = post;
        this.updateView();
    }

    private void updateView() {
        images = post.getGalleryImages();

        // Find the largest height and width and set the layout to that
        int maxHeight = 0;
        int maxWidth = 0;

        for (Image image : images) {
            int height = image.getHeight();
            int width = image.getWidth();

            if (height > maxHeight) {
                maxHeight = height;
            }
            if (width > maxWidth) {
                maxWidth = width;
            }
        }

        // Should scale height to fit with the width as the image will be scaled later
        int screenWidth = App.get().getScreenWidth();
        float widthScale = screenWidth / (float)maxWidth;
        setLayoutParams(new ViewGroup.LayoutParams(screenWidth, (int) (maxHeight * widthScale)));

        ImageAdapter adapter = new ImageAdapter(getContext(), images);
        binding.galleryImages.setAdapter(adapter);

        // Keep all images alive to not have to reload them
        binding.galleryImages.setOffscreenPageLimit(images.size());

        // Add listener to change the text saying which item we're on
        binding.galleryImages.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                // Update the active text to show which image we are now on
                setActiveImageText(position + 1);

                lockSlidr(position != 0);
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                // Not implemented
            }
            @Override
            public void onPageScrollStateChanged(int state) {
                // Not implemented
            }

            /**
             * Lock or unlock a Slidr
             *
             * @param lock True to lock
             */
            private void lockSlidr(boolean lock) {
                Context context = getContext();

                // This might be bad? The "correct" way of doing it might be to add listeners
                // and be notified that way, but I don't want to add 1000 functions to add the listener
                // all the way up here from an activity
                if (context instanceof LockableSlidr) {
                    ((LockableSlidr)context).lock(lock);
                }
            }
        });
        // Set initial state
        setActiveImageText(1);

        // TODO get extras with which image is currently viewed for when post is opened
    }

    /**
     * Updates the text in {@link ContentGalleryBinding#activeImageText}
     *
     * @param activeImagePos The image position now active. Note this starts at 1, not 0
     */
    private void setActiveImageText(int activeImagePos) {
        binding.activeImageText.setText(String.format(Locale.getDefault(), "%d / %d", activeImagePos, images.size()));
    }


    /**
     * The pager adapter responsible for handling the images in the post
     */
    private class ImageAdapter extends PagerAdapter {
        Context context;
        List<Image> images;

        public ImageAdapter(Context context, List<Image> images) {
            this.context = context;
            this.images = images;
        }

        @Override
        public int getCount() {
            return images.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, final int position) {
            Image image = images.get(position);

            // Use ContentImage as that already has listeners, NSFW caching etc already
            ContentImage contentImage = new ContentImage(context);
            contentImage.setWithImageUrl(post, image.getUrl());

            container.addView(contentImage);

            return contentImage;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((ImageView) object);
        }
    }
}
