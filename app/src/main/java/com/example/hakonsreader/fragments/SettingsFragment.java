package com.example.hakonsreader.fragments;

import android.os.Bundle;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        String key = preference.getKey();

        // Update the theme right away so the app doesn't have to be restarted
        if (key.equals(getString(R.string.prefs_key_theme))) {
            App.updateTheme();
        }

        return super.onPreferenceTreeClick(preference);
    }
}