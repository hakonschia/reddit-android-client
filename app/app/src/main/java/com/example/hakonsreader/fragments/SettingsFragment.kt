package com.example.hakonsreader.fragments

import android.app.Dialog
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.app.DialogCompat
import androidx.preference.*
import com.example.hakonsreader.App.Companion.get
import com.example.hakonsreader.R
import com.example.hakonsreader.activites.MainActivity
import com.example.hakonsreader.interfaces.OnUnreadMessagesBadgeSettingChanged
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.ktx.Firebase

class SettingsFragment : PreferenceFragmentCompat() {
    companion object {
        /**
         * The String.format() format used for formatting the summary for the link scale
         */
        private const val LINK_SCALE_SUMMARY_FORMAT = "%.2f"
    }

    private lateinit var settings: SharedPreferences

    var unreadMessagesBadgeSettingChanged: OnUnreadMessagesBadgeSettingChanged? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        settings = PreferenceManager.getDefaultSharedPreferences(requireContext())

        // Set that the auto hide comments preference is only a number (with sign)
        val hideComments: EditTextPreference? = findPreference(getString(R.string.prefs_key_hide_comments_threshold))
        hideComments?.let {
            it.setOnBindEditTextListener { editText: EditText -> editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED }
            it.onPreferenceChangeListener = hideCommentsChangeListener
            setHideCommentsSummary(it, null)
        }

        val language: ListPreference? = findPreference(getString(R.string.prefs_key_language))
        language?.let {
            it.onPreferenceChangeListener = languageChangeListener
        }

        val autoPlayVideos: ListPreference? = findPreference(getString(R.string.prefs_key_auto_play_videos))
        autoPlayVideos?.let {
            it.onPreferenceChangeListener = autoPlayVideosChangeListener
        }

        // The enabled state isn't stored, so if never auto playing videos is set then disable the nsfw auto play
        // (this is the same functionality as is done in autoPlayVideosChangeListener)
        val autoPlayNsfwVideos: SwitchPreference? = findPreference(getString(R.string.prefs_key_auto_play_nsfw_videos))
        val neverAutoPlayVideos = (settings.getString(getString(R.string.prefs_key_auto_play_videos), getString(R.string.prefs_default_value_auto_play_videos))
                == getString(R.string.prefs_key_auto_play_videos_never))
        autoPlayNsfwVideos?.isEnabled = !neverAutoPlayVideos

        val filteredSubreddits: EditTextPreference? = findPreference(getString(R.string.prefs_key_filter_posts_from_default_subreddits))
        filteredSubreddits?.let {
            it.onPreferenceChangeListener = filteredSubredditsChangeListener
            setFilteredSubredditsSummary(it, null)
        }

        val linkScalePreference: SeekBarPreference? = findPreference(getString(R.string.prefs_key_link_scale))
        linkScalePreference?.let {
            it.onPreferenceChangeListener = linkScaleChangeListener
            it.summary = String.format(LINK_SCALE_SUMMARY_FORMAT, linkScalePreference.value / 100f)
        }

        val unreadMessagesBadgePreference: SwitchPreference? = findPreference(getString(R.string.prefs_key_inbox_show_badge))
        unreadMessagesBadgePreference?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue: Any? ->
            unreadMessagesBadgeSettingChanged?.showUnreadMessagesBadge(newValue as Boolean)
            true
        }

        val crashReportPreference: SwitchPreference? = findPreference(getString(R.string.prefs_key_send_crash_reports))
        crashReportPreference?.let {
            it.onPreferenceChangeListener = crashReportsChangeListener
        }
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        val key = preference.key

        // Update the theme right away
        if (key == getString(R.string.prefs_key_theme)) {
            get().updateTheme()
        }

        return super.onPreferenceTreeClick(preference)
    }


    /**
     * Listener for the threshold for hiding comments. This ensures that if nothing is input into the input
     * field then the default is set
     */
    private val hideCommentsChangeListener = Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any? ->
        preference as EditTextPreference
        // The value is stored as a string
        val value = newValue as String?

        // If no value is set, set to default
        if (value == null || value.isEmpty()) {
            val defaultHideComments = resources.getInteger(R.integer.prefs_default_hide_comments_threshold)
            val defaultAsString = defaultHideComments.toString()
            settings.edit()
                    .putString(getString(R.string.prefs_key_hide_comments_threshold), defaultAsString)
                    .apply()

            // TODO going into the setting again still shows the old value, even though it is actually updated
            //  going out of the fragment and then going back shows the correct value
            setHideCommentsSummary(preference, defaultAsString)

            // return false = don't update since we're manually updating the value ourselves
            return@OnPreferenceChangeListener false
        }

        setHideCommentsSummary(preference, value)
        true
    }

    /**
     * Listener for language changes. Language is updated automatically
     */
    private val languageChangeListener = Preference.OnPreferenceChangeListener { _, newValue: Any ->
        // Changing the language is seemingly not possible from an Application class, so do it in MainActivity instead
        // This won't cause an issue as the settings fragment is always a fragment in MainActivity (although if this is changed
        // this will also have to be changed, so if I ever change it lets hope I actually remember it)
        val activity = activity as MainActivity?

        // We have to pass the language here since the value stored in SharedPreferences isn't updated until
        // this function returns true, so trying to retrieve the value in updateLanguage() would retrieve the old value
        activity?.updateLanguage(newValue.toString())
        true
    }

    /**
     * Listener for when auto playing videos changes. This will change the auto play NSFW videos preference
     * as it should be dependant on the normal auto play.
     *
     * If normal auto play is set to "Never", NSFW auto play should be disabled as well
     */
    private val autoPlayVideosChangeListener = Preference.OnPreferenceChangeListener { _: Preference?, newValue: Any ->
        val asString = newValue as String
        val autoPlayNsfw: SwitchPreference = findPreference(getString(R.string.prefs_key_auto_play_nsfw_videos))
                ?: return@OnPreferenceChangeListener true

        // TODO this isnt updated when going into the settings, only when changing. Have to enable this in onCreatePreferences as well
        if (asString == getString(R.string.prefs_key_auto_play_videos_never)) {
            // Ideally I wouldn't have to manually call setChecked(false) as it would be ideal if the actual value
            // would be saved, but when retrieving the value and the preference is disabled it would always return false
            // But this works fine enough
            autoPlayNsfw.isEnabled = false
            autoPlayNsfw.isChecked = false
        } else {
            autoPlayNsfw.isEnabled = true
        }
        true
    }

    private val filteredSubredditsChangeListener = Preference.OnPreferenceChangeListener { preference: Preference, value: Any? ->
        setFilteredSubredditsSummary(preference as EditTextPreference, value as String?)
        true
    }

    /**
     * Listener that updates the summary on the preference to the actual value the new value represents
     */
    private val linkScaleChangeListener = Preference.OnPreferenceChangeListener { preference: Preference, newValue: Any ->
        val value = newValue as Int
        val actualValue = value / 100f
        preference.summary = String.format(LINK_SCALE_SUMMARY_FORMAT, actualValue)
        true
    }

    /**
     * Listener that updates if Firebase crashlytics is enabled or disabled
     */
    private val crashReportsChangeListener = Preference.OnPreferenceChangeListener { preference: Preference, newValue: Any ->
        newValue as Boolean
        preference as SwitchPreference

        // Delete unsent reports. If this goes from true to false it makes sense to delete them, if it goes from false to true
        // it also makes sense as the user might not want the previous reports to be sent
        Firebase.crashlytics.deleteUnsentReports()

        if (newValue) {
            Dialog(requireContext()).apply {
                setContentView(R.layout.dialog_enable_crashlytics)
                window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

                setCancelable(false)
                setCanceledOnTouchOutside(false)

                findViewById<Button>(R.id.btnEnable).setOnClickListener {
                    Firebase.crashlytics.setCrashlyticsCollectionEnabled(true)
                    dismiss()
                }

                findViewById<Button>(R.id.btnCancel).setOnClickListener {
                    // This should already be false, but just to be sure
                    Firebase.crashlytics.setCrashlyticsCollectionEnabled(false)

                    // The preference listener always returns true, but if this is called then it was
                    // canceled by the user afterwards, so the setting has to be set to false again
                    preference.sharedPreferences.edit()
                            .putBoolean(preference.key, false)
                            .apply()
                    preference.isChecked = false

                    dismiss()
                }

                show()
            }
        } else {
            // https://firebase.google.com/docs/projects/manage-installations#delete-fid
            FirebaseInstallations.getInstance().delete()
            Firebase.crashlytics.setCrashlyticsCollectionEnabled(false)
        }

        true
    }


    /**
     * Sets the summary for the hide comments threshold preference
     *
     * @param preference The preference
     * @param value The value to use directly. If this is `null` then the value stored
     * in the preference is used
     */
    private fun setHideCommentsSummary(preference: EditTextPreference, value: String?) {
        preference.summary = value ?: preference.text
    }

    /**
     * Sets the summary for the filtered subreddits preference
     *
     * @param preference The preference
     * @param value The value to use directly. If this is `null` then the value stored
     * in the preference is used
     */
    private fun setFilteredSubredditsSummary(preference: EditTextPreference, value: String?) {
        // The value must be passed since when it is used in the changeListener the value in
        // the preference wont be updated until the change listener returns

        // This won't update before you go in/out of the settings, since changing the summary provider
        // when it already has been is apparently not allowed

        val filteredSubs = (value ?: preference.text).split("\n".toRegex()).toTypedArray()
        var count = 0

        for (filteredSub in filteredSubs) {
            // In case there are empty lines, don't count them
            if (filteredSub.isNotBlank()) {
                count++
            }
        }

        val filteredSummary = resources.getQuantityString(R.plurals.filteredSubredditsSummary, count, count)
        preference.summary = filteredSummary
    }

}