package com.example.hakonsreader.fragments;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hakonsreader.R;
import com.example.hakonsreader.api.model.User;
import com.example.hakonsreader.constants.SharedPreferencesConstants;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;

/**
 * Fragment for the user profile
 */
public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";

    private User user;


    @Override
    public void setArguments(@Nullable Bundle args) {
        this.user = new Gson().fromJson(args.getString(SharedPreferencesConstants.USER_INFO), User.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        Resources resources = view.getResources();

        TextView username = view.findViewById(R.id.profileName);
        TextView age = view.findViewById(R.id.profileAge);
        TextView commentKarma = view.findViewById(R.id.profileCommentKarma);
        TextView postKarma = view.findViewById(R.id.profilePostKarma);

        if (this.user != null) {
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
        }

        return view;
    }

}
