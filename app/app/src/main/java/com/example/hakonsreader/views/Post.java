package com.example.hakonsreader.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.api.enums.PostType;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.databinding.PostBinding;
import com.example.hakonsreader.interfaces.OnVideoManuallyPaused;
import com.squareup.picasso.Callback;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class Post extends Content {
    private static final String TAG = "Post";

    /**
     * Flag used for when the {@link Post#maxHeight} isn't set
     */
    private static final int NO_MAX_HEIGHT = -1;

    private final PostBinding binding;
    private boolean showTextContent = true;

    /**
     * The max height of the entire post (post info + content + post bar)
     */
    private int maxHeight = NO_MAX_HEIGHT;

    private Callback imageLoadedCallback;
    @Nullable
    private OnVideoManuallyPaused onVideoManuallyPaused;

    private ViewTreeObserver.OnGlobalLayoutListener contentOnGlobalLayoutListener;

    private int postInfoHeight = 0;
    private int postBarHeight = 0;

    public Post(Context context) {
        this(context, null, 0, 0);
    }
    public Post(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }
    public Post(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }
    public Post(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        binding = PostBinding.inflate(LayoutInflater.from(context), this, true);
    }

    /**
     * Sets the callback for when a video post has been manually paused
     *
     * @param onVideoManuallyPaused The callback
     */
    public void setOnVideoPostPaused(@Nullable OnVideoManuallyPaused onVideoManuallyPaused) {
        this.onVideoManuallyPaused = onVideoManuallyPaused;
    }

    /**
     * Call this if the score should always be hidden. Must be called before {@link Post#setRedditPost(RedditPost)}
     */
    public void setHideScore(boolean hideScore) {
        binding.postFullBar.setHideScore(hideScore);
    }

    /**
     * Gets if the score is set to always be hidden
     *
     * @return True if the score is always hidden
     */
    public boolean getHideScore() {
        return binding.postFullBar.getHideScore();
    }

    /**
     * Sets the {@link Callback} to use for when the post is an image and the image has finished loading.
     * 
     * <p>This must be set before {@link Post#setRedditPost(RedditPost)}</p>
     *
     * @param imageLoadedCallback The callback for when images are finished loading
     */
    public void setImageLoadedCallback(Callback imageLoadedCallback) {
        this.imageLoadedCallback = imageLoadedCallback;
    }

    /**
     * Sets if the content should be shown or not. Default to true
     *
     * <p>This only sets the flag to show the content or not. If content shouldn't be shown this must be set
     * before {@link Post#setRedditPost(RedditPost)} as the content is generated in that call</p>
     *
     * @param showTextContent True if content should be set or not
     */
    public void setShowTextContent(boolean showTextContent) {
        this.showTextContent = showTextContent;
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
     * Sets the max height the post can have (post info + content + post bar)
     *
     * @param maxHeight The height limit
     */
    public void setMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
    }

    // Thank god this is a one person project, because I would feel really bad for anyone having
    // to debug this abomination of a code for updating the height
    /**
     * Updates the max height the post can have (post info + content + post bar)
     *
     * @param maxHeight The height limit
     */
    public void updateMaxHeight(int maxHeight) {
        // Ensure that the layout listener is removed. If this is still present, it will cause an infinite
        // callback loop since the height will most likely be changed inside the listener
        binding.content.getViewTreeObserver().removeOnGlobalLayoutListener(contentOnGlobalLayoutListener);
        this.maxHeight = maxHeight;

        View content = binding.content.getChildAt(0);
        if (content == null) {
            return;
        }

        // Get height of the content and the total height of the entire post so we can resize the content correctly
        int contentHeight = binding.content.getMeasuredHeight();
        int totalHeight = binding.postsParentLayout.getMeasuredHeight();

        // TODO this doesn't really work, since if the content is actually smaller the it will still resize
        //  to the full height
        int newContentHeight = maxHeight - totalHeight + contentHeight;

        setContentHeight(newContentHeight);
    }

    /**
     * Sets the height of only the content view of the post. The value will be animated for a duration
     * of 250 milliseconds
     *
     * <p>This does not check the max height set</p>
     *
     * @param height The height to set
     */
    public void setContentHeight(int height) {
        // TODO links dont correctly regain their previous view. The image isn't correctly put back and
        //  the link text doesn't change position (might have to do something inside the class itself)
        if (height < 0) {
            return;
        }

        View content = binding.content.getChildAt(0);

        ValueAnimator anim = ValueAnimator.ofInt(content.getMeasuredHeight(), height);
        anim.addUpdateListener(valueAnimator -> {
            int val = (Integer) valueAnimator.getAnimatedValue();
            ViewGroup.LayoutParams layoutParams = content.getLayoutParams();
            layoutParams.height = val;
            content.setLayoutParams(layoutParams);
        });
        anim.setDuration(250);
        anim.start();
    }

    /**
     * @return The height of only the content in the post
     */
    public Integer getContentHeight() {
        View view = binding.content.getChildAt(0);
        if (view != null) {
            return view.getMeasuredHeight();
        } else {
            return null;
        }
    }


    /**
     * Updates the view
     */
    @Override
    protected void updateView() {
        // Ensure view is fresh if used in a RecyclerView
        this.cleanUpContent();

        binding.postInfo.setPost(redditPost);
        this.addContent();
        binding.postFullBar.setPost(redditPost);
    }

    /**
     * Updates the information in the post without re-creating the content
     *
     * @param post The post with updated information
     */
    public void updatePostInfo(RedditPost post) {
        redditPost = post;
        binding.postInfo.setPost(post);
        binding.postFullBar.setPost(post);
    }


    /**
     * Enables or disables the animation for any {@link com.robinhood.ticker.TickerView} found
     * in this view
     *
     * @param enable True to enable
     */
    public void enableTickerAnimation(boolean enable) {
        binding.postFullBar.enableTickerAnimation(enable);
    }

    /**
     * Adds the post content
     *
     * <p>If {@link Post#showTextContent} is {@code false} and the post type is {@link PostType#TEXT} nothing happens</p>
     *
     * <p>The height of the post is resized to match {@link Post#maxHeight}, if needed</p>
     */
    private void addContent() {
        if (!showTextContent && redditPost.getPostType() == PostType.TEXT) {
            return;
        }

        View content = generatePostContent(redditPost, getContext());
        if (content != null) {
            binding.content.addView(content);

            if (maxHeight != NO_MAX_HEIGHT) {
                contentOnGlobalLayoutListener = () -> {
                    // Get height of the content and the total height of the entire post so we can resize the content correctly
                    int height = content.getMeasuredHeight();
                    int totalHeight = binding.postsParentLayout.getMeasuredHeight();

                    // TODO this sometimes fires multiple times and updates the height multiple times, causing
                    //  the view to jump a tiny bit after it has done the initial transition

                    // Entire post is too large, set new content height
                    if (totalHeight > maxHeight) {
                        LayoutParams params = (LayoutParams) content.getLayoutParams();
                        params.height = maxHeight - totalHeight + height;
                        content.setLayoutParams(params);
                    }
                };
                binding.content.getViewTreeObserver().addOnGlobalLayoutListener(contentOnGlobalLayoutListener);
            }
        }

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) binding.content.getLayoutParams();
        // Align link and text posts to start of parent, otherwise center
        if (content instanceof ContentLink || content instanceof ContentText || content instanceof ContentPostRemoved) {
            params.removeRule(RelativeLayout.CENTER_IN_PARENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_START);
        } else {
            params.removeRule(RelativeLayout.ALIGN_PARENT_START);
            params.addRule(RelativeLayout.CENTER_IN_PARENT);
        }

        binding.content.setLayoutParams(params);
    }


    /**
     * Generates the content view for a post
     *
     * @param post The post to generate for
     * @return A view with the content of the post
     */
    private Content generatePostContent(RedditPost post, Context context) {
        // If the post has been removed don't try to render the content as it can cause a crash later
        // Just show that the post has been removed
        // For instance, if the post is not uploaded to reddit the URL will still link to something (like an imgur gif)
        // TODO maybe the only posts actually removed completely so they're not able ot be watched are videos? Even text/images uploaded
        //  to reddit directly are still there
        if (post.getRemovedByCategory() != null) {
            ContentPostRemoved c = new ContentPostRemoved(context);
            c.setRedditPost(post);
            return c;
        }

        Content content = null;

        switch (post.getPostType()) {
            case IMAGE:
                ContentImage image = new ContentImage(context);
                image.setImageLoadedCallback(imageLoadedCallback);
                image.setRedditPost(redditPost);
                content = image;
                break;

            case VIDEO:
            case GIF:
            // Links such as youtube, gfycat etc are rich video posts
            case RICH_VIDEO:
                // Ensure we know how to handle a video, otherwise it might not load
                // Show as link content if we can't show it as a video

                if (ContentVideo.Companion.isRedditPostVideoPlayable(post)) {
                    content = new ContentVideo(context);
                    ((ContentVideo)content).setOnVideoManuallyPaused(onVideoManuallyPaused);
                } else if (App.Companion.get().openYouTubeVideosInApp()
                        && (redditPost.getDomain().equals("youtu.be") || redditPost.getDomain().equals("youtube.com"))) {
                    content = new ContentYoutubeVideo(context);
                } else {
                    content = new ContentLink(context);
                }

                content.setRedditPost(post);

                break;

            case CROSSPOST:
                // Generate the content the parent post would generate and show that
                RedditPost parent = post.getCrossposts().get(0);
                if (parent.getPostType() == PostType.TEXT) {
                    if (showTextContent) {
                        content = generatePostContent(parent, context);
                    }
                } else {
                    content = generatePostContent(parent, context);
                }
                break;

            case LINK:
                content = new ContentLink(context);
                content.setRedditPost(post);
                break;

            case TEXT:
                // If there is no text on the post there is no point in creating a view for it
                String selfText = post.getSelftext();
                if (selfText != null && !selfText.isEmpty()) {
                    content = new ContentText(context);
                    content.setRedditPost(post);
                }
                break;

            case GALLERY:
                content = new ContentGallery(context);
                content.setRedditPost(post);
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
    @NotNull
    @Override
    public Bundle getExtras() {
        Content c = (Content) binding.content.getChildAt(0);
        return  c != null ? c.getExtras() : new Bundle();
    }

    /**
     * Sets a bundle of information to restore the state of the post
     *
     * <p>Currently only restores state for video posts</p>
     *
     * @param data The data to use for restoring the state
     */
    @Override
    public void setExtras(@NonNull Bundle data) {
        Content c = (Content) binding.content.getChildAt(0);

        if (c != null) {
            c.setExtras(data);
        }
    }

    /**
     * Retrieve the list of views mapped to the corresponding transition name, to be used in a
     * shared element transition
     *
     * @return A list of pairs with a View mapped to a transition name
     */
    @Override
    public List<Pair<View, String>> getTransitionViews() {
        Context context = getContext();

        List<Pair<View, String>> pairs = new ArrayList<>();
        pairs.add(Pair.create(binding.postInfo, context.getString(R.string.transition_post_info)));
        pairs.add(Pair.create(binding.postFullBar, context.getString(R.string.transition_post_full_bar)));

        Content content = (Content) binding.content.getChildAt(0);
        if (content != null) {
            // Not all subclasses add custom transition views, so if no custom view is found use the
            // view itself as the transition view
            List<Pair<View, String>> contentTransitionViews = content.getTransitionViews();
            if (contentTransitionViews.isEmpty()) {
                pairs.add(Pair.create(content, context.getString(R.string.transition_post_content)));
            } else {
                pairs.addAll(contentTransitionViews);
            }
        }

        return pairs;
    }

    @Override
    public void viewSelected() {
        Content content = (Content) binding.content.getChildAt(0);
        if (content != null) {
            content.viewSelected();
        }
    }

    @Override
    public void viewUnselected() {
        Content content = (Content) binding.content.getChildAt(0);
        if (content != null) {
            content.viewUnselected();
        }
    }

    /**
     * Gets the position of the contents Y position on the screen
     * <p>Crossposts is taken into account and will return the position of the actual content
     * inside the crosspost</p>
     *
     * @return The Y position of the content
     */
    public int getContentY() {
        // Get the views position on the screen
        int[] location = new int[2];
        binding.content.getLocationOnScreen(location);
        return location[1];
    }

    /**
     * Gets the bottom position of the contents Y position on the screen
     * <p>Crossposts is taken into account and will return the position of the actual content
     * inside the crosspost</p>
     *
     * @return The Y position of the bottom of the content
     */
    public int getContentBottomY() {
        return getContentY() + binding.content.getMeasuredHeight();
    }
}
