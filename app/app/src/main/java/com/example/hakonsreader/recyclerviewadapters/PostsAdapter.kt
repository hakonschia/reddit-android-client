package com.example.hakonsreader.recyclerviewadapters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.hakonsreader.R
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.interfaces.OnVideoFullscreenListener
import com.example.hakonsreader.interfaces.OnVideoManuallyPaused
import com.example.hakonsreader.recyclerviewadapters.diffutils.PostsDiffCallback
import com.example.hakonsreader.views.ListDivider
import com.example.hakonsreader.views.Post
import java.time.Duration
import java.time.Instant

/**
 * Adapter for recycler view of [RedditPost]
 */
class PostsAdapter : RecyclerView.Adapter<PostsAdapter.ViewHolder>() {
    
    private var posts = ArrayList<RedditPost>()
    val postExtras = HashMap<String, Bundle>()

    /**
     * The amount of minutes scores should be hidden (default to -1 means not specified)
     */
    var hideScoreTime = -1

    /**
     * Listener for when a post has been clicked
     */
    var onPostClicked: OnPostClicked? = null

    /**
     * Listener for when a video has manually been paused
     */
    var onVideoManuallyPaused: OnVideoManuallyPaused? = null

    /**
     * Listener for when a video has entered fullscreen
     */
    var onVideoFullscreenListener: OnVideoFullscreenListener? = null

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

            posts = list as ArrayList<RedditPost>
            diffResults.dispatchUpdatesTo(this)
        }
    }

    fun getPosts() : List<RedditPost> {
        return posts
    }

    /**
     * Removes all posts from the list
     */
    fun clearPosts() {
        val size = posts.size
        posts.clear()
        notifyItemRangeRemoved(0, size)
    }

    private fun saveExtras(post: Post) {
        val previousPost = post.redditPost
        if (previousPost != null) {
            postExtras[previousPost.id] = post.extras
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        saveExtras(holder.post)
        val post = posts[position]
        val previousId = holder.getPostId()

        // Disable ticker animation to avoid it updating when scrolling
        holder.post.enableTickerAnimation(false)

        val created = Instant.ofEpochSecond(post.createdAt)
        val now = Instant.now()
        val between = Duration.between(created, now)
        holder.post.hideScore = hideScoreTime > between.toMinutes()

        // Only update, don't recreate post content
        if (post.id == previousId) {
            holder.post.updatePostInfo(post)
        } else {
            holder.post.redditPost = post
        }

        val savedExtras = postExtras[post.id]
        if (savedExtras != null) {
            holder.post.extras = savedExtras
        }
        holder.post.enableTickerAnimation(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
                R.layout.list_item_post,
                parent,
                false
        )

        return ViewHolder(view)
    }

    override fun getItemCount() = posts.size

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        val divider = ListDivider(ContextCompat.getDrawable(recyclerView.context, R.drawable.list_divider))
        recyclerView.addItemDecoration(divider)
    }


    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val post: Post = view.findViewById<Post>(R.id.post).apply {
            setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onPostClicked?.postClicked(this)
                }
            }

            setOnVideoPostPaused(onVideoManuallyPaused)
            setVideoFullscreenListener(onVideoFullscreenListener)

            // Text posts shouldn't be shown in lists of posts
            setShowTextContent(false)
        }

        /**
         * Gets the ID of the [RedditPost] this ViewHolder is currently holding
         *
         * @return A string ID
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
         * Crossposts is taken into account and will return the position of the actual content
         * inside the crosspost
         *
         * @return The Y position of the content
         */
        fun getContentY(): Int {
            return post.contentY
        }

        /**
         * Gets the bottom position of the contents Y position on the screen
         *
         * Crossposts is taken into account and will return the position of the actual content
         * inside the crosspost
         *
         * @return The Y position of the bottom of the content
         */
        fun getContentBottomY(): Int {
            return post.contentBottomY
        }

        /**
         * Gets a bundle of extras that include the ViewHolder state
         *
         *
         * Use [ViewHolder.setExtras] to restore the state
         *
         * @return The state of the ViewHolder
         */
        fun getExtras(): Bundle {
            return post.extras
        }

        /**
         * Sets extras that have been saved to restore state.
         *
         * @param data The extras to set
         */
        fun setExtras(data: Bundle?) {
            data?.let { post.extras = it }
        }

        /**
         * Call when the ViewHolder should be destroyed. Any resources are freed up
         */
        fun destroy() {
            post.cleanUpContent()
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