package com.example.hakonsreader.views;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.hakonsreader.R;
import com.example.hakonsreader.activites.DispatcherActivity;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.databinding.ContentLinkBinding;
import com.squareup.picasso.Picasso;

/**
 * Class for post contents that are a link
 */
public class ContentLink extends ConstraintLayout {
    private static final String TAG = "ContentLink";

    private final ContentLinkBinding binding;
    private RedditPost post;

    public ContentLink(@NonNull Context context) {
        this(context, null, 0, 0);
    }
    public ContentLink(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }
    public ContentLink(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }
    public ContentLink(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        binding = ContentLinkBinding.inflate(LayoutInflater.from(context), this, true);

        binding.thumbnail.setOnClickListener(v -> this.openLink());
        binding.link.setOnClickListener(v -> this.openLink());
    }

    /**
     * Sets the post this content is for and updates the view
     *
     * @param post The post
     */
    public void setPost(RedditPost post) {
        this.post = post;
        this.updateView();
    }

    private void updateView() {
        if (!post.getThumbnail().isEmpty()) {
            Picasso.get()
                    .load(post.getThumbnail())
                    .resize((int)getResources().getDimension(R.dimen.postLinkThumnailWidth), (int)getResources().getDimension(R.dimen.postLinkThumnailHeight))
                    .into(binding.thumbnail);
        }

        binding.link.setText(post.getUrl());
    }

    /**
     * Opens the link found in {@link ContentLink#post} in the browser
     */
    private void openLink() {
        Intent intent = new Intent(getContext(), DispatcherActivity.class);
        intent.putExtra(DispatcherActivity.URL_KEY, post.getUrl());
        getContext().startActivity(intent);
    }
}
