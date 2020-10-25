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
import androidx.databinding.BindingAdapter;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.model.User;
import com.example.hakonsreader.databinding.FragmentProfileBinding;
import com.example.hakonsreader.misc.Util;
import com.example.hakonsreader.recyclerviewadapters.PostsAdapter;
import com.example.hakonsreader.viewmodels.PostsViewModel;
import com.example.hakonsreader.viewmodels.factories.PostsFactory;
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

    private final RedditApi redditApi = App.get().getApi();
    private FragmentProfileBinding binding;
    private User user;
    /**
     * The username of the user to retrieve information for. If this is null, the fragment is
     * for logged in users
     */
    private String username;

    private PostsViewModel postsViewModel;
    private PostsAdapter adapter;
    private LinearLayoutManager layoutManager;


    /**
     * Creates a new ProfileFragment for logged in users
     *
     * @return A new ProfileFragment for logged in users
     */
    public static ProfileFragment newInstance() {
        return new ProfileFragment();
    }

    /**
     * Create a new ProfileFragment for a user that is NOT the logged in user. For logged in users
     * user {@link ProfileFragment#newInstance()}
     *
     * @param username The username to create the fragment for
     * @return A ProfileFragment for a user
     */
    public static ProfileFragment newInstance(String username) {
        Bundle args = new Bundle();
        args.putString("username", username);

        ProfileFragment fragment = new ProfileFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = FragmentProfileBinding.inflate(getLayoutInflater());

        Bundle args = getArguments();
        if (args != null) {
            username = args.getString("username");
        }

        adapter = new PostsAdapter();
        layoutManager = new LinearLayoutManager(getContext());

        postsViewModel = new ViewModelProvider(this, new PostsFactory(
                getContext()
        )).get(PostsViewModel.class);
        postsViewModel.getPosts().observe(this, posts -> {
            adapter.addPosts(posts);
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding.posts.setAdapter(adapter);
        binding.posts.setLayoutManager(layoutManager);

        // No username given, load profile for logged in user
        if (username == null) {
            user = User.getStoredUser();
        } else {
            binding.username.setText(username);
        }

        // Retrieve user info if there is no user previously stored, or if it's the fragments first time
        // loading (to ensure the information is up-to-date)
        if (user == null || firstLoad) {
            // Retrieve user info and then update
            this.getUserInfo();
            firstLoad = false;
        } else {
            this.updateViews();
        }

        return binding.getRoot();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    /**
     * Updates the views with the information found in {@link ProfileFragment#user}
     */
    private void updateViews() {
        binding.setUser(user);
        binding.setLoggedInUser(username == null);

    }

    /**
     * Retrieves user information about the currently logged in user
     */
    public void getUserInfo() {
        // user(null) gets information about the logged in user, so we can use username directly
        redditApi.user(username).getInfo(user -> {
            // Load the posts for the user
            postsViewModel.loadPosts(binding.parentLayout, user.getName(), true);
            this.user = user;

            // Store the updated user information if this profile is for the logged in user
            if (username == null) {
                User.storeUserInfo(user);
            }

            this.updateViews();
        }, (code, t) -> {
            t.printStackTrace();
            Util.handleGenericResponseErrors(binding.parentLayout, code, t);
        });
    }


    /**
     * Binding adapter for setting the profile picture
     *
     * @param imageView The view to insert the profile picture into
     * @param url The URL for the profile picture
     */
    @BindingAdapter("profilePicture")
    public static void setProfilePicture(ImageView imageView, String url) {
        // Load the users profile picture
        Picasso.get()
                .load(url)
                .placeholder(R.drawable.ic_baseline_person_100)
                .error(R.drawable.ic_baseline_person_100)
                .into(imageView);
    }

    /**
     * Binding adapter to set the profile age text. The text is formatted as "d. MMMM y",
     * 5. September 2012"
     *
     * @param textView The TextView to set the text on
     * @param createdAt The timestamp, in seconds, the profile was created
     */
    @BindingAdapter("profileAge")
    public static void setProfileAge(TextView textView, long createdAt) {
        // Format date as "5. September 2012"
        SimpleDateFormat dateFormat = new SimpleDateFormat("d. MMMM y", Locale.getDefault());
        Date date = Date.from(Instant.ofEpochSecond(createdAt));

        String ageText = String.format(textView.getResources().getString(R.string.profileAge), dateFormat.format(date));
        textView.setText(ageText);
    }
}
