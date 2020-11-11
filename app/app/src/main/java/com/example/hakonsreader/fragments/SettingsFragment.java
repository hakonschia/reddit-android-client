package com.example.hakonsreader.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;

import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;

public class SettingsFragment extends PreferenceFragmentCompat {
    private static final String TAG = "SettingsFragment";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
        getPreferenceManager();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());

        // Set that the auto hide comments preference is only a number (with sign)
        EditTextPreference hideComments = getPreferenceManager().findPreference(getString(R.string.prefs_key_hide_comments_threshold));
        hideComments.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED));
        hideComments.setOnPreferenceChangeListener((preference, newValue) -> {
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
        });
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
}