package com.example.hakonsreader.views

import android.animation.ValueAnimator
import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.RelativeLayout
import androidx.core.util.Pair
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.api.enums.PostType
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.databinding.PostBinding
import com.example.hakonsreader.interfaces.OnVideoFullscreenListener
import com.example.hakonsreader.interfaces.OnVideoManuallyPaused
import com.example.hakonsreader.views.ContentVideo.Companion.isRedditPostVideoPlayable
import com.squareup.picasso.Callback
import java.util.*

class Post : Content {

    companion object {
        private const val TAG = "Post"

        /**
         * Flag used for when the [Post.maxHeight] isn't set
         */
        private const val NO_MAX_HEIGHT = -1
    }

    private val binding = PostBinding.inflate(LayoutInflater.from(context), this, true)

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
    var onVideoManuallyPaused: OnVideoManuallyPaused? = null

    /**
     * The callback for when a video post has been manually paused
     */
    var onVideoFullscreenListener: OnVideoFullscreenListener? = null

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
            updatePostInfo(it)
        }
    }

    /**
     * Listener for layout changes for the content of the post, which is used to resize the content
     * based on [maxHeight]
     */
    private lateinit var contentOnGlobalLayoutListener: OnGlobalLayoutListener


    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)


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

        binding.content.getChildAt(0) ?: return

        // Get height of the content and the total height of the entire post so we can resize the content correctly
        val contentHeight = binding.content.measuredHeight
        val totalHeight = binding.postsParentLayout.measuredHeight

        // TODO this doesn't really work, since if the content is actually smaller the it will still resize
        //  to the full height
        val newContentHeight = maxHeight - totalHeight + contentHeight
        setContentHeight(newContentHeight)
    }

    /**
     * Sets the height of only the content view of the post. The value will be animated for a duration
     * of 250 milliseconds
     *
     * This does not check the max height set
     *
     * @param height The height to set
     */
    fun setContentHeight(height: Int) {
        // TODO links dont correctly regain their previous view. The image isn't correctly put back and
        //  the link text doesn't change position (might have to do something inside the class itself)
        if (height < 0) {
            return
        }
        val content = binding.content.getChildAt(0)

        ValueAnimator.ofInt(content.measuredHeight, height).run {
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
        if (!showTextContent && redditPost.getPostType() == PostType.TEXT) {
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
        // If the post has been removed don't try to render the content as it can cause a crash later
        // Just show that the post has been removed
        // For instance, if the post is not uploaded to reddit the URL will still link to something (like an imgur gif)
        // TODO maybe the only posts actually removed completely so they're not able ot be watched are videos? Even text/images uploaded
        //  to reddit directly are still there
        if (post.removedByCategory != null) {
            val c = ContentPostRemoved(context)
            c.setRedditPost(post)
            return c
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
                    setImageLoadedCallback(this@Post.imageLoadedCallback)
                }
            }

            PostType.VIDEO, PostType.GIF, PostType.RICH_VIDEO -> {
                // Ensure we know how to handle a video, otherwise it might not load
                // Show as link content if we can't show it as a video
                if (isRedditPostVideoPlayable(post)) {
                    ContentVideo(context).apply {
                        setOnVideoManuallyPaused(this@Post.onVideoManuallyPaused)
                        setOnVideoFullscreenListener(this@Post.onVideoFullscreenListener)
                    }
                } else if (App.get().openYouTubeVideosInApp()
                        && (redditPost.domain == "youtu.be" || redditPost.domain == "youtube.com")) {
                    ContentYoutubeVideo(context)
                } else {
                    ContentLink(context)
                }
            }

            PostType.LINK -> {
                ContentLink(context)
            }

            PostType.TEXT -> {
                // If there is no text on the post there is no point in creating a view for it
                val selfText = post.selftext

                if (selfText.isNotEmpty()) {
                    ContentText(context)
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
     * If relevant to the type of post, various resoruces (such as video players) are released
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
            postLiveData = App.get().database.posts().getPostById(redditPost.id).apply { observe(it, postObserver) }
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