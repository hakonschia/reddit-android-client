package com.example.hakonsreader.fragments;

import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hakonsreader.R;
import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.model.User;
import com.example.hakonsreader.interfaces.OnFailure;
import com.example.hakonsreader.interfaces.OnResponse;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;

/**
 * Fragment for the user profile
 */
public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";

    private boolean firstLoad = true;

    private RedditApi redditApi = RedditApi.getInstance();
    private User user;

    private TextView username;
    private TextView age;
    private TextView commentKarma;
    private TextView postKarma;
    private ImageView picture;

    // Response handler for retrieval of user information
    private OnResponse<User> onUserResponse = (call, response) -> {
        if (!response.isSuccessful()) {
            Log.d(TAG, "onResponse: Error");

            return;
        }

        User user = response.body();
        if (user == null) {
            Log.w(TAG, "onResponse: user is null");

            return;
        }

        // Store the updated user information
        User.storeUserInfo(user);
        this.user = user;

        this.updateViews();
    };
    private OnFailure<User> onUserFailure = (call, t) -> { };


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        username = view.findViewById(R.id.profileName);
        age = view.findViewById(R.id.profileAge);
        commentKarma = view.findViewById(R.id.profileCommentKarma);
        postKarma = view.findViewById(R.id.profilePostKarma);
        picture = view.findViewById(R.id.profilePicture);

        this.user = User.getStoredUser();

        // Retrieve user info if there is no user previously stored, or if it's the fragments first time
        // loading (to ensure the information is up-to-date)
        if (this.user == null || this.firstLoad) {
            // Retrieve user info and then update
            this.getUserInfo();
            this.firstLoad = false;
        } else {
            this.updateViews();
        }

        return view;
    }


    /**
     * Updates the views with the information found in {@link ProfileFragment#user}
     */
    private void updateViews() {
        Resources resources = getResources();

        // Format date as "5. September 2012"
        SimpleDateFormat dateFormat = new SimpleDateFormat("d. MMMM y", Locale.getDefault());
        Date date = Date.from(Instant.ofEpochSecond(this.user.getCreatedAt()));

        String ageText = String.format(resources.getString(R.string.profileAge), dateFormat.format(date));
        String commentKarmaText = String.format(resources.getString(R.string.commentKarma), this.user.getCommentKarma());
        String postKarmaText = String.format(resources.getString(R.string.postKarma), this.user.getPostKarma());

        username.setText(this.user.getName());
        age.setText(ageText);
        commentKarma.setText(commentKarmaText);
        postKarma.setText(postKarmaText);

        // Load the users profile picture
        Picasso.get().load(this.user.getProfilePictureUrl()).into(picture);
    }

    /**
     * Retrieves user information about the currently logged in user
     */
    public void getUserInfo() {
        this.redditApi.getUserInfo(this.onUserResponse, this.onUserFailure);
    }
}
