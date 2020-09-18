package com.example.hakonsreader.activites;

import android.net.Uri;
import android.os.Bundle;
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
import com.example.hakonsreader.api.model.AccessToken;
import com.example.hakonsreader.constants.NetworkConstants;
import com.example.hakonsreader.constants.SharedPreferencesConstants;
import com.example.hakonsreader.databinding.ActivityMainBinding;
import com.example.hakonsreader.fragments.LogInFragment;
import com.example.hakonsreader.fragments.PostsContainerFragment;
import com.example.hakonsreader.fragments.ProfileFragment;
import com.example.hakonsreader.fragments.SettingsFragment;
import com.example.hakonsreader.fragments.SubredditFragment;
import com.example.hakonsreader.interfaces.ItemLoadingListener;
import com.example.hakonsreader.misc.SharedPreferencesManager;
import com.example.hakonsreader.misc.TokenManager;
import com.example.hakonsreader.misc.Util;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity implements ItemLoadingListener {
    private SubredditFragment globalOffensive;


    private static final String TAG = "MainActivity";

    private ActivityMainBinding binding;

    // The fragments to show in the nav bar
    private PostsContainerFragment postsFragment;
    private ProfileFragment profileFragment;
    private LogInFragment logInFragment;
    private SettingsFragment settingsFragment;

    // Interface towards the Reddit API
    private RedditApi redditApi = RedditApi.getInstance(NetworkConstants.USER_AGENT);


    // Handler for token responses. If an access token is given user information is automatically retrieved
    private OnResponse<AccessToken> onTokenResponse = token -> {
        this.binding.loadingIcon.decreaseLoadCount();

        // Store the new token
        TokenManager.saveToken(token);

        // Re-create the start fragment as it now should load posts for the logged in user
        // TODO this is kinda bad as it gets posts and then gets posts again for the logged in user
        this.setupStartFragment();

        Snackbar.make(this.binding.parentLayout, R.string.loggedIn, Snackbar.LENGTH_SHORT).show();
    };
    private OnFailure onTokenFailure = (code, t) -> {
        this.binding.loadingIcon.decreaseLoadCount();

        Util.handleGenericResponseErrors(this.binding.parentLayout, code, t);
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        this.setupNavBar();
        this.setupStartFragment();
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
            if (state == null || !state.equals(App.getOAuthState())) {
                Util.showErrorLoggingInSnackbar(this.binding.parentLayout);
                return;
            }

            String code = uri.getQueryParameter("code");
            if (code == null) {
                Util.showErrorLoggingInSnackbar(this.binding.parentLayout);
                return;
            }

            App.clearOAuthState();

            // This might be bad, but onResume is called when opening a post and going back and still
            // holds the same intent which causes this branch to execute again, causing issues
            getIntent().replaceExtras(new Bundle());
            getIntent().setAction("");
            getIntent().setData(null);
            getIntent().setFlags(0);

            this.binding.loadingIcon.increaseLoadCount();
            this.redditApi.getAccessToken(code, this.onTokenResponse, this.onTokenFailure);
        }
    }

    @Override
    public void onAttachFragment(@NonNull Fragment fragment) {
        super.onAttachFragment(fragment);

        // When the PostsContainer has been attached, register listener for when it has started/finished loading something
        if (fragment instanceof PostsContainerFragment) {
            PostsContainerFragment f = (PostsContainerFragment) fragment;

            f.setLoadingListener(this);
        }
    }

    @Override
    public void onCountChange(boolean up) {
        if (up) {
            this.binding.loadingIcon.increaseLoadCount();
        } else {
            this.binding.loadingIcon.decreaseLoadCount();
        }
    }

    /**
     * Creates new fragments and passes along needed information such as the access token
     */
    private void setupStartFragment() {
        this.postsFragment = new PostsContainerFragment();

        getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragmentContainer, this.postsFragment)
            .commit();
    }

    /**
     * Sets up the navbar to be able to switch between the fragments
     */
    private void setupNavBar() {
        BottomNavigationView navBar = findViewById(R.id.bottomNav);
        navBar.setOnNavigationItemSelectedListener(item -> {
            Fragment selected = null;

            // Item pressed is same as item selected, do nothing as this adds to the backstack
            if (navBar.getSelectedItemId() == item.getItemId()) {
                return false;
            }

            switch (item.getItemId()) {
                case R.id.navHome:
                    selected = this.postsFragment;
                    break;

                case R.id.navSubreddit:
                    // TODO this
                    if (this.globalOffensive == null) {
                        this.globalOffensive = SubredditFragment.newInstance("GlobalOffensive");
                    }
                    selected = this.globalOffensive;
                    break;

                case R.id.navProfile:
                    // If not logged in, show log in page
                    if (TokenManager.getToken() == null) {
                        if (this.logInFragment == null) {
                            this.logInFragment = new LogInFragment();
                        }

                        selected = this.logInFragment;
                    } else {
                        if (this.profileFragment == null) {
                            this.profileFragment = new ProfileFragment();
                        }

                        selected = this.profileFragment;
                    }
                    break;

                case R.id.navSettings:
                    if (this.settingsFragment == null) {
                        this.settingsFragment = new SettingsFragment();
                    }

                    selected = this.settingsFragment;
                    break;

                default:
                    return false;
            }

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, selected)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    // When back is pressed go to the last fragment
                    .addToBackStack(null)
                    .commit();

            return true;
        });
    }

    /**
     * Click listener for the "Log out" button
     * <p>Revokes the access/refresh token and clears any information stored locally</p>
     *
     * @param view Ignored
     */
    public void btnLogOutOnClick(View view) {
        // Revoke token
        this.redditApi.revokeRefreshToken(response -> {
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