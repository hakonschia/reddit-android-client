package com.example.hakonsreader.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.hakonsreader.R;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.databinding.FullPostBarBinding;
import com.robinhood.ticker.TickerUtils;

/**
 * View for the full bar underneath a Reddit post
 */
public class FullPostBar extends ConstraintLayout {
    private RedditPost post;

    private final FullPostBarBinding binding;
    private int hideScoreTime;

    public FullPostBar(@NonNull Context context) {
        this(context, null, 0, 0);
    }
    public FullPostBar(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }
    public FullPostBar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }
    public FullPostBar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
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
        binding.setPost(post);

        this.updateView();
    }

    /**
     * Call this if the score should always be hidden. Must be called before {@link FullPostBar#setPost(RedditPost)}
     */
    public void setHideScore(boolean hideScore) {
        binding.voteBar.setHideScore(hideScore);
    }

    /**
     * Gets if the score is set to always be hidden
     *
     * @return True if the score is always hidden
     */
    public boolean getHideScore() {
        return binding.voteBar.getHideScore();
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

        // This has to include both as it might go from "0 comments" to "1 comment", also "999 comments" to "1.0K comments"
        binding.numComments.setCharacterLists(TickerUtils.provideNumberList(), TickerUtils.provideNumberList());
        binding.numComments.setText(commentsText);
    }

    /**
     * Enables or disables the animation for any {@link com.robinhood.ticker.TickerView} found
     * in this view
     *
     * @param enable True to enable
     */
    public void enableTickerAnimation(boolean enable) {
        binding.voteBar.enableTickerAnimation(enable);
        binding.numComments.setAnimationDuration(enable ? (long)getResources().getInteger(R.integer.tickerAnimationDefault) : 0);
    }

    /**
     * Check if the TickerViews have animation enabled
     *
     * @return True if animation is enabled
     */
    public boolean tickerAnimationEnabled() {
        // Technically voteBar and numComments can have different values, but assume they're always synced
        return binding.voteBar.tickerAnimationEnabled();
    }
}
