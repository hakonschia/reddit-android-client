package com.example.hakonsreader.views.util;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Space;

import androidx.core.content.ContextCompat;
import androidx.databinding.BindingAdapter;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.api.model.Subreddit;
import com.example.hakonsreader.api.model.flairs.RichtextFlair;
import com.example.hakonsreader.views.Tag;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Utility class for views. Provides functions to create View tags (such as NSFW tags) and
 * some binding adapters
 */
public class ViewUtil {
    private static final String TAG = "ViewUtil";
    
    private ViewUtil() { }


    /**
     * Sets an ImageView to a subreddits icon. If no icon is found a default drawable is used
     *
     * @param imageView The view to insert the image into
     * @param subreddit The subreddit to set the image for
     */
    @BindingAdapter("subredditIcon")
    public static void setSubredditIcon(ImageView imageView, Subreddit subreddit) {
        String iconURL = subreddit.getIcon();
        String communityURL = subreddit.getCommunityIcon();
        int size = (int) imageView.getResources().getDimension(R.dimen.subredditIconSizeInList);

        // We use error and placeholder as the default icon. In case no internet it would override
        // the android:src in the layout, and the error isn't shown until the attempt to get the image has been made
        if (iconURL != null && !iconURL.isEmpty()) {
            Picasso.get()
                    .load(iconURL)
                    .placeholder(R.drawable.ic_baseline_emoji_emotions_200)
                    .error(R.drawable.ic_baseline_emoji_emotions_200)
                    .resize(size, size)
                    .into(imageView);
        } else if(communityURL != null && !communityURL.isEmpty()) {
            Picasso.get()
                    .load(communityURL)
                    .placeholder(R.drawable.ic_baseline_emoji_emotions_200)
                    .error(R.drawable.ic_baseline_emoji_emotions_200)
                    .resize(size, size)
                    .into(imageView);
        } else {
            imageView.setImageDrawable(ContextCompat.getDrawable(imageView.getContext(), R.drawable.ic_baseline_emoji_emotions_200));
        }
    }

    /**
     * Sets an ImageView to a subreddits banner background. If no image is found the visibility of the view
     * is set to GONE.
     *
     * <p>If data saving is enabled the image will only be loaded if the image is found in the local cache</p>
     *
     * @param imageView The ImageView to load the banner into
     * @param subreddit The subreddit to load the banner for
     */
    @BindingAdapter("subredditBanner")
    public static void setSubredditBannerImage(ImageView imageView, Subreddit subreddit) {
        String bannerURL = subreddit.getBannerBackgroundImage();
        if (bannerURL != null && !bannerURL.isEmpty()) {
            // Data saving on, only load if the image is already cached
            if (App.Companion.get().dataSavingEnabled()) {
                Picasso.get()
                        .load(bannerURL)
                        .networkPolicy(NetworkPolicy.OFFLINE)
                        .into(imageView, new Callback() {
                            @Override
                            public void onSuccess() {
                                imageView.setVisibility(View.VISIBLE);

                            }
                            @Override
                            public void onError(Exception e) {
                                imageView.setVisibility(View.GONE);
                            }
                        });
            } else {
                imageView.setVisibility(View.VISIBLE);
                Picasso.get()
                        .load(bannerURL)
                        .into(imageView);
            }
        } else {
            imageView.setVisibility(View.GONE);
        }
    }


    /**
     * Creates a spoiler tag
     *
     * @param context The context for the tag
     * @return A {@link Tag} with spoiler text and formatting
     */
    public static Tag createSpoilerTag(Context context) {
        Resources resources = context.getResources();

        Tag tag = new Tag(context);
        tag.setFillColor(ContextCompat.getColor(context, R.color.tagSpoiler));
        tag.setTextColor(ContextCompat.getColor(context, R.color.tagSpoilerText));
        tag.addText(resources.getString(R.string.tagSpoiler));

        return tag;
    }

    /**
     * Creates a NSFW tag
     *
     * @param context The context for the tag
     * @return A {@link Tag} with NSFW text and formatting
     */
    public static Tag createNsfwTag(Context context) {
        Resources resources = context.getResources();

        Tag tag = new Tag(context);
        tag.setFillColor(ContextCompat.getColor(context, R.color.tagNSFW));
        tag.setTextColor(ContextCompat.getColor(context, R.color.tagNSFWText));
        tag.addText(resources.getString(R.string.tagNSFW));

        return tag;
    }


    /**
     * Creates a flair for a post or comment
     */
    public static Tag createFlair(List<RichtextFlair> flairs, String flairText, String flairColor, String backgroundColor, Context context) {
        // No richtext flair items, and no standard flair text, return as there won't be anything to add
        if ((flairs == null || flairs.isEmpty()) && (flairText == null || flairText.isEmpty())) {
            return null;
        }

        Tag tag = new Tag(context);

        if (flairColor.equals("dark")) {
            tag.setTextColor(ContextCompat.getColor(context, R.color.flairTextDark));
            tag.setFillColor(ContextCompat.getColor(context, R.color.flairBackgroundDark));
        } else {
            tag.setTextColor(ContextCompat.getColor(context, R.color.flairTextLight));
            tag.setFillColor(ContextCompat.getColor(context, R.color.flairBackgroundLight));
        }

        if (backgroundColor != null && !backgroundColor.isEmpty()) {
            tag.setFillColor(backgroundColor);
        }

        // If no richtext flairs, try to see if there is a text flair
        // Apparently some subs set both text and richtext flairs *cough* GlobalOffensive *cough*
        // so make sure only one is
        if (flairs.isEmpty()) {
            tag.addText(flairText);
        } else {
            // Add all views the flair has
            flairs.forEach(flair -> {
                if (flair.getType().equals("text")) {
                    tag.addText(flair.getText());
                } else if (flair.getType().equals("emoji")) {
                    tag.addImage(flair.getUrl());
                }
            });
        }

        return tag;
    }


    /**
     * Adds a tag to a ViewGroup, with extra space after it
     *
     * @param layout The layout to add the tag to
     * @param tag The tag to add
     */
    public static void addTagWithSpace(ViewGroup layout, Tag tag) {
        layout.addView(tag);

        Space space = new Space(layout.getContext());
        space.setMinimumWidth((int)layout.getResources().getDimension(R.dimen.tagSpace));
        layout.addView(space);
    }

}
