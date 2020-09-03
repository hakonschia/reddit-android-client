package com.example.hakonsreader.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hakonsreader.R;
import com.example.hakonsreader.api.model.User;
import com.example.hakonsreader.constants.SharedPreferencesConstants;
import com.google.gson.Gson;

/**
 * Fragment for the user profile
 */
public class ProfileFragment extends Fragment {
    private User user;

    private TextView username;


    @Override
    public void setArguments(@Nullable Bundle args) {
        this.user = new Gson().fromJson(args.getString(SharedPreferencesConstants.USER_INFO), User.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (this.user == null) {

            return null;
            // Show a "log in" screen
        } else {
            View view = inflater.inflate(R.layout.fragment_profile, container, false);
            this.username = view.findViewById(R.id.profileName);

            this.username.setText(user.getName());

            return view;
        }
    }

}
