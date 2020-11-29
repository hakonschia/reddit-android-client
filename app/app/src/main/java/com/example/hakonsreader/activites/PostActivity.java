package com.example.hakonsreader.activites;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.transition.Transition;
import android.transition.TransitionListenerAdapter;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.api.enums.PostType;
import com.example.hakonsreader.api.model.RedditComment;
import com.example.hakonsreader.api.model.RedditListing;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.constants.SharedPreferencesConstants;
import com.example.hakonsreader.databinding.ActivityPostBinding;
import com.example.hakonsreader.interfaces.LockableSlidr;
import com.example.hakonsreader.misc.Util;
import com.example.hakonsreader.recyclerviewadapters.CommentsAdapter;
import com.example.hakonsreader.viewmodels.CommentsViewModel;
import com.example.hakonsreader.views.Content;
import com.example.hakonsreader.views.ContentVideo;
import com.google.gson.Gson;
import com.r0adkll.slidr.Slidr;
import com.r0adkll.slidr.model.SlidrInterface;
import com.squareup.picasso.Callback;

/**
 * Activity to show a Reddit post with its comments
 */
public class PostActivity extends AppCompatActivity implements LockableSlidr {
    private static final String TAG = "PostActivity";

    /**
     * The key used to store the stat of the post transition in saved instance states
     */
    private static final String TRANSITION_STATE_KEY = "transitionState";


    /**
     * The key used for sending the post to this activity
     */
    public static final String POST_KEY = "post";

    /**
     * The key used for sending the ID of the post to this activity
     *
     * <p>Use this is the post isn't retrieved when starting the activity</p>
     */
    public static final String POST_ID_KEY = "post_id";

    /**
     * They key used to tell what kind of listing something is
     */
    public static final String KIND_KEY = "kind";

    /**
     * The key used to tell if the post score should be hidden
     */
    public static final String HIDE_SCORE_KEY = "hideScore";

    /**
     * The key used to tell the ID of the comment chain to show
     */
    public static final String COMMENT_ID_CHAIN = "commentIdChain";


    /**
     * Request code for opening a reply activity
     */
    public static final int REQUEST_REPLY = 1;


    private ActivityPostBinding binding;

    private CommentsViewModel commentsViewModel;
    private CommentsAdapter commentsAdapter;
    private LinearLayoutManager layoutManager;
    private boolean videoPlayingWhenPaused;

    private RedditPost post;
    private RedditListing replyingTo;
    private SlidrInterface slidrInterface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        slidrInterface = Slidr.attach(this);
        binding = ActivityPostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // This is kinda hacky, but it looks weird if the "No comments yet" appears before the comments
        binding.setNoComments(false);
        binding.setCommentChainShown(false);

        this.setupCommentsViewModel();
        this.setupPost();

        if (savedInstanceState == null) {
            this.loadComments();
        } else {
            binding.parentLayout.setProgress(savedInstanceState.getFloat(TRANSITION_STATE_KEY));
        }

        // Go to first/last comment on longclicks on navigation buttons
        binding.goToPreviousTopLevelComment.setOnLongClickListener(this::goToFirstComment);
        binding.goToNextTopLevelComment.setOnLongClickListener(this::goToLastComment);

        binding.commentsSwipeRefresh.setOnRefreshListener(() -> {
            binding.commentsSwipeRefresh.setRefreshing(false);
            commentsViewModel.restart();
        });
        binding.commentsSwipeRefresh.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(this, R.color.colorAccent));
        binding.parentLayout.addTransitionListener(transitionListener);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putFloat(TRANSITION_STATE_KEY, binding.parentLayout.getProgress());
    }

    @Override
    protected void onPause() {
        super.onPause();

        Bundle data = binding.post.getExtras();

        videoPlayingWhenPaused = data.getBoolean(ContentVideo.EXTRA_IS_PLAYING);
        binding.post.viewUnselected();
    }

    @Override
    protected void onResume() {
        super.onResume();

        App.get().setActiveActivity(this);

        // Only resume if the video was actually playing when pausing
        if (videoPlayingWhenPaused) {
            binding.post.viewSelected();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Ensure resources are freed when the activity exits
        binding.post.cleanUpContent();
        binding = null;
    }

    /**
     * Handles results for when a reply has been sent
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_REPLY && data != null) {
                RedditComment newComment = new Gson().fromJson(data.getStringExtra(ReplyActivity.LISTING_KEY), RedditComment.class);
                RedditComment parent = replyingTo instanceof RedditComment ? (RedditComment) replyingTo : null;

                commentsViewModel.insertComment(newComment, parent);
            }
        }
    }

    /**
     * Sets up and calls various bindings on {@link ActivityPostBinding#post}
     */
    private void setupPost() {
        // TODO When the post is opened from an intent outside the app (from intent-filter) this doesn't work
        // If we're in landscape the "height" is the width of the screen
        boolean portrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        int height = portrait ? App.get().getScreenHeight() : App.get().getScreenWidth();
        int maxHeight = (int)(height * (App.get().getMaxPostSizePercentage() / 100f));

        binding.post.setMaxHeight(maxHeight);
        binding.post.setHideScore(getIntent().getExtras().getBoolean(HIDE_SCORE_KEY));
        // Don't allow to open the post again when we are now in the post
        binding.post.setAllowPostOpen(false);
    }

    /**
     * Sets up {@link PostActivity#commentsViewModel}
     */
    private void setupCommentsViewModel() {
        commentsViewModel = new ViewModelProvider(this).get(CommentsViewModel.class);
        commentsViewModel.onLoadingCountChange().observe(this, binding.loadingIcon::onCountChange);
        commentsViewModel.getPost().observe(this, newPost -> {
            boolean postPreviouslySet = post != null;
            post = newPost;

            // If we have a post already just update the info (the content gets reloaded which looks weird for videos)
            if (postPreviouslySet) {
                this.updatePostInfo();
            } else {
                this.onPostLoaded(null);
            }
        });
        commentsViewModel.getComments().observe(this, comments -> {
            // Check if there are any comments from before to avoid "No comments" not appearing when comments are reloaded
            if (comments.isEmpty() && commentsAdapter.getItemCount() != 0) {
                commentsAdapter.clearComments();
                return;
            }

            // Get the last time the post was opened (last time comments were retrieved)
            String lastTimeOpenedKey = post.getId() + SharedPreferencesConstants.POST_LAST_OPENED_TIMESTAMP;
            SharedPreferences sharedPreferences = getSharedPreferences(SharedPreferencesConstants.PREFS_NAME, MODE_PRIVATE);
            long lastTimePostOpened = sharedPreferences.getLong(lastTimeOpenedKey, -1);
            // Update the value
            // TODO these values should probably be deleted at some point? Can check at startup if any of the values are
            //  over a few days old or something and delete those that are
            sharedPreferences.edit().putLong(lastTimeOpenedKey, System.currentTimeMillis() / 1000L).apply();

            commentsAdapter.setLastTimePostOpened(lastTimePostOpened);
            commentsAdapter.setComments(comments);

            binding.setNoComments(comments.isEmpty());
        });
        commentsViewModel.getError().observe(this, error -> Util.handleGenericResponseErrors(binding.parentLayout, error.getError(), error.getThrowable()));
    }

    /**
     * Gets the post ID from either the intent extras or the intent URI data if the activity was started
     * from a URI intent
     */
    private void loadComments() {
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();

        String postId;
        String p = extras.getString(POST_KEY);

        // If activity is started with post data
        if (p != null) {
            post = new Gson().fromJson(p, RedditPost.class);
            postId = post.getId();
            binding.post.setHideScore(extras.getBoolean(PostActivity.HIDE_SCORE_KEY));

            // If we have an image ensure the image is fully loaded before we start the transition
            // This is to avoid images showing the placeholder image during the transition. Even if
            // Picasso is caching the image, it sometimes still needs to load the image from the cache
            // which looks weird
            // This won't produce a very noticeable delay, as it doesn't take a long time to load the image
            // TODO this doesn't really work as expected, plus it messes up the max height
            if (post.getPostType() == PostType.IMAGE) {
                postponeEnterTransition();

                binding.post.setImageLoadedCallback(new Callback() {
                    @Override
                    public void onSuccess() {
                        startPostponedEnterTransition();
                    }
                    @Override
                    public void onError(Exception e) {
                        startPostponedEnterTransition();
                    }
                });
            }

            Bundle postExtras = null;

            // For videos we don't want to set the extras right away. If a video is playing during the
            // animation the animation looks very choppy, so it should only be played at the end
            if (post.getPostType() == PostType.VIDEO) {
                // Since we have the post loaded already we have a transition as well (the activity is started
                // from clicking on a list)
                getWindow().getSharedElementEnterTransition().addListener(new TransitionListenerAdapter() {
                    @Override
                    public void onTransitionEnd(Transition transition) {
                        super.onTransitionEnd(transition);

                        // TODO the thumbnail is shown the entire time, make it so the frame the video
                        //  ended at is shown instead
                        binding.post.setExtras(getIntent().getExtras().getBundle(Content.EXTRAS));
                    }
                });
            } else {
                postExtras = getIntent().getExtras().getBundle(Content.EXTRAS);
            }

            this.onPostLoaded(postExtras);
        } else {
            // If post is started with only the ID of the post
            postId = extras.getString(POST_ID_KEY);
        }

        commentsViewModel.setPostId(postId);
        commentsViewModel.loadComments();
    }

    /**
     * Called when {@link PostActivity#post} has been set.
     *
     * <p>Notifies the view about the new post data and calls {@link PostActivity#setupCommentsList()}</p>
     *
     * @param extras The extras to set after the post has been created. This can be {@code null}
     */
    private void onPostLoaded(@Nullable Bundle extras) {
        binding.setPost(post);
        binding.post.setRedditPost(post);
        this.setupCommentsList();

        if (extras != null) {
            binding.post.setExtras(extras);
        }
    }

    /**
     * Update the information about the post in the UI, but does not make the content update
     */
    private void updatePostInfo() {
        binding.setPost(post);
        binding.post.updatePostInfo(post);
    }

    /**
     * Sets up {@link ActivityPostBinding#comments}
     */
    private void setupCommentsList() {
        commentsAdapter = new CommentsAdapter(post);
        commentsAdapter.setOnReplyListener(this::replyTo);
        commentsAdapter.setCommentIdChain(getIntent().getExtras().getString(COMMENT_ID_CHAIN, ""));
        commentsAdapter.setLoadMoreCommentsListener((c, p) -> commentsViewModel.loadMoreComments(c, p));

        layoutManager = new LinearLayoutManager(this);
        binding.comments.setAdapter(commentsAdapter);
        binding.comments.setLayoutManager(layoutManager);

        commentsAdapter.setOnChainShown(() -> binding.setCommentChainShown(true));
        binding.showAllComments.setOnClickListener(v -> {
            commentsAdapter.setCommentIdChain("");
            binding.setCommentChainShown(false);
        });
    }

    /**
     * Scrolls to the next top level comment
     * @param view Ignored
     */
    public void goToNextTopLevelComment(View view) {
        int currentPos = layoutManager.findFirstVisibleItemPosition();
        // Add 1 so that we can go directly from a top level to the next without scrolling
        int next = commentsAdapter.getNextTopLevelCommentPos(currentPos + 1);

        // Stop the current scroll (done manually by the user) to avoid scrolling past the comment navigated to
        binding.comments.stopScroll();

        // Scroll to the position, with 0 pixels offset from the top
        layoutManager.scrollToPositionWithOffset(next, 0);
    }

    /**
     * Scrolls to the previous top level comment
     * @param view Ignored
     */
    public void goToPreviousTopLevelComment(View view) {
        int currentPos = layoutManager.findFirstVisibleItemPosition();
        // Subtract 1 so that we can go directly from a top level to the previous without scrolling
        int previous = commentsAdapter.getPreviousTopLevelCommentPos(currentPos - 1);

        // Stop the current scroll (done manually by the user) to avoid scrolling past the comment navigated to
        binding.comments.stopScroll();
        layoutManager.scrollToPositionWithOffset(previous, 0);
    }

    /**
     * Scrolls to the first comment
     *
     * @param view Ignored
     * @return Always true, as this function will be used to indicate a long click has been handled
     */
    public boolean goToFirstComment(View view) {
        binding.comments.stopScroll();
        binding.comments.scrollToPosition(0);
        return true;
    }

    /**
     * Scrolls to the last comment
     *
     * @param view Ignored
     * @return Always true, as this function will be used to indicate a long click has been handled
     */
    public boolean goToLastComment(View view) {
        binding.comments.stopScroll();
        binding.comments.scrollToPosition(commentsAdapter.getItemCount() - 1);
        return true;
    }


    /**
     * Replies to a comment or post
     *
     * @param listing The listing to reply to
     */
    private void replyTo(RedditListing listing) {
        replyingTo = listing;

        // Open activity or fragment or something to allow reply
        Intent intent = new Intent(this, ReplyActivity.class);
        intent.putExtra(ReplyActivity.LISTING_KIND_KEY, listing.getKind());
        intent.putExtra(ReplyActivity.LISTING_KEY, new Gson().toJson(listing));

        startActivityForResult(intent, REQUEST_REPLY);
        overridePendingTransition(R.anim.slide_up, R.anim.slide_down);
    }

    /**
     * Click listener for the post reply button
     * @param view Ignored
     */
    public void replyToPost(View view) {
        this.replyTo(post);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void lock(boolean lock) {
        if (lock) {
            slidrInterface.lock();
        } else {
            slidrInterface.unlock();
        }
    }

    /**
     * Transition listener that automatically pauses the video content when the end of the transition
     * has been reached
     */
    private final MotionLayout.TransitionListener transitionListener = new MotionLayout.TransitionListener() {
        @Override
        public void onTransitionCompleted(MotionLayout motionLayout, int currentId) {
            // Pause video when the transition has finished to the end
            // We could potentially pause it earlier, like when the transition is halfway done?
            // We also can start it when we reach the start, not sure if that is good or bad
            if (currentId == R.id.end) {
                binding.post.viewUnselected();
            }
        }

        @Override
        public void onTransitionStarted(MotionLayout motionLayout, int startId, int endId) {
            // Not implemented
        }
        @Override
        public void onTransitionChange(MotionLayout motionLayout, int startId, int endId, float progress) {
            // Not implemented
        }
        @Override
        public void onTransitionTrigger(MotionLayout motionLayout, int triggerId, boolean positive, float progress) {
            // Not implemented
        }
    };
}