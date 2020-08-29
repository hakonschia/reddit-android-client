package com.example.hakonsreader;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.example.hakonsreader.constants.NetworkConstants;
import com.example.hakonsreader.constants.OAuthConstants;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.requestOAuth();
    }

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


    @Override
    protected void onResume() {
        super.onResume();

        Uri uri = getIntent().getData();

        if (uri == null) {
            return;
        }
    }
}