package com.example.hakonsreader.misc;

import android.content.Context;
import android.view.View;

import com.example.hakonsreader.R;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.views.PostContentImage;
import com.example.hakonsreader.views.PostContentLink;
import com.example.hakonsreader.views.PostContentText;
import com.example.hakonsreader.views.PostContentVideo;
import com.google.android.material.snackbar.Snackbar;

public class Util {


    /**
     * Generates content view for a post
     *
     * @param post The post to generate for
     * @return A view with the content of the post
     */
    public static View generatePostContent(RedditPost post, Context context) {
        switch (post.getPostType()) {
            case IMAGE:
                return new PostContentImage(context, post);

            case VIDEO:
                return new PostContentVideo(context, post);

            case RICH_VIDEO:
                // Links such as youtube, gfycat etc are rich video posts
                return null;

            case LINK:
                return new PostContentLink(context, post);

            case TEXT:
                return new PostContentText(context, post);

            default:
                return null;
        }
    }


    /**
     * Creates and shows a snackbar for generic server errors
     *
     * @param parent The view to attach the snackbar to
     */
    public static void showGenericServerErrorSnackbar(View parent) {
        Snackbar.make(parent, parent.getResources().getString(R.string.genericServerError), Snackbar.LENGTH_SHORT).show();
    }

    /**
     * Creates and shows a snackbar for errors caused by too many requests sent
     *
     * @param parent The view to attach the snackbar to
     */
    public static void showTooManyRequestsSnackbar(View parent) {
        Snackbar.make(parent, parent.getResources().getString(R.string.tooManyRequestsError), Snackbar.LENGTH_SHORT).show();
    }
}
