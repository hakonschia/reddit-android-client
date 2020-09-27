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
import com.example.hakonsreader.misc.Util;

public class ContentText extends ScrollView {

    private RedditPost post;
    private TextView textView;

    public ContentText(Context context, RedditPost post) {
        super(context);

        this.setScrollbarFadingEnabled(false);

        this.textView = new TextView(context);
        this.addView(this.textView);

        this.post = post;
        this.updateView();
    }

    public ContentText(Context context) {
        super(context);
    }
    public ContentText(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }
    public ContentText(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    private void updateView() {
        this.textView.setTextColor(getContext().getColor(R.color.textColorTextPosts));

        String html = post.getSelftextHTML();

        // Self text posts with only a title won't have a body
        if (html != null) {
            this.textView.setMovementMethod(LinkMovementMethod.getInstance());
            this.textView.setLinkTextColor(getContext().getColor(R.color.linkColor));
            this.textView.setText(Util.fromHtml(html));
        }

        int padding = (int) getResources().getDimension(R.dimen.defaultIndent);

        setPadding(padding, 0, padding, 0);
    }

}
