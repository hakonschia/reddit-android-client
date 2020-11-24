package com.example.hakonsreader.fragments;

import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.databinding.ObservableField;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.enums.SortingMethods;
import com.example.hakonsreader.api.enums.PostTimeSort;
import com.example.hakonsreader.api.exceptions.NoSubredditInfoException;
import com.example.hakonsreader.api.exceptions.SubredditNotFoundException;
import com.example.hakonsreader.api.model.Subreddit;
import com.example.hakonsreader.api.persistence.AppDatabase;
import com.example.hakonsreader.api.responses.GenericError;
import com.example.hakonsreader.databinding.FragmentSubredditBinding;
import com.example.hakonsreader.databinding.SubredditBannedBinding;
import com.example.hakonsreader.databinding.SubredditNotFoundBinding;
import com.example.hakonsreader.databinding.SubredditPrivateBinding;
import com.example.hakonsreader.interfaces.SortableWithTime;
import com.example.hakonsreader.recyclerviewadapters.listeners.PostScrollListener;
import com.example.hakonsreader.misc.Util;
import com.example.hakonsreader.recyclerviewadapters.PostsAdapter;
import com.example.hakonsreader.viewmodels.PostsViewModel;
import com.example.hakonsreader.viewmodels.factories.PostsFactory;
import com.robinhood.ticker.TickerUtils;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment containing a subreddit
 */
public class SubredditFragment extends Fragment implements SortableWithTime {
    private static final String TAG = "SubredditFragment";

    /**
     * The key representing how many list view states have been stored
     */

    private static final String FIRST_VIEW_STATE_STORED_KEY = "first_view_state_stored";
    private static final String LAST_VIEW_STATE_STORED_KEY = "last_view_state_stored";
    private static final String VIEW_STATE_STORED_KEY = "view_state_stored";

    /**
     * The key used to store the post IDs the fragment is showing
     */
    private static final String POST_IDS_KEY = "post_ids";

    /**
     * The key used to store the state of {@link SubredditFragment#layoutManager}
     */
    private static final String LAYOUT_STATE_KEY = "layout_state";


    private final RedditApi api = App.get().getApi();
    private FragmentSubredditBinding binding;

    private AppDatabase database;
    private Bundle saveState;
    private List<String> postIds;

    /**
     * Observable that automatically updates the UI when the internal object
     * is updated with {@link ObservableField#set(Object)}
     */
    private final ObservableField<Subreddit> subreddit = new ObservableField<Subreddit>() {
        @Override
        public void set(Subreddit value) {
            super.set(value);
            // Probably not how ObservableField is supposed to be used? Works though

            updateIcon();
            binding.setSubreddit(value);
            adapter.setHideScoreTime(value.getHideScoreTime());
        }

        /**
         * Updates the UI based on {@link SubredditFragment#subreddit}
         *
         * <p>Runs on the UI thread</p>
         */
        private void updateIcon() {
            final Subreddit sub = get();

            final String iconURL = sub.getIconImage();
            final String communityURL = sub.getCommunityIcon();

            requireActivity().runOnUiThread(() -> {
                if (iconURL != null && !iconURL.isEmpty()) {
                    Picasso.get()
                            .load(iconURL)
                            .into(binding.subredditIcon);
                } else if(communityURL != null && !communityURL.isEmpty()) {
                    Picasso.get()
                            .load(communityURL)
                            .into(binding.subredditIcon);
                } else {
                    binding.subredditIcon.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_emoji_emotions_200));
                }
            });
        }
    };

    private PostsViewModel postsViewModel;
    private PostsAdapter adapter;
    private LinearLayoutManager layoutManager;


    /**
     * Creates a new instance of the fragment
     *
     * @param subreddit The subreddit to instantiate
     * @return The newly created fragment
     */
    public static SubredditFragment newInstance(String subreddit) {
        Bundle args = new Bundle();
        args.putString("subreddit", subreddit);

        SubredditFragment fragment = new SubredditFragment();
        fragment.setArguments(args);

        return fragment;
    }

    /**
     * Sets up {@link FragmentSubredditBinding#posts}
     */
    private void setupPostsList() {
        binding.posts.setAdapter(adapter);
        binding.posts.setLayoutManager(layoutManager);
        binding.posts.setOnScrollChangeListener(new PostScrollListener(binding.posts, () -> postsViewModel.loadPosts()));
    }

    /**
     * Sets up {@link SubredditFragment#postsViewModel}.
     */
    private void setupViewModel() {
        postsViewModel = new ViewModelProvider(this, new PostsFactory(
                getContext(),
                getSubredditName(),
                false
        )).get(PostsViewModel.class);

        postsViewModel.getPosts().observe(this, posts -> {
            if (posts.isEmpty()) {
                adapter.clearPosts();
                return;
            }

            int size = adapter.getItemCount();
            adapter.submitList(posts);

            // Possible state to restore
            if (saveState != null && size == 0) {
                Parcelable state = saveState.getParcelable(saveKey(LAYOUT_STATE_KEY));
                if (state != null) {
                    layoutManager.onRestoreInstanceState(state);

                    // If we're at this point we probably don't want the toolbar expanded
                    // We get here when the fragment/activity holding the fragment has been restarted
                    // so it usually looks odd if the toolbar now shows
                    binding.subredditAppBarLayout.setExpanded(false, false);

                    // Resume videos etc etc
                    this.restoreViewHolderStates();
                }
            }
        });
        postsViewModel.onLoadingCountChange().observe(this, up -> {
            binding.loadingIcon.onCountChange(up);
        });
        postsViewModel.getError().observe(this, error -> this.handleErrors(error.getError(), error.getThrowable()));
    }


    /**
     * Restores the state of the visible ViewHolders based on {@link SubredditFragment#saveState}
     */
    private void restoreViewHolderStates() {
        if (saveState != null) {
            int firstVisible = saveState.getInt(FIRST_VIEW_STATE_STORED_KEY);
            int lastVisible = saveState.getInt(LAST_VIEW_STATE_STORED_KEY);

            for (int i = firstVisible; i <= lastVisible; i++) {
                PostsAdapter.ViewHolder viewHolder = (PostsAdapter.ViewHolder)binding.posts.findViewHolderForLayoutPosition(i);

                if (viewHolder != null) {
                    // If the view has been destroyed the ViewHolders havent been created yet
                    Bundle extras = saveState.getBundle(VIEW_STATE_STORED_KEY + i);
                    if (extras != null) {
                        viewHolder.setExtras(extras);
                    }
                }
            }
        }
    }

    /**
     * Click listener for the "+ Subscribe/- Unsubscribe" button.
     *
     * <p>Sends an API request to Reddit to subscribe/unsubscribe</p>
     *
     * @param ignored Ignored
     */
    public void subscribeOnClick(View ignored) {
        // It's better if the internal values are also observable and auto update, but
        final Subreddit sub = subreddit.get();
        final boolean subscribed = sub.isSubscribed();

        api.subreddit(sub.getName()).subscribe(!subscribed, voidValue -> {
            sub.setSubscribed(!subscribed);
            subreddit.set(sub);
        }, (code, t) -> {
            t.printStackTrace();
            Util.handleGenericResponseErrors(getView(), code, t);
        });
    }

    /**
     * @return The name of the subreddit the fragment is for, or null if no subreddit is set
     */
    public String getSubredditName() {
        Subreddit s = subreddit.get();
        if (s != null) {
            return subreddit.get().getName();
        }

        return null;
    }

    /**
     * Retrieves information for a subreddit from the Reddit API
     *
     * <p>{@link SubredditFragment#subreddit} is updated with the information retrieved from the API
     * and is inserted into the local DB (if it is not a NSFW sub)</p>
     *
     * @param subredditName The name of the subreddit to get information for
     */
    private void retrieveSubredditInfo(String subredditName) {
        api.subreddit(subredditName).info(sub -> {
            new Thread(() -> {
                // Lets assume the user doesn't want to store NSFW. We could use the setting for caching
                // images/videos but it's somewhat going beyond the intent of the setting
                if (!sub.isNsfw()) {
                    database.subreddits().insert(sub);
                }
            }).start();

            subreddit.set(sub);
        }, this::handleErrors);
    }

    /**
     * Handles the errors received by the API
     *
     * @param error The error received
     * @param throwable The throwable received
     */
    private void handleErrors(GenericError error, Throwable throwable) {
        String errorReason = error.getReason();
        throwable.printStackTrace();

        // Duplication of code here but idk how to generify the bindings?
        // These should also be in the center of the bottom parent/appbar or have margin to the bottom of the appbar
        // since now it might go over the appbar
        if (GenericError.SUBREDDIT_BANNED.equals(errorReason)) {
            SubredditBannedBinding layout = SubredditBannedBinding.inflate(getLayoutInflater(), binding.parentLayout, true);
            layout.setSubreddit(getSubredditName());

            CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) layout.getRoot().getLayoutParams();
            params.gravity = Gravity.CENTER;
            layout.getRoot().requestLayout();
        } else if (GenericError.SUBREDDIT_PRIVATE.equals(errorReason)) {
            SubredditPrivateBinding layout = SubredditPrivateBinding.inflate(getLayoutInflater(), binding.parentLayout, true);
            layout.setSubreddit(getSubredditName());

            CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) layout.getRoot().getLayoutParams();
            params.gravity = Gravity.CENTER;
            layout.getRoot().requestLayout();
        } else {
            // NoSubredditInfoException is retrieved when trying to get info from front page, popular, or all
            // and we don't need to show anything of this to the user
            if (throwable instanceof NoSubredditInfoException) {
                return;
            } else if (throwable instanceof SubredditNotFoundException) {
                SubredditNotFoundBinding layout = SubredditNotFoundBinding.inflate(getLayoutInflater(), binding.parentLayout, true);
                layout.setSubreddit(getSubredditName());

                CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) layout.getRoot().getLayoutParams();
                params.gravity = Gravity.CENTER;
                layout.getRoot().requestLayout();
                return;
            }

            Util.handleGenericResponseErrors(binding.parentLayout, error, throwable);
        }
    }

    /**
     * Refreshes the posts in the fragment
     *
     * <p>To ensure that no list state is saved and restored, {@link SubredditFragment#saveState} is
     * destroyed and set to a fresh bundle</p>
     */
    private void refreshPosts() {
        // If the user had previously gone out of the fragment and gone back, refreshing would
        // restore the list state that was saved at that point, making the list scroll to that point
        saveState = new Bundle();
        postsViewModel.restart();
    }

    /**
     * Saves the state of the fragment to a bundle. Restore the state with {@link SubredditFragment#restoreState(Bundle)}
     *
     * @param saveState The bundle to store the state to
     */
    public void saveState(@NonNull Bundle saveState) {
        saveState.putStringArrayList(saveKey(POST_IDS_KEY), (ArrayList<String>) postsViewModel.getPostIds());
        saveState.putParcelable(saveKey(LAYOUT_STATE_KEY), layoutManager.onSaveInstanceState());
    }

    /**
     * Restores the state stored for when the activity holding the fragment has been recreated in a
     * way that doesn't permit the fragment to store its own state
     *
     * @param state The bundle holding the stored state
     */
    public void restoreState(@Nullable Bundle state) {
        if (state != null) {
            // Might be asking for trouble by doing overriding saveState like this? This function
            // is meant to only be called when there is no saved state by the fragment
            saveState = state;
        }
    }

    /**
     * Converts a base key into a unique key for this subreddit, so that the subreddit state can be
     * stored in a global bundle holding states for multiple subreddits
     *
     * @param baseKey The base key to use
     * @return A key unique to this subreddit
     */
    private String saveKey(String baseKey) {
        return baseKey + "_" + getSubredditName();
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = FragmentSubredditBinding.inflate(getLayoutInflater());

        adapter = new PostsAdapter();
        layoutManager = new LinearLayoutManager(getContext());
        postIds = new ArrayList<>();

        database = AppDatabase.getInstance(getContext());

        Bundle args = getArguments();
        if (args != null) {
            String subredditName = args.getString("subreddit", "");
            subreddit.set(new Subreddit(subredditName));

            binding.setStandardSub(RedditApi.STANDARD_SUBS.contains(subredditName.toLowerCase()));
            binding.subscribe.setOnClickListener(this::subscribeOnClick);

            // Standard subs won't have information, so there is no point in attempting to get it
            if (!RedditApi.STANDARD_SUBS.contains(subredditName.toLowerCase())) {
                new Thread(() -> {
                    Subreddit sub = database.subreddits().get(subredditName);
                    if (sub != null) {
                        subreddit.set(sub);
                    }
                }).start();

                this.retrieveSubredditInfo(subredditName);
            }
        }

        this.setupViewModel();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView " + getSubredditName());

        // Bind the refresh button in the toolbar
        binding.subredditRefresh.setOnClickListener(view -> this.refreshPosts());

        // Although only the numbers will actually change, we need to add alphabet characters as well
        //  for the initial animation when going from an empty string to "341 subscribers"
        binding.subredditSubscribers.setCharacterLists(TickerUtils.provideAlphabeticalList(), TickerUtils.provideAlphabeticalList());

        // Setup the RecyclerView posts list
        this.setupPostsList();

        if (saveState != null) {
            postIds = saveState.getStringArrayList(POST_IDS_KEY + "_" + getSubredditName());
            if (postIds != null) {
                postsViewModel.setPostIds(postIds);
            }
        }

        binding.postsRefreshLayout.setOnRefreshListener(() -> {
            this.refreshPosts();

            // If we want to use the "native" refresher for the refresh layout we have to somehow
            // differentiate it in the ViewModel (not important for now)
            binding.postsRefreshLayout.setRefreshing(false);
        });
        // For some reason I can't change the background color from XML, so we have to do it in code
        binding.postsRefreshLayout.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(requireContext(), R.color.colorAccent));

        return binding.getRoot();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause " + getSubredditName());

        if (saveState == null) {
            saveState = new Bundle();
        }

        // It's probably not necessary to loop through all, but ViewHolders are still active even when not visible
        // so just getting firstVisible and lastVisible probably won't be enough
        for (int i = 0; i < adapter.getItemCount(); i++) {
            PostsAdapter.ViewHolder viewHolder = (PostsAdapter.ViewHolder)binding.posts.findViewHolderForLayoutPosition(i);

            if (viewHolder != null) {
                Bundle extras = viewHolder.getExtras();
                saveState.putBundle(VIEW_STATE_STORED_KEY + i, extras);

                viewHolder.pause();
            }
        }

        int firstVisible = layoutManager.findFirstVisibleItemPosition();
        int lastVisible = layoutManager.findLastVisibleItemPosition();

        saveState.putInt(FIRST_VIEW_STATE_STORED_KEY, firstVisible);
        saveState.putInt(LAST_VIEW_STATE_STORED_KEY, lastVisible);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume " + getSubredditName());

        // If the fragment is selected without any posts load posts automatically
        if (adapter.getPosts().isEmpty() && postIds.isEmpty()) {
            postsViewModel.loadPosts();
        }

        this.restoreViewHolderStates();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy " + getSubredditName());

        // Ensure that all videos are cleaned up
        for (int i = 0; i < adapter.getItemCount(); i++) {
            PostsAdapter.ViewHolder viewHolder = (PostsAdapter.ViewHolder)binding.posts.findViewHolderForLayoutPosition(i);

            if (viewHolder != null) {
                viewHolder.destroy();
            }
        }

        binding = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView " + getSubredditName());

        // onDestroyView is called when a fragment is no longer visible. We store the list state of the fragment
        // here and when onCreateView is called again we restore the state (the fragment object itself is not
        // destroyed, so saveState will be the same)
        if (saveState == null) {
            saveState = new Bundle();
        }

        saveState.putParcelable(saveKey(LAYOUT_STATE_KEY), layoutManager.onSaveInstanceState());
        saveState.putStringArrayList(saveKey(POST_IDS_KEY), (ArrayList<String>) postsViewModel.getPostIds());
    }

    @Override
    public void newSort() {
        postsViewModel.restart(SortingMethods.NEW, null);
    }

    @Override
    public void hot() {
        postsViewModel.restart(SortingMethods.HOT, null);
    }

    @Override
    public void top(PostTimeSort timeSort) {
        postsViewModel.restart(SortingMethods.TOP, timeSort);
    }

    @Override
    public void controversial(PostTimeSort timeSort) {
        postsViewModel.restart(SortingMethods.CONTROVERSIAL, timeSort);
    }
}
