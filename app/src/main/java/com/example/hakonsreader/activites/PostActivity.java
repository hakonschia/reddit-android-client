package com.example.hakonsreader.activites;

import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
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
import com.google.gson.Gson;
import com.r0adkll.slidr.Slidr;

/**
 * Activity to show a Reddit post with its comments
 */
public class PostActivity extends AppCompatActivity {
    private static final String TAG = "PostActivity";

    /**
     * The max height the content of the post can have (the image, video etc.)
     * Set during initialization
     */
    private static int MAX_CONTENT_HEIGHT = -1;


    private RedditApi redditApi = RedditApi.getInstance(NetworkConstants.USER_AGENT);

    private ActivityPostBinding binding;

    private CommentsAdapter commentsAdapter;
    private LinearLayoutManager layoutManager;

    private RedditPost post;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.binding = ActivityPostBinding.inflate(getLayoutInflater());
        setContentView(this.binding.getRoot());

        Slidr.attach(this);

        // Postpone transition until the height of the content is known
        postponeEnterTransition();

        if (MAX_CONTENT_HEIGHT == -1) {
            MAX_CONTENT_HEIGHT = (int) getResources().getDimension(R.dimen.postContentMaxHeight);
        }

        this.post = new Gson().fromJson(getIntent().getExtras().getString("post"), RedditPost.class);

        this.setupCommentsList();

        this.binding.postInfoContainer.postInfo.setPost(post);
        this.binding.postInfoContainer.postFullBar.setPost(post);

        View postContent = Util.generatePostContent(this.post, this);
        if (postContent != null) {
            this.binding.postInfoContainer.content.addView(postContent);
            LinearLayout.MarginLayoutParams params = (LinearLayout.MarginLayoutParams) postContent.getLayoutParams();

            //params.setMarginStart(R.dimen.defaultIndent);
            //params.setMarginEnd(R.dimen.defaultIndent);
            //postContent.requestLayout();

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
                    }

                    // Remove listener to avoid an infinite loop of layout changes
                    binding.postInfoContainer.content.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    // The runnable in post is called after the UI is (apparently) drawn, so it
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

    /**
     * Sets up {@link ActivityPostBinding#comments}
     */
    private void setupCommentsList() {
        this.commentsAdapter = new CommentsAdapter(this.post);
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