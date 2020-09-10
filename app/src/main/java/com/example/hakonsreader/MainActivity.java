package com.example.hakonsreader;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.interfaces.OnFailure;
import com.example.hakonsreader.api.interfaces.OnResponse;
import com.example.hakonsreader.api.model.AccessToken;
import com.example.hakonsreader.constants.NetworkConstants;
import com.example.hakonsreader.constants.SharedPreferencesConstants;
import com.example.hakonsreader.fragments.LogInFragment;
import com.example.hakonsreader.fragments.PostsContainerFragment;
import com.example.hakonsreader.fragments.ProfileFragment;
import com.example.hakonsreader.interfaces.ItemLoadingListener;
import com.example.hakonsreader.misc.SharedPreferencesManager;
import com.example.hakonsreader.misc.TokenManager;
import com.example.hakonsreader.views.LoadingIcon;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import okhttp3.logging.HttpLoggingInterceptor;

public class MainActivity extends AppCompatActivity implements ItemLoadingListener {
    private static final String TAG = "MainActivity";

    /**
     * The width of the screen of the current device
     */
    public static int SCREEN_WIDTH;

    private LoadingIcon loadingIcon;

    // The fragments to show in the nav bar
    private PostsContainerFragment postsFragment;
    private ProfileFragment profileFragment;
    private LogInFragment logInFragment;

    // Interface towards the Reddit API
    private RedditApi redditApi;

    // The random string generated for OAuth authentication
    private String oauthState;


    // Handler for token responses. If an access token is given user information is automatically retrieved
    private OnResponse<AccessToken> onTokenResponse = token -> {
        this.loadingIcon.decreaseLoadCount();

        // Store the new token
        TokenManager.saveToken(token);

        // Re-create the start fragment as it now should load posts for the logged in user
        //this.setupStartFragment();

        Toast.makeText(MainActivity.this, "Logged in", Toast.LENGTH_LONG).show();
    };
    private OnFailure onTokenFailure = (call, t) -> this.loadingIcon.decreaseLoadCount();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SCREEN_WIDTH = getScreenWidth();

        this.loadingIcon = findViewById(R.id.loading_icon);

        SharedPreferences prefs = getSharedPreferences(SharedPreferencesConstants.PREFS_NAME, MODE_PRIVATE);
        SharedPreferencesManager.create(prefs);

        this.setupRedditApi();

        this.setupNavBar();
        this.setupStartFragment();

        if (prefs.getBoolean(SharedPreferencesConstants.NIGHT_MODE, false)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
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
            String code = uri.getQueryParameter("code");

            this.loadingIcon.increaseLoadCount();
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
            this.loadingIcon.increaseLoadCount();
        } else {
            this.loadingIcon.decreaseLoadCount();
        }
    }


    /**
     * @return The width of the screen in pixels
     */
    private int getScreenWidth() {
        return getResources().getDisplayMetrics().widthPixels;
    }

    /**
     * Sets up the reddit API object
     */
    private void setupRedditApi() {
        // Set the previously stored token, and the listener for new tokens
        this.redditApi = RedditApi.getInstance(NetworkConstants.USER_AGENT);
        this.redditApi.setToken(TokenManager.getToken());
        this.redditApi.setOnNewToken(TokenManager::saveToken);
        this.redditApi.setLoggingLevel(HttpLoggingInterceptor.Level.BODY);
        this.redditApi.setCallbackURL(NetworkConstants.CALLBACK_URL);
        this.redditApi.setClientID(NetworkConstants.CLIENT_ID);
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
                case R.id.nav_home:
                    selected = this.postsFragment;
                    break;

                case R.id.nav_subreddit:
                    // TODO this
                    break;

                case R.id.nav_profile:
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