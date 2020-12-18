package com.example.hakonsreader.activites

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.fragments.SubredditFragment
import com.example.hakonsreader.interfaces.LockableSlidr
import com.r0adkll.slidr.Slidr
import com.r0adkll.slidr.model.SlidrInterface

/**
 * Activity for a subreddit (used when a subreddit is clicked from a post)
 */
class SubredditActivity : AppCompatActivity(), LockableSlidr {

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
                    if (sub != null) {
                        subreddit = sub
                    } else {
                        finish()
                        return
                    }
                }
            }

            fragment = SubredditFragment.newInstance(subreddit)
            supportFragmentManager.beginTransaction()
                    .add(R.id.subredditActivityFragment, fragment!!, SAVED_SUBREDDIT)
                    .commit()
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
        Log.d(TAG, "checkSlidr: $totalSlidrLocks")
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
        return fragment?.getSubredditName()
    }
}