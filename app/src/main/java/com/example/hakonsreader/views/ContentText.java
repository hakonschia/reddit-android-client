package com.example.hakonsreader.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.misc.InternalLinkMovementMethod;
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

        String markdown = post.getSelftext(true);

        // Self text posts with only a title won't have a body
        if (markdown != null) {
            // Note the movement method must be set before applying the markdown
            textView.setMovementMethod(InternalLinkMovementMethod.getInstance(getContext()));
            textView.setLinkTextColor(getContext().getColor(R.color.linkColor));
            // TODO this crashes the app with some tables, such as https://www.reddit.com/r/test/comments/j7px9a/sadasd/
            //  not sure what happens here as the same text can be rendered fine in a stand-alone app
            App.get().getMark().setMarkdown(textView, markdown);
        }

        int padding = (int) getResources().getDimension(R.dimen.defaultIndent);
        setPadding(padding, 0, padding, 0);
    }

}
