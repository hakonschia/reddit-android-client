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
import com.example.hakonsreader.api.model.Image;
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


    private final PostBinding binding;
    private RedditPost postData;
    private boolean showContent = true;
    /**
     * If set to true the post can be opened in a new activity
     */
    private boolean allowPostOpen = true;
    /**
     * If set to true the post has been opened in a new activity
     */
    private boolean postOpened = false;
    private int maxContentHeight = NO_MAX_HEIGHT;


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
     * Sets the post data to use for this view
     *
     * <p>The view is updated automatically. If this is used in a RecyclerView the view is also
     * recycled</p>
     *
     * @param post The post to use
     */
    public void setPostData(RedditPost post) {
        postData = post;

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

    public void updatePostInfo(RedditPost post) {
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
        // If the post has been removed don't try to render the content as it can cause a crash later
        // Just show that the post has been removed
        if (post.getRemovedByCategory() != null) {
            Log.d(TAG, "generatePostContent: post has been removed!");
            ContentPostRemoved c = new ContentPostRemoved(context);
            c.setPost(post);
            return c;
        }

        View content;

        switch (post.getPostType()) {
            case IMAGE:
                ContentImage image = new ContentImage(context);
                image.setPost(postData);
                content = image;
                break;

            case VIDEO:
            case GIF:
            // Links such as youtube, gfycat etc are rich video posts
            // TODO For rich video, create a function to check if it's on a domain that allows for direct videos
            //  otherwise just provide it as link content
            case RICH_VIDEO:
                ContentVideo video = new ContentVideo(context);
                video.setPost(post);
                content = video;
                break;

            case CROSSPOST:
                RedditPost parent = postData.getCrossposts().get(0);

                // If we are in a post only care about the actual post content, as it's not enough space
                // to show the entire parent post info
                if (getContext() instanceof PostActivity) {
                    content = generatePostContent(parent, context);
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
                ContentLink contentLink = new ContentLink(context);
                contentLink.setPost(post);
                content = contentLink;
                break;

            case TEXT:
                ContentText contentText = new ContentText(context);
                contentText.setPost(post);
                content = contentText;
                break;

            case GALLERY:
                ContentGallery contentGallery = new ContentGallery(context);
                contentGallery.setPost(postData);
                content = contentGallery;
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
        // If the post has already been opened don't open it again
        // This is to avoid if a user clicks on the post twice fast before it opens it will open twice
        // which a) looks weird as two posts will be open twice, and b) when going back to the posts the post
        // view will be missing
        if (allowPostOpen && !postOpened) {
            Intent intent = new Intent(getContext(), PostActivity.class);
            intent.putExtra(PostActivity.POST_KEY, new Gson().toJson(postData));

            Bundle extras = getExtras();
            intent.putExtra("extras", extras);

            this.pauseVideo();

            Activity activity = (Activity)getContext();

            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, getTransitionViews());
            activity.startActivity(intent, options.toBundle());

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
     * Copies the link to {@link Post#postData} to the clipboard and shows a toast that it has been copied
     */
    private void copyLinkToClipBoard() {
        Activity activity = (Activity)getContext();

        ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("reddit post", postData.getUrl());
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
     * Sets a bundle of information to restore the state of the post
     *
     * <p>Currently only restores state for video posts</p>
     *
     * @param data The data to use for restoring the state
     */
    public void setExtras(Bundle data) {
        View c = binding.content.getChildAt(0);

        if (c instanceof ContentVideo) {
            ContentVideo video = (ContentVideo)c;
            video.setExtras(data); }
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
     * @return Returns if the video content is currently playing. Always returns false if the content
     * isn't video content
     */
    public boolean isVideoPlaying() {
        View content = binding.content.getChildAt(0);
        if (content instanceof ContentVideo) {
            return ((ContentVideo)content).isPlaying();
        } else if (content instanceof Post) {
            // If the content is a crosspost pause the video in the crosspost post
            return ((Post)content).isVideoPlaying();
        }

        // I suppose if the post isn't a video it's also not playing?
        return false;
    }

    /**
     * Pauses the video content
     *
     * <p>If the content isn't a video nothing is done</p>
     */
    public void pauseVideo() {
        View content = binding.content.getChildAt(0);
        if (content instanceof ContentVideo) {
            ((ContentVideo)content).setPlayback(false);
        } else if (content instanceof Post) {
            // If the content is a crosspost pause the video in the crosspost post
            ((Post)content).pauseVideo();
        }
    }

    /**
     * Plays the video content
     *
     * <p>If the content isn't a video nothing is done</p>
     */
    public void playVideo() {
        View content = binding.content.getChildAt(0);
        if (content instanceof ContentVideo) {
            ((ContentVideo)content).setPlayback(true);
        } else if (content instanceof Post) {
            // If the content is a crosspost play the video in the crosspost post
            ((Post)content).playVideo();
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
