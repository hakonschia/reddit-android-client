package com.example.hakonsreader;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.model.AccessToken;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.api.model.RedditPostResponse;
import com.example.hakonsreader.api.model.User;
import com.example.hakonsreader.constants.OAuthConstants;
import com.example.hakonsreader.constants.SharedPreferencesConstants;
import com.example.hakonsreader.fragments.SubredditFragment;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private TextView activeSubredditName;
    private ViewPager viewPager;
    private WebView oauthWebView;

    private SharedPreferences prefs;
    private final RedditApi redditApi = RedditApi.getInstance();

    private AccessToken accessToken;
    private User user;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.initViews();
        //this.setupViewPager(this.viewPager);

        Gson gson = new Gson();
        // Load accesstoken, userinfo
        this.prefs = getSharedPreferences(SharedPreferencesConstants.PREFS_NAME, MODE_PRIVATE);

        this.accessToken = gson.fromJson(this.prefs.getString(SharedPreferencesConstants.ACCESS_TOKEN, ""), AccessToken.class);
        this.user = gson.fromJson(this.prefs.getString(SharedPreferencesConstants.USER_INFO, ""), User.class);

        if (this.accessToken != null) {
            //this.getUserInfo();
        }

        // TODO just load front page no matter what (4 fragments: custom sub - front page - popular - all)
        //  Show a nice bar with 4 sections on top under title to indicate that you can swipe (could also be clickable but need to be large enough)
        boolean loggedIn = false;


        if (loggedIn) {
            // Get new access token if close to expiration or something. This should probably be
            // its own function that gets a new token that gets called before every request
        }

        this.redditApi.getSubredditPosts("GlobalOffensive").enqueue(new Callback<RedditPostResponse>() {
            @Override
            public void onResponse(Call<RedditPostResponse> call, Response<RedditPostResponse> response) {

                Log.d(TAG, "onResponse: " + response);

                List<RedditPost> posts = response.body().getPosts();
                if (posts == null) {
                    Log.d(TAG, "onResponse: posts is null");

                    return;
                }
            }

            @Override
            public void onFailure(Call<RedditPostResponse> call, Throwable t) {
                t.printStackTrace();
            }
        });

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

            this.redditApi.getAccessToken(code).enqueue(new Callback<AccessToken>() {
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

                    SharedPreferences.Editor prefsEditor = prefs.edit();
                    Gson gson = new Gson();
                    String tokenJson = gson.toJson(accessToken);

                    prefsEditor.putString(SharedPreferencesConstants.ACCESS_TOKEN, tokenJson);
                    prefsEditor.apply();

                    Toast.makeText(MainActivity.this, "Logged in", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onFailure(Call<AccessToken> call, Throwable t) {
                    t.printStackTrace();
                }
            });
        }
    }

    /**
     * Initializes all UI elements
     */
    private void initViews() {
        this.activeSubredditName = findViewById(R.id.activeSubredditName);
        //this.viewPager = findViewById(R.id.container);
        //this.oauthWebView = findViewById(R.id.oauthWebView);
    }

    /**
     * Adds the subreddit fragments to the view pager
     *
     * @param viewPager The ViewPager to add the fragments to
     */
    public void setupViewPager(ViewPager viewPager) {
        SectionsStatePagerAdapter adapter = new SectionsStatePagerAdapter(getSupportFragmentManager(), 0);

        SubredditFragment frontPage = new SubredditFragment();
        SubredditFragment popular = new SubredditFragment();
        SubredditFragment all = new SubredditFragment();
        frontPage.setPosts(this.getDummyPosts());
        popular.setPosts(this.getDummyPosts());
        all.setPosts(this.getDummyPosts());

       // adapter.addFragment(new SubredditFragment(), "Custom sub");
        adapter.addFragment(frontPage, "Front page");
        adapter.addFragment(popular, "Popular");
        adapter.addFragment(all, "All");

        viewPager.setAdapter(adapter);
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

    private List<RedditPost> getDummyPosts() {
        List<RedditPost> posts = new ArrayList<>();
        posts.add(new RedditPost("GlobalOffensive", "FaZe won major", "hakonschia", "ger", false, 2));
        posts.add(new RedditPost("GlobalOffensive", "FaZe won major3", "hakonschia", "ger", false, 142));
        posts.add(new RedditPost("GlobalOffensive", "FaZe won major4", "hakonschia", "ger", false, 2222));
        posts.add(new RedditPost("GlobalOffensive", "FaZe won major5", "hakonschia", "ger", false, 235));

        return posts;
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
        this.redditApi.getUserInfo(this.accessToken).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (!response.isSuccessful()) {
                    Log.d(TAG, "onResponse: Error!");

                    return;
                }

                User user = response.body();
                if (user == null) {
                    Log.w(TAG, "onResponse: user is null");

                    return;
                }

                activeSubredditName.setText(user.getName());

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