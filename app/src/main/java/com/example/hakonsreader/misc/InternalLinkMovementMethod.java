package com.example.hakonsreader.misc;


import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Layout;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.TextView;

import com.example.hakonsreader.activites.SubredditActivity;

import java.util.List;

/**
 * Set this on a textview and then you can potentially open links locally if applicable
 *
 * Taken from: https://gitlab.com/Commit451/LabCoat/-/blob/0da57c371815902f4ba24fcd7bceaa1e7a8d7bb7/app/src/main/java/com/commit451/gitlab/util/InternalLinkMovementMethod.java
 */
public class InternalLinkMovementMethod extends LinkMovementMethod {
    private static final String TAG = "InternalLinkMovementMethod";

    private static InternalLinkMovementMethod subredditAndUserInstance;


    /**
     * Retrieves the instance that checks for links that are subreddits and user profiles
     *
     * <p>When a subreddit or user profile is detected the corresponding activity is started.
     * If no match is found the link is handled normally</p>
     *
     * @param context The context to use for opening new activities
     * @return The static instance
     */
    public static InternalLinkMovementMethod getInstance(Context context) {
        if (subredditAndUserInstance == null) {
            subredditAndUserInstance = new InternalLinkMovementMethod(linkText -> {
                // Match either "r/something" or "/r/something"
                String subredditRegex = "^/?r/.*";

                // Match either "u/hakonschia" or "/u/hakonschia/", and "user/hakonschia" or "/user/hakonschia"
                String userRegex = "^/?u(ser)?/.*";


                if (linkText.matches(subredditRegex)) {
                    // Extract only the subreddit. Probably an easier way to do this, but this way I don't have to
                    // worry about if it's "r/" or "/r/" as it

                    // TODO this can probably be extracted for users as well when a UserActivity is created
                    // Ensure it's a always "/r/" not "r/" as this makes extracting the subreddit easier
                    linkText = (linkText.charAt(0) == '/' ? "" : "/") + linkText;
                    String[] segments = linkText.split("/");

                    // The segments include an empty string before the first slash
                    String subreddit = segments[2];

                    Intent intent = new Intent(context, SubredditActivity.class);
                    intent.putExtra(SubredditActivity.SUBREDDIT_KEY, subreddit);
                    context.startActivity(intent);
                    return true;
                } else if (linkText.matches(userRegex)) {
                    Log.d(TAG, "asNormalComment: Trying to open user profile");

                    return true;
                }
                // TODO if it's an image url open ImageActivity


                return false;
            });
        }

        return subredditAndUserInstance;
    }


    private OnLinkClickedListener mOnLinkClickedListener;

    public InternalLinkMovementMethod(OnLinkClickedListener onLinkClickedListener) {
        mOnLinkClickedListener = onLinkClickedListener;
    }

    public boolean onTouchEvent(TextView widget, android.text.Spannable buffer, android.view.MotionEvent event) {
        int action = event.getAction();

        //http://stackoverflow.com/questions/1697084/handle-textview-link-click-in-my-android-app
        if (action == MotionEvent.ACTION_UP) {
            int x = (int) event.getX();
            int y = (int) event.getY();

            x -= widget.getTotalPaddingLeft();
            y -= widget.getTotalPaddingTop();

            x += widget.getScrollX();
            y += widget.getScrollY();

            Layout layout = widget.getLayout();
            int line = layout.getLineForVertical(y);
            int off = layout.getOffsetForHorizontal(line, x);

            URLSpan[] link = buffer.getSpans(off, off, URLSpan.class);
            if (link.length != 0) {
                String url = link[0].getURL();
                boolean handled = mOnLinkClickedListener.onLinkClicked(url);
                if (handled) {
                    return true;
                }
                return super.onTouchEvent(widget, buffer, event);
            }
        }
        return super.onTouchEvent(widget, buffer, event);
    }

    public interface OnLinkClickedListener {
        boolean onLinkClicked(String url);
    }
}
