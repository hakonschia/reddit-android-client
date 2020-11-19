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
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.api.enums.Thing;
import com.example.hakonsreader.api.model.RedditComment;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.databinding.ListItemCommentBinding;
import com.example.hakonsreader.databinding.ListItemHiddenCommentBinding;
import com.example.hakonsreader.databinding.ListItemMoreCommentBinding;
import com.example.hakonsreader.interfaces.LoadMoreComments;
import com.example.hakonsreader.interfaces.OnReplyListener;
import com.example.hakonsreader.misc.InternalLinkMovementMethod;
import com.example.hakonsreader.recyclerviewadapters.diffutils.CommentsDiffCallback;
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

    /**
     * The value returned from {@link CommentsAdapter#getItemViewType(int)} when the comment is
     * a "more comments" comment
     */
    private static final int MORE_COMMENTS_TYPE = 0;

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


    /**
     * The list of comments that should be shown, unless a comment chain is set to be shown.
     * This list might not include all comments, as comments that are hidden
     * ({@link RedditComment#isCollapsed()}) will not have its children in this list
     */
    private List<RedditComment> comments = new ArrayList<>();

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
    private RecyclerView recyclerViewAttachedTo;
    private String commentIdChain;
    private OnReplyListener replyListener;
    private LoadMoreComments loadMoreCommentsListener;


    /**
     * @param post The post the comment is for
     */
    public CommentsAdapter(RedditPost post) {
        this.post = post;
    }

    /**
     * Sets the listener for when "2 more comments" comments in the list are clicked
     *
     * @param loadMoreCommentsListener The callback to set
     */
    public void setLoadMoreCommentsListener(LoadMoreComments loadMoreCommentsListener) {
        this.loadMoreCommentsListener = loadMoreCommentsListener;
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
     * @return The reply listener set on the adapter
     */
    public OnReplyListener getReplyListener() {
        return replyListener;
    }

    /**
     * Appends comments to the current list of comments
     *
     * @param newComments The comments to add
     */
    public void addComments(List<RedditComment> newComments) {
        List<RedditComment> previous = comments;
        comments = newComments;

        checkAndSetHiddenComments();

        if (commentIdChain != null && !commentIdChain.isEmpty()) {
            setChain(newComments);
        } else {
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                    new CommentsDiffCallback(previous, comments)
            );
            diffResult.dispatchUpdatesTo(this);
        }
    }

    /**
     * Goes through {@link CommentsAdapter#comments} and checks if a comments score is below
     * the users threshold or if Reddit has specified that it should be hidden.
     *
     * <p>Comments hidden set {@link RedditComment#setCollapsed(boolean)} to true and all its
     * children are removed from {@link CommentsAdapter#comments}</p>
     */
    private void checkAndSetHiddenComments() {
        List<RedditComment> commentsToRemove = new ArrayList<>();
        int hideThreshold = App.get().getAutoHideScoreThreshold();
        comments.forEach(comment -> {
            if (hideThreshold >= comment.getScore() || comment.isCollapsed()) {
                // If we got here from the score threshold make sure collapsed is set to true
                comment.setCollapsed(true);
                commentsToRemove.addAll(getShownReplies(comment));
            }
        });

        // We can't modify the comments list while looping over it, so we have to store the comments
        // that should be removed and remove them afterwards
        comments.removeAll(commentsToRemove);
    }


    /**
     * Sets {@link CommentsAdapter#chain} based on {@link CommentsAdapter#commentIdChain}.
     * If the chain is found, the currently shown list is stored in {@link CommentsAdapter#commentsShownWhenChainSet}.
     *
     * <p>This function calls {@link RecyclerView.Adapter#notifyDataSetChanged()}</p>
     *
     * @param commentsToLookIn The comments to look in
     */
    private void setChain(List<RedditComment> commentsToLookIn) {
        // TODO this is bugged when in a chain and going into a new chain
        // TODO the commentsShownWhenChainSet aren't updated with new comments loaded while in the chain

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

        notifyDataSetChanged();

        if (recyclerViewAttachedTo != null) {
            recyclerViewAttachedTo.getLayoutManager().scrollToPosition(0);
        }
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

            if (comment.getDepth() == 0 && !comment.isCollapsed()) {
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

            if (comment.getDepth() == 0 && !comment.isCollapsed()) {
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

        start.setCollapsed(true);

        // Remove all its replies
        List<RedditComment> replies = getShownReplies(start);
        comments.removeAll(replies);

        // The comment explicitly hidden isn't being removed, but its UI is updated
        // Its children are removed from the list
        notifyItemChanged(startPos);
        notifyItemRangeRemoved(startPos + 1, replies.size());
    }

    /**
     * Shows a comment chain that has previously been hidden
     *
     * @param start The start of the chain
     */
    public void showComments(RedditComment start) {
        int pos = comments.indexOf(start);

        start.setCollapsed(false);
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
                if (!reply.isCollapsed()) {
                    replies.addAll(getShownReplies(reply));
                }
            }
        }

        return replies;
    }

    /**
     * Returns the base depth for the current list shown. The value returned here
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
            case MORE_COMMENTS_TYPE:
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

        // If we're in a chain and on the first element (the start of the chain) then highlight it
        boolean highlight = position == 0 && !chain.isEmpty();

        switch (holder.getItemViewType()) {
            case MORE_COMMENTS_TYPE:
                ((MoreCommentsViewHolder)holder).bind(comment);
                break;

            case HIDDEN_COMMENT_TYPE:
                ((HiddenCommentViewHolder)holder).bind(comment, highlight);
                break;

            default:
            case NORMAL_COMMENT_TYPE:
                ((NormalCommentViewHolder)holder).bind(comment, highlight);
                break;
        }

    }

    @Override
    public int getItemViewType(int position) {
        RedditComment comment = comments.get(position);

        if (Thing.MORE.getValue().equals(comment.getKind())) {
            return MORE_COMMENTS_TYPE;
        } else if (comment.isCollapsed()) {
            return HIDDEN_COMMENT_TYPE;
        } else {
            return NORMAL_COMMENT_TYPE;
        }
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        recyclerViewAttachedTo = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        recyclerViewAttachedTo = null;
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

        Resources res = barrier.getResources();
        final int barWidth = (int)res.getDimension(R.dimen.commentSideBarWidth);
        final int indent = (int)res.getDimension(R.dimen.commentDepthIndent);

        // Find the previous sidebars
        ArrayList<View> previousSideBars = new ArrayList<>();
        parent.findViewsWithText(previousSideBars, childDescription, FIND_VIEWS_WITH_CONTENT_DESCRIPTION);

        int previousSideBarsSize = previousSideBars.size();
        
        // Top level comments don't have sidebars, remove all previous
        if (depth == 0) {
            previousSideBars.forEach(parent::removeView);
            Log.d(TAG, "addSideBars: Removing all sidebars");
            return;
        } else if (previousSideBarsSize == depth) {

            Log.d(TAG, "addSideBars: Correct amount of sidebars already added (" + depth + ")");
            // The depth is the same, we can keep the previous sidebars
            return;
        } else {
            Log.d(TAG, "addSideBars: previous=" + previousSideBarsSize + ", depth=" + depth);

            // Too many sidebars, remove the overflow
            if (previousSideBarsSize > depth) {
                Log.d(TAG, "addSideBars: Removing overflow, previous="+previousSideBarsSize + ", depth=" + depth);
                removeSideBarsOverflow(previousSideBars, parent, barrier, previousSideBarsSize - depth, indent);
                return;
            } else {
                // For now, remove all and the correct amount will be added below
                previousSideBars.forEach(parent::removeView);
                Log.d(TAG, "addSideBars: Adding new sidebars, previous="+previousSideBarsSize + ", depth=" + depth);
            }

        }

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
     * Removes overflow side bars from a ConstraintLayout
     *
     * @param sideBars The list of the side bars. The side bars are removed from this list
     * @param parent The parent layout where the side bars are added. The side bars are removed from the layout
     * @param barrier The barrier the side bars are referenced/constrained to
     * @param sideBarsToRemove The amount of side bars to remove
     * @param indent The indent to use for the side bars
     */
    private static void removeSideBarsOverflow(List<View> sideBars, ConstraintLayout parent, Barrier barrier, int sideBarsToRemove, int indent) {
        int size = sideBars.size();
        for (int i = size; i > size - sideBarsToRemove; i--) {
            View sideBar = sideBars.remove(i - 1);
            parent.removeView(sideBar);
        }

        // Get the last side bar kept and constrain it to the barrier
        View lastSideBar = sideBars.get(sideBars.size() - 1);

        // Set constraints
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(parent);
        constraintSet.connect(lastSideBar.getId(), ConstraintSet.END, barrier.getId(), ConstraintSet.END, indent);
        constraintSet.applyTo(parent);
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

        // Could be null, but that's something we would want to discover when debugging
        loadMoreCommentsListener.loadMoreComments(comment, parent);
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

        /**
         * Binds the ViewHolder to a comment
         *
         * @param comment The comment to bind
         * @param highlight True if the comment should have a slight highlight around it
         */
        public void bind(RedditComment comment, boolean highlight) {
            binding.setComment(comment);
            binding.setHighlight(highlight);
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
         * Binds the ViewHolder to a comment
         *
         * @param comment The comment to bind
         * @param highlight True if the comment should have a slight highlight around it
         */
        public void bind(RedditComment comment, boolean highlight) {
            binding.setComment(comment);
            binding.setHighlight(highlight);

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
