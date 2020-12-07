package com.example.hakonsreader.activites;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.constants.NetworkConstants;
import com.example.hakonsreader.databinding.ActivityMainBinding;
import com.example.hakonsreader.fragments.LogInFragment;
import com.example.hakonsreader.fragments.PostsContainerFragment;
import com.example.hakonsreader.fragments.ProfileFragment;
import com.example.hakonsreader.fragments.SelectSubredditFragmentK;
import com.example.hakonsreader.fragments.SettingsFragment;
import com.example.hakonsreader.fragments.SubredditFragment;
import com.example.hakonsreader.interfaces.OnSubredditSelected;
import com.example.hakonsreader.misc.Util;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import static com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT;


public class MainActivity extends AppCompatActivity implements OnSubredditSelected {
    private static final String TAG = "MainActivity";

    private static final String POSTS_FRAGMENT = "postsFragment";
    private static final String ACTIVE_SUBREDDIT_FRAGMENT = "activeSubredditFragment";
    private static final String SELECT_SUBREDDIT_FRAGMENT = "selectSubredditFragment";
    private static final String PROFILE_FRAGMENT = "profileFragment";
    private static final String ACTIVE_NAV_ITEM = "activeNavItem";

    /**
     * The key used to store the name of the subreddit represented in {@link MainActivity#activeSubreddit}
     *
     * <p>When this key holds a value it does not necessarily mean the subreddit was active (shown on screen)
     * at the time the instance was saved, but means that when clicking on the subreddit navbar, this should
     * be shown again instead of the list of subreddits</p>
     */
    private static final String ACTIVE_SUBREDDIT_NAME = "active_subreddit_name";


    private ActivityMainBinding binding;

    private Bundle savedState;

    // The fragments to show in the nav bar
    private PostsContainerFragment postsFragment;
    private SubredditFragment activeSubreddit;
    private SelectSubredditFragmentK selectSubredditFragment;
    private ProfileFragment profileFragment;
    private LogInFragment logInFragment;
    private SettingsFragment settingsFragment;
    private final BottomNavigationViewListener navigationViewListener = new BottomNavigationViewListener();

    // Interface towards the Reddit API
    private final RedditApi redditApi = App.get().getApi();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Switch back to the app theme (from the launcher theme)
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // For testing purposes hardcode going into a subreddit/post etc.
        Intent intent = new Intent(this, DispatcherActivity.class);
        // TODO there are some issues with links, if a markdown link has superscript inside of it, markwon doesnt recognize it (also spaces in links causes issues)
        //  https://www.reddit.com/r/SpeedyDrawings/comments/jgg06k/this_gave_me_a_mild_heart_attack/
        intent.putExtra(DispatcherActivity.URL_KEY, "https://www.reddit.com/r/GlobalOffensive/comments/jznuc5/just_finished_the_m4a4_cybershark_a_new_skin_from/");
       // startActivity(intent);

        if (savedInstanceState != null) {
            savedState = savedInstanceState;
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
        binding = null;
    }

    /**
     * Saves the state of the fragments and active nav item
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState");

        // If fragments aren't null, save them
        // Save which fragment is the active one as well
        if (postsFragment != null) {
            postsFragment.saveState(outState);

            if (postsFragment.isAdded()) {
                getSupportFragmentManager().putFragment(outState, POSTS_FRAGMENT, postsFragment);
            }
        }
        if (activeSubreddit != null) {
            // If there is an active subreddit it won't be null, store the state of it even if it isn't
            // currently added (shown on screen)
            outState.putString(ACTIVE_SUBREDDIT_NAME, activeSubreddit.getSubredditName());
            activeSubreddit.saveState(outState);

            if (activeSubreddit.isAdded()) {
                getSupportFragmentManager().putFragment(outState, ACTIVE_SUBREDDIT_FRAGMENT, activeSubreddit);
            }
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
        App.get().setActiveActivity(this);

        Uri uri = getIntent().getData();
        if (uri == null) {
            return;
        }

        // Resumed from OAuth authorization
        if (uri.toString().startsWith(NetworkConstants.CALLBACK_URL)) {
            String state = uri.getQueryParameter("state");

            // Not a match from the state we generated, something weird is happening
            if (state == null || !state.equals(App.get().getOAuthState())) {
                Util.showErrorLoggingInSnackbar(binding.parentLayout);
                return;
            }

            String code = uri.getQueryParameter("code");
            if (code == null) {
                Util.showErrorLoggingInSnackbar(binding.parentLayout);
                return;
            }

            App.get().clearOAuthState();

            // This might be bad, but onResume is called when opening a post and going back and still
            // holds the same intent which causes this branch to execute again, causing issues
            getIntent().replaceExtras(new Bundle());
            getIntent().setAction("");
            getIntent().setData(null);
            getIntent().setFlags(0);

            redditApi.getAccessToken(code, ignored -> {
                // Get information about the user. If this fails it doesn't really matter, as it isn't
                // strictly needed at this point
                redditApi.user().info(App::storeUserInfo, (e, t) -> {
                    // Ignored
                });

                // Re-create the start fragment as it now should load posts for the logged in user
                // TODO this is kinda bad as it gets posts and then gets posts again for the logged in user
                postsFragment = null;
                this.setupStartFragment();

                Snackbar.make(binding.parentLayout, R.string.loggedIn, LENGTH_SHORT).show();
            }, (e, t) -> Util.handleGenericResponseErrors(binding.parentLayout, e, t));
        }
    }

    /**
     * If back is pressed when not in the home page the home page is selected, otherwise nothing happens
     */
    @Override
    public void onBackPressed() {
        // TODO we should actually go back to the previous navbar, not always just go back like this
        if (binding.bottomNav.getSelectedItemId() == R.id.navSubreddit && activeSubreddit != null) {
            activeSubreddit = null;

            // If the fragment has been killed by the OS make a new one (after a while it might be killed)
            if (selectSubredditFragment == null) {
                selectSubredditFragment = new SelectSubredditFragmentK();
            }
            // Since we are in a way going back in the same navbar item, use the close transition
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, selectSubredditFragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                    .commit();
        } else {
            binding.bottomNav.setSelectedItemId(R.id.navHome);
        }
    }

    /**
     * Called when a subreddit has been selected from a {@link SelectSubredditFragmentK} fragment
     * <p>A new instance of {@link SubredditFragment} is created and shown</p>
     *
     * @param subredditName The subreddit selected
     */
    @Override
    public void subredditSelected(String subredditName) {
        // Create new subreddit fragment and replace
        activeSubreddit = SubredditFragment.Companion.newInstance(subredditName);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, activeSubreddit)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();
    }

    /**
     * Updates the language
     *
     * @param language The language to switch to. If this is {@code null} the language found in
     *                 SharedPreferences will be used
     */
    public void updateLanguage(String language) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        if (language == null) {
            language = settings.getString(getString(R.string.prefs_key_language), getString(R.string.prefs_default_language));
        }
        // TODO this
        //updateLocale(new Locale(language));
    }

    /**
     * Restores the state of the fragments as saved in {@link MainActivity#onSaveInstanceState(Bundle)}
     *
     * @param restoredState The bundle with the state of the fragments
     */
    private void restoreFragmentStates(@NonNull Bundle restoredState) {
        postsFragment = (PostsContainerFragment) getSupportFragmentManager().getFragment(restoredState, POSTS_FRAGMENT);
        activeSubreddit = (SubredditFragment) getSupportFragmentManager().getFragment(restoredState, ACTIVE_SUBREDDIT_FRAGMENT);
        selectSubredditFragment = (SelectSubredditFragmentK) getSupportFragmentManager().getFragment(restoredState, SELECT_SUBREDDIT_FRAGMENT);
        profileFragment = (ProfileFragment) getSupportFragmentManager().getFragment(restoredState, PROFILE_FRAGMENT);

        if (postsFragment == null) {
            postsFragment = new PostsContainerFragment();
            postsFragment.restoreState(restoredState);
        }

        // Active subreddit not restored directly, check if it should be restored manually
        if (activeSubreddit == null) {
            String activeSubredditName = restoredState.getString(ACTIVE_SUBREDDIT_NAME);
            if (activeSubredditName != null) {
                // The state of the fragment itself is restored when it is accessed again
                activeSubreddit = SubredditFragment.Companion.newInstance(activeSubredditName);
            }
        }

        // Restore the listener as it won't be set automatically
        if (selectSubredditFragment != null) {
            selectSubredditFragment.setSubredditSelected(this);
        }
    }

    /**
     * Creates the posts fragment and replaces it in the container view
     */
    private void setupStartFragment() {
        if (postsFragment == null) {
            postsFragment = new PostsContainerFragment();
        }

        // Use an open transition since we're calling this when the app has been started
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, postsFragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();
    }

    /**
     * Sets up the navbar to be able to switch between the fragments
     *
     * @param restoredState The restored state of the activity. If this isn't null (the activity is
     *                      restored from a previous point) the active nav bar item is set to what is
     *                      stored in the state
     */
    private void setupNavBar(Bundle restoredState) {
        binding.bottomNav.setOnNavigationItemSelectedListener(navigationViewListener);

        // Set listener for when an item has been clicked when already selected
        binding.bottomNav.setOnNavigationItemReselectedListener(item -> {
            // When the subreddit is clicked when already in subreddit go back to the subreddit list
            if (item.getItemId() == R.id.navSubreddit) {
                activeSubreddit = null;

                if (selectSubredditFragment == null) {
                    selectSubredditFragment = new SelectSubredditFragmentK();
                    selectSubredditFragment.setSubredditSelected(this);
                }

                // Since we are in a way going back in the same navbar item, use the close transition
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, selectSubredditFragment)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                        .commit();
            }
        });

        if (restoredState != null) {
            int active = restoredState.getInt(ACTIVE_NAV_ITEM);

            // When we're restoring a state we don't want to play an animation, as the user has manually
            // selected a change
            navigationViewListener.disableAnimationForNextChange();
            binding.bottomNav.setSelectedItemId(active);
        }
    }

    /**
     * Selects the profile nav bar
     */
    public void selectProfileNavBar() {
        binding.bottomNav.setSelectedItemId(R.id.navProfile);
    }


    /**
     * Custom BottomNavigationView item selection listener
     */
    private class BottomNavigationViewListener implements BottomNavigationView.OnNavigationItemSelectedListener {

        // TODO this class should probably be decoupled from MainActivity (need to pass the functions to call or something)

        private boolean playAnimationForNextChange = true;

        /**
         * The current index the nav bar is in (from left to right). Used to determine what transition
         * to play when changing the nav bar item
         */
        private int navBarPos;


        /**
         * Disables the animation for the next nav bar change. Note this only lasts for one change
         */
        public void disableAnimationForNextChange() {
            this.playAnimationForNextChange = false;
        }

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            Fragment selected;

            int previousPos = navBarPos;

            switch (item.getItemId()) {
                case R.id.navHome:
                    navBarPos = 1;
                    if (postsFragment == null) {
                        postsFragment = new PostsContainerFragment();
                    }
                    selected = postsFragment;
                    break;

                case R.id.navSubreddit:
                    navBarPos = 2;
                    selected = onNavBarSubreddit();
                    break;

                case R.id.navProfile:
                    navBarPos = 3;
                    selected = onNavBarProfile();
                    break;

                case R.id.navSettings:
                    navBarPos = 4;
                    if (settingsFragment == null) {
                        settingsFragment = new SettingsFragment();
                    }
                    selected = settingsFragment;
                    break;

                default:
                    return false;
            }

            // Example: previous = 2, current = 3. We are going right
            boolean goingRight = previousPos < navBarPos;

            if (!playAnimationForNextChange){
                replaceNavBarFragmentNoAnimation(selected);
                playAnimationForNextChange = true;
            } else {
                replaceNavBarFragment(selected, goingRight);
            }

            return true;
        }

        /**
         * Replaces the fragment in {@link ActivityMainBinding#fragmentContainer} without any animation. This should
         * be used in accordance with the nav bar.
         *
         * @param fragment The new fragment to show
         */
        private void replaceNavBarFragmentNoAnimation(Fragment fragment) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            transaction
                    .replace(R.id.fragmentContainer, fragment)
                    // Although we don't use the backstack to pop elements, it is needed to keep the state
                    // of the fragments (otherwise posts are reloaded when coming back)
                    // With the use of a local database I can easily restore the state without the back stack
                    // Not sure whats best to use, with addToBackStack it's smoother as it doesn't have to load
                    // from db etc. (it doesn't take a long time) but it probably uses more ram to hold everything in memory?
                    //.addToBackStack(null)
                    .commit();
        }


        /**
         * Replaces the fragment in {@link ActivityMainBinding#fragmentContainer}. This should
         * be used in accordance with the nav bar
         *
         * @param fragment The new fragment to show
         * @param goingRight True if going from right in the nav bar items, this changes the way
         *                   the animation slides so it makes sense based on the nav bar
         */
        private void replaceNavBarFragment(Fragment fragment, boolean goingRight) {
            getSupportFragmentManager().beginTransaction()
                    // TODO if there is an ongoing transition and the user selects another nav bar item, the app crashes (need to somehow cancel the ongoing transition or something)
                    .setCustomAnimations(goingRight ? R.anim.slide_in_right : R.anim.slide_in_left, goingRight ? R.anim.slide_out_left : R.anim.slide_out_right)
                    .replace(R.id.fragmentContainer, fragment)
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
                    selectSubredditFragment = new SelectSubredditFragmentK();
                    selectSubredditFragment.setSubredditSelected(MainActivity.this);
                }
                return selectSubredditFragment;
            } else {
                // There is an active subreddit selected, show that instead of the list to return to the subreddit
                activeSubreddit.restoreState(savedState);
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
            // If logged in, show profile, otherwise show log in page
            if (App.get().isUserLoggedIn()) {
                if (profileFragment == null) {
                    profileFragment = ProfileFragment.Companion.newInstance();
                }

                return profileFragment;
            } else {
                if (logInFragment == null) {
                    logInFragment = new LogInFragment();
                }

                return logInFragment;
            }
        }
    }
}