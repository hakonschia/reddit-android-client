package com.example.hakonsreader.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.databinding.ContentTextBinding;
import com.example.hakonsreader.misc.InternalLinkMovementMethod;


/**
 * View for text posts. This only shows the text of the post (with Markwon), but extends {@link ScrollView}
 * instead of {@link TextView} so that the scrolling of the text has acceleration/drag that continues
 * scrolling after the user has stopped scrolling (as this is expected behaviour when scrolling)
 */
public class ContentText extends Content {

    private final ContentTextBinding binding;

    public ContentText(Context context) {
        this(context, null, 0);
    }
    public ContentText(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }
    public ContentText(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        binding = ContentTextBinding.inflate(LayoutInflater.from(context), this, true);
    }

    @Override
    protected void updateView() {
        String markdown = redditPost.getSelftext();

        // Self text posts with only a title won't have a body
        if (markdown != null) {
            // Note the movement method must be set before applying the markdown
            binding.content.setMovementMethod(InternalLinkMovementMethod.getInstance(getContext()));

            // TODO this crashes the app with some tables, such as https://www.reddit.com/r/test/comments/j7px9a/sadasd/
            //  not sure what happens here as the same text can be rendered fine in a stand-alone app
            markdown = App.get().getAdjuster().adjust(markdown);
            App.get().getMark().setMarkdown(binding.content, markdown);
        }
    }
}
