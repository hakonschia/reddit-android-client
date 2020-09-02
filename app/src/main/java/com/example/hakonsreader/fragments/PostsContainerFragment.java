package com.example.hakonsreader.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.example.hakonsreader.R;
import com.example.hakonsreader.SectionsPageAdapter;
import com.example.hakonsreader.api.model.RedditPost;
import com.google.android.material.tabs.TabLayout;

import java.util.List;

/**
 * Fragment that contains the subreddit fragments
 */
public class PostsContainerFragment extends Fragment {
    private static final String TAG = "PostsContainerFragment";

    private PostsFragment frontPage = new PostsFragment("Front page");
    private PostsFragment popular = new PostsFragment("Popular");
    private PostsFragment all = new PostsFragment("All");

    public PostsContainerFragment() {
        Log.d(TAG, "PostsContainerFragment: Creating PostsContainerFragment");
    }
    
    public void setFrontPagePosts(List<RedditPost> posts) {
        this.frontPage.setPosts(posts);
    }

    /**
     * Adds the different subreddit (frontpage, popular, all, and custom) fragments to the view pager
     *
     * @param viewPager The view pager to add the fragments to
     */
    private void setupViewPager(ViewPager viewPager) {
        SectionsPageAdapter adapter = new SectionsPageAdapter(getActivity().getSupportFragmentManager(), 0);
        adapter.addFragment(this.frontPage);
        adapter.addFragment(this.popular);
        adapter.addFragment(this.all);

        viewPager.setAdapter(adapter);

        // Always keep all fragments alive
        viewPager.setOffscreenPageLimit(3);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_posts_container, container, false);

        ViewPager viewPager = view.findViewById(R.id.postsContainer);
        setupViewPager(viewPager);


        Log.d(TAG, "PostsContainerFragment: Creating view PostsContainerFragment");

        return view;
    }
}
