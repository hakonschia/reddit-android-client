package com.example.hakonsreader.views;

import android.content.Context;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.hakonsreader.R;
import com.example.hakonsreader.api.model.RedditPost;

public class PostContentText extends ScrollView {

    private RedditPost post;
    private TextView textView;

    public PostContentText(Context context, RedditPost post) {
        super(context);

        this.setScrollbarFadingEnabled(false);
        this.setBackgroundColor(context.getColor(R.color.iconColor));

        this.textView = new TextView(context);
        this.addView(this.textView);

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
        this.textView.setTextColor(getContext().getColor(R.color.textColorTextPosts));

        String html = post.getSelftextHtml();

        // Self text posts with only a title won't have a body
        if (html != null) {
            this.textView.setMovementMethod(LinkMovementMethod.getInstance());
            this.textView.setLinkTextColor(getContext().getColor(R.color.linkColor));
            this.textView.setText(Html.fromHtml(post.getSelftextHtml(), Html.FROM_HTML_MODE_LEGACY));
        }
    }

}
