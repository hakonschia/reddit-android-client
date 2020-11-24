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
import com.example.hakonsreader.api.enums.PostType;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.databinding.PostBinding;
import com.google.gson.Gson;
import com.squareup.picasso.Callback;

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
     * If set to true the post can be opened in a new activity
     */
    private boolean allowPostOpen = true;
    /**
     * If set to true the post has been opened in a new activity
     */
    private boolean postOpened = false;
    private int maxHeight = NO_MAX_HEIGHT;

    private Callback imageLoadedCallback;


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
     * Sets if the post should be allowed to be opened
     *
     * @param allowPostOpen True if clicking on the post should open it in a new {@link PostActivity}
     *                      Default is true
     */
    public void setAllowPostOpen(boolean allowPostOpen) {
        this.allowPostOpen = allowPostOpen;
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
     * Sets the max height the post can have
     *
     * @param maxHeight The height limit
     */
    public void setMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
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

        setOnClickListener(v -> openPost());
        setOnLongClickListener(v -> copyLinkToClipboard());
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
                binding.content.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                    // Get height of the content and the total height of the entire post so we can resize the content correctly
                    int height = content.getMeasuredHeight();
                    int totalHeight = binding.postsParentLayout.getMeasuredHeight();

                    // When loading from intent filter the total height is smaller than what it really is. hmm

                    // Entire post is too large, set new content height
                    if (totalHeight > maxHeight) {
                        LayoutParams params = (LayoutParams) content.getLayoutParams();
                        params.height = maxHeight - totalHeight + height;
                        content.setLayoutParams(params);
                    }

                    // TODO if video post maybe resume video after this is done as animation might look better
                });
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
                if (ContentVideo.KNOWN_VIDEO_DOMAINS.contains(post.getDomain().toLowerCase())) {
                    content = new ContentVideo(context);
                } else {
                    content = new ContentLink(context);
                }
                content.setRedditPost(post);

                break;

            case CROSSPOST:
                RedditPost parent = post.getCrossposts().get(0);

                // If we are in a post only care about the actual post content, as it's not enough space
                // to show the entire parent post info
                if (getContext() instanceof PostActivity) {
                    content = generatePostContent(parent, context);
                } else {
                    // Otherwise the content is the entire parents info
                    Post c = new Post(context);

                    // Propagate any information about the parent post to the crosspost, or else
                    // it will act as a full post ignoring values it should have
                    c.setAllowPostOpen(allowPostOpen);
                    c.setMaxHeight(maxHeight);
                    c.setImageLoadedCallback(imageLoadedCallback);
                    c.setShowTextContent(showTextContent);
                    c.setHideScore(binding.postFullBar.getHideScore());
                    c.setRedditPost(parent);

                    // Add a border around to show where the crosspost post is and where the actual post it
                    c.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.border_crosspost));
                    content = c;
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
     * Opens {@link Post#redditPost} in a new activity
     */
    private void openPost() {
        // If the post has already been opened don't open it again
        // This is to avoid if a user clicks on the post twice fast before it opens it will open twice
        // which a) looks weird as two posts will be open twice, and b) when going back to the posts the post
        // view will be missing
        if (allowPostOpen && !postOpened) {
            Intent intent = new Intent(getContext(), PostActivity.class);
            intent.putExtra(PostActivity.POST_KEY, new Gson().toJson(redditPost));

            Bundle extras = getExtras();
            intent.putExtra(Content.EXTRAS, extras);
            intent.putExtra(PostActivity.HIDE_SCORE_KEY, binding.postFullBar.getHideScore());

            // Only really applicable for videos, as they should be paused
            this.viewUnselected();

            Activity activity = (Activity)getContext();

            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, getTransitionViews());
            activity.startActivityForResult(intent,2, options.toBundle());

            // Set it back to un-opened after a short amount of time so the user can open the post again after exiting
            // The sleep time just has to be long enough to avoid double clicks, the sleep time is fairly arbitrary
            postOpened = true;
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                postOpened = false;
            }).start();
        }
    }

    /**
     * Copies the link of the post to the clipboard
     *
     * @return True (ie. event handled in a long click)
     */
    public boolean copyLinkToClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("reddit post", getRedditPost().getUrl());
        clipboard.setPrimaryClip(clip);

        Toast.makeText(getContext(), R.string.linkCopied, Toast.LENGTH_SHORT).show();
        return true;
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
    @Override
    public Bundle getExtras() {
        Content c = (Content) binding.content.getChildAt(0);
        return c != null ? c.getExtras() : new Bundle();
    }

    /**
     * Sets a bundle of information to restore the state of the post
     *
     * <p>Currently only restores state for video posts</p>
     *
     * @param data The data to use for restoring the state
     */
    @Override
    public void setExtras(Bundle data) {
        Content c = (Content) binding.content.getChildAt(0);

        if (c != null) {
            c.setExtras(data);
        }
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
