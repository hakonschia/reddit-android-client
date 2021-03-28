package com.example.hakonsreader.views.util;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.databinding.BindingAdapter;
import androidx.fragment.app.FragmentManager;

import com.example.hakonsreader.R;
import com.example.hakonsreader.api.interfaces.ReportableListing;
import com.example.hakonsreader.api.model.RedditComment;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.api.model.Subreddit;
import com.example.hakonsreader.api.model.flairs.RichtextFlair;
import com.example.hakonsreader.fragments.bottomsheets.ReportsBottomSheet;
import com.example.hakonsreader.interfaces.OnReportsIgnoreChangeListener;
import com.example.hakonsreader.views.Tag;
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
        if (subreddit == null) {
            return;
        }
        String iconURL = subreddit.getIcon();
        String communityURL = subreddit.getCommunityIcon();

        // We use error and placeholder as the default icon. In case no internet it would override
        // the android:src in the layout, and the error isn't shown until the attempt to get the image has been made
        if (iconURL != null && !iconURL.isEmpty()) {
            Picasso.get()
                    .load(iconURL)
                    .placeholder(R.drawable.ic_emoji_emotions_200dp)
                    .error(R.drawable.ic_emoji_emotions_200dp)
                    .into(imageView);
        } else if(communityURL != null && !communityURL.isEmpty()) {
            Picasso.get()
                    .load(communityURL)
                    .placeholder(R.drawable.ic_emoji_emotions_200dp)
                    .error(R.drawable.ic_emoji_emotions_200dp)
                    .into(imageView);
        } else {
            String subredditName = subreddit.getName();
            switch (subredditName.toLowerCase()) {
                case "popular":
                    imageView.setImageDrawable(ContextCompat.getDrawable(imageView.getContext(), R.drawable.ic_trending_up_100));
                    break;

                case "all":
                    imageView.setImageDrawable(ContextCompat.getDrawable(imageView.getContext(), R.drawable.ic_text_rotation_angleup_24));
                    break;

                default:
                    imageView.setImageDrawable(ContextCompat.getDrawable(imageView.getContext(), R.drawable.ic_emoji_emotions_200dp));
                    break;
            }
        }
    }

    /**
     * Adds the authors flair to the comment. If the author has no flair the view is set to {@link View#GONE}
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
     * Adds the authors flair to the comment. If the author has no flair the view is set to {@link View#GONE}
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
     * Adds the authors flair to the comment. If the author has no flair the view is set to {@link View#GONE}
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
     * Adds the users flair in a subreddit. The the user has no flair is the subreddit this view is
     * set to {@link View#GONE}
     *
     * @param tag The tag to set the flair on
     * @param subreddit The subreddit
     */
    @BindingAdapter("subredditUserFlair")
    public static void setLinkFlair(Tag tag, Subreddit subreddit) {
        if (subreddit == null) {
            return;
        }

        boolean tagAdded = ViewUtil.setFlair(
                tag,
                subreddit.getUserFlairRichText(),
                subreddit.getUserFlairText(),
                subreddit.getUserFlairTextColor(),
                subreddit.getUserFlairBackgroundColor()
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

    /**
     * Opens a BottomSheet with the user reports of a ReportableListing
     *
     * @param listing The listing to show user reports for
     * @param context The context to open the BottomSheet in
     * @param onReportsIgnoreChange The callback for when the reports have been ignored/unignored
     */
    public static void openReportsBottomSheet(ReportableListing listing, Context context, OnReportsIgnoreChangeListener onReportsIgnoreChange) {
        if (listing.getNumReports() == 0) {
            return;
        }
        FragmentManager manager = ((AppCompatActivity)context).getSupportFragmentManager();

        ReportsBottomSheet bottomSheet = ReportsBottomSheet.Companion.newInstance(listing);
        bottomSheet.setOnIgnoreChange(onReportsIgnoreChange);

        bottomSheet.show(manager, "reportsBottomSheet");
    }
}
