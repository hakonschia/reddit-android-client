package com.example.hakonsreader.views

import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.Pair
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.example.hakonsreader.R
import com.example.hakonsreader.api.enums.PostType
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.api.persistence.RedditPostsDao
import com.example.hakonsreader.databinding.PostBinding
import com.example.hakonsreader.fragments.bottomsheets.PeekTextPostBottomSheet
import com.example.hakonsreader.misc.dpToPixels
import com.example.hakonsreader.views.ContentVideo.Companion.isRedditPostVideoPlayable
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Callback
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject

/**
 * View for displaying a Reddit post, including the post information ([PostInfo]), the content (a subclass
 * of [Content]), and the post bar ([PostBar])
 *
 * If wanted, the max height of the post can be set with [maxHeight] (and [updateMaxHeight] to update
 * after the content has been created) which will ensure post does not go above said height. The
 * content of the post will be resized to fit the given height.
 */
@AndroidEntryPoint
class Post @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : Content(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "Post"

        /**
         * Value used when [Post.maxHeight] isn't set
         */
        private const val NO_MAX_HEIGHT = -1
    }

    @Inject
    lateinit var postsDao: RedditPostsDao

    private val binding = PostBinding.inflate(LayoutInflater.from(context), this, true).apply {
        setOnLongClickListener {
            redditPost?.let { post ->
                // If we're already showing text content it's no point in showing this dialog
                if (post.getPostType() == PostType.TEXT && !showTextContent) {
                    val markdown: String = post.selftext

                    if (markdown.isNotEmpty()) {
                        if (context is AppCompatActivity) {
                            PeekTextPostBottomSheet.newInstance(post).show((context as AppCompatActivity).supportFragmentManager, "Text post")
                        } else {
                            // Not sure if this will ever happen, but in case it does
                            // This would make the peek url in the post not work though, as it uses bottom sheet as well
                            AlertDialog.Builder(context)
                                    .setView(ContentText(context).apply {
                                        setRedditPost(post)
                                    })
                                    .show()
                        }

                    } else {
                        Snackbar.make(it, R.string.postHasNoText, Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
            true
        }
    }

    /**
     * If set to false, text posts will not show the content of the post, only the post info/post bar
     */
    var showTextContent = true

    /**
     * The callback for when an image post has been loaded
     */
    var imageLoadedCallback: Callback? = null

    /**
     * The callback for when a video post has been manually paused
     */
    var onVideoManuallyPaused: ((ContentVideo) -> Unit)? = null

    /**
     * The callback for when a video post has been manually paused
     */
    var onVideoFullscreenListener: ((ContentVideo) -> Unit)? = null

    /**
     * The max height of the entire post (post info + content + post bar)
     */
    var maxHeight = NO_MAX_HEIGHT

    /**
     * True if the score on the post is/should be hidden
     */
    var hideScore: Boolean
        get() = binding.postFullBar.getHideScore()
        set(value) = binding.postFullBar.setHideScore(value)


    // It is probably quite bad to use LiveData to observe the changes inside here? But it makes it
    // very easy to update the information without redrawing the content, and I can also add animations
    // on the changes fairly easy this way. The dangerous pitfall is to forget to remove observers and such
    // which can probably cause memory leaks and possibly cause performance issues
    /**
     * The lifecycle owner of the post
     *
     * If this is set then the view will observe changes in the local database
     * to the post set with [setRedditPost]
     *
     * Note: This should be set to `null` when the lifecycle owner is destroyed to avoid a memory leak.
     * Setting this to null will remove the current LiveData observer
     */
    var lifecycleOwner: LifecycleOwner? = null
        set(value) {
            field = value
            if (value == null) {
                postLiveData?.removeObserver(postObserver)
                postLiveData = null
            }
        }

    /**
     * The LiveData that is currently being observed
     */
    private var postLiveData: LiveData<RedditPost>? = null

    /**
     * The observer [postLiveData] uses
     */
    private var postObserver = Observer<RedditPost> {
        if (it != null) {
            // Crossposts aren't stored in the database, and the new post is from the database
            // so copy the old ones, if possible.
            // Same with third party objects, as they are stored as a raw string it's faster to just
            // copy it instead of bothering with the json parsing
            // TODO imgur albums dont seem to be passed along correctly? Possibly issue with all albums
            redditPost?.let { old ->
                it.crossposts = old.crossposts
                it.thirdPartyObject = old.thirdPartyObject
            }

            updatePostInfo(it)
        }
    }

    /**
     * Listener for layout changes for the content of the post, which is used to resize the content
     * based on [maxHeight]
     */
    private lateinit var contentOnGlobalLayoutListener: OnGlobalLayoutListener


    // Thank god this is a one person project, because I would feel really bad for anyone having
    // to debug this abomination of a code for updating the height
    /**
     * Updates the max height the post can have (post info + content + post bar)
     *
     * @param maxHeight The height limit
     */
    fun updateMaxHeight(maxHeight: Int) {
        // Ensure that the layout listener is removed. If this is still present, it will cause an infinite
        // callback loop since the height will most likely be changed inside the listener
        if (this::contentOnGlobalLayoutListener.isInitialized) {
            binding.content.viewTreeObserver.removeOnGlobalLayoutListener(contentOnGlobalLayoutListener)
        }
        this.maxHeight = maxHeight

        val content = binding.content.getChildAt(0) as Content? ?: return

        // Get height of the content and the total height of the entire post so we can resize the content correctly
        val contentHeight = binding.content.measuredHeight
        val totalHeight = binding.postsParentLayout.measuredHeight

        // For some views making the measure with WRAP_CONTENT doesn't work, luckily those views
        // know the wanted height already, so we can use the wantedHeight to achieve the same functionality
        val wantedHeight = if (content.wantedHeight >= 0) {
            content.wantedHeight
        } else {
            // Measure what the content at most wants
            content.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            content.measuredHeight
        }

        // Calculate the height the content at most can be
        val newContentHeight = maxHeight - totalHeight + contentHeight

        // If the content does not need the maximum size "reserved" for it, set to the measured/wanted size
        // This height is WRAP_CONTENT, so it will only take what it needs
        if (newContentHeight > wantedHeight) {
            setContentHeight(wantedHeight)
        } else {
            setContentHeight(newContentHeight)
        }
    }

    /**
     * Sets the height of only the content view of the post. The value will be animated for a duration
     * of 250 milliseconds
     *
     * This does not check [maxHeight]
     *
     * @param height The height to set
     */
    private fun setContentHeight(height: Int) {
        // TODO links dont correctly regain their previous view. The image isn't correctly put back and
        //  the link text doesn't change position (might have to do something inside the class itself)
        if (height < 0) {
            return
        }
        val content = binding.content.getChildAt(0)

        ValueAnimator.ofInt(content.height, height).run {
            addUpdateListener { valueAnimator ->
                val value = valueAnimator.animatedValue as Int
                val layoutParams = content.layoutParams
                layoutParams.height = value
                content.layoutParams = layoutParams
            }

            duration = 250
            start()
        }
    }

    /**
     * @return The height of only the content in the post
     */
    fun getContentHeight() = binding.content.getChildAt(0)?.measuredHeight


    /**
     * Enables or disables layout animation for various views in the layout
     *
     * @param enable If set to true, layout animations will be enabled
     */
    fun enableLayoutAnimations(enable: Boolean) {
        binding.postInfo.enableLayoutAnimations(enable)
        binding.postFullBar.enableTickerAnimation(enable)
    }

    /**
     * Updates the information in the post without re-creating the content
     *
     * @param post The post with updated information
     */
    fun updatePostInfo(post: RedditPost) {
        redditPost = post
        binding.postInfo.setPost(post)
        binding.postFullBar.post = redditPost
    }

    /**
     * Adds the post content
     *
     *
     * If [Post.showTextContent] is `false` and the post type is [PostType.TEXT] nothing happens
     *
     *
     * The height of the post is resized to match [Post.maxHeight], if needed
     */
    private fun addContent() {
        val hasTextContent = redditPost.getPostType() == PostType.TEXT
                // If the post is a crosspost and the crosspost is a text post, it's seen as LINK
                // post as the post is a "link" to the crosspost, so check here if it's a text post
                // as it would be added for generatePostContent
                || redditPost?.crossposts?.find { it.getPostType() == PostType.TEXT } != null
        if (!showTextContent && hasTextContent) {
            return
        }

        val content = generatePostContent(redditPost, context)

        if (content != null) {
            binding.content.addView(content)
            if (maxHeight != NO_MAX_HEIGHT) {
                contentOnGlobalLayoutListener = OnGlobalLayoutListener {

                    // Get height of the content and the total height of the entire post so we can resize the content correctly
                    val height = content.measuredHeight
                    val totalHeight = binding.postsParentLayout.measuredHeight

                    // Entire post is too large, set new content height
                    if (totalHeight > maxHeight) {
                        val params = content.layoutParams as LayoutParams
                        params.height = maxHeight - totalHeight + height
                        content.layoutParams = params
                    }
                }
                binding.content.viewTreeObserver.addOnGlobalLayoutListener(contentOnGlobalLayoutListener)
            }
        }

        val params = binding.content.layoutParams as RelativeLayout.LayoutParams
        // Align link and text posts to start of parent, otherwise center
        if (content is ContentLink || content is ContentText || content is ContentPostRemoved) {
            params.removeRule(RelativeLayout.CENTER_IN_PARENT)
            params.addRule(RelativeLayout.ALIGN_PARENT_START)
        } else {
            params.removeRule(RelativeLayout.ALIGN_PARENT_START)
            params.addRule(RelativeLayout.CENTER_IN_PARENT)
        }
        binding.content.layoutParams = params
    }

    /**
     * Generates the content view for a post
     *
     * @param post The post to generate for
     * @return A view with the content of the post
     */
    private fun generatePostContent(post: RedditPost, context: Context): Content? {
        val margin8 = dpToPixels(8f, resources)
        val marginParams = MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setMargins(margin8, 0, margin8, 0)
        }

        // If the post has been removed don't try to render the content as it can cause a crash later
        // Just show that the post has been removed
        // For instance, if the post is not uploaded to reddit the URL will still link to something (like an imgur gif)
        // TODO maybe the only posts actually removed completely so they're not able ot be watched are videos? Even text/images uploaded
        //  to reddit directly are still there
        if (post.removedByCategory != null) {
            return ContentPostRemoved(context).apply {
                setRedditPost(post)
                layoutParams = marginParams
            }
        }

        // Generate the content based on the crosspost. Videos hosted on reddit aren't sent to the "child"
        // post (this post) but it is in the parent
        val crosspost = post.crossposts?.firstOrNull()
        if (crosspost != null) {
            return generatePostContent(crosspost, context)
        }

        return when (post.getPostType()) {
            PostType.IMAGE -> {
                ContentImage(context).apply {
                    imageLoadedCallback = this@Post.imageLoadedCallback
                }
            }

            PostType.VIDEO, PostType.GIF, PostType.RICH_VIDEO -> {
                // Ensure we know how to handle a video, otherwise it might not load
                // Show as link content if we can't show it as a video
                if (isRedditPostVideoPlayable(post)) {
                    ContentVideo(context).apply {
                        this@Post.onVideoManuallyPaused?.let { setOnVideoManuallyPaused(it) }
                        this@Post.onVideoFullscreenListener?.let { setOnVideoFullscreenListener(it) }
                    }
                } else {
                    ContentLink(context).apply {
                        layoutParams = marginParams
                    }
                }
            }

            PostType.LINK -> {
                ContentLink(context).apply {
                    layoutParams = marginParams
                }
            }

            PostType.TEXT -> {
                // If there is no text on the post there is no point in creating a view for it
                val selfText = post.selftext

                if (selfText.isNotEmpty()) {
                    ContentText(context).apply {
                        layoutParams = marginParams
                    }
                } else {
                    null
                }
            }

            PostType.GALLERY -> {
                ContentGallery(context)
            }

            else -> null
        }?.apply {
            setRedditPost(post)
            transitionName = context.getString(R.string.transition_post_content)
       }
    }

    /**
     * Releases any relevant resources and removes the content view
     *
     *
     * If relevant to the type of post, various resources (such as video players) are released
     * when this is called
     */
    fun cleanUpContent() {
        // Free up any resources that might not be garbage collected automatically
        val v = binding.content.getChildAt(0)

        // Release the exo player from video posts
        if (v is ContentVideo) {
            v.release()
        } else if (v is ContentGallery) {
            v.release()
        }

        binding.content.removeAllViewsInLayout()
        // Make sure the view size resets
        binding.content.forceLayout()
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
        // Get the views position on the screen
        val location = IntArray(2)
        binding.content.getLocationOnScreen(location)
        return location[1]
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
        return getContentY() + binding.content.measuredHeight
    }

    /**
     * Gets the content view displayed in the post
     */
    fun getContent(): View? {
        return binding.content.getChildAt(0)
    }


    /**
     * Updates the view
     */
    override fun updateView() {
        // Ensure view is fresh if used in a RecyclerView
        cleanUpContent()

        binding.postInfo.setPost(redditPost, updateAwards = true)
        addContent()
        binding.postFullBar.post = redditPost
    }

    override fun setRedditPost(redditPost: RedditPost?) {
        super.setRedditPost(redditPost)

        if (redditPost == null) {
            return
        }

        // New post set, remove observer on previous LiveData and get a new one from the database to observe
        lifecycleOwner?.let {
            postLiveData?.removeObserver(postObserver)
            postLiveData = postsDao.getPostById(redditPost.id).apply { observe(it, postObserver) }
        }
    }

    /**
     * Retrieve a bundle of information that can be useful for saving the state of the post
     *
     *
     * Currently only saves state for video posts
     *
     * @return A bundle that might include state variables
     */
    override fun getExtras() : Bundle {
        val c = binding.content.getChildAt(0) as Content?
        return c?.getExtras() ?: Bundle()
    }

    /**
     * Sets a bundle of information to restore the state of the post
     *
     *
     * Currently only restores state for video posts
     *
     * @param data The data to use for restoring the state
     */
    override fun setExtras(data: Bundle) {
        val c = binding.content.getChildAt(0) as Content?
        c?.setExtras(data)
    }

    /**
     * Retrieve the list of views mapped to the corresponding transition name, to be used in a
     * shared element transition
     *
     * @return A list of pairs with a View mapped to a transition name
     */
    override fun getTransitionViews(): List<Pair<View, String>> {
        val context = context

        val pairs: MutableList<Pair<View, String>> = ArrayList()
        pairs.add(Pair.create(binding.postInfo, context.getString(R.string.transition_post_info)))
        pairs.add(Pair.create(binding.postFullBar, context.getString(R.string.transition_post_full_bar)))

        val content = binding.content.getChildAt(0) as Content?

        if (content != null) {
            // Not all subclasses add custom transition views, so if no custom view is found use the
            // view itself as the transition view
            val contentTransitionViews = content.transitionViews

            if (contentTransitionViews.isEmpty()) {
                pairs.add(Pair.create(content, context.getString(R.string.transition_post_content)))
            } else {
                pairs.addAll(contentTransitionViews)
            }
        }

        return pairs
    }

    override fun viewSelected() {
        (binding.content.getChildAt(0) as Content?)?.viewSelected()
    }
    override fun viewUnselected() {
        (binding.content.getChildAt(0) as Content?)?.viewUnselected()
    }
}