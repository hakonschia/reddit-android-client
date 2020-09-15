package com.example.hakonsreader.recyclerviewadapters;

import android.content.Context;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hakonsreader.R;
import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.model.RedditComment;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.constants.NetworkConstants;
import com.example.hakonsreader.misc.Util;
import com.example.hakonsreader.views.VoteBar;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.ViewHolder> {
    private static final String TAG = "CommentsAdapter";

    private RedditApi redditApi = RedditApi.getInstance(NetworkConstants.USER_AGENT);

    private List<RedditComment> comments = new ArrayList<>();
    private RedditPost post;


    /**
     * @param post The post the comment is for
     */
    public CommentsAdapter(RedditPost post) {
        this.post = post;
    }

    public void addComments(List<RedditComment> comments) {
        this.comments.addAll(comments);
        notifyDataSetChanged();
    }

    /**
     * Inserts a sublist of new comments into a given position
     *
     * @param newComments The comments to add
     * @param at The position to insert the comments
     */
    public void insertComments(List<RedditComment> newComments, int at) {
        this.comments.addAll(at, newComments);
        notifyItemRangeInserted(at, newComments.size());
    }

    public void removeComment(RedditComment comment) {
        int pos = this.comments.indexOf(comment);
        this.comments.remove(pos);
        notifyItemRemoved(pos);
    }

    /**
     * Find the next top level comment of the comment chain
     *
     * @param currentPos The position to start looking at
     * @return The position of the next top level comment, or {@code currentPos} if there are no more top level comments
     */
    public int getNextTopLevelCommentPos(int currentPos) {
        for(int i = currentPos; i < comments.size(); i++) {
            RedditComment comment = comments.get(i);

            if (comment.getDepth() == 0) {
                return i;
            }
        }

        return currentPos;
    }

    public int getPreviousTopLevelCommentPos(int currentPos) {
        for(int i = currentPos; i >= 0; i--) {
            RedditComment comment = comments.get(i);

            if (comment.getDepth() == 0) {
                return i;
            }
        }

        return currentPos;
    }

    /**
     * Loads more comments and adds them to {@link CommentsAdapter#comments}
     *
     * @param parent The parent comment to load from. This comment has to be a "2 more comments" comment.
     *               When the comments have been loaded this will be removed from {@link CommentsAdapter#comments}
     */
    public void getMoreComments(RedditComment parent) {
        this.redditApi.getMoreComments(post.getId(), parent.getChildren(), comments -> {
            // Find the parent index to know where to insert the new comments
            int commentPos = this.comments.indexOf(parent);
            this.insertComments(comments, commentPos);

            // Remove the parent comment (this is the "2 more comments" comment)
            this.removeComment(parent);
        }, (code, t) -> {

        });
    }

    /**
     * Hides comments from being shown. Does not remove the comments from the actual list
     *
     * @param start The comment to start at. This comment and any replies will be hidden
     */
    private void hideComments(RedditComment start) {

    }

    /**
     * Sets a holder as a "4 more comments" comment
     * <p>The only thing shown is the text of "more comments", everything else is hidden away</p>
     *
     * @param comment The comment data
     * @param holder The holder to set for
     */
    private void asMoreComments(RedditComment comment, ViewHolder holder) {
        int extraComments = comment.getExtraCommentsCount();

        String extraCommentsText = holder.itemView.getResources().getQuantityString(
                R.plurals.extraComments,
                extraComments,
                extraComments
        );

        // Clear everything except the author field which now holds the amount of extra comments
        holder.author.setText(extraCommentsText);
        holder.itemView.setOnClickListener(view -> this.getMoreComments(comment));
        holder.itemView.setOnLongClickListener(null);

        holder.age.setText("");
        holder.content.setText("");
        holder.voteBar.setVisibility(View.GONE);
    }

    /**
     * Sets the contents of the the view holder as a standard comment with content, vote bars etc.
     *
     * @param comment The comment data
     * @param holder The holder to set for
     */
    private void asNormalComment(RedditComment comment, ViewHolder holder) {
        Context context = holder.itemView.getContext();

        String authorText = String.format(context.getString(R.string.authorPrefixed), comment.getAuthor());

        holder.content.setText(Html.fromHtml(comment.getBodyHtml(), Html.FROM_HTML_MODE_COMPACT));
        holder.content.setMovementMethod(LinkMovementMethod.getInstance());

        // TODO create something around the text to highlight better the post is from OP
        if (comment.getAuthor().equals(post.getAuthor())) {
            holder.author.setText(
                    String.format(Locale.getDefault(), context.getString(R.string.commentByPoster), comment.getAuthor())
            );
        } else {
            holder.author.setText(authorText);
        }

        // Calculate the time since the comment was posted
        Instant created = Instant.ofEpochSecond(comment.getCreatedAt());
        Duration between = Duration.between(created, Instant.now());
        holder.age.setText(Util.createAgeText(context.getResources(), between));

        holder.voteBar.setListing(comment);
        holder.voteBar.setVisibility(View.VISIBLE);

        // Remove the listener if there is one (from "more comments")
        holder.itemView.setOnClickListener(null);

        // Hide comments on long clicks
        holder.itemView.setOnLongClickListener(view -> {
            this.hideComments(comment);
            Log.d(TAG, "onBindViewHolder: Hiding comments from " + comment.getBody());
            return true;
        });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.list_layout_comment,
                parent,
                false
        );

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RedditComment comment = this.comments.get(position);

        // TODO remove magic string and create "listing" enum or something
        // The comment is a "12 more comments"
        if (comment.getKind().equals("more")) {
            this.asMoreComments(comment, holder);
        } else {
            this.asNormalComment(comment, holder);
        }

        if (comment.isMod()) {
            holder.author.setTextColor(holder.itemView.getContext().getColor(R.color.modTextColor));
        } else {
            holder.author.setTextColor(holder.itemView.getContext().getColor(R.color.linkColor));
        }

        int paddingStart = comment.getDepth() * (int)holder.itemView.getResources().getDimension(R.dimen.comment_depth_indent);
        holder.itemView.setPadding(paddingStart, 0, 0, 0);
        // Update the layout with new padding
        holder.itemView.requestLayout();
    }

    @Override
    public int getItemCount() {
        return this.comments.size();
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView author;
        private TextView age;
        private TextView content;
        private VoteBar voteBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            this.author = itemView.findViewById(R.id.commentAuthor);
            this.age = itemView.findViewById(R.id.commentAge);
            this.content = itemView.findViewById(R.id.commentContent);
            this.voteBar = itemView.findViewById(R.id.commentVoteBar);
        }
    }
}
