package com.example.hakonsreader.recyclerviewadapters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.hakonsreader.R
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.misc.generatePostContent
import com.example.hakonsreader.recyclerviewadapters.diffutils.PostsDiffCallback
import com.example.hakonsreader.views.*

/**
 * Adapter for recycler view of [RedditPost].
 *
 * Note: When the RecyclerView using this adapter is finished with it (ie. the RecyclerView/the view
 * holding the RecyclerView is destroyed) the RecyclerView has to set its adapter to `null`, otherwise
 * a leak will occur (if there are video posts, or galleries with videos, these ExoPlayer instances
 * might not be released, and if [lifecycleOwner] is set these references will also not be cleared).
 * [postExtras] will be set when this is done to store the states of the ViewHolders
 *
 * @param onEndOfListReached The callback that is invoked when the bottom of the list has almost been reached.
 * The threshold for what is seen as the bottom of the list is determined by [numRemainingPostsBeforeEndOfList]. The
 * callback will be invoked when the [onBindViewHolder] is called for any position after the given position, and
 * will only be called once (reset with [resetOnEndOfList])
 */
class PostsAdapter(
    private val onEndOfListReached: () -> Unit
) : RecyclerView.Adapter<PostsAdapter.ViewHolder>() {

    companion object {
        @Suppress("UNUSED")
        private const val TAG = "PostsAdapter"
    }
    
    private var posts: List<RedditPost> = ArrayList()

    /**
     * A list holding the currently unused content views from recycled posts
     */
    private val unusedContentViews: MutableList<Content> = ArrayList()

    /**
     * A list of the view holders this adapter has
     */
    val viewHolders = ArrayList<ViewHolder>()

    /**
     * The extras for the posts. Setting this value will update the ViewHolders automatically
     */
    var postExtras = HashMap<String, Bundle>()
        set(value) {
            field = value

            viewHolders.forEach {
                val id = it.post.redditPost?.id
                if (id != null) {
                    val extras: Bundle? = field[id]
                    if (extras != null) {
                        it.post.extras = extras
                    }
                }
            }
        }


    /**
     * Listener for when a post has been clicked
     */
    var onPostClicked: OnPostClicked? = null

    /**
     * Listener for when a video has manually been paused
     */
    var onVideoManuallyPaused: ((ContentVideo) -> Unit)? = null

    /**
     * Listener for when a video has entered fullscreen
     */
    var onVideoFullscreenListener: ((ContentVideo) -> Unit)? = null

    /**
     * The amount of posts left in the list before calling [onEndOfListReached]
     */
    var numRemainingPostsBeforeEndOfList = 10

    /**
     * The amount of items in the list at the last attempt at loading more posts
     */
    private var lastLoadAttemptCount = 0

    /**
     * The lifecycle owner of the adapter. If this is set the posts in the adapter will observe
     * the local database for updates to the posts, which will update the post information without
     * redrawing the ViewHolder
     *
     * When the view is destroyed, this must be set to `null` to avoid a leak, as well as setting the
     * ViewHolders of the adapter to null (with [Post.lifecycleOwner]). Optionally, set the entire
     * adapter to null to combine these operations.
     */
    var lifecycleOwner: LifecycleOwner? = null
        set(value) {
            field = value
            viewHolders.forEach {
                it.post.lifecycleOwner = value
            }
        }


    /**
     * Resets the [onEndOfListReached] runnable to be called again
     */
    fun resetOnEndOfList() {
        lastLoadAttemptCount = -1
    }

    /**
     * Submits the list of posts to show in the RecyclerView
     *
     * @param list The posts to show
     */
    fun submitList(list: List<RedditPost>) {
        if (list.isEmpty()) {
            clearPosts()
        } else {
            val previous = posts

            val diffResults = DiffUtil.calculateDiff(
                    PostsDiffCallback(previous, list),
                    true
            )

            posts = list
            diffResults.dispatchUpdatesTo(this)
        }
    }

    /**
     * Removes all posts from the list
     */
    fun clearPosts() {
        val size = posts.size
        posts = ArrayList()
        notifyItemRangeRemoved(0, size)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Invoke the callback if the position is almost at the end, and only call it once for
        // each list size

        // Eg.:
        // posts.size = 25
        // position = 3, numRemainingPostsBeforeRun = 10
        // pos + numRemaining = 13, don't invoke

        // position = 15
        // pos + numRemaining = 25, invoke it as we're now close to the end (if we haven't already)
        if (position + numRemainingPostsBeforeEndOfList >= posts.size && lastLoadAttemptCount < posts.size) {
            lastLoadAttemptCount = posts.size
            onEndOfListReached.invoke()
        }

        val post = posts[position]

        val content = generatePostContent(
            holder.view.context,
            post,
            showTextContent = false,
            unusedContentViews
        )?.also { content ->
            if (content is ContentVideo) {
                onVideoManuallyPaused?.let { content.setOnVideoManuallyPaused(it) }
                onVideoFullscreenListener?.let { content.setOnVideoFullscreenListener(it) }
            }

            val savedExtras = postExtras[post.id]
            if (savedExtras != null) {
                content.extras = savedExtras
            }

            content.redditPost = post
        }

        holder.addContent(content)
        holder.post.redditPost = post
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
                R.layout.list_item_post,
                parent,
                false
        )

        return ViewHolder(view).also {
            viewHolders.add(it)
        }
    }

    override fun getItemCount() = posts.size

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        val divider = ListDivider(ContextCompat.getDrawable(recyclerView.context, R.drawable.list_divider))
        recyclerView.addItemDecoration(divider)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        viewHolders.forEach {
            it.saveExtras()
           // it.destroy()
        }

        viewHolders.clear()
        unusedContentViews.clear()

        lifecycleOwner = null
    }

    override fun onViewRecycled(holder: ViewHolder) {
        holder.saveExtras()
        val content = holder.getAndRemoveContent()
        // There is no point in storing remove post content as it is rare and a non-expensive view
        if (content != null && content !is ContentPostRemoved) {
            content.recycle()
            unusedContentViews.add(content)
        }
    }

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val post: Post = view.findViewById<Post>(R.id.post).apply {
            setOnClickListener {
                if (absoluteAdapterPosition != RecyclerView.NO_POSITION) {
                    onPostClicked?.postClicked(this)
                }
            }

            lifecycleOwner = this@PostsAdapter.lifecycleOwner

            // Text posts shouldn't be shown in lists of posts
            showTextContent = false
        }

        /**
         * @return The ID of the [RedditPost] this ViewHolder is currently holding, or null if no
         * post is in this ViewHolder
         */
        fun getPostId(): String? {
            return post.redditPost?.id
        }

        /**
         * Call when the view holder has been selected (ie. it is now the main visible view holder)
         */
        fun onSelected() {
            post.viewSelected()
        }

        /**
         * Call when the view holder has been unselected (ie. not the main visible view holder anymore)
         */
        fun onUnselected() {
            post.viewUnselected()
        }

        /**
         * Gets the position of the contents Y position on the screen
         *
         * @return The Y position of the content
         */
        fun getContentY(): Int {
            return post.getContentY()
        }

        /**
         * Gets the bottom position of the contents Y position on the screen
         *
         * @return The Y position of the bottom of the content
         */
        fun getContentBottomY(): Int {
            return post.getContentBottomY()
        }

        /**
         * Gets a bundle of extras that include the ViewHolder state
         *
         * @return The state of the ViewHolder
         */
        fun getExtras(): Bundle {
            return post.extras
        }

        /**
         * Call when the ViewHolder should be destroyed. Any resources are freed up, this includes
         * nulling [Post.lifecycleOwner]
         */
        fun destroy() {
            post.cleanUpContent()
            post.lifecycleOwner = null
        }

        /**
         * Saves the extras of the view holder to [postExtras]
         */
        fun saveExtras() {
            val rp = post.redditPost
            if (rp != null) {
                val extras = post.extras

                // No point in saving empty bundles (this also ensures extras wont be overridden)
                if (!extras.isEmpty) {
                    postExtras[rp.id] = extras
                }
            }
        }

        /**
         * Adds content to the post
         */
        fun addContent(content: Content?) {
            post.supplyContent(content)
        }

        /**
         * Retrieves the content from the post and removes the view from the view hierarchy
         */
        fun getAndRemoveContent(): Content? {
            return post.getAndRemoveContent()
        }
    }

    /**
     * Interface for when a post in the adapter has been clicked
     */
    fun interface OnPostClicked {
        fun postClicked(post: Post)
    }
}

/**
 * Formats the author text based on whether or not it is posted by a a mod or admin
 * If no match is found, the default author color is used
 *
 * If multiple values are true, the precedence is:
 * - Admin
 * - Mod
 *
 * @param tv The TextView to format
 * @param post The post the text is for
 */
@BindingAdapter("authorTextColorPost")
fun formatAuthor(tv: TextView, post: RedditPost?) {
    if (post == null) {
        return
    }

    val color = when {
        post.isAdmin() -> R.color.commentByAdminBackground
        post.isMod() -> R.color.commentByModBackground
        else -> R.color.link_color
    }
    tv.setTextColor(ContextCompat.getColor(tv.context, color))
}