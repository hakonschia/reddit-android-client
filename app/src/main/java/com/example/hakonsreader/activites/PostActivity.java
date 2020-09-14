package com.example.hakonsreader.activites;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hakonsreader.R;
import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.constants.NetworkConstants;
import com.example.hakonsreader.misc.Util;
import com.example.hakonsreader.recyclerviewadapters.CommentsAdapter;
import com.example.hakonsreader.views.FullPostBar;
import com.example.hakonsreader.views.LoadingIcon;
import com.example.hakonsreader.views.PostInfo;
import com.google.gson.Gson;
import com.r0adkll.slidr.Slidr;

/**
 * Activity to show a Reddit post with its comments
 */
public class PostActivity extends AppCompatActivity {
    private static final String TAG = "PostActivity";
    private static int MAX_CONTENT_HEIGHT = -1;

    private RedditApi redditApi = RedditApi.getInstance(NetworkConstants.USER_AGENT);

    private CoordinatorLayout parentLayout;
    private LoadingIcon loadingIcon;
    private PostInfo postInfo;
    private FrameLayout postContent;
    private FullPostBar fullPostBar;
    private RecyclerView commentsList;
    private View content;

    private CommentsAdapter commentsAdapter;
    private LinearLayoutManager layoutManager;

    private RedditPost post;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);
        Slidr.attach(this);

        // Postpone transition until the height of the content is known
        postponeEnterTransition();

        if (MAX_CONTENT_HEIGHT == -1) {
            MAX_CONTENT_HEIGHT = (int) getResources().getDimension(R.dimen.postContentMaxHeight);
        }

        this.post = new Gson().fromJson(getIntent().getExtras().getString("post"), RedditPost.class);

        this.initViews();
        this.setupCommentsList();

        this.postInfo.setPost(post);
        this.fullPostBar.setPost(post);


        content = Util.generatePostContent(this.post, this);
        if (content != null) {
            this.postContent.addView(content);
            LinearLayout.MarginLayoutParams params = (LinearLayout.MarginLayoutParams) content.getLayoutParams();

            // Convert dp to pixels
            int pixels = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    20,
                    getResources().getDisplayMetrics()
            );

            // TODO this makes transitions look weird
            //params.setMarginStart(pixels);
            //params.setMarginEnd(pixels);
            //content.requestLayout();

            // Ensure the content doesn't go over the set height limit
            this.postContent.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    int height = content.getMeasuredHeight();

                    // Content is too large, set new height
                    if (height >= MAX_CONTENT_HEIGHT) {
                        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) postContent.getLayoutParams();
                        layoutParams.height = MAX_CONTENT_HEIGHT;
                        postContent.setLayoutParams(layoutParams);
                    }

                    // Remove listener to avoid an infinite loop of layout changes
                    postContent.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    // The runnable in post is called after the UI is (apparently) drawn, so it
                    // is then safe to start the transition
                    postContent.post(() -> startPostponedEnterTransition());
                }
            });
        } else {
            startPostponedEnterTransition();
        }

        this.loadingIcon.increaseLoadCount();
        this.redditApi.getComments(post.getId(), (comments -> {
            this.commentsAdapter.addComments(comments);
            this.loadingIcon.decreaseLoadCount();
        }), ((code, t) -> {
            if (code == 503) {
                Util.showGenericServerErrorSnackbar(this.parentLayout);
            }
            this.loadingIcon.decreaseLoadCount();
            t.printStackTrace();
        }));
    }

    /**
     * Initializes all the views of the activity
     */
    private void initViews() {
        this.parentLayout = findViewById(R.id.postParentLayout);
        this.loadingIcon = findViewById(R.id.loadingIcon);
        this.postInfo = findViewById(R.id.post_info_comments);
        this.postContent = findViewById(R.id.postContent);
        this.fullPostBar = findViewById(R.id.post_full_bar_comments);
        this.commentsList = findViewById(R.id.post_comments);
    }

    /**
     * Sets up {@link PostActivity#commentsList}
     */
    private void setupCommentsList() {
        this.commentsAdapter = new CommentsAdapter(this.post);
        this.layoutManager = new LinearLayoutManager(this);

        this.commentsList.setAdapter(this.commentsAdapter);
        this.commentsList.setLayoutManager(this.layoutManager);
    }

    /**
     * Scrolls to the next top level comment
     * @param view Ignored
     */
    public void goToNextTopLevelComment(View view) {
        int currentPos = layoutManager.findFirstVisibleItemPosition();
        // Add 1 so that we can go directly from a top level to the next without scrolling
        int next = this.commentsAdapter.getNextTopLevelCommentPos(currentPos + 1);

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

        this.layoutManager.scrollToPositionWithOffset(previous, 0);
    }
}