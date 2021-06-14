package com.example.hakonsreader.activities

import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.app.SharedElementCallback
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import com.example.hakonsreader.R
import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.enums.PostType
import com.example.hakonsreader.api.interfaces.ReplyableListing
import com.example.hakonsreader.api.model.RedditComment
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.constants.SharedPreferencesConstants
import com.example.hakonsreader.databinding.ActivityPostBinding
import com.example.hakonsreader.interfaces.OnReplyListener
import com.example.hakonsreader.misc.Settings
import com.example.hakonsreader.misc.handleGenericResponseErrors
import com.example.hakonsreader.recyclerviewadapters.CommentsAdapter
import com.example.hakonsreader.viewmodels.CommentsViewModel
import com.example.hakonsreader.views.Content
import com.example.hakonsreader.views.ContentVideo
import com.example.hakonsreader.views.VideoPlayer
import com.example.hakonsreader.views.util.goneIf
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.r0adkll.slidr.Slidr
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Activity to show a Reddit post with its comments
 */
@AndroidEntryPoint
class PostActivity : BaseActivity(), OnReplyListener {

    companion object {
        @Suppress("unused")
        private const val TAG = "PostActivity"

        /**
         * The key used to store the state of the post transition in saved instance states
         *
         * The value stored with this key is a [Float]
         */
        private const val SAVED_TRANSITION_STATE_KEY = "saved_transitionState"

        /**
         * The key used in [onSaveInstanceState] to store if the transition is enabled
         *
         * The value stored with this key is a [Boolean]. This value being `true` means the transition
         * is enabled (ie. the post collapses automatically)
         */
        private const val SAVED_TRANSITION_ENABLED_KEY = "saved_transitionEnabled"


        /**
         * The key used for sending the post to this activity
         *
         * The value of this key should be a JSON string representing the post
         */
        const val EXTRAS_POST_KEY = "extras_PostActivity_post"

        /**
         * The key used for sending the ID of the post to this activity
         *
         * Use this is the post isn't retrieved when starting the activity
         */
        const val EXTRAS_POST_ID_KEY = "extras_PostActivity_postId"

        /**
         * The key used to tell if the post score should be hidden
         */
        const val EXTRAS_HIDE_SCORE_KEY = "extras_PostActivity_hideScore"

        /**
         * The key used to tell the ID of the comment chain to show
         */
        const val EXTRAS_COMMENT_ID_CHAIN = "extras_PostActivity_commentIdChain"


        /**
         * A Bitmap which can be used to hold an already loaded image. This should be set just before the
         * activity is started and will be nulled when the activity is destroyed
         */
        var BITMAP: Bitmap? = null
    }

    @Inject
    lateinit var api: RedditApi

    private lateinit var binding: ActivityPostBinding

    private val commentsViewModel: CommentsViewModel by viewModels()

    /**
     * The post shown in the activity
     */
    private var post: RedditPost? = null

    /**
     * When sending a reply to the post/a comment in the post, this will represent that
     * listing being replied to
     */
    private var replyingTo: ReplyableListing? = null

    /**
     * True if the video content for the post was playing when [onPause] was called
     */
    private var videoPlayingWhenPaused = false

    /**
     * The max height the post can have. This is for the entire post, ie. post info, content, and post bar combined
     */
    private val maxPostHeight by lazy {
        getHeightForPost(forWhenCollapsedDisabled = false)
    }

    /**
     * The max height the post can have when the post collapse has been disabled.
     * This is for the entire post, ie. post info, content, and post bar combined
     */
    private val maxPostHeightWhenCollapsedDisabled by lazy {
        getHeightForPost(forWhenCollapsedDisabled = true)
    }

    /**
     * Handles results when adding replies to the post (only top-level comments, not replies
     * to other comments in the post)
     */
    private val replyActivityResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val data = result.data ?: return@registerForActivityResult

        val newComment = Gson().fromJson(data.getStringExtra(ReplyActivity.EXTRAS_LISTING), RedditComment::class.java)
        val parent = if (replyingTo is RedditComment) replyingTo as RedditComment else null
        commentsViewModel.insertComment(newComment, parent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Slidr.attach(this)

        setupBinding()
        setupCommentsViewModel()
        setupCommentsList()

        // No state saved means we have no comments, so load them
        if (savedInstanceState == null) {
            // We only want to set this once, as if the chain is removed it shouldn't be set again on config changes
            intent.extras?.getString(EXTRAS_COMMENT_ID_CHAIN)?.let {
                commentsViewModel.showChain(it)
            }
            loadComments()
        } else {
            binding.parentLayout.progress = savedInstanceState.getFloat(SAVED_TRANSITION_STATE_KEY)

            val transitionEnabled = savedInstanceState.getBoolean(SAVED_TRANSITION_ENABLED_KEY, Settings.collapsePostsByDefaultWhenScrollingComments())
            enableTransition(transitionEnabled, showSnackbar = false, updateHeight = false)
        }
    }

    override fun onResume() {
        super.onResume()

        if (videoPlayingWhenPaused) {
            binding.post.viewSelected()
        }
    }

    override fun onPause() {
        super.onPause()

        // This is only really for when the activity is destroyed, but onPause is called first
        // and it calls viewUnselected which would make the extras PAUSED be false
        commentsViewModel.savedExtras = binding.post.extras

        videoPlayingWhenPaused = binding.post.extras.getBoolean(VideoPlayer.EXTRA_IS_PLAYING)
        binding.post.viewUnselected()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putFloat(SAVED_TRANSITION_STATE_KEY, binding.parentLayout.progress)
        outState.putBoolean(SAVED_TRANSITION_ENABLED_KEY, binding.parentLayout.getTransition(R.id.postTransition).isEnabled)
    }

    override fun onDestroy() {
        super.onDestroy()

        // Ensure resources are freed when the activity exits
        binding.post.cleanUpContent()
        binding.post.lifecycleOwner = null
        
        BITMAP = null
    }

    /**
     * Sets up [binding]
     */
    private fun setupBinding() {
        binding = ActivityPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding) {
            // This is kinda hacky, but it looks weird if the "No comments yet" appears before the comments
            // have had a chance to load
            noComments = false
            commentChainShown = false

            post.bitmap = BITMAP

            post.lifecycleOwner = this@PostActivity

            post.maxHeight = maxPostHeight
            post.hideScore = intent.extras?.getBoolean(EXTRAS_HIDE_SCORE_KEY, false) == true

            // Go to first/last comment on long clicks on navigation buttons
            goToNextTopLevelComment.setOnLongClickListener(this@PostActivity::goToLastComment)
            goToPreviousTopLevelComment.setOnLongClickListener(this@PostActivity::goToFirstComment)

            commentsSwipeRefresh.setOnRefreshListener {
                // We're using our own loading icon, so remove this
                commentsSwipeRefresh.isRefreshing = false

                // If the app has been idled the post ID and such might be killed and the system will
                // try to restore the activity, so this would throw an exception since the post ID isn't set
                try {
                    commentsViewModel.restart()
                } catch (e: IllegalStateException) {
                    finish()
                }
            }
            commentsSwipeRefresh.setProgressBackgroundColorSchemeColor(
                    ContextCompat.getColor(this@PostActivity, R.color.colorAccent)
            )

            parentLayout.setTransitionListener(transitionListener)

            expandOrCollapsePost.setOnLongClickListener { toggleTransitionEnabled(); true }
        }

        val collapsePostByDefault = Settings.collapsePostsByDefaultWhenScrollingComments()
        enableTransition(collapsePostByDefault, showSnackbar = false, updateHeight = !collapsePostByDefault)
    }

    /**
     * Observes values from [commentsViewModel]
     */
    private fun setupCommentsViewModel() {
        with (commentsViewModel) {
            preferences = getSharedPreferences(SharedPreferencesConstants.PREFS_NAME_POST_OPENED, MODE_PRIVATE)

            commentUpdatedCallback = { position ->
                (binding.comments.adapter as CommentsAdapter).notifyItemChanged(position)
            }

            post.observe(this@PostActivity) {
                if (it != null) {
                    onNewPostInfo(it, savedExtras)

                    // We don't want to accidentally set this more than once, this is only for
                    // configuration changes (if we manually refreshed the comments this will trigger again)
                    savedExtras = null
                }
            }

            comments.observe(this@PostActivity) { comments ->
                binding.commentChainShown = chainId != null

                val adapter = binding.comments.adapter as CommentsAdapter

                // If the ViewModel has a chain and the adapter doesn't then a chain was just set
                // and we want to store the layout state. We only want to store this when the adapter doesn't have
                // a chain, so that if we're in a chain and go into another one then the saved state is still
                // for the "all comments" state
                if (chainId != null && adapter.currentChainId == null) {
                    if (adapter.currentChainId == null) {
                        layoutState = binding.comments.layoutManager?.onSaveInstanceState()
                    }
                } else if (chainId == null && adapter.currentChainId != null && layoutState != null) {
                    // If the ViewModel no longer has a chain, but the adapter does, then the chain was
                    // removed and we want to restore the layout state if one was saved
                    binding.comments.layoutManager?.onRestoreInstanceState(layoutState)
                }

                adapter.lastTimeOpened = getLastTimePostOpened()
                adapter.submitList(comments)

                binding.noComments = comments.isEmpty()
            }

            isLoading.observe(this@PostActivity, { isLoading ->
                binding.progressBarLayout.goneIf(!isLoading)
            })

            error.observe(this@PostActivity, { error ->
                handleGenericResponseErrors(binding.parentLayout, error.error, error.throwable)
            })
        }
    }

    /**
     * Sets up [ActivityPostBinding.comments]
     */
    private fun setupCommentsList() {
        with(binding) {
            val adapter = CommentsAdapter(api, commentsViewModel).apply {
                replyListener = this@PostActivity
            }

            comments.adapter = adapter
            comments.layoutManager = LinearLayoutManager(this@PostActivity)
            showAllComments.setOnClickListener { commentsViewModel.removeChain() }
        }
    }

    /**
     * Called when post info has been retrieved, either if when first loading or when it has
     * updated
     *
     * Calls [updatePostInfo] or [onPostLoaded] accordingly and sets [post]
     */
    private fun onNewPostInfo(newPost: RedditPost, extras: Bundle? = null) {
        val postPreviouslySet = binding.post.redditPost != null
        post = newPost

        (binding.comments.adapter as CommentsAdapter).post = newPost

        // If we have a post already just update the info so the content isn't reloaded
        if (postPreviouslySet) {
            updatePostInfo(newPost)
        } else {
            onPostLoaded(newPost, extras)
        }
    }

    /**
     * Updates the post info without re-drawing the content
     *
     * @see onPostLoaded
     */
    private fun updatePostInfo(newPost: RedditPost) {
        binding.setPost(newPost)
        binding.post.updatePostInfo(newPost)
    }

    /**
     * This should be called when [post] has been set and should generate the view for the post
     *
     * @see updatePostInfo
     */
    private fun onPostLoaded(newPost: RedditPost, extras: Bundle? = null) {
        binding.setPost(newPost)

        if (extras != null) {
            binding.post.extras = extras
        }

        binding.post.redditPost = newPost
    }

    /**
     * Gets the post ID from either the intent extras or the intent URI data if the activity was started
     * from a URI intent and loads the comments for the post
     */
    private fun loadComments() {
        val postJson = intent.extras?.getString(EXTRAS_POST_KEY)

        // Load 3rd API calls if the post hasn't been loaded already, as we then have to draw
        // the content of the post. If we have a post already the 3rd party calls should have been
        // made already
        var loadThirdParty = true
        var post: RedditPost? = null

        // Started from inside the app (post already loaded from before)
        val postId = if (postJson != null) {
            loadThirdParty = false
            post = setPostFromJson(postJson)
            post?.id
        } else {
            intent.extras?.getString(EXTRAS_POST_ID_KEY)
        }

        if (postId != null) {
            commentsViewModel.let {
                it.postId = postId

                try {
                    it.loadComments(loadThirdParty, post?.thirdPartyObject)
                } catch (e: IllegalStateException) {
                    finish()
                }
            }
        }
    }

    /**
     * Sets [post] from a JSON string
     *
     * If the post is an image post, then the enter transition is delayed until the image has loaded
     *
     * @param json The JSON to set the post from
     * @return The parsed post, or null if the json is invalid
     */
    private fun setPostFromJson(json: String) : RedditPost? {
        val redditPost = Gson().fromJson(json, RedditPost::class.java)

        return if (redditPost != null) {
            val postExtras: Bundle? = intent.extras?.getBundle(Content.EXTRAS)

            when (redditPost.getPostType()) {
                // Add a transition listener that sets the extras for videos after the enter transition is done,
                // so that the video doesn't play during the transition (which looks odd since it's very choppy)
                PostType.VIDEO -> {
                    // Load the post, but don't set extras yet
                    onNewPostInfo(redditPost)

                    val content = binding.post.getContent() as ContentVideo

                    // If a thumbnail was passed to the activity it will be used as the thumbnail during
                    // the transition
                    // Otherwise we should love the default thumbnail
                    if (BITMAP == null) {
                        content.loadThumbnail()
                    }

                    content.setOnVideoFullscreenListener { contentVideo ->
                        val intent = Intent(this, VideoActivity::class.java).apply {
                            putExtra(VideoActivity.EXTRAS_EXTRAS, contentVideo.extras)
                        }

                        // Pause the video here so it doesn't play both places
                        contentVideo.viewUnselected()
                        startActivity(intent)
                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                    }

                    // We want to set this right away to ensure that the audio icon doesn't appear
                    // and disappear when the extras are set
                    val hasAudio = postExtras?.getBoolean(VideoPlayer.EXTRA_HAS_AUDIO) ?: true
                    content.showAudioIcon(hasAudio)

                    // For videos we don't want to set the extras right away. If a video is playing during the
                    // animation the animation looks very choppy, so it should only be played at the end
                    setEnterSharedElementCallback(object : SharedElementCallback() {
                        override fun onSharedElementEnd(sharedElementNames: MutableList<String>?, sharedElements: MutableList<View>?, sharedElementSnapshots: MutableList<View>?) {
                            // Enable this after the transition is over as the thumbnail kind of jumps sometimes if
                            // animations are enabled
                            content.enableControllerTransitions(true)

                            if (postExtras != null) {
                                binding.post.extras = postExtras
                            }
                        }
                    })
                }

                // Nothing special for the post, set the extras
                else -> onNewPostInfo(redditPost, postExtras)
            }

            redditPost
        } else {
            null
        }
    }

    /**
     * Scrolls to the next top level comment
     *
     * @param view Ignored
     */
    fun goToNextTopLevelComment(@Suppress("UNUSED_PARAMETER")view: View) {
        val layoutManager = binding.comments.layoutManager as LinearLayoutManager
        val adapter = binding.comments.adapter as CommentsAdapter

        val currentPos = layoutManager.findFirstVisibleItemPosition()
        val next = adapter.getNextTopLevelCommentPos(currentPos + 1)
        smoothScrollHelper(currentPos, next)
    }

    /**
     * Scrolls to the previous top level comment
     *
     * @param view Ignored
     */
    fun goToPreviousTopLevelComment(@Suppress("UNUSED_PARAMETER")view: View) {
        val layoutManager = binding.comments.layoutManager as LinearLayoutManager
        val adapter = binding.comments.adapter as CommentsAdapter

        val currentPos = layoutManager.findFirstVisibleItemPosition()
        // We're at the top so we can't scroll further up
        if (currentPos == 0) {
            return
        }
        val previous = adapter.getPreviousTopLevelCommentPos(currentPos - 1)
        smoothScrollHelper(currentPos, previous)
    }

    /**
     * Scrolls to the first comment
     *
     * @param view Ignored
     * @return Always true, as this function will be used to indicate a long click has been handled
     */
    private fun goToFirstComment(@Suppress("UNUSED_PARAMETER")view: View): Boolean {
        binding.comments.stopScroll()
        binding.comments.scrollToPosition(0)
        return true
    }

    /**
     * Scrolls to the last comment
     *
     * @param view Ignored
     * @return Always true, as this function will be used to indicate a long click has been handled
     */
    private fun goToLastComment(@Suppress("UNUSED_PARAMETER")view: View): Boolean {
        binding.comments.stopScroll()
        binding.comments.scrollToPosition(binding.comments.adapter!!.itemCount - 1)
        return true
    }


    /**
     * Scrolls [ActivityPostBinding.comments] to a given position, respecting the users
     * setting for whether or not the scroll should be smooth or instant.
     *
     * @param currentPos The current scroll position
     * @param scrollPos The position to scroll to
     */
    private fun smoothScrollHelper(currentPos: Int, scrollPos: Int) {
        val layoutManager = binding.comments.layoutManager as LinearLayoutManager

        // Scrolling up
        val gapSize = if (currentPos > scrollPos) {
            currentPos - scrollPos
        } else {
            // Scrolling down
            scrollPos - currentPos
        }

        // Stop the current scroll (done manually by the user) to avoid scrolling past the comment navigated to
        binding.comments.stopScroll()
        if (Settings.commentSmoothScrollThreshold() >= gapSize) {
            val smoothScroller = object : LinearSmoothScroller(this) {
                override fun getVerticalSnapPreference(): Int {
                    return SNAP_TO_START
                }
            }.apply { targetPosition = scrollPos }

            layoutManager.startSmoothScroll(smoothScroller)
        } else {
            layoutManager.scrollToPositionWithOffset(scrollPos, 0)
        }
    }

    /**
     * Replies to a comment or post
     *
     * @param listing The listing to reply to
     */
    override fun replyTo(listing: ReplyableListing) {
        replyingTo = listing

        val intent = Intent(this, ReplyActivity::class.java).apply {
            putExtra(ReplyActivity.EXTRAS_LISTING_KIND, listing.kind)
            putExtra(ReplyActivity.EXTRAS_LISTING, Gson().toJson(listing))
        }

        replyActivityResult.launch(intent)
        overridePendingTransition(R.anim.slide_up, R.anim.slide_down)
    }

    /**
     * Click listener for the reply to post
     *
     * @param view Ignored
     */
    fun replyToPost(@Suppress("UNUSED_PARAMETER")view: View) {
        post?.let { replyTo(it) }
    }

    /**
     * Toggles the post expansion transition from being enabled and shows a snackbar that says if it
     * is now enabled or disabled
     */
    private fun toggleTransitionEnabled() {
        val transition = binding.parentLayout.getTransition(R.id.postTransition)
        enableTransition(!transition.isEnabled)
    }

    /**
     * Enables or disables the post collapse transition and optionally shows a snackbar to notify the
     * user of the change
     *
     * @param enable True to enable the transition, false to disable
     * @param showSnackbar True to show a snackbar. Default to `true`
     * @param updateHeight True to update the height of the content
     */
    private fun enableTransition(enable: Boolean, showSnackbar: Boolean = true, updateHeight: Boolean = true) {
        val transition = binding.parentLayout.getTransition(R.id.postTransition)
        transition.setEnable(enable)

        val stringId = if (enable) {
            if (updateHeight) {
                binding.post.updateMaxHeight(maxPostHeight)
            }

            binding.expandOrCollapsePostBlock.visibility = GONE
            R.string.postTransitionEnabled
        } else {
            if (updateHeight) {
                binding.post.updateMaxHeight(maxPostHeightWhenCollapsedDisabled)
            }

            binding.expandOrCollapsePostBlock.visibility = VISIBLE
            R.string.postTransitionDisabled
        }

        if (showSnackbar) {
            Snackbar.make(binding.parentLayout, stringId, LENGTH_SHORT).show()
        }
    }

    /**
     * Gets the height to use for the post
     *
     * @param forWhenCollapsedDisabled True if the height should be for when the post collapse is disabled
     */
    private fun getHeightForPost(forWhenCollapsedDisabled: Boolean) : Int {
        // If we're in landscape the "height" is the width of the screen
        val portrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        val height = if (portrait) Resources.getSystem().displayMetrics.heightPixels else Resources.getSystem().displayMetrics.widthPixels

        return if (forWhenCollapsedDisabled) {
            (height * (Settings.getMaxPostSizePercentageWhenCollapseDisabled() / 100f)).toInt()
        } else {
            (height * (Settings.getMaxPostSizePercentage() / 100f)).toInt()
        }
    }

    /**
     * Transition listener that automatically pauses the video content when the end of the transition
     * has been reached
     */
    private val transitionListener = object : MotionLayout.TransitionListener {
        override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
            // We could potentially pause it earlier, like when the transition is halfway done?

            // viewSelected has to be called for gallery slidr locks, but you might not want videos to
            // play again if they weren't playing before. We could potentially store the extras and if
            // it's a video and ContentVideo.EXTRA_IS_PLAYING is true, then we call viewSelected, or something
            // (we probably have to check if it's a video manually though)
            if (currentId == R.id.start) {
                binding.post.viewSelected()
            } else if (currentId == R.id.end) {
                binding.post.viewUnselected()
            }
        }

        override fun onTransitionStarted(motionLayout: MotionLayout?, p1: Int, p2: Int) {
            // Not implemented
        }
        override fun onTransitionChange(motionLayout: MotionLayout?, p1: Int, p2: Int, p3: Float) {
            // Not implemented
        }
        override fun onTransitionTrigger(motionLayout: MotionLayout?, p1: Int, p2: Boolean, p3: Float) {
            // Not implemented
        }
    }
}