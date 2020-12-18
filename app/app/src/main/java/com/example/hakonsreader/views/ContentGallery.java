package com.example.hakonsreader.views;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.example.hakonsreader.App;
import com.example.hakonsreader.api.model.Image;
import com.example.hakonsreader.databinding.ContentGalleryBinding;
import com.example.hakonsreader.interfaces.LockableSlidr;

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
        // TODO galleries can contain GIFs from reddit (and imgur)
        //  https://www.reddit.com/r/LadyBoners/comments/k4id7n/presenting_mr_misha_collins/
        // For now, if the url isn't found (it's a gif) then ignore the image
        images = redditPost.getGalleryImages().stream()
                .filter(image -> image.getUrl() != null)
                .collect(Collectors.toList());

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

        int imagesSize = images.size();

        ImageAdapter adapter = new ImageAdapter(getContext(), images);
        binding.galleryImages.setAdapter(adapter);

        // Keep all images alive to not have to reload them
        binding.galleryImages.setOffscreenPageLimit(imagesSize);

        binding.galleryImages.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                setActiveImageText(position);

                // Make sure the slidr is locked when not on the first item, so that swiping right will
                // swipe on the gallery, not remove the activity
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
        });

        // If I can manage to lock the slidr only when the gallery is touched, and unlocked when not touched
        // then I can add back the infinite paging. If I can't manage that then having a gallery will lock
        // the slidr completely
        /*
        // The ViewPager will be an infinite scroller. The adapter returns a size 3 times images.size()
        // so set the current item to the middle
        //      Here
        //        |
        // 0 1    0 1    0 1
        // Since imgur albums are loaded here, and they can be only 1 image, don't add scrolling functionality to it
        binding.galleryImages.setCurrentItem(imagesSize == 1 ? 0 : images.size());

        // Add listener to change the text saying which item we're on
        binding.galleryImages.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            int currentPos;

            @Override
            public void onPageSelected(int position) {
                currentPos = position;

                // Update the active text to show which image we are now on
                setActiveImageText(position % images.size());
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager.SCROLL_STATE_IDLE) {
                    // Here       Go here    Or here
                    //   |          |          |
                    // 0 1        0 1        0 1
                    if (currentPos == images.size() - 1 || currentPos == adapter.getCount() - 1) {
                        binding.galleryImages.setCurrentItem(images.size() * 2 - 1, false);
                    } else if (currentPos == images.size() * 2 || currentPos == 0) {
                        // Or here  Go here    Here
                        // |          |          |
                        // 0 1        0 1        0 1
                        binding.galleryImages.setCurrentItem(images.size(), false);
                    }
                }
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                // Not implemented
            }
        });


        // If the context of the gallery is attached to a Slidr make sure it's locked so
        // swipes in the gallery won't slide the activity/fragment away
        lockSlidr(true);
         */

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

    @Override
    public Bundle getExtras() {
        Bundle extras = super.getExtras();
        extras.putInt(EXTRAS_ACTIVE_IMAGE, binding.galleryImages.getCurrentItem());
        return extras;
    }

    @Override
    public void setExtras(Bundle extras) {
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
           // return images.size() == 1 ? 1 : images.size() * 3;
            return images.size();
        }

        @Override
        public Object instantiateItem(ViewGroup container, final int position) {
            //Image image = images.get(position % images.size());
            Image image = images.get(position);

            // Use ContentImage as that already has listeners, NSFW caching etc already
            ContentImage contentImage = new ContentImage(context);
            contentImage.setWithImageUrl(redditPost, image.getUrl());

            // TODO imgur albums might be gifs

            container.addView(contentImage);

            return contentImage;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((ContentImage) object);
        }
    }
}
