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
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.widget.ViewPager2;

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
    private final List<ContentGalleryImage> galleryViews = new ArrayList<>();

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

        Adapter adapter = new Adapter(images);
        binding.galleryImages.setAdapter(adapter);

        // Keep a maximum of 5 items at a time, or 2 when data saving is enabled. This should probably
        // be enough to make large galleries not all load at once which potentially wastes data, and
        // at the same time not have to load items when going through the gallery (unless data saving is on)
        int offscreenLimit = App.Companion.get().dataSavingEnabled() ? 2 : 5;
        binding.galleryImages.setOffscreenPageLimit(offscreenLimit);

        binding.galleryImages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            ContentGalleryImage currentView = null;

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                setActiveImageText(position);

                // Make sure the slidr is locked when not on the first item, so that swiping right will
                // swipe on the gallery, not remove the activity (this would probably be wrong for RTL layouts?)
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


    private class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {
        private final List<Image> images;

        public Adapter(List<Image> images) {
            this.images = images;
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            // Destroy previous image
            holder.image.destroy();

            Image image = images.get(position);
            holder.image.setImage(image);
            holder.image.setTag(position);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ContentGalleryImage view = new ContentGalleryImage(parent.getContext());
            view.setPost(redditPost);
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            view.setLayoutParams(params);

            galleryViews.add(view);
            return new ViewHolder(view);
        }

        @Override
        public int getItemCount() {
            return images.size();
        }

        private class ViewHolder extends RecyclerView.ViewHolder {
            private final ContentGalleryImage image;
            public ViewHolder(ContentGalleryImage image) {
                super(image);
                this.image = image;
            }
        }
    }
}
