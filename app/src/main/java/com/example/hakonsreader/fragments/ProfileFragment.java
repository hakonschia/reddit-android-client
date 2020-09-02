package com.example.hakonsreader.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hakonsreader.R;
import com.example.hakonsreader.api.model.User;
import com.example.hakonsreader.constants.OAuthConstants;

/**
 * Fragment for the user profile
 */
public class ProfileFragment extends Fragment {
    private User user;
    private Button btnLogIn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // TODO if logged in show this, if not show different layout with like "log in" or something
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        this.btnLogIn = view.findViewById(R.id.btnLogIn);
        this.btnLogIn.setOnClickListener(this::btnLogInOnClick);

        return view;
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
}
