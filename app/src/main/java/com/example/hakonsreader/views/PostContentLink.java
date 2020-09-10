package com.example.hakonsreader.views;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.hakonsreader.R;
import com.example.hakonsreader.api.model.RedditPost;
import com.squareup.picasso.Picasso;

public class PostContentLink extends ConstraintLayout {
    private static final int THUMBNAIL_SIZE_NOT_SET = -1;
    private static int thumbnailSize = THUMBNAIL_SIZE_NOT_SET;

    private RedditPost post;

    private ImageView thumbnail;
    private TextView link;


    public PostContentLink(@NonNull Context context) {
        super(context);
        inflate(getContext(), R.layout.layout_post_content_link, this);

        this.thumbnail = findViewById(R.id.link_content_thumbnail);
        this.link = findViewById(R.id.link_content_link);

        this.thumbnail.setOnClickListener(v -> this.openLink());
        this.link.setOnClickListener(v -> this.openLink());

        if (thumbnailSize == THUMBNAIL_SIZE_NOT_SET) {
            thumbnailSize = (int)getResources().getDimension(R.dimen.post_link_thumnail_size);
        }
    }

    public void setPost(RedditPost post) {
        this.post = post;

        this.updateView();
    }

    private void updateView() {
        Picasso.get()
                .load(this.post.getThumbnail())
                .resize(thumbnailSize, thumbnailSize)
                .into(this.thumbnail);

        this.link.setText(post.getUrl());
    }

    private void openLink() {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(post.getUrl()));
        getContext().startActivity(i);
    }
}
