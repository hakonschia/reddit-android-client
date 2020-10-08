package com.example.hakonsreader.views;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
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
    private ContentLinkBinding binding;

    private RedditPost post;


    public ContentLink(@NonNull Context context, RedditPost post) {
        super(context);
        binding = ContentLinkBinding.inflate(LayoutInflater.from(context), this, true);

        this.post = post;

        binding.thumbnail.setOnClickListener(v -> this.openLink());
        binding.link.setOnClickListener(v -> this.openLink());

        this.updateView();
    }

    public ContentLink(@NonNull Context context) {
        super(context);
    }


    private void updateView() {
        Picasso.get()
                .load(post.getThumbnail())
                .resize((int)getResources().getDimension(R.dimen.postLinkThumnailWidth), (int)getResources().getDimension(R.dimen.postLinkThumnailHeight))
                .into(binding.thumbnail);

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
