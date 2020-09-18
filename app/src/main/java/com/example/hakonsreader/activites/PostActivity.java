package com.example.hakonsreader.activites;

import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.hakonsreader.R;
import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.constants.NetworkConstants;
import com.example.hakonsreader.databinding.ActivityPostBinding;
import com.example.hakonsreader.misc.Util;
import com.example.hakonsreader.recyclerviewadapters.CommentsAdapter;
import com.example.hakonsreader.views.PostContentLink;
import com.example.hakonsreader.views.PostContentVideo;
import com.google.gson.Gson;
import com.r0adkll.slidr.Slidr;

/**
 * Activity to show a Reddit post with its comments
 */
public class PostActivity extends AppCompatActivity {
    private static final String TAG = "PostActivity";

    /**
     * The max height the content of the post can have (the image, video etc.)
     *
     * <p>Set during initialization</p>
     */
    private static int MAX_CONTENT_HEIGHT = -1;


    private RedditApi redditApi = RedditApi.getInstance(NetworkConstants.USER_AGENT);

    private ActivityPostBinding binding;

    private CommentsAdapter commentsAdapter;
    private LinearLayoutManager layoutManager;

    private RedditPost post;
    private View postContent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.binding = ActivityPostBinding.inflate(getLayoutInflater());
        setContentView(this.binding.getRoot());
        Slidr.attach(this);

        // Extra information that might hold information about the state of the post
        Bundle extras = getIntent().getExtras().getBundle("extras");

        // Postpone transition until the height of the content is known
        postponeEnterTransition();

        if (MAX_CONTENT_HEIGHT == -1) {
            MAX_CONTENT_HEIGHT = (int) getResources().getDimension(R.dimen.postContentMaxHeight);
        }

        this.post = new Gson().fromJson(getIntent().getExtras().getString("post"), RedditPost.class);

        this.setupCommentsList();

        this.binding.postInfoContainer.postInfo.setPost(post);
        this.binding.postInfoContainer.postFullBar.setPost(post);

        postContent = Util.generatePostContent(this.post, this);
        if (postContent != null) {
            this.binding.postInfoContainer.content.addView(postContent);

            // Align link post to start of parent
            if (postContent instanceof PostContentLink) {
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
                    if (height >= MAX_CONTENT_HEIGHT) {
                        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) binding.postInfoContainer.content.getLayoutParams();
                        layoutParams.height = MAX_CONTENT_HEIGHT;
                        binding.postInfoContainer.content.setLayoutParams(layoutParams);

                        // TODO find a better way to scale the video as it doesn't smoothly transition the video
                        if (postContent instanceof PostContentVideo) {
                            ((PostContentVideo)postContent).updateHeight(MAX_CONTENT_HEIGHT);
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

        this.binding.loadingIcon.increaseLoadCount();
        this.redditApi.getComments(post.getId(), (comments -> {
            this.commentsAdapter.addComments(comments);
            this.binding.loadingIcon.decreaseLoadCount();
        }), ((code, t) -> {
            if (code == 503) {
                Util.showGenericServerErrorSnackbar(this.binding.parentLayout);
            }
            this.binding.loadingIcon.decreaseLoadCount();
            t.printStackTrace();
        }));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Ensure resources are freed when the activity exits
        Util.cleanupPostContent(postContent);
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
}