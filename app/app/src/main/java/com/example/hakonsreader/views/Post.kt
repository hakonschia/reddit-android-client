package com.example.hakonsreader.views

import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.Pair
import androidx.core.view.children
import androidx.core.view.get
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.example.hakonsreader.R
import com.example.hakonsreader.activities.PostActivity
import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.enums.PostType
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.api.persistence.RedditPostsDao
import com.example.hakonsreader.databinding.PostBinding
import com.example.hakonsreader.fragments.bottomsheets.PeekTextPostBottomSheet
import com.example.hakonsreader.misc.Settings
import com.example.hakonsreader.misc.generatePostContent
import com.example.hakonsreader.recyclerviewadapters.menuhandlers.showPopupForPost
import com.example.hakonsreader.views.util.ViewUtil
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.robinhood.ticker.TickerUtils
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject

/**
 * View for displaying a Reddit post, including the post information, the content, and the post bar
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
        @Suppress("UNUSED")
        private const val TAG = "Post"

        /**
         * Value used when [Post.maxHeight] isn't set
         */
        private const val NO_MAX_HEIGHT = -1
    }

    private var postExtras: Bundle? = null

    @Inject
    lateinit var api: RedditApi

    @Inject
    lateinit var postsDao: RedditPostsDao

    /**
     * If true awards should be shown on the post
     * */
    private val showAwards = Settings.showAwards()

    private val binding = PostBinding.inflate(LayoutInflater.from(context), this, true).apply {
        postPopupMenu.setOnClickListener {
            showPopupForPost(it, redditPost, postsDao, api)
        }

        this.showAwards = this@Post.showAwards

        setOnLongClickListener {
            redditPost?.let { post ->
                // If we're already showing text content it's no point in showing this dialog
                if (post.getPostType() == PostType.TEXT && !showTextContent) {
                    val markdown: String = post.selftext

                    if (markdown.isNotEmpty()) {
                        if (context is AppCompatActivity) {
                            PeekTextPostBottomSheet.newInstance(post).show(context.supportFragmentManager, "Text post")
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
    var hideScore: Boolean = false


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

    private var suppliedContent: Content? = null

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

        val wantedContentHeight = content.wantedHeight

        // For some views making the measure with WRAP_CONTENT doesn't work, luckily those views
        // know the wanted height already, so we can use the wantedHeight to achieve the same functionality
        val wantedHeight = if (wantedContentHeight >= 0) {
            wantedContentHeight
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
     * Updates the information in the post without re-creating the content
     *
     * @param post The post with updated information
     */
    fun updatePostInfo(post: RedditPost) {
        redditPost = post
        updateInfo(post, updateAwards = false)
        updatePostBar(post, animateTickers = true)
    }

    private fun updateInfo(post: RedditPost, updateAwards: Boolean) {
        binding.post = post
        binding.isCrosspost = post.crosspostParentId != null

        // If we aren't going to show awards we should not set the listing as it will cause the
        // view to update, even if the view isn't visible (and cause all the views to be created and images loaded)
        if (showAwards && updateAwards) {
            binding.awards.listing = post
        }
        binding.userReportsTitle.setOnClickListener { ViewUtil.openReportsBottomSheet(post, context) { binding.invalidateAll() } }

        val crossposts = post.crossposts
        if (crossposts != null && crossposts.isNotEmpty()) {
            val crosspost = crossposts[0]
            binding.crosspost = crosspost
            binding.crosspostText.setOnClickListener { openPost(crosspost) }
        }
    }

    private fun updatePostBar(post: RedditPost, animateTickers: Boolean) {
        // TODO this might be wrong as this function is used to both update only the post info and when creating the post
        binding.voteBar.listing = post
        binding.voteBar.updateVoteStatus(animateTickers)

        val comments = post.amountOfComments.toFloat()

        binding.numComments.setCharacterLists(TickerUtils.provideNumberList())

        // Above 10k comments, show "1.5k comments" instead
        binding.numComments.setText(if (comments > 1000) {
            String.format(resources.getString(R.string.numCommentsThousands), comments / 1000f)
        } else {
            resources.getQuantityString(
                R.plurals.numComments,
                post.amountOfComments,
                post.amountOfComments
            )
        }, animateTickers)
    }

    /**
     * Opens a post in a [PostActivity]
     *
     * @param post The post to open
     */
    private fun openPost(post: RedditPost) {
        if (context is AppCompatActivity) {
            val intent = Intent(context, PostActivity::class.java).apply {
                putExtra(PostActivity.EXTRAS_POST_KEY, Gson().toJson(post))
            }
            (context as AppCompatActivity).startActivity(intent)
        }
    }

    /**
     * Adds the post content
     *
     * If [Post.showTextContent] is `false` and the post type is [PostType.TEXT] nothing happens
     *
     * The height of the post is resized to match [Post.maxHeight], if needed
     */
    private fun addContent() {
        val content = suppliedContent ?: generatePostContent(context, redditPost, showTextContent, null)
        content?.also { c ->
            c.setBitmap(bitmap)

            postExtras?.let {
                c.setExtras(it)
            }

            if (c is ContentVideo) {
                onVideoManuallyPaused?.let { c.setOnVideoManuallyPaused(it) }
                onVideoFullscreenListener?.let { c.setOnVideoFullscreenListener(it) }
            }

            c.setRedditPost(redditPost)
        }

        // Should only be used for this post, in case of a reuse
        postExtras = null

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

        // We don't want to keep this reference any longer than necessary
        suppliedContent = null
    }

    /**
     * Releases any relevant resources and removes the content view
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

        binding.content.removeAllViews()
        // Make sure the view size resets
        binding.content.forceLayout()
    }

    /**
     * Gets the position of the contents Y position on the screen
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
     * @return The Y position of the bottom of the content
     */
    fun getContentBottomY(): Int {
        return getContentY() + binding.content.measuredHeight
    }

    /**
     * @return The content view displayed in the post, or null if no content is being displayed
     */
    fun getContent(): Content? {
        return if (binding.content.childCount > 0) binding.content[0] as Content else null
    }

    /**
     * Supply the view with a content and don't generate it automatically.
     * This must be called before [setRedditPost]
     *
     * @param content The content to supply. If this View has a parent then it will be removed from
     * the parent
     */
    fun supplyContent(content: Content?) {
        suppliedContent = content
    }

    /**
     * Retrieves the content currently displayed and removes it from the view hierarchy so that it
     * can be reused later
     *
     * @return The content displayed, or null if no content is currently being displayed
     */
    fun getAndRemoveContent(): Content? {
        val content = getContent()

        binding.content.removeView(content)

        return content
    }

    /**
     * Updates the view
     */
    override fun updateView() {
        addContent()
        updateInfo(redditPost, updateAwards = true)
        updatePostBar(redditPost, animateTickers = false)
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
     * Currently only saves state for video posts
     *
     * @return A bundle that might include state variables
     */
    override fun getExtras() : Bundle {
        val c = binding.content.getChildAt(0) as Content?
        return c?.getExtras() ?: Bundle()
    }

    /**
     * Sets a bundle of information to restore the state of the post. If the post content has not yet been
     * generated then the bundle will be saved and passed when it is created.
     *
     * @param data The data to use for restoring the state
     */
    override fun setExtras(data: Bundle) {
        val c = binding.content.getChildAt(0) as Content?
        if (c != null) {
            c.setExtras(data)
        } else {
            postExtras = data
        }
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

        binding.postsParentLayout.children.forEach { view ->
            // We want to handle the content below manually
            // If the view isn't visible then it will appear in the new activity until it decides itself
            // that the view actually isn't visible and remove it
            if (view !is Content && view != binding.content && view.transitionName != null && view.visibility == View.VISIBLE) {
                pairs.add(Pair(view, view.transitionName))
            }
        }

        val content = getContent()

        if (content != null) {
            // Not all subclasses add custom transition views, so if no custom view is found use the
            // view itself as the transition view
            val contentTransitionViews = content.transitionViews

            if (contentTransitionViews.isEmpty()) {
                pairs.add(Pair.create(content, context.getString(R.string.transition_post_content)))
            } else {
                pairs.addAll(contentTransitionViews)
            }
        } else {
            // This is really only for if text content is not showing here, but might be in the new
            // activity. If this is not added then no transition occurs and it just fades in, but with
            // this the text content sort of comes out/in of the rest of the post
            pairs.add(Pair(binding.content, binding.content.transitionName))
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