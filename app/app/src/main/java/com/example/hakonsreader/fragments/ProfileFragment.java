package com.example.hakonsreader.fragments;

import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.interfaces.OnFailure;
import com.example.hakonsreader.api.interfaces.OnResponse;
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

    private PostsViewModel postsViewModel;
    private PostsAdapter adapter;
    private LinearLayoutManager layoutManager;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = FragmentProfileBinding.inflate(getLayoutInflater());

        adapter = new PostsAdapter();
        layoutManager = new LinearLayoutManager(getContext());

        postsViewModel = new ViewModelProvider(this, new PostsFactory(
                getContext()
        )).get(PostsViewModel.class);
        postsViewModel.getPosts().observe(this, posts -> {
            adapter.addPosts(posts);
        });
        postsViewModel.loadPosts(binding.parentLayout, "hakonschia", true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding.posts.setAdapter(adapter);
        binding.posts.setLayoutManager(layoutManager);

//        user = User.getStoredUser();

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
        Resources resources = getResources();

        // Format date as "5. September 2012"
        SimpleDateFormat dateFormat = new SimpleDateFormat("d. MMMM y", Locale.getDefault());
        Date date = Date.from(Instant.ofEpochSecond(user.getCreatedAt()));

        String ageText = String.format(resources.getString(R.string.profileAge), dateFormat.format(date));
        String commentKarmaText = String.format(resources.getString(R.string.commentKarma), user.getCommentKarma());
        String postKarmaText = String.format(resources.getString(R.string.postKarma), user.getPostKarma());

        binding.username.setText(user.getName());
        binding.profileAge.setText(ageText);
        binding.commentKarma.setText(commentKarmaText);
        binding.postKarma.setText(postKarmaText);

        // Load the users profile picture
        Picasso.get().load(user.getProfilePictureUrl()).into(binding.profilePicture);
    }

    /**
     * Retrieves user information about the currently logged in user
     */
    public void getUserInfo() {
        redditApi.user().getInfo(user -> {
            // Store the updated user information
            //User.storeUserInfo(user);
            this.user = user;

            this.updateViews();
        }, (code, t) -> {
            t.printStackTrace();
            Util.handleGenericResponseErrors(binding.parentLayout, code, t);
        });
    }
}
