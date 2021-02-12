package com.example.hakonsreader.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.example.hakonsreader.R
import com.example.hakonsreader.api.enums.PostTimeSort
import com.example.hakonsreader.api.enums.SortingMethods
import com.example.hakonsreader.misc.SectionsPageAdapter
import java.util.ArrayList

class StandardSubContainerFragment : Fragment() {

    companion object {
        /**
         * The key used to store the subreddit currently visible on the screen
         */
        private const val ACTIVE_SUBREDDIT_KEY = "active_subreddit_posts_container"

        /**
         * @return A new instance of this fragment
         */
        fun newInstance() = StandardSubContainerFragment()
    }

    /**
     * Enum representing the names of the standard subs that this fragment can display.
     *
     * The enum value represents the subreddit name in lowercase, where front page is represented
     * as an empty string
     */
    enum class StandarSub(val value: String) {
        FRONT_PAGE(""),
        POPULAR("popular"),
        ALL("all")
    }

    /**
     * The sub to load as the default. This has to be set before the fragments view is rendered
     */
    var defaultSub: StandarSub = StandarSub.FRONT_PAGE

    private var saveState: Bundle? = null
    private var viewPager: ViewPager? = null
    private val fragments = ArrayList<SubredditFragment>()

    fun setActiveSubreddit(sub: StandarSub) {
        viewPager?.currentItem = sub.ordinal
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_posts_container, container, false)

        setupFragments()

        viewPager = view.findViewById<ViewPager>(R.id.postsContainer).apply { setupViewPager(this) }

        return view
    }

    private fun setupViewPager(viewPager: ViewPager) {
        val adapter = SectionsPageAdapter(childFragmentManager, FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT).apply {
            fragments.forEach {
                addFragment(it)
            }
        }

        viewPager.adapter = adapter

        // Always keep all fragments alive
        viewPager.offscreenPageLimit = 3

        if (saveState != null) {
            viewPager.setCurrentItem(saveState!!.getInt(ACTIVE_SUBREDDIT_KEY), false)
        } else {
            viewPager.setCurrentItem(getDefaultSubPosition(), false)
        }
    }

    /**
     * Sets up [fragments], if it is empty
     */
    private fun setupFragments() {
        if (fragments.isEmpty()) {
            fragments.apply {
                // We could use the enum value, but this is probably the easiest way to get the first letter capitalized
                add(SubredditFragment.newInstance("", SortingMethods.HOT, PostTimeSort.DAY).apply { restoreState(saveState) })
                add(SubredditFragment.newInstance("Popular", SortingMethods.HOT, PostTimeSort.DAY).apply { restoreState(saveState) })
                add(SubredditFragment.newInstance("All", SortingMethods.HOT, PostTimeSort.DAY).apply { restoreState(saveState) })
            }
        }
    }

    /**
     * Gets the array position in [fragments] based on [defaultSub]
     */
    private fun getDefaultSubPosition() = defaultSub.ordinal

    /**
     * Restores the state stored for when the activity holding the fragment has been recreated in a
     * way that doesn't permit the fragment to store its own state
     *
     * @param state The bundle holding the stored state
     */
    fun restoreState(state: Bundle?) = state?.let { saveState = it }

    /**
     * Saves the state of the fragments to a bundle. Restore the state with [restoreState]
     *
     * @param saveState The bundle to store the state to
     */
    fun saveState(saveState: Bundle) {
        // If the fragments haven't been recreated yet, ie. in a subreddit from the navbar and they were destroyed
        // but haven't yet been accessed again
        fragments.forEach {
            it.saveState(saveState)
        }

        // viewPager shouldn't be null, but if it is use the default position
        saveState.putInt(ACTIVE_SUBREDDIT_KEY, viewPager?.currentItem ?: getDefaultSubPosition())
    }
}