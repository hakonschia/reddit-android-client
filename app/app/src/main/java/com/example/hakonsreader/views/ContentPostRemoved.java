package com.example.hakonsreader.views;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.databinding.BindingAdapter;

import com.example.hakonsreader.R;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.databinding.ContentPostRemovedBinding;

/**
 * View for posts that have been removed
 */
public class ContentPostRemoved extends ConstraintLayout {
    private static final String TAG = "ContentPostRemoved";
    private final ContentPostRemovedBinding binding;
    private RedditPost post;


    public ContentPostRemoved(Context context) {
        this(context, null, 0, 0);
    }
    public ContentPostRemoved(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }
    public ContentPostRemoved(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }
    public ContentPostRemoved(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        binding = ContentPostRemovedBinding.inflate(LayoutInflater.from(context), this, true);
    }


    /**
     * Sets the post to use for this view
     *
     * @param post The post to set
     */
    public void setPost(RedditPost post) {
        this.post = post;
        binding.setPost(post);
        Log.d(TAG, "setPost: bruh");
    }


    /**
     * Sets the text saying which category (ie. moderator, admin etc.) removed the post
     *
     * @param tv The TextView to set the text on
     * @param post The post that has been removed
     */
    @BindingAdapter("removedBy")
    public static void removedBy(TextView tv, RedditPost post) {
        String removedByCategory = post.getRemovedByCategory();
        Log.d(TAG, "removedBy: " + removedByCategory);

        if (removedByCategory.equals("moderator")) {
            tv.setText(tv.getResources().getString(R.string.postRemovedByMods, post.getSubreddit()));
        } else {
            tv.setText(tv.getResources().getString(R.string.postRemovedGeneric));
        }
    }
}