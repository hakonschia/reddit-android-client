package com.example.hakonsreader.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

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

        ListPreference language = findPreference(getString(R.string.prefs_key_language));
        language.setOnPreferenceChangeListener(languageChangeListener);
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
            // return false = don't update since we're manually updating the value ourselves
            // TODO This doesn't update the summary however, until the user goes out of the preferences and back
            //  this is only a visual "bug", as when the value is used it will be updated
            return false;
        }

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
}