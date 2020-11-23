package com.example.hakonsreader.activites;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hakonsreader.R;
import com.example.hakonsreader.fragments.LogInFragment;

/**
 * Activity that serves as a container for a {@link LogInFragment}
 */
public class LogInActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, new LogInFragment())
                .commit();
    }
}
