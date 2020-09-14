package com.example.hakonsreader.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.hakonsreader.R;
import com.example.hakonsreader.api.model.RedditPost;

/**
 * View for the full bar underneath a Reddit post
 */
public class FullPostBar extends ConstraintLayout {
    private RedditPost post;

    private VoteBar voteBar;
    private TextView numComments;

    public FullPostBar(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        inflate(getContext(), R.layout.layout_full_post_bar, this);

        this.voteBar = findViewById(R.id.voteBar);
        this.numComments = findViewById(R.id.postNumComments);
    }

    /**
     * Sets the post this bar is for. Automatically updates the view for the bar
     *
     * @param post The post to set
     */
    public void setPost(RedditPost post) {
        this.post = post;
        this.voteBar.setListing(post);

        this.updateView();
    }

    /**
     * Updates the view based on the post set with {@link FullPostBar#post}
     */
    private void updateView() {
        this.voteBar.updateVoteStatus();

        float comments = this.post.getAmountOfComments();

        String commentsText;

        // Above 10k comments, show "1.5k comments" instead
        if (comments > 1000) {
            commentsText = String.format(getResources().getString(R.string.numCommentsThousands), comments / 1000f);
        } else {
            commentsText = getResources().getQuantityString(
                    R.plurals.numComments,
                    post.getAmountOfComments(),
                    post.getAmountOfComments()
            );
        }

        this.numComments.setText(commentsText);
    }
}
