package com.example.hakonsreader;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.model.AccessToken;
import com.example.hakonsreader.api.model.User;
import com.example.hakonsreader.constants.OAuthConstants;
import com.example.hakonsreader.constants.SharedPreferencesConstants;
import com.example.hakonsreader.fragments.LogInFragment;
import com.example.hakonsreader.fragments.PostsContainerFragment;
import com.example.hakonsreader.fragments.ProfileFragment;
import com.example.hakonsreader.interfaces.OnFailure;
import com.example.hakonsreader.interfaces.OnResponse;
import com.example.hakonsreader.misc.SharedPreferencesManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    /**
     * The width of the screen of the current device
     */
    public static int SCREEN_WIDTH;

    // The fragments to show in the nav bar
    private PostsContainerFragment postsFragment;
    private ProfileFragment profileFragment;
    private LogInFragment logInFragment;

    // Interface towards the Reddit API
    private RedditApi redditApi;

    // The random string generated for OAuth authentication
    private String oauthState;


    // Handler for token responses. If an access token is given user information is automatically retrieved
    private OnResponse<AccessToken> onTokenResponse = (call, response) -> {
        if (!response.isSuccessful()) {
            Toast.makeText(MainActivity.this, "Access not given " + response.code(), Toast.LENGTH_LONG).show();

            return;
        }

        AccessToken token = response.body();
        if (token == null) {
            // TODO some error handling

            return;
        }

        // Assume the token was created when the request was sent
        token.setRetrievedAt(response.raw().sentRequestAtMillis());

        // Store access token
        AccessToken.storeToken(token);

        this.getUserInfo();
        Toast.makeText(MainActivity.this, "Logged in", Toast.LENGTH_LONG).show();
    };
    private OnFailure<AccessToken> onTokenFailure = (call, t) -> {

    };

    // Response handler for retrieval of user information
    private OnResponse<User> onUserResponse = (call, response) -> {
        if (!response.isSuccessful()) {
            Log.d(TAG, "onResponse: Error");

            return;
        }

        User user = response.body();
        if (user == null) {
            Log.w(TAG, "onResponse: user is null");

            return;
        }

        // Store the updated user information
        User.storeUserInfo(user);

        // Make sure the profile fragment is updated next time the profile is selected
        this.profileFragment = null;
    };
    private OnFailure<User> onUserFailure = (call, t) -> {

    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SCREEN_WIDTH = getScreenWidth();

        SharedPreferences prefs = getSharedPreferences(SharedPreferencesConstants.PREFS_NAME, MODE_PRIVATE);
        SharedPreferencesManager.create(prefs);

        this.setupNavBar();
        this.setupFragments();

        this.redditApi = RedditApi.getInstance();

        // If there is a user logged in retrieve updated user information
        if (AccessToken.getStoredToken() != null) {
            this.getUserInfo();
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
        if (uri.toString().startsWith(OAuthConstants.CALLBACK_URL)) {
            String code = uri.getQueryParameter("code");

            this.redditApi.getAccessToken(code, this.onTokenResponse, this.onTokenFailure);
        }
    }


    private int getScreenWidth() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.widthPixels;
    }

    /**
     * Creates new fragments and passes along needed information such as the access token
     */
    private void setupFragments() {
        this.postsFragment = new PostsContainerFragment();

        // TODO find a proper way to do this without creating all fragments at the start and-
        //  having all fragments alive (keep state or something to keep posts)
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, this.postsFragment);
        transaction.commit();
    }

    /**
     * Sets up the navbar to be able to switch between the fragments
     */
    private void setupNavBar() {
        BottomNavigationView navBar = findViewById(R.id.bottomNav);
        navBar.setOnNavigationItemSelectedListener(item -> {
            Fragment selected = null;

            switch (item.getItemId()) {
                case R.id.nav_home:
                    selected = this.postsFragment;
                    break;

                case R.id.nav_subreddit:
                    // TODO this
                    break;

                case R.id.nav_profile:
                    // If not logged in, show log in page
                    if (User.getStoredUser() == null) {
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
     * Retrieves user information about the currently logged in user
     */
    public void getUserInfo() {
        this.redditApi.getUserInfo(this.onUserResponse, this.onUserFailure);
    }

    /**
     * Click listener for the "Log out" button
     * <p>Revokes the access/refresh token and clears any information stored locally</p>
     *
     * @param view Ignored
     */
    public void btnLogOutOnClick(View view) {
        // Revoke token
        this.redditApi.revokeRefreshToken().enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    // Show "Logged out" or something
                } else {
                    // Idk?
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // If failed because of internet connection try to revoke later
            }
        });

        // Clear shared preferences
        this.clearUserInfoFromPrefs();
    }

    /**
     * Clears any information stored locally about a logged in user from SharedPreferences
     */
    private void clearUserInfoFromPrefs() {
        SharedPreferencesManager.remove(SharedPreferencesConstants.ACCESS_TOKEN);
        SharedPreferencesManager.remove(SharedPreferencesConstants.USER_INFO);
    }
}