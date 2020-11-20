package com.example.hakonsreader.views;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.hakonsreader.R;
import com.example.hakonsreader.activites.DispatcherActivity;
import com.example.hakonsreader.api.model.Image;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.databinding.ContentLinkBinding;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Class for post contents that are a link. This extends ScrollView so that if the user
 * selects a max size for posts that is very low this view can be scrolled to view it in its entirety
 */
public class ContentLink extends Content {
    private static final String TAG = "ContentLink";

    private final ContentLinkBinding binding;

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

    @Override
    protected void updateView() {
        // The previews will (I believe) never be above 1080p, and that should be fine for most devices
        // TODO although this will use more data, so it might be reasonable to add a data saving setting where
        //  this image quality is reduced

        List<Image> previews = redditPost.getPreviewImages();
        if (redditPost.isNsfw()) {
            List<Image> obfuscatedPreviews = redditPost.getObfuscatedPreviewImages();
            if (obfuscatedPreviews != null && !obfuscatedPreviews.isEmpty()) {
                previews = obfuscatedPreviews;
            }
        }

        if (!previews.isEmpty()) {
            Image preview = previews.get(previews.size() - 1);

            // If the image was deleted the response for getting the image will return a 404
            // We could add a callback and reduce the image height in onError, but it looks kind of weird
            // since it has to do the request which causes it suddenly jump, so if the image has been deleted
            // we just let the size be as it is
            if (!preview.getUrl().isEmpty()) {
                Picasso.get()
                        .load(preview.getUrl())
                        .into(binding.thumbnail);
            }
        } else {
            // No thumbnail, reduce the height so it doesn't take up too much unnecessary space
            ViewGroup.LayoutParams params = binding.thumbnail.getLayoutParams();
            params.height = (int) getResources().getDimension(R.dimen.contentLinkNoThumbnailSize);
            binding.thumbnail.setLayoutParams(params);
        }

        binding.link.setText(redditPost.getUrl());
    }

    /**
     * Dispatches the link to {@link DispatcherActivity}
     */
    private void openLink() {
        Intent intent = new Intent(getContext(), DispatcherActivity.class);
        intent.putExtra(DispatcherActivity.URL_KEY, redditPost.getUrl());
        getContext().startActivity(intent);
    }
}
