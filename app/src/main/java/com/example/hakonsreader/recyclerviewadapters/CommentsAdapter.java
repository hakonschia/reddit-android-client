package com.example.hakonsreader.recyclerviewadapters;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.model.RedditComment;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.interfaces.OnReplyListener;
import com.example.hakonsreader.misc.InternalLinkMovementMethod;
import com.example.hakonsreader.misc.Util;
import com.example.hakonsreader.views.VoteBar;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static android.view.View.GONE;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.ViewHolder> {
    private static final String TAG = "CommentsAdapter";

    private RedditApi redditApi = App.getApi();


    /**
     * The list of comments that should be shown. This list might not include all comments for the post
     * as some might be hidden
     */
    private List<RedditComment> comments = new ArrayList<>();

    /**
     * The comments that have been selected to be hidden.
     * These are only the comments that have been explicitly selected to be hidden, and not its children.
     * These comments are still shown in a preview form to show that a comment chain is hidden.
     */
    private List<RedditComment> commentsHidden = new ArrayList<>();


    private RedditPost post;
    private View parentLayout;

    private OnReplyListener replyListener;


    /**
     * @param post The post the comment is for
     */
    public CommentsAdapter(RedditPost post) {
        this.post = post;
    }

    /**
     * Sets the parent layout this adapter is in
     *
     * @param parentLayout The parent layout of the adapter
     */
    public void setParentLayout(View parentLayout) {
        this.parentLayout = parentLayout;
    }

    /**
     * Sets the listener for what to do when the reply button has been clicked
     *
     * @param replyListener The listener to call
     */
    public void setOnReplyListener(OnReplyListener replyListener) {
        this.replyListener = replyListener;
    }

    /**
     * Adds a new top level comment
     *
     * @param newComment The comment to add
     */
    public void addComment(RedditComment newComment) {
        this.comments.add(newComment);
        notifyItemInserted(this.comments.size() - 1);
    }

    /**
     * Adds a new comment as a reply to {@code parent}
     *
     * @param newComment The comment to add
     * @param parent The parent comment being replied to
     */
    public void addComment(RedditComment newComment, RedditComment parent) {
        int pos = this.comments.indexOf(parent);

        this.comments.add(pos + 1, newComment);
        notifyItemInserted(pos + 1);
    }

    /**
     * Appends comments to the current list of comments
     *
     * @param comments The comments to add
     */
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

    /**
     * Removes a comment from the comment list
     *
     * @param comment The comment to remove
     */
    public void removeComment(RedditComment comment) {
        int pos = this.comments.indexOf(comment);
        this.comments.remove(pos);
        notifyItemRemoved(pos);
    }

    public void setComments(List<RedditComment> comments) {
        this.comments = comments;
        notifyDataSetChanged();
    }

    public void setCommentsHidden(List<RedditComment> commentsHidden) {
        this.commentsHidden = commentsHidden;
        notifyDataSetChanged();
    }

    public List<RedditComment> getComments() {
        return comments;
    }

    public List<RedditComment> getCommentsHidden() {
        return commentsHidden;
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

    /**
     * Finds the position of the previous top level comment
     *
     * @param currentPos The position to start from
     * @return The position of the previous top level comment
     */
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
     * @param comment The comment to load from. This comment has to be a "2 more comments" comment.
     *               When the comments have been loaded this will be removed from {@link CommentsAdapter#comments}
     * @param parent The parent comment of {@code comment}
     */
    public void getMoreComments(RedditComment comment, RedditComment parent) {
        this.redditApi.getMoreComments(post.getID(), comment.getChildren(), parent, newComments -> {
            // Find the parent index to know where to insert the new comments
            int commentPos = comments.indexOf(comment);
            this.insertComments(newComments, commentPos);

            // Remove the previous comment (this is the "2 more comments" comment)
            this.removeComment(comment);
            parent.removeReply(comment);
        }, (code, t) -> {
            Util.handleGenericResponseErrors(parentLayout, code, t);
        });
    }

    /**
     * Hides comments from being shown. Does not remove the comments from the actual list
     *
     * @param start The comment to start at. This comment and any replies will be hidden
     */
    private void hideComments(RedditComment start) {
        int startPos = comments.indexOf(start);

        // Update the comment selected to show that it is now a hidden comment chain
        commentsHidden.add(start);
        notifyItemChanged(startPos);

        // Remove all its replies
        List<RedditComment> replies = start.getReplies();
        comments.removeAll(replies);

        // The comment explicitly hidden isn't being removed, but its UI is updated
        notifyItemRangeRemoved(startPos + 1, replies.size());
    }

    /**
     * Shows a comment chain that has previously been hidden
     *
     * @param start The start of the chain
     */
    private void showComments(RedditComment start) {
        int pos = comments.indexOf(start);

        // This comment is no longer hidden
        commentsHidden.remove(start);
        notifyItemChanged(pos);

        // Find the replies to the starting comment that are shown (not previously hidden)
        List<RedditComment> replies = getShownReplies(start);

        // Add back all its children
        comments.addAll(pos + 1, replies);
        notifyItemRangeInserted(pos + 1, replies.size());
    }

    /**
     * Retrieve the list of replies to a comment that are shown
     *
     * @param parent The parent to retrieve replies for
     * @return The list of children of {@code parent} that are shown. Children of children are also
     * included in the list
     */
    private List<RedditComment> getShownReplies(RedditComment parent) {
        List<RedditComment> replies = new ArrayList<>();

        for (RedditComment reply : parent.getReplies()) {
            // Only add direct children, let the children handle their children
            if (reply.getDepth() - 1 == parent.getDepth()) {
                replies.add(reply);

                // Reply isn't hidden which means it potentially has children to show
                if (!commentsHidden.contains(reply)) {
                    replies.addAll(getShownReplies(reply));
                }
            }
        }

        return replies;
    }



    @Override
    public int getItemCount() {
        return comments.size();
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RedditComment comment = comments.get(position);

        // Format holder based on who the user is (mod, poster, or no one special)
        if (comment.isMod()) {
            holder.asMod();
        } else if (post.getAuthor().equals(comment.getAuthor())) {
            holder.asPoster();
        }

        // TODO remove magic string and create "listing" enum or something
        if (comment.getKind().equals("more")) {
            // TODO the parent comment isn't necessarily the previous, get the comment before in the list whos depth is one lower
            holder.asMoreComments(comment, this.comments.get(position - 1));
        } else {
            holder.asNormalComment(comment);
        }

        if (commentsHidden.contains(comment)) {
            holder.commentHidden(() -> this.showComments(comment));
        }

        int paddingStart = comment.getDepth() * (int)holder.itemView.getResources().getDimension(R.dimen.comment_depth_indent);
        holder.itemView.setPadding(paddingStart, 0, 0, 0);
        // Update the layout with new padding
        holder.itemView.requestLayout();
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.reset();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.list_item_comment,
                parent,
                false
        );

        return new ViewHolder(view);
    }



    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView author;
        private TextView age;
        private TextView content;
        private ImageView locked;
        private ImageView stickied;
        private ImageButton reply;
        private VoteBar voteBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            author = itemView.findViewById(R.id.commentAuthor);
            age = itemView.findViewById(R.id.commentAge);
            content = itemView.findViewById(R.id.commentContent);
            locked = itemView.findViewById(R.id.lock);
            stickied = itemView.findViewById(R.id.stickied);
            reply = itemView.findViewById(R.id.reply);
            voteBar = itemView.findViewById(R.id.commentVoteBar);
        }


        /**
         * Resets the view to default values so all views start out the same
         */
        private void reset() {
            // Reset author text (from when comment is by poster/mod)
            author.setBackground(null);
            author.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.linkColor));

            // Reset if holder previously was a hidden comment
            author.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
            age.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));

            locked.setVisibility(View.GONE);
            stickied.setVisibility(View.GONE);

            // Reset it holder previously was "5 more comments"
            voteBar.setVisibility(View.VISIBLE);
            reply.setVisibility(View.VISIBLE);
            itemView.setOnClickListener(null);
        }

        /**
         * Formats the comment as a mod comment
         */
        private void asMod() {
            author.setBackground(ContextCompat.getDrawable(itemView.getContext(), R.drawable.comment_by_mod));
            author.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.textColor));
        }

        /**
         * Formats the comment as a comment posted by the OP of the post
         */
        private void asPoster() {
            author.setBackground(ContextCompat.getDrawable(itemView.getContext(), R.drawable.comment_by_poster));
            author.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.textColor));
        }

        /**
         * Sets the contents of the the view holder as a standard comment with content, vote bars etc.
         *
         * @param comment The comment to use for the holder
         */
        private void asNormalComment(RedditComment comment) {
            Context context = itemView.getContext();

            content.setText(Util.fromHtml(comment.getBodyHtml(), context));
            content.setMovementMethod(InternalLinkMovementMethod.getInstance(context));

            author.setText(String.format(context.getString(R.string.authorPrefixed), comment.getAuthor()));

            // Calculate the time since the comment was posted
            Instant created = Instant.ofEpochSecond(comment.getCreatedAt());
            Duration between = Duration.between(created, Instant.now());
            age.setText(Util.createAgeText(context.getResources(), between));

            if (post.isLocked()) {
                reply.setVisibility(GONE);
            }
            if (comment.isLocked()) {
                locked.setVisibility(View.VISIBLE);
            }
            if (comment.isStickied()) {
                stickied.setVisibility(View.VISIBLE);
            }

            reply.setOnClickListener(view -> replyListener.replyTo(comment));

            voteBar.setListing(comment);

            // Hide comments on long clicks
            // This has to be set on both the TextView as well as the entire holder since the TextView
            // has movementMethod set to allow for clickable hyperlinks which makes setting it on only
            // the holder not work for the TextView
            content.setOnLongClickListener(view -> {
                // Not a hyperlink (even long clicking on the hyperlink would open it, so don't collapse as well)
                if (content.getSelectionStart() == -1 && content.getSelectionEnd() == -1) {
                    hideComments(comment);
                }
                return true;
            });
            itemView.setOnLongClickListener(view -> {
                hideComments(comment);
                return true;
            });
        }

        /**
         * Sets a holder as a "4 more comments" comment
         * <p>The only thing shown is the text of "more comments", everything else is hidden away</p>
         *
         * @param comment The comment to use for the holder
         */
        private void asMoreComments(RedditComment comment, RedditComment parent) {
            int extraComments = comment.getExtraCommentsCount();

            String extraCommentsText = itemView.getResources().getQuantityString(
                    R.plurals.extraComments,
                    extraComments,
                    extraComments
            );

            // Clear everything except the author field which now holds the amount of extra comments
            author.setText(extraCommentsText);
            itemView.setOnClickListener(view -> getMoreComments(comment, parent));
            itemView.setOnLongClickListener(null);

            age.setText("");
            content.setText("");
            reply.setVisibility(GONE);
            voteBar.setVisibility(GONE);
        }

        /**
         * Formats the comment as a hidden comment. Only use this for the comment that was
         * explicitly selected to be hidden, as the comment will still be shown, with adjusted
         * formatting to show it contains a hidden comment chain
         *
         * @param runnable What to do when the comment is clicked again
         */
        private void commentHidden(Runnable runnable) {
            author.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));
            age.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));

            content.setText("");
            reply.setVisibility(GONE);
            voteBar.setVisibility(GONE);

            itemView.setOnClickListener(view -> runnable.run());
            itemView.setOnLongClickListener(null);
        }
    }
}
