package com.example.hakonsreader.activities

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import com.example.hakonsreader.R
import com.example.hakonsreader.fragments.ProfileFragment
import com.example.hakonsreader.interfaces.LockableSlidr
import com.r0adkll.slidr.Slidr
import com.r0adkll.slidr.model.SlidrInterface

/**
 * Activity to show a users profile.
 */
class ProfileActivity : BaseActivity(), LockableSlidr {

    companion object {
        private const val TAG = "ProfileActivity"

        /**
         * The key used to send which username the profile is for
         */
        const val USERNAME_KEY = "username"

        /**
         * The key used to save the fragment when the activity has been killed and recreated
         */
        const val SAVED_FRAGMENT = "savedFragment"
    }

    private var fragment: ProfileFragment? = null
    private lateinit var slidrInterface: SlidrInterface
    private var totalSlidrLocks = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val username = intent.extras?.getString(USERNAME_KEY)

        if (username == null) {
            finish()
            return
        }

        fragment = if (savedInstanceState != null) {
            supportFragmentManager.getFragment(savedInstanceState, SAVED_FRAGMENT) as ProfileFragment?
        } else {
            ProfileFragment.newInstance(username)
        }?.apply {
            retainInstance = true

            supportFragmentManager.beginTransaction()
                    .replace(R.id.profileContainer, this)
                    .commit()
        }

        slidrInterface = Slidr.attach(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        fragment?.let { supportFragmentManager.putFragment(outState, SAVED_FRAGMENT, it) }
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

    override fun setSupportActionBar(toolbar: Toolbar?) {
        super.setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { finish(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }
}