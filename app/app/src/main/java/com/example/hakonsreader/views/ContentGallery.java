package com.example.hakonsreader.views;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.example.hakonsreader.App;
import com.example.hakonsreader.api.model.Image;
import com.example.hakonsreader.databinding.ContentGalleryBinding;
import com.example.hakonsreader.interfaces.LockableSlidr;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;


/**
 * Class for gallery posts. A gallery post is simply a collection of multiple images
 */
public class ContentGallery extends Content {
    private static final String TAG = "ContentGallery";


    /**
     * The key for extras in {@link ContentGallery#getExtras()} that tells which image is currently active.
     */
    public static final String EXTRAS_ACTIVE_IMAGE = "activeImage";

    // This file and ContentImage is really coupled together, should be fixed to not be so terrible


    private final ContentGalleryBinding binding;
    private List<Image> images;
    private List<ContentGalleryImage> galleryViews = new ArrayList<>();

    /**
     * The current Slidr lock this view has called. This should be checked to make sure duplicate calls
     * to lock a Slidr isn't done
     */
    private boolean slidrLocked = false;


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


    @Override
    protected void updateView() {
        images = redditPost.getGalleryImages();

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
        int screenWidth = App.Companion.get().getScreenWidth();
        float widthScale = screenWidth / (float)maxWidth;
        setLayoutParams(new ViewGroup.LayoutParams(screenWidth, (int) (maxHeight * widthScale)));

        ImageAdapter adapter = new ImageAdapter(getContext(), images);
        binding.galleryImages.setAdapter(adapter);

        // Keep a maximum of 5 items at a time, or 2 when data saving is enabled. This should probably
        // be enough to make large galleries not all load at once which potentially wastes data, and
        // at the same time not have to load items when going through the gallery (unless data saving is on)
        int offscreenLimit = App.Companion.get().dataSavingEnabled() ? 2 : 5;
        binding.galleryImages.setOffscreenPageLimit(offscreenLimit);

        binding.galleryImages.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            ContentGalleryImage currentView;

            @Override
            public void onPageSelected(int position) {
                setActiveImageText(position);

                // Make sure the slidr is locked when not on the first item, so that swiping right will
                // swipe on the gallery, not remove the activity
                lockSlidr(position != 0);

                // Unselect the previous and set new and select that
                if (currentView != null) {
                    currentView.viewUnselected();
                }

                currentView = findViewWithTag(position);
                if (currentView != null) {
                    currentView.viewSelected();
                }
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                // Not implemented
            }
            @Override
            public void onPageScrollStateChanged(int state) {
                // Not implemented
            }
        });

        // Set initial state
        setActiveImageText(0);
    }

    /**
     * Updates the text in {@link ContentGalleryBinding#activeImageText}
     *
     * @param activeImagePos The image position now active.
     */
    private void setActiveImageText(int activeImagePos) {
        // Imgur albums are also handled as galleries, and they might only contain one image, so make it
        // look like only one image by removing the text
        if (images.size() == 1) {
            binding.activeImageText.setVisibility(GONE);
        } else {
            binding.activeImageText.setText(String.format(Locale.getDefault(), "%d / %d", activeImagePos + 1, images.size()));
        }
    }

    /**
     * Lock or unlock a Slidr.
     *
     * @param lock True to lock, false to unlock
     */
    private void lockSlidr(boolean lock) {
        // Return to avoid duplicate calls on the same type of lock
        if (lock == slidrLocked) {
            return;
        }

        slidrLocked = lock;
        Context context = getContext();

        // This might be bad? The "correct" way of doing it might be to add listeners
        // and be notified that way, but I don't want to add 1000 functions to add the listener
        // all the way up here from an activity
        if (context instanceof LockableSlidr) {
            ((LockableSlidr)context).lock(lock);
        }
    }

    @NonNull
    @Override
    public Bundle getExtras() {
        Bundle extras = super.getExtras();

        extras.putInt(EXTRAS_ACTIVE_IMAGE, binding.galleryImages.getCurrentItem());
        return extras;
    }

    @Override
    public void setExtras(@NonNull Bundle extras) {
        super.setExtras(extras);

        int activeImage = extras.getInt(EXTRAS_ACTIVE_IMAGE, images.size());
        binding.galleryImages.setCurrentItem(activeImage, false);
    }

    @Override
    public void viewSelected() {
        super.viewSelected();

        // Send a request to lock the slidr if the view is selected when not on the first image
        if (binding.galleryImages.getCurrentItem() != 0) {
            lockSlidr(true);
        }
    }

    @Override
    public void viewUnselected() {
        super.viewUnselected();

        // Send a request to unlock the slidr if the view is unselected when not on the first image
        if (binding.galleryImages.getCurrentItem() != 0) {
            lockSlidr(false);
        }
    }

    /**
     * Releases all views in the gallery
     */
    public void release() {
        galleryViews.forEach(ContentGalleryImage::destroy);
        galleryViews.clear();
    }


    /**
     * The pager adapter responsible for handling the images in the post
     */
    public class ImageAdapter extends PagerAdapter {
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

        @NotNull
        @Override
        public Object instantiateItem(@NotNull ViewGroup container, final int position) {
            //Image image = images.get(position % images.size());
            Image image = images.get(position);

            ContentGalleryImage view = new ContentGalleryImage(context);
            view.setPost(redditPost);
            view.setImage(image);
            view.setTag(position);

            container.addView(view);
            galleryViews.add(position, view);

            return view;
        }

        @Override
        public boolean isViewFromObject(@NotNull View view, @NotNull Object object) {
            return view == object;
        }

        @Override
        public void destroyItem(@NotNull ViewGroup container, int position, @NotNull Object object) {
            ContentGalleryImage view = (ContentGalleryImage) object;
            galleryViews.remove(view);
            view.destroy();
            container.removeView(view);
        }
    }
}
