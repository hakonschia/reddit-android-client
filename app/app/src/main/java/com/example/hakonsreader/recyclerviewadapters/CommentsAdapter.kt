package com.example.hakonsreader.recyclerviewadapters

import android.graphics.Typeface
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.constraintlayout.widget.Barrier
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.api.enums.Thing
import com.example.hakonsreader.api.model.RedditComment
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.databinding.ListItemCommentBinding
import com.example.hakonsreader.databinding.ListItemHiddenCommentBinding
import com.example.hakonsreader.databinding.ListItemMoreCommentBinding
import com.example.hakonsreader.interfaces.LoadMoreComments
import com.example.hakonsreader.interfaces.OnReplyListener
import com.example.hakonsreader.recyclerviewadapters.diffutils.CommentsDiffCallback
import com.example.hakonsreader.recyclerviewadapters.menuhandlers.showPopupForCommentExtraForLoggedInUser
import com.example.hakonsreader.recyclerviewadapters.menuhandlers.showPopupForCommentExtraForNonLoggedInUser
import com.example.hakonsreader.views.util.ViewUtil

/**
 * Adapter for a RecyclerView populated with [RedditComment] objects. This adapter
 * supports three different comment layouts:
 * * Normal comments
 * * Hidden comments
 * * "More" comments (eg. "2 more comments")
 */
class CommentsAdapter(private val post: RedditPost) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        /**
         * The value returned from [getItemViewType] when the comment is
         * a "more comments" comment
         */
        private const val MORE_COMMENTS_TYPE = 0

        /**
         * The value returned from [getItemViewType] when the comment is
         * a normal comment
         */
        private const val NORMAL_COMMENT_TYPE = 1

        /**
         * The value returned from [getItemViewType] when the comment is
         * a hidden comment
         */
        private const val HIDDEN_COMMENT_TYPE = 2
    }

    /**
     * The list of comments that should be shown, unless a comment chain is set to be shown.
     *
     * This list might not include all comments, as comments that are hidden
     * ([RedditComment.isCollapsed]) will not have its children in this list
     */
    private var comments = ArrayList<RedditComment>()

    /**
     * If [commentIdChain] is set, this list will hold the chain of comments
     * that should be shown
     */
    private var chain = ArrayList<RedditComment>()

    /**
     * The list of comments shown when a chain is shown. This will be used to go back to the comments
     * previously shown when the user wants to get out of a chain
     */
    private var commentsShownWhenChainSet: ArrayList<RedditComment>? = null

    /**
     * The timestamp in milliseconds of when the post was opened last
     */
    var lastTimeOpened = -1L

    /**
     * The ID of the comment to show in a comment chain (the start of the chain)
     *
     * Setting the value for this will automatically update the comments shown
     */
    var commentIdChain = ""
        set(value) {
            // Don't do anything if the id is the same, because why would you
            if (field.equals(value, ignoreCase = true)) {
                return
            }
            field = value

            setChain(comments)
        }

    /**
     * The layout state at the time when a comment chain was set. This can be used to restore
     * the state when going back to all comments
     */
    private var layoutStateWhenChainShown: Parcelable? = null

    /**
     * The listener for when the reply button has been clicked on an item
     */
    var replyListener: OnReplyListener? = null

    /**
     * The listener for when a "3 more comments" comment has been clicked
     */
    var loadMoreCommentsListener: LoadMoreComments? = null

    /**
     * The listener to run when a comment chain has been shown
     */
    var onChainShown: Runnable? = null

    /**
     * The RecyclerView this adapter is attached to
     */
    private var recyclerViewAttachedTo: RecyclerView? = null


    /**
     * Sets the comments to use in the list
     *
     * @param newComments The comments to add
     */
    fun submitList(newComments: List<RedditComment>) {
        val previous = comments
        comments = newComments as ArrayList<RedditComment>

        checkAndSetHiddenComments()

        if (commentIdChain.isNotEmpty()) {
            setChain(newComments)
        } else {
            DiffUtil.calculateDiff(
                    CommentsDiffCallback(previous, comments)
            ).dispatchUpdatesTo(this)
        }
    }

    /**
     * Notifies that a comment has been updated
     *
     * @param comment The comment updated
     */
    fun notifyItemChanged(comment: RedditComment) {
        val pos = comments.indexOf(comment)
        if (pos != -1) {
            notifyItemChanged(pos)
        }
    }

    /**
     * Removes all comments from the list
     */
    fun clearComments() {
        val size = comments.size
        comments.clear()
        notifyItemRangeRemoved(0, size)
    }

    /**
     * Goes through [comments] and checks if a comments score is below the users threshold or if
     * Reddit has specified that it should be hidden.
     *
     * Comments with [RedditComment.isCollapsed] set to true children are removed
     */
    private fun checkAndSetHiddenComments() {
        val commentsToRemove: MutableList<RedditComment> = java.util.ArrayList()
        val hideThreshold = App.get().autoHideScoreThreshold

        comments.forEach { comment: RedditComment ->
            if (hideThreshold >= comment.score || comment.isCollapsed) {
                // If we got here from the score threshold make sure collapsed is set to true
                comment.isCollapsed = true
                commentsToRemove.addAll(getShownReplies(comment))
            }
        }

        // We can't modify the comments list while looping over it, so we have to store the comments
        // that should be removed and remove them afterwards
        comments.removeAll(commentsToRemove)
    }

    /**
     * Sets [chain] based on [commentIdChain]
     *
     * If the chain is found, the currently shown list is stored in [commentsShownWhenChainSet]
     *
     * This function calls [notifyDataSetChanged]
     *
     * @param commentsToLookIn The comments to look in
     */
    private fun setChain(commentsToLookIn: List<RedditComment>) {
        // TODO this is bugged when in a chain and going into a new chain
        // TODO the commentsShownWhenChainSet aren't updated with new comments loaded while in the chain

        if (commentIdChain.isNotEmpty()) {
            // We have to clear the list here. In case the comment isn't found every comment should be shown
            val previousChainSize = chain.size
            chain.clear()

            for (comment in commentsToLookIn) {
                if (comment.id.equals(commentIdChain, ignoreCase = true)) {
                    chain = getShownReplies(comment) as ArrayList<RedditComment>

                    // The actual comment must also be added at the start
                    chain.add(0, comment)

                    // This list should take us back to all the comments, so if we're setting a chain
                    // from within a chain, don't store the list
                    if (previousChainSize == 0) {
                        layoutStateWhenChainShown = recyclerViewAttachedTo?.layoutManager?.onSaveInstanceState()
                        commentsShownWhenChainSet = comments
                    }
                    comments = chain
                    onChainShown?.run()

                    break
                }
            }
        } else {
            chain.clear()
        }

        // Go back to before the chain was set
        if (chain.isEmpty() && commentsShownWhenChainSet != null) {
            comments = commentsShownWhenChainSet as ArrayList<RedditComment>
            if (layoutStateWhenChainShown != null) {
                recyclerViewAttachedTo?.layoutManager?.onRestoreInstanceState(layoutStateWhenChainShown)
            }
        }

        notifyDataSetChanged()
    }


    /**
     * Shows a comment chain that has previously been hidden
     *
     * @param start The start of the chain
     * @see hideComments
     */
    fun showComments(start: RedditComment) {
        val pos = comments.indexOf(start)

        start.isCollapsed = false
        notifyItemChanged(pos)

        val replies = getShownReplies(start)
        comments.addAll(pos + 1, replies)
        notifyItemRangeInserted(pos + 1, replies.size)
    }

    /**
     * Hides comments from being shown
     *
     * @param start The comment to start at. This element will be updated to a [HIDDEN_COMMENT_TYPE]
     * and all its children will be removed from [comments]
     * @see showComments
     */
    fun hideComments(start: RedditComment) {
        val startPos = comments.indexOf(start)
        if (startPos == -1) {
            return
        }

        start.isCollapsed = true

        val replies = getShownReplies(start)
        comments.removeAll(replies)

        // The comment explicitly hidden isn't being removed, but its UI is updated
        // Its children are removed from the list
        notifyItemChanged(startPos)
        notifyItemRangeRemoved(startPos + 1, replies.size)
    }

    /**
     * Retrieve the list of replies to a comment that are shown
     *
     * @param parent The parent to retrieve replies for
     * @return The list of children of [parent] that are shown. Children of children are also
     * included in the list
     */
    private fun getShownReplies(parent: RedditComment) : List<RedditComment> {
        val replies = ArrayList<RedditComment>()

        parent.replies.forEach {
            // Only add direct children, let the children handle their children
            if (it.depth - 1 == parent.depth) {
                replies.add(it)

                // Reply isn't hidden which means it potentially has children to show
                if (!it.isCollapsed) {
                    replies.addAll(getShownReplies(it))
                }
            }
        }

        return replies
    }


    /**
     * Find the position of the next top level comment
     *
     * @param currentPos The position to start looking at
     * @return The position of the next top level comment, or [currentPos] if there are no more top level comments
     *
     * @see getPreviousTopLevelCommentPos
     */
    fun getNextTopLevelCommentPos(currentPos: Int) : Int {
        for (i in currentPos until comments.size) {
            if (comments[i].depth == 0) {
                return i
            }
        }

        return currentPos
    }

    /**
     * Finds the position of the previous top level comment
     *
     * @param currentPos The position to start from
     * @return The position of the previous top level comment, or [currentPos] if there are no more top level comments
     *
     * @see getNextTopLevelCommentPos
     */
    fun getPreviousTopLevelCommentPos(currentPos: Int) : Int {
        for (i in currentPos downTo 0) {
            if (comments[i].depth == 0) {
                return i
            }
        }

        return currentPos
    }

    /**
     * Returns the base depth for the current list shown. The value returned here
     * will be the first comments depth, which should be used to calculate how many sidebars so show
     */
    fun getBaseDepth() = comments[0].depth


    /**
     * LongClick listener for views
     *
     * @param view Ignored
     * @param comment The comment clicked
     * @return True (event is always consumed)
     */
    fun hideCommentsLongClick(view: View, comment: RedditComment): Boolean {
        hideComments(comment)
        return true
    }

    /**
     * LongClick listener for text views. Hides a comment chain
     *
     * @param view The view clicked. Note this must be a [TextView], or else the function will not
     * do anything but consume the event
     * @param comment The comment clicked
     * @return True (event is always consumed)
     */
    fun hideCommentsLongClickText(view: View, comment: RedditComment): Boolean {
        if (view is TextView) {
            // Not a hyperlink (even long clicking on the hyperlink would open it, so don't collapse as well)
            if (view.selectionStart == -1 && view.selectionEnd == -1) {
                hideComments(comment)
            }
        }
        return true
    }

    /**
     * OnClick listener for "2 more comments" comments.
     *
     * [loadMoreCommentsListener] will be called to load the comments
     *
     * @param view Ignored
     * @param comment The comment to load from. This comment has to be a "2 more comments" comment
     */
    @BindingAdapter("getMoreComments")
    fun getMoreComments(view: View, comment: RedditComment) {
        val pos = comments.indexOf(comment)
        val depth = comment.depth

        // The parent is the first comment upwards in the list that has a lower depth
        var parent: RedditComment? = null

        // On posts with a lot of comments the last comment is often a "771 more comments" which is a
        // top level comment, which means it won't have a parent so it's no point in trying to find it
        if (depth != 0) {
            for (i in pos - 1 downTo 0) {
                val c = comments[i]
                if (c.depth < depth) {
                    parent = c
                    break
                }
            }
        }

        // Could be null, but that's something we would want to discover when debugging
        loadMoreCommentsListener!!.loadMoreComments(comment, parent)
    }

    /**
     * OnClick listener for the "more" button on a normal comment. This will show a popup menu with
     * extra options such as saving the comment, or deleting it
     */
    fun moreOnClick(view: View, comment: RedditComment?) {
        if (comment == null) {
            return
        }

        // We could check if privately browsing. Mod/save etc. won't appear anyways, only delete comment
        // since we only have the user, the API doesn't send back that data since the access token is anonymous
        if (App.get().isUserLoggedIn) {
            showPopupForCommentExtraForLoggedInUser(view, comment, this)
        } else {
            showPopupForCommentExtraForNonLoggedInUser(view, comment, this)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val comment = comments[position]

        val highlight = position == 0 && chain.isNotEmpty() // Always highlight first comment in a chain
                // User wants to highlight new comments, and the comment was added after the last time the post was opened
                || (App.get().highlightNewComments() && (lastTimeOpened > 0 && comment.createdAt > lastTimeOpened))

        val byLoggedInUser = comment.author == App.getStoredUser()?.username

        when (holder.itemViewType) {
            MORE_COMMENTS_TYPE -> (holder as MoreCommentsViewHolder).bind(comment)
            HIDDEN_COMMENT_TYPE -> (holder as HiddenCommentViewHolder).bind(comment, highlight, byLoggedInUser)
            NORMAL_COMMENT_TYPE -> (holder as NormalCommentViewHolder).bind(comment, highlight, byLoggedInUser)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            MORE_COMMENTS_TYPE -> {
                val binding = ListItemMoreCommentBinding.inflate(layoutInflater, parent, false)
                binding.adapter = this
                MoreCommentsViewHolder(binding)
            }
            HIDDEN_COMMENT_TYPE -> {
                val binding = ListItemHiddenCommentBinding.inflate(layoutInflater, parent, false)
                binding.adapter = this
                HiddenCommentViewHolder(binding)
            }
            else -> {
                val binding = ListItemCommentBinding.inflate(layoutInflater, parent, false)
                binding.adapter = this
                binding.post = post
                NormalCommentViewHolder(binding)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val comment = comments[position]

        return when {
            comment.kind == Thing.MORE.value -> MORE_COMMENTS_TYPE
            comment.isCollapsed -> HIDDEN_COMMENT_TYPE
            else -> NORMAL_COMMENT_TYPE
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        recyclerViewAttachedTo = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        recyclerViewAttachedTo = null
    }

    override fun getItemCount() = comments.size


    /**
     * ViewHolder for comments that are shown as the entire comment
     */
    inner class NormalCommentViewHolder(private val binding: ListItemCommentBinding) : RecyclerView.ViewHolder(binding.root) {
        /**
         * Binds the ViewHolder to a comment
         *
         * @param comment The comment to bind
         * @param highlight True if the comment should have a slight highlight around it
         */
        fun bind(comment: RedditComment, highlight: Boolean, byLoggedInUser: Boolean) {
            binding.comment = comment
            binding.highlight = highlight
            binding.isByLoggedInUser = byLoggedInUser

            // If the ticker has animation enabled it will animate from the previous comment to this one
            // which is very weird behaviour, so disable the animation and enable it again when we have set the comment
            binding.commentVoteBar.enableTickerAnimation(false)
            binding.commentVoteBar.listing = comment
            binding.commentVoteBar.enableTickerAnimation(true)

            // Execute all the bindings now, or else scrolling/changes to the dataset will have a
            // small, but noticeable delay, causing the old comment to still appear
            binding.executePendingBindings()
        }
    }

    /**
     * ViewHolder for comments that are hidden (the comments explicitly selected to be hidden)
     */
    inner class HiddenCommentViewHolder(private val binding: ListItemHiddenCommentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(comment: RedditComment, highlight: Boolean, byLoggedInUser: Boolean) {
            binding.root.setOnClickListener {
                showComments(comment)
            }

            binding.comment = comment
            binding.highlight = highlight
            binding.isByLoggedInUser = byLoggedInUser
            binding.executePendingBindings()
        }
    }

    /**
     * ViewHolder for comments that are "2 more comments" type comments, that will load the comments
     * when clicked
     */
    inner class MoreCommentsViewHolder(private val binding: ListItemMoreCommentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(comment: RedditComment?) {
            binding.comment = comment
            binding.executePendingBindings()
        }
    }
}

/**
 * Formats the author text based on whether or not it is posted by an admin or a mod.
 * If no match is found, the default author color is used.
 *
 * If the comment is by the poster, the typeface will always be set to bold (as well as
 * potential admin/mod)
 *
 * If the comment is made by an admin and a mod, the precedence is:
 * * Admin
 * * Mod
 *
 * @param tv The TextView to format
 * @param comment The comment the text is for
 */
@BindingAdapter("authorTextColorComment")
fun formatAuthor(tv: TextView, comment: RedditComment) {
    formatAuthorInternal(tv, comment, false)
}

/**
 * Formats the author text based on whether or not it is posted by an admin or a mod.
 * If no match is found, the default author color is used.
 *
 * If the comment is by the poster, the typeface will always be set to bold (as well as
 * potential admin/mod)
 *
 * If the comment is made by an admin and a mod, the precedence is:
 * * Admin
 * * Mod
 *
 * @param tv The TextView to format
 * @param comment The comment the text is for
 */
@BindingAdapter("authorTextColorCommentWithItalic")
fun formatAuthorWithItalic(tv: TextView, comment: RedditComment) {
    formatAuthorInternal(tv, comment, true)
}

private fun formatAuthorInternal(tv: TextView, comment: RedditComment, italic: Boolean) {
    tv.typeface = if (comment.isByPoster) {
        // Comments by posters should always be bold
        Typeface.defaultFromStyle(
                if (italic) Typeface.BOLD_ITALIC else Typeface.BOLD
        )
    } else {
        Typeface.defaultFromStyle(
                if (italic) Typeface.ITALIC else Typeface.NORMAL
        )
    }

    tv.setTextColor(ContextCompat.getColor(tv.context, when {
        comment.isAdmin() -> R.color.commentByAdminBackground
        comment.isMod() -> R.color.commentByModBackground
        comment.isByPoster -> R.color.opposite_background
        else -> R.color.link_color
    }))
}

/**
 * Adds the authors flair to the comment. If the author has no flair the view is set to [View.GONE]
 *
 * @param view The view that holds the author flair
 * @param comment The comment
 */
@BindingAdapter("authorFlair")
fun addAuthorFlair(view: FrameLayout, comment: RedditComment?) {
    if (comment == null) {
        return
    }
    val tag = ViewUtil.createFlair(
            comment.authorRichtextFlairs,
            comment.authorFlairText,
            comment.authorFlairTextColor,
            comment.authorFlairBackgroundColor,
            view.context
    )

    view.visibility = if (tag != null) {
        // The view might still have old views
        view.removeAllViews()
        view.addView(tag)

        View.VISIBLE
    } else {
        // No author flair, remove the view so it doesn't take up space
       View.GONE
    }
}


/**
 * Adds sidebars to a ConstraintLayout to visually show the comment depth
 *
 * @param barrier The layout to add the sidebars to
 * @param depth The depth of the comment
 */
@BindingAdapter("sidebars")
fun addSidebars(barrier: Barrier, depth: Int) {
    val parent = barrier.parent as ConstraintLayout
    // The contentDescription for the sidebars, this is used to find the sidebars again later
    val contentDescription = "sidebar"
    val res = barrier.resources
    val barWidth = res.getDimension(R.dimen.commentSideBarWidth).toInt()
    val indent = res.getDimension(R.dimen.commentDepthIndent).toInt()

    // Find the previous sidebars added to the ConstraintLayout
    // By doing this we can reuse views by removing only the overflow amount/only adding the extra
    // needed. When scrolling through a lot of comments this will save 100s of views from being created
    val previousSidebars = java.util.ArrayList<View>()
    parent.findViewsWithText(previousSidebars, contentDescription, View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION)
    val previousSidebarsSize = previousSidebars.size

    when {
        // Top level comments don't have sidebars, remove all previous
        depth == 0 -> {
            previousSidebars.forEach{ view: View -> parent.removeView(view) }
            // If the barrier doesn't have any views referenced to it hidden comments will
            // expand out from the right side, so ensure it has something referenced
            barrier.referencedIds = intArrayOf(R.id.emptyView)
            return
        }

        // The depth is the same, we can keep the previous sidebars (do nothing)
        previousSidebarsSize == depth -> {
            return
        }

        // Too many sidebars, remove the overflow
        previousSidebarsSize > depth -> {
            removeSidebarsOverflow(previousSidebars, parent, barrier, previousSidebarsSize - depth, indent)
            return
        }
    }


    // If we get here we need to add sidebars

    // ConstraintSet for the constraints of the sidebars
    val constraintSet = ConstraintSet()

    // The previous sidebar
    var previous: View? = null

    // Remove the constraint of the last sidebar as that is constrained to the barrier
    // Get the last side bar kept and constrain it to the barrie
    // The last sidebar is constrained "end_toEndOf=barrier", that has to be removed as this
    // sidebar should now be constrained "end_toStartOf=nextSideBar"
    if (previousSidebarsSize > 0) {
        val lastSidebar = previousSidebars[previousSidebars.size - 1]

        // Set constraints
        constraintSet.clone(parent)
        constraintSet.clear(lastSidebar.id, ConstraintSet.END)
        previous = lastSidebar
    }

    // The reference IDs the barrier will use
    val referenceIds = IntArray(depth)

    // Copy the IDs
    for (i in previousSidebars.indices) {
        referenceIds[i] = previousSidebars[i].id
    }

    // The downside to keeping the same views is that since the comments can be different type of
    // layouts (normal comment, hidden comment, more comment) ConstraintSet will give out warnings
    // that an id is unknown when the sidebars were previously in a different layout
    // ("id unknown sideBarsBarrier", "id unknown normalComment") since those IDs aren't found
    // in this layout (the sidebars are constrained to the parent/barrier from the previous layout
    // which are different views).
    // It doesn't seem to cause any issues, but the logcat gets spammed with the warnings

    // Every comment is only responsible for the lines to its side, so each line will match up
    // with the line for the comment above and below to create a long line throughout the entire list
    for (i in previousSidebarsSize until depth) {
        val id = View.generateViewId()
        referenceIds[i] = id
        val view = View(barrier.context)
        view.setBackgroundColor(ContextCompat.getColor(barrier.context, R.color.commentSideBar))
        view.contentDescription = contentDescription
        view.id = id

        // The width of the view is set with this
        constraintSet.constrainWidth(id, barWidth)

        // With the sidebar is constrained to top/bottom of parent, MATCH_CONSTRAINT height will match the parent height
        // bottom_toBottomOf=parent
        constraintSet.connect(id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        // top_toTopOf=parent
        constraintSet.connect(id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)

        // If previous is null, set start_toStart to parent (the first sidebar), otherwise set start_toEnd to previous
        if (previous == null) {
            // start_toStartOf=parent
            constraintSet.connect(id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        } else {
            // start_toEndOf=<previous side bar>
            constraintSet.connect(id, ConstraintSet.START, previous.id, ConstraintSet.END, indent)
        }

        // Last sidebar, connect the end to the end of the barrier to create a margin from the last sidebar
        // to the comment itself
        if (i == depth - 1) {
            constraintSet.connect(id, ConstraintSet.END, barrier.id, ConstraintSet.END, indent)
        }
        parent.addView(view)
        previous = view
    }

    // Apply all the constraints
    constraintSet.applyTo(parent)

    // The barrier will move to however long out it has to, so we don't have to adjust anything
    // with the layout itself
    barrier.referencedIds = referenceIds
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
private fun removeSidebarsOverflow(sideBars: MutableList<View>, parent: ConstraintLayout, barrier: Barrier, sideBarsToRemove: Int, indent: Int) {
    val size = sideBars.size
    for (i in size downTo size - sideBarsToRemove + 1) {
        val sideBar: View = sideBars.removeAt(i - 1)
        parent.removeView(sideBar)
    }

    // Get the last side bar kept and constrain it to the barrier
    val lastSideBar = sideBars[sideBars.size - 1]

    // Set constraints
    val constraintSet = ConstraintSet()
    constraintSet.clone(parent)
    constraintSet.connect(lastSideBar.id, ConstraintSet.END, barrier.id, ConstraintSet.END, indent)
    constraintSet.applyTo(parent)
}