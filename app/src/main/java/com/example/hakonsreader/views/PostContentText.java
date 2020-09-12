package com.example.hakonsreader.views;

import android.content.Context;
import android.text.Html;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import com.example.hakonsreader.R;
import com.example.hakonsreader.api.model.RedditPost;

public class PostContentText extends androidx.appcompat.widget.AppCompatTextView {

    private RedditPost post;

    public PostContentText(Context context, RedditPost post) {
        super(context);

        this.post = post;
        this.updateView();
    }

    public PostContentText(Context context) {
        super(context);
    }
    public PostContentText(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }
    public PostContentText(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    private void updateView() {
        this.setTextColor(getContext().getColor(R.color.textColorTextPosts));

        String html = post.getSelftextHtml();

        // Self text posts with only a title won't have a body
        if (html != null) {
            this.setText(Html.fromHtml(post.getSelftextHtml(), Html.FROM_HTML_MODE_LEGACY));
        }
    }

}
