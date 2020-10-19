package com.example.hakonsreader.recyclerviewadapters;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.databinding.BindingAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.enums.Thing;
import com.example.hakonsreader.api.model.RedditComment;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.databinding.ListItemCommentBinding;
import com.example.hakonsreader.databinding.VoteBarBinding;
import com.example.hakonsreader.interfaces.OnReplyListener;
import com.example.hakonsreader.misc.InternalLinkMovementMethod;
import com.example.hakonsreader.misc.Util;
import com.example.hakonsreader.misc.ViewUtil;
import com.example.hakonsreader.views.Tag;
import com.example.hakonsreader.views.VoteBar;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static android.view.View.GONE;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.ViewHolder> {
    private static final String TAG = "CommentsAdapter";

    private RedditApi redditApi = App.get().getApi();


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

    public OnReplyListener getReplyListener() {
        return replyListener;
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
        int pos = comments.indexOf(comment);
        comments.remove(pos);
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
     * Hides comments from being shown. Does not remove the comments from the actual list
     *
     * @param start The comment to start at. This comment and any replies will be hidden
     */
    public void hideComments(RedditComment start) {
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
    public void showComments(RedditComment start) {
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


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        ListItemCommentBinding binding = ListItemCommentBinding.inflate(layoutInflater, parent, false);

        // The post will always be the same so set it now
        binding.setPost(post);
        binding.setAdapter(this);

        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RedditComment comment = comments.get(position);
        holder.bind(comment);

        int indent = (int)holder.itemView.getResources().getDimension(R.dimen.comment_depth_indent);
        holder.itemView.setPadding(indent * comment.getDepth(), 0, 0, 0);


        // TODO this is extremely laggy when changes to the dataset happens
        /*
        // With preDrawListener we can get the height of the itemView before it is drawn, and then create the sidebars with that height
        holder.itemView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                int height = holder.itemView.getMeasuredHeight();
                int barWidth = (int)holder.itemView.getResources().getDimension(R.dimen.comment_side_bar_width);
                int indent = (int)holder.itemView.getResources().getDimension(R.dimen.comment_depth_indent);

                // Ensure there are no sidebars already (loading more comments can cause issues because the holder
                // wont be recycled before it is re-drawn)
                holder.binding.sideBars.removeAllViews();

                // Every comment is only responsible for the lines to its side, so each line will match up
                // with the line for the comment above and below to create a long line throughout the entire list
                // Also top level comments don't have a side bar as that looks weird (i <= comment.getDepth() to add it)
                for (int i = 0; i < comment.getDepth(); i++) {
                    View view = new View(holder.itemView.getContext());
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(barWidth, height);
                    LinearLayout.MarginLayoutParams marginLayoutParams = new ViewGroup.MarginLayoutParams(params);
                    marginLayoutParams.rightMargin = indent;

                    view.setLayoutParams(marginLayoutParams);
                    view.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.secondary_background));

                    holder.binding.sideBars.addView(view);
                }

                // Remove the listener to avoid infinite calls
                holder.itemView.getViewTreeObserver().removeOnPreDrawListener(this);

                // Returning false means to cancel the current scheduled draw, which would not include
                // the sidebars and would cause them to appear a split second later
                return false;
            }
        });
         */
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }


    /**
     * LongClick listener for views
     *
     * @param view Ignored
     * @param comment The comment clicked
     * @return True (event is always consumed)
     */
    public boolean hideCommentsLongClick(View view, RedditComment comment) {
        hideComments(comment);
        return true;
    }

    /**
     * LongClick listener for text views. Hides a comment chain
     *
     * @param view The view clicked. Note this must be a {@link TextView}, or else the function will not
     *             do anything but consume the event
     * @param comment The comment clicked
     * @return True (event is always consumed)
     */
    public boolean hideCommentsLongClickText(View view, RedditComment comment) {
        if (view instanceof TextView) {
            TextView tv = (TextView) view;

            // Not a hyperlink (even long clicking on the hyperlink would open it, so don't collapse as well)
            if (tv.getSelectionStart() == -1 && tv.getSelectionEnd() == -1) {
                hideComments(comment);
            }
        }

        return true;
    }

    /**
     * Sets the markdown for the text and adds a movement method to handle reddit links
     *
     * @param textView The textview to add the markdown to
     * @param markdown The markdown text
     */
    @BindingAdapter("commentMarkdown")
    public static void setCommentMarkdown(TextView textView, @Nullable String markdown) {
        if (markdown == null) {
            return;
        }
        textView.setMovementMethod(InternalLinkMovementMethod.getInstance(textView.getContext()));
        markdown = App.get().getAdjuster().adjust(markdown);
        App.get().getMark().setMarkdown(textView, markdown);
    }

    /**
     * Adds the authors flair to the comment. If the author has no flair the view is set to {@link View#GONE}
     *
     * @param view The view that holds the author flair
     * @param comment The comment
     */
    @BindingAdapter("authorFlair")
    public static void addAuthorFlair(FrameLayout view, @Nullable RedditComment comment) {
        if (comment == null) {
            return;
        }
        Tag tag = ViewUtil.createFlair(
                comment.getAuthorRichtextFlairs(),
                comment.getAuthorFlairText(),
                comment.getAuthorFlairTextColor(),
                comment.getAuthorFlairBackgroundColor(),
                view.getContext()
        );

        if (tag != null) {
            // The view might still have old views
            view.removeAllViews();
            view.addView(tag);

            view.setVisibility(View.VISIBLE);
        } else {
            // No author flair, remove the view so it doesn't take up space
            view.setVisibility(View.GONE);
        }
    }

    /**
     * Formats the author text based on whether or not it is posted by a mod, the poster.
     *
     * <p>If both {@code asMod} or {@code asPoster} are false the format is set to default</p>
     *
     * @param tv The TextView to format. Changes the text color and background drawable
     * @param asMod True if posted (and distinguished) by a moderator
     * @param asPoster True if posted by the poster of the post
     */
    @BindingAdapter({"asMod", "asPoster"})
    public static void formatAuthor(TextView tv, boolean asMod, boolean asPoster) {
        if (asMod) {
            tv.setBackground(ContextCompat.getDrawable(tv.getContext(), R.drawable.comment_by_mod));
            tv.setTextColor(ContextCompat.getColor(tv.getContext(), R.color.text_color));
        } else if (asPoster) {
            tv.setBackground(ContextCompat.getDrawable(tv.getContext(), R.drawable.comment_by_poster));
            tv.setTextColor(ContextCompat.getColor(tv.getContext(), R.color.text_color));
        } else {
            tv.setBackground(null);
            tv.setTextColor(ContextCompat.getColor(tv.getContext(), R.color.link_color));
        }
    }

    /**
     * OnClick listener for "2 more comments" comments.
     *
     * <p>Loads more comments and adds them to {@link CommentsAdapter#comments}</p>
     *
     * @param comment The comment to load from. This comment has to be a "2 more comments" comment.
     *               When the comments have been loaded this will be removed from {@link CommentsAdapter#comments}
     */
    @BindingAdapter("getMoreComments")
    public void getMoreComments(View view, RedditComment comment) {
        int pos = comments.indexOf(comment);
        int depth = comment.getDepth();

        // The parent is the first comment upwards in the list that has a lower depth
        RedditComment parent = null;

        // On posts with a lot of comments the last comment is often a "771 more comments" which is a
        // top level comment, which means it won't have a parent so it's no point in trying to find it
        if (depth != 0) {
            for (int i = pos - 1; i >= 0; i--) {
                RedditComment c = comments.get(i);
                if (c.getDepth() < depth) {
                    parent = c;
                    break;
                }
            }
        }

        // TODO move api call to ViewModel
        final RedditComment finalParent = parent;
        redditApi.getMoreComments(post.getId(), comment.getChildren(), finalParent, newComments -> {
            // Find the parent index to know where to insert the new comments
            int commentPos = comments.indexOf(comment);
            this.insertComments(newComments, commentPos);

            // Remove the previous comment (this is the "2 more comments" comment)
            this.removeComment(comment);


            if (finalParent != null) {
                finalParent.removeReply(comment);
            }
        }, (code, t) -> {
            t.printStackTrace();
            Util.handleGenericResponseErrors(parentLayout, code, t);
        });
    }


    public class ViewHolder extends RecyclerView.ViewHolder {
        private final ListItemCommentBinding binding;

        public ViewHolder(ListItemCommentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Bind the ViewHolder to a comment
         *
         * @param comment The comment to bind
         */
        public void bind(RedditComment comment) {
            binding.setComment(comment);
            binding.setIsMoreComments(Thing.MORE.getValue().equals(comment.getKind()));
            binding.setCommentHidden(commentsHidden.contains(comment));
            binding.executePendingBindings();

            // TODO when the vote bar uses data binding this can probably be set through xml
            binding.commentVoteBar.setListing(comment);
        }
    }
}
