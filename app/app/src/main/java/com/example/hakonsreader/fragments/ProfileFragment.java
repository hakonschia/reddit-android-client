package com.example.hakonsreader.fragments;

import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.databinding.BindingAdapter;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.model.RedditUser;
import com.example.hakonsreader.databinding.FragmentProfileBinding;
import com.example.hakonsreader.recyclerviewadapters.listeners.PostScrollListener;
import com.example.hakonsreader.misc.Util;
import com.example.hakonsreader.recyclerviewadapters.PostsAdapter;
import com.example.hakonsreader.viewmodels.PostsViewModel;
import com.example.hakonsreader.viewmodels.factories.PostsFactory;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Fragment for the user profile
 */
public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";
    private static final String POST_IDS_KEY = "post_ids_profile";
    private static final String LAYOUT_STATE_KEY = "layout_state_profile";
    /**
     * The key used to save the progress of the MotionLayout
     */
    private static final String LAYOUT_ANIMATION_PROGRESS_KEY = "layout_progress";

    /**
     * The key set in the bundle with getArguments() that says the username the fragment is for
     */
    private static final String USERNAME_KEY = "username";
    /**
     * The key set in the bundle with getArguments() that says if the fragment is for the logged in user
     */
    private static final String IS_LOGGED_IN_USER_KEY = "isLoggedInUser";



    private final RedditApi redditApi = App.get().getApi();
    private FragmentProfileBinding binding;

    /**
     * The object representing the Reddit user the fragment is for
     */
    private RedditUser user;

    /**
     * Flag to check if the fragment has loaded user information
     */
    private boolean firstLoad = true;

    /**
     * Flag to set if the fragment is for the logged in user or not
     */
    private boolean isLoggedInUser = false;

    /**
     * The username of the user to retrieve information for. If this is null, the fragment will be
     * for logged in users
     */
    private String username;

    private Bundle saveState;
    private List<String> postIds;
    private PostsViewModel postsViewModel;
    private PostsAdapter adapter;
    private LinearLayoutManager layoutManager;


    /**
     * Creates a new ProfileFragment for logged in users. For fragments for users NOT the logged in user
     * use {@link ProfileFragment#newInstance(String)}
     *
     * @return A new ProfileFragment for logged in users
     */
    public static ProfileFragment newInstance() {
        Bundle args = new Bundle();
        args.putBoolean(IS_LOGGED_IN_USER_KEY, true);

        ProfileFragment fragment = new ProfileFragment();
        fragment.setArguments(args);

        return fragment;
    }

    /**
     * Create a new ProfileFragment for a user that is NOT the logged in user. For logged in users
     * user {@link ProfileFragment#newInstance()}
     *
     * @param username The username to create the fragment for. If this is equal to "me" or the username
     *                 stored in SharedPreferences, the fragment will be for the logged in user
     * @return A ProfileFragment for a user
     */
    public static ProfileFragment newInstance(String username) {
        // Hardcoding values like this is obviously bad, but this is the only case we're doing something special
        RedditUser user = App.getStoredUser();
        if (username.equalsIgnoreCase("me") || (user != null && username.equalsIgnoreCase(user.getName()))) {
            return ProfileFragment.newInstance();
        }

        Bundle args = new Bundle();
        args.putString(USERNAME_KEY, username);

        ProfileFragment fragment = new ProfileFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            username = args.getString(USERNAME_KEY);
            isLoggedInUser = args.getBoolean(IS_LOGGED_IN_USER_KEY);
        }

        // No username given, load profile for logged in user
        if (isLoggedInUser) {
            user = App.getStoredUser();
            if (user != null) {
                username = user.getName();
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.setupBinding(container);
        this.setupPostsList();
        this.setupViewModel();

        if (savedInstanceState != null) {
            binding.parentLayout.setProgress(savedInstanceState.getFloat(LAYOUT_ANIMATION_PROGRESS_KEY));
        }

        postIds = new ArrayList<>();

        // Retrieve user info if there is no user previously stored, or if it's the fragments first time
        // loading (to ensure the information is up-to-date)
        // If it's the fragments first time loading retrieve new user info to ensure it is up-to-date
        if (firstLoad) {
            // Retrieve user info and then update
            this.getUserInfo();
            firstLoad = false;
        } else {
            this.updateViews();
        }

        if (isLoggedInUser) {
            enablePrivateBrowsing(App.get().isUserLoggedInPrivatelyBrowsing());
        }

        if (saveState != null) {
            binding.parentLayout.setProgress(saveState.getFloat(LAYOUT_ANIMATION_PROGRESS_KEY));
            postIds = saveState.getStringArrayList(POST_IDS_KEY);
            postsViewModel.setPostIds(postIds);
        }

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();

        // If we have no username we can't get posts (we can't ask for posts for the logged in user without their username)
        // The posts are retrieved automatically when the user information loads
        if (adapter.getPosts().isEmpty() && postIds.isEmpty() && username != null) {
            postsViewModel.loadPosts();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        // onSaveInstanceState is called for configuration changes (such as orientation)
        // so we need to store the animation state here and in saveState (for when the fragment has
        // been replaced but not destroyed)
        outState.putFloat(LAYOUT_ANIMATION_PROGRESS_KEY, binding.parentLayout.getProgress());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (saveState == null) {
            saveState = new Bundle();
        }

        saveState.putFloat(LAYOUT_ANIMATION_PROGRESS_KEY, binding.parentLayout.getProgress());
        saveState.putParcelable(LAYOUT_STATE_KEY, layoutManager.onSaveInstanceState());
        saveState.putStringArrayList(POST_IDS_KEY, (ArrayList<String>) postsViewModel.getPostIds());

        binding = null;
    }


    /**
     * Inflates and sets up {@link ProfileFragment#binding}
     *
     * @param container The ViewGroup container for the fragment
     */
    private void setupBinding(ViewGroup container) {
        binding = FragmentProfileBinding.inflate(getLayoutInflater(), container, false);

        // We might not have a username at this point (first time loading for logged in user)
        if (username != null) {
            binding.username.setText(username);
        }

        // Kinda weird to do this here, but even if we are privately browsing and on another users profile
        // it should indicate that we're privately browsing (as with your own profile and subreddits)
        enablePrivateBrowsing(App.get().isUserLoggedInPrivatelyBrowsing());
        binding.setLoggedInUser(isLoggedInUser);
    }

    /**
     * Sets up {@link ProfileFragment#postsViewModel}
     */
    private void setupViewModel() {
        postsViewModel = new ViewModelProvider(this, new PostsFactory(
                getContext(),
                username,
                true
        )).get(PostsViewModel.class);

        postsViewModel.getPosts().observe(getViewLifecycleOwner(), posts -> {
            adapter.submitList(posts);

            if (saveState != null) {
                Parcelable state = saveState.getParcelable(LAYOUT_STATE_KEY);
                if (state != null) {
                    layoutManager.onRestoreInstanceState(saveState.getParcelable(LAYOUT_STATE_KEY));
                }
            }
        });
        postsViewModel.getError().observe(getViewLifecycleOwner(), e -> Util.handleGenericResponseErrors(requireView(), e.getError(), e.getThrowable()));
        postsViewModel.onLoadingCountChange().observe(getViewLifecycleOwner(), up -> {
            binding.loadingIcon.onCountChange(up);
        });
    }

    /**
     * Sets up {@link FragmentProfileBinding#posts}
     */
    private void setupPostsList() {
        adapter = new PostsAdapter();
        layoutManager = new LinearLayoutManager(getContext());

        binding.posts.setAdapter(adapter);
        binding.posts.setLayoutManager(layoutManager);
        binding.posts.setOnScrollChangeListener(new PostScrollListener(binding.posts, () -> postsViewModel.loadPosts()));
    }


    /**
     * Updates {@link ProfileFragment#binding} with new user information if {@link ProfileFragment#user} isn't {@code null}
     */
    private void updateViews() {
        if (user != null) {
            binding.setUser(user);
        }
    }

    /**
     * Retrieves user information about the user the fragment is for. When information is retrieved
     * posts are automatically loaded
     */
    public void getUserInfo() {
        binding.loadingIcon.onCountChange(true);
        // user(null) gets information about the logged in user, so we can use username directly
        redditApi.user(username).info(newUser -> {
            user = newUser;
            username = user.getName();

            // Store the updated user information if this profile is for the logged in user
            if (isLoggedInUser) {
                App.storeUserInfo(newUser);

                // If this is the first time the user is on their profile, the username won't be set
                // on the ViewModel, so set it
                postsViewModel.setUserOrSubredditName(newUser.getName());
            }

            // Load the posts for the user
            postsViewModel.loadPosts();

            // If the view has been destroyed by the time the response comes back, binding will be nulled
            if (binding != null) {
                this.updateViews();
                binding.loadingIcon.onCountChange(false);
            }
        }, (e, t) -> {
            // If you get to this point and the user has left the fragment it will cause a NPE
            if (binding != null) {
                Util.handleGenericResponseErrors(binding.parentLayout, e, t);
                binding.loadingIcon.onCountChange(false);
            }
        });
    }

    public void enablePrivateBrowsing(boolean enable) {
        binding.setPrivatelyBrowsing(enable);
        // Setting the conditional in xml causes an error because of color/int mismatch
        binding.profilePicture.setBorderColor(
                ContextCompat.getColor(requireContext(),
                enable ? R.color.privatelyBrowsing : R.color.opposite_background)
        );
    }


    /**
     * Binding adapter for setting the profile picture
     *
     * @param imageView The view to insert the profile picture into
     * @param url The URL for the profile picture
     */
    @BindingAdapter("profilePicture")
    public static void setProfilePicture(ImageView imageView, String url) {
        // Load the users profile picture
        Picasso.get()
                .load(url)
                .placeholder(R.drawable.ic_baseline_person_100)
                .error(R.drawable.ic_baseline_person_100)
                .into(imageView);
    }

    /**
     * Binding adapter to set the profile age text. The text is formatted as "d. MMMM y",
     * 5. September 2012"
     *
     * @param textView The TextView to set the text on
     * @param createdAt The timestamp, in seconds, the profile was created. If this is negative, nothing is done
     */
    @BindingAdapter("profileAge")
    public static void setProfileAge(TextView textView, long createdAt) {
        if (createdAt >= 0) {
            // Format date as "5. September 2012"
            SimpleDateFormat dateFormat = new SimpleDateFormat("d. MMMM y", Locale.getDefault());
            Date date = Date.from(Instant.ofEpochSecond(createdAt));

            String ageText = String.format(textView.getResources().getString(R.string.profileAge), dateFormat.format(date));
            textView.setText(ageText);
        }
    }
}
