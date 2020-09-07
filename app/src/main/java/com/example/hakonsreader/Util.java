package com.example.hakonsreader;

import android.content.Context;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.model.RedditPost;

public class Util {
    private Util() {}


    /**
     * Updates the vote status for a post (button + text colors)
     *
     * @param post The post to update for
     * @param context The context of the vote buttons
     * @param voteBar The view that holds the vote buttons
     */
    public static void updateVoteStatus(RedditPost post, View voteBar, Context context) {
        RedditApi.VoteType voteType = post.getVoteType();

        ImageButton upvote = voteBar.findViewById(R.id.vote_bar_upvote);
        ImageButton downvote = voteBar.findViewById(R.id.vote_bar_downvote);
        TextView score = voteBar.findViewById(R.id.vote_bar_score);

        int color = R.color.textColor;

        // Reset both buttons as at least one will change
        // (to avoid keeping the color if going from upvote to downvote and vice versa)
        upvote.getDrawable().setTint(context.getColor(R.color.no_vote));
        downvote.getDrawable().setTint(context.getColor(R.color.no_vote));

        switch (voteType) {
            case Upvote:
                color = R.color.upvoted;
                upvote.getDrawable().setTint(context.getColor(color));
                break;

            case Downvote:
                color = R.color.downvoted;
                downvote.getDrawable().setTint(context.getColor(color));
                break;

            case NoVote:
            default:
                break;
        }

        score.setTextColor(context.getColor(color));
    }
}
