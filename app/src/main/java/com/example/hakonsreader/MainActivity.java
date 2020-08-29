package com.example.hakonsreader;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.hakonsreader.api.model.AccessToken;
import com.example.hakonsreader.api.service.RedditService;
import com.example.hakonsreader.constants.NetworkConstants;
import com.example.hakonsreader.constants.OAuthConstants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        boolean loggedIn = false;

        if (loggedIn) {
            // Go to another activity (with reddit front page or something)
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
            String state = uri.getQueryParameter("state");

            Retrofit.Builder builder = new Retrofit.Builder()
                    .baseUrl(NetworkConstants.REDDIT_API_URL)
                    .addConverterFactory(GsonConverterFactory.create());

            Retrofit retrofit = builder.build();

            RedditService service = retrofit.create(RedditService.class);
            Call<AccessToken> accessTokenCall = service.getAccessToken(
                    "authorization_code",
                    code,
                    OAuthConstants.CALLBACK_URL
            );

            accessTokenCall.enqueue(new Callback<AccessToken>() {
                @Override
                public void onResponse(Call<AccessToken> call, Response<AccessToken> response) {
                    Toast.makeText(MainActivity.this, "Nice bruh", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onFailure(Call<AccessToken> call, Throwable t) {
                    Toast.makeText(MainActivity.this, "not nice bruh", Toast.LENGTH_LONG).show();
                }
            });
        }
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
     *
     */
    private void requestOAuth() {
        // TODO generate random state, make sure it is the same when we get a result in onResume
        String url = String.format(
                "%s?client_id=%s&response_type=%s&state=%s&redirect_uri=%s&duration=%s&scope=%s",
                OAuthConstants.REDDIT_OAUTH_URL,
                OAuthConstants.CLIENT_ID,
                OAuthConstants.RESPONSE_TYPE,
                "randomString", // state
                OAuthConstants.CALLBACK_URL,
                OAuthConstants.DURATION,
                OAuthConstants.SCOPE
        );

        Intent oauthIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(oauthIntent);
    }

}