package com.example.hakonsreader.recyclerviewadapters.listeners;

import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hakonsreader.App;
import com.example.hakonsreader.recyclerviewadapters.PostsAdapter;
import com.example.hakonsreader.views.Content;

import org.jetbrains.annotations.Nullable;

/**
 * Scroll listener for RecyclerView of Reddit posts. This will automatically "select" a post
 * when it is the main content on the screen. The behaviour of being "selected" and "unselected"
 * is defined in subclasses' implementation of {@link Content#viewSelected()} and {@link Content#viewUnselected()}
 */
public class PostScrollListener implements View.OnScrollChangeListener {
    private static final String TAG = "PostScrollListener";

    private static final int SCREEN_HEIGHT = App.Companion.get().getScreenHeight();


    private final LinearLayoutManager layoutManager;
    private final PostsAdapter adapter;

    /**
     * The list of the posts
     */
    private final RecyclerView posts;

    /**
     * The runnable to run when the end of list has been reached
     */
    private final Runnable onEndOfList;

    /**
     * The amount of items in the list at the last attempt at loading more posts
     */
    private int lastLoadAttemptCount;

    /**
     * The amount of posts left in the list before calling {@link PostScrollListener#onEndOfList}
     */
    private int numRemainingPostsBeforeRun = 10;

    /**
     * The ID of the post to ignore when calling {@link PostsAdapter.ViewHolder#onSelected()}
     */
    private String postToIgnore = "";


    /**
     * Scroll listener that can be used for a {@link RecyclerView} with a {@link PostsAdapter} attached to it.
     * The listener will:
     * <ol>
     *     <li>
     *         Automatically run a {@link Runnable} when the end of the list has (almost) been reached.
     *         The end of the list is calculated based on the adapters item count and the value set
     *         with {@link PostScrollListener#setNumRemainingPostsBeforeRun(int)} (default to 5)
     *     </li>
     *     <li>
     *         Automatically calls {@link PostsAdapter.ViewHolder#onSelected()} and
     *         {@link PostsAdapter.ViewHolder#onUnselected()} based on if a post has been "selected" (ie. is the main
     *         item on the screen) or "unselected" (ie. no longer the main item)
     *     </li>
     *</ol>
     *
     * @param posts The {@link RecyclerView} the listener is attached to. The RecyclerView must have
     *              an adapter of type {@link PostsAdapter} and layout manager of type {@link LinearLayoutManager}
     *              set before this is called
     * @param onEndOfList The runnable to run when the end of the list has (almost) been reached
     * @throws IllegalStateException If the {@code RecyclerView} does not have an adapter or layout manager
     * attached to it
     */
    public PostScrollListener(RecyclerView posts, Runnable onEndOfList) {
        this.posts = posts;
        this.onEndOfList = onEndOfList;

        this.adapter = (PostsAdapter) posts.getAdapter();
        this.layoutManager = (LinearLayoutManager) posts.getLayoutManager();

        if (this.adapter == null) {
            throw new IllegalStateException("The RecyclerView must have an adapter attached to it");
        }
        if (this.layoutManager == null) {
            throw new IllegalStateException("The RecyclerView must have a layout manager attached to it");
        }
    }

    /**
     * Sets the amount of posts left in the list before the runnable set in the constructor runs.
     * This has a default value of 5 posts.
     *
     * @param numRemainingPostsBeforeRun The amount of posts left
     */
    public void setNumRemainingPostsBeforeRun(int numRemainingPostsBeforeRun) {
        this.numRemainingPostsBeforeRun = numRemainingPostsBeforeRun;
    }

    /**
     * Sets the ID of a post to ignore when calling {@link PostsAdapter.ViewHolder#onSelected()}, so that
     * scrolling past the post will be ignored. This will be reset when {@link PostsAdapter.ViewHolder#onUnselected()}
     * would be called, so that scrolling up again later will still produce the expected behaviour
     *
     * @param postToIgnore The ID of the post to ignore
     */
    public void setPostToIgnore(@Nullable String postToIgnore) {
        if (postToIgnore == null) {
            postToIgnore = "";
        } else {
            this.postToIgnore = postToIgnore;
        }
    }

    /**
     * Resets the onEndOfList runnable to be called again. This should be used to re-call the runnable
     * passed to the constructor
     */
    public void resetOnEndOfList() {
        lastLoadAttemptCount = -1;
    }

    @Override
    public void onScrollChange(View v, int scrollX, int scrollY, int oldX, int oldY) {
        // Find the positions of first and last visible items to find all visible items
        int posFirstItem = layoutManager.findFirstVisibleItemPosition();
        int posLastItem = layoutManager.findLastVisibleItemPosition();

        int listSize = adapter.getItemCount();

        // Load more posts before we reach the end to create an "infinite" list
        // Only load posts if there hasn't been an attempt at loading more posts
        if (posLastItem + numRemainingPostsBeforeRun > listSize && lastLoadAttemptCount < listSize) {
            lastLoadAttemptCount = adapter.getItemCount();

            onEndOfList.run();
        }

        this.checkSelectedPost(posFirstItem, posLastItem, oldY > 0);
    }

    /**
     * Goes through the selected range of posts and calls {@link PostsAdapter.ViewHolder#onSelected()}
     * and {@link PostsAdapter.ViewHolder#onUnselected()} based on if a post has been "selected" (ie. is the main
     * item on the screen) or "unselected" (ie. no longer the main item)
     *
     * @param startPost The index of the post to start at (from {@link PostsAdapter#getPosts()} or {@link PostScrollListener#layoutManager}
     * @param endPost The index of the post to end at (from {@link PostsAdapter#getPosts()} or {@link PostScrollListener#layoutManager}
     * @param scrollingUp Whether or not we are scrolling up or down in the list
     */
    private void checkSelectedPost(int startPost, int endPost, boolean scrollingUp) {
        // The behavior is:
        // When scrolling UP:
        // 1. postToIgnore is reset when any view is UNSELECTED
        // 1. If the bottom of the content is under the screen, the view is UN SELECTED

        // When scrolling DOWN:
        // 1. If the top of the content is above the screen, the view is UNSELECTED
        // 2. If the bottom of the content is under the screen, the view is UNSELECTED
        // 3. If the top of the content is above 3/4th of the screen, the view is SELECTED


        // Go through all visible views and select/un select the view holder based on where on the screen they are
        for (int i = startPost; i <= endPost; i++) {
            PostsAdapter.ViewHolder viewHolder = (PostsAdapter.ViewHolder)posts.findViewHolderForLayoutPosition(i);

            // If we have no view holder there isn't anything we can do later
            if (viewHolder == null) {
                continue;
            }

            // (0, 0) is top left
            int y = viewHolder.getContentY();
            int viewBottom = viewHolder.getContentBottomY();


            // If the view is above the screen (< 0) it is "unselected"
            // If the bottom of the view is above the screen height it is "unselected"
            // If the view is below 35% of the screen height it is "selected" (kind of backwards since 0 is at the top)

            if (y < 0 || viewBottom > SCREEN_HEIGHT) {
                viewHolder.onUnselected();

                // We only want to reset the post to ignore on scrolling up, otherwise we have to ensure
                // that the view being unselected is the post that is being ignored
                if (scrollingUp) {
                    postToIgnore = "";
                }
            } else if (y < SCREEN_HEIGHT * 0.35f) {
                if (!viewHolder.getPostId().equals(postToIgnore)) {
                    viewHolder.onSelected();
                }
            }
        }
    }
}
