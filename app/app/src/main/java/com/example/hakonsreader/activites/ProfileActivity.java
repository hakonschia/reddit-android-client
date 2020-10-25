package com.example.hakonsreader.activites;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hakonsreader.R;
import com.example.hakonsreader.fragments.ProfileFragment;

public class ProfileActivity extends AppCompatActivity {
    /**
     * The key used to send which username the profile is for
     */
    public static final String USERNAME_KEY = "username";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        String username = getIntent().getExtras().getString(USERNAME_KEY);

        ProfileFragment fragment = ProfileFragment.newInstance(username);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.profileContainer, fragment)
                .commit();
    }
}
