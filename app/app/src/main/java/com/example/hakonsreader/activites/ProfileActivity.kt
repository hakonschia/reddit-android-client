package com.example.hakonsreader.activites

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.hakonsreader.R
import com.example.hakonsreader.fragments.ProfileFragment
import com.example.hakonsreader.interfaces.LockableSlidr
import com.r0adkll.slidr.Slidr
import com.r0adkll.slidr.model.SlidrInterface

/**
 * Activity to show a users profile.
 */
class ProfileActivity : AppCompatActivity(), LockableSlidr {

    companion object {

        /**
         * The key used to send which username the profile is for
         */
        const val USERNAME_KEY = "username"
    }

    private lateinit var slidrInterface: SlidrInterface


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val username = intent.extras?.getString(USERNAME_KEY)

        if (username == null) {
            finish()
            return
        }

        val fragment = ProfileFragment.newInstance(username)

        supportFragmentManager.beginTransaction()
                .replace(R.id.profileContainer, fragment)
                .commit()

        slidrInterface = Slidr.attach(this)
    }

    override fun lock(lock: Boolean) {
        if (lock) {
            slidrInterface.lock()
        } else {
            slidrInterface.unlock()
        }
    }

}