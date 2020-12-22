package com.example.hakonsreader.views.util;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Space;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.databinding.BindingAdapter;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.api.model.RedditComment;
import com.example.hakonsreader.api.model.RedditPost;
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
                    .placeholder(R.drawable.ic_emoji_emotions_200dp)
                    .error(R.drawable.ic_emoji_emotions_200dp)
                    .resize(size, size)
                    .into(imageView);
        } else if(communityURL != null && !communityURL.isEmpty()) {
            Picasso.get()
                    .load(communityURL)
                    .placeholder(R.drawable.ic_emoji_emotions_200dp)
                    .error(R.drawable.ic_emoji_emotions_200dp)
                    .resize(size, size)
                    .into(imageView);
        } else {
            imageView.setImageDrawable(ContextCompat.getDrawable(imageView.getContext(), R.drawable.ic_emoji_emotions_200dp));
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
     * Adds the authors flair to the comment. If the author has no flair the view is set to [View.GONE]
     *
     * @param tag The tag to set the flair on
     * @param comment The comment
     */
    @BindingAdapter("authorFlair")
    public static void setAuthorFlair(Tag tag, RedditComment comment) {
        if (comment == null) {
            return;
        }

        boolean tagAdded = ViewUtil.setFlair(
                tag,
                comment.getAuthorRichtextFlairs(),
                comment.getAuthorFlairText(),
                comment.getAuthorFlairTextColor(),
                comment.getAuthorFlairBackgroundColor()
        );

        if (tagAdded) {
            tag.setVisibility(View.VISIBLE);
        } else {
            tag.setVisibility(View.GONE);
        }
    }

    /**
     * Adds the authors flair to the comment. If the author has no flair the view is set to [View.GONE]
     *
     * @param tag The tag to set the flair on
     * @param post The post
     */
    @BindingAdapter("authorFlair")
    public static void setAuthorFlair(Tag tag, RedditPost post) {
        if (post == null) {
            return;
        }

        boolean tagAdded = ViewUtil.setFlair(
                tag,
                post.getAuthorRichtextFlairs(),
                post.getAuthorFlairText(),
                post.getAuthorFlairTextColor(),
                post.getAuthorFlairBackgroundColor()
        );

        if (tagAdded) {
            tag.setVisibility(View.VISIBLE);
        } else {
            tag.setVisibility(View.GONE);
        }
    }

    /**
     * Adds the authors flair to the comment. If the author has no flair the view is set to [View.GONE]
     *
     * @param tag The tag to set the flair on
     * @param post The post
     */
    @BindingAdapter("linkFlair")
    public static void setLinkFlair(Tag tag, RedditPost post) {
        if (post == null) {
            return;
        }

        boolean tagAdded = ViewUtil.setFlair(
                tag,
                post.getLinkRichtextFlairs(),
                post.getLinkFlairText(),
                post.getLinkFlairTextColor(),
                post.getLinkFlairBackgroundColor()
        );

        if (tagAdded) {
            tag.setVisibility(View.VISIBLE);
        } else {
            tag.setVisibility(View.GONE);
        }
    }

    /**
     * Sets the flairs from a Reddit post or comment on a Tag
     *
     * @param tag The tag to set the text on
     * @param flairs The list of richtext flairs
     * @param flairText The text for the flair
     * @param textColor The text color for the flair (can be Reddit specific "dark" or "light")
     * @param backgroundColor The flair background color
     * @return True if the tag had something added to it, false otherwise
     */
    public static boolean setFlair(@NonNull Tag tag, List<RichtextFlair> flairs, String flairText, String textColor, String backgroundColor) {
        // No richtext flair items, and no standard flair text, return as there won't be anything to add
        if ((flairs == null || flairs.isEmpty()) && (flairText == null || flairText.isEmpty())) {
            return false;
        }

        tag.clear();

        Context context = tag.getContext();

        if (textColor.equals("dark")) {
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

        return true;
    }
}
