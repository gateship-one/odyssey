package org.odyssey.fragments;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.odyssey.OdysseyMainActivity;
import org.odyssey.R;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.odyssey_main_settings);
        PreferenceManager.setDefaultValues(getActivity(),R.xml.odyssey_main_settings,false);

        // set toolbar behaviour and title
        OdysseyMainActivity activity = (OdysseyMainActivity) getActivity();
        activity.setUpToolbar(getResources().getString(R.string.fragment_title_settings), false, true);
        activity.setUpPlayButton(null);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("pref_theme")) {
            Intent intent = getActivity().getIntent();
            getActivity().finish();
            startActivity(intent);
        }
    }
}
