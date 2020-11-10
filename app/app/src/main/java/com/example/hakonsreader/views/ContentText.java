package com.example.hakonsreader.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.misc.InternalLinkMovementMethod;


/**
 * View for text posts. This only shows the text of the post (with Markwon), but extends {@link ScrollView}
 * instead of {@link TextView} so that the scrolling of the text has acceleration/drag that continues
 * scrolling after the user has stopped scrolling (as this is expected behaviour when scrolling)
 */
public class ContentText extends ScrollView {

    private RedditPost post;
    private final TextView textView;

    public ContentText(Context context) {
        this(context, null, 0);
    }
    public ContentText(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }
    public ContentText(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        textView = new TextView(context);
        this.addView(textView);
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
        textView.setTextColor(getContext().getColor(R.color.textColorTextPosts));

        String markdown = post.getSelftext();

        // Self text posts with only a title won't have a body
        if (markdown != null) {
            // Note the movement method must be set before applying the markdown
            textView.setMovementMethod(InternalLinkMovementMethod.getInstance(getContext()));
            textView.setLinkTextColor(ContextCompat.getColor(getContext(), R.color.link_color));

            // TODO this crashes the app with some tables, such as https://www.reddit.com/r/test/comments/j7px9a/sadasd/
            //  not sure what happens here as the same text can be rendered fine in a stand-alone app
            markdown = App.get().getAdjuster().adjust(markdown);
            App.get().getMark().setMarkdown(textView, markdown);
        }
    }

}
