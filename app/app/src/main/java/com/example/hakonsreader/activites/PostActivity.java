package com.example.hakonsreader.activites;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.transition.Transition;
import android.transition.TransitionListenerAdapter;
import android.view.View;

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
import com.example.hakonsreader.databinding.ActivityPostBinding;
import com.example.hakonsreader.interfaces.LockableSlidr;
import com.example.hakonsreader.misc.Util;
import com.example.hakonsreader.recyclerviewadapters.CommentsAdapter;
import com.example.hakonsreader.viewmodels.CommentsViewModel;
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
     * The percentage of the screen height that the post will at maximum take
     */
    private static final float MAX_POST_HEIGHT_PERCENTAGE = 0.65f;


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
     * The key used in intent extras for listings
     */
    public static final String LISTING_KEY = "listing";

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
        // have had a chance to load, so always assume there are comments (since there usually are)
        // TODO this has to be updated when new comments are added (if there were no comments and a comment was
        //  posted to the post, it's now obviously not empty anymore)
        binding.setNoComments(false);

        this.setupCommentsViewModel();

        // TODO When the post is opened from an intent outside the app (from intent-filter) this doesn't work
        // If we're in landscape the "height" is the width of the screen
        boolean portrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        int height = portrait ? App.get().getScreenHeight() : App.get().getScreenWidth();
        int maxHeight = (int)(height * (App.get().getMaxPostSizePercentage() / 100f));

        binding.post.setMaxHeight(maxHeight);
        binding.post.setHideScore(getIntent().getExtras().getBoolean(HIDE_SCORE_KEY));
        // Don't allow to open the post again when we are now in the post
        binding.post.setAllowPostOpen(false);

        this.loadComments();

        // TODO when going into a post and going to landscape and then back the animation of going
        //  back to the subreddit goes under the screen

        // Go to first/last comment on longclicks on navigation buttons
        // Previous is upwards, next is down
        binding.goToPreviousTopLevelComment.setOnLongClickListener(this::goToFirstComment);
        binding.goToNextTopLevelComment.setOnLongClickListener(this::goToLastComment);

        binding.commentsSwipeRefresh.setOnRefreshListener(() -> {
            binding.commentsSwipeRefresh.setRefreshing(false);
            commentsViewModel.restart();
        });
        binding.commentsSwipeRefresh.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(this, R.color.colorAccent));

        binding.parentLayout.addTransitionListener(new MotionLayout.TransitionListener() {
            @Override
            public void onTransitionCompleted(MotionLayout motionLayout, int currentId) {
                // Pause video when the transition has finished to the end
                // We could potentially pause it earlier, like when the transition is halfway done?
                // We also can start it when we reach the start, not sure if that is good or bad
                if (currentId == R.id.end) {
                    binding.post.pauseVideo();
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
        });
        // TODO when the animation is finished hiding the post videos should be paused
    }

    @Override
    protected void onPause() {
        super.onPause();

        videoPlayingWhenPaused = binding.post.isVideoPlaying();
        binding.post.pauseVideo();
    }

    @Override
    protected void onResume() {
        super.onResume();

        App.get().setActiveActivity(this);

        // Only resume if the video was actually playing when pausing
        if (videoPlayingWhenPaused) {
            binding.post.playVideo();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Ensure resources are freed when the activity exits
        binding.post.cleanUpContent();
        binding = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_REPLY && data != null) {
                RedditComment newComment = new Gson().fromJson(data.getStringExtra(LISTING_KEY), RedditComment.class);

                // Adding a top-level comment
                if (replyingTo instanceof RedditPost) {
                    commentsAdapter.addComment(newComment);
                } else {
                    // Replying to a comment
                    commentsAdapter.addComment(newComment, (RedditComment)replyingTo);
                }
            }
        }
    }


    /**
     * Sets up {@link PostActivity#commentsViewModel}
     */
    private void setupCommentsViewModel() {
        commentsViewModel = new ViewModelProvider(this).get(CommentsViewModel.class);
        commentsViewModel.onLoadingChange().observe(this, binding.loadingIcon::onCountChange);
        commentsViewModel.getPost().observe(this, newPost -> {
            boolean postPreviouslySet = post != null;
            post = newPost;

            // If we have a post already just update the info (the content gets reloaded which looks weird for videos)
            if (postPreviouslySet) {
                this.updatePostInfo();
            } else {
                this.onPostLoaded();
            }
        });
        commentsViewModel.getComments().observe(this, comments -> {
            // Check if there are any comments from before to avoid "No comments" not appearing when comments are reloaded
            if (comments.isEmpty() && commentsAdapter.getItemCount() != 0) {
                commentsAdapter.clearComments();
                return;
            }

            commentsAdapter.addComments(comments);
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

            // If we have an image ensure the image is fully loaded before we start the transition
            // This is to avoid images showing the placeholder image during the transition. Even if
            // Picasso is caching the image, it sometimes still needs to load the image from the cache
            // which looks weird
            // This won't produce a very noticeable delay, as it doesn't take a long time to load the image
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

            // Since we have the post loaded we have a transition as well
            getWindow().getSharedElementEnterTransition().addListener(new TransitionListenerAdapter() {
                @Override
                public void onTransitionEnd(Transition transition) {
                    super.onTransitionEnd(transition);

                    // Start videos again when the transition is finished, as playing videos during the transition
                    // can make the view weird.
                    // TODO the thumbnail is shown the entire time, make it so the frame the video
                    //  ended at is shown instead
                    if (post.getPostType() == PostType.VIDEO) {
                        binding.post.setExtras(getIntent().getExtras().getBundle("extras"));
                    }
                }
            });

            this.onPostLoaded();
        } else {
            // If post is started with only the ID of the post
            postId = extras.getString(POST_ID_KEY);
        }

        commentsViewModel.setPostId(postId);
        commentsViewModel.loadComments();
    }

    /**
     * Called when {@link PostActivity#post} has been set.
     * <p>Notifies the view about the new post data and calls {@link PostActivity#setupCommentsList()}</p>
     */
    private void onPostLoaded() {
        binding.setPost(post);
        binding.post.setPostData(post);
        this.setupCommentsList();
    }

    /**
     * Update the information about the post in the UI
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

        // TODO this should be some sort of button above the list
        binding.post.setOnClickListener(v -> commentsAdapter.setCommentIdChain(""));

        binding.comments.setAdapter(commentsAdapter);
        binding.comments.setLayoutManager(layoutManager);
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
        intent.putExtra(KIND_KEY, listing.getKind());
        intent.putExtra(LISTING_KEY, new Gson().toJson(listing));

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


    public interface LoadMoreComments {
        void loadMoreComments(RedditComment comment, RedditComment parent);
    }
}