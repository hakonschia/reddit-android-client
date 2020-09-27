package com.example.hakonsreader.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.hakonsreader.R;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.databinding.FullPostBarBinding;

/**
 * View for the full bar underneath a Reddit post
 */
public class FullPostBar extends ConstraintLayout {
    private RedditPost post;

    private FullPostBarBinding binding;

    public FullPostBar(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        binding = FullPostBarBinding.inflate(LayoutInflater.from(context), this, true);
    }

    /**
     * Sets the post this bar is for. Automatically updates the view for the bar
     *
     * @param post The post to set
     */
    public void setPost(RedditPost post) {
        this.post = post;
        binding.voteBar.setListing(post);

        this.updateView();
    }

    /**
     * Updates the view based on the post set with {@link FullPostBar#post}
     */
    private void updateView() {
        binding.voteBar.updateVoteStatus();

        float comments = post.getAmountOfComments();

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

        binding.numComments.setText(commentsText);
    }
}
