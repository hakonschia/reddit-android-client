package com.example.hakonsreader.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.activites.MainActivity;

public class SettingsFragment extends PreferenceFragmentCompat {
    private static final String TAG = "SettingsFragment";

    private SharedPreferences settings;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
        getPreferenceManager();
        settings = PreferenceManager.getDefaultSharedPreferences(requireContext());

        // Set that the auto hide comments preference is only a number (with sign)
        EditTextPreference hideComments = findPreference(getString(R.string.prefs_key_hide_comments_threshold));
        hideComments.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED));
        hideComments.setOnPreferenceChangeListener(hideCommentsChangeListener);
        setHideCommentsSummary(hideComments, null);

        ListPreference language = findPreference(getString(R.string.prefs_key_language));
        language.setOnPreferenceChangeListener(languageChangeListener);

        ListPreference autoPlayVideos = findPreference(getString(R.string.prefs_key_auto_play_videos));
        autoPlayVideos.setOnPreferenceChangeListener(autoPlayVideosChangeListener);

        // The enabled state isn't stored, so if never auto playing videos is set then disable the nsfw auto play
        // (this is the same functionality as is done in autoPlayVideosChangeListener)
        SwitchPreference autoPlayNsfwVideos = findPreference(getString(R.string.prefs_key_auto_play_nsfw_videos));
        boolean neverAutoPlayVideos = settings.getString(getString(R.string.prefs_key_auto_play_videos), getString(R.string.prefs_default_value_auto_play_videos))
                .equals(getString(R.string.prefs_key_auto_play_videos_never));
        autoPlayNsfwVideos.setEnabled(!neverAutoPlayVideos);

        EditTextPreference filteredSubreddits = findPreference(getString(R.string.prefs_key_filter_posts_from_default_subreddits));
        filteredSubreddits.setOnPreferenceChangeListener(filteredSubredditsChangeListener);
        setFilteredSubredditsSummary(filteredSubreddits, null);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        String key = preference.getKey();

        // Update the theme right away
        if (key.equals(getString(R.string.prefs_key_theme))) {
            App.get().updateTheme();
        }

        return super.onPreferenceTreeClick(preference);
    }


    /**
     * Listener for the threshold for hiding comments. This ensures that if nothing is input into the input
     * field then the default is set
     */
    private final Preference.OnPreferenceChangeListener hideCommentsChangeListener = (preference, newValue) -> {
        // The value is stored as a string
        String value = (String) newValue;

        // If no value is set, set to default
        if (value == null || value.isEmpty()) {
            int defaultHideComments = getResources().getInteger(R.integer.prefs_default_hide_comments_threshold);
            String defaultAsString = String.valueOf(defaultHideComments);

            settings.edit()
                    .putString(getString(R.string.prefs_key_hide_comments_threshold), defaultAsString)
                    .apply();

            // TODO going into the setting again still shows the old value, even though it is actually updated
            //  going out of the fragment and then going back shows the correct value
            setHideCommentsSummary((EditTextPreference) preference, defaultAsString);

            // return false = don't update since we're manually updating the value ourselves
            return false;
        }

        setHideCommentsSummary((EditTextPreference) preference, value);

        return true;
    };

    /**
     * Listener for language changes. Language is updated automatically
     */
    private final Preference.OnPreferenceChangeListener languageChangeListener = (preference, newValue) -> {
        // Changing the language is seemingly not possible from an Application class, so do it in MainActivity instead
        // This won't cause an issue as the settings fragment is always a fragment in MainActivity (although if this is changed
        // this will also have to be changed, so if I ever change it lets hope I actually remember it)
        MainActivity activity = (MainActivity) getActivity();

        // We have to pass the language here since the value stored in SharedPreferences isn't updated until
        // this function returns true, so trying to retrieve the value in updateLanguage() would retrieve the old value
        activity.updateLanguage(newValue.toString());
        return true;
    };

    /**
     * Listener for when auto playing videos changes. This will change the auto play NSFW videos preference
     * as it should be dependant on the normal auto play.
     *
     * <p>If normal auto play is set to "Never", NSFW auto play should be disabled as well</p>
     */
    private final Preference.OnPreferenceChangeListener autoPlayVideosChangeListener = (preference, newValue) -> {
        String asString = (String) newValue;
        SwitchPreference autoPlayNsfw = findPreference(getString(R.string.prefs_key_auto_play_nsfw_videos));

        // TODO this isnt updated when going into the settings, only when changing. Have to enable this in onCreatePreferences as well
        if (asString.equals(getString(R.string.prefs_key_auto_play_videos_never))) {
            // Ideally I wouldn't have to manually call setChecked(false) as it would be ideal if the actual value
            // would be saved, but when retrieving the value and the preference is disabled it would always return false
            // But this works fine enough
            autoPlayNsfw.setEnabled(false);
            autoPlayNsfw.setChecked(false);
        } else {
            autoPlayNsfw.setEnabled(true);
        }

        return true;
    };

    private final Preference.OnPreferenceChangeListener filteredSubredditsChangeListener = (preference, value) -> {
        setFilteredSubredditsSummary((EditTextPreference) preference, (String) value);
        return true;
    };


    /**
     * Sets the summary for the hide comments threshold preference
     *
     * @param preference The preference
     * @param value The value to use directly. If this is {@code null} then the value stored
     *              in the preference is used
     */
    private void setHideCommentsSummary(EditTextPreference preference, @Nullable String value) {
        if (value == null) {
            value = preference.getText();
        }
        preference.setSummary(value);
    }

    /**
     * Sets the summary for the filtered subreddits preference
     *
     * @param preference The preference
     * @param value The value to use directly. If this is {@code null} then the value stored
     *              in the preference is used
     */
    private void setFilteredSubredditsSummary(@NonNull EditTextPreference preference, @Nullable String value) {
        // The value must be passed since when it is used in the changeListener the value in
        // the preference wont be updated until the change listener returns

        // This won't update before you go in/out of the settings, since changing the summary provider
        // when it already has been is apparently not allowed

        if (value == null) {
            value = preference.getText();
        }

        String[] filteredSubs = value.split("\n");
        int count = 0;
        // Count only the non-empty subreddits (as they're not actual subredits)
        // Probably a better way to do this but whatever
        for (String filteredSub : filteredSubs) {
            if (!filteredSub.isEmpty()) {
                count++;
            }
        }

        String filteredSummary = getResources().getQuantityString(R.plurals.filteredSubredditsSummary, count, count);
        preference.setSummary(filteredSummary);
    }
}