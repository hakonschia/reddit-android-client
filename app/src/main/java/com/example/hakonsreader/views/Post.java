package com.example.hakonsreader.views;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;

import com.example.hakonsreader.R;
import com.example.hakonsreader.activites.PostActivity;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.databinding.PostBinding;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;

public class Post extends RelativeLayout {
    private static final String TAG = "Post";

    /**
     * Flag used for when the {@link Post#maxContentHeight} isn't set
     */
    private static final int NO_MAX_HEIGHT = -1;


    private PostBinding binding;
    private RedditPost postData;
    private boolean showContent = true;
    private int maxContentHeight = NO_MAX_HEIGHT;


    public Post(Context context) {
        super(context);
        binding = PostBinding.inflate(LayoutInflater.from(context), this, true);
    }
    public Post(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        binding = PostBinding.inflate(LayoutInflater.from(context), this, true);
    }
    public Post(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        binding = PostBinding.inflate(LayoutInflater.from(context), this, true);
    }
    public Post(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        binding = PostBinding.inflate(LayoutInflater.from(context), this, true);
    }


    /**
     * Sets the post data to use for this view
     *
     * <p>The view is updated automatically. If this is used in a RecyclerView the view is also
     * recycled</p>
     *
     * @param post The post to use
     */
    public void setPostData(RedditPost post) {
        this.postData = post;

        super.setOnClickListener(v -> this.openPost());
        super.setOnLongClickListener(v -> {
            this.copyLinkToClipBoard();
            return true;
        });

        this.updateView();
    }

    /**
     * Sets if the content should be shown or not. Default to true
     *
     * <p>This only sets the flag to show the content or not. If content shouldn't be shown this must be set
     * before {@link Post#setPostData(RedditPost)} as the content is generated in that call</p>
     *
     * @param showContent True if content should be set or not
     */
    public void setShowContent(boolean showContent) {
        this.showContent = showContent;
    }

    /**
     * Retrieves the layout holding the content
     *
     * @return The parent layout of the content. The content is found as the first element inside this layout
     */
    public FrameLayout getContentLayout() {
        return binding.content;
    }

    /**
     * Sets the max height the content of the post can have.
     *
     * @param maxContentHeight The height limit
     */
    public void setMaxContentHeight(int maxContentHeight) {
        this.maxContentHeight = maxContentHeight;
    }

    /**
     * Updates the view
     */
    private void updateView() {
        // Ensure view is fresh if used in a RecyclerView
        this.cleanUpContent();

        binding.postInfo.setPost(postData);
        this.addContent();
        binding.postFullBar.setPost(postData);
    }

    /**
     * Adds the post content
     *
     * <p>If {@link Post#showContent} is {@code false}, nothing happens</p>
     */
    private void addContent() {
        if (!showContent) {
            return;
        }

        View content = generatePostContent(postData, getContext());
        if (content != null) {
            binding.content.addView(content);

            if (maxContentHeight != NO_MAX_HEIGHT) {
                binding.content.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        int height = content.getMeasuredHeight();

                        // Content is too large, set new height
                        if (height >= maxContentHeight) {
                            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) content.getLayoutParams();
                            params.height = maxContentHeight;
                            content.setLayoutParams(params);
                        }

                        // TODO if video post maybe resume video after this is done as animation might look better

                        // Remove listener to avoid infinite calls of layout changes
                        binding.content.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });
            }
        }

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) binding.content.getLayoutParams();
        // Align link and text posts to start of parent, otherwise center
        if (content instanceof ContentLink || content instanceof ContentText) {
            params.removeRule(RelativeLayout.CENTER_IN_PARENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_START);
        } else {
            params.removeRule(RelativeLayout.ALIGN_PARENT_START);
            params.addRule(RelativeLayout.CENTER_IN_PARENT);
        }

        binding.content.setLayoutParams(params);
    }


    /**
     * Generates content view for a post
     *
     * @param post The post to generate for
     * @return A view with the content of the post
     */
    private View generatePostContent(RedditPost post, Context context) {
        View content;

        switch (post.getPostType()) {
            case IMAGE:
                content = new ContentImage(context, post);
                break;

            case VIDEO:
                content = new ContentVideo(context, post);
                break;

            case RICH_VIDEO:
                // Links such as youtube, gfycat etc are rich video posts
                content = null;
                break;

            case CROSSPOST:
                RedditPost parent = postData.getCrossposts().get(0);

                // If we are in a post only care about the actual post content, as it's not enough space
                // to show the entire parent post info
                if (getContext() instanceof PostActivity) {
                    content = new ContentVideo(context, parent);
                } else {
                    // Otherwise the content is the entire parents info
                    Post c = new Post(context);
                    c.setPostData(parent);

                    // Add a border around to show where the crosspost post is and where the actual post it
                    c.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.border));
                    content = c;
                }
                break;

            case LINK:
                content = new ContentLink(context, post);
                break;

            case TEXT:
                content = new ContentText(context, post);
                break;

            default:
                return null;
        }

        if (content != null) {
            content.setTransitionName(context.getString(R.string.transition_post_content));
        }
        return content;
    }

    /**
     * Opens {@link Post#postData} in a new activity
     */
    private void openPost() {
        Intent intent = new Intent(getContext(), PostActivity.class);
        intent.putExtra(PostActivity.POST, new Gson().toJson(postData));

        Bundle extras = getExtras();
        intent.putExtra("extras", extras);

        pauseVideo();

        Activity activity = (Activity)getContext();

        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, getTransitionViews());
        activity.startActivity(intent, options.toBundle());
    }

    /**
     * Copies the link to {@link Post#postData} to the clipboard and shows a toast that it has been copied
     */
    private void copyLinkToClipBoard() {
        Activity activity = (Activity)getContext();

        ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("reddit post", postData.getPermalink());
        clipboard.setPrimaryClip(clip);

        Toast.makeText(activity, R.string.linkCopied, Toast.LENGTH_SHORT).show();

        // DEBUG
        Log.d(TAG, "copyLinkToClipboard: " + new GsonBuilder().setPrettyPrinting().create().toJson(postData));
    }

    /**
     * Releases any relevant resources and removes the content view
     *
     * <p>If relevant to the type of post, various resoruces (such as video players) are released
     * when this is called</p>
     */
    public void cleanUpContent() {
        // Free up any resources that might not be garbage collected automatically
        View v = binding.content.getChildAt(0);

        // Release the exo player from video posts
        if (v instanceof ContentVideo) {
            ((ContentVideo)v).release();
        }

        binding.content.removeAllViewsInLayout();
        // Make sure the view size resets
        binding.content.forceLayout();
    }

    /**
     * Retrieve a bundle of information that can be useful for saving the state of the post
     *
     * <p>Currently only saves state for video posts</p>
     *
     * @return A bundle that might include state variables
     */
    public Bundle getExtras() {
        Bundle extras = new Bundle();

        View c = binding.content.getChildAt(0);

        if (c instanceof ContentVideo) {
            ContentVideo video = (ContentVideo)c;
            extras = video.getExtras();
        }

        return extras;
    }

    /**
     * Resumes state of a video according to how it was when it was clicked
     *
     * @param data The data to use for restoring the state
     */
    public void resumeVideoPost(Bundle data) {
        ContentVideo video = (ContentVideo)binding.content.getChildAt(0);
        video.setExtras(data);
    }


    /**
     * Retrieve the list of views mapped to the corresponding transition name
     *
     * @return A list of pairs with a View mapped to a transition name
     */
    public Pair<View, String>[] getTransitionViews() {
        Context context = getContext();

        List<Pair<View, String>> pairs = new ArrayList<>();
        pairs.add(Pair.create(binding.postInfo, context.getString(R.string.transition_post_info)));
        pairs.add(Pair.create(binding.postFullBar, context.getString(R.string.transition_post_full_bar)));

        View content = binding.content.getChildAt(0);
        if (content != null) {
            pairs.add(Pair.create(content, context.getString(R.string.transition_post_content)));
        }

        return pairs.toArray(new Pair[0]);
    }

    /**
     * Formats the post as a mod post
     */
    public void asMod() {
        binding.postInfo.asMod();
    }

    /**
     * Resets formatting
     */
    public void reset() {
        binding.postInfo.reset();
    }

    /**
     * Pauses the video content
     */
    public void pauseVideo() {
        View video = binding.content.getChildAt(0);
        if (video instanceof ContentVideo) {
            ((ContentVideo)video).setPlayback(false);
        }
    }

    /**
     * Plays the video content
     */
    public void playVideo() {
        View video = binding.content.getChildAt(0);
        if (video instanceof ContentVideo) {
            ((ContentVideo)video).setPlayback(true);
        }
    }
}
