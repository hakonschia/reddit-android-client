package com.example.hakonsreader.activites;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.fragments.ProfileFragment;
import com.example.hakonsreader.interfaces.LockableSlidr;
import com.r0adkll.slidr.Slidr;
import com.r0adkll.slidr.model.SlidrInterface;

/**
 * Activity to show a users profile.
 *
 * <p>This activity only holds a {@link android.widget.FrameLayout} that shows a
 * {@link ProfileFragment} with the given username</p>
 */
public class ProfileActivity extends AppCompatActivity implements LockableSlidr {

    /**
     * The key used to send which username the profile is for
     */
    public static final String USERNAME_KEY = "username";

    private SlidrInterface slidrInterface;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        String username = getIntent().getExtras().getString(USERNAME_KEY);

        ProfileFragment fragment = ProfileFragment.Companion.newInstance(username);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.profileContainer, fragment)
                .commit();

        slidrInterface = Slidr.attach(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        App.get().setActiveActivity(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void lock(boolean lock) {
        if (lock) {
            slidrInterface.lock();
        } else {
            slidrInterface.unlock();
        }
    }
}
