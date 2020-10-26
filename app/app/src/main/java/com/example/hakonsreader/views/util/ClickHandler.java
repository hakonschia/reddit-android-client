package com.example.hakonsreader.views.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.hakonsreader.R;
import com.example.hakonsreader.activites.ImageActivity;
import com.example.hakonsreader.activites.ProfileActivity;
import com.example.hakonsreader.activites.SubredditActivity;

/**
 * Various click handlers that can be used as click listeners for data binding
 */
public class ClickHandler {
    private static final String TAG = "ClickHandler";

    private ClickHandler() { }


    /**
     * Empty function to consume long click events. This can be used in XML to handle
     * a conditional {@code onLongClick} where one operand should be empty
     *
     * @return Always {@code true}
     */
    public static boolean emptyClickBoolean() { return true; }

    /**
     * Opens an activity with the selected subreddit
     *
     * @param view The view itself is ignored, but this cannot be null as the context is needed
     * @param subreddit The subreddit to open
     */
    public static void openSubredditInActivity(View view, String subreddit) {
        Context context = view.getContext();
        Activity activity = (Activity)context;

        // Don't open another activity if we are already in that subreddit (because why would you)
        // TODO also check if we are in PostActivity and the post was started from the same subreddit
        // TODO also check for fragments
        if (activity instanceof SubredditActivity) {
            SubredditActivity subredditActivity = (SubredditActivity)activity;
            if (subredditActivity.getSubredditName().equals(subreddit)) {
                return;
            }
        }

        // Send some data like what sub it is etc etc so it knows what to load
        Intent intent = new Intent(context, SubredditActivity.class);
        intent.putExtra(SubredditActivity.SUBREDDIT_KEY, subreddit);

        activity.startActivity(intent);

        // Slide the activity in
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    /**
     * Opens an activity to show a users profile
     *
     * @param view The view itself is ignored, but this cannot be null as the context is needed
     * @param username The username of the profile to open
     */
    public static void openProfileInActivity(View view, String username) {
        Context context = view.getContext();
        Activity activity = (Activity)context;

        // Send some data like what sub it is etc etc so it knows what to load
        Intent intent = new Intent(context, ProfileActivity.class);
        intent.putExtra(ProfileActivity.USERNAME_KEY, username);

        activity.startActivity(intent);

        // Slide the activity in
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }


    /**
     * Opens an image in fullscreen
     *
     * @param view The view itself is ignored, but this cannot be null as the context is needed
     * @param imageUrl The URL to the image
     */
    public static void openImageInFullscreen(View view, String imageUrl) {
        Context context = view.getContext();
        Activity activity = (Activity)context;

        // Send some data like what sub it is etc etc so it knows what to load
        Intent intent = new Intent(context, ImageActivity.class);
        intent.putExtra(ImageActivity.IMAGE_URL, imageUrl);

        activity.startActivity(intent);

        // Slide the activity in
        activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

}
