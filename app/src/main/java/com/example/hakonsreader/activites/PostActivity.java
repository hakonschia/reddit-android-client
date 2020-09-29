package com.example.hakonsreader.activites;

import android.content.Intent;
import android.os.Bundle;
import android.transition.Transition;
import android.transition.TransitionListenerAdapter;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.hakonsreader.R;
import com.example.hakonsreader.api.enums.PostType;
import com.example.hakonsreader.api.interfaces.RedditListing;
import com.example.hakonsreader.api.model.RedditComment;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.databinding.ActivityPostBinding;
import com.example.hakonsreader.recyclerviewadapters.CommentsAdapter;
import com.example.hakonsreader.viewmodels.CommentsViewModel;
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


    private ActivityPostBinding binding;

    private CommentsViewModel commentsViewModel;
    private CommentsAdapter commentsAdapter;
    private LinearLayoutManager layoutManager;

    private RedditPost post;
    private RedditListing replyingTo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Slidr.attach(this);

        binding = ActivityPostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Gson gson = new Gson();
        post = gson.fromJson(getIntent().getExtras().getString(POST), RedditPost.class);

        this.setupCommentsList();

        binding.post.setMaxContentHeight((int)getResources().getDimension(R.dimen.postContentMaxHeight));
        binding.post.setPostData(post);

        commentsViewModel = new ViewModelProvider(this).get(CommentsViewModel.class);
        commentsViewModel.getComments().observe(this, commentsAdapter::addComments);
        commentsViewModel.onLoadingChange().observe(this, up -> {
            if (Boolean.TRUE.equals(up)) {
                binding.loadingIcon.increaseLoadCount();
            } else {
                binding.loadingIcon.decreaseLoadCount();
            }
        });

        // TODO when going into a post and going to landscape and then back the animation of going
        //  back to the subreddit goes under the screen

        // Go to first/last comment on longclicks on navigation buttons
        // Previous is upwards, next is down
        binding.goToPreviousTopLevelComment.setOnLongClickListener(this::goToFirstComment);
        binding.goToNextTopLevelComment.setOnLongClickListener(this::goToLastComment);
        
        getWindow().getSharedElementEnterTransition().addListener(new TransitionListenerAdapter() {
            @Override
            public void onTransitionEnd(Transition transition) {
                super.onTransitionEnd(transition);

                // Start videos again when the transition is finished, as playing videos during the transition
                // can make the view weird.
                // TODO the thumbnail is shown the entire time, make it so the frame the video
                //  ended at is shown instead
                if (post.getPostType() == PostType.VIDEO) {
                    binding.post.resumeVideoPost(getIntent().getExtras().getBundle("extras"));
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (commentsAdapter.getItemCount() == 0) {
            commentsViewModel.loadComments(binding.parentLayout, post);
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
            if (requestCode == REQUEST_REPLY) {
                if (data != null) {
                    RedditComment newComment = new Gson().fromJson(data.getStringExtra(LISTING), RedditComment.class);

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
    }


    /**
     * Sets up {@link ActivityPostBinding#comments}
     */
    private void setupCommentsList() {
        commentsAdapter = new CommentsAdapter(post);
        commentsAdapter.setParentLayout(binding.parentLayout);
        commentsAdapter.setOnReplyListener(this::replyTo);

        layoutManager = new LinearLayoutManager(this);

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
        this.replyTo(post);
    }
}