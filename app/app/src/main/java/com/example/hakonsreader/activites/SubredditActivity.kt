package com.example.hakonsreader.activites

import android.os.Bundle
import android.view.View
import androidx.drawerlayout.widget.DrawerLayout
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.api.enums.PostTimeSort
import com.example.hakonsreader.api.enums.SortingMethods
import com.example.hakonsreader.fragments.SubredditFragment
import com.example.hakonsreader.interfaces.LockableSlidr
import com.r0adkll.slidr.Slidr
import com.r0adkll.slidr.model.SlidrInterface

/**
 * Activity for a subreddit (used when a subreddit is clicked from a post)
 */
class SubredditActivity : BaseActivity(), LockableSlidr {

    companion object {
        private const val TAG = "SubredditActivity"

        /**
         * The key used to save the subreddit fagment
         */
        private const val SAVED_SUBREDDIT = "subredditFragment"

        /**
         * The key used to transfer data about which subreddit the activity is for
         */
        const val SUBREDDIT_KEY = "subreddit"

        /**
         * The key used to send to this activity how to sort the posts when loading this subreddit
         *
         * The value with this key should be the value of corresponding enum value from [SortingMethods]
         */
        const val SORT = "sort"

        /**
         * The key used to send to this activity the time sort for the posts when loading this subreddit
         *
         * The value with this key should be the value of corresponding enum value from [PostTimeSort]
         */
        const val TIME_SORT = "time_sort"

        /**
         * The key used to send to this activity if the subreddit rules should automatically be shown
         * when entering the subreddit (does not apply for standard subs)
         *
         * The value with this key should be a [Boolean]
         */
        const val SHOW_RULES = "show_rules"
    }


    private var fragment: SubredditFragment? = null
    private lateinit var slidrInterface: SlidrInterface
    private var totalSlidrLocks = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subreddit)
        slidrInterface = Slidr.attach(this)

        // Restore the fragment if possible
        if (savedInstanceState != null) {
            fragment = supportFragmentManager.findFragmentByTag(SAVED_SUBREDDIT) as SubredditFragment?
        } else {
            var subreddit = ""
            var sort: SortingMethods? = null
            var timeSort: PostTimeSort? = null
            var showRules = false

            val intent = intent
            val uri = intent.data

            // Activity started from URL intent
            if (uri != null) {

                // First path segment is "/r/", second is the subreddit
                subreddit = uri.pathSegments[1]
            } else {
                // Activity started from manual intent in app
                val data = intent.extras

                if (data != null) {
                    val sub = data.getString(SUBREDDIT_KEY)
                    sort = data.getString(SORT)?.let { s -> SortingMethods.values().find { it.value == s } }
                    timeSort = data.getString(TIME_SORT)?.let { s -> PostTimeSort.values().find { it.value == s } }
                    showRules = data.getBoolean(SHOW_RULES)

                    if (sub != null) {
                        subreddit = sub
                    } else {
                        finish()
                        return
                    }
                }
            }

            // The slidr has to be locked when the drawer is open, otherwise swiping the drawer
            // away will also swipe the activity away
            val drawerListener = object : DrawerLayout.DrawerListener {
                override fun onDrawerOpened(drawerView: View) {
                    lock(true)
                }

                override fun onDrawerClosed(drawerView: View) {
                    lock(false)
                }

                override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
                override fun onDrawerStateChanged(newState: Int) {}
            }

            fragment = SubredditFragment.newInstance(subreddit, sort, timeSort, showRules).apply {
                this.drawerListener = drawerListener
                supportFragmentManager.beginTransaction()
                        .add(R.id.subredditActivityFragment, this, SAVED_SUBREDDIT)
                        .commit()
            }
        }
    }

    override fun onBackPressed() {
        if (fragment?.closeDrawerIfOpen() == false) {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        App.get().setActiveActivity(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        supportFragmentManager.putFragment(outState, SAVED_SUBREDDIT, fragment!!)
    }

    override fun lock(lock: Boolean) {
        if (lock) {
            totalSlidrLocks++
        } else {
            totalSlidrLocks--
        }

        checkSlidr()
    }

    /**
     * Locks the [slidrInterface] if [totalSlidrLocks] is greater than 0, and unlocks otherwise
     *
     * if [totalSlidrLocks] has been set to below 0, then it is set back to 0 first
     */
    private fun checkSlidr() {
        if (totalSlidrLocks < 0) {
            totalSlidrLocks = 0
        }

        if (totalSlidrLocks > 0) {
            slidrInterface.lock()
        } else {
            slidrInterface.unlock()
        }
    }

    /**
     * @return The name of the subreddit the activity is displaying
     */
    fun getSubredditName(): String? {
        return fragment?.subredditName
    }
}