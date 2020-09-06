package com.example.hakonsreader.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.example.hakonsreader.R;
import com.example.hakonsreader.SectionsPageAdapter;


/**
 * Fragment that contains the subreddit fragments
 */
public class PostsContainerFragment extends Fragment {
    private static final String TAG = "PostsContainerFragment";

    private SubredditFragment[] fragments;


    /**
     * Creates and initializes the fragments needed. Sets the fragments array
     * Note: Setup is only done if {@link PostsContainerFragment#fragments} is null
     */
    private void setupFragments() {
        if (this.fragments == null) {
            this.fragments = new SubredditFragment[]{
                    new SubredditFragment(""),
                    new SubredditFragment("Popular"),
                    new SubredditFragment("All")
            };
        }
    }

    /**
     * Adds the different subreddit (frontpage, popular, all, and custom) fragments to the view pager
     *
     * @param viewPager The view pager to add the fragments to
     */
    private void setupViewPager(ViewPager viewPager) {
        SectionsPageAdapter adapter = new SectionsPageAdapter(getChildFragmentManager(), FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);

        for (Fragment fragment : this.fragments) {
            adapter.addFragment(fragment);
        }

        viewPager.setAdapter(adapter);

        // Always keep all fragments alive
        viewPager.setOffscreenPageLimit(3);

        // Listen to changes to let the fragments know they have been selected
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

        this.setupFragments();

        ViewPager viewPager = view.findViewById(R.id.postsContainer);
        this.setupViewPager(viewPager);

        // onFragmentSelected is called when the ViewPager notices a swipe, so manually call for the first
        this.fragments[0].onFragmentSelected();

        return view;
    }
}
