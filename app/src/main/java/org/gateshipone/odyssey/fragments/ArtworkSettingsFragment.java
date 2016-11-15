/*
 * Copyright (C) 2016  Hendrik Borghorst & Frederik Luetkes
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.gateshipone.odyssey.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.artworkdatabase.ArtworkDatabaseManager;
import org.gateshipone.odyssey.artworkdatabase.ArtworkManager;
import org.gateshipone.odyssey.artworkdatabase.BulkDownloadService;
import org.gateshipone.odyssey.listener.ToolbarAndFABCallback;

import static org.gateshipone.odyssey.artworkdatabase.BulkDownloadService.BUNDLE_KEY_ALBUM_PROVIDER;
import static org.gateshipone.odyssey.artworkdatabase.BulkDownloadService.BUNDLE_KEY_ARTIST_PROVIDER;
import static org.gateshipone.odyssey.artworkdatabase.BulkDownloadService.BUNDLE_KEY_WIFI_ONLY;

public class ArtworkSettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    /**
     * Callback to setup toolbar and fab
     */
    private ToolbarAndFABCallback mToolbarAndFABCallback;

    /**
     * Called to do initial creation of a fragment.
     * <p>
     * This method will setup a listener to start the system audio equalizer.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // add listener to clear album data
        Preference clearAlbums = findPreference("pref_clear_album");
        clearAlbums.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                ArtworkDatabaseManager.getInstance(getContext()).clearAlbumImages();
                return true;
            }
        });

        // add listener to clear artist data
        Preference clearArtist = findPreference("pref_clear_artist");
        clearArtist.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                ArtworkDatabaseManager.getInstance(getContext()).clearArtistImages();
                return true;
            }
        });

        Preference clearBlockedAlbums = findPreference("pref_clear_blocked_album");
        clearBlockedAlbums.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                ArtworkDatabaseManager.getInstance(getContext()).clearBlockedAlbumImages();
                return true;
            }
        });

        Preference clearBlockedArtists = findPreference("pref_clear_blocked_artist");
        clearBlockedArtists.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                ArtworkDatabaseManager.getInstance(getContext()).clearBlockedArtistImages();
                return true;
            }
        });

        Preference buldLoad = findPreference("pref_bulk_load");
        buldLoad.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                Intent serviceIntent = new Intent(getActivity(), BulkDownloadService.class);
                serviceIntent.setAction(BulkDownloadService.ACTION_START_BULKDOWNLOAD);

                SharedPreferences sharedPref = android.support.v7.preference.PreferenceManager.getDefaultSharedPreferences(getContext().getApplicationContext());
                serviceIntent.putExtra(BUNDLE_KEY_ARTIST_PROVIDER, sharedPref.getString("pref_artist_provider", "last_fm"));
                serviceIntent.putExtra(BUNDLE_KEY_ALBUM_PROVIDER, sharedPref.getString("pref_album_provider", "musicbrainz"));
                serviceIntent.putExtra(BUNDLE_KEY_WIFI_ONLY, sharedPref.getBoolean("pref_download_wifi_only", true));
                getActivity().startService(serviceIntent);
                return true;
            }
        });
    }

    /**
     * Called when the fragment is first attached to its context.
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mToolbarAndFABCallback = (ToolbarAndFABCallback) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement ToolbarAndFABCallback");
        }
    }

    /**
     * Called when the fragment resumes.
     * <p/>
     * Register listener and setup the toolbar.
     */
    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        if (mToolbarAndFABCallback != null) {
            // set toolbar behaviour and title
            mToolbarAndFABCallback.setupToolbar(getResources().getString(R.string.fragment_title_settings), false, false, false);
            // set up play button
            mToolbarAndFABCallback.setupFAB(null);
        }
    }

    /**
     * Called when the Fragment is no longer resumed.
     * <p/>
     * Unregister listener.
     */
    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    /**
     * Create the preferences from an xml resource file
     */
    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.odyssey_artwork_settings);
        PreferenceManager.setDefaultValues(getActivity(), R.xml.odyssey_artwork_settings, false);
    }

    /**
     * Called when a shared preference is changed, added, or removed.
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("pref_album_provider") || key.equals("pref_artist_provider") || key.equals("pref_download_wifi_only")) {
            Intent nextIntent = new Intent(BulkDownloadService.ACTION_CANCEL_BULKDOWNLOAD);
            getActivity().getApplicationContext().sendBroadcast(nextIntent);

            ArtworkManager artworkManager = ArtworkManager.getInstance(getContext().getApplicationContext());

            artworkManager.cancelAllRequests(getContext());

            switch (key) {
                case "pref_album_provider":
                    artworkManager.setAlbumProvider(sharedPreferences.getString("pref_album_provider", "musicbrainz"));
                    break;
                case "pref_artist_provider":
                    artworkManager.setArtistProvider(sharedPreferences.getString("pref_artist_provider", "last_fm"));
                    break;
                case "pref_download_wifi_only":
                    artworkManager.setWifiOnly(sharedPreferences.getBoolean("pref_download_wifi_only", true));
                    break;
            }
        }
    }

}
