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

    private var overridenSub: StandarSub? = null

    private var saveState: Bundle? = null
    private var viewPager: ViewPager2? = null
    private val fragments = ArrayList<SubredditFragment>()

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

        setupFragments()

        viewPager = view.findViewById<ViewPager2>(R.id.postsContainer).apply {
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    setToolbar(position)

                    // Save the active item now, as the viewpager might be nulled by the time saveState is called
                    activeItem = position
                }
            })

            setupViewPager(this)
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewPager = null
    }

    private fun setupViewPager(viewPager: ViewPager2) {
        val startPos: Int = if (overridenSub != null) {
            val v = overridenSub!!.ordinal
            overridenSub = null
            v
        } else {
            when {
                activeItem >= 0 -> activeItem
                saveState != null -> saveState!!.getInt(ACTIVE_SUBREDDIT_KEY)
                else -> getDefaultSubPosition()
            }
        }

        fragments.forEachIndexed { index, fragment ->
            // Only set the toolbar automatically on the starting fragment
            fragment.setToolbarOnActivity = startPos == index
        }

        Adapter(fragments, this).apply {
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

        saveState.putInt(ACTIVE_SUBREDDIT_KEY, activeItem)
    }

    /**
     * Calls [AppCompatActivity.setSupportActionBar] on the fragments activity with the toolbar in
     * the subreddit given by [position]
     *
     * @param position The subreddit in position in [fragments] to set the toolbar for
     */
    private fun setToolbar(position: Int) {
        if (fragments.size >= position) {
            // Get the toolbar and set it on the activity. This has to be called each time
            // otherwise all the toolbars will be added as this container fragment is created
            // which means the last fragments toolbar will be the one added, which invalidates
            // the other toolbars click listeners
            fragments[position].getToolbar()?.let {
                (requireActivity() as AppCompatActivity).setSupportActionBar(it)
            }
        }
    }

    private inner class Adapter(val fragments: List<SubredditFragment>, fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount() = fragments.size
        override fun createFragment(position: Int) = fragments[position]
    }
}