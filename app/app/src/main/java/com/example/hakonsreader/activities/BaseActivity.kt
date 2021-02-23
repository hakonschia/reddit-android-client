package com.example.hakonsreader.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.example.hakonsreader.R
import java.util.*

/**
 * Base activity that ensures the child activities will always have the correct language applied, based
 * on the value in SharedPreferences
 */
open class BaseActivity : AppCompatActivity() {

    // When switching from the default language to something else, only MainActivity would have
    // the locale updated, so if classes extend this instead of AppCompatActivity they will also be
    // updated when that language change is done

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateLanguage()
    }

    private fun updateLanguage() {
        val settings = PreferenceManager.getDefaultSharedPreferences(this)
        val lang = settings.getString(getString(R.string.prefs_key_language), getString(R.string.prefs_default_language))
            ?: return

        val config = resources.configuration
        config.setLocale(Locale(lang))
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}