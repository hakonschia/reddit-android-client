package com.example.hakonsreader.recyclerviewadapters;

import android.content.res.Resources;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.Barrier;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
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
import com.example.hakonsreader.databinding.ListItemHiddenCommentBinding;
import com.example.hakonsreader.databinding.ListItemMoreCommentBinding;
import com.example.hakonsreader.interfaces.OnReplyListener;
import com.example.hakonsreader.misc.InternalLinkMovementMethod;
import com.example.hakonsreader.misc.Util;
import com.example.hakonsreader.views.util.ViewUtil;
import com.example.hakonsreader.views.Tag;

import java.util.ArrayList;
import java.util.List;

import static android.view.View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION;


/**
 * Adapter for a RecyclerView populated with {@link RedditComment} objects. This adapter
 * supports three different comment layouts:
 * <ol>
 *     <li>Normal comments</li>
 *     <li>Hidden comments</li>
 *     <li>"More" comments (eg. "2 more comments")</li>
 * </ol>
 */
public class CommentsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = "CommentsAdapter";

    // TODO this has become kind of messy and should be refactored to remove stuff that isn't used
    //  and so I know what is actually going on here


    /**
     * The value returned from {@link CommentsAdapter#getItemViewType(int)} when the comment is
     * a "more comments" comment
     */
    private static final int MORE_COMMETS_TYPE = 0;

    /**
     * The value returned from {@link CommentsAdapter#getItemViewType(int)} when the comment is
     * a normal comment
     */
    private static final int NORMAL_COMMENT_TYPE = 1;

    /**
     * The value returned from {@link CommentsAdapter#getItemViewType(int)} when the comment is
     * a hidden comment
     */
    private static final int HIDDEN_COMMENT_TYPE = 2;


    private final RedditApi redditApi = App.get().getApi();

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

    /**
     * If {@link CommentsAdapter#commentIdChain} is set, this list will hold the chain of comments
     * that should be shown
     */
    private List<RedditComment> chain = new ArrayList<>();

    /**
     * The list of comments shown when a chain is shown. This will be used to go back to the comments
     * previously shown when the user wants to get out of a chain
     */
    private List<RedditComment> commentsShownWhenChainSet;


    private final RedditPost post;
    private View parentLayout;
    private String commentIdChain;
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
     * Adds a new top level comment. The comment is added as the first element
     *
     * @param newComment The comment to add
     */
    public void addComment(RedditComment newComment) {
        comments.add(0, newComment);
        notifyItemInserted(0);
    }

    /**
     * Adds a new comment as a reply to {@code parent}
     *
     * @param newComment The comment to add
     * @param parent The parent comment being replied to
     */
    public void addComment(RedditComment newComment, RedditComment parent) {
        int pos = comments.indexOf(parent);

        comments.add(pos + 1, newComment);
        notifyItemInserted(pos + 1);
    }

    /**
     * Appends comments to the current list of comments
     *
     * @param newComments The comments to add
     */
    public void addComments(List<RedditComment> newComments) {
        comments.addAll(newComments);
        setChain(newComments);

        notifyDataSetChanged();
        checkForHiddenComments(newComments);
    }

    /**
     * Inserts a sublist of new comments into a given position
     *
     * @param newComments The comments to add
     * @param at The position to insert the comments
     */
    public void insertComments(List<RedditComment> newComments, int at) {
        comments.addAll(at, newComments);
        notifyItemRangeInserted(at, newComments.size());
        checkForHiddenComments(newComments);
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
        checkForHiddenComments(comments);
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
     * Sets {@link CommentsAdapter#chain} based on {@link CommentsAdapter#commentIdChain}.
     * If the chain is found, the currently shown list is stored in {@link CommentsAdapter#commentsShownWhenChainSet}
     *
     * @param commentsToLookIn The comments to look in
     */
    private void setChain(List<RedditComment> commentsToLookIn) {
        // TODO this is bugged when in a chain and going into a new chain

        if (commentIdChain != null && !commentIdChain.isEmpty()) {
            // We have to clear the list here. In case the comment isn't found every comment should be shown
            int previousChainSize = chain.size();
            chain.clear();

            for (RedditComment comment : commentsToLookIn) {
                if (comment.getId().equalsIgnoreCase(commentIdChain)) {
                    chain = getShownReplies(comment);

                    // The actual comment must also be added at the start
                    chain.add(0, comment);

                    // This list should take us back to all the comments, so if we're setting a chain
                    // from within a chain, don't store the list
                    if (previousChainSize == 0) {
                        commentsShownWhenChainSet = comments;
                    }
                    comments = chain;
                    break;
                }
            }
        } else {
            chain.clear();
        }

        if (chain.isEmpty() && commentsShownWhenChainSet != null) {
            comments = commentsShownWhenChainSet;
        }

        // TODO animate the changes with "notifyItemRangeInserted" etc., it looks pretty bad now
        //  also it should scroll to the top on chains (now it can end up in the middle of a chain)
        notifyDataSetChanged();
    }

    /**
     * Sets what comment chain should be shown. If this is null or empty the entire list is shown.
     *
     * <p>This can be called before the comments are set, or after</p>
     *
     * @param commentIdChain The ID of the comment to show
     */
    public void setCommentIdChain(String commentIdChain) {
        // Don't do anything if the id is the same, because why would you
        if (this.commentIdChain != null && this.commentIdChain.equalsIgnoreCase(commentIdChain)) {
            return;
        }
        this.commentIdChain = commentIdChain;
        setChain(comments);
    }

    /**
     * Notifies that a comment has been updated
     *
     * @param comment The comment updated
     */
    public void notifyItemChanged(RedditComment comment) {
        int pos = comments.indexOf(comment);
        if (pos != -1) {
            notifyItemChanged(pos);
        }
    }

    /**
     * Removes all comments from the list
     */
    public void clearComments() {
        int size = comments.size();
        comments.clear();
        commentsHidden.clear();
        notifyItemRangeRemoved(0, size);
    }

    /**
     * Find the next top level comment of the comment (hidden comments are ignored)
     *
     * @param currentPos The position to start looking at
     * @return The position of the next top level comment, or {@code currentPos} if there are no more top level comments
     */
    public int getNextTopLevelCommentPos(int currentPos) {
        for(int i = currentPos; i < comments.size(); i++) {
            RedditComment comment = comments.get(i);

            if (comment.getDepth() == 0 && !commentsHidden.contains(comment)) {
                return i;
            }
        }

        return currentPos;
    }

    /**
     * Finds the position of the previous top level comment (hidden comments are ignored)
     *
     * @param currentPos The position to start from
     * @return The position of the previous top level comment
     */
    public int getPreviousTopLevelCommentPos(int currentPos) {
        for(int i = currentPos; i >= 0; i--) {
            RedditComment comment = comments.get(i);

            if (comment.getDepth() == 0 && !commentsHidden.contains(comment)) {
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
        // Comment not found in the list, return to avoid weird stuff potentially happening
        if (startPos == -1) {
            return;
        }

        // Update the comment selected to show that it is now a hidden comment chain
        commentsHidden.add(start);
        notifyItemChanged(startPos);

        // Remove all its replies
        List<RedditComment> replies = getShownReplies(start);
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

    /**
     * Goes through a list of comments and hides comments that have a lower score than the
     * threshold set in the preferences
     *
     * @param comments The comments to check
     */
    private void checkForHiddenComments(List<RedditComment> comments) {
        // We could store this when the adapter is created, but if we retrieve the value now
        // it's updated if the user has changed the value since the adapter was created
        int hideThreshold = App.get().getAutoHideScoreThreshold();
        comments.forEach(comment -> {
            // Hide comments if the score is below the threshold
            // Also hide comments if Reddit says they should be so (these are typically comments hidden because
            // of downvotes, but can't be determined by a threshold since they have a hidden score)
            if (comment.isCollapsed() || hideThreshold >= comment.getScore()) {
                hideComments(comment);
            }
        });
    }

    /**
     * Returns the base depth for the current list shown. If a comment chain is shown, the value returned here
     * will be the first comments depth, which should be used to calculate how many sidebars so show
     */
    public int getBaseDepth() {
        return comments.get(0).getDepth();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());

        // Create view holder based on which type of item we have
        switch (viewType) {
            case MORE_COMMETS_TYPE:
                ListItemMoreCommentBinding b = ListItemMoreCommentBinding.inflate(layoutInflater, parent, false);
                b.setAdapter(this);
                return new MoreCommentsViewHolder(b);

            case HIDDEN_COMMENT_TYPE:
                ListItemHiddenCommentBinding b1 = ListItemHiddenCommentBinding.inflate(layoutInflater, parent, false);
                b1.setAdapter(this);
                return new HiddenCommentViewHolder(b1);

            default:
            case NORMAL_COMMENT_TYPE:
                ListItemCommentBinding b2 = ListItemCommentBinding.inflate(layoutInflater, parent, false);
                b2.setPost(post);
                b2.setAdapter(this);
                return new NormalCommentViewHolder(b2);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        RedditComment comment = comments.get(position);

        switch (holder.getItemViewType()) {
            case MORE_COMMETS_TYPE:
                ((MoreCommentsViewHolder)holder).bind(comment);
                break;

            case HIDDEN_COMMENT_TYPE:
                ((HiddenCommentViewHolder)holder).bind(comment);
                break;

            default:
            case NORMAL_COMMENT_TYPE:
                ((NormalCommentViewHolder)holder).bind(comment);
                break;
        }

    }

    @Override
    public int getItemViewType(int position) {
        RedditComment comment = comments.get(position);

        if (Thing.MORE.getValue().equals(comment.getKind())) {
            return MORE_COMMETS_TYPE;
        } else if (commentsHidden.contains(comment)) {
            return HIDDEN_COMMENT_TYPE;
        } else {
            return NORMAL_COMMENT_TYPE;
        }
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }


    /**
     * Adds sidebars to the comment (to visually show the comment depth)
     *
     * @param barrier The layout to add the sidebars to
     * @param depth The depth of the comment
     */
    @BindingAdapter("sideBars")
    public static void addSideBars(Barrier barrier, int depth) {
        final String childDescription = "sidebar";
        final ConstraintLayout parent = (ConstraintLayout) barrier.getParent();

        // Find the previous sidebars and remove them
        ArrayList<View> outputViews = new ArrayList<>();
        parent.findViewsWithText(outputViews, childDescription, FIND_VIEWS_WITH_CONTENT_DESCRIPTION);
        outputViews.forEach(parent::removeView);

        // Top level comments don't have sidebars
        if (depth == 0) {
            return;
        }

        Resources res = barrier.getResources();
        int barWidth = (int)res.getDimension(R.dimen.commentSideBarWidth);
        int indent = (int)res.getDimension(R.dimen.commentDepthIndent);

        View previous = null;
        // The reference IDs the barrier will use
        int[] referenceIds = new int[depth];

        // Every comment is only responsible for the lines to its side, so each line will match up
        // with the line for the comment above and below to create a long line throughout the entire list
        for (int i = 0; i < depth; i++) {
            int id = View.generateViewId();
            referenceIds[i] = id;

            View view = new View(barrier.getContext());

            view.setBackgroundColor(ContextCompat.getColor(barrier.getContext(), R.color.commentSideBar));
            view.setContentDescription(childDescription);
            view.setId(id);

            // Set constraints
            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(parent);

            // The width of the view is set with this
            constraintSet.constrainWidth(id, barWidth);

            // With the sidebar constrained to top/bottom of parent, MATCH_CONSTRAINT height will match the parent height
            // bottom_toBottomOf=parent
            constraintSet.connect(id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
            // top_toTopOf=parent
            constraintSet.connect(id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);

            // If previous is null, set start_toStart to parent (the first sidebar), otherwise set start_toEnd to previous
            if (previous == null) {
                // start_toStartOf=parent
                constraintSet.connect(id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
            } else {
                // start_toEndOf=<previous side bar>
                constraintSet.connect(id, ConstraintSet.START, previous.getId(), ConstraintSet.END, indent);
            }

            // Last sidebar, connect the end to the end of the barrier to create a margin
            if (i == depth - 1) {
                constraintSet.connect(id, ConstraintSet.END, barrier.getId(), ConstraintSet.END, indent);
            }

            // The view has to be added to the layout BEFORE the constraints are set, otherwise they wont work
            parent.addView(view);
            previous = view;

            constraintSet.applyTo(parent);
        }

        // The barrier will move to however long out it has to, so we don't have to adjust anything
        // with the layout itself
        barrier.setReferencedIds(referenceIds);
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
     * Formats the author text based on whether or not it is posted by a and admin or a mod.
     * If no match is found, the default author color is used.
     *
     * <p>If the comment is by the poster, the typeface will always be set to bold (as well as
     * potential admin/mod)</p>
     *
     * <p>If multiple values are true, the precedence is:
     * <ol>
     *     <li>Admin</li>
     *     <li>Mod</li>
     *     <li>Poster</li>
     * </ol>
     * </p>
     *
     * @param tv The TextView to format
     * @param comment The comment the text is for
     */
    @BindingAdapter("authorTextColorComment")
    public static void formatAuthor(TextView tv, RedditComment comment) {
        formatAuthorInternal(tv, comment, false);
    }

    /**
     * Formats the author text based on whether or not it is posted by a and admin or a mod.
     * If no match is found, the default author color is used.
     *
     * <p>The text is always made italic</p>
     *
     * <p>If the comment is by the poster, the typeface will always be set to bold (as well as
     * potential admin/mod)</p>
     *
     * <p>If multiple values are true, the precedence is:
     * <ol>
     *     <li>Admin</li>
     *     <li>Mod</li>
     *     <li>Poster</li>
     * </ol>
     * </p>
     *
     * @param tv The TextView to format
     * @param comment The comment the text is for
     */
    @BindingAdapter("authorTextColorCommentWithItalic")
    public static void formatAuthorWithItalic(TextView tv, RedditComment comment) {
        formatAuthorInternal(tv, comment, true);
    }

    /**
     * Formats the author text based on whether or not it is posted by a and admin or a mod.
     * If no match is found, the default author color is used.
     *
     * <p>If the comment is by the poster, the typeface will always be set to bold (as well as
     * potential admin/mod)</p>
     *
     * <p>If multiple values are true, the precedence is:
     * <ol>
     *     <li>Admin</li>
     *     <li>Mod</li>
     *     <li>Poster</li>
     * </ol>
     * </p>
     *
     * @param tv The TextView to format
     * @param comment The comment the text is for
     */
    private static void formatAuthorInternal(TextView tv, RedditComment comment, boolean italic) {
        Typeface typface;
        if (comment.isByPoster()) {
            typface = Typeface.defaultFromStyle(italic ? Typeface.BOLD_ITALIC : Typeface.BOLD);
        } else {
            typface = Typeface.defaultFromStyle(italic ? Typeface.ITALIC : Typeface.NORMAL);
        }
        tv.setTypeface(typface);

        if (comment.isAdmin()) {
            tv.setTextColor(ContextCompat.getColor(tv.getContext(), R.color.commentByAdminBackground));
        } else if (comment.isMod()) {
            tv.setTextColor(ContextCompat.getColor(tv.getContext(), R.color.commentByModBackground));
        } else if (comment.isByPoster()) {
            tv.setTextColor(ContextCompat.getColor(tv.getContext(), R.color.opposite_background));
        } else {
            tv.setTextColor(ContextCompat.getColor(tv.getContext(), R.color.link_color));
        }
    }


    /**
     * OnClick listener for "2 more comments" comments.
     *
     * <p>Loads more comments and adds them to {@link CommentsAdapter#comments}</p>
     *
     * @param view Ignored
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
        redditApi.post(post.getId()).moreComments(comment.getChildren(), finalParent, newComments -> {
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


    /**
     * ViewHolder for comments that are "2 more comments" type comments, that will load the comments
     * when clicked
     */
    public static class MoreCommentsViewHolder extends RecyclerView.ViewHolder {
        private final ListItemMoreCommentBinding binding;

        public MoreCommentsViewHolder(ListItemMoreCommentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(RedditComment comment) {
            binding.setComment(comment);
            binding.executePendingBindings();
        }
    }

    /**
     * ViewHolder for comments that are hidden (the comments explicitly selected to be hidden)
     */
    public static class HiddenCommentViewHolder extends RecyclerView.ViewHolder {
        private final ListItemHiddenCommentBinding binding;

        public HiddenCommentViewHolder(ListItemHiddenCommentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(RedditComment comment) {
            binding.setComment(comment);
            binding.executePendingBindings();
        }
    }

    /**
     * ViewHolder for comments that are shown as the entire comment
     */
    public static class NormalCommentViewHolder extends RecyclerView.ViewHolder {
        private final ListItemCommentBinding binding;

        public NormalCommentViewHolder(ListItemCommentBinding binding) {
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

            // If the ticker has animation enabled it will animate from the previous comment to this one
            // which is very weird behaviour, so disable the animation and enable it again when we have set the comment
            binding.commentVoteBar.enableTickerAnimation(false);
            binding.commentVoteBar.setListing(comment);
            binding.commentVoteBar.enableTickerAnimation(true);

            // Execute all the bindings now, or else scrolling/changes to the dataset will have a
            // small, but noticeable delay, causing the old comment to still appear
            binding.executePendingBindings();
        }
    }
}
