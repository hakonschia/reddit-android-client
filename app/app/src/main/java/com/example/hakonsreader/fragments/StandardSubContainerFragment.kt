package com.example.hakonsreader.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.hakonsreader.R
import com.example.hakonsreader.api.enums.PostTimeSort
import com.example.hakonsreader.api.enums.SortingMethods
import com.example.hakonsreader.misc.SectionsPageAdapter
import java.util.ArrayList

class StandardSubContainerFragment : Fragment() {

    companion object {
        private const val TAG = "StandardSubContainerFragment"

        /**
         * The key used to store the subreddit currently visible on the screen
         */
        private const val ACTIVE_SUBREDDIT_KEY = "posts_container_active_subreddit"


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

    private var overridenSub: StandarSub? = null

    private var viewPager: ViewPager2? = null

    /**
     * The currently active item in the ViewPager, or -1 if not yet set
     */
    private var activeItem = -1

    /**
     * Sets the subreddit to be active. If the view is not currently created, then the value will
     * be stored and set when the view is created. Otherwise, the correct subreddit will be selected
     * with a sliding animation
     */
    fun setActiveSubreddit(sub: StandarSub) {
        if (viewPager == null) {
            overridenSub = sub
        } else {
            overridenSub = null
            viewPager!!.currentItem = sub.ordinal
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_posts_container, container, false)

        viewPager = view.findViewById<ViewPager2>(R.id.postsContainer).apply {
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    setToolbar(position)

                    // Save the active item now, as the viewpager might be nulled by the time saveState is called
                    activeItem = position
                }
            })

            setupViewPager(this, savedInstanceState)
        }

        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(ACTIVE_SUBREDDIT_KEY, activeItem)
    }

    override fun onDestroyView() {
        viewPager = null
        super.onDestroyView()
    }

    private fun setupViewPager(viewPager: ViewPager2, savedInstanceState: Bundle?) {
        val startPos: Int = if (overridenSub != null) {
            val v = overridenSub!!.ordinal
            overridenSub = null
            v
        } else {
            when {
                activeItem >= 0 -> activeItem
                savedInstanceState != null -> savedInstanceState.getInt(ACTIVE_SUBREDDIT_KEY)
                else -> getDefaultSubPosition()
            }
        }

        Adapter(startPos, this).apply {
            viewPager.adapter = this
        }

        // Always keep all fragments alive
        viewPager.offscreenPageLimit = 3

        // If an item has been selected based on overridenSub, and it isn't the sub previously selected
        // then it wouldn't switch for some reason, so put it in post
        viewPager.post {
            viewPager.setCurrentItem(startPos, false)
        }
    }

    /**
     * Gets the array position in [fragments] based on [defaultSub]
     */
    private fun getDefaultSubPosition() = defaultSub.ordinal

    /**
     * Calls [AppCompatActivity.setSupportActionBar] on the fragments activity with the toolbar in
     * the subreddit given by [position]
     *
     * @param position The subreddit position in the ViewPager/[getChildFragmentManager]
     */
    private fun setToolbar(position: Int) {
        // For some reason there is a sync issue between the this.fragments and the fragments actually
        // shown, so this is the easiest way to get the actual fragments to correctly set the toolbar
        // This issue only appears when the nav drawer has been opened from this container fragment
        // and changing the theme, which causes a recreate and some issues occur with setting the toolbar
        val fragments = childFragmentManager.fragments
        if (fragments.size >= position) {
            (fragments[position] as SubredditFragment).getToolbar()?.let {
                (requireActivity() as AppCompatActivity).setSupportActionBar(it)
            }
        }
    }

    private inner class Adapter(val startPos: Int, fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount() = 3
        override fun createFragment(position: Int) : Fragment {
            return when (position) {
                0 -> SubredditFragment.newInstance("", SortingMethods.HOT, PostTimeSort.DAY)
                1 -> SubredditFragment.newInstance("Popular", SortingMethods.HOT, PostTimeSort.DAY)
                2 -> SubredditFragment.newInstance("All", SortingMethods.HOT, PostTimeSort.DAY)

                // Idk?
                else -> SubredditFragment.newInstance("", SortingMethods.HOT, PostTimeSort.DAY)
            }.also {
                it.setToolbarOnActivity = startPos == position
            }
        }
    }
}