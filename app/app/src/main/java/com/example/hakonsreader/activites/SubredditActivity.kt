package com.example.hakonsreader.activites

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.fragments.SubredditFragment
import com.r0adkll.slidr.Slidr
import com.r0adkll.slidr.model.SlidrInterface

/**
 * Activity for a subreddit (used when a subreddit is clicked from a post)
 */
class SubredditActivity : AppCompatActivity() {

    companion object {
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
    private var slidrInterface: SlidrInterface? = null

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

    /**
     * {@inheritDoc}
     */
    fun lock(lock: Boolean) {
        if (lock) {
            slidrInterface!!.lock()
        } else {
            slidrInterface!!.unlock()
        }
    }

    /**
     * @return The name of the subreddit the activity is displaying
     */
    fun getSubredditName(): String? {
        return fragment!!.subredditName
    }
}