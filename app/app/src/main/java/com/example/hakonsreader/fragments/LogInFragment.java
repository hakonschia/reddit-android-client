package com.example.hakonsreader.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.constants.NetworkConstants;

/**
 * Fragment for the login page
 */
public class LogInFragment extends Fragment {
    private static final String TAG = "LogInFragment";
    
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
        // Generate a new state to validate when we get a response back
        String state = App.get().generateAndGetOAuthState();

        Uri uri = new Uri.Builder()
                .scheme("https")
                .authority("reddit.com")
                .path("api/v1/authorize")
                .appendQueryParameter("response_type", NetworkConstants.RESPONSE_TYPE)
                .appendQueryParameter("duration", NetworkConstants.DURATION)
                .appendQueryParameter("redirect_uri" ,NetworkConstants.CALLBACK_URL)
                .appendQueryParameter("client_id", NetworkConstants.CLIENT_ID)
                .appendQueryParameter("scope", NetworkConstants.SCOPE)
                .appendQueryParameter("state", state)
                .build();

        Intent oauthIntent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(oauthIntent);
    }
}
