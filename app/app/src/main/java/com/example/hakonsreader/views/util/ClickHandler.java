package com.example.hakonsreader.views.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import com.example.hakonsreader.R;
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
     * @param subreddit The subreddit to open
     */
    public static void openSubredditInActivity(View view,  String subreddit) {
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

}
