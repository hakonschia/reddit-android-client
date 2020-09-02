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

import java.util.List;

/**
 * Fragment that contains the subreddit fragments
 */
public class PostsContainerFragment extends Fragment {
    private static final String TAG = "PostsContainerFragment";

    private SubredditFragment frontPage = new SubredditFragment("Front page");
    private SubredditFragment popular = new SubredditFragment("Popular");
    private SubredditFragment all = new SubredditFragment("All");

    private SubredditFragment[] fragments = {frontPage, popular, all};


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

        for (Fragment fragment : this.fragments) {
            adapter.addFragment(fragment);
        }

        viewPager.setAdapter(adapter);

        // Always keep all fragments alive
        viewPager.setOffscreenPageLimit(3);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                fragments[position].onFragmentSelected();
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                // Not implemented
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                // Not implemented
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_posts_container, container, false);

        ViewPager viewPager = view.findViewById(R.id.postsContainer);
        setupViewPager(viewPager);

        return view;
    }
}
