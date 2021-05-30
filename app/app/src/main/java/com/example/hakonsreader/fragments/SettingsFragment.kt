package com.example.hakonsreader.fragments

import android.app.Dialog
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.preference.*
import com.example.hakonsreader.R
import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.broadcastreceivers.InboxWorkerStartReceiver
import com.example.hakonsreader.interfaces.LanguageListener
import com.example.hakonsreader.interfaces.OnUnreadMessagesBadgeSettingChanged
import com.example.hakonsreader.views.preferences.multicolor.MultiColorFragCompat
import com.example.hakonsreader.views.preferences.multicolor.MultiColorPreference
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Fragment for displaying user settings
 */
@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {

    companion object {
        @Suppress("UNUSED")
        private const val TAG = "SettingsFragment"

        /**
         * The String.format() format used for formatting the summary for the link scale
         */
        private const val LINK_SCALE_SUMMARY_FORMAT = "%.2f"

        /**
         * @return A new instance of this fragment
         */
        fun newInstance() = SettingsFragment()
    }

    // The API won't be used to make any calls here, but is needed to adjust third party options
    @Inject
    lateinit var api: RedditApi

    private lateinit var settings: SharedPreferences

    var unreadMessagesBadgeSettingChanged: OnUnreadMessagesBadgeSettingChanged? = null
    var languageListener: LanguageListener? = null

    // TODO the filtered subreddits dont update until the fragment is recreated, if you go to settings
    //  then filter a subreddit then go back it wont appear since the fragment isn't recreated

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        settings = PreferenceManager.getDefaultSharedPreferences(requireContext())

        val themePreference: SwitchPreference? = findPreference(getString(R.string.prefs_key_theme))
        themePreference?.onPreferenceChangeListener = themeChangeListener

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

        // The enabled state isn't stored, so if auto playing videos is disabled then disable the nsfw auto play
        // (this is the same functionality as is done in autoPlayVideosChangeListener)
        val autoPlayNsfwVideos: SwitchPreference? = findPreference(getString(R.string.prefs_key_auto_play_nsfw_videos))

        val autoPlayVideos: SwitchPreference? = findPreference(getString(R.string.prefs_key_auto_play_videos_switch))
        autoPlayVideos?.let {
            it.onPreferenceChangeListener = autoPlayVideosChangeListener
            autoPlayNsfwVideos?.isEnabled = !it.isChecked
            if (it.isChecked) {
                autoPlayNsfwVideos?.isEnabled = true
            } else {
                autoPlayNsfwVideos?.isEnabled = false
                autoPlayNsfwVideos?.isChecked = false
            }
        }

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

        // Third party options all use the same listener
        findPreference<SwitchPreference>(getString(R.string.prefs_key_third_party_load_gfycat_gifs))?.let {
            it.onPreferenceChangeListener = thirdPartyOptionsListener
        }
        findPreference<SwitchPreference>(getString(R.string.prefs_key_third_party_load_imgur_gifs))?.let {
            it.onPreferenceChangeListener = thirdPartyOptionsListener
        }
        findPreference<SwitchPreference>(getString(R.string.prefs_key_third_party_load_imgur_albums))?.let {
            it.onPreferenceChangeListener = thirdPartyOptionsListener
        }

        findPreference<ListPreference>(getString(R.string.prefs_key_inbox_update_frequency))?.let {
            it.onPreferenceChangeListener = inboxFrequencyListener
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference?) {
        if (preference is MultiColorPreference) {
            val dialog = MultiColorFragCompat.newInstance(preference.key)
            dialog.setTargetFragment(this, 0)
            dialog.show(parentFragmentManager, null)
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    /**
     * Listener for light/dark mode. Automatically updates the theme
     */
    private val themeChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
        newValue as Boolean
        if (newValue) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        true
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
        newValue as String

        // We have to pass the language here since the value stored in SharedPreferences isn't updated until
        // this function returns true, so trying to retrieve the value in updateLanguage() would retrieve the old value
        languageListener?.updateLanguage(newValue)
        true
    }

    /**
     * Listener for when auto playing videos changes. This will change the auto play NSFW videos preference
     * as it should be dependant on the normal auto play.
     *
     * If normal auto play is set to "Never", NSFW auto play should be disabled as well
     */
    private val autoPlayVideosChangeListener = Preference.OnPreferenceChangeListener { _: Preference?, newValue: Any ->
        val autoPlayNsfw: SwitchPreference = findPreference(getString(R.string.prefs_key_auto_play_nsfw_videos))
                ?: return@OnPreferenceChangeListener true

        // TODO this isnt updated when going into the settings, only when changing. Have to enable this in onCreatePreferences as well
        if (newValue as Boolean) {
            autoPlayNsfw.isEnabled = true
        } else {
            // Ideally I wouldn't have to manually call setChecked(false) as it would be ideal if the actual value
            // would be saved, but when retrieving the value and the preference is disabled it would always return false
            // But this works fine enough
            autoPlayNsfw.isEnabled = false
            autoPlayNsfw.isChecked = false
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

        return@OnPreferenceChangeListener if (newValue) {
            CrashReportsConfirmDialog.newInstance(preference.key).show(childFragmentManager, "crash_reports_dialog")
            // The setting should only be actually enabled by the user confirming in the dialog
            false
        } else {
            // https://firebase.google.com/docs/projects/manage-installations#delete-fid
            FirebaseInstallations.getInstance().delete()
            Firebase.crashlytics.setCrashlyticsCollectionEnabled(false)

            // When turning the setting off we should keep the new value
            true
        }
    }

    /**
     * Generic listener for third party options changes (only for boolean options)
     */
    private val thirdPartyOptionsListener = Preference.OnPreferenceChangeListener { preference, newValue ->
        // It is an error to use this listener on anything else than a SwitchPreference
        newValue as Boolean

        when (preference.key) {
            getString(R.string.prefs_key_third_party_load_gfycat_gifs) -> api.thirdPartyOptions.loadGfycatGifs = newValue
            getString(R.string.prefs_key_third_party_load_imgur_gifs) -> api.thirdPartyOptions.loadImgurGifs = newValue
            getString(R.string.prefs_key_third_party_load_imgur_albums) ->  api.thirdPartyOptions.loadImgurAlbums = newValue
        }

        true
    }

    private val inboxFrequencyListener = Preference.OnPreferenceChangeListener { _, newValue ->
        val freq = when (newValue as String) {
            getString(R.string.prefs_key_inbox_update_frequency_15_min) -> 15
            getString(R.string.prefs_key_inbox_update_frequency_30_min) -> 30
            getString(R.string.prefs_key_inbox_update_frequency_60_min) -> 60
            else -> -1
        }

        InboxWorkerStartReceiver.startInboxWorker(requireContext(), freq, replace = true)

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


    /**
     * DialogFragment that ensures the user confirms enabling Firebase crash reports
     *
     * Note this fragment MUST be a child fragment of a [PreferenceFragmentCompat], otherwise it will crash
     */
    class CrashReportsConfirmDialog : DialogFragment() {
        companion object {
            private const val ARGS_PREFERENCE_KEY = "args_preferenceKey"

            fun newInstance(preferenceKey: String) = CrashReportsConfirmDialog().apply {
                arguments = bundleOf(ARGS_PREFERENCE_KEY to preferenceKey)
            }
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val context = requireContext()
            return Dialog(context).apply {
                setContentView(R.layout.dialog_enable_crashlytics)
                window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

                setCancelable(false)
                setCanceledOnTouchOutside(false)

                findViewById<Button>(R.id.btnEnable).setOnClickListener {
                    val parent = requireParentFragment() as PreferenceFragmentCompat
                    parent.findPreference<SwitchPreference>(requireArguments().getString(ARGS_PREFERENCE_KEY)!!)?.let {
                        it.sharedPreferences.edit()
                            .putBoolean(it.key, true)
                            .apply()
                        it.isChecked = true
                    }

                    Firebase.crashlytics.setCrashlyticsCollectionEnabled(true)
                    dismiss()
                }

                findViewById<Button>(R.id.btnCancel).setOnClickListener {
                    // This should already be false, but just to be sure
                    Firebase.crashlytics.setCrashlyticsCollectionEnabled(false)
                    dismiss()
                }
            }
        }
    }
}