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
import com.example.hakonsreader.api.model.RedditComment;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.misc.Util;
import com.example.hakonsreader.views.VoteBar;
import com.google.gson.GsonBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.ViewHolder> {
    private static final String TAG = "CommentsAdapter";

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

        // TODO make this cleaner

        // TODO remove magic string and create "listing" enum or something
        // The comment is a "12 more comments"
        if (comment.getKind().equals("more")) {
            int extraComments = comment.getExtraCommentsCount();

            String extraCommentsText = holder.itemView.getResources().getQuantityString(
                    R.plurals.extraComments,
                    extraComments,
                    extraComments
            );

            // TODO add listener to actually fetch the comments
            // Clear everything except the author field which now holds the amount of extra comments
            holder.author.setText(extraCommentsText);
            holder.age.setText("");
            holder.content.setText("");
            holder.voteBar.setVisibility(View.GONE);
        } else {
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

        // DEBUG
        holder.itemView.setOnClickListener(view -> {
            Log.d(TAG, "onBindViewHolder: " + new GsonBuilder().setPrettyPrinting().create().toJson(comment));
        });
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

            this.author = itemView.findViewById(R.id.comment_author);
            this.age = itemView.findViewById(R.id.comment_age);
            this.content = itemView.findViewById(R.id.comment_content);
            this.voteBar = itemView.findViewById(R.id.comment_vote_bar);
        }
    }
}
