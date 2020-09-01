package com.example.hakonsreader;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.model.AccessToken;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.api.model.RedditPostResponse;
import com.example.hakonsreader.api.model.User;
import com.example.hakonsreader.constants.OAuthConstants;
import com.example.hakonsreader.constants.SharedPreferencesConstants;
import com.example.hakonsreader.fragments.PostsFragment;
import com.example.hakonsreader.recyclerviewadapters.PostsAdapter;
import com.google.gson.Gson;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private ViewPager viewPager;
    private PostsFragment frontPage = new PostsFragment();
    private PostsFragment popular = new PostsFragment();
    private PostsFragment all = new PostsFragment();

    private TextView activeSubredditName;

    private static SharedPreferences prefs;
    private RedditApi redditApi;

    private AccessToken accessToken;
    private User user;

    private Callback<AccessToken> tokenResponse = new Callback<AccessToken>() {
        @Override
        public void onResponse(Call<AccessToken> call, Response<AccessToken> response) {
            if (!response.isSuccessful()) {
                Toast.makeText(MainActivity.this, "Access not given " + response.code(), Toast.LENGTH_LONG).show();

                return;
            }

            // Store access token in SharedPreferences
            accessToken = response.body();

            if (accessToken == null) {
                // TODO some error handling

                return;
            }

            // Assume the token was created when the request was sent
            accessToken.setRetrievedAt(response.raw().sentRequestAtMillis());

            saveAccessToken(accessToken);
            RedditApi.setAccessToken(accessToken);

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

        this.initViews();
        this.setupViewPager(this.viewPager);

        Gson gson = new Gson();

        // Load accesstoken, userinfo
        prefs = getSharedPreferences(SharedPreferencesConstants.PREFS_NAME, MODE_PRIVATE);

        this.accessToken = gson.fromJson(this.prefs.getString(SharedPreferencesConstants.ACCESS_TOKEN, ""), AccessToken.class);
        this.user = gson.fromJson(this.prefs.getString(SharedPreferencesConstants.USER_INFO, ""), User.class);

        redditApi = RedditApi.getInstance(accessToken);

        // If there is a user logged in retrieve updated user information
        if (this.accessToken != null) {
            this.getUserInfo();
        }

        if (this.accessToken.expiresSoon()) {
            Log.d(TAG, "onCreate: access token about expire");
        }

        this.getFrontPagePosts();

        // TODO just load front page no matter what (4 fragments: custom sub - front page - popular - all)
        //  Show a nice bar with 4 sections on top under title to indicate that you can swipe (could also be clickable but need to be large enough)


        /*
        this.redditApi.getSubredditPosts("GlobalOffensive").enqueue(new Callback<RedditPostResponse>() {
            @Override
            public void onResponse(Call<RedditPostResponse> call, Response<RedditPostResponse> response) {

                Log.d(TAG, "onResponse: " + response);

                List<RedditPost> posts = response.body().getPosts();
                if (posts == null) {
                    Log.d(TAG, "onResponse: posts is null");

                    return;
                }

                posts.forEach(post -> {
                    Log.d(TAG, post.getTitle());
                });

                adapter.setPosts(posts);
            }

            @Override
            public void onFailure(Call<RedditPostResponse> call, Throwable t) {
                t.printStackTrace();
            }
        });
         */
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

            this.redditApi.getAccessToken(code).enqueue(this.tokenResponse);
        }
    }

    /**
     * Saves an AccessToken object to SharedPreferences
     *
     * @param accessToken The token to save
     */
    public static void saveAccessToken(AccessToken accessToken) {
        // TODO this is probably bad? No idea if this is guaranteed to be "alive" at all times
        SharedPreferences.Editor prefsEditor = prefs.edit();
        Gson gson = new Gson();
        String tokenJson = gson.toJson(accessToken);

        Log.d(TAG, "saveAccessToken: " + tokenJson);

        prefsEditor.putString(SharedPreferencesConstants.ACCESS_TOKEN, tokenJson);
        prefsEditor.apply();
    }


    /**
     * Adds the different subreddit (frontpage, popular, all, and custom) fragments to the view pager
     *
     * @param viewPager The view pager to add the fragments to
     */
    private void setupViewPager(ViewPager viewPager) {
        SectionsPageAdapter adapter = new SectionsPageAdapter(getSupportFragmentManager(), 0);
        adapter.addFragment(this.frontPage);
        adapter.addFragment(this.popular);
        adapter.addFragment(this.all);

        viewPager.setAdapter(adapter);
    }


    /**
     * Initializes all UI elements
     */
    private void initViews() {
        this.activeSubredditName = findViewById(R.id.activeSubredditName);
        this.viewPager = findViewById(R.id.container);
    }

    /**
     * Retrieves posts from reddit.com/
     * <p>Uses the stored access token to retrieve the custom front page if a user is logged in</p>
     */
    private void getFrontPagePosts() {
        /*
        if (this.accessToken.expiresSoon()) {
            this.refreshToken();
        }
         */

        this.redditApi.getFrontPagePosts(this.accessToken).enqueue(new Callback<RedditPostResponse>() {
            @Override
            public void onResponse(Call<RedditPostResponse> call, Response<RedditPostResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    return;
                }

                List<RedditPost> posts = response.body().getPosts();

                frontPage.setPosts(posts);
            }

            @Override
            public void onFailure(Call<RedditPostResponse> call, Throwable t) {

            }
        });
    }

    /**
     * Opens a web page to log a user in with OAuth
     */
    private void requestOAuth() {
        // TODO generate random state, make sure it is the same when we get a result in onResume
        String url = String.format(
                "%s?client_id=%s&response_type=%s&state=%s&redirect_uri=%s&scope=%s&duration=%s",
                "https://www.reddit.com/api/v1/authorize/",
                OAuthConstants.CLIENT_ID,
                OAuthConstants.RESPONSE_TYPE,
                "randomString", // state
                OAuthConstants.CALLBACK_URL,
                OAuthConstants.SCOPE,
                OAuthConstants.DURATION
        );

        /*
        this.oauthWebView.loadUrl(url);

        this.oauthWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d(TAG, "onPageFinished: View finished: " + url);

                super.onPageFinished(view, url);
            }
        });
*/
        // Maybe WebView is better so it doesnt open a million web pages?
        Intent oauthIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(oauthIntent);
    }

    /* ---------------- Event listeners ---------------- */
    /**
     * Opens the Reddit OAuth window to log in to Reddit
     *
     * @param view
     */
    public void btnLogInOnClick(View view) {
        this.requestOAuth();
    }

    /**
     * Retrieves user information about the currently logged in user
     */
    public void getUserInfo() {
        /*
        if (this.accessToken.expiresSoon()) {
            this.refreshToken();
        }
         */

        // Hmm this wont wait for the token to be refreshed
        this.redditApi.getUserInfo(this.accessToken).enqueue(new Callback<User>() {
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

                activeSubredditName.setText(user.getName());

                // Store the updated user information in SharedPreferences
                Gson gson = new Gson();
                String tokenJson = gson.toJson(user);

                SharedPreferences.Editor prefsEditor = prefs.edit();
                prefsEditor.putString(SharedPreferencesConstants.USER_INFO, tokenJson);
                prefsEditor.apply();
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }
}