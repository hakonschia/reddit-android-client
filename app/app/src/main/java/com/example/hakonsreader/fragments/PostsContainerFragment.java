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
import com.example.hakonsreader.misc.SectionsPageAdapter;


/**
 * Fragment that contains the standard subreddit fragments: Front page, Popular, All
 */
public class PostsContainerFragment extends Fragment  {
    private static final String TAG = "PostsContainerFragment";

    /**
     * The key used to store the subreddit currently visible on the screen
     */
    private static final String ACTIVE_SUBREDDIT_KEY = "active_subreddit_posts_container";


    private ViewPager viewPager;
    private SubredditFragment[] fragments;
    private Bundle saveState;

    /**
     * Creates and initializes the fragments needed. Sets {@link PostsContainerFragment#fragments}
     *
     * <p>Note: Setup is only done if {@link PostsContainerFragment#fragments} is null</p>
     */
    private void setupFragments() {
        if (this.fragments == null) {
            this.fragments = new SubredditFragment[]{
                    SubredditFragment.Companion.newInstance(""),
                    SubredditFragment.Companion.newInstance("Popular"),
                    SubredditFragment.Companion.newInstance("All")
            };

            for (SubredditFragment fragment : fragments) {
                fragment.restoreState(saveState);
            }
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

        if (saveState != null) {
            viewPager.setCurrentItem(saveState.getInt(ACTIVE_SUBREDDIT_KEY), false);
        }
    }

    /**
     * Saves the state of the fragments to a bundle. Restore the state with {@link PostsContainerFragment#restoreState(Bundle)}
     *
     * @param saveState The bundle to store the state to
     */
    public void saveState(Bundle saveState) {
        // If the fragments haven't been recreated yet, ie. in a subreddit from the navbar and they were destroyed
        // but haven't yet been accessed again
        if (fragments != null) {
            for (SubredditFragment fragment : fragments) {
                // I don't believe this will happen, but in case the fragment has been killed by the OS don't cause a NPE
                if (fragment != null) {
                    fragment.saveState(saveState);
                }
            }

            saveState.putInt(ACTIVE_SUBREDDIT_KEY, viewPager.getCurrentItem());
        }
    }

    /**
     * Restores the state stored for when the activity holding the fragment has been recreated in a
     * way that doesn't permit the fragment to store its own state
     *
     * @param state The bundle holding the stored state
     */
    public void restoreState(Bundle state) {
        if (state != null) {
            saveState = state;
        }
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_posts_container, container, false);

        this.setupFragments();

        viewPager = view.findViewById(R.id.postsContainer);
        this.setupViewPager(viewPager);

        return view;
    }
}
