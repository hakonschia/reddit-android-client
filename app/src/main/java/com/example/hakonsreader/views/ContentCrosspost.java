package com.example.hakonsreader.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.example.hakonsreader.api.model.RedditPost;

public class ContentCrosspost extends LinearLayout {

    private RedditPost post;

    public ContentCrosspost(Context context, RedditPost post) {
        super(context);
        this.post = post;

        this.updateView();
    }

    public ContentCrosspost(Context context) {
        super(context);
    }
    public ContentCrosspost(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }
    public ContentCrosspost(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    public ContentCrosspost(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }


    private void updateView() {
    }

}
