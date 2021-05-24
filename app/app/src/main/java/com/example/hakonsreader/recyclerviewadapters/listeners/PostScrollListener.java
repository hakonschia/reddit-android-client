package com.example.hakonsreader.recyclerviewadapters.listeners;

import android.content.res.Resources;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hakonsreader.recyclerviewadapters.PostsAdapter;
import com.example.hakonsreader.views.Content;

import org.jetbrains.annotations.Nullable;

/**
 * Scroll listener for RecyclerView of Reddit posts. This will automatically "select" a post
 * when it is the main content on the screen. The behaviour of being "selected" and "unselected"
 * is defined in subclasses' implementation of {@link Content#viewSelected()} and {@link Content#viewUnselected()}
 *
 * This listener can currently only be attached to a {@link RecyclerView} with a {@link LinearLayoutManager}
 * and {@link PostsAdapter} attached to it
 *
 * This listener implements {@link LifecycleObserver} and will listen to {@link Lifecycle.Event#ON_RESUME} and
 * {@link Lifecycle.Event#ON_PAUSE} which will ensure the listener never listens to anything in a paused state
 */
public class PostScrollListener extends RecyclerView.OnScrollListener implements LifecycleObserver {
    private static final String TAG = "PostScrollListener";

    private final int SCREEN_HEIGHT = Resources.getSystem().getDisplayMetrics().heightPixels;


    /**
     * The ID of the post to ignore when calling {@link PostsAdapter.ViewHolder#onSelected()}
     */
    private String postToIgnore = "";

    /**
     * If true the listener is in a paused state and should not listen to scroll changes
     */
    private boolean paused = false;

    /**
     * Sets the ID of a post to ignore when calling {@link PostsAdapter.ViewHolder#onSelected()}, so that
     * scrolling past the post will be ignored. This will be reset when {@link PostsAdapter.ViewHolder#onUnselected()}
     * would be called, so that scrolling up again later will still produce the expected behaviour
     *
     * @param postToIgnore The ID of the post to ignore
     */
    public void setPostToIgnore(@Nullable String postToIgnore) {
        if (postToIgnore == null) {
            this.postToIgnore = "";
        } else {
            this.postToIgnore = postToIgnore;
        }
    }

    /**
     * @return The ID of the post currently being ignored by the listener
     */
    public String getPostToIgnore() {
        return postToIgnore;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    private void paused() {
        paused = true;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private void resumed() {
        paused = false;
    }

    @Override
    public void onScrolled(@NonNull RecyclerView posts, int dx, int dy) {
        if (paused) {
            return;
        }

        // Currently this listener only supports a LinearLayoutManager. This will cause issues later
        // if we want to use a different one for future layouts, but currently it will work fine
        LinearLayoutManager layoutManager = (LinearLayoutManager) posts.getLayoutManager();

        // Find the positions of first and last visible items to find all visible items
        int posFirstItem = layoutManager.findFirstVisibleItemPosition();
        int posLastItem = layoutManager.findLastVisibleItemPosition();

        this.checkSelectedPost(posts, posFirstItem, posLastItem, dy < 0);
    }

    /**
     * Goes through the selected range of posts and calls {@link PostsAdapter.ViewHolder#onSelected()}
     * and {@link PostsAdapter.ViewHolder#onUnselected()} based on if a post has been "selected" (ie. is the main
     * item on the screen) or "unselected" (ie. no longer the main item)
     *
     * @param startPost The index of the post to start at
     * @param endPost The index of the post to end at
     * @param scrollingUp Whether or not we are scrolling up or down in the list
     */
    private void checkSelectedPost(RecyclerView posts, int startPost, int endPost, boolean scrollingUp) {
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
