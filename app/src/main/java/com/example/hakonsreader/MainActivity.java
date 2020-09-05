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
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    public static int SCREEN_WIDTH;

    private static SharedPreferences prefs;

    // The fragments in the home menu (accessible from the nav bar)
    private List<Fragment> fragmentList;

    // The fragments found in fragmentList
    private PostsContainerFragment postsFragment;
    private ProfileFragment profileFragment;
    private LogInFragment logInFragment;

    // Interface towards the Reddit API
    private RedditApi redditApi;

    // The random string generated for OAuth authentication
    private String oauthState;


    // Response handler for access token response
    private Callback<AccessToken> tokenResponse = new Callback<AccessToken>() {
        @Override
        public void onResponse(Call<AccessToken> call, Response<AccessToken> response) {
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

            Toast.makeText(MainActivity.this, "Logged in", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onFailure(Call<AccessToken> call, Throwable t) {
            t.printStackTrace();

            Toast.makeText(MainActivity.this, "Network error probably", Toast.LENGTH_LONG).show();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SCREEN_WIDTH = getScreenWidth();

        prefs = getSharedPreferences(SharedPreferencesConstants.PREFS_NAME, MODE_PRIVATE);
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

            this.redditApi.getAccessToken(code, this.tokenResponse);
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
        this.profileFragment = new ProfileFragment();
        this.logInFragment = new LogInFragment();

        this.fragmentList = new ArrayList<>();
        this.fragmentList.add(this.postsFragment);
        this.fragmentList.add(this.profileFragment);
        this.fragmentList.add(this.logInFragment);

        // TODO find a proper way to do this without creating all fragments at the start and-
        //  having all fragments alive (keep state or something to keep posts)
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Add all fragments
        for (Fragment fragment : this.fragmentList) {
            transaction.add(R.id.fragmentContainer, fragment);

            // Hide all but the posts fragment (home page)
            if (fragment != this.postsFragment) {
                transaction.hide(fragment);
            }
        }

        // Commit all changes
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
                    // TODO this should recreate the fragment as a user might have logged in during the application
                    if (User.getStoredUser() == null) {
                        Log.d(TAG, "setupNavBar: Showing loginfragment");
                        selected = this.logInFragment;
                    } else {
                        selected = this.profileFragment;
                    }
                    break;

                default:
                    return false;
            }

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            // Loop through the list of fragments and show the selected, hide all others
            for (int i = 0; i < this.fragmentList.size(); i++) {
                Fragment current = this.fragmentList.get(i);
                if (current == selected) {
                    transaction.show(current);
                } else {
                    transaction.hide(current);
                }
            }

            transaction.commit();

            return true;
        });
    }

    /**
     * Retrieves user information about the currently logged in user
     */
    public void getUserInfo() {
        this.redditApi.getUserInfo(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
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
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                t.printStackTrace();
            }
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