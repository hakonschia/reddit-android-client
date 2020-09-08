package com.example.hakonsreader.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hakonsreader.R;
import com.example.hakonsreader.constants.NetworkConstants;

/**
 * Fragment for the login page
 */
public class LogInFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_log_in, container, false);

        // Bind the button onClick to open the OAuth page
        view.findViewById(R.id.btnLogIn).setOnClickListener(this::requestOAuth);

        return view;
    }

    /**
     * Opens a web page to log a user in with OAuth
     */
    private void requestOAuth(View view) {
        // TODO generate random state, make sure it is the same when we get a result in onResume
        String url = String.format(
                "%s?client_id=%s&response_type=%s&state=%s&redirect_uri=%s&scope=%s&duration=%s",
                "https://www.reddit.com/api/v1/authorize/",
                NetworkConstants.CLIENT_ID,
                NetworkConstants.RESPONSE_TYPE,
                "randomString", // state
                NetworkConstants.CALLBACK_URL,
                NetworkConstants.SCOPE,
                NetworkConstants.DURATION
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
}
