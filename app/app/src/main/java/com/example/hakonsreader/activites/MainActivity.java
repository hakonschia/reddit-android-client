package com.example.hakonsreader.activites;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.interfaces.OnFailure;
import com.example.hakonsreader.api.interfaces.OnResponse;
import com.example.hakonsreader.api.model.Subreddit;
import com.example.hakonsreader.constants.NetworkConstants;
import com.example.hakonsreader.constants.SharedPreferencesConstants;
import com.example.hakonsreader.databinding.ActivityMainBinding;
import com.example.hakonsreader.fragments.LogInFragment;
import com.example.hakonsreader.fragments.PostsContainerFragment;
import com.example.hakonsreader.fragments.ProfileFragment;
import com.example.hakonsreader.fragments.SelectSubredditFragment;
import com.example.hakonsreader.fragments.SettingsFragment;
import com.example.hakonsreader.fragments.SubredditFragment;
import com.example.hakonsreader.interfaces.OnSubredditSelected;
import com.example.hakonsreader.misc.SharedPreferencesManager;
import com.example.hakonsreader.misc.TokenManager;
import com.example.hakonsreader.misc.Util;
import com.google.android.material.snackbar.Snackbar;


public class MainActivity extends AppCompatActivity implements OnSubredditSelected {
    private static final String TAG = "MainActivity";

    private static final String POSTS_FRAGMENT = "postsFragment";
    private static final String ACTIVE_SUBREDDIT_FRAGMENT = "activeSubredditFragment";
    private static final String SELECT_SUBREDDIT_FRAGMENT = "selectSubredditFragment";
    private static final String PROFILE_FRAGMENT = "profileFragment";
    private static final String ACTIVE_NAV_ITEM = "activeNavItem";


    private ActivityMainBinding binding;

    // The fragments to show in the nav bar
    private PostsContainerFragment postsFragment;
    private SubredditFragment activeSubreddit;
    private SelectSubredditFragment selectSubredditFragment;
    private ProfileFragment profileFragment;
    private LogInFragment logInFragment;
    private SettingsFragment settingsFragment;

    // Interface towards the Reddit API
    private RedditApi redditApi = App.get().getApi();


    // Handler for token responses. If an access token is given user information is automatically retrieved
    private OnResponse<Void> onTokenResponse = ignored -> {
        // Re-create the start fragment as it now should load posts for the logged in user
        // TODO this is kinda bad as it gets posts and then gets posts again for the logged in user
        this.setupStartFragment();

        Snackbar.make(this.binding.parentLayout, R.string.loggedIn, Snackbar.LENGTH_SHORT).show();
    };
    private OnFailure onTokenFailure = (code, t) -> {
        Util.handleGenericResponseErrors(this.binding.parentLayout, code, t);
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Switch back to the app theme (from the launcher theme)
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // For testing purposes hardcode going into a subreddit
        Intent intent = new Intent(this, DispatcherActivity.class);
        intent.putExtra("subreddit", "test");
        intent.putExtra(DispatcherActivity.URL_KEY, "https://www.reddit.com/r/GlobalOffensive/comments/jcdyze/saw_vs_faze_clan_dreamhack_open_fall_2020_group_c/");
       // startActivity(intent);

        if (savedInstanceState != null) {
            this.restoreFragmentStates(savedInstanceState);
        } else {
            // Only setup the start fragment if we have no state to restore (as this is then a new activity)
            this.setupStartFragment();
        }

        this.setupNavBar(savedInstanceState);
        App.get().registerReceivers();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        App.get().unregisterReceivers();
    }

    /**
     * Saves the state of the fragments and active nav item
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState: ");

        // If fragments aren't null, save them
        // Save which fragment is the active one as well
        if (postsFragment != null && postsFragment.isAdded()) {
            getSupportFragmentManager().putFragment(outState, POSTS_FRAGMENT, postsFragment);
        }
        if (activeSubreddit != null && activeSubreddit.isAdded()) {
            getSupportFragmentManager().putFragment(outState, ACTIVE_SUBREDDIT_FRAGMENT, activeSubreddit);
        }
        if (selectSubredditFragment != null && selectSubredditFragment.isAdded()) {
            getSupportFragmentManager().putFragment(outState, SELECT_SUBREDDIT_FRAGMENT, selectSubredditFragment);
        }
        if (profileFragment != null && profileFragment.isAdded()) {
            getSupportFragmentManager().putFragment(outState, PROFILE_FRAGMENT, profileFragment);
        }
        // Login/settings fragments can just be recreated when needed as they don't store any specific state

        // Store state of navbar
        outState.putInt(ACTIVE_NAV_ITEM, binding.bottomNav.getSelectedItemId());
    }

    @Override
    protected void onResume() {
        super.onResume();

        Uri uri = getIntent().getData();
        if (uri == null) {
            return;
        }

        // Resumed from OAuth authorization
        if (uri.toString().startsWith(NetworkConstants.CALLBACK_URL)) {
            String state = uri.getQueryParameter("state");

            // Not a match from the state we generated, something weird is happening
            if (state == null || !state.equals(App.get().getOAuthState())) {
                Util.showErrorLoggingInSnackbar(this.binding.parentLayout);
                return;
            }

            String code = uri.getQueryParameter("code");
            if (code == null) {
                Util.showErrorLoggingInSnackbar(this.binding.parentLayout);
                return;
            }

            App.get().clearOAuthState();

            // This might be bad, but onResume is called when opening a post and going back and still
            // holds the same intent which causes this branch to execute again, causing issues
            getIntent().replaceExtras(new Bundle());
            getIntent().setAction("");
            getIntent().setData(null);
            getIntent().setFlags(0);

            redditApi.getAccessToken(code, this.onTokenResponse, this.onTokenFailure);
        }
    }

    /**
     * If back is pressed when not in the home page the home page is selected, otherwise nothing happens
     */
    @Override
    public void onBackPressed() {
        // Always go back to the home page on back presses
        if (binding.bottomNav.getSelectedItemId() != R.id.navHome) {
            binding.bottomNav.setSelectedItemId(R.id.navHome);
            // Else do something else probably
        }
    }

    /**
     * Called when a subreddit has been selected from a {@link SelectSubredditFragment} fragment
     * <p>A new instance of {@link SubredditFragment} is created and shown</p>
     *
     * @param subreddit The subreddit selected
     */
    @Override
    public void subredditSelected(Subreddit subreddit) {
        // Create new subreddit fragment and replace
        activeSubreddit = SubredditFragment.newInstance(subreddit.getName());
        replaceFragment(activeSubreddit);
    }

    /**
     * Restores the state of the fragments as saved in {@link MainActivity#onSaveInstanceState(Bundle)}
     *
     * @param restoredState The bundle with the state of the fragments
     */
    private void restoreFragmentStates(@NonNull Bundle restoredState) {
        postsFragment = (PostsContainerFragment) getSupportFragmentManager().getFragment(restoredState, POSTS_FRAGMENT);
        activeSubreddit = (SubredditFragment) getSupportFragmentManager().getFragment(restoredState, ACTIVE_SUBREDDIT_FRAGMENT);
        selectSubredditFragment = (SelectSubredditFragment) getSupportFragmentManager().getFragment(restoredState, SELECT_SUBREDDIT_FRAGMENT);
        profileFragment = (ProfileFragment) getSupportFragmentManager().getFragment(restoredState, PROFILE_FRAGMENT);
    }

    /**
     * Creates the posts fragment and replaces it in the container view
     */
    private void setupStartFragment() {
        if (postsFragment == null) {
            postsFragment = new PostsContainerFragment();
        }
        replaceFragment(postsFragment);
    }

    /**
     * Sets up the navbar to be able to switch between the fragments
     *
     * @param restoredState The restored state of the activity. If this isn't null (the activity is
     *                      restored from a previous point) the active nav bar item is set to what is
     *                      stored in the state
     */
    private void setupNavBar(Bundle restoredState) {
        binding.bottomNav.setOnNavigationItemSelectedListener(item -> {
            Fragment selected;

            switch (item.getItemId()) {
                case R.id.navHome:
                    if (postsFragment == null) {
                        postsFragment = new PostsContainerFragment();
                    }
                    selected = postsFragment;
                    break;

                case R.id.navSubreddit:
                    selected = this.onNavBarSubreddit();
                    break;

                case R.id.navProfile:
                    selected = this.onNavBarProfile();
                    break;

                case R.id.navSettings:
                    if (settingsFragment == null) {
                        settingsFragment = new SettingsFragment();
                    }
                    selected = settingsFragment;
                    break;

                default:
                    return false;
            }

            replaceFragment(selected);

            return true;
        });

        // Set listener for when an item has been clicked when already selected
        binding.bottomNav.setOnNavigationItemReselectedListener(item -> {
            Fragment selected = null;

            // When the subreddit is clicked when already in subreddit go back to the subreddit list
            if (item.getItemId() == R.id.navSubreddit) {
                activeSubreddit = null;

                if (selectSubredditFragment == null) {
                    selectSubredditFragment = new SelectSubredditFragment();
                    selectSubredditFragment.setSubredditSelected(this);
                }
                selected = selectSubredditFragment;
            }

            if (selected != null) {
                replaceFragment(selected);
            }
        });

        if (restoredState != null) {
            int active = restoredState.getInt(ACTIVE_NAV_ITEM);
            binding.bottomNav.setSelectedItemId(active);
        }
    }

    /**
     * Replaces the fragment in {@link ActivityMainBinding#fragmentContainer}
     *
     * @param fragment The new fragment to show
     */
    private void replaceFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                // Although we don't use the backstack to pop elements, it is needed to keep the state
                // of the fragments (otherwise posts are reloaded when coming back)
                // With the use of a local database I can easily restore the state without the back stack
                // Not sure whats best to use, with addToBackStack it's smoother as it doesn't have to load
                // from db etc. (it doesn't take a long time) but it probably uses more ram to hold everything in memory?
                //.addToBackStack(null)
                .commit();
    }

    /**
     * Retrieves the correct fragment for when subreddit is clicked in the nav bar
     *
     * <p>If this is the first time clicking the icon the subreddit list is shown.
     * If clicked when already in the subreddit nav bar the subreddit list is shown.
     * If clicked when not already in the subreddit, the previous subreddit fragment selected is shown</p>
     *
     * @return The correct fragment to show
     */
    private Fragment onNavBarSubreddit() {
        // No subreddit created (first time here), or we in a subreddit looking to get back
        if (activeSubreddit == null) {
            if (selectSubredditFragment == null) {
                selectSubredditFragment = new SelectSubredditFragment();
                selectSubredditFragment.setSubredditSelected(this);
            }
            return selectSubredditFragment;
        } else {
            return activeSubreddit;
        }
    }

    /**
     * Retrieves the correct fragment for profiles
     *
     * @return If a user is logged in a {@link ProfileFragment} is shown, otherwise a {@link LogInFragment}
     * is shown so the user can log in
     */
    private Fragment onNavBarProfile() {
        // If not logged in, show log in page
        if (TokenManager.getToken().getRefreshToken() == null) {
            if (logInFragment == null) {
                logInFragment = new LogInFragment();
            }

            return logInFragment;
        } else {
            if (profileFragment == null) {
                profileFragment = new ProfileFragment();
            }

            return profileFragment;
        }
    }


    /**
     * Click listener for the "Log out" button
     * <p>Revokes the access/refresh token and clears any information stored locally</p>
     *
     * @param view Ignored
     */
    public void btnLogOutOnClick(View view) {
        // Revoke token
        redditApi.revokeRefreshToken(response -> {
        }, (call, t) -> {
            // If failed because of internet connection try to revoke later
        });


        // Clear shared preferences
        this.clearUserInfo();
    }

    /**
     * Clears any information stored locally about a logged in user
     */
    private void clearUserInfo() {
        TokenManager.removeToken();

        SharedPreferencesManager.remove(SharedPreferencesConstants.USER_INFO);
    }
}