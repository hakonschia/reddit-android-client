package com.example.hakonsreader.activites

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.transition.Transition
import android.transition.TransitionListenerAdapter
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView.SmoothScroller
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.api.enums.PostType
import com.example.hakonsreader.api.interfaces.ReplyableListing
import com.example.hakonsreader.api.model.RedditComment
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.constants.SharedPreferencesConstants
import com.example.hakonsreader.databinding.ActivityPostBinding
import com.example.hakonsreader.interfaces.LoadMoreComments
import com.example.hakonsreader.interfaces.LockableSlidr
import com.example.hakonsreader.interfaces.OnReplyListener
import com.example.hakonsreader.misc.Util
import com.example.hakonsreader.recyclerviewadapters.CommentsAdapter
import com.example.hakonsreader.viewmodels.CommentsViewModel
import com.example.hakonsreader.views.Content
import com.example.hakonsreader.views.VideoPlayer
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.r0adkll.slidr.Slidr
import com.r0adkll.slidr.model.SlidrInterface
import com.squareup.picasso.Callback

/**
 * Activity to show a Reddit post with its comments
 */
class PostActivity : AppCompatActivity(), OnReplyListener, LockableSlidr {

    companion object {
        private const val TAG = "PostActivity"

        /**
         * The key used to store the stat of the post transition in saved instance states
         */
        private const val TRANSITION_STATE_KEY = "transitionState"

        /**
         * The key used for sending the post to this activity
         */
        const val POST_KEY = "post"

        /**
         * The key used for sending the ID of the post to this activity
         *
         * Use this is the post isn't retrieved when starting the activity
         */
        const val POST_ID_KEY = "post_id"

        /**
         * The key used to tell if the post score should be hidden
         */
        const val HIDE_SCORE_KEY = "hideScore"

        /**
         * The key used to tell the ID of the comment chain to show
         */
        const val COMMENT_ID_CHAIN = "commentIdChain"


        /**
         * Request code for opening a reply activity
         */
        const val REQUEST_REPLY = 1
    }

    private lateinit var binding: ActivityPostBinding
    private lateinit var slidrInterface: SlidrInterface

    private var commentsViewModel: CommentsViewModel? = null
    private var commentsAdapter: CommentsAdapter? = null
    private var commentsLayoutManager: LinearLayoutManager? = null

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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        slidrInterface = Slidr.attach(this)

        setupBinding()
        setupCommentsViewModel()
        setupPost()

        // No state saved means we have no comments, so load them
        if (savedInstanceState == null) {
            loadComments()
        } else {
            binding.parentLayout.progress = savedInstanceState.getFloat(TRANSITION_STATE_KEY)
        }
    }

    override fun onResume() {
        super.onResume()
        App.get().setActiveActivity(this)

        if (videoPlayingWhenPaused) {
            binding.post.viewSelected()
        }
    }

    override fun onPause() {
        super.onPause()

        videoPlayingWhenPaused = binding.post.extras.getBoolean(VideoPlayer.EXTRA_IS_PLAYING)
        binding.post.viewUnselected()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.parentLayout.progress.let { outState.putFloat(TRANSITION_STATE_KEY, it) }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Ensure resources are freed when the activity exits
        binding.post.cleanUpContent()
    }

    /**
     * Handles results for when a reply has been sent
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_REPLY && resultCode == RESULT_OK && data != null) {
            val newComment = Gson().fromJson(data.getStringExtra(ReplyActivity.LISTING_KEY), RedditComment::class.java)
            val parent = if (replyingTo is RedditComment) replyingTo as RedditComment else null
            commentsViewModel?.insertComment(newComment, parent)
        }
    }

    override fun lock(lock: Boolean) {
        if (lock) {
            slidrInterface.lock()
        } else {
            slidrInterface.unlock()
        }
    }


    /**
     * Sets up [binding]
     */
    private fun setupBinding() {
        binding = ActivityPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // This is kinda hacky, but it looks weird if the "No comments yet" appears before the comments
        // have had a chance to load
        binding.noComments = false
        binding.commentChainShown = false

        // Go to first/last comment on long clicks on navigation buttons
        binding.goToNextTopLevelComment.setOnLongClickListener(this::goToLastComment)
        binding.goToPreviousTopLevelComment.setOnLongClickListener(this::goToFirstComment)

        binding.commentsSwipeRefresh.setOnRefreshListener {
            // We're using our own loading icon, so remove this
            binding.commentsSwipeRefresh.isRefreshing = false
            commentsViewModel?.restart()
        }
        binding.commentsSwipeRefresh.setProgressBackgroundColorSchemeColor(
                ContextCompat.getColor(this, R.color.colorAccent)
        )

        binding.parentLayout.setTransitionListener(transitionListener)

        binding.expandOrCollapsePost.setOnLongClickListener { toggleTransitionEnabled(); true }

        val collapsePostByDefault = App.get().collapsePostsByDefaultWhenScrollingComments()
        val transition = binding.parentLayout.getTransition(R.id.postTransition)
        binding.expandOrCollapsePostBlock.visibility = if (collapsePostByDefault) {
            // This should be the default for the transition, but might as well set it
            transition.setEnable(true)
            GONE
        } else {
            transition.setEnable(false)
            VISIBLE
        }
    }

    /**
     * Sets up [commentsViewModel]
     */
    private fun setupCommentsViewModel() {
        commentsViewModel = ViewModelProvider(this).get(CommentsViewModel::class.java)

        commentsViewModel?.let {
            it.getPost().observe(this, { newPost ->
                val postPreviouslySet = binding.post.redditPost != null
                post = newPost

                // If we have a post already just update the info so the content isn't reloaded
                if (postPreviouslySet) {
                    updatePostInfo()
                } else {
                    onPostLoaded()
                }
            })

            it.getComments().observe(this, { comments ->
                // New comments are empty, previous comments are not, clear the previous comments
                if (comments.isEmpty() && commentsAdapter?.itemCount != 0) {
                    commentsAdapter?.clearComments()
                    return@observe
                }

                // TODO these values should probably be deleted at some point? Can check at startup if any of the values are
                //  over a few days old or something and delete those that are
                val lastTimeOpenedKey = post?.id + SharedPreferencesConstants.POST_LAST_OPENED_TIMESTAMP
                val preferences = getSharedPreferences(SharedPreferencesConstants.PREFS_NAME_POST_OPENED, MODE_PRIVATE)
                val lastTimeOpened = preferences.getLong(lastTimeOpenedKey, -1)

                // Update the value
                preferences.edit().putLong(lastTimeOpenedKey, System.currentTimeMillis() / 1000L).apply()

                commentsAdapter?.lastTimeOpened = lastTimeOpened
                commentsAdapter?.submitList(comments)

                binding.noComments = comments.isEmpty()
            })

            it.onLoadingCountChange().observe(this, { up -> binding.loadingIcon.onCountChange(up) })
            it.getError().observe(this, { error ->
                Util.handleGenericResponseErrors(binding.parentLayout, error.error, error.throwable)
            })
        }
    }

    /**
     * Sets up [ActivityPostBinding.comments], if [post] isn't `null`
     */
    private fun setupCommentsList() {
        post?.let { post ->
            commentsLayoutManager = LinearLayoutManager(this)

            commentsAdapter = CommentsAdapter(post).apply {
                replyListener = this@PostActivity
                commentIdChain = intent.extras?.getString(COMMENT_ID_CHAIN, "") ?: ""
                loadMoreCommentsListener = LoadMoreComments { comment, parent -> commentsViewModel?.loadMoreComments(comment, parent) }
                onChainShown = Runnable { binding.commentChainShown = true }
            }

            binding.comments.adapter = commentsAdapter
            binding.comments.layoutManager = commentsLayoutManager
            binding.showAllComments.setOnClickListener {
                commentsAdapter?.commentIdChain = ""
                binding.commentChainShown = false
            }
        }
    }

    /**
     * Sets up [ActivityPostBinding.post]
     */
    private fun setupPost() {
        // If we're in landscape the "height" is the width of the screen
        val portrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        val height = if (portrait) App.get().screenHeight else App.get().screenWidth
        val maxHeight = (height * (App.get().getMaxPostSizePercentage() / 100f)).toInt()

        binding.post.setMaxHeight(maxHeight)
        binding.post.hideScore = intent.extras?.getBoolean(HIDE_SCORE_KEY, false) == true
    }


    /**
     * Updates the post info without re-drawing the content
     *
     * @see onPostLoaded
     */
    private fun updatePostInfo() {
        binding.setPost(post)
        binding.post.updatePostInfo(post)
    }

    /**
     * This should be called when [post] has been set and should generate the view for the post
     *
     * [setupCommentsList] is automatically called
     *
     * @see updatePostInfo
     */
    private fun onPostLoaded(extras: Bundle? = null) {
        binding.setPost(post)
        binding.post.redditPost = post

        if (extras != null) {
            binding.post.extras = extras
        }

        setupCommentsList()
    }

    /**
     * Gets the post ID from either the intent extras or the intent URI data if the activity was started
     * from a URI intent and loads the comments for the post
     */
    private fun loadComments() {
        val postJson = intent.extras?.getString(POST_KEY)

        // Started from inside the app (post already loaded from before)
        val postId = if (postJson != null) {
            setPostFromJson(postJson)
        } else {
            intent.extras?.getString(POST_ID_KEY)
        }

        if (postId != null) {
            commentsViewModel?.let {
                it.postId = postId
                it.loadComments()
            }
        }
    }

    /**
     * Sets [post] from a JSON string
     *
     * If the post is an image post, then the enter transition is delayed until the image has loaded
     *
     * @param json The JSON to set the post from
     * @return The ID of the post, or null if the json is invalid
     */
    private fun setPostFromJson(json: String) : String? {
        val redditPost = Gson().fromJson(json, RedditPost::class.java)

        return if (redditPost != null) {
            post = redditPost
            val postExtras: Bundle? = intent.extras?.getBundle(Content.EXTRAS)

            when (redditPost.getPostType()) {
                // Postpone the enter transition until the image is loaded
                // TODO this doesn't work perfectly as the "loading" image is still shown sometimes for a split second
                PostType.IMAGE -> {
                    postponeEnterTransition()
                    binding.post.setImageLoadedCallback(object : Callback {
                        override fun onSuccess() {
                            startPostponedEnterTransition()
                        }
                        override fun onError(e: Exception) {
                            startPostponedEnterTransition()
                        }
                    })

                    onPostLoaded(postExtras)
                }

                // Add a transition listener that sets the extras for videos after the enter transition is done,
                // so that the video doesn't play during the transition (which looks odd since it's very choppy)
                PostType.VIDEO -> {
                    // Load the post, but don't set extras yet
                    onPostLoaded()

                    // For videos we don't want to set the extras right away. If a video is playing during the
                    // animation the animation looks very choppy, so it should only be played at the end
                    window.sharedElementEnterTransition.addListener(object : TransitionListenerAdapter() {
                        override fun onTransitionEnd(transition: Transition?) {
                            super.onTransitionEnd(transition)

                            // TODO the thumbnail is shown the entire time, make it so the frame the video
                            //  ended at is shown instead
                            binding.post.extras = postExtras
                        }
                    })
                }

                // Nothing special for the post, set the extras
                else -> onPostLoaded(postExtras)
            }

            redditPost.id
        } else {
            null
        }
    }

    /**
     * Scrolls to the next top level comment
     *
     * @param view Ignored
     */
    fun goToNextTopLevelComment(view: View) {
        val currentPos = commentsLayoutManager!!.findFirstVisibleItemPosition()
        val next = commentsAdapter!!.getNextTopLevelCommentPos(currentPos + 1)
        smoothScrollHelper(currentPos, next)
    }

    /**
     * Scrolls to the previous top level comment
     *
     * @param view Ignored
     */
    fun goToPreviousTopLevelComment(view: View) {
        val currentPos = commentsLayoutManager!!.findFirstVisibleItemPosition()
        // We're at the top so we can't scroll further up
        if (currentPos == 0) {
            return
        }
        val previous = commentsAdapter!!.getPreviousTopLevelCommentPos(currentPos - 1)
        smoothScrollHelper(currentPos, previous)
    }

    /**
     * Scrolls to the first comment
     *
     * @param view Ignored
     * @return Always true, as this function will be used to indicate a long click has been handled
     */
    private fun goToFirstComment(view: View): Boolean {
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
    private fun goToLastComment(view: View): Boolean {
        binding.comments.stopScroll()
        binding.comments.scrollToPosition(commentsAdapter!!.itemCount - 1)
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
        // Scrolling up
        val gapSize: Int = if (currentPos > scrollPos) {
            currentPos - scrollPos
        } else {
            // Scrolling down
            scrollPos - currentPos
        }

        // Stop the current scroll (done manually by the user) to avoid scrolling past the comment navigated to
        binding.comments.stopScroll()
        if (App.get().commentSmoothScrollThreshold() >= gapSize) {
            val smoothScroller: SmoothScroller = object : LinearSmoothScroller(this) {
                override fun getVerticalSnapPreference(): Int {
                    return SNAP_TO_START
                }
            }
            smoothScroller.targetPosition = scrollPos
            commentsLayoutManager?.startSmoothScroll(smoothScroller)
        } else {
            commentsLayoutManager?.scrollToPositionWithOffset(scrollPos, 0)
        }
    }

    /**
     * Replies to a comment or post
     *
     * @param listing The listing to reply to
     */
    override fun replyTo(listing: ReplyableListing) {
        replyingTo = listing

        val intent = Intent(this, ReplyActivity::class.java)
        intent.putExtra(ReplyActivity.LISTING_KIND_KEY, listing.kind)
        intent.putExtra(ReplyActivity.LISTING_KEY, Gson().toJson(listing))

        startActivityForResult(intent, REQUEST_REPLY)
        overridePendingTransition(R.anim.slide_up, R.anim.slide_down)
    }

    /**
     * Click listener for the reply to post
     *
     * @param view Ignored
     */
    fun replyToPost(view: View) {
        post?.let { replyTo(it) }
    }

    /**
     * Toggles the post expansion transition from being enabled and shows a snackbar that says if it
     * is now enabled or disabled
     */
    private fun toggleTransitionEnabled() {
        val transition = binding.parentLayout.getTransition(R.id.postTransition)
        val enable = !transition.isEnabled
        transition.setEnable(enable)

        val stringId = if (enable) {
            binding.expandOrCollapsePostBlock.visibility = GONE
            R.string.postTransitionEnabled
        } else {
            binding.expandOrCollapsePostBlock.visibility = VISIBLE
            R.string.postTransitionDisabled
        }

        Snackbar.make(binding.parentLayout, stringId, LENGTH_SHORT).show()
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