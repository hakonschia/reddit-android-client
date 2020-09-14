package com.example.hakonsreader.views;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.hakonsreader.R;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.databinding.LayoutPostContentLinkBinding;
import com.squareup.picasso.Picasso;

public class PostContentLink extends ConstraintLayout {
    private static final int THUMBNAIL_SIZE_NOT_SET = -1;
    private static int thumbnailSize = THUMBNAIL_SIZE_NOT_SET;

    private LayoutPostContentLinkBinding binding;

    private RedditPost post;


    public PostContentLink(@NonNull Context context, RedditPost post) {
        super(context);
        this.binding = LayoutPostContentLinkBinding.inflate(LayoutInflater.from(context), this, true);

        this.post = post;

        this.binding.thumbnail.setOnClickListener(v -> this.openLink());
        this.binding.link.setOnClickListener(v -> this.openLink());

        if (thumbnailSize == THUMBNAIL_SIZE_NOT_SET) {
            thumbnailSize = (int)getResources().getDimension(R.dimen.post_link_thumnail_size);
        }

        this.updateView();
    }

    public PostContentLink(@NonNull Context context) {
        super(context);
    }


    private void updateView() {
        Picasso.get()
                .load(this.post.getThumbnail())
                .resize(thumbnailSize, thumbnailSize)
                .into(this.binding.thumbnail);

        this.binding.link.setText(post.getUrl());
    }

    private void openLink() {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(post.getUrl()));
        getContext().startActivity(i);
    }
}
