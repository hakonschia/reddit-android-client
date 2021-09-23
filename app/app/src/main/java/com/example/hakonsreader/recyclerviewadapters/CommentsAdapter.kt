package com.example.hakonsreader.recyclerviewadapters

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.text.Spannable
import android.text.style.URLSpan
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.constraintlayout.widget.Barrier
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.text.toSpannable
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.hakonsreader.R
import com.example.hakonsreader.activities.DispatcherActivity
import com.example.hakonsreader.activities.PostActivity
import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.enums.Thing
import com.example.hakonsreader.api.model.RedditComment
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.databinding.ListItemCommentBinding
import com.example.hakonsreader.databinding.ListItemHiddenCommentBinding
import com.example.hakonsreader.databinding.ListItemMoreCommentBinding
import com.example.hakonsreader.interfaces.OnReplyListener
import com.example.hakonsreader.interfaces.OnReportsIgnoreChangeListener
import com.example.hakonsreader.misc.CreateIntentOptions
import com.example.hakonsreader.misc.InternalLinkMovementMethod
import com.example.hakonsreader.states.AppState
import com.example.hakonsreader.misc.Settings
import com.example.hakonsreader.misc.createIntent
import com.example.hakonsreader.recyclerviewadapters.diffutils.CommentsDiffCallback
import com.example.hakonsreader.recyclerviewadapters.menuhandlers.showPopupForComments
import com.example.hakonsreader.viewmodels.CommentsViewModel
import com.example.hakonsreader.views.LinkPreview
import com.example.hakonsreader.views.util.setLongClickToPeekUrl

/**
 * Adapter for a RecyclerView populated with [RedditComment] objects. This adapter
 * supports three different comment layouts:
 * * Normal comments
 * * Hidden comments
 * * "More" comments (eg. "2 more comments")
 */
class CommentsAdapter constructor(
        private val api: RedditApi,
        private val viewModel: CommentsViewModel,
        private val settings: Settings
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        @Suppress("UNUSED")
        private const val TAG = "CommentsAdapter"

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
     * The list of colors to use for sidebars
     */
    // We only need to get this when loading the adapter, as it will be the same for all comments
    // and cannot change during the adapters life
    private val sidebarColors = settings.commentSidebarColors()

    /**
     * The list of comments shown in the adapter
     */
    private var comments: List<RedditComment> = ArrayList()

    /**
     * The ID of the chain the adapter is currently showing. This will be initialized to chain ID
     * in [viewModel], but is not guaranteed to be synced afterwards
     */
    var currentChainId: String? = viewModel.chainId
        private set

    /**
     * The post the comments are for. This should be set before comments are shown
     */
    var post: RedditPost? = null

    /**
     * The timestamp in milliseconds of when the post was opened last
     */
    var lastTimeOpened = -1L

    /**
     * The listener for when the reply button has been clicked on an item
     */
    var replyListener: OnReplyListener? = null

    /**
     * Sets the comments to use in the list
     *
     * @param newComments The comments to add
     */
    fun submitList(newComments: List<RedditComment>) {
        val previous = comments
        comments = newComments

        // If a new chain is being shown we need to update the entire list
        // This isn't really ideal, but DiffUtil currently doesn't have a way of differentiating the items
        // since they don't change when a chain is shown, so they won't be updated correctly (depth and highlighting)
        if (currentChainId != viewModel.chainId) {
            currentChainId = viewModel.chainId
            notifyDataSetChanged()
        } else {
            DiffUtil.calculateDiff(
                CommentsDiffCallback(previous, newComments)
            ).dispatchUpdatesTo(this)
        }
    }

    /**
     * Notifies that a comment has been updated
     *
     * @param comment The comment updated
     */
    @UiThread
    fun notifyItemChanged(comment: RedditComment) {
        val pos = comments.indexOf(comment)
        if (pos != -1) {
            notifyItemChanged(pos)
        }
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
     * OnClick listener for the "more" button on a normal comment. This will show a popup menu with
     * extra options such as saving the comment, or deleting it
     */
    fun moreOnClick(view: View, comment: RedditComment?) {
        if (comment == null) {
            return
        }

        showPopupForComments(view, comment, this, api, viewModel)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val comment = comments[position]

        // Highlight the root comment in a chain
        val highlight = comment.id == viewModel.chainId
                // User wants to highlight new comments, and the comment was added after the last time the post was opened
                || (settings.highlightNewComments() && (lastTimeOpened > 0 && comment.createdAt > lastTimeOpened))

        val byLoggedInUser = comment.author == AppState.getUserInfo()?.userInfo?.username

        when (holder.itemViewType) {
            MORE_COMMENTS_TYPE -> (holder as MoreCommentsViewHolder).bind(comment, settings.showAllSidebars())
            HIDDEN_COMMENT_TYPE -> (holder as HiddenCommentViewHolder).bind(comment, highlight, byLoggedInUser, settings.showAllSidebars())
            NORMAL_COMMENT_TYPE -> (holder as NormalCommentViewHolder).bind(comment, highlight, byLoggedInUser, settings.showAllSidebars())
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            MORE_COMMENTS_TYPE -> {
                MoreCommentsViewHolder(ListItemMoreCommentBinding.inflate(layoutInflater, parent, false).apply {
                    viewModel = this@CommentsAdapter.viewModel
                })
            }
            HIDDEN_COMMENT_TYPE -> {
                HiddenCommentViewHolder(ListItemHiddenCommentBinding.inflate(layoutInflater, parent, false).apply {
                    viewModel = this@CommentsAdapter.viewModel
                })
            }
            else -> {
                NormalCommentViewHolder(ListItemCommentBinding.inflate(layoutInflater, parent, false).apply {
                    adapter = this@CommentsAdapter
                    viewModel = this@CommentsAdapter.viewModel
                    post = this@CommentsAdapter.post
                    showAwards = settings.showAwards()
                    onReportsIgnoreChange =  OnReportsIgnoreChangeListener { invalidateAll() }
                    showPeekParentButton = settings.showPeekParentButtonInComments()
                    commentContent.setLongClickToPeekUrl {
                        comment?.let {
                            this@CommentsAdapter.viewModel.hideComments(it)
                        }
                    }
                })
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val comment = comments[position]

        return when {
            comment.isCollapsed -> HIDDEN_COMMENT_TYPE
            comment.kind == Thing.MORE.value -> MORE_COMMENTS_TYPE
            else -> NORMAL_COMMENT_TYPE
        }
    }

    override fun getItemCount() = comments.size


    /**
     * ViewHolder for comments that are shown as the entire comment
     */
    inner class NormalCommentViewHolder(private val binding: ListItemCommentBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            // This custom movement method checks if the link clicked is to the same post as we are showing
            // and if it links to a comment in the post it sets that chain on the view model
            binding.commentContent.movementMethod = InternalLinkMovementMethod { link: String, context: Context ->
                val redditPost = post

                // The options don't really matter as we only care about PostActivity links
                val intent = createIntent(link, CreateIntentOptions(), context)

                if (intent.component?.className == PostActivity::class.java.name
                    && redditPost != null
                    && intent.getStringExtra(PostActivity.EXTRAS_POST_ID_KEY) == redditPost.id
                    && intent.hasExtra(PostActivity.EXTRAS_COMMENT_ID_CHAIN)) {
                    viewModel.showChain(intent.getStringExtra(PostActivity.EXTRAS_COMMENT_ID_CHAIN)!!)
                } else {
                    // We could just start "intent" directly, but DispatcherActivity deals with animations
                    // for some links
                    Intent(context, DispatcherActivity::class.java).apply {
                        putExtra(DispatcherActivity.EXTRAS_URL_KEY, link)
                        context.startActivity(this)
                    }
                }

                true
            }
        }

        /**
         * Binds the ViewHolder to a comment
         *
         * @param comment The comment to bind
         * @param highlight True if the comment should have a slight highlight around it
         */
        fun bind(comment: RedditComment, highlight: Boolean, byLoggedInUser: Boolean, showAllSidebars: Boolean) {
            with(binding) {
                this.comment = comment
                this.highlight = highlight
                isByLoggedInUser = byLoggedInUser

                if (showAwards) {
                    awards.listing = comment
                }

                commentVoteBar.listing = comment

                addSidebars(sideBarsBarrier, comment.depth - getBaseDepth(), sidebarColors, showAllSidebars)
                // Execute all the bindings now, or else scrolling/changes to the dataset will have a
                // small, but noticeable delay, causing the old comment to still appear
                // This needs to be called before the link previews are added, since the previews use
                // the spans set with Markwon, and it might not (and probably wont) be set before the previews are added
                executePendingBindings()

                if (settings.showLinkPreview()) {
                    showLinkPreviews()
                    executePendingBindings()
                }
            }
        }

        /**
         * Shows link previews for the comment
         */
        private fun showLinkPreviews() {
            val text = binding.commentContent.text.toSpannable()
            val urls = text.getSpans(0, text.length, URLSpan::class.java)

            // By using a LinearLayout for loading the link previews we will lose some performance
            // because of nested layouts, but considering that links are relatively rare, and that
            // each comment usually won't have more than a few links, I'll take that minor performance
            // hit for a lot cleaner code (setting constraints via code is kind of messy)
            // Also moving where the previews are in the layout is a lot easier, if I want to change that later

            // Remove all previews views
            binding.linkPreviews.removeAllViews()

            binding.linkPreviews.visibility = if (urls.isNotEmpty()) {
                // The spans seems to always be in reversed order, so reverse them to the original order
                urls.reverse()
                setLinkPreviews(text, urls)
                VISIBLE
            } else {
                // Set to gone if no previews to remove the top margin the link layout has
                GONE
            }
        }

        /**
         * Sets the link previews for the comment
         *
         * @param fullText The spannable holding the entire text
         * @param spans The array of URLSpans to show previews for
         */
        private fun setLinkPreviews(fullText: Spannable, spans: Array<URLSpan>) {
            val showPreviewForIdenticalLinks = settings.showLinkPreviewForIdenticalLinks()

            spans.forEach { span ->
                val start = fullText.getSpanStart(span)
                val end = fullText.getSpanEnd(span)
                val text = fullText.substring(start, end).trim()
                val url = span.url

                // TODO "text" will actually include superscripts, since that text isn't actually removed
                //  from the text, it uses a RelativeSizeSpan with 0f to "remove" the characters
                //  best solution would be to remove the text in the Markwon plugin
                //  Easiest solution is probably to check if "text" starts with "^" or "^(" and remove it here
                //  (and ")" at the end if it starts with "^("

                // If the text and the url is the same and the user doesn't want to preview those
                if (text == url && !showPreviewForIdenticalLinks) {
                    return@forEach
                }

                LinkPreview(binding.root.context).run {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    setText(text)
                    setLink(url)

                    binding.linkPreviews.addView(this)
                }
            }
        }
    }

    /**
     * ViewHolder for comments that are hidden (the comments explicitly selected to be hidden)
     */
    inner class HiddenCommentViewHolder(private val binding: ListItemHiddenCommentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(comment: RedditComment, highlight: Boolean, byLoggedInUser: Boolean, showAllSidebars: Boolean) {
            with(binding) {
                root.setOnClickListener {
                    this@CommentsAdapter.viewModel.showComments(comment)
                }
                this.comment = comment
                this.highlight = highlight
                isByLoggedInUser = byLoggedInUser
                addSidebars(sideBarsBarrier, comment.depth - getBaseDepth(), sidebarColors, showAllSidebars)
                executePendingBindings()
            }
        }
    }

    /**
     * ViewHolder for comments that are "2 more comments" type comments, that will load the comments
     * when clicked
     */
    inner class MoreCommentsViewHolder(private val binding: ListItemMoreCommentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(comment: RedditComment, showAllSidebars: Boolean) {
            with(binding) {
                this.comment = comment
                addSidebars(sideBarsBarrier, comment.depth - getBaseDepth(), sidebarColors, showAllSidebars)
                executePendingBindings()
            }
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
 * Adds sidebars to a ConstraintLayout
 */
fun addSidebars(barrier: Barrier, depth: Int, colors: List<Int>, showAllSidebars: Boolean) {
    if (showAllSidebars) {
        addAllSidebars(barrier, depth, colors)
    } else {
        addOneSidebar(barrier, depth, colors)
    }
}

/**
 * Adds one sidebar to a ConstraintLayout to show the boundary of the comment. The comment
 * will be indented according to [depth]
 *
 * @param barrier The barrier to reference the sidebar to
 * @param depth The depth of the comment, if this is 0 then no sidebar is added
 */
private fun addOneSidebar(barrier: Barrier, depth: Int, colors: List<Int>) {
    val parent = barrier.parent as ConstraintLayout
    val res = barrier.context.resources
    val indent = res.getDimension(R.dimen.commentDepthIndent).toInt()
    val barWidth = res.getDimension(R.dimen.commentSideBarWidthOneBar).toInt()
    val barMargin = res.getDimension(R.dimen.commentSideBarMarginOneBar).toInt()
    val contentDescription = "sidebar"

    val previousSidebars = ArrayList<View>()
    parent.findViewsWithText(previousSidebars, contentDescription, View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION)

    // No sidebar for top-level comments
    if (depth == 0) {
        previousSidebars.forEach { parent.removeView(it) }
        barrier.referencedIds = intArrayOf(R.id.emptyView)
        return
    }

    // Reuse the previous sidebar if possible
    val view = if (previousSidebars.isNotEmpty()) {
        previousSidebars[0]
    } else {
        // Create the view
        View(barrier.context).apply {
            id = View.generateViewId()
            this.contentDescription = contentDescription
            // Use a drawable for rounded corners, although this makes the sidebars of same depth
            // with a tiny gap
            background = ContextCompat.getDrawable(barrier.context, R.drawable.comment_sidebar_one_sidebar_background)
            parent.addView(this)
        }
    }.apply {
        // This has to be set when the view is created, and updated when reused (otherwise the color can be wrong)
        // Use depth - 1 to not skip the first color (the sidebar "belongs" to the comment here, compared to it starting from
        // below the comment it "belongs" to when all are shown)
        DrawableCompat.setTint(background, colors[(depth - 1) % colors.size])
    }

    // Set constraints
    ConstraintSet().apply {
        clone(parent)

        // Set width of the view
        constrainWidth(view.id, barWidth)
        constrainHeight(view.id, 0)

        // start_toStartOf=parent, margin_start = indent*depth
        connect(view.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, indent * depth)

        // end_toEndOf=barrier
        connect(view.id, ConstraintSet.END, barrier.id, ConstraintSet.END, barMargin)

        // bottom_toBottomOf=parent
        connect(view.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        // top_toTopOf=parent
        connect(view.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)

        // Apply all the constraints
        applyTo(parent)
    }

    val referencedIds = IntArray(1)
    referencedIds[0] = view.id
    barrier.referencedIds = referencedIds
}

/**
 * Adds sidebars to a ConstraintLayout to visually show the comment depth
 *
 * @param barrier The barrier to reference the sidebar to
 * @param depth The depth of the comment, if this is 0 then no sidebars are added
 */
private fun addAllSidebars(barrier: Barrier, depth: Int, colors: List<Int>) {
    val parent = barrier.parent as ConstraintLayout
    // The contentDescription for the sidebars, this is used to find the sidebars again later
    val contentDescription = "sidebar"
    val res = barrier.resources
    val barWidth = res.getDimension(R.dimen.commentSideBarWidth).toInt()
    val indent = res.getDimension(R.dimen.commentDepthIndent).toInt()

    // Find the previous sidebars added to the ConstraintLayout
    // By doing this we can reuse views by removing only the overflow amount/only adding the extra
    // needed. When scrolling through a lot of comments this will save 100s of views from being created
    val previousSidebars = ArrayList<View>()
    parent.findViewsWithText(previousSidebars, contentDescription, View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION)
    val previousSidebarsSize = previousSidebars.size

    when {
        // Top level comments don't have sidebars, remove all previous
        depth == 0 -> {
            previousSidebars.forEach{ parent.removeView(it) }
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
        val lastSidebar = previousSidebars.last()

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
        val view = View(barrier.context).apply {
            setBackgroundColor(colors[i % colors.size])
            this.contentDescription = contentDescription
            this.id = id
        }

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
    ConstraintSet().run {
        clone(parent)
        connect(lastSideBar.id, ConstraintSet.END, barrier.id, ConstraintSet.END, indent)
        applyTo(parent)
    }
}