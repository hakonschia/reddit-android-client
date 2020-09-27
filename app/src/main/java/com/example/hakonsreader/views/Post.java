package com.example.hakonsreader.views;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;

import com.example.hakonsreader.R;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.databinding.PostBinding;

public class Post extends RelativeLayout {
    private static final String TAG = "Post";

    private PostBinding binding;
    private RedditPost postData;
    private boolean showContent = true;
    private Runnable onContentFinished;
    private int maxContentHeight = -1;


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
     * @return The view object of the content
     */
    public View getContent() {
        return binding.content.getChildAt(0);
    }

    /**
     * Sets the max height the content of the post can have.
     *
     * <p>When using this you might want to use {@link Post#setOnContentFinished(Runnable)} to run code
     * when the content has finished calculating its height</p>
     *
     * @param maxContentHeight The height limit
     */
    public void setMaxContentHeight(int maxContentHeight) {
        this.maxContentHeight = maxContentHeight;
    }

    /**
     * @param onContentFinished a runnable that runs when content height has been set
     */
    public void setOnContentFinished(Runnable onContentFinished) {
        this.onContentFinished = onContentFinished;
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

        View content = generatePostContent(postData, getContext(), this);
        if (content != null) {
            binding.content.addView(content);

            if (maxContentHeight != -1) {
                binding.content.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        int maxHeight = (int)getResources().getDimension(R.dimen.postContentMaxHeight);

                        int height = content.getMeasuredHeight();

                        // Content is too large, set new height
                        if (height >= maxHeight) {
                            RelativeLayout.LayoutParams params = (LayoutParams) binding.content.getLayoutParams();
                            params.height = maxHeight;

                            binding.content.setLayoutParams(params);

                            // For videos the PlayerView also has to update its height, or else it will just go below the view
                            if (content instanceof ContentVideo) {
                                ((ContentVideo)content).updateHeight(maxHeight);
                            }
                        }

                        // Remove listener to avoid infinite calls of layout changes
                        binding.content.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                        // The runnable in post is (apparently) called after the UI is drawn, so it
                        // is then safe to start the transition
                        binding.content.post(onContentFinished);
                    }
                });
            }
        } else {
            if (onContentFinished != null) {
                onContentFinished.run();
            }
        }

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) binding.content.getLayoutParams();
        // Align link post to start of parent, otherwise center
        if (content instanceof ContentLink) {
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
    private View generatePostContent(RedditPost post, Context context, RelativeLayout parent) {
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

            case LINK:
                content = new ContentLink(context, post);
                break;

            case TEXT:
                content = new ContentText(context, post);
                break;

            default:
                return null;
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
}
