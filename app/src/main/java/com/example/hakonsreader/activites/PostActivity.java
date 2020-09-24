package com.example.hakonsreader.activites;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.interfaces.RedditListing;
import com.example.hakonsreader.api.model.RedditComment;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.databinding.ActivityPostBinding;
import com.example.hakonsreader.misc.Util;
import com.example.hakonsreader.recyclerviewadapters.CommentsAdapter;
import com.example.hakonsreader.views.PostContentLink;
import com.example.hakonsreader.views.PostContentText;
import com.example.hakonsreader.views.PostContentVideo;
import com.google.gson.Gson;
import com.r0adkll.slidr.Slidr;

/**
 * Activity to show a Reddit post with its comments
 */
public class PostActivity extends AppCompatActivity {
    private static final String TAG = "PostActivity";

    /**
     * The key used for sending the post data to this activity
     */
    public static final String POST = "post";

    /**
     * They key used to tell what kind of listing something is
     */
    public static final String KIND = "kind";

    /**
     * The key used in intent extras for listings
     */
    public static final String LISTING = "listing";

    /**
     * Request code for opening a reply activity
     */
    public static final int REQUEST_REPLY = 1;


    /**
     * The max height the content of the post can have (the image, video etc.)
     *
     * <p>Set during initialization</p>
     */
    private static int maxContentHeight = -1;


    private RedditApi redditApi = App.getApi();

    private ActivityPostBinding binding;

    private CommentsAdapter commentsAdapter;
    private LinearLayoutManager layoutManager;

    private RedditPost post;
    private RedditListing replyingTo;
    private View postContent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.binding = ActivityPostBinding.inflate(getLayoutInflater());
        setContentView(this.binding.getRoot());
        Slidr.attach(this);

        // Postpone transition until the height of the content is known
        postponeEnterTransition();

        if (maxContentHeight == -1) {
            maxContentHeight = (int) getResources().getDimension(R.dimen.postContentMaxHeight);
        }

        this.post = new Gson().fromJson(getIntent().getExtras().getString(POST), RedditPost.class);

        this.setupCommentsList();
        this.getComments();

        this.binding.postInfoContainer.postInfo.setPost(post);
        this.binding.postInfoContainer.postFullBar.setPost(post);

        this.addPostContent();

        this.commentsAdapter.setOnReplyListener(this::replyTo);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_REPLY) {
                if (data != null) {
                    RedditComment newComment = new Gson().fromJson(data.getStringExtra(LISTING), RedditComment.class);

                    // Adding a top-level comment
                    if (this.replyingTo instanceof RedditPost) {
                        this.commentsAdapter.addComment(newComment);
                    } else {
                        // Replying to a comment
                        this.commentsAdapter.addComment(newComment, (RedditComment)this.replyingTo);
                    }
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Ensure resources are freed when the activity exits
        Util.cleanupPostContent(postContent);
    }


    /**
     * Adds the content to the post
     *
     * <p>When the content is added the enter transition is called</p>
     */
    private void addPostContent() {
        // Extra information that might hold information about the state of the post
        Bundle extras = getIntent().getExtras().getBundle("extras");

        postContent = Util.generatePostContent(this.post, this);
        if (postContent != null) {
            this.binding.postInfoContainer.content.addView(postContent);

            // Align link post to start of parent
            if (postContent instanceof PostContentLink || postContent instanceof PostContentText) {
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) this.binding.postInfoContainer.content.getLayoutParams();
                params.addRule(RelativeLayout.ALIGN_PARENT_START);

                this.binding.postInfoContainer.content.setLayoutParams(params);
            } else if (postContent instanceof PostContentVideo) {
                this.resumeVideoPost(extras);
            }


            // Ensure the content doesn't go over the set height limit
            this.binding.postInfoContainer.content.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    int height = postContent.getMeasuredHeight();

                    // Content is too large, set new height
                    if (height >= maxContentHeight) {
                        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) binding.postInfoContainer.content.getLayoutParams();
                        layoutParams.height = maxContentHeight;
                        binding.postInfoContainer.content.setLayoutParams(layoutParams);

                        // TODO find a better way to scale the video as it doesn't smoothly transition the video
                        if (postContent instanceof PostContentVideo) {
                            ((PostContentVideo)postContent).updateHeight(maxContentHeight);
                        }
                    }

                    // Remove listener to avoid infinite calls of layout changes
                    binding.postInfoContainer.content.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    // The runnable in post is (apparently) called after the UI is drawn, so it
                    // is then safe to start the transition
                    binding.postInfoContainer.content.post(() -> startPostponedEnterTransition());
                }
            });
        } else {
            startPostponedEnterTransition();
        }
    }

    /**
     * Retrieves the comments for the post and adds them to the adapter
     */
    private void getComments() {
        this.binding.loadingIcon.increaseLoadCount();
        this.redditApi.getComments(post.getID(), (comments -> {
            this.commentsAdapter.addComments(comments);
            this.binding.loadingIcon.decreaseLoadCount();
        }), ((code, t) -> {
            if (code == 503) {
                Util.showGenericServerErrorSnackbar(this.binding.parentLayout);
            }
            this.binding.loadingIcon.decreaseLoadCount();
        }));
    }

    /**
     * Resumes state of a video according to how it was when it was clicked
     *
     * @param data The data to use for restoring the state
     */
    private void resumeVideoPost(Bundle data) {
        PostContentVideo video = (PostContentVideo)postContent;

        long timestamp = data.getLong(PostContentVideo.EXTRA_TIMESTAMP);
        boolean isPlaying = data.getBoolean(PostContentVideo.EXTRA_IS_PLAYING);
        boolean showController = data.getBoolean(PostContentVideo.EXTRA_SHOW_CONTROLS);

        video.setPosition(timestamp);
        video.setPlayback(isPlaying);
        video.setControllerVisible(showController);
    }

    /**
     * Sets up {@link ActivityPostBinding#comments}
     */
    private void setupCommentsList() {
        this.commentsAdapter = new CommentsAdapter(this.post);
        this.commentsAdapter.setParentLayout(this.binding.parentLayout);
        this.layoutManager = new LinearLayoutManager(this);

        this.binding.comments.setAdapter(this.commentsAdapter);
        this.binding.comments.setLayoutManager(this.layoutManager);
    }

    /**
     * Scrolls to the next top level comment
     * @param view Ignored
     */
    public void goToNextTopLevelComment(View view) {
        int currentPos = layoutManager.findFirstVisibleItemPosition();
        // Add 1 so that we can go directly from a top level to the next without scrolling
        int next = this.commentsAdapter.getNextTopLevelCommentPos(currentPos + 1);

        // Stop the current scroll (done manually by the user) to avoid scrolling past the comment navigated to
        this.binding.comments.stopScroll();

        // Scroll to the position, with 0 pixels offset from the top
        // TODO smooth scroll
        this.layoutManager.scrollToPositionWithOffset(next, 0);
    }

    /**
     * Scrolls to the previous top level comment
     * @param view Ignored
     */
    public void goToPreviousTopLevelComment(View view) {
        int currentPos = layoutManager.findFirstVisibleItemPosition();
        // Subtract 1 so that we can go directly from a top level to the previous without scrolling
        int previous = this.commentsAdapter.getPreviousTopLevelCommentPos(currentPos - 1);

        // Stop the current scroll (done manually by the user) to avoid scrolling past the comment navigated to
        this.binding.comments.stopScroll();

        this.layoutManager.scrollToPositionWithOffset(previous, 0);
    }

    /**
     * Replies to a comment or post
     *
     * @param listing The listing to reply to
     */
    private void replyTo(RedditListing listing) {
        this.replyingTo = listing;

        // Open activity or fragment or something to allow reply
        Intent intent = new Intent(this, ReplyActivity.class);
        intent.putExtra(KIND, listing.getKind());
        intent.putExtra(LISTING, new Gson().toJson(listing));

        startActivityForResult(intent, REQUEST_REPLY);
        overridePendingTransition(R.anim.slide_up, R.anim.slide_down);
    }

    /**
     * Click listener for the post reply button
     * @param view Ignored
     */
    public void replyToPost(View view) {
        Log.d(TAG, "replyToPost: Replying to " + post.getTitle());
        this.replyTo(this.post);
    }
}