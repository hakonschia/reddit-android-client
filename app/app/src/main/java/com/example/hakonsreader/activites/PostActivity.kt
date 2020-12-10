package com.example.hakonsreader.activites

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.transition.Transition
import android.transition.TransitionListenerAdapter
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView.SmoothScroller
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.api.enums.PostType
import com.example.hakonsreader.api.interfaces.ReplyableListing
import com.example.hakonsreader.api.model.RedditComment
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.constants.SharedPreferencesConstants
import com.example.hakonsreader.databinding.ActivityPostBinding
import com.example.hakonsreader.interfaces.LoadMoreComments
import com.example.hakonsreader.interfaces.OnReplyListener
import com.example.hakonsreader.misc.Util
import com.example.hakonsreader.recyclerviewadapters.CommentsAdapter
import com.example.hakonsreader.viewmodels.CommentsViewModel
import com.example.hakonsreader.views.Content
import com.example.hakonsreader.views.ContentVideo
import com.google.gson.Gson
import com.r0adkll.slidr.Slidr
import com.r0adkll.slidr.model.SlidrInterface
import com.squareup.picasso.Callback

/**
 * Activity to show a Reddit post with its comments
 */
class PostActivity : AppCompatActivity(), OnReplyListener {

    companion object {
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

    private var binding: ActivityPostBinding? = null
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
            binding?.parentLayout?.progress = savedInstanceState.getFloat(TRANSITION_STATE_KEY)
        }
    }

    override fun onResume() {
        super.onResume()
        App.get().setActiveActivity(this)

        if (videoPlayingWhenPaused) {
            binding?.post?.viewSelected()
        }
    }

    override fun onPause() {
        super.onPause()

        binding?.post?.let {
            videoPlayingWhenPaused = it.extras.getBoolean(ContentVideo.EXTRA_IS_PLAYING)
            it.viewUnselected()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding?.parentLayout?.progress?.let { outState.putFloat(TRANSITION_STATE_KEY, it) }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Ensure resources are freed when the activity exits
        binding?.post?.cleanUpContent()
        binding = null
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

    /**
     * Sets up [binding]
     */
    private fun setupBinding() {
        binding = ActivityPostBinding.inflate(layoutInflater)

        binding?.let {
            setContentView(it.root)

            // This is kinda hacky, but it looks weird if the "No comments yet" appears before the comments
            // have had a chance to load
            it.noComments = false
            it.commentChainShown = false

            // Go to first/last comment on long clicks on navigation buttons
            it.goToNextTopLevelComment.setOnLongClickListener(this::goToLastComment)
            it.goToPreviousTopLevelComment.setOnLongClickListener(this::goToFirstComment)

            it.commentsSwipeRefresh.setOnRefreshListener {
                // We're using our own loading icon, so remove this
                it.commentsSwipeRefresh.isRefreshing = false
                commentsViewModel?.restart()
            }
            it.commentsSwipeRefresh.setProgressBackgroundColorSchemeColor(
                    ContextCompat.getColor(this, R.color.colorAccent)
            )

            it.parentLayout.setTransitionListener(transitionListener)
        }
    }

    /**
     * Sets up [commentsViewModel]
     */
    private fun setupCommentsViewModel() {
        commentsViewModel = ViewModelProvider(this).get(CommentsViewModel::class.java)

        commentsViewModel?.let {
            it.getPost().observe(this, { newPost ->
                val postPreviouslySet = post != null
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

                binding?.noComments = comments.isEmpty()
            })

            it.onLoadingCountChange().observe(this, { up -> binding?.loadingIcon?.onCountChange(up) })
            it.getError().observe(this, { error ->
                Util.handleGenericResponseErrors(binding?.parentLayout, error.error, error.throwable)
            })
        }
    }

    /**
     * Sets up [ActivityPostBinding.comments]
     */
    private fun setupCommentsList() {
        commentsLayoutManager = LinearLayoutManager(this)
        commentsAdapter = CommentsAdapter(post!!)
        commentsAdapter?.let {
            it.replyListener = this
            it.commentIdChain = intent.extras?.getString(COMMENT_ID_CHAIN, "") ?: ""
            it.loadMoreCommentsListener = LoadMoreComments { comment, parent -> commentsViewModel?.loadMoreComments(comment, parent) }
            it.onChainShown = Runnable { binding?.commentChainShown = true }
        }

        binding?.let { bind ->
            bind.comments.adapter = commentsAdapter
            bind.comments.layoutManager = commentsLayoutManager
            bind.showAllComments.setOnClickListener {
                commentsAdapter?.commentIdChain = ""
                bind.commentChainShown = false
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

        binding?.let {
            it.post.setMaxHeight(maxHeight)
            it.post.hideScore = intent.extras?.getBoolean(HIDE_SCORE_KEY, false) == true
            // Don't allow to open the post again when we are now in the post
            it.post.setAllowPostOpen(false)
        }
    }


    /**
     * Updates the post info without re-drawing the content
     */
    private fun updatePostInfo() {
        binding?.let {
            it.setPost(post)
            it.post.updatePostInfo(post)
        }
    }

    /**
     * Sets [post] on [binding] and calls [setupCommentsList]
     */
    private fun onPostLoaded(extras: Bundle? = null) {
        binding?.let {
            it.setPost(post)
            it.post.redditPost = post

            if (extras != null) {
                it.post.extras = extras
            }
        }

        setupCommentsList()
    }

    /**
     * Gets the post ID from either the intent extras or the intent URI data if the activity was started
     * from a URI intent and loads the comments for the post
     */
    private fun loadComments() {
        val postJson = intent.extras?.getString(POST_KEY)
        val postId = if (postJson != null) {
            post = Gson().fromJson(postJson, RedditPost::class.java)

            binding?.post?.hideScore = intent.extras?.getBoolean(HIDE_SCORE_KEY) == true

            var postExtras: Bundle? = intent.extras?.getBundle(Content.EXTRAS)

            // TODO this doesn't work perfectly as the "loading" image is still shown sometimes for a split second
            // If we have an image wait with the transition until the image is loaded
            when {
                post?.getPostType() == PostType.IMAGE -> {
                    postponeEnterTransition()

                    binding?.post?.setImageLoadedCallback(object : Callback {
                        override fun onSuccess() {
                            startPostponedEnterTransition()
                        }

                        override fun onError(e: Exception) {
                            startPostponedEnterTransition()
                        }
                    })
                }
                post?.getPostType() == PostType.VIDEO -> {
                    // For videos we don't want to set the extras right away. If a video is playing during the
                    // animation the animation looks very choppy, so it should only be played at the end
                    window.sharedElementEnterTransition.addListener(object : TransitionListenerAdapter() {
                        override fun onTransitionEnd(transition: Transition?) {
                            super.onTransitionEnd(transition)

                            // TODO the thumbnail is shown the entire time, make it so the frame the video
                            //  ended at is shown instead
                            binding?.post?.extras = postExtras
                        }
                    })
                }
                else -> {
                    postExtras = intent.extras?.getBundle(Content.EXTRAS)
                }
            }

            onPostLoaded(postExtras)

            post?.id
        } else {
            intent.extras?.getString(POST_ID_KEY)
        }

        commentsViewModel?.let {
            if (postId != null) {
                it.postId = postId
                it.loadComments()
            }
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
        binding?.comments?.stopScroll()
        binding?.comments?.scrollToPosition(0)
        return true
    }

    /**
     * Scrolls to the last comment
     *
     * @param view Ignored
     * @return Always true, as this function will be used to indicate a long click has been handled
     */
    private fun goToLastComment(view: View): Boolean {
        binding?.comments?.stopScroll()
        binding?.comments?.scrollToPosition(commentsAdapter!!.itemCount - 1)
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
        binding?.comments?.stopScroll()
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
    public fun replyToPost(view: View) {
        post?.let { replyTo(it) }
    }

    /**
     * Transition listener that automatically pauses the video content when the end of the transition
     * has been reached
     */
    private val transitionListener = object : MotionLayout.TransitionListener {
        override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
            // Pause video when the transition has finished to the end
            // We could potentially pause it earlier, like when the transition is halfway done?
            // We also can start it when we reach the start, not sure if that is good or bad
            if (currentId == R.id.end) {
                binding?.post?.viewUnselected()
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