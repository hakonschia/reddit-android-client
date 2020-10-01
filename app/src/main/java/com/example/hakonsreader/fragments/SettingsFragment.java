package com.example.hakonsreader.fragments;

import android.os.Bundle;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;

public class SettingsFragment extends PreferenceFragmentCompat {
    private static final String TAG = "SettingsFragment";
    
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        Log.d(TAG, "onPreferenceTreeClick: " + preference);


        if (preference.getKey().equals(getString(R.string.theme_key))) {
            App.updateTheme();
            return true;
        }

        return super.onPreferenceTreeClick(preference);
    }
}