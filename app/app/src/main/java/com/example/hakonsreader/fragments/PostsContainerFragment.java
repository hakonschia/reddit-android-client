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
import com.example.hakonsreader.interfaces.ItemLoadingListener;
import com.example.hakonsreader.misc.SectionsPageAdapter;


/**
 * Fragment that contains the subreddit fragments
 */
public class PostsContainerFragment extends Fragment  {
    private static final String TAG = "PostsContainerFragment";

    private SubredditFragment[] fragments;
    private ItemLoadingListener loadingListener;

    /**
     * Creates and initializes the fragments needed. Sets {@link PostsContainerFragment#fragments}
     *
     * <p>Note: Setup is only done if {@link PostsContainerFragment#fragments} is null</p>
     */
    private void setupFragments() {
        if (this.fragments == null) {
            this.fragments = new SubredditFragment[]{
                    SubredditFragment.newInstance(""),
                    SubredditFragment.newInstance("Popular"),
                    SubredditFragment.newInstance("All")
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

        for (SubredditFragment fragment : this.fragments) {
            adapter.addFragment(fragment);
        }

        viewPager.setAdapter(adapter);

        // Always keep all fragments alive
        viewPager.setOffscreenPageLimit(3);
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_posts_container, container, false);

        this.setupFragments();

        ViewPager viewPager = view.findViewById(R.id.postsContainer);
        this.setupViewPager(viewPager);

        return view;
    }
}
